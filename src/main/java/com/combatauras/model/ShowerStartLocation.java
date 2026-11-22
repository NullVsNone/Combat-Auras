package com.combatauras.model;

public enum ShowerStartLocation
{
    TOP("Top (fall down)"),
    BOTTOM("Bottom (rise up)"),
    LEFT("Left (sweep right)"),
    RIGHT("Right (sweep left)");

    private final String label;

    ShowerStartLocation(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
