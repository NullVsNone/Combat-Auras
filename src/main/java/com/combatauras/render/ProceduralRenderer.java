package com.combatauras.render;

import com.combatauras.settings.SettingsSnapshot;
import com.combatauras.model.ShowerStartLocation;
import com.combatauras.model.StatusEffect;
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
import java.util.Map;
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
    private final Map<Integer, BufferedImage> spriteIconCache = new LruCache<>(64);
    private final Map<Integer, BufferedImage> itemIconCache = new LruCache<>(64);
    private int cachedIconSize = 0;
    private float smoothedGlowAlpha = 0f;
    private long lastGlowFrameNanos = 0L;
    private long showerStartNanos = 0L;

    // Eased falloff shared by edges and corners: 0 = innermost (clear), 1 = screen edge (full colour)
    private static final float[] GLOW_FRACTIONS = {0f, 0.35f, 0.65f, 0.85f, 1f};
    private static final float[] GLOW_ALPHAS = {0f, 0.07f, 0.28f, 0.60f, 1f};

    // Cached vignette paints, rebuilt only when colour or geometry changes
    private int glowKeyRgb = -1;
    private int glowKeyW = -1;
    private int glowKeyH = -1;
    private int glowKeyEdgeW = -1;
    private int glowKeyEdgeH = -1;
    private final java.awt.Paint[] glowPaints = new java.awt.Paint[8];
    private int paintBuilds = 0;

    /** How many times the gradient paints were (re)built; for tests guarding against per-frame rebuilds. */
    public int getPaintBuilds()
    {
        return paintBuilds;
    }
    private Color tintColor = null;

    // Precomputed deterministic shower lanes (zero allocation)
    private final float[] showerPhaseOffset = new float[36];
    private final float[] showerSpeedScale = new float[36];

    public ProceduralRenderer()
    {
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
        smoothedGlowAlpha = 0f;
        lastGlowFrameNanos = 0L;
        showerStartNanos = 0L;
        glowKeyRgb = -1;
        tintColor = null;
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
        int showerSpriteCount,
        int[] showerSpriteIds,
        int[] showerSpriteColors,
        int showerItemCount,
        int[] showerItemIds,
        int[] showerItemColors
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
            smoothedGlowAlpha = 0f;
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

        float masterIntensity = config.effectIntensity;
        float motion = config.motionStrength;
        double timeSec = nowNanos / 1_000_000_000.0;

        // 1. Blended screen-edge vignette (cached gradient paints: 4 edges + 4 rounded corners)
        if (masterIntensity > 0.01f && (anyStatus || anySpellGlow))
        {
            drawAcceleratedEdgeGlow(
                g,
                width,
                height,
                nowNanos,
                config,
                masterIntensity,
                motion,
                statusSeverities,
                statusColors,
                glowExtraCount,
                glowExtraColors
            );
        }

        Composite oldComposite = g.getComposite();

        // 2. Centralized icon shower for all warnings (spells, heart, buffs, prayer, freeze, low HP, low run)
        if (anySpellIcons)
        {
            drawUnifiedIconShower(
                g, width, height, nowNanos, timeSec, config, client, spriteManager, itemManager,
                showerSpriteCount, showerSpriteIds, showerSpriteColors,
                showerItemCount, showerItemIds, showerItemColors
            );
        }

        g.setComposite(oldComposite);
    }

    private void drawAcceleratedEdgeGlow(
        Graphics2D g,
        int width,
        int height,
        long nowNanos,
        SettingsSnapshot config,
        float masterIntensity,
        float motion,
        float[] statusSeverities,
        int[] statusColors,
        int extraCount,
        int[] extraColors
    )
    {
        float rAcc = 0f;
        float gAcc = 0f;
        float bAcc = 0f;
        float weightSum = 0f;
        float maxStrength = 0f;
        int strongestArgb = 0;

        float pulseHealth = 1f;
        if (statusSeverities[StatusEffect.LOW_HEALTH.ordinal()] > 0f && config.healthPulseStrength > 0f)
        {
            pulseHealth = Motion.pulse(nowNanos, Motion.HEARTBEAT_RATE, 0.65f, config.healthPulseStrength * motion);
        }

        for (int i = 0; i < statusSeverities.length; i++)
        {
            float sev = statusSeverities[i];
            if (sev <= 0.005f)
            {
                continue;
            }
            int argb = statusColors[i];
            float a = ((argb >>> 24) & 0xFF) / 255f;
            // Colour mix uses the steady weight; the pulse only animates strength, so the blended colour (and the
            // cached gradient paints built from it) stays constant while low HP pulses
            float cw = sev * a;
            float w = i == StatusEffect.LOW_HEALTH.ordinal() ? cw * pulseHealth : cw;
            rAcc += ((argb >>> 16) & 0xFF) * cw;
            gAcc += ((argb >>> 8) & 0xFF) * cw;
            bAcc += (argb & 0xFF) * cw;
            weightSum += cw;
            if (w > maxStrength)
            {
                maxStrength = w;
                strongestArgb = argb;
            }
        }

        if (extraCount > 0)
        {
            float spellPulse = Motion.pulse(nowNanos, Motion.SCREEN_PULSE_RATE, 0.72f, motion);
            for (int i = 0; i < extraCount; i++)
            {
                int argb = extraColors[i];
                float a = ((argb >>> 24) & 0xFF) / 255f;
                float cw = 0.65f * a;
                float w = cw * spellPulse;
                rAcc += ((argb >>> 16) & 0xFF) * cw;
                gAcc += ((argb >>> 8) & 0xFF) * cw;
                bAcc += (argb & 0xFF) * cw;
                weightSum += cw;
                if (w > maxStrength)
                {
                    maxStrength = w;
                    strongestArgb = argb;
                }
            }
        }

        if (weightSum <= 0.001f)
        {
            // Only fully transparent colours are active: fade out instead of freezing the last alpha
            smoothedGlowAlpha = 0f;
            lastGlowFrameNanos = nowNanos;
            return;
        }

        int r;
        int gr;
        int b;
        if (config.glowColorMode == GlowColorMode.STRONGEST)
        {
            r = (strongestArgb >>> 16) & 0xFF;
            gr = (strongestArgb >>> 8) & 0xFF;
            b = strongestArgb & 0xFF;
        }
        else
        {
            // Quantised to 5 bits per channel (invisible at vignette alpha) so float drift never rebuilds the paints
            r = Math.min(255, Math.round(rAcc / weightSum)) & 0xF8;
            gr = Math.min(255, Math.round(gAcc / weightSum)) & 0xF8;
            b = Math.min(255, Math.round(bAcc / weightSum)) & 0xF8;
        }

        float depth = Math.max(0.10f, config.edgeDepth) * 0.5f;
        float targetAlpha = Math.min(0.92f, maxStrength * masterIntensity);
        float dtSec;
        if (lastGlowFrameNanos == 0L)
        {
            dtSec = 0.08f;
        }
        else
        {
            dtSec = Math.max(0.001f, Math.min(0.25f, (nowNanos - lastGlowFrameNanos) / 1_000_000_000f));
        }
        lastGlowFrameNanos = nowNanos;
        float blend = 1f - (float) Math.exp(-dtSec * 4.5f);
        smoothedGlowAlpha += (targetAlpha - smoothedGlowAlpha) * blend;
        float globalAlpha = smoothedGlowAlpha;
        if (globalAlpha <= 0.005f)
        {
            return;
        }

        int edgeW = Math.max(12, Math.min(width / 2, Math.round(width * depth)));
        int edgeH = Math.max(12, Math.min(height / 2, Math.round(height * depth)));
        int rgb = (r << 16) | (gr << 8) | b;
        ensureGlowPaints(rgb, width, height, edgeW, edgeH);

        java.awt.Paint oldPaint = g.getPaint();
        Composite oldComposite = g.getComposite();
        g.setComposite(quantizedAlpha(globalAlpha));

        drawEdges(g, width, height, edgeW, edgeH, config.glowTop, config.glowBottom, config.glowLeft, config.glowRight);

        float tint = config.screenTint * 0.22f;
        if (tint > 0.01f)
        {
            g.setComposite(quantizedAlpha(Math.min(90f / 255f, tint * globalAlpha)));
            g.setColor(tintColor);
            g.fillRect(0, 0, width, height);
        }

        g.setComposite(oldComposite);
        g.setPaint(oldPaint);
    }

    /**
     * Edge strips plus corners for any combination of enabled edges. Where both edges of a corner are on, the
     * rounded radial corner joins them; where only one is on, that edge's strip continues straight through the
     * corner (its gradient already spans it), so a single edge never ends in a hard cut. Nothing is drawn for
     * disabled edges, and the cached paints are reused unchanged.
     */
    private void drawEdges(Graphics2D g, int width, int height, int edgeW, int edgeH,
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

    private void ensureGlowPaints(int rgb, int width, int height, int edgeW, int edgeH)
    {
        if (rgb == glowKeyRgb && width == glowKeyW && height == glowKeyH && edgeW == glowKeyEdgeW && edgeH == glowKeyEdgeH)
        {
            return;
        }
        glowKeyRgb = rgb;
        paintBuilds++;
        glowKeyW = width;
        glowKeyH = height;
        glowKeyEdgeW = edgeW;
        glowKeyEdgeH = edgeH;

        Color[] colors = new Color[GLOW_FRACTIONS.length];
        for (int i = 0; i < colors.length; i++)
        {
            colors[i] = new Color((rgb >>> 16) & 0xFF, (rgb >>> 8) & 0xFF, rgb & 0xFF, Math.round(GLOW_ALPHAS[i] * 255f));
        }
        tintColor = new Color(rgb);

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
        int readyItemCount,
        int[] readyItemIds,
        int[] readyItemColors
    )
    {
        int totalSources = readySpriteCount + readyItemCount;
        if (totalSources <= 0)
        {
            return;
        }

        int maxIcons = Math.min(showerPhaseOffset.length, config.showerMaxIcons);
        int iconSize = config.showerIconSize;
        float speedMult = config.showerSpeed;
        float baseSpeed = 0.22f * speedMult;
        float screenFrac = config.showerScreenFraction;
        ShowerStartLocation startLoc = config.showerStartLocation;

        for (int i = 0; i < maxIcons; i++)
        {
            int slot = i % totalSources;
            BufferedImage icon;
            int fallbackColor;
            if (slot < readySpriteCount)
            {
                icon = getScaledSpriteIcon(readySpriteIds[slot], client, spriteManager, iconSize);
                fallbackColor = readySpriteColors[slot];
            }
            else
            {
                int itemIdx = slot - readySpriteCount;
                icon = getScaledItemIcon(readyItemIds[itemIdx], itemManager, iconSize);
                fallbackColor = readyItemColors[itemIdx];
            }

            double elapsedSec = showerStartNanos > 0L ? Math.max(0.0, (nowNanos - showerStartNanos) / 1_000_000_000.0) : timeSec;
            double rawCycle = elapsedSec * baseSpeed * showerSpeedScale[i] - showerPhaseOffset[i];
            if (rawCycle < 0.0)
            {
                continue;
            }
            double cycle = rawCycle % 1.0;
            float fade = (float) Math.sin(cycle * Math.PI);
            float alpha = Math.max(0f, Math.min(1f, fade * config.showerOpacity));
            if (alpha <= 0.03f)
            {
                continue;
            }

            int x;
            int y;
            int sway = (int) Math.round(Math.sin(timeSec * Motion.SWAY_RATE + i) * 8.0 * config.motionStrength);
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
