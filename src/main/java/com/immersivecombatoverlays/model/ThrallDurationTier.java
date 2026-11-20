package com.immersivecombatoverlays.model;

public enum ThrallDurationTier
{
    STANDARD("Standard (100%)", 1.0f),
    MASTER_CA("Master+ CA (200%)", 2.0f);

    private final String label;
    private final float multiplier;

    ThrallDurationTier(String label, float multiplier)
    {
        this.label = label;
        this.multiplier = multiplier;
    }

    public float getMultiplier()
    {
        return multiplier;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
