package com.immersivecombatoverlays.model;

public enum SpellCooldownDisplayMode
{
    RADIAL_AND_TEXT("Radial + Text"),
    FILL_AND_TEXT("Fill + Text"),
    TEXT_ONLY("Text only"),
    RADIAL_ONLY("Radial only");

    private final String label;

    SpellCooldownDisplayMode(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
