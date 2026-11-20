package com.immersivecombatoverlays.model;

/**
 * Combat spells that expire or have a recast cooldown.
 * All timers track time until the spell can be recast (next recast time).
 */
public enum CombatSpell
{
    RESURRECT_THRALL(
        "Resurrect Thrall",
        Spellbook.ARCEUUS,
        2979,
        12413,
        100,
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
        12411,
        100,
        new String[]{"Death Charge"}
    ),
    MARK_OF_DARKNESS(
        "Mark of Darkness",
        Spellbook.ARCEUUS,
        1305,
        12414,
        300,
        new String[]{"Mark of Darkness"}
    ),
    WARD_OF_ARCEUUS(
        "Ward of Arceuus",
        Spellbook.ARCEUUS,
        1306,
        12415,
        50,
        new String[]{"Ward of Arceuus"}
    ),
    SHADOW_VEIL(
        "Shadow Veil",
        Spellbook.ARCEUUS,
        1315,
        12412,
        100,
        new String[]{"Shadow Veil"}
    ),
    CORRUPTION(
        "Corruption",
        Spellbook.ARCEUUS,
        1307,
        -1,
        100,
        new String[]{"Lesser Corruption", "Greater Corruption"}
    ),
    VENGEANCE(
        "Vengeance",
        Spellbook.LUNAR,
        564,
        2451,
        50,
        new String[]{"Vengeance"}
    ),
    VENGEANCE_OTHER(
        "Vengeance Other",
        Spellbook.LUNAR,
        561,
        2451,
        50,
        new String[]{"Vengeance Other"}
    ),
    CHARGE(
        "Charge",
        Spellbook.STANDARD,
        322,
        -1,
        100,
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
    private final int cooldownVarbitId;
    private final int defaultRecastTicks;
    private final String[] widgetNames;

    CombatSpell(
        String displayName,
        Spellbook spellbook,
        int fallbackSpriteId,
        int cooldownVarbitId,
        int defaultRecastTicks,
        String[] widgetNames
    )
    {
        this.displayName = displayName;
        this.spellbook = spellbook;
        this.fallbackSpriteId = fallbackSpriteId;
        this.cooldownVarbitId = cooldownVarbitId;
        this.defaultRecastTicks = defaultRecastTicks;
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

    public int getCooldownVarbitId()
    {
        return cooldownVarbitId;
    }

    public int getDefaultRecastTicks()
    {
        return defaultRecastTicks;
    }

    public String[] getWidgetNames()
    {
        return widgetNames;
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
