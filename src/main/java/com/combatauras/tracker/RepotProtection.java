package com.combatauras.tracker;

import com.combatauras.config.CombatAurasConfig;
import com.combatauras.model.Cooldown;
import com.combatauras.model.PotionEffect;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;

/**
 * Deprioritizes left-click Drink / Invigorate / Apply / Crush on items still on cooldown, and on a Menaphite
 * remedy while a divine boost runs. The entry only moves below "Cancel"; right-click always still works.
 */
@Singleton
public class RepotProtection
{
    private final Client client;
    private final CombatAurasConfig config;
    private final ItemCooldowns cooldowns;

    @Inject
    public RepotProtection(Client client, CombatAurasConfig config, ItemCooldowns cooldowns)
    {
        this.client = client;
        this.config = config;
        this.cooldowns = cooldowns;
    }

    /** Call on ClientTick while the menu is closed (before the client sorts it). */
    public void apply()
    {
        if (!config.repotProtectionEnabled())
        {
            return;
        }
        MenuEntry[] entries = client.getMenu().getMenuEntries();
        if (entries == null)
        {
            return;
        }
        boolean guardMenaphite = config.menaphiteDivineGuard();
        for (MenuEntry entry : entries)
        {
            if (entry == null || !isDoseOption(entry.getOption()))
            {
                continue;
            }
            int itemId = entry.getItemId();
            if (itemId <= 0)
            {
                continue;
            }
            Cooldown cd = cooldowns.of(itemId);
            if ((cd != null && !cd.isReady())
                || (guardMenaphite && cooldowns.catalog().get(itemId).effect == PotionEffect.MENAPHITE_REMEDY
                    && cooldowns.isDivineBoostActive()))
            {
                entry.setDeprioritized(true);
            }
        }
    }

    static boolean isDoseOption(String option)
    {
        return "Drink".equals(option) || "Invigorate".equals(option) || "Apply".equals(option) || "Crush".equals(option);
    }
}
