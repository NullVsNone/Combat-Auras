package com.combatauras.model;

public enum BorderStyle
{
    NONE("None"),
    ITEM_OUTLINE("Steady outline"),
    BREATHING_GLOW("Breathing glow"),
    SOFT_GLOW("Soft glow"),
    ANIMATED_BORDER("Animated border"),
    CORNER_MARKERS("Corner markers"),
    BOX("Box");

    private final String label;

    BorderStyle(String label)
    {
        this.label = label;
    }

    /** Draws the 1px outline built from the icon's sprite. */
    public boolean usesOutline()
    {
        return this == ITEM_OUTLINE || this == BREATHING_GLOW || this == SOFT_GLOW || this == ANIMATED_BORDER;
    }

    /** Draws the soft aura built from the icon's sprite. */
    public boolean usesAura()
    {
        return this == SOFT_GLOW || this == BREATHING_GLOW;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
