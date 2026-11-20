package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.CombatSpell;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.VarbitID;

/**
 * Deprioritizes the left-click "Cast" on a spell that cannot be recast yet (thrall still up, Vengeance still
 * charged, any spell on its recast cooldown), so a misclick does not waste the click (same idea as the
 * Plugin Hub's no-misclick-veng, extended to every tracked spell with a per-spell toggle).
 * The entry is only moved below "Cancel"; it is never removed, so a right-click cast still works.
 */
public class SpellRecastProtection
{
    private static final CombatSpell[] SPELLS = CombatSpell.values();
    private final boolean[] blocked = new boolean[SPELLS.length];

    public void apply(Client client, ImmersiveCombatOverlaysConfig config, CombatSpellTracker spellTracker)
    {
        boolean any = false;
        for (int i = 0; i < SPELLS.length; i++)
        {
            CombatSpell spell = SPELLS[i];
            blocked[i] = ImmersiveCombatOverlaysConfig.isSpellRecastProtected(config, spell) && !canRecast(client, spell, spellTracker);
            any |= blocked[i];
        }
        if (!any)
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
            if (entry == null || !"Cast".equals(entry.getOption()) || entry.getTarget() == null)
            {
                continue;
            }
            CombatSpell spell = castSpell(entry.getTarget());
            if (spell != null && blocked[spell.ordinal()])
            {
                entry.setDeprioritized(true);
            }
        }
    }

    private static boolean canRecast(Client client, CombatSpell spell, CombatSpellTracker spellTracker)
    {
        if (spell == CombatSpell.VENGEANCE && client.getVarbitValue(VarbitID.VENGEANCE_REBOUND) == 1)
        {
            return false;
        }
        return spellTracker.getCooldown(spell).isReady();
    }

    /**
     * The tracked spell a "Cast" entry's target names (e.g. "Resurrect Greater Ghost"), or null.
     */
    static CombatSpell castSpell(String target)
    {
        String name = CombatSpellTracker.stripTags(target).trim();
        for (CombatSpell spell : SPELLS)
        {
            if (spell.matchesWidgetName(name))
            {
                return spell;
            }
        }
        return null;
    }
}
