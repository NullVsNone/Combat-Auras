package com.immersivecombatoverlays.render;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.game.ItemManager;

/**
 * Caches 1px sprite silhouette outlines, outward-flowing sprite auras, and fill masks
 * so ready-state borders and glows never allocate or scan pixels per frame.
 * Shared by both InventoryCooldownOverlay (items) and SpellCooldownOverlay (spells/tabs/orbs).
 */
public class ItemOutlineCache
{
    public static final int AURA_PAD = 5;

    private final Map<Long, BufferedImage> outlineCache = new HashMap<>();
    private final Map<Long, BufferedImage> auraCache = new HashMap<>();
    private final Map<Long, BufferedImage> fillMaskCache = new HashMap<>();

    public void clear()
    {
        outlineCache.clear();
        auraCache.clear();
        fillMaskCache.clear();
    }

    public BufferedImage getOutline(ItemManager itemManager, int itemId, Color color)
    {
        long key = (((long) itemId) << 32) | (color.getRGB() & 0xFFFFFFFFL);
        BufferedImage cached = outlineCache.get(key);
        if (cached != null)
        {
            return cached;
        }
        if (itemManager == null)
        {
            return null;
        }
        BufferedImage sprite = itemManager.getImage(itemId);
        if (sprite == null)
        {
            return null;
        }
        BufferedImage outline = buildOutline(sprite, color);
        if (outlineCache.size() > 256)
        {
            outlineCache.clear();
        }
        outlineCache.put(key, outline);
        return outline;
    }

    public BufferedImage getAura(ItemManager itemManager, int itemId, Color color)
    {
        long key = (((long) itemId) << 32) | (color.getRGB() & 0xFFFFFFFFL);
        BufferedImage cached = auraCache.get(key);
        if (cached != null)
        {
            return cached;
        }
        if (itemManager == null)
        {
            return null;
        }
        BufferedImage sprite = itemManager.getImage(itemId);
        if (sprite == null)
        {
            return null;
        }
        BufferedImage aura = buildOutwardAura(sprite, color, AURA_PAD);
        if (auraCache.size() > 256)
        {
            auraCache.clear();
        }
        auraCache.put(key, aura);
        return aura;
    }

    public BufferedImage getFillMask(ItemManager itemManager, int itemId, Color tint)
    {
        long key = (((long) itemId) << 32) | (tint.getRGB() & 0xFFFFFFFFL);
        BufferedImage cached = fillMaskCache.get(key);
        if (cached != null)
        {
            return cached;
        }
        if (itemManager == null)
        {
            return null;
        }
        BufferedImage sprite = itemManager.getImage(itemId);
        if (sprite == null)
        {
            return null;
        }
        BufferedImage mask = buildFillMask(sprite, tint);
        if (fillMaskCache.size() > 256)
        {
            fillMaskCache.clear();
        }
        fillMaskCache.put(key, mask);
        return mask;
    }

    public static BufferedImage buildOutline(BufferedImage sprite, Color color)
    {
        int w = sprite.getWidth();
        int h = sprite.getHeight();
        BufferedImage outline = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int argb = color.getRGB();

        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                int alpha = (sprite.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 80)
                {
                    continue;
                }
                if (isOpaque(sprite, x - 1, y, w, h)
                    || isOpaque(sprite, x + 1, y, w, h)
                    || isOpaque(sprite, x, y - 1, w, h)
                    || isOpaque(sprite, x, y + 1, w, h))
                {
                    outline.setRGB(x, y, argb);
                }
            }
        }
        return outline;
    }

    /**
     * Builds an outward-flowing glow halo that hugs the exact silhouette of the sprite
     * and fades smoothly outward on all sides, while keeping the interior of the sprite clear.
     */
    public static BufferedImage buildOutwardAura(BufferedImage sprite, Color color, int pad)
    {
        int w = sprite.getWidth();
        int h = sprite.getHeight();
        int aw = w + pad * 2;
        int ah = h + pad * 2;
        BufferedImage aura = new BufferedImage(aw, ah, BufferedImage.TYPE_INT_ARGB);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int maxAlpha = Math.max(120, color.getAlpha());
        float maxDistSq = (pad + 0.5f) * (pad + 0.5f);

        for (int ay = 0; ay < ah; ay++)
        {
            int sy = ay - pad;
            for (int ax = 0; ax < aw; ax++)
            {
                int sx = ax - pad;
                if (isOpaque(sprite, sx, sy, w, h))
                {
                    continue;
                }

                float minDistSq = maxDistSq + 1f;
                int y0 = Math.max(0, sy - pad);
                int y1 = Math.min(h - 1, sy + pad);
                int x0 = Math.max(0, sx - pad);
                int x1 = Math.min(w - 1, sx + pad);

                for (int ny = y0; ny <= y1; ny++)
                {
                    int dy = ny - sy;
                    int dySq = dy * dy;
                    if (dySq >= minDistSq)
                    {
                        continue;
                    }
                    for (int nx = x0; nx <= x1; nx++)
                    {
                        if (((sprite.getRGB(nx, ny) >>> 24) & 0xFF) > 80)
                        {
                            int dx = nx - sx;
                            float dSq = dx * dx + dySq;
                            if (dSq < minDistSq)
                            {
                                minDistSq = dSq;
                            }
                        }
                    }
                }

                if (minDistSq <= maxDistSq)
                {
                    float d = (float) Math.sqrt(minDistSq);
                    float t = Math.max(0f, 1f - (d - 0.8f) / (pad + 0.2f));
                    int a = Math.max(0, Math.min(255, Math.round(maxAlpha * 0.72f * t * t)));
                    if (a > 0)
                    {
                        aura.setRGB(ax, ay, (a << 24) | (r << 16) | (g << 8) | b);
                    }
                }
            }
        }
        return aura;
    }

    public static BufferedImage buildFillMask(BufferedImage sprite, Color tint)
    {
        int w = sprite.getWidth();
        int h = sprite.getHeight();
        BufferedImage mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int argb = tint.getRGB();

        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                int alpha = (sprite.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 80)
                {
                    mask.setRGB(x, y, argb);
                }
            }
        }
        return mask;
    }

    private static boolean isOpaque(BufferedImage img, int x, int y, int w, int h)
    {
        if (x < 0 || y < 0 || x >= w || y >= h)
        {
            return false;
        }
        return ((img.getRGB(x, y) >>> 24) & 0xFF) > 80;
    }
}
