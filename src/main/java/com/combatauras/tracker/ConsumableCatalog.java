package com.combatauras.tracker;

import com.combatauras.model.BoostPotion;
import com.combatauras.model.PotionEffect;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.IntFunction;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The single classifier for every consumable: which potion effect, boost, food timer, heart or prayer restore an
 * item is. Built from the item's real data (name, inventory actions, noted flag) once per item id and cached, so
 * the per-frame and per-menu-sort callers never touch strings.
 * <p>
 * New consumables are added in one place: {@link PotionItems} name rules for timed potions and boosts,
 * {@link FoodCooldownTracker#classify} for eat / drink timers. Anything drinkable with a dose suffix that matches
 * no rule is logged once at debug level, so game updates that add potions show up immediately.
 */
@Singleton
public class ConsumableCatalog
{
    private static final Logger log = LoggerFactory.getLogger(ConsumableCatalog.class);

    /** What an inventory item is, for every feature that cares. Immutable. */
    public static final class Entry
    {
        public final PotionEffect effect;
        public final BoostPotion boost;
        public final FoodCooldownTracker.ConsumableType food;
        public final boolean heart;
        public final boolean prayerRestore;

        Entry(PotionEffect effect, BoostPotion boost, FoodCooldownTracker.ConsumableType food, boolean heart, boolean prayerRestore)
        {
            this.effect = effect;
            this.boost = boost;
            this.food = food;
            this.heart = heart;
            this.prayerRestore = prayerRestore;
        }

        /** Something the cooldown / reminder features track. */
        public boolean isTracked()
        {
            return effect != null || boost != null || food != null || heart;
        }
    }

    public static final Entry NOTHING = new Entry(null, null, null, false, false);

    private final IntFunction<ItemComposition> lookup;
    private static final int MAX_ID = 1 << 20;
    // Indexed by item id (ids are dense and small): an inventory redraw looks up every item each frame without
    // boxing. Grows on demand. Client thread only.
    private Entry[] cache = new Entry[1 << 15];

    @Inject
    public ConsumableCatalog(ItemManager itemManager)
    {
        this(itemManager::getItemComposition);
    }

    public ConsumableCatalog(IntFunction<ItemComposition> lookup)
    {
        this.lookup = lookup;
    }

    public Entry get(int itemId)
    {
        if (itemId <= 0)
        {
            return NOTHING;
        }
        if (itemId >= MAX_ID)
        {
            return classifyComposition(itemId); // implausible id: classify without caching
        }
        if (itemId >= cache.length)
        {
            cache = Arrays.copyOf(cache, Integer.highestOneBit(itemId) << 1);
        }
        Entry entry = cache[itemId];
        if (entry == null)
        {
            entry = classifyComposition(itemId);
            cache[itemId] = entry;
        }
        return entry;
    }

    private Entry classifyComposition(int itemId)
    {
        ItemComposition comp = lookup.apply(itemId);
        return comp == null ? NOTHING
            : classify(itemId, comp.getName(), comp.getInventoryActions(), comp.getNote() != -1);
    }

    public void clear()
    {
        Arrays.fill(cache, null);
    }

    public static Entry classify(int itemId, String name, String[] actions, boolean noted)
    {
        if (noted || name == null)
        {
            return NOTHING;
        }
        boolean heart = HeartCooldownTracker.isHeart(itemId);
        PotionEffect effect = PotionItems.getEffect(name);
        BoostPotion boost = effect == null ? PotionItems.getBoost(name) : null;
        FoodCooldownTracker.ConsumableType food = FoodCooldownTracker.classify(name, actions, false);
        boolean prayerRestore = isPrayerRestore(name);
        if (food == FoodCooldownTracker.ConsumableType.POTION && effect == null && boost == null && !prayerRestore
            && log.isDebugEnabled())
        {
            log.debug("Unrecognised potion (no timer or boost rule): {} ({})", name, itemId);
        }
        if (!heart && effect == null && boost == null && food == null && !prayerRestore)
        {
            return NOTHING;
        }
        return new Entry(effect, boost, food, heart, prayerRestore);
    }

    public static boolean isPrayerRestore(String name)
    {
        String lower = name.toLowerCase(Locale.ROOT);
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
