package com.immersivecombatoverlays.model;

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

    @Override
    public String toString()
    {
        return label;
    }
}
