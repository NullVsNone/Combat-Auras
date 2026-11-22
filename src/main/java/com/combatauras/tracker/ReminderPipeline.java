package com.combatauras.tracker;

import com.combatauras.model.BoostPotion;
import com.combatauras.model.PotionEffect;
import com.combatauras.model.ReminderFamily;
import com.combatauras.settings.Settings;
import com.combatauras.settings.SettingsSnapshot;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

/**
 * Item reminders (heart, timed potions, boosts) in one pass per game tick:
 * <ol>
 *     <li>Expiry latch per effect: a reminder starts only on the tick the effect stops running beyond its
 *     "warn early" lead (so at the lead, or at expiry), lasts its family's duration, and clears when the effect
 *     is re-applied. An unused potion sitting in the inventory never triggers anything.</li>
 *     <li>Inventory scan through the {@link ConsumableCatalog}: one representative item per showing reminder,
 *     with its family colour, for the overlays to draw.</li>
 * </ol>
 * Results are plain arrays the overlays copy from; nothing here allocates per tick.
 */
@Singleton
public class ReminderPipeline
{
    public static final int HEART = 0;
    private static final int EFFECT_BASE = 1;
    private static final int BOOST_BASE = EFFECT_BASE + PotionEffect.values().length;
    private static final int KEY_COUNT = BOOST_BASE + BoostPotion.values().length;
    public static final int MAX_ITEMS = 16;

    private final Client client;
    private final Settings settings;
    private final ConsumableCatalog catalog;
    private final CombatStateTracker combatState;
    private final HeartCooldownTracker heartTracker;
    private final PotionTimers potionTimers;
    private final BoostPotionTimers boostTimers;

    private final boolean[] wasActive = new boolean[KEY_COUNT];
    private final long[] startedNanos = new long[KEY_COUNT];
    private final boolean[] seen = new boolean[KEY_COUNT];

    private final int[] itemIds = new int[MAX_ITEMS];
    private final int[] itemColors = new int[MAX_ITEMS];
    private final int[] itemFamilies = new int[MAX_ITEMS];
    private final boolean[] familyLive = new boolean[ReminderFamily.values().length];
    private volatile int itemCount = 0;

    @Inject
    public ReminderPipeline(Client client, Settings settings, ConsumableCatalog catalog, CombatStateTracker combatState,
        HeartCooldownTracker heartTracker, PotionTimers potionTimers, BoostPotionTimers boostTimers)
    {
        this.client = client;
        this.settings = settings;
        this.catalog = catalog;
        this.combatState = combatState;
        this.heartTracker = heartTracker;
        this.potionTimers = potionTimers;
        this.boostTimers = boostTimers;
    }

    public static int key(PotionEffect effect)
    {
        return EFFECT_BASE + effect.ordinal();
    }

    public static int key(BoostPotion boost)
    {
        return BOOST_BASE + boost.ordinal();
    }

    public void reset()
    {
        Arrays.fill(wasActive, false);
        Arrays.fill(startedNanos, 0L);
        Arrays.fill(familyLive, false);
        itemCount = 0;
    }

    /** Game tick (client thread), after the timers have updated. */
    public void update(long nowNanos)
    {
        SettingsSnapshot s = settings.get();
        observe(HEART, ReminderPolicy.activeBeyondLead(heartTracker.getCooldown(), s.familyLeadTicks(ReminderFamily.HEART.ordinal())), nowNanos);
        for (PotionEffect effect : PotionEffect.values())
        {
            observe(key(effect), ReminderPolicy.activeBeyondLead(potionTimers.getCooldown(effect), s.leadTicks(effect)), nowNanos);
        }
        int boostLead = s.familyLeadTicks(ReminderFamily.BOOST.ordinal());
        for (BoostPotion boost : BoostPotion.values())
        {
            observe(key(boost), ReminderPolicy.activeBeyondLead(boostTimers.getCooldown(client, boost, s), boostLead), nowNanos);
        }
        collectItems(s, nowNanos);
    }

    /**
     * Records this tick's state for one key.
     *
     * @param active whether the effect is still running beyond its lead
     */
    public void observe(int key, boolean active, long nowNanos)
    {
        if (active)
        {
            startedNanos[key] = 0L;
        }
        else if (wasActive[key])
        {
            startedNanos[key] = nowNanos;
        }
        wasActive[key] = active;
    }

    public boolean isShowing(int key, int durationSeconds, long nowNanos)
    {
        return ReminderPolicy.withinDuration(startedNanos[key], durationSeconds, nowNanos);
    }

    private void collectItems(SettingsSnapshot s, long nowNanos)
    {
        Arrays.fill(familyLive, false);
        Arrays.fill(seen, false);
        int count = 0;
        ItemContainer inv = client.getItemContainer(InventoryID.INV);
        if (inv != null)
        {
            boolean heartOk = allowed(s, ReminderFamily.HEART);
            boolean potionOk = allowed(s, ReminderFamily.POTION);
            boolean boostOk = allowed(s, ReminderFamily.BOOST);
            for (Item item : inv.getItems())
            {
                if (count >= MAX_ITEMS)
                {
                    break;
                }
                if (item == null || item.getId() <= 0)
                {
                    continue;
                }
                ConsumableCatalog.Entry entry = catalog.get(item.getId());
                ReminderFamily family;
                int key;
                if (entry.heart && heartOk)
                {
                    family = ReminderFamily.HEART;
                    key = HEART;
                }
                else if (entry.effect != null && potionOk && s.potionRemind(entry.effect.ordinal()))
                {
                    family = ReminderFamily.POTION;
                    key = key(entry.effect);
                }
                else if (entry.boost != null && boostOk)
                {
                    family = ReminderFamily.BOOST;
                    key = key(entry.boost);
                }
                else
                {
                    continue;
                }
                if (seen[key] || !isShowing(key, s.familyDurationSeconds(family.ordinal()), nowNanos))
                {
                    continue;
                }
                seen[key] = true;
                itemIds[count] = item.getId();
                itemColors[count] = s.familyColor(family.ordinal());
                itemFamilies[count] = family.ordinal();
                familyLive[family.ordinal()] = true;
                count++;
            }
        }
        itemCount = count;
    }

    private boolean allowed(SettingsSnapshot s, ReminderFamily family)
    {
        int f = family.ordinal();
        return s.familyEnabled(f) && ReminderPolicy.combatGate(s.familyOnlyInCombat(f), combatState);
    }

    public int itemCount()
    {
        return itemCount;
    }

    public int itemId(int i)
    {
        return itemIds[i];
    }

    public int itemColor(int i)
    {
        return itemColors[i];
    }

    /** ReminderFamily ordinal of item {@code i}. */
    public int itemFamily(int i)
    {
        return itemFamilies[i];
    }

    public boolean isFamilyLive(ReminderFamily family)
    {
        return familyLive[family.ordinal()];
    }
}
