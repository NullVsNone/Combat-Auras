package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.CombatSpell;
import com.immersivecombatoverlays.settings.Settings;
import com.immersivecombatoverlays.settings.SettingsSnapshot;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.VarbitID;

/**
 * Deprioritizes the left-click "Cast" on a spell that cannot be recast yet (thrall still up, Vengeance still
 * charged, any spell on its recast cooldown), so a misclick does not waste the click (same idea as the
 * Plugin Hub's no-misclick-veng, extended to every tracked spell with a per-spell toggle).
 * The entry is only moved below "Cancel"; it is never removed, so a right-click cast still works.
 */
@Singleton
public class SpellRecastProtection
{
    private static final CombatSpell[] SPELLS = CombatSpell.values();
    private final boolean[] blocked = new boolean[SPELLS.length];

    private final Client client;
    private final Settings settings;
    private final CombatSpellTracker spellTracker;

    @Inject
    public SpellRecastProtection(Client client, Settings settings, CombatSpellTracker spellTracker)
    {
        this.client = client;
        this.settings = settings;
        this.spellTracker = spellTracker;
    }

    /** Call on ClientTick while the menu is closed (before the client sorts it). */
    public void apply()
    {
        apply(client, settings.get(), spellTracker);
    }

    public void apply(Client client, SettingsSnapshot s, CombatSpellTracker spellTracker)
    {
        boolean any = false;
        for (int i = 0; i < SPELLS.length; i++)
        {
            blocked[i] = s.spellRecastProtection(i) && !canRecast(client, SPELLS[i], spellTracker);
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
