package com.combatauras.tracker;

import com.combatauras.model.BoostPotion;
import com.combatauras.model.Cooldown;
import com.combatauras.model.PotionEffect;
import com.combatauras.model.ReminderFamily;
import com.combatauras.settings.Settings;
import com.combatauras.settings.SettingsSnapshot;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;

/**
 * The one answer to "is this item on cooldown?", shared by the inventory overlay and re-dose protection.
 */
@Singleton
public class ItemCooldowns
{
    private final Client client;
    private final Settings settings;
    private final ConsumableCatalog catalog;
    private final HeartCooldownTracker heartTracker;
    private final PotionTimers potionTimers;
    private final BoostPotionTimers boostTimers;

    @Inject
    public ItemCooldowns(Client client, Settings settings, ConsumableCatalog catalog, HeartCooldownTracker heartTracker,
        PotionTimers potionTimers, BoostPotionTimers boostTimers)
    {
        this.client = client;
        this.settings = settings;
        this.catalog = catalog;
        this.heartTracker = heartTracker;
        this.potionTimers = potionTimers;
        this.boostTimers = boostTimers;
    }

    private static final PotionEffect[] DIVINES = java.util.Arrays.stream(PotionEffect.values())
        .filter(PotionEffect::isDivine).toArray(PotionEffect[]::new);

    /** Whether a divine potion holds at least one of the skills this boost potion raises. */
    static boolean overlaps(BoostPotion boost, PotionEffect divine)
    {
        for (net.runelite.api.Skill a : boost.getSkills())
        {
            for (net.runelite.api.Skill b : divine.getDivineSkills())
            {
                if (a == b)
                {
                    return true;
                }
            }
        }
        return false;
    }

    public ConsumableCatalog catalog()
    {
        return catalog;
    }

    /**
     * Heart, timed potion or boost cooldown of this item, or null when the item has none (or it is switched off).
     */
    public Cooldown of(int itemId)
    {
        return resolve(catalog.get(itemId), settings.get(), client, heartTracker, potionTimers, boostTimers);
    }

    /** The "warn early" lead (ticks) of the reminder family this item belongs to; 0 when it has none. */
    public int leadTicks(int itemId)
    {
        ConsumableCatalog.Entry item = catalog.get(itemId);
        SettingsSnapshot s = settings.get();
        if (item.heart)
        {
            return s.familyLeadTicks(ReminderFamily.HEART.ordinal());
        }
        if (item.effect != null)
        {
            return s.leadTicks(item.effect);
        }
        if (item.boost != null)
        {
            return s.familyLeadTicks(ReminderFamily.BOOST.ordinal());
        }
        return 0;
    }

    static Cooldown resolve(ConsumableCatalog.Entry item, SettingsSnapshot s, Client client,
        HeartCooldownTracker heartTracker, PotionTimers potionTimers, BoostPotionTimers boostTimers)
    {
        if (item.heart)
        {
            return s.familyEnabled(ReminderFamily.HEART)
                ? heartTracker.getCooldown() : null;
        }
        PotionEffect effect = item.effect;
        if (effect != null)
        {
            return s.isTracked(effect) ? potionTimers.getCooldown(effect) : null;
        }
        BoostPotion boost = item.boost;
        if (boost == null || !s.familyEnabled(ReminderFamily.BOOST))
        {
            return null;
        }
        Cooldown boostCd = boostCooldown(boost, s, client, potionTimers, boostTimers);
        if (boost == BoostPotion.GUTHIXIAN_ROOT)
        {
            // Guthixian root also restores run energy like a stamina; show whichever lasts longer
            Cooldown stamCd = potionTimers.getCooldown(PotionEffect.STAMINA);
            if (stamCd != null && !stamCd.isReady()
                && (boostCd == null || boostCd.isReady() || stamCd.getRemainingTicks() > boostCd.getRemainingTicks()))
            {
                return stamCd;
            }
        }
        return boostCd;
    }

    /**
     * Time until a boost potion is worth re-dosing. Skills an active divine potion holds snap back to base when it
     * ends, so for those the divine's remaining time is the answer rather than the decay estimate.
     */
    public static Cooldown boostCooldown(BoostPotion boost, SettingsSnapshot s, Client client,
        PotionTimers potionTimers, BoostPotionTimers boostTimers)
    {
        // Skills an active divine potion holds: they snap back to base when it ends, so that is the re-dose time
        int held = 0;
        Cooldown divine = null;
        for (PotionEffect e : DIVINES)
        {
            Cooldown cd = potionTimers.getCooldown(e);
            if (cd == null || cd.isReady())
            {
                continue;
            }
            for (net.runelite.api.Skill skill : e.getDivineSkills())
            {
                held |= BoostPotionTimers.skillBit(skill);
            }
            if (overlaps(boost, e) && (divine == null || cd.getRemainingTicks() > divine.getRemainingTicks()))
            {
                divine = cd;
            }
        }
        Cooldown boostCd = boostTimers.getCooldown(client, boost, s, held);
        if (divine != null && (boostCd == null || boostCd.isReady() || divine.getRemainingTicks() >= boostCd.getRemainingTicks()))
        {
            return divine;
        }
        return boostCd;
    }

    /**
     * A Menaphite remedy strips divine effects (divine potions, saturated heart), so drinking one by
     * accident while they run throws away the no-decay part of those boosts.
     */
    public static boolean isDivineBoostActive(HeartCooldownTracker heartTracker, PotionTimers potionTimers)
    {
        for (PotionEffect effect : PotionEffect.values())
        {
            if (effect.isDivine())
            {
                Cooldown cd = potionTimers.getCooldown(effect);
                if (cd != null && !cd.isReady())
                {
                    return true;
                }
            }
        }
        return heartTracker.isSaturatedBoostActive();
    }

    public boolean isDivineBoostActive()
    {
        return isDivineBoostActive(heartTracker, potionTimers);
    }
}
