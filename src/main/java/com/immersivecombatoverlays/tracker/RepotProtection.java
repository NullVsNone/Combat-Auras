package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.BoostPotion;
import com.immersivecombatoverlays.model.Cooldown;
import com.immersivecombatoverlays.model.PotionEffect;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.client.util.Text;

/**
 * Prevents accidental left-click re-dosing of hearts or potions that are still on cooldown.
 */
public class RepotProtection
{
    public void apply(
        Client client,
        ImmersiveCombatOverlaysConfig config,
        HeartCooldownTracker heartTracker,
        PotionTimers potionTimers,
        BoostPotionTimers boostTimers
    )
    {
        if (!config.repotProtectionEnabled())
        {
            return;
        }

        MenuEntry[] entries = client.getMenu().getMenuEntries();
        if (entries == null || entries.length == 0)
        {
            return;
        }

        for (int i = entries.length - 1; i >= 0; i--)
        {
            MenuEntry entry = entries[i];
            if (entry == null)
            {
                continue;
            }
            String option = entry.getOption();
            if (!"Drink".equals(option) && !"Invigorate".equals(option) && !"Apply".equals(option) && !"Crush".equals(option))
            {
                continue;
            }

            int itemId = entry.getItemId();
            if (itemId <= 0)
            {
                continue;
            }

            String target = entry.getTarget() != null ? Text.removeTags(entry.getTarget()) : null;
            Cooldown cd = resolveItemCooldown(client, config, heartTracker, potionTimers, boostTimers, itemId, target);
            if ((cd != null && !cd.isReady())
                || (config.menaphiteDivineGuard() && PotionItems.getEffect(itemId, target) == PotionEffect.MENAPHITE_REMEDY
                    && isDivineBoostActive(heartTracker, potionTimers)))
            {
                entry.setDeprioritized(true);
            }
        }
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

    public static Cooldown resolveItemCooldown(
        Client client,
        ImmersiveCombatOverlaysConfig config,
        HeartCooldownTracker heartTracker,
        PotionTimers potionTimers,
        BoostPotionTimers boostTimers,
        int itemId,
        String itemName
    )
    {
        if (config.heartsEnabled() && HeartCooldownTracker.isHeart(itemId))
        {
            return heartTracker.getCooldown(itemId);
        }

        PotionEffect effect = PotionItems.getEffect(itemId, itemName);
        if (effect != null)
        {
            boolean enabled = (effect.getCategory() == PotionEffect.Category.COOLDOWN && config.potionCooldownsEnabled())
                || (effect.getCategory() == PotionEffect.Category.EFFECT && config.potionEffectsEnabled())
                || (effect.getCategory() == PotionEffect.Category.RAID && config.raidPotionsEnabled());
            if (enabled)
            {
                return potionTimers.getCooldown(effect);
            }
        }

        if (config.boostPotionsEnabled())
        {
            BoostPotion boost = PotionItems.getBoost(itemId, itemName);
            if (boost != null)
            {
                Cooldown boostCd = boostTimers.getCooldown(client, boost, config);
                if (boost == BoostPotion.GUTHIXIAN_ROOT)
                {
                    Cooldown stamCd = potionTimers.getCooldown(PotionEffect.STAMINA);
                    if (stamCd != null && !stamCd.isReady()
                        && (boostCd == null || boostCd.isReady() || stamCd.getRemainingTicks() > boostCd.getRemainingTicks()))
                    {
                        return stamCd;
                    }
                }
                return boostCd;
            }
        }

        return null;
    }
}
