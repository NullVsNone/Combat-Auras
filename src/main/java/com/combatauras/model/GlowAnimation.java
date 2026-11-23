package com.combatauras.model;

/** How an icon glow (spell icons, the Magic tab, the spec orb, prayer restores) animates while it is shown. */
public enum GlowAnimation
{
    PULSE("Pulse"),
    STEADY("Steady");

    private final String label;

    GlowAnimation(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
