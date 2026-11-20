package com.immersivecombatoverlays.model;

public enum DisplayMode
{
    ITEM_FILL("Item fill"),
    CIRCLE("Circle"),
    TEXT_ONLY("Text only");

    private final String label;

    DisplayMode(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
