package com.immersivecombatoverlays.model;

/**
 * How the screen-edge glow picks its colour when several warnings are active at once.
 */
public enum GlowColorMode
{
    BLEND("Blend all"),
    STRONGEST("Strongest warning");

    private final String label;

    GlowColorMode(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
