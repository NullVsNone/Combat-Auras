package com.immersivecombatoverlays.render;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.ShowerStartLocation;
import com.immersivecombatoverlays.model.StatusEffect;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import com.immersivecombatoverlays.tracker.CombatSpellTracker;
import net.runelite.api.Client;
import net.runelite.api.SpritePixels;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;

/**
 * Zero-allocation procedural screen overlay renderer.
 *
 * Key optimizations over health-status-overlays:
 * 1. Zero per-frame heap allocations (primitive arrays, quantized AlphaComposite LUT, cached scaled icons).
 * 2. Configurable downsample step + optional FPS cap for the offscreen vignette buffer.
 * 3. Smooth multi-color blending across all active status effects AND all expired combat spells.
 * 4. Split/interleaved showering spell icons fetched via SpriteManager (preserving Resource Pack sprites).
 */
public class ProceduralRenderer
{
    private static final int ALPHA_STEPS = 32;
    private static final AlphaComposite[] ALPHA_LUT = new AlphaComposite[ALPHA_STEPS + 1];

    static
    {
        for (int i = 0; i <= ALPHA_STEPS; i++)
        {
            ALPHA_LUT[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i / (float) ALPHA_STEPS);
        }
    }

    // Unified pre-scaled icon caches (respects Resource Packs)
    private final Map<Integer, BufferedImage> spriteIconCache = new HashMap<>();
    private final Map<Integer, BufferedImage> itemIconCache = new HashMap<>();
    private int cachedIconSize = 0;
    private float smoothedGlowAlpha = 0f;
    private long lastGlowFrameNanos = 0L;
    private long showerStartNanos = 0L;

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
    }

    public void render(
        Graphics2D g,
        int width,
        int height,
        long nowNanos,
        ImmersiveCombatOverlaysConfig config,
        Client client,
        SpriteManager spriteManager,
        ItemManager itemManager,
        CombatSpellTracker spellTracker,
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

        float masterIntensity = config.effectIntensity() / 100f;
        float motion = config.motionStrength() / 100f;
        double timeSec = nowNanos / 1_000_000_000.0;

        // 1. Hardware-accelerated blended screen-edge vignette (4 GradientPaint strips, zero CPU pixel loops)
        if (masterIntensity > 0.01f && (anyStatus || anySpellGlow))
        {
            drawAcceleratedEdgeGlow(
                g,
                width,
                height,
                nowNanos,
                timeSec,
                config,
                masterIntensity,
                motion,
                statusSeverities,
                statusColors,
                glowExtraCount,
                glowExtraColors,
                0,
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
        double timeSec,
        ImmersiveCombatOverlaysConfig config,
        float masterIntensity,
        float motion,
        float[] statusSeverities,
        int[] statusColors,
        int spellCount,
        int[] spellColors,
        int itemCount,
        int[] itemColors
    )
    {
        float rAcc = 0f;
        float gAcc = 0f;
        float bAcc = 0f;
        float weightSum = 0f;
        float maxStrength = 0f;

        float pulseHealth = 1f;
        if (statusSeverities[StatusEffect.LOW_HEALTH.ordinal()] > 0f && config.healthPulseStrength() > 0)
        {
            float pStr = (config.healthPulseStrength() / 100f) * motion;
            double beat = 0.5 + 0.5 * Math.sin(timeSec * 5.2);
            pulseHealth = (1f - 0.35f * pStr) + (0.35f * pStr) * (float) beat;
        }

        for (int i = 0; i < statusSeverities.length; i++)
        {
            float sev = statusSeverities[i];
            if (sev <= 0.005f)
            {
                continue;
            }
            if (i == StatusEffect.LOW_HEALTH.ordinal())
            {
                sev *= pulseHealth;
            }
            int argb = statusColors[i];
            float a = ((argb >>> 24) & 0xFF) / 255f;
            float w = sev * a;
            rAcc += ((argb >>> 16) & 0xFF) * w;
            gAcc += ((argb >>> 8) & 0xFF) * w;
            bAcc += (argb & 0xFF) * w;
            weightSum += w;
            if (w > maxStrength)
            {
                maxStrength = w;
            }
        }

        int totalExtra = spellCount + itemCount;
        if (totalExtra > 0)
        {
            float spellPulse = 0.72f + 0.28f * (float) (0.5 + 0.5 * Math.sin(timeSec * 3.4)) * motion + (1f - motion) * 0.28f;
            for (int i = 0; i < totalExtra; i++)
            {
                int argb = (i < spellCount) ? spellColors[i] : itemColors[i - spellCount];
                float a = ((argb >>> 24) & 0xFF) / 255f;
                float w = 0.65f * a * spellPulse;
                rAcc += ((argb >>> 16) & 0xFF) * w;
                gAcc += ((argb >>> 8) & 0xFF) * w;
                bAcc += (argb & 0xFF) * w;
                weightSum += w;
                if (w > maxStrength)
                {
                    maxStrength = w;
                }
            }
        }

        if (weightSum <= 0.001f)
        {
            return;
        }

        int r = Math.min(255, Math.round(rAcc / weightSum));
        int gr = Math.min(255, Math.round(gAcc / weightSum));
        int b = Math.min(255, Math.round(bAcc / weightSum));

        float depth = Math.max(0.10f, config.edgeDepth() / 100f) * 0.5f;
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
        int edgeAlpha = Math.max(1, Math.min(255, Math.round(globalAlpha * 255f)));

        Color edgeColor = new Color(r, gr, b, edgeAlpha);
        Color clearColor = new Color(r, gr, b, 0);

        int edgeW = Math.max(12, Math.round(width * depth));
        int edgeH = Math.max(12, Math.round(height * depth));

        java.awt.Paint oldPaint = g.getPaint();

        // Top
        g.setPaint(new GradientPaint(0, 0, edgeColor, 0, edgeH, clearColor));
        g.fillRect(0, 0, width, edgeH);
        // Bottom
        g.setPaint(new GradientPaint(0, height, edgeColor, 0, height - edgeH, clearColor));
        g.fillRect(0, height - edgeH, width, edgeH);
        // Left
        g.setPaint(new GradientPaint(0, 0, edgeColor, edgeW, 0, clearColor));
        g.fillRect(0, 0, edgeW, height);
        // Right
        g.setPaint(new GradientPaint(width, 0, edgeColor, width - edgeW, 0, clearColor));
        g.fillRect(width - edgeW, 0, edgeW, height);

        float tint = (config.screenTint() / 100f) * 0.22f * globalAlpha;
        if (tint > 0.01f)
        {
            g.setColor(new Color(r, gr, b, Math.min(90, Math.round(tint * 255f))));
            g.fillRect(0, 0, width, height);
        }

        g.setPaint(oldPaint);
    }

    private void drawUnifiedIconShower(
        Graphics2D g,
        int width,
        int height,
        long nowNanos,
        double timeSec,
        ImmersiveCombatOverlaysConfig config,
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

        int maxIcons = Math.min(showerPhaseOffset.length, config.spellShowerMaxIcons());
        int iconSize = config.spellShowerIconSize();
        float speedMult = config.spellShowerSpeed() / 100f;
        float baseSpeed = 0.22f * speedMult;
        float screenFrac = Math.max(0.10f, Math.min(1.0f, config.showerScreenPercent() / 100f));
        ShowerStartLocation startLoc = config.showerStartLocation();

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
            float alpha = Math.max(0f, Math.min(1f, fade * 0.92f));
            if (alpha <= 0.03f)
            {
                continue;
            }

            int x;
            int y;
            int sway = (int) Math.round(Math.sin(timeSec * 2.5 + i) * 8.0);
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
        if (spriteIconCache.size() > 64)
        {
            spriteIconCache.clear();
        }
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
        if (raw == null)
        {
            return null;
        }
        BufferedImage scaled = scalePreserveAspect(raw, targetSize);
        if (itemIconCache.size() > 64)
        {
            itemIconCache.clear();
        }
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
