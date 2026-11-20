package com.immersivecombatoverlays.model;

public enum ReadyStyle
{
    NONE("None"),
    SINGLE_FLASH("Single flash"),
    PULSE("Pulse"),
    SOLID("Solid");

    private final String label;

    ReadyStyle(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
