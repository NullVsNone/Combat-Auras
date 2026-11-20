package com.immersivecombatoverlays.overlay;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.CombatSpell;
import com.immersivecombatoverlays.model.Cooldown;
import com.immersivecombatoverlays.model.SpellCooldownDisplayMode;
import com.immersivecombatoverlays.model.BorderStyle;
import com.immersivecombatoverlays.render.CooldownRenderer;
import com.immersivecombatoverlays.render.ItemOutlineCache;
import com.immersivecombatoverlays.tracker.CombatSpellTracker;
import com.immersivecombatoverlays.tracker.CombatStateTracker;
import java.awt.image.BufferedImage;
import net.runelite.client.game.SpriteManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.SpritePixels;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;

/**
 * Overlays combat spells directly in the spellbook (preserving Resource Pack sprites)
 * with remaining recast cooldown time, glows expired/ready spells in the spellbook,
 * and glows the Magic spellbook tab icon with blended active spell colors.
 */
@Singleton
public class SpellCooldownOverlay extends Overlay
{
    private static final int SPELLBOOK_GROUP_ID = 218;
    private static final int SPRITE_TAB_MAGIC = 780;
    private static final int SPRITE_SPEC_ORB = 1607;
    private static final int[] SPEC_ORB_COMPONENTS = {
        InterfaceID.Orbs.SPECENERGY_BACKING,
        InterfaceID.OrbsNomap.SPECENERGY_BACKING,
        InterfaceID.OrbsOsm.SPECENERGY_BACKING,
        InterfaceID.OrbsOsmNomap.SPECENERGY_BACKING,
    };
    private static final int[] MAGIC_TAB_COMPONENTS = {
        InterfaceID.Toplevel.STONE6,
        InterfaceID.ToplevelOsrsStretch.STONE6,
        InterfaceID.ToplevelPreEoc.STONE6,
    };

    private final Client client;
    private final ImmersiveCombatOverlaysConfig config;
    private final CombatSpellTracker spellTracker;
    private final CombatStateTracker combatState;
    private final SpriteManager spriteManager;

    private final Arc2D.Float reusableArc = new Arc2D.Float(Arc2D.PIE);
    private final int[] readySpellOrdinals = new int[CombatSpell.values().length];
    private final int[] readySpellColors = new int[CombatSpell.values().length];
    private final Map<Long, BufferedImage> spriteOutlineCache = new HashMap<>();
    private final Map<Long, BufferedImage> spriteAuraCache = new HashMap<>();
    private final Map<Integer, CombatSpell> widgetSpellCache = new HashMap<>();
    private final Map<Long, BufferedImage> spriteFillMaskCache = new HashMap<>();
    private volatile int currentSpellbook = 0;
    private volatile boolean specReadyLive = false;
    private long specReadySinceNanos = 0L;

    @Inject
    public SpellCooldownOverlay(
        Client client,
        ImmersiveCombatOverlaysConfig config,
        CombatSpellTracker spellTracker,
        CombatStateTracker combatState,
        SpriteManager spriteManager
    )
    {
        this.client = client;
        this.config = config;
        this.spellTracker = spellTracker;
        this.combatState = combatState;
        this.spriteManager = spriteManager;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
    }

    public void updateSpellbook(boolean combatActive)
    {
        this.currentSpellbook = client.getVarbitValue(VarbitID.SPELLBOOK);
        boolean specOk = config.specEnabled() && (!config.specOnlyInCombat() || combatActive);
        boolean ready = specOk && (client.getVarpValue(VarPlayerID.SA_ENERGY) / 10) >= config.specThresholdPercent();
        long now = System.nanoTime();
        if (!ready)
        {
            specReadySinceNanos = 0L;
        }
        else if (specReadySinceNanos == 0L)
        {
            specReadySinceNanos = now;
        }
        int dur = config.specDurationSeconds();
        this.specReadyLive = ready && (dur <= 0 || now - specReadySinceNanos <= dur * 1_000_000_000L);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN || !config.spellsEnabled())
        {
            return null;
        }

        long nowNanos = System.nanoTime();
        double pulse = 0.55 + 0.45 * Math.sin((nowNanos / 1_000_000_000.0) * 4.2);

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Render cooldowns & ready glows over visible spell widgets in the spellbook
        renderSpellbookSpells(graphics, pulse);

        if (!config.allowItemGlows())
        {
            return null;
        }

        // 2. Render blended glow on the Magic spellbook tab icon when any spell is expired/ready
        if (config.spellbookTabGlow())
        {
            renderSpellbookTabGlow(graphics, pulse);
        }

        // 3. Spec orb glow when Special Attack is ready
        if (config.specOrbGlow() && (specReadyLive || config.debugSpecReady()))
        {
            renderSpecOrbGlow(graphics, pulse);
        }

        // 4. Optional floating Debug Spell Grid preview (works regardless of current spellbook)
        if (config.debugPreviewSpellGrid())
        {
            renderDebugSpellGrid(graphics, pulse);
        }

        return null;
    }

    private void renderSpellbookSpells(Graphics2D g, double pulse)
    {
        Widget spellbookContainer = client.getWidget(SPELLBOOK_GROUP_ID, 0);
        if (spellbookContainer == null || spellbookContainer.isHidden())
        {
            return;
        }

        Widget[] children = spellbookContainer.getStaticChildren();
        if (children == null || children.length == 0)
        {
            children = spellbookContainer.getChildren();
        }
        if (children == null)
        {
            return;
        }

        for (Widget child : children)
        {
            if (child == null || child.isHidden())
            {
                continue;
            }

            int wId = child.getId();
            CombatSpell matchedSpell = widgetSpellCache.get(wId);
            if (matchedSpell == null && !widgetSpellCache.containsKey(wId))
            {
                String rawName = child.getName();
                if (rawName != null && !rawName.isEmpty())
                {
                    matchedSpell = matchSpell(Text.removeTags(rawName).trim());
                }
                if (widgetSpellCache.size() > 256)
                {
                    widgetSpellCache.clear();
                }
                widgetSpellCache.put(wId, matchedSpell);
            }
            if (matchedSpell == null)
            {
                continue;
            }
            spellTracker.registerWidget(matchedSpell, child.getId(), child.getSpriteId());
            if (!ImmersiveCombatOverlaysConfig.isSpellEnabled(config, matchedSpell) && !ImmersiveCombatOverlaysConfig.isDebugSpellActive(config, matchedSpell))
            {
                continue;
            }

            Rectangle bounds = child.getBounds();
            if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
            {
                continue;
            }

            Cooldown cd = spellTracker.getCooldown(matchedSpell);
            if (!cd.isReady())
            {
                drawSpellCooldown(g, bounds, child.getId(), child.getSpriteId(), cd);
            }
            else if (ImmersiveCombatOverlaysConfig.isSpellbookGlowEnabled(config, matchedSpell)
                && spellTracker.isSpellReadyReminder(matchedSpell, combatState, config))
            {
                Color spellColor = ImmersiveCombatOverlaysConfig.getSpellColor(config, matchedSpell);
                drawIconAuraAndOutline(g, bounds, child.getId(), child.getSpriteId(), spellColor, pulse);
            }
        }
    }

    private void drawSpellCooldown(Graphics2D g, Rectangle bounds, int widgetId, int spriteId, Cooldown cd)
    {
        SpellCooldownDisplayMode mode = config.spellCooldownDisplayMode();
        Color tint = config.spellCooldownOverlayColor();
        float frac = cd.getRemainingFraction();

        if (mode == SpellCooldownDisplayMode.RADIAL_AND_TEXT || mode == SpellCooldownDisplayMode.RADIAL_ONLY)
        {
            int size = Math.min(bounds.width, bounds.height) - 2;
            int cx = bounds.x + (bounds.width - size) / 2;
            int cy = bounds.y + (bounds.height - size) / 2;
            reusableArc.setFrame(cx, cy, size, size);
            reusableArc.setAngleStart(90.0);
            reusableArc.setAngleExtent(-360.0 * frac);
            g.setColor(tint);
            g.fill(reusableArc);
        }
        else if (mode == SpellCooldownDisplayMode.FILL_AND_TEXT)
        {
            BufferedImage mask = spriteId > 0 ? getSpriteFillMask(widgetId, spriteId, tint) : null;
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

        if (mode != SpellCooldownDisplayMode.RADIAL_ONLY)
        {
            String text = CooldownRenderer.formatSeconds(cd.getRemainingSeconds());
            CooldownRenderer.drawCenteredText(g, bounds, text, Color.WHITE);
        }
    }

    private void renderSpellbookTabGlow(Graphics2D g, double pulse)
    {
        int spellbook = this.currentSpellbook;
        int count = spellTracker.collectReadySpells(
            combatState, config, spellbook, true, readySpellOrdinals, readySpellColors
        );
        if (count <= 0)
        {
            return;
        }

        Widget magicTab = findMagicTabWidget();
        if (magicTab == null || magicTab.isHidden())
        {
            return;
        }

        Rectangle bounds = magicTab.getBounds();
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
        {
            return;
        }

        // Blend all active ready spell colors together for the spellbook tab icon glow
        int rSum = 0, gSum = 0, bSum = 0, aMax = 0;
        for (int i = 0; i < count; i++)
        {
            int argb = readySpellColors[i];
            rSum += (argb >>> 16) & 0xFF;
            gSum += (argb >>> 8) & 0xFF;
            bSum += argb & 0xFF;
            aMax = Math.max(aMax, (argb >>> 24) & 0xFF);
        }
        Color blended = new Color(rSum / count, gSum / count, bSum / count, Math.max(120, aMax));
        drawIconAuraAndOutline(g, bounds, magicTab.getId(), SPRITE_TAB_MAGIC, blended, pulse);
    }

    private void renderDebugSpellGrid(Graphics2D g, double pulse)
    {
        CombatSpell[] spells = CombatSpell.values();
        int cols = 5;
        int cell = 30;
        int pad = 6;
        int startX = 16;
        int startY = 46;
        int rows = (spells.length + cols - 1) / cols;
        int boxW = cols * (cell + pad) + pad;
        int boxH = rows * (cell + pad) + pad + 16;

        g.setColor(new Color(20, 20, 24, 195));
        g.fillRoundRect(startX, startY, boxW, boxH, 8, 8);
        g.setColor(new Color(90, 90, 105, 200));
        g.drawRoundRect(startX, startY, boxW, boxH, 8, 8);
        g.setColor(Color.WHITE);
        g.drawString("Spell Debug Preview", startX + 8, startY + 13);

        for (int i = 0; i < spells.length; i++)
        {
            CombatSpell spell = spells[i];
            int col = i % cols;
            int row = i / cols;
            int cx = startX + pad + col * (cell + pad);
            int cy = startY + 18 + pad + row * (cell + pad);
            Rectangle r = new Rectangle(cx, cy, cell, cell);

            int wId = spellTracker.getResolvedWidgetId(spell);
            int sId = spellTracker.getResolvedSpriteId(spell);
            BufferedImage icon = resolveSprite(wId, sId);
            if (icon != null)
            {
                int ix = cx + (cell - icon.getWidth()) / 2;
                int iy = cy + (cell - icon.getHeight()) / 2;
                g.drawImage(icon, ix, iy, null);
            }

            Cooldown cd = spellTracker.getCooldown(spell);
            if (cd.isReady())
            {
                long cycleMs = (System.currentTimeMillis() + i * 750L) % 8000L;
                if (cycleMs < 4500L)
                {
                    int remTicks = Math.max(1, (int) Math.ceil((4500L - cycleMs) / 600.0));
                    cd = Cooldown.active(remTicks, 8);
                }
            }
            if (!cd.isReady())
            {
                drawSpellCooldown(g, r, wId, sId, cd);
            }
            else
            {
                drawIconAuraAndOutline(g, r, wId, sId, ImmersiveCombatOverlaysConfig.getSpellColor(config, spell), pulse);
            }
        }
    }

    private void renderSpecOrbGlow(Graphics2D g, double pulse)
    {
        // Target the orb circle itself. The ORB_SPECENERGY container also spans the "100" text beside the orb,
        // which is why centring the glow on it drew the ring off to one side.
        Widget orb = null;
        for (int componentId : SPEC_ORB_COMPONENTS)
        {
            Widget w = client.getWidget(componentId);
            if (w != null && !w.isHidden())
            {
                orb = w;
                break;
            }
        }
        if (orb == null)
        {
            return;
        }
        Rectangle bounds = orb.getBounds();
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
        {
            return;
        }
        // Build the outline from the orb's own (possibly resource-pack) sprite so its shape and size match exactly
        int spriteId = orb.getSpriteId() > 0 ? orb.getSpriteId() : SPRITE_SPEC_ORB;
        drawIconAuraAndOutline(g, bounds, orb.getId(), spriteId, config.specColor(), pulse);
    }

    private Widget findMagicTabWidget()
    {
        for (int componentId : MAGIC_TAB_COMPONENTS)
        {
            Widget w = client.getWidget(componentId);
            if (w != null && !w.isHidden())
            {
                return w;
            }
        }
        // Scan modern resizable bottom-line (164), classic resizable (161), and fixed (548) for Magic tab
        int[] groups = {164, 161, 548};
        for (int group : groups)
        {
            Widget root = client.getWidget(group, 0);
            if (root == null || root.isHidden())
            {
                continue;
            }
            Widget found = findMagicWidgetInChildren(root.getStaticChildren());
            if (found != null)
            {
                return found;
            }
            found = findMagicWidgetInChildren(root.getChildren());
            if (found != null)
            {
                return found;
            }
        }
        return null;
    }

    private static Widget findMagicWidgetInChildren(Widget[] children)
    {
        if (children == null)
        {
            return null;
        }
        for (Widget child : children)
        {
            if (child == null || child.isHidden())
            {
                continue;
            }
            String name = child.getName();
            if (name != null && (name.contains("Magic") || name.contains("Spellbook")))
            {
                return child;
            }
            String[] actions = child.getActions();
            if (actions != null)
            {
                for (String act : actions)
                {
                    if ("Magic".equalsIgnoreCase(act))
                    {
                        return child;
                    }
                }
            }
        }
        return null;
    }

    public void clearCache()
    {
        spriteOutlineCache.clear();
        spriteAuraCache.clear();
        widgetSpellCache.clear();
        spriteFillMaskCache.clear();
    }

    private BufferedImage getSpriteFillMask(int widgetId, int spriteId, Color tint)
    {
        long key = ((((long) (widgetId > 0 ? widgetId : spriteId)) << 32)) | (tint.getRGB() & 0xFFFFFFFFL);
        BufferedImage cached = spriteFillMaskCache.get(key);
        if (cached != null)
        {
            return cached;
        }
        BufferedImage sprite = null;
        if (widgetId > 0 && client.getWidgetSpriteOverrides() != null)
        {
            SpritePixels wOverride = client.getWidgetSpriteOverrides().get(widgetId);
            if (wOverride != null)
            {
                sprite = wOverride.toBufferedImage();
            }
        }
        if (sprite == null && client.getSpriteOverrides() != null)
        {
            SpritePixels override = client.getSpriteOverrides().get(spriteId);
            if (override != null)
            {
                sprite = override.toBufferedImage();
            }
        }
        if (sprite == null)
        {
            sprite = spriteManager.getSprite(spriteId, 0);
        }
        if (sprite == null)
        {
            return null;
        }
        int w = sprite.getWidth();
        int h = sprite.getHeight();
        BufferedImage mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int argb = tint.getRGB();
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                if (((sprite.getRGB(x, y) >>> 24) & 0xFF) > 25)
                {
                    mask.setRGB(x, y, argb);
                }
            }
        }
        if (spriteFillMaskCache.size() > 128)
        {
            spriteFillMaskCache.clear();
        }
        spriteFillMaskCache.put(key, mask);
        return mask;
    }

    private void drawIconAuraAndOutline(Graphics2D g, Rectangle bounds, int widgetId, int spriteId, Color base, double pulse)
    {
        BorderStyle style = config.spellBorderStyle();
        boolean needsSprite = spriteId > 0 && (
            style == BorderStyle.ITEM_OUTLINE
            || style == BorderStyle.SOFT_GLOW
            || style == BorderStyle.BREATHING_GLOW
            || style == BorderStyle.ANIMATED_BORDER
        );
        BufferedImage outline = needsSprite ? getSpriteOutline(widgetId, spriteId, base) : null;
        BufferedImage aura = (spriteId > 0 && (style == BorderStyle.SOFT_GLOW || style == BorderStyle.BREATHING_GLOW))
            ? getSpriteAura(widgetId, spriteId, base)
            : null;
        CooldownRenderer.drawStyledGlow(g, bounds, outline, aura, ItemOutlineCache.AURA_PAD, base, style, pulse, System.nanoTime());
    }

    private BufferedImage resolveSprite(int widgetId, int spriteId)
    {
        BufferedImage sprite = null;
        if (widgetId > 0 && client.getWidgetSpriteOverrides() != null)
        {
            SpritePixels wOverride = client.getWidgetSpriteOverrides().get(widgetId);
            if (wOverride != null)
            {
                sprite = wOverride.toBufferedImage();
            }
        }
        if (sprite == null && client.getSpriteOverrides() != null)
        {
            SpritePixels override = client.getSpriteOverrides().get(spriteId);
            if (override != null)
            {
                sprite = override.toBufferedImage();
            }
        }
        if (sprite == null)
        {
            sprite = spriteManager.getSprite(spriteId, 0);
        }
        return sprite;
    }

    private BufferedImage getSpriteOutline(int widgetId, int spriteId, Color color)
    {
        long key = (((long) (widgetId > 0 ? widgetId : spriteId)) << 32) | (color.getRGB() & 0xFFFFFFFFL);
        BufferedImage cached = spriteOutlineCache.get(key);
        if (cached != null)
        {
            return cached;
        }
        BufferedImage sprite = resolveSprite(widgetId, spriteId);
        if (sprite == null)
        {
            return null;
        }
        BufferedImage outline = ItemOutlineCache.buildOutline(sprite, new Color(color.getRed(), color.getGreen(), color.getBlue(), 215));
        if (spriteOutlineCache.size() > 128)
        {
            spriteOutlineCache.clear();
        }
        spriteOutlineCache.put(key, outline);
        return outline;
    }

    private BufferedImage getSpriteAura(int widgetId, int spriteId, Color color)
    {
        long key = (((long) (widgetId > 0 ? widgetId : spriteId)) << 32) | (color.getRGB() & 0xFFFFFFFFL);
        BufferedImage cached = spriteAuraCache.get(key);
        if (cached != null)
        {
            return cached;
        }
        BufferedImage sprite = resolveSprite(widgetId, spriteId);
        if (sprite == null)
        {
            return null;
        }
        BufferedImage aura = ItemOutlineCache.buildOutwardAura(sprite, color, ItemOutlineCache.AURA_PAD);
        if (spriteAuraCache.size() > 128)
        {
            spriteAuraCache.clear();
        }
        spriteAuraCache.put(key, aura);
        return aura;
    }

    private static CombatSpell matchSpell(String cleanedName)
    {
        for (CombatSpell spell : CombatSpell.values())
        {
            if (spell.matchesWidgetName(cleanedName))
            {
                return spell;
            }
        }
        return null;
    }
}
