package com.combatauras.model;

/**
 * Thrall duration scaling. Completing the Master Combat Achievement tier doubles thrall duration.
 * AUTO reads the tier from the game, so most players never need to change it.
 */
public enum ThrallDurationTier
{
    AUTO("Auto-detect"),
    STANDARD("Standard (100%)"),
    MASTER_CA("Master+ CA (200%)");

    private final String label;

    ThrallDurationTier(String label)
    {
        this.label = label;
    }

    /**
     * @param masterTierComplete whether the game reports the Master CA tier as complete (used by AUTO)
     */
    public int getMultiplier(boolean masterTierComplete)
    {
        switch (this)
        {
            case MASTER_CA:
                return 2;
            case STANDARD:
                return 1;
            default:
                return masterTierComplete ? 2 : 1;
        }
    }

    @Override
    public String toString()
    {
        return label;
    }
}
