package com.combatauras.render;

import com.combatauras.model.BorderStyle;
import com.combatauras.model.Cooldown;
import com.combatauras.model.DisplayMode;
import com.combatauras.model.ReadyStyle;
import com.combatauras.settings.SettingsSnapshot;
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
    // Glow strength above 125% also thickens the line styles, whose alpha is already near opaque
    private static final Stroke BOLD_BOX_STROKE = new BasicStroke(2.5f);
    private static final Stroke BOLD_SWEEP_STROKE = new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
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
        SettingsSnapshot s,
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
            if (readyStrength > 0.01f)
            {
                drawItemGlow(g, bounds, itemManager, itemId, s.readyColor, s.readyBorder, readyStrength, nowNanos, s);
            }
            return;
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float frac = cooldown.getRemainingFraction();
        if (s.cooldownStyle == DisplayMode.ITEM_FILL)
        {
            BufferedImage mask = itemManager != null ? outlineCache.getFillMask(itemManager, itemId, s.cooldownTint) : null;
            drawFill(g, bounds, mask, frac, s.cooldownTint);
        }
        else if (s.cooldownStyle == DisplayMode.CIRCLE)
        {
            drawRadial(g, reusableArc, bounds, 4, frac, s.cooldownTint);
        }

        if (s.showCooldownTimer)
        {
            drawCenteredText(g, bounds, formatTicks(cooldown.getRemainingTicks()), Color.WHITE);
        }
    }

    /** Glow an inventory item in {@code style}, building (and caching) its outline and aura only when the style needs them. */
    public void drawItemGlow(
        Graphics2D g,
        Rectangle bounds,
        ItemManager itemManager,
        int itemId,
        Color base,
        BorderStyle style,
        double pulse,
        long nowNanos,
        SettingsSnapshot s
    )
    {
        if (style == BorderStyle.NONE)
        {
            return;
        }
        BufferedImage outline = style.usesOutline() ? outlineCache.getOutline(itemManager, itemId, base) : null;
        BufferedImage aura = style.usesAura() ? outlineCache.getAura(itemManager, itemId, base) : null;
        drawStyledGlow(g, bounds, outline, aura, base, style, pulse, nowNanos, s);
    }

    /** Fill the bottom {@code frac} of the icon, masked to its sprite when a mask is available. */
    public static void drawFill(Graphics2D g, Rectangle bounds, BufferedImage mask, float frac, Color tint)
    {
        int fillH = Math.max(1, Math.round(bounds.height * frac));
        int dstY = bounds.y + (bounds.height - fillH);
        if (mask != null)
        {
            int maskW = mask.getWidth();
            int maskH = mask.getHeight();
            int srcFillH = Math.max(1, Math.round(maskH * frac));
            g.drawImage(mask, bounds.x, dstY, bounds.x + bounds.width, bounds.y + bounds.height,
                0, maskH - srcFillH, maskW, maskH, null);
        }
        else
        {
            g.setColor(tint);
            g.fillRect(bounds.x, dstY, bounds.width, fillH);
        }
    }

    /** Clockwise pie of the remaining fraction, starting at 12 o'clock. */
    public static void drawRadial(Graphics2D g, Arc2D.Float arc, Rectangle bounds, int inset, float frac, Color tint)
    {
        int size = Math.min(bounds.width, bounds.height) - inset;
        arc.setFrame(bounds.x + (bounds.width - size) / 2f, bounds.y + (bounds.height - size) / 2f, size, size);
        arc.setAngleStart(90.0);
        arc.setAngleExtent(-360.0 * frac);
        g.setColor(tint);
        g.fill(arc);
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

    /**
     * Draw {@code img} at {@code alpha}. Alpha above 1 (glow strength over 100%) adds a second layer for the
     * excess, so a stronger glow gets brighter instead of clipping.
     */
    private static void drawLayered(Graphics2D g, BufferedImage img, int x, int y, int w, int h, float alpha, Composite restore)
    {
        g.setComposite(ProceduralRenderer.quantizedAlpha(Math.min(1f, alpha)));
        g.drawImage(img, x, y, w, h, null);
        if (alpha > 1.01f)
        {
            g.setComposite(ProceduralRenderer.quantizedAlpha(Math.min(1f, alpha - 1f)));
            g.drawImage(img, x, y, w, h, null);
        }
        g.setComposite(restore);
    }

    private static void drawCentered(Graphics2D g, Rectangle bounds, BufferedImage img, float alpha, Composite restore)
    {
        int w = img.getWidth();
        int h = img.getHeight();
        drawLayered(g, img, bounds.x + (bounds.width - w) / 2, bounds.y + (bounds.height - h) / 2, w, h, alpha, restore);
    }

    private static int clampAlpha(double a)
    {
        return Math.max(0, Math.min(255, (int) Math.round(a)));
    }

    /**
     * The one glow routine shared by inventory items, spell icons, the Magic tab and the spec orb.
     * {@code pulse} is the caller's 0..1 animation strength; motion and glow strength come from the snapshot.
     */
    public static void drawStyledGlow(
        Graphics2D g,
        Rectangle bounds,
        BufferedImage outline,
        BufferedImage aura,
        Color base,
        BorderStyle border,
        double pulse,
        long nowNanos,
        SettingsSnapshot s
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
        float baseA = base.getAlpha() / 255f;
        float strength = s.iconGlowStrength;
        float motion = s.motionStrength;

        if (border.usesAura())
        {
            boolean breathing = border == BorderStyle.BREATHING_GLOW;
            double breath = breathing ? Motion.pulse(nowNanos, Motion.BREATH_RATE, 0.30f, motion) : pulse;
            float a = Math.max(0.18f, Math.min(1f, (float) breath * baseA)) * strength;

            if (aura != null)
            {
                if (breathing)
                {
                    float scale = 1.0f + 0.08f * motion * Motion.wave(nowNanos, Motion.BREATH_RATE);
                    int sw = Math.round(aura.getWidth() * scale);
                    int sh = Math.round(aura.getHeight() * scale);
                    drawLayered(g, aura, bounds.x + (bounds.width - sw) / 2, bounds.y + (bounds.height - sh) / 2, sw, sh, a, oldComp);
                }
                else
                {
                    drawCentered(g, bounds, aura, a, oldComp);
                }
            }
            else
            {
                float cx = bounds.x + bounds.width * 0.5f;
                float cy = bounds.y + bounds.height * 0.5f;
                float radius = Math.max(8f, Math.min(bounds.width, bounds.height) * 0.58f);
                int cAlpha = clampAlpha(Math.max(35, Math.min(170, base.getAlpha() * 0.65 * breath)) * strength);
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
                drawCentered(g, bounds, outline, a, oldComp);
            }
        }
        else if (border == BorderStyle.ITEM_OUTLINE)
        {
            if (outline != null)
            {
                drawCentered(g, bounds, outline, Math.max(0.15f, Math.min(1f, (float) pulse * baseA)) * strength, oldComp);
            }
        }
        else if (border == BorderStyle.ANIMATED_BORDER)
        {
            double angle = Motion.angle(nowNanos, Motion.SWEEP_RATE, motion);
            if (outline != null)
            {
                int ow = outline.getWidth();
                int oh = outline.getHeight();
                int ox = bounds.x + (bounds.width - ow) / 2;
                int oy = bounds.y + (bounds.height - oh) / 2;

                // Subtle dim base outline of the sprite
                float dimAlpha = Math.max(0.15f, Math.min(0.45f, 0.28f * (float) pulse * baseA)) * strength;
                drawLayered(g, outline, ox, oy, ow, oh, dimAlpha, oldComp);

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

                drawLayered(g, scratch, ox, oy, ow, oh, strength, oldComp);
            }
            else
            {
                int baseAlpha = clampAlpha(Math.max(40, Math.min(255, base.getAlpha() * 0.30 * pulse)) * strength);
                int sweepAlpha = clampAlpha(Math.max(140, Math.min(255, base.getAlpha() * pulse)) * strength);
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
                g.setStroke(strength > 1.25f ? BOLD_SWEEP_STROKE : ANIMATED_SWEEP_STROKE);
                g.drawRoundRect(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2, 6, 6);
                g.setPaint(oldPaint);
            }
        }
        else if (border == BorderStyle.CORNER_MARKERS)
        {
            int len = Math.max(4, Math.min(bounds.width, bounds.height) / 4);
            int x1 = bounds.x + 1, y1 = bounds.y + 1;
            int x2 = bounds.x + bounds.width - 2, y2 = bounds.y + bounds.height - 2;
            g.setStroke(strength > 1.25f ? BOLD_BOX_STROKE : BOX_STROKE);
            g.setColor(new Color(r, gr, b, clampAlpha(Math.max(100, Math.min(255, base.getAlpha() * pulse)) * strength)));
            g.drawLine(x1, y1, x1 + len, y1); g.drawLine(x1, y1, x1, y1 + len);
            g.drawLine(x2, y1, x2 - len, y1); g.drawLine(x2, y1, x2, y1 + len);
            g.drawLine(x1, y2, x1 + len, y2); g.drawLine(x1, y2, x1, y2 - len);
            g.drawLine(x2, y2, x2 - len, y2); g.drawLine(x2, y2, x2, y2 - len);
        }
        else if (border == BorderStyle.BOX)
        {
            g.setStroke(strength > 1.25f ? BOLD_BOX_STROKE : BOX_STROKE);
            g.setColor(new Color(r, gr, b, clampAlpha(Math.max(80, Math.min(255, base.getAlpha() * pulse)) * strength)));
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
     * Returns 0 when the highlight should no longer be drawn. {@code motion} (0..1) scales the Pulse animation.
     */
    public static float readyStrength(ReadyStyle style, boolean permanent, int durationSeconds, long elapsedNanos, long nowNanos, float motion)
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
                return Motion.pulse(nowNanos, Motion.READY_PULSE_RATE, 0.45f, motion);
            default:
                return 1f;
        }
    }

    // Timer labels by remaining ticks: cooldowns only change once per tick, so each label is built once and reused
    // every frame instead of formatting a new String per icon per frame. Client thread only.
    private static final String[] TICK_LABELS = new String[6000]; // one hour

    /** Timer label for a cooldown with {@code ticks} game ticks left (cached). */
    public static String formatTicks(int ticks)
    {
        if (ticks < 0 || ticks >= TICK_LABELS.length)
        {
            return formatSeconds(Math.max(0, ticks) * 0.6f);
        }
        String label = TICK_LABELS[ticks];
        if (label == null)
        {
            label = formatSeconds(ticks * 0.6f);
            TICK_LABELS[ticks] = label;
        }
        return label;
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
        // One decimal without String.format (slow, and locale-dependent: "5,4" in some locales)
        int tenths = Math.round(seconds * 10f);
        return (tenths / 10) + "." + (tenths % 10);
    }
}
