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
            observe(key(boost), ReminderPolicy.activeBeyondLead(
                ItemCooldowns.boostCooldown(boost, s, client, potionTimers, boostTimers), boostLead), nowNanos);
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
                else if (entry.effect != null && potionOk && s.potionRemind(entry.effect.ordinal())
                    && !isImmunity(entry.effect))
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
        itemCount = dropBoostsCoveredByDivine(count);
    }

    /**
     * When a divine potion ends, the regular potions for the same stat expire on the same tick. Show only the
     * divine's reminder (one alert, not two competing colours); a boost potion stays if no divine covers it.
     */
    private int dropBoostsCoveredByDivine(int count)
    {
        int kept = 0;
        for (int i = 0; i < count; i++)
        {
            if (itemFamilies[i] == ReminderFamily.BOOST.ordinal() && coveredByShownDivine(catalog.get(itemIds[i]).boost, count))
            {
                continue;
            }
            itemIds[kept] = itemIds[i];
            itemColors[kept] = itemColors[i];
            itemFamilies[kept] = itemFamilies[i];
            kept++;
        }
        if (kept != count)
        {
            Arrays.fill(familyLive, false);
            for (int i = 0; i < kept; i++)
            {
                familyLive[itemFamilies[i]] = true;
            }
        }
        return kept;
    }

    private boolean coveredByShownDivine(BoostPotion boost, int count)
    {
        if (boost == null)
        {
            return false;
        }
        for (int i = 0; i < count; i++)
        {
            if (itemFamilies[i] != ReminderFamily.POTION.ordinal())
            {
                continue;
            }
            PotionEffect effect = catalog.get(itemIds[i]).effect;
            if (effect != null && effect.isDivine() && ItemCooldowns.overlaps(boost, effect))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Antipoison / anti-venom expiry is shown by the Poison / Venom warning (its own section), not as a potion
     * reminder. The item overlays still show the immunity timer.
     */
    public static boolean isImmunity(PotionEffect effect)
    {
        return effect == PotionEffect.ANTIPOISON || effect == PotionEffect.ANTIVENOM;
    }

    /** Whether the immunity of {@code effect} just ended and its Poison / Venom reminder should show. */
    public boolean immunityEnded(PotionEffect effect, SettingsSnapshot s, int durationSeconds, long nowNanos)
    {
        return s.potionRemind(effect.ordinal()) && isShowing(key(effect), durationSeconds, nowNanos);
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

    /**
     * Whether a showing expiry reminder already asks for a re-pot of this stat: a divine potion holding it, a boost
     * potion raising it, or an overload / smelling salts. Client thread; scans at most {@link #MAX_ITEMS} entries.
     */
    public boolean isRepotShowing(net.runelite.api.Skill skill)
    {
        for (int i = 0; i < itemCount; i++)
        {
            ConsumableCatalog.Entry entry = catalog.get(itemIds[i]);
            if (entry.boost != null && contains(entry.boost.getSkills(), skill))
            {
                return true;
            }
            PotionEffect effect = entry.effect;
            if (effect == null)
            {
                continue;
            }
            if (effect == PotionEffect.OVERLOAD || effect == PotionEffect.BLIGHTED_OVERLOAD || effect == PotionEffect.SMELLING_SALTS
                || contains(effect.getDivineSkills(), skill))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(net.runelite.api.Skill[] skills, net.runelite.api.Skill skill)
    {
        for (net.runelite.api.Skill s : skills)
        {
            if (s == skill)
            {
                return true;
            }
        }
        return false;
    }

    public boolean isFamilyLive(ReminderFamily family)
    {
        return familyLive[family.ordinal()];
    }
}
