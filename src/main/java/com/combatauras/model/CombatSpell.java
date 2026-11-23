package com.combatauras.model;

import net.runelite.api.gameval.VarbitID;

/**
 * Combat spells that expire or have a recast cooldown.
 * All timers track time until the spell can be recast (next recast time), which is the
 * later of the buff ending and the recast cooldown ending.
 *
 * Varbit IDs and durations mirror RuneLite's core Timers & Buffs plugin.
 */
public enum CombatSpell
{
    RESURRECT_THRALL(
        "Resurrect Thrall",
        Spellbook.ARCEUUS,
        2979,
        VarbitID.ARCEUUS_RESURRECTION_ACTIVE,
        VarbitID.ARCEUUS_RESURRECTION_COOLDOWN,
        17,
        new String[]{
            "Resurrect Lesser Ghost", "Resurrect Superior Ghost", "Resurrect Greater Ghost",
            "Resurrect Lesser Skeleton", "Resurrect Superior Skeleton", "Resurrect Greater Skeleton",
            "Resurrect Lesser Zombie", "Resurrect Superior Zombie", "Resurrect Greater Zombie"
        }
    ),
    DEATH_CHARGE(
        "Death Charge",
        Spellbook.ARCEUUS,
        1310,
        VarbitID.ARCEUUS_DEATH_CHARGE_ACTIVE,
        VarbitID.ARCEUUS_DEATH_CHARGE_COOLDOWN,
        100,
        new String[]{"Death Charge"}
    ),
    MARK_OF_DARKNESS(
        "Mark of Darkness",
        Spellbook.ARCEUUS,
        1305,
        -1,
        -1,
        10,
        new String[]{"Mark of Darkness"}
    ),
    WARD_OF_ARCEUUS(
        "Ward of Arceuus",
        Spellbook.ARCEUUS,
        1306,
        -1,
        VarbitID.ARCEUUS_WARD_COOLDOWN,
        50,
        new String[]{"Ward of Arceuus"}
    ),
    SHADOW_VEIL(
        "Shadow Veil",
        Spellbook.ARCEUUS,
        1315,
        VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE,
        VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN,
        50,
        new String[]{"Shadow Veil"}
    ),
    CORRUPTION(
        "Corruption",
        Spellbook.ARCEUUS,
        1307,
        -1,
        VarbitID.ARCEUUS_CORRUPTION_COOLDOWN,
        50,
        new String[]{"Lesser Corruption", "Greater Corruption"}
    ),
    VENGEANCE(
        "Vengeance",
        Spellbook.LUNAR,
        564,
        -1,
        VarbitID.VENGEANCE_TIMELIMIT,
        50,
        new String[]{"Vengeance"}
    ),
    VENGEANCE_OTHER(
        "Vengeance Other",
        Spellbook.LUNAR,
        561,
        -1,
        VarbitID.VENGEANCE_TIMELIMIT,
        50,
        new String[]{"Vengeance Other"}
    ),
    CHARGE(
        "Charge",
        Spellbook.STANDARD,
        322,
        -1,
        -1,
        0,
        new String[]{"Charge"}
    );

    public enum Spellbook
    {
        STANDARD(0),
        ANCIENT(1),
        LUNAR(2),
        ARCEUUS(3);

        private final int varbitValue;

        Spellbook(int varbitValue)
        {
            this.varbitValue = varbitValue;
        }

        public int getVarbitValue()
        {
            return varbitValue;
        }
    }

    private final String displayName;
    private final Spellbook spellbook;
    private final int fallbackSpriteId;
    private final int activeVarbitId;
    private final int cooldownVarbitId;
    private final int cooldownTicks;
    private final String[] widgetNames;

    CombatSpell(
        String displayName,
        Spellbook spellbook,
        int fallbackSpriteId,
        int activeVarbitId,
        int cooldownVarbitId,
        int cooldownTicks,
        String[] widgetNames
    )
    {
        this.displayName = displayName;
        this.spellbook = spellbook;
        this.fallbackSpriteId = fallbackSpriteId;
        this.activeVarbitId = activeVarbitId;
        this.cooldownVarbitId = cooldownVarbitId;
        this.cooldownTicks = cooldownTicks;
        this.widgetNames = widgetNames;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public Spellbook getSpellbook()
    {
        return spellbook;
    }

    public int getFallbackSpriteId()
    {
        return fallbackSpriteId;
    }

    /**
     * Varbit that is non-zero while the spell's buff/summon is active, or -1 if the game exposes none.
     */
    public int getActiveVarbitId()
    {
        return activeVarbitId;
    }

    /**
     * Varbit that is 1 while the spell is on its recast cooldown, or -1 if the game exposes none.
     */
    public int getCooldownVarbitId()
    {
        return cooldownVarbitId;
    }

    /**
     * Length of the recast cooldown in game ticks (0 if the spell has none).
     */
    public int getCooldownTicks()
    {
        return cooldownTicks;
    }

    public boolean matchesWidgetName(String cleanedName)
    {
        if (cleanedName == null || cleanedName.isEmpty())
        {
            return false;
        }
        for (String name : widgetNames)
        {
            if (name.equalsIgnoreCase(cleanedName))
            {
                return true;
            }
        }
        return false;
    }
}
