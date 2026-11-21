package com.immersivecombatoverlays.model;

/**
 * When a spell reminder may show.
 */
public enum ReminderContext
{
    COMBAT("In combat"),
    THIEVING("While pickpocketing"),
    ALWAYS("Always");

    private final String label;

    ReminderContext(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
