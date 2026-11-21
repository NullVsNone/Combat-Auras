package com.immersivecombatoverlays.render;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.BorderStyle;
import com.immersivecombatoverlays.model.Cooldown;
import com.immersivecombatoverlays.model.DisplayMode;
import com.immersivecombatoverlays.model.ReadyStyle;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import net.runelite.client.game.ItemManager;

/**
 * Renders inventory item cooldown overlays, timer text, and ready indicators.
 */
@javax.inject.Singleton
public class CooldownRenderer
{
    private static final Stroke BOX_STROKE = new BasicStroke(1.5f);
    private static final Stroke ANIMATED_BASE_STROKE = new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke ANIMATED_SWEEP_STROKE = new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Font TIMER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    private final ItemOutlineCache outlineCache = new ItemOutlineCache();
    private final Arc2D.Float reusableArc = new Arc2D.Float(Arc2D.PIE);

    public void clearCache()
    {
        outlineCache.clear();
    }

    /**
     * @param readyStrength 0..1 strength of the "ready" highlight when the cooldown is finished (0 = draw nothing).
     *                      Computed by the caller from the ready animation/duration settings.
     */
    public void renderItemCooldown(
        Graphics2D g,
        Rectangle bounds,
        int itemId,
        Cooldown cooldown,
        ImmersiveCombatOverlaysConfig config,
        ItemManager itemManager,
        long nowNanos,
        float readyStrength
    )
    {
        if (cooldown == null || bounds == null)
        {
            return;
        }

        if (cooldown.isReady())
        {
            if (readyStrength > 0.01f && config.allowItemGlows())
            {
                drawReadyIndicator(g, bounds, itemId, config, itemManager, nowNanos, readyStrength);
            }
            return;
        }

        DisplayMode mode = config.displayMode();
        Color tint = config.cooldownOverlayColor();
        float frac = cooldown.getRemainingFraction();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (mode == DisplayMode.ITEM_FILL)
        {
            BufferedImage mask = itemManager != null ? outlineCache.getFillMask(itemManager, itemId, tint) : null;
            if (mask != null)
            {
                int maskW = mask.getWidth();
                int maskH = mask.getHeight();
                int srcFillH = Math.max(1, Math.round(maskH * frac));
                int srcY = maskH - srcFillH;
                int fillH = Math.max(1, Math.round(bounds.height * frac));
                int dstY = bounds.y + (bounds.height - fillH);
                g.drawImage(
                    mask,
                    bounds.x, dstY, bounds.x + bounds.width, bounds.y + bounds.height,
                    0, srcY, maskW, maskH,
                    null
                );
            }
            else
            {
                int fillH = Math.max(1, Math.round(bounds.height * frac));
                int fillY = bounds.y + (bounds.height - fillH);
                g.setColor(tint);
                g.fillRect(bounds.x, fillY, bounds.width, fillH);
            }
        }
        else if (mode == DisplayMode.CIRCLE)
        {
            int size = Math.min(bounds.width, bounds.height) - 4;
            int cx = bounds.x + (bounds.width - size) / 2;
            int cy = bounds.y + (bounds.height - size) / 2;
            reusableArc.setFrame(cx, cy, size, size);
            reusableArc.setAngleStart(90.0);
            reusableArc.setAngleExtent(-360.0 * frac);
            g.setColor(tint);
            g.fill(reusableArc);
        }

        if (config.showCooldownTimer())
        {
            String text = cooldown.isUnknownDuration() ? "?" : formatSeconds(cooldown.getRemainingSeconds());
            drawCenteredText(g, bounds, text, Color.WHITE);
        }
    }

    private void drawReadyIndicator(
        Graphics2D g,
        Rectangle bounds,
        int itemId,
        ImmersiveCombatOverlaysConfig config,
        ItemManager itemManager,
        long nowNanos,
        float pulse
    )
    {
        BorderStyle border = config.borderStyle();
        if (border == BorderStyle.NONE)
        {
            return;
        }

        Color base = config.readyColor();
        boolean needsSprite = border == BorderStyle.ITEM_OUTLINE
            || border == BorderStyle.BREATHING_GLOW
            || border == BorderStyle.SOFT_GLOW
            || border == BorderStyle.ANIMATED_BORDER;
        BufferedImage outline = needsSprite ? outlineCache.getOutline(itemManager, itemId, base) : null;
        BufferedImage aura = (border == BorderStyle.SOFT_GLOW || border == BorderStyle.BREATHING_GLOW)
            ? outlineCache.getAura(itemManager, itemId, base)
            : null;
        drawStyledGlow(g, bounds, outline, aura, ItemOutlineCache.AURA_PAD, base, border, pulse, nowNanos);
    }

    private static final ThreadLocal<BufferedImage> SWEEP_SCRATCH = new ThreadLocal<>();

    private static BufferedImage getScratchBuffer(int w, int h)
    {
        BufferedImage buf = SWEEP_SCRATCH.get();
        if (buf == null || buf.getWidth() != w || buf.getHeight() != h)
        {
            buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            SWEEP_SCRATCH.set(buf);
        }
        return buf;
    }

    public static void drawStyledGlow(
        Graphics2D g,
        Rectangle bounds,
        BufferedImage outline,
        Color base,
        BorderStyle border,
        double pulse,
        long nowNanos
    )
    {
        drawStyledGlow(g, bounds, outline, null, ItemOutlineCache.AURA_PAD, base, border, pulse, nowNanos);
    }

    public static void drawStyledGlow(
        Graphics2D g,
        Rectangle bounds,
        BufferedImage outline,
        BufferedImage aura,
        int auraPad,
        Color base,
        BorderStyle border,
        double pulse,
        long nowNanos
    )
    {
        if (border == BorderStyle.NONE || bounds == null)
        {
            return;
        }

        Stroke oldStroke = g.getStroke();
        Composite oldComp = g.getComposite();
        int r = base.getRed();
        int gr = base.getGreen();
        int b = base.getBlue();

        if (border == BorderStyle.SOFT_GLOW || border == BorderStyle.BREATHING_GLOW)
        {
            double breathWave = 0.5 + 0.5 * Math.sin((nowNanos / 1_000_000_000.0) * 3.0);
            double breath = border == BorderStyle.BREATHING_GLOW ? (0.30 + 0.70 * breathWave) : pulse;
            float a = Math.max(0.18f, Math.min(1f, (float) breath * (base.getAlpha() / 255f)));

            if (aura != null)
            {
                g.setComposite(ProceduralRenderer.quantizedAlpha(a));
                int ax = bounds.x + (bounds.width - aura.getWidth()) / 2;
                int ay = bounds.y + (bounds.height - aura.getHeight()) / 2;
                if (border == BorderStyle.BREATHING_GLOW)
                {
                    float scale = 1.0f + 0.08f * (float) breathWave;
                    int sw = Math.round(aura.getWidth() * scale);
                    int sh = Math.round(aura.getHeight() * scale);
                    int sx = bounds.x + (bounds.width - sw) / 2;
                    int sy = bounds.y + (bounds.height - sh) / 2;
                    g.drawImage(aura, sx, sy, sw, sh, null);
                }
                else
                {
                    g.drawImage(aura, ax, ay, null);
                }
                g.setComposite(oldComp);
            }
            else
            {
                float cx = bounds.x + bounds.width * 0.5f;
                float cy = bounds.y + bounds.height * 0.5f;
                float radius = Math.max(8f, Math.min(bounds.width, bounds.height) * 0.58f);
                int cAlpha = Math.max(35, Math.min(170, (int) Math.round(base.getAlpha() * 0.65 * breath)));
                Paint oldPaint = g.getPaint();
                g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(cx, cy),
                    radius,
                    new float[]{0.0f, 0.55f, 1.0f},
                    new Color[]{new Color(r, gr, b, cAlpha), new Color(r, gr, b, cAlpha / 2), new Color(r, gr, b, 0)}
                ));
                g.fillOval(Math.round(cx - radius), Math.round(cy - radius), Math.round(radius * 2f), Math.round(radius * 2f));
                g.setPaint(oldPaint);
            }

            if (outline != null)
            {
                g.setComposite(ProceduralRenderer.quantizedAlpha(a));
                g.drawImage(outline, bounds.x + (bounds.width - outline.getWidth()) / 2, bounds.y + (bounds.height - outline.getHeight()) / 2, null);
                g.setComposite(oldComp);
            }
        }
        else if (border == BorderStyle.ITEM_OUTLINE)
        {
            if (outline != null)
            {
                float a = Math.max(0.15f, Math.min(1f, (float) pulse * (base.getAlpha() / 255f)));
                g.setComposite(ProceduralRenderer.quantizedAlpha(a));
                g.drawImage(outline, bounds.x + (bounds.width - outline.getWidth()) / 2, bounds.y + (bounds.height - outline.getHeight()) / 2, null);
                g.setComposite(oldComp);
            }
        }
        else if (border == BorderStyle.ANIMATED_BORDER)
        {
            double angle = (nowNanos / 1_000_000_000.0) * 3.6;
            if (outline != null)
            {
                int ow = outline.getWidth();
                int oh = outline.getHeight();
                int ox = bounds.x + (bounds.width - ow) / 2;
                int oy = bounds.y + (bounds.height - oh) / 2;

                // Subtle dim base outline of the sprite
                float dimAlpha = Math.max(0.15f, Math.min(0.45f, 0.28f * (float) pulse * (base.getAlpha() / 255f)));
                g.setComposite(ProceduralRenderer.quantizedAlpha(dimAlpha));
                g.drawImage(outline, ox, oy, null);

                // Bright rotating light sweep masked strictly to the sprite's 1px outline
                BufferedImage scratch = getScratchBuffer(ow, oh);
                Graphics2D sg = scratch.createGraphics();
                sg.setComposite(AlphaComposite.Clear);
                sg.fillRect(0, 0, ow, oh);
                sg.setComposite(AlphaComposite.SrcOver);
                sg.drawImage(outline, 0, 0, null);
                sg.setComposite(AlphaComposite.SrcIn);
                float scx = ow * 0.5f;
                float scy = oh * 0.5f;
                float srad = Math.max(ow, oh) * 0.55f;
                float sdx = (float) Math.cos(angle) * srad;
                float sdy = (float) Math.sin(angle) * srad;
                int sweepAlpha = Math.max(170, Math.min(255, (int) Math.round(base.getAlpha() * pulse)));
                sg.setPaint(new GradientPaint(
                    scx + sdx, scy + sdy, new Color(r, gr, b, sweepAlpha),
                    scx - sdx, scy - sdy, new Color(r, gr, b, 0)
                ));
                sg.fillRect(0, 0, ow, oh);
                sg.dispose();

                g.setComposite(oldComp);
                g.drawImage(scratch, ox, oy, null);
            }
            else
            {
                int baseAlpha = Math.max(40, Math.min(255, (int) Math.round(base.getAlpha() * 0.30 * pulse)));
                int sweepAlpha = Math.max(140, Math.min(255, (int) Math.round(base.getAlpha() * pulse)));
                g.setStroke(ANIMATED_BASE_STROKE);
                g.setColor(new Color(r, gr, b, baseAlpha));
                g.drawRoundRect(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2, 6, 6);

                float cx = bounds.x + bounds.width * 0.5f;
                float cy = bounds.y + bounds.height * 0.5f;
                float rad = Math.max(bounds.width, bounds.height) * 0.65f;
                float dx = (float) Math.cos(angle) * rad;
                float dy = (float) Math.sin(angle) * rad;
                Paint oldPaint = g.getPaint();
                g.setPaint(new GradientPaint(
                    cx + dx, cy + dy, new Color(r, gr, b, sweepAlpha),
                    cx - dx, cy - dy, new Color(r, gr, b, 0)
                ));
                g.setStroke(ANIMATED_SWEEP_STROKE);
                g.drawRoundRect(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2, 6, 6);
                g.setPaint(oldPaint);
            }
        }
        else if (border == BorderStyle.CORNER_MARKERS)
        {
            int len = Math.max(4, Math.min(bounds.width, bounds.height) / 4);
            int x1 = bounds.x + 1, y1 = bounds.y + 1;
            int x2 = bounds.x + bounds.width - 2, y2 = bounds.y + bounds.height - 2;
            g.setStroke(BOX_STROKE);
            g.setColor(new Color(r, gr, b, Math.max(100, Math.min(255, (int) Math.round(base.getAlpha() * pulse)))));
            g.drawLine(x1, y1, x1 + len, y1); g.drawLine(x1, y1, x1, y1 + len);
            g.drawLine(x2, y1, x2 - len, y1); g.drawLine(x2, y1, x2, y1 + len);
            g.drawLine(x1, y2, x1 + len, y2); g.drawLine(x1, y2, x1, y2 - len);
            g.drawLine(x2, y2, x2 - len, y2); g.drawLine(x2, y2, x2, y2 - len);
        }
        else if (border == BorderStyle.BOX)
        {
            g.setStroke(BOX_STROKE);
            g.setColor(new Color(r, gr, b, Math.max(80, Math.min(255, (int) Math.round(base.getAlpha() * pulse)))));
            g.drawRoundRect(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2, 4, 4);
        }

        g.setStroke(oldStroke);
    }

    public static void drawCenteredText(Graphics2D g, Rectangle bounds, String text, Color color)
    {
        Font oldFont = g.getFont();
        g.setFont(TIMER_FONT);
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(text);
        int textX = bounds.x + (bounds.width - textW) / 2;
        int textY = bounds.y + (bounds.height + fm.getAscent() - fm.getDescent()) / 2;

        g.setColor(Color.BLACK);
        g.drawString(text, textX + 1, textY + 1);
        g.setColor(color);
        g.drawString(text, textX, textY);
        g.setFont(oldFont);
    }

    /**
     * Strength (0..1) of an item's ready highlight, {@code elapsedNanos} after its cooldown finished.
     * Returns 0 when the highlight should no longer be drawn.
     */
    public static float readyStrength(ReadyStyle style, boolean permanent, int durationSeconds, long elapsedNanos, long nowNanos)
    {
        if (style == null || style == ReadyStyle.NONE || elapsedNanos < 0L)
        {
            return 0f;
        }
        long durNanos = Math.max(1, durationSeconds) * 1_000_000_000L;
        if (!permanent && elapsedNanos >= durNanos)
        {
            return 0f;
        }
        switch (style)
        {
            case SINGLE_FLASH:
            {
                // One bright flash that fades out; a permanent highlight settles at a dim steady outline
                float t = Math.min(1f, elapsedNanos / (float) durNanos);
                float fade = (1f - t) * (1f - t);
                return permanent ? Math.max(0.35f, fade) : fade;
            }
            case PULSE:
            {
                double phase = (nowNanos / 1_000_000_000.0) * 3.2;
                return 0.45f + 0.55f * (float) (0.5 + 0.5 * Math.sin(phase));
            }
            default:
                return 1f;
        }
    }

    public static String formatSeconds(float seconds)
    {
        if (seconds >= 60f)
        {
            int mins = (int) (seconds / 60f);
            int secs = Math.round(seconds % 60f);
            if (secs == 60)
            {
                mins++;
                secs = 0;
            }
            return mins + ":" + (secs < 10 ? "0" : "") + secs;
        }
        if (seconds >= 10f)
        {
            return String.valueOf(Math.round(seconds));
        }
        return String.format("%.1f", seconds);
    }
}
