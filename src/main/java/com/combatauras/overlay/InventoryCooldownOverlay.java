package com.combatauras.overlay;

import com.combatauras.model.BorderStyle;
import com.combatauras.model.Cooldown;
import com.combatauras.render.CooldownRenderer;
import com.combatauras.render.Motion;
import com.combatauras.settings.Settings;
import com.combatauras.settings.SettingsSnapshot;
import com.combatauras.tracker.ConsumableCatalog;
import com.combatauras.tracker.FoodCooldownTracker;
import com.combatauras.tracker.ItemCooldowns;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Renders cooldowns and ready indicators over inventory items (hearts, potions, boost potions).
 */
@Singleton
public class InventoryCooldownOverlay extends WidgetItemOverlay
{
    private final Settings settings;
    private final ItemManager itemManager;
    private final ItemCooldowns cooldowns;
    private final FoodCooldownTracker foodTracker;
    private final StatusOverlay statusOverlay;
    private final CooldownRenderer renderer;

    // Per-inventory-slot ready transition tracking (no per-frame allocation)
    private static final int SLOT_COUNT = 28;
    private final int[] slotItemId = new int[SLOT_COUNT];
    private final boolean[] slotWasCooling = new boolean[SLOT_COUNT];
    private final long[] slotReadySinceNanos = new long[SLOT_COUNT];

    @Inject
    public InventoryCooldownOverlay(
        Settings settings,
        ItemManager itemManager,
        ItemCooldowns cooldowns,
        FoodCooldownTracker foodTracker,
        StatusOverlay statusOverlay,
        CooldownRenderer renderer
    )
    {
        this.settings = settings;
        this.itemManager = itemManager;
        this.cooldowns = cooldowns;
        this.foodTracker = foodTracker;
        this.statusOverlay = statusOverlay;
        this.renderer = renderer;
        showOnInventory();
    }

    public void clearCache()
    {
        Arrays.fill(slotItemId, 0);
        Arrays.fill(slotWasCooling, false);
        Arrays.fill(slotReadySinceNanos, 0L);
    }

    /**
     * Strength of the ready highlight for the item in {@code slot}. The highlight only appears after we saw the
     * item on cooldown and it finished, so unused potions sitting in the inventory never glow.
     */
    public float trackReady(int slot, int itemId, boolean ready, long nowNanos)
    {
        if (slot < 0 || slot >= SLOT_COUNT)
        {
            return 0f;
        }
        if (slotItemId[slot] != itemId)
        {
            slotItemId[slot] = itemId;
            slotWasCooling[slot] = false;
            slotReadySinceNanos[slot] = 0L;
        }
        if (!ready)
        {
            slotWasCooling[slot] = true;
            slotReadySinceNanos[slot] = 0L;
            return 0f;
        }
        if (!slotWasCooling[slot])
        {
            return 0f;
        }
        if (slotReadySinceNanos[slot] == 0L)
        {
            slotReadySinceNanos[slot] = nowNanos;
        }
        SettingsSnapshot s = settings.get();
        float strength = CooldownRenderer.readyStrength(
            s.readyStyle, s.readyPermanent, s.readyDurationSeconds, nowNanos - slotReadySinceNanos[slot], nowNanos, s.motionStrength);
        if (strength <= 0f && !s.readyPermanent)
        {
            // Highlight finished: forget the transition until the next cooldown
            slotWasCooling[slot] = false;
        }
        return strength;
    }

    /** One-tick fade-in of the ready highlight in {@code slot}, from when its cooldown finished. */
    public float readyFade(int slot, long nowNanos)
    {
        return slot < 0 || slot >= SLOT_COUNT ? 1f : Motion.fadeIn(slotReadySinceNanos[slot], nowNanos);
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        Rectangle bounds = widgetItem.getCanvasBounds();
        if (bounds == null)
        {
            return;
        }

        SettingsSnapshot s = settings.get();
        ConsumableCatalog.Entry item = cooldowns.catalog().get(itemId);
        if (!item.isTracked() && !item.prayerRestore && !s.debugPreviewInventory)
        {
            return; // most inventory items: one cached lookup and done
        }
        long nowNanos = System.nanoTime();

        // Glow prayer-restore potions while Low Prayer is active, in the inventory's ready border style
        if (item.prayerRestore && s.lowPrayerInventoryGlow && statusOverlay != null && statusOverlay.isPrayerLow())
        {
            BorderStyle style = s.readyBorder == BorderStyle.NONE ? BorderStyle.ITEM_OUTLINE : s.readyBorder;
            renderer.drawItemGlow(graphics, bounds, itemManager, itemId, s.lowPrayerColor, style,
                Motion.iconGlow(s.lowPrayerGlowAnimation, nowNanos, s.motionStrength),
                Motion.fadeIn(statusOverlay.prayerLowSinceNanos(), nowNanos), nowNanos, s);
        }

        Cooldown cooldown = cooldowns.of(itemId);
        if (cooldown == null && s.foodCooldownsEnabled)
        {
            cooldown = foodTracker.getCooldown(item.food);
        }
        int slot = widgetItem.getWidget() != null ? widgetItem.getWidget().getIndex() : -1;
        boolean debugSlot = false;
        if (cooldown == null && s.debugPreviewInventory)
        {
            int idx = slot >= 0 ? slot : (itemId & 3);
            if (idx >= 0 && idx < 4)
            {
                long cycleMs = (System.currentTimeMillis() + idx * 1800L) % 8000L;
                if (cycleMs < 5000L)
                {
                    int remTicks = Math.max(1, (int) Math.ceil((5000L - cycleMs) / 600.0));
                    cooldown = Cooldown.active(remTicks, 9);
                }
                else
                {
                    cooldown = Cooldown.ready();
                }
                debugSlot = true;
            }
        }
        if (cooldown == null)
        {
            return;
        }

        float readyStrength = debugSlot
            ? CooldownRenderer.readyStrength(s.readyStyle, true, s.readyDurationSeconds, 0L, nowNanos, s.motionStrength)
            : trackReady(slot, itemId, cooldown.isReady(), nowNanos);

        float fade = debugSlot ? 1f : readyFade(slot, nowNanos);
        renderer.renderItemCooldown(graphics, bounds, itemId, cooldown, s, itemManager, nowNanos, readyStrength, fade);
    }
}
