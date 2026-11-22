package com.combatauras.overlay;

import com.combatauras.config.CombatAurasConfig;
import com.combatauras.model.Cooldown;
import com.combatauras.render.CooldownRenderer;
import com.combatauras.render.ItemOutlineCache;
import com.combatauras.render.ProceduralRenderer;
import java.awt.Composite;
import java.awt.image.BufferedImage;
import com.combatauras.tracker.ConsumableCatalog;
import com.combatauras.tracker.FoodCooldownTracker;
import com.combatauras.tracker.ItemCooldowns;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import com.combatauras.model.AnimationMode;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Renders cooldowns and ready indicators over inventory items (hearts, potions, boost potions).
 */
@Singleton
public class InventoryCooldownOverlay extends WidgetItemOverlay
{
    private final Client client;
    private final CombatAurasConfig config;
    private final ItemManager itemManager;
    private final ItemCooldowns cooldowns;
    private final FoodCooldownTracker foodTracker;
    private final ImmersiveStatusOverlay statusOverlay;
    private final ItemOutlineCache prayerOutlineCache = new ItemOutlineCache();
    private final CooldownRenderer renderer;

    // Per-inventory-slot ready transition tracking (no per-frame allocation)
    private static final int SLOT_COUNT = 28;
    private final int[] slotItemId = new int[SLOT_COUNT];
    private final boolean[] slotWasCooling = new boolean[SLOT_COUNT];
    private final long[] slotReadySinceNanos = new long[SLOT_COUNT];

    @Inject
    public InventoryCooldownOverlay(
        Client client,
        CombatAurasConfig config,
        ItemManager itemManager,
        ItemCooldowns cooldowns,
        FoodCooldownTracker foodTracker,
        ImmersiveStatusOverlay statusOverlay,
        CooldownRenderer renderer
    )
    {
        this.client = client;
        this.config = config;
        this.itemManager = itemManager;
        this.cooldowns = cooldowns;
        this.foodTracker = foodTracker;
        this.statusOverlay = statusOverlay;
        this.renderer = renderer;
        showOnInventory();
    }

    public void clearCache()
    {
        prayerOutlineCache.clear();
        java.util.Arrays.fill(slotItemId, 0);
        java.util.Arrays.fill(slotWasCooling, false);
        java.util.Arrays.fill(slotReadySinceNanos, 0L);
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
        boolean permanent = config.animationMode() == AnimationMode.PERMANENT;
        float strength = CooldownRenderer.readyStrength(
            config.readyStyle(), permanent, config.readyDurationSeconds(), nowNanos - slotReadySinceNanos[slot], nowNanos);
        if (strength <= 0f && !permanent)
        {
            // Highlight finished: forget the transition until the next cooldown
            slotWasCooling[slot] = false;
        }
        return strength;
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        Rectangle bounds = widgetItem.getCanvasBounds();
        if (bounds == null)
        {
            return;
        }

        long nowNanos = System.nanoTime();
        ConsumableCatalog.Entry item = cooldowns.catalog().get(itemId);
        if (!item.isTracked() && !item.prayerRestore && !config.debugPreviewInventory())
        {
            return; // most inventory items: one cached lookup and done
        }

        // Glow prayer-restore potions in inventory when Low Prayer is active
        if (config.allowItemGlows() && config.lowPrayerInventoryGlow() && statusOverlay != null && statusOverlay.isPrayerLow() && item.prayerRestore)
        {
            BufferedImage outline = prayerOutlineCache.getOutline(itemManager, itemId, config.lowPrayerColor());
            if (outline != null)
            {
                double pulse = 0.55 + 0.45 * Math.sin((nowNanos / 1_000_000_000.0) * 4.2);
                Composite oldComp = graphics.getComposite();
                graphics.setComposite(ProceduralRenderer.quantizedAlpha((float) pulse));
                graphics.drawImage(outline, bounds.x, bounds.y, null);
                graphics.setComposite(oldComp);
            }
        }

        Cooldown cooldown = cooldowns.of(itemId);
        if (cooldown == null && config.foodCooldownsEnabled())
        {
            cooldown = foodTracker.getCooldown(item.food);
        }
        int slot = widgetItem.getWidget() != null ? widgetItem.getWidget().getIndex() : -1;
        boolean debugSlot = false;
        if (cooldown == null && config.debugPreviewInventory())
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
            ? CooldownRenderer.readyStrength(config.readyStyle(), true, config.readyDurationSeconds(), 0L, nowNanos)
            : trackReady(slot, itemId, cooldown.isReady(), nowNanos);

        renderer.renderItemCooldown(
            graphics,
            bounds,
            itemId,
            cooldown,
            config,
            itemManager,
            nowNanos,
            readyStrength
        );
    }
}
