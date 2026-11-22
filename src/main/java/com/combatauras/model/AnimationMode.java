package com.combatauras.model;

public enum AnimationMode
{
    TIMED("Timed"),
    PERMANENT("Permanent");

    private final String label;

    AnimationMode(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
