package com.combatauras.render;

import com.combatauras.settings.SettingsSnapshot;
import com.combatauras.settings.VisualProfile;
import com.combatauras.model.VisualStyle;
import com.combatauras.model.ShowerStartLocation;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import com.combatauras.model.GlowColorMode;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;
import net.runelite.api.Client;
import net.runelite.api.SpritePixels;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;

/**
 * Procedural screen overlay renderer (edge vignette + icon shower).
 *
 * Performance notes:
 * - The vignette is 4 edge strips + 4 elliptical corners. Gradient paints are cached per (colour, size) and the
 *   animated intensity is applied through a pre-built AlphaComposite LUT, so steady-state frames allocate nothing.
 * - Nothing touches Graphics2D when no warning is active.
 * - Shower icons are scaled once and cached (Resource Pack sprite overrides respected).
 */
@javax.inject.Singleton
public class ProceduralRenderer
{
    private static final int ALPHA_STEPS = 64;
    private static final AlphaComposite[] ALPHA_LUT = new AlphaComposite[ALPHA_STEPS + 1];

    static
    {
        for (int i = 0; i <= ALPHA_STEPS; i++)
        {
            ALPHA_LUT[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i / (float) ALPHA_STEPS);
        }
    }

    // Unified pre-scaled icon caches (respects Resource Packs)
    private final KeyedCache<BufferedImage> spriteIconCache = new KeyedCache<>(64);
    private final KeyedCache<BufferedImage> itemIconCache = new KeyedCache<>(64);
    private int cachedIconSize = 0;
    private long lastGlowFrameNanos = 0L;
    private long showerStartNanos = 0L;

    // Eased falloff shared by edges and corners: 0 = innermost (clear), 1 = screen edge (full colour)
    private static final float[] GLOW_FRACTIONS = {0f, 0.35f, 0.65f, 0.85f, 1f};
    private static final float[] GLOW_ALPHAS = {0f, 0.07f, 0.28f, 0.60f, 1f};

    /**
     * One visual style's glow this frame (index = VisualStyle ordinal). Everything is pre-allocated: a frame with
     * several styles active allocates nothing and rebuilds no gradient unless that style's colour or size changed.
     */
    private static final int STYLES = VisualStyle.values().length;
    private final float[] slotR = new float[STYLES];
    private final float[] slotG = new float[STYLES];
    private final float[] slotB = new float[STYLES];
    private final float[] slotWeight = new float[STYLES];
    private final float[] slotMax = new float[STYLES];
    private final int[] slotStrongest = new int[STYLES];
    private final int[] slotRgb = new int[STYLES];
    private final float[] smoothedSlotAlpha = new float[STYLES];
    private final GlowPaints[] slotPaints = new GlowPaints[STYLES];
    private final int[] edgeOwner = new int[4];
    private int paintBuilds = 0;

    /** Cached vignette paints of one style, rebuilt only when its colour or geometry changes. */
    private static final class GlowPaints
    {
        int keyRgb = -1;
        int keyW = -1;
        int keyH = -1;
        int keyEdgeW = -1;
        int keyEdgeH = -1;
        final java.awt.Paint[] paints = new java.awt.Paint[8];
        Color tint;
    }

    /** How many times the gradient paints were (re)built; for tests guarding against per-frame rebuilds. */
    public int getPaintBuilds()
    {
        return paintBuilds;
    }

    // Precomputed deterministic shower lanes (zero allocation)
    private final float[] showerPhaseOffset = new float[36];
    private final float[] showerSpeedScale = new float[36];

    public ProceduralRenderer()
    {
        for (int i = 0; i < STYLES; i++)
        {
            slotPaints[i] = new GlowPaints();
            slotRgb[i] = -1;
        }
        Random rng = new Random(0x5EEDBEEFL);
        for (int i = 0; i < showerPhaseOffset.length; i++)
        {
            showerPhaseOffset[i] = rng.nextFloat();
            showerSpeedScale[i] = 0.85f + rng.nextFloat() * 0.35f;
        }
    }

    public void clearCaches()
    {
        spriteIconCache.clear();
        itemIconCache.clear();
        resetGlow();
        lastGlowFrameNanos = 0L;
        showerStartNanos = 0L;
        for (GlowPaints gp : slotPaints)
        {
            gp.keyRgb = -1;
        }
    }

    public void render(
        Graphics2D g,
        int width,
        int height,
        long nowNanos,
        SettingsSnapshot config,
        Client client,
        SpriteManager spriteManager,
        ItemManager itemManager,
        float[] statusSeverities,
        int[] statusColors,
        int glowExtraCount,
        int[] glowExtraColors,
        float[] glowExtraPulse,
        float[] glowExtraFade,
        int showerSpriteCount,
        int[] showerSpriteIds,
        int[] showerSpriteColors,
        int showerItemCount,
        int[] showerItemIds,
        int[] showerItemColors
    )
    {
        render(g, width, height, nowNanos, config, client, spriteManager, itemManager, statusSeverities, statusColors,
            glowExtraCount, glowExtraColors, glowExtraPulse, glowExtraFade, null,
            showerSpriteCount, showerSpriteIds, showerSpriteColors, null,
            showerItemCount, showerItemIds, showerItemColors, null);
    }

    /**
     * @param glowExtraStyles   VisualStyle ordinal per extra glow (null = all shared look); statuses use the snapshot's
     * @param showerSpriteStyles VisualStyle ordinal per shower sprite (null = shared look)
     * @param showerItemStyles  VisualStyle ordinal per shower item (null = shared look)
     */
    public void render(
        Graphics2D g,
        int width,
        int height,
        long nowNanos,
        SettingsSnapshot config,
        Client client,
        SpriteManager spriteManager,
        ItemManager itemManager,
        float[] statusSeverities,
        int[] statusColors,
        int glowExtraCount,
        int[] glowExtraColors,
        float[] glowExtraPulse,
        float[] glowExtraFade,
        int[] glowExtraStyles,
        int showerSpriteCount,
        int[] showerSpriteIds,
        int[] showerSpriteColors,
        int[] showerSpriteStyles,
        int showerItemCount,
        int[] showerItemIds,
        int[] showerItemColors,
        int[] showerItemStyles
    )
    {
        if (width <= 0 || height <= 0)
        {
            return;
        }

        // Check if anything is active at all before touching Graphics2D
        boolean anyStatus = false;
        for (int i = 0; i < statusSeverities.length; i++)
        {
            if (statusSeverities[i] > 0.005f)
            {
                anyStatus = true;
                break;
            }
        }
        boolean anySpellGlow = glowExtraCount > 0;
        boolean anySpellIcons = showerSpriteCount > 0 || showerItemCount > 0;
        if (!anyStatus && !anySpellGlow && !anySpellIcons)
        {
            resetGlow();
            lastGlowFrameNanos = nowNanos;
            showerStartNanos = 0L;
            return;
        }
        if (anySpellIcons && showerStartNanos == 0L)
        {
            showerStartNanos = nowNanos;
        }
        else if (!anySpellIcons)
        {
            showerStartNanos = 0L;
        }

        double timeSec = nowNanos / 1_000_000_000.0;

        // 1. Screen-edge vignette per visual style (cached gradient paints: 4 edges + 4 rounded corners each)
        if (anyStatus || anySpellGlow)
        {
            drawAcceleratedEdgeGlow(g, width, height, nowNanos, config, statusSeverities, statusColors,
                glowExtraCount, glowExtraColors, glowExtraPulse, glowExtraFade, glowExtraStyles);
        }

        Composite oldComposite = g.getComposite();

        // 2. Centralized icon shower for all warnings (spells, heart, buffs, prayer, freeze, low HP, low run)
        if (anySpellIcons)
        {
            drawUnifiedIconShower(
                g, width, height, nowNanos, timeSec, config, client, spriteManager, itemManager,
                showerSpriteCount, showerSpriteIds, showerSpriteColors, showerSpriteStyles,
                showerItemCount, showerItemIds, showerItemColors, showerItemStyles
            );
        }

        g.setComposite(oldComposite);
    }

    private void resetGlow()
    {
        java.util.Arrays.fill(smoothedSlotAlpha, 0f);
    }

    private void accumulate(int k, int argb, float colourWeight, float strength)
    {
        slotR[k] += ((argb >>> 16) & 0xFF) * colourWeight;
        slotG[k] += ((argb >>> 8) & 0xFF) * colourWeight;
        slotB[k] += (argb & 0xFF) * colourWeight;
        slotWeight[k] += colourWeight;
        if (strength > slotMax[k])
        {
            slotMax[k] = strength;
            slotStrongest[k] = argb;
        }
    }

    /**
     * Each active visual style gets its own colour and fade. Where styles share an edge, the strongest one owns it,
     * so every edge is filled at most once per frame (the same pixel cost as a single style) and two styles never
     * stack into a double-bright band. The screen tint, if any, is drawn once in the strongest style's colour.
     */
    private void drawAcceleratedEdgeGlow(
        Graphics2D g,
        int width,
        int height,
        long nowNanos,
        SettingsSnapshot config,
        float[] statusSeverities,
        int[] statusColors,
        int extraCount,
        int[] extraColors,
        float[] extraPulse,
        float[] extraFade,
        int[] extraStyles
    )
    {
        java.util.Arrays.fill(slotR, 0f);
        java.util.Arrays.fill(slotG, 0f);
        java.util.Arrays.fill(slotB, 0f);
        java.util.Arrays.fill(slotWeight, 0f);
        java.util.Arrays.fill(slotMax, 0f);

        for (int i = 0; i < statusSeverities.length; i++)
        {
            float sev = statusSeverities[i];
            if (sev <= 0.005f)
            {
                continue;
            }
            int k = config.statusStyle(i);
            int argb = statusColors[i];
            float a = ((argb >>> 24) & 0xFF) / 255f;
            // Colour mix uses the steady weight; the heartbeat only animates strength, so the blended colour (and the
            // cached gradient paints built from it) stays constant while a warning pulses
            float cw = sev * a;
            float beat = config.statusPulse(i);
            float w = beat > 0f ? cw * Motion.heartbeat(nowNanos, beat, config.profile(k).motion) : cw;
            accumulate(k, argb, cw, w);
        }

        // Spells and item reminders breathe gently by default, or use the heartbeat when one is set
        for (int i = 0; i < extraCount; i++)
        {
            int k = extraStyles != null ? extraStyles[i] : 0;
            float motion = config.profile(k).motion;
            int argb = extraColors[i];
            float a = ((argb >>> 24) & 0xFF) / 255f;
            float cw = 0.65f * a * extraFade[i];
            float w = cw * (extraPulse[i] > 0f
                ? Motion.heartbeat(nowNanos, extraPulse[i], motion)
                : Motion.pulse(nowNanos, Motion.SCREEN_PULSE_RATE, 0.72f, motion));
            accumulate(k, argb, cw, w);
        }

        float dtSec = lastGlowFrameNanos == 0L ? 0.08f
            : Math.max(0.001f, Math.min(0.25f, (nowNanos - lastGlowFrameNanos) / 1_000_000_000f));
        lastGlowFrameNanos = nowNanos;
        float blend = 1f - (float) Math.exp(-dtSec * 4.5f);

        int dominant = -1;
        for (int k = 0; k < STYLES; k++)
        {
            VisualProfile p = config.profile(k);
            float target = 0f;
            if (slotWeight[k] > 0.001f && p.intensity > 0.01f)
            {
                int argb = slotStrongest[k];
                if (config.glowColorMode == GlowColorMode.STRONGEST)
                {
                    slotRgb[k] = argb & 0xFFFFFF;
                }
                else
                {
                    // Quantised to 5 bits per channel (invisible at vignette alpha) so float drift never rebuilds paints
                    int r = Math.min(255, Math.round(slotR[k] / slotWeight[k])) & 0xF8;
                    int gr = Math.min(255, Math.round(slotG[k] / slotWeight[k])) & 0xF8;
                    int b = Math.min(255, Math.round(slotB[k] / slotWeight[k])) & 0xF8;
                    slotRgb[k] = (r << 16) | (gr << 8) | b;
                }
                target = Math.min(0.92f, slotMax[k] * p.intensity);
            }
            else if (slotWeight[k] > 0f)
            {
                // Only fully transparent colours: drop instead of freezing the last alpha
                smoothedSlotAlpha[k] = 0f;
            }
            // A style that just ended fades out in its last colour
            smoothedSlotAlpha[k] += (target - smoothedSlotAlpha[k]) * blend;
            if (smoothedSlotAlpha[k] <= 0.005f || slotRgb[k] < 0)
            {
                smoothedSlotAlpha[k] = target <= 0f ? 0f : smoothedSlotAlpha[k];
                continue;
            }
            if (dominant < 0 || smoothedSlotAlpha[k] > smoothedSlotAlpha[dominant])
            {
                dominant = k;
            }
        }
        if (dominant < 0)
        {
            return;
        }

        // Edge ownership: top, bottom, left, right
        for (int e = 0; e < 4; e++)
        {
            int owner = -1;
            float best = 0.005f;
            for (int k = 0; k < STYLES; k++)
            {
                if (smoothedSlotAlpha[k] > best && slotRgb[k] >= 0 && hasEdge(config.profile(k), e))
                {
                    best = smoothedSlotAlpha[k];
                    owner = k;
                }
            }
            edgeOwner[e] = owner;
        }

        java.awt.Paint oldPaint = g.getPaint();
        Composite oldComposite = g.getComposite();
        for (int k = 0; k < STYLES; k++)
        {
            boolean top = edgeOwner[0] == k;
            boolean bottom = edgeOwner[1] == k;
            boolean left = edgeOwner[2] == k;
            boolean right = edgeOwner[3] == k;
            if (!top && !bottom && !left && !right)
            {
                continue;
            }
            GlowPaints gp = paintsFor(k, config.profile(k), width, height);
            g.setComposite(quantizedAlpha(smoothedSlotAlpha[k]));
            drawEdges(g, gp.paints, width, height, gp.keyEdgeW, gp.keyEdgeH, top, bottom, left, right);
        }

        VisualProfile dp = config.profile(dominant);
        float tint = dp.tint * 0.22f;
        if (tint > 0.01f)
        {
            GlowPaints gp = paintsFor(dominant, dp, width, height);
            g.setComposite(quantizedAlpha(Math.min(90f / 255f, tint * smoothedSlotAlpha[dominant])));
            g.setColor(gp.tint);
            g.fillRect(0, 0, width, height);
        }

        g.setComposite(oldComposite);
        g.setPaint(oldPaint);
    }

    private static boolean hasEdge(VisualProfile p, int edge)
    {
        switch (edge)
        {
            case 0: return p.top;
            case 1: return p.bottom;
            case 2: return p.left;
            default: return p.right;
        }
    }

    private GlowPaints paintsFor(int k, VisualProfile p, int width, int height)
    {
        float depth = Math.max(0.10f, p.depth) * 0.5f;
        int edgeW = Math.max(12, Math.min(width / 2, Math.round(width * depth)));
        int edgeH = Math.max(12, Math.min(height / 2, Math.round(height * depth)));
        GlowPaints gp = slotPaints[k];
        ensureGlowPaints(gp, slotRgb[k], width, height, edgeW, edgeH);
        return gp;
    }

    /**
     * Edge strips plus corners for any combination of enabled edges. Where both edges of a corner are on, the
     * rounded radial corner joins them; where only one is on, that edge's strip continues straight through the
     * corner (its gradient already spans it), so a single edge never ends in a hard cut. Nothing is drawn for
     * disabled edges, and the cached paints are reused unchanged.
     */
    private static void drawEdges(Graphics2D g, java.awt.Paint[] glowPaints, int width, int height, int edgeW, int edgeH,
        boolean top, boolean bottom, boolean left, boolean right)
    {
        int midW = width - 2 * edgeW;
        int midH = height - 2 * edgeH;
        if (midW > 0)
        {
            if (top)
            {
                g.setPaint(glowPaints[0]);
                g.fillRect(edgeW, 0, midW, edgeH);
            }
            if (bottom)
            {
                g.setPaint(glowPaints[1]);
                g.fillRect(edgeW, height - edgeH, midW, edgeH);
            }
        }
        if (midH > 0)
        {
            if (left)
            {
                g.setPaint(glowPaints[2]);
                g.fillRect(0, edgeH, edgeW, midH);
            }
            if (right)
            {
                g.setPaint(glowPaints[3]);
                g.fillRect(width - edgeW, edgeH, edgeW, midH);
            }
        }
        drawCorner(g, 0, 0, edgeW, edgeH, top, left, glowPaints[4], glowPaints[0], glowPaints[2]);
        drawCorner(g, width - edgeW, 0, edgeW, edgeH, top, right, glowPaints[5], glowPaints[0], glowPaints[3]);
        drawCorner(g, 0, height - edgeH, edgeW, edgeH, bottom, left, glowPaints[6], glowPaints[1], glowPaints[2]);
        drawCorner(g, width - edgeW, height - edgeH, edgeW, edgeH, bottom, right, glowPaints[7], glowPaints[1], glowPaints[3]);
    }

    private static void drawCorner(Graphics2D g, int x, int y, int w, int h, boolean horizontalEdge, boolean verticalEdge,
        java.awt.Paint corner, java.awt.Paint horizontalStrip, java.awt.Paint verticalStrip)
    {
        java.awt.Paint paint = pickCornerPaint(horizontalEdge, verticalEdge, corner, horizontalStrip, verticalStrip);
        if (paint != null)
        {
            g.setPaint(paint);
            g.fillRect(x, y, w, h);
        }
    }

    /** Which paint fills a corner: radial when both edges are on, the single edge's strip, or nothing. */
    public static <P> P pickCornerPaint(boolean horizontalEdge, boolean verticalEdge, P corner, P horizontalStrip, P verticalStrip)
    {
        if (horizontalEdge && verticalEdge)
        {
            return corner;
        }
        if (horizontalEdge)
        {
            return horizontalStrip;
        }
        return verticalEdge ? verticalStrip : null;
    }

    private void ensureGlowPaints(GlowPaints gp, int rgb, int width, int height, int edgeW, int edgeH)
    {
        if (rgb == gp.keyRgb && width == gp.keyW && height == gp.keyH && edgeW == gp.keyEdgeW && edgeH == gp.keyEdgeH)
        {
            return;
        }
        gp.keyRgb = rgb;
        paintBuilds++;
        gp.keyW = width;
        gp.keyH = height;
        gp.keyEdgeW = edgeW;
        gp.keyEdgeH = edgeH;

        Color[] colors = new Color[GLOW_FRACTIONS.length];
        for (int i = 0; i < colors.length; i++)
        {
            colors[i] = new Color((rgb >>> 16) & 0xFF, (rgb >>> 8) & 0xFF, rgb & 0xFF, Math.round(GLOW_ALPHAS[i] * 255f));
        }
        gp.tint = new Color(rgb);
        java.awt.Paint[] glowPaints = gp.paints;

        // Linear edges: start = inner boundary (clear), end = screen edge (full)
        glowPaints[0] = new LinearGradientPaint(new Point2D.Float(0, edgeH), new Point2D.Float(0, 0), GLOW_FRACTIONS, colors);
        glowPaints[1] = new LinearGradientPaint(new Point2D.Float(0, height - edgeH), new Point2D.Float(0, height), GLOW_FRACTIONS, colors);
        glowPaints[2] = new LinearGradientPaint(new Point2D.Float(edgeW, 0), new Point2D.Float(0, 0), GLOW_FRACTIONS, colors);
        glowPaints[3] = new LinearGradientPaint(new Point2D.Float(width - edgeW, 0), new Point2D.Float(width, 0), GLOW_FRACTIONS, colors);

        // Elliptical corners centred on each inner corner; identical falloff so edges and corners meet seamlessly
        glowPaints[4] = cornerPaint(edgeW, edgeH, edgeW, edgeH, colors);
        glowPaints[5] = cornerPaint(width - edgeW, edgeH, edgeW, edgeH, colors);
        glowPaints[6] = cornerPaint(edgeW, height - edgeH, edgeW, edgeH, colors);
        glowPaints[7] = cornerPaint(width - edgeW, height - edgeH, edgeW, edgeH, colors);
    }

    private static java.awt.Paint cornerPaint(float cx, float cy, float rx, float ry, Color[] colors)
    {
        AffineTransform t = new AffineTransform();
        t.translate(cx, cy);
        t.scale(rx, ry);
        return new RadialGradientPaint(
            new Point2D.Float(0f, 0f),
            1f,
            new Point2D.Float(0f, 0f),
            GLOW_FRACTIONS,
            colors,
            MultipleGradientPaint.CycleMethod.NO_CYCLE,
            MultipleGradientPaint.ColorSpaceType.SRGB,
            t
        );
    }

    private void drawUnifiedIconShower(
        Graphics2D g,
        int width,
        int height,
        long nowNanos,
        double timeSec,
        SettingsSnapshot config,
        Client client,
        SpriteManager spriteManager,
        ItemManager itemManager,
        int readySpriteCount,
        int[] readySpriteIds,
        int[] readySpriteColors,
        int[] readySpriteStyles,
        int readyItemCount,
        int[] readyItemIds,
        int[] readyItemColors,
        int[] readyItemStyles
    )
    {
        int totalSources = readySpriteCount + readyItemCount;
        if (totalSources <= 0)
        {
            return;
        }

        // Icon count and size stay shared (one budget, one icon cache); movement and opacity follow each icon's style
        int maxIcons = Math.min(showerPhaseOffset.length, config.showerMaxIcons);
        int iconSize = config.showerIconSize;

        float showerFade = Motion.fadeIn(showerStartNanos, nowNanos); // the whole shower eases in over one tick
        for (int i = 0; i < maxIcons; i++)
        {
            int slot = i % totalSources;
            BufferedImage icon;
            int fallbackColor;
            int style;
            if (slot < readySpriteCount)
            {
                icon = getScaledSpriteIcon(readySpriteIds[slot], client, spriteManager, iconSize);
                fallbackColor = readySpriteColors[slot];
                style = readySpriteStyles != null ? readySpriteStyles[slot] : 0;
            }
            else
            {
                int itemIdx = slot - readySpriteCount;
                icon = getScaledItemIcon(readyItemIds[itemIdx], itemManager, iconSize);
                fallbackColor = readyItemColors[itemIdx];
                style = readyItemStyles != null ? readyItemStyles[itemIdx] : 0;
            }
            VisualProfile p = config.profile(style);
            float baseSpeed = 0.22f * p.showerSpeed;
            float screenFrac = p.showerFraction;
            ShowerStartLocation startLoc = p.showerStart;

            double elapsedSec = showerStartNanos > 0L ? Math.max(0.0, (nowNanos - showerStartNanos) / 1_000_000_000.0) : timeSec;
            double rawCycle = elapsedSec * baseSpeed * showerSpeedScale[i] - showerPhaseOffset[i];
            if (rawCycle < 0.0)
            {
                continue;
            }
            double cycle = rawCycle % 1.0;
            float fade = (float) Math.sin(cycle * Math.PI);
            float alpha = Math.max(0f, Math.min(1f, fade * p.showerOpacity * showerFade));
            if (alpha <= 0.03f)
            {
                continue;
            }

            int x;
            int y;
            int sway = (int) Math.round(Math.sin(timeSec * Motion.SWAY_RATE + i) * 8.0 * p.motion);
            if (startLoc == ShowerStartLocation.BOTTOM)
            {
                double travel = height * screenFrac;
                x = Math.round(((i + 0.5f) / maxIcons) * width) - (iconSize / 2) + sway;
                y = height - (int) Math.round(cycle * travel);
            }
            else if (startLoc == ShowerStartLocation.LEFT)
            {
                double travel = width * screenFrac;
                x = (int) Math.round(cycle * travel) - iconSize;
                y = Math.round(((i + 0.5f) / maxIcons) * height) - (iconSize / 2) + sway;
            }
            else if (startLoc == ShowerStartLocation.RIGHT)
            {
                double travel = width * screenFrac;
                x = width - (int) Math.round(cycle * travel);
                y = Math.round(((i + 0.5f) / maxIcons) * height) - (iconSize / 2) + sway;
            }
            else
            {
                double travel = height * screenFrac;
                x = Math.round(((i + 0.5f) / maxIcons) * width) - (iconSize / 2) + sway;
                y = (int) Math.round(cycle * travel) - iconSize;
            }

            g.setComposite(quantizedAlpha(alpha));
            if (icon != null)
            {
                g.drawImage(icon, x, y, null);
            }
            else
            {
                g.setColor(new Color(fallbackColor, true));
                g.fillOval(x, y, iconSize, iconSize);
                g.setColor(Color.WHITE);
                g.drawOval(x, y, iconSize, iconSize);
            }
        }
    }

    private BufferedImage getScaledSpriteIcon(int spriteId, Client client, SpriteManager spriteManager, int targetSize)
    {
        if (cachedIconSize != targetSize)
        {
            cachedIconSize = targetSize;
            spriteIconCache.clear();
            itemIconCache.clear();
        }
        BufferedImage cached = spriteIconCache.get(spriteId);
        if (cached != null)
        {
            return cached;
        }
        if (spriteManager == null || spriteId <= 0)
        {
            return null;
        }
        BufferedImage raw = null;
        if (client != null && client.getSpriteOverrides() != null)
        {
            SpritePixels override = client.getSpriteOverrides().get(spriteId);
            if (override != null)
            {
                raw = override.toBufferedImage();
            }
        }
        if (raw == null && spriteManager != null)
        {
            raw = spriteManager.getSprite(spriteId, 0);
        }
        if (raw == null)
        {
            return null;
        }
        BufferedImage scaled = scalePreserveAspect(raw, targetSize);
        spriteIconCache.put(spriteId, scaled);
        return scaled;
    }

    private BufferedImage getScaledItemIcon(int itemId, ItemManager itemManager, int targetSize)
    {
        if (cachedIconSize != targetSize)
        {
            cachedIconSize = targetSize;
            spriteIconCache.clear();
            itemIconCache.clear();
        }
        BufferedImage cached = itemIconCache.get(itemId);
        if (cached != null)
        {
            return cached;
        }
        if (itemManager == null || itemId <= 0)
        {
            return null;
        }
        BufferedImage raw = itemManager.getImage(itemId);
        if (raw == null || !ItemOutlineCache.hasVisiblePixels(raw))
        {
            // ItemManager images load asynchronously; never cache a still-blank placeholder
            return null;
        }
        BufferedImage scaled = scalePreserveAspect(raw, targetSize);
        itemIconCache.put(itemId, scaled);
        return scaled;
    }

    public static BufferedImage scalePreserveAspect(BufferedImage raw, int targetSize)
    {
        int rw = raw.getWidth();
        int rh = raw.getHeight();
        int minX = rw, minY = rh, maxX = -1, maxY = -1;
        for (int y = 0; y < rh; y++)
        {
            for (int x = 0; x < rw; x++)
            {
                if (((raw.getRGB(x, y) >>> 24) & 0xFF) > 20)
                {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX || maxY < minY)
        {
            minX = 0; minY = 0; maxX = rw - 1; maxY = rh - 1;
        }
        int cropW = (maxX - minX) + 1;
        int cropH = (maxY - minY) + 1;
        int inner = Math.max(4, targetSize - 2);

        BufferedImage scaled = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = scaled.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double scale = Math.min((double) inner / cropW, (double) inner / cropH);
        int dw = Math.max(1, (int) Math.round(cropW * scale));
        int dh = Math.max(1, (int) Math.round(cropH * scale));
        int dx = (targetSize - dw) / 2;
        int dy = (targetSize - dh) / 2;
        sg.drawImage(raw, dx, dy, dx + dw, dy + dh, minX, minY, maxX + 1, maxY + 1, null);
        sg.dispose();
        return scaled;
    }

    public static AlphaComposite quantizedAlpha(float alpha)
    {
        int idx = Math.max(0, Math.min(ALPHA_STEPS, Math.round(alpha * ALPHA_STEPS)));
        return ALPHA_LUT[idx];
    }
}
