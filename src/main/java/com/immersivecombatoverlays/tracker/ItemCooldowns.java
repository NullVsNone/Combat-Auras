package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.BoostPotion;
import com.immersivecombatoverlays.model.Cooldown;
import com.immersivecombatoverlays.model.PotionEffect;
import com.immersivecombatoverlays.model.ReminderFamily;
import com.immersivecombatoverlays.settings.Settings;
import com.immersivecombatoverlays.settings.SettingsSnapshot;
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
        Cooldown boostCd = boostTimers.getCooldown(client, boost, s);
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
