package com.immersivecombatoverlays.overlay;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.Cooldown;
import com.immersivecombatoverlays.render.CooldownRenderer;
import com.immersivecombatoverlays.render.ItemOutlineCache;
import com.immersivecombatoverlays.render.ProceduralRenderer;
import java.awt.Composite;
import java.awt.image.BufferedImage;
import com.immersivecombatoverlays.tracker.BoostPotionTimers;
import com.immersivecombatoverlays.tracker.FoodCooldownTracker;
import com.immersivecombatoverlays.tracker.HeartCooldownTracker;
import com.immersivecombatoverlays.tracker.PotionTimers;
import com.immersivecombatoverlays.tracker.RepotProtection;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
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
    private final ImmersiveCombatOverlaysConfig config;
    private final ItemManager itemManager;
    private final HeartCooldownTracker heartTracker;
    private final PotionTimers potionTimers;
    private final BoostPotionTimers boostTimers;
    private final FoodCooldownTracker foodTracker;
    private final ImmersiveStatusOverlay statusOverlay;
    private final ItemOutlineCache prayerOutlineCache = new ItemOutlineCache();
    private final CooldownRenderer renderer;
    private final Map<Integer, String> itemNameCache = new ConcurrentHashMap<>();

    @Inject
    public InventoryCooldownOverlay(
        Client client,
        ImmersiveCombatOverlaysConfig config,
        ItemManager itemManager,
        HeartCooldownTracker heartTracker,
        PotionTimers potionTimers,
        BoostPotionTimers boostTimers,
        FoodCooldownTracker foodTracker,
        ImmersiveStatusOverlay statusOverlay,
        CooldownRenderer renderer
    )
    {
        this.client = client;
        this.config = config;
        this.itemManager = itemManager;
        this.heartTracker = heartTracker;
        this.potionTimers = potionTimers;
        this.boostTimers = boostTimers;
        this.foodTracker = foodTracker;
        this.statusOverlay = statusOverlay;
        this.renderer = renderer;
        showOnInventory();
    }

    public void clearCache()
    {
        itemNameCache.clear();
        prayerOutlineCache.clear();
    }

    public void cacheInventoryItemNames()
    {
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        if (inv == null)
        {
            return;
        }
        for (Item item : inv.getItems())
        {
            if (item != null && item.getId() > 0 && !itemNameCache.containsKey(item.getId()))
            {
                ItemComposition comp = itemManager.getItemComposition(item.getId());
                if (comp != null && comp.getName() != null)
                {
                    itemNameCache.put(item.getId(), comp.getName());
                }
            }
        }
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
        String name = itemNameCache.get(itemId);

        // Glow prayer-restore potions in inventory when Low Prayer is active
        if (config.lowPrayerInventoryGlow() && statusOverlay != null && statusOverlay.isPrayerLow() && isPrayerRestoreItem(name))
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

        Cooldown cooldown = RepotProtection.resolveItemCooldown(
            client, config, heartTracker, potionTimers, boostTimers, itemId, name
        );
        if (cooldown == null && config.foodCooldownsEnabled() && name != null)
        {
            cooldown = foodTracker.getCooldown(name);
        }
        if (cooldown == null && config.debugPreviewInventory())
        {
            int idx = widgetItem.getWidget() != null ? widgetItem.getWidget().getIndex() : (itemId & 3);
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
            }
        }
        if (cooldown == null)
        {
            return;
        }

        renderer.renderItemCooldown(
            graphics,
            bounds,
            itemId,
            cooldown,
            config,
            itemManager,
            nowNanos
        );
    }
    public static boolean isPrayerRestoreItem(String name)
    {
        if (name == null || name.isEmpty())
        {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.contains("prayer potion")
            || lower.contains("super restore")
            || lower.contains("sanfew serum")
            || lower.contains("moonlight potion")
            || lower.contains("prayer enhance")
            || lower.contains("prayer regeneration")
            || lower.contains("ancient brew")
            || lower.contains("forgotten brew");
    }
}
