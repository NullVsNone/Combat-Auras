package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.CombatSpell;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.VarbitID;

/**
 * Deprioritizes the left-click "Cast" on Vengeance while it is still charged or on its recast cooldown,
 * so a misclick does not waste a cast (same idea as the Plugin Hub's no-misclick-veng).
 * The entry is only moved below "Cancel"; it is never removed, so a right-click cast still works.
 */
public class SpellRecastProtection
{
    public void apply(Client client, ImmersiveCombatOverlaysConfig config, CombatSpellTracker spellTracker)
    {
        if (!config.vengeanceRecastProtection())
        {
            return;
        }
        boolean charged = client.getVarbitValue(VarbitID.VENGEANCE_REBOUND) == 1;
        boolean onCooldown = !spellTracker.getCooldown(CombatSpell.VENGEANCE).isReady();
        if (!charged && !onCooldown)
        {
            return;
        }

        MenuEntry[] entries = client.getMenu().getMenuEntries();
        if (entries == null)
        {
            return;
        }
        for (MenuEntry entry : entries)
        {
            if (entry != null && isVengeanceCast(entry.getOption(), entry.getTarget()))
            {
                entry.setDeprioritized(true);
            }
        }
    }

    static boolean isVengeanceCast(String option, String target)
    {
        if (option == null || target == null || !"Cast".equals(option))
        {
            return false;
        }
        return "Vengeance".equals(CombatSpellTracker.stripTags(target).trim());
    }
}
