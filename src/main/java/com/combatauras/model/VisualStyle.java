package com.combatauras.model;

/**
 * Which look a reminder's screen glow and icon shower use: the shared settings, or one of four customisable styles.
 */
public enum VisualStyle
{
    GLOBAL("Global (shared settings)"),
    STYLE_1("Style 1"),
    STYLE_2("Style 2"),
    STYLE_3("Style 3"),
    STYLE_4("Style 4");

    private final String label;

    VisualStyle(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
