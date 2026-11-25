package com.combatauras.settings;

import com.combatauras.model.ShowerStartLocation;

/**
 * The resolved look of one visual style (the shared settings, or Style 1-4): which screen edges glow and how
 * strongly, and how its icon shower moves. Immutable; held by {@link SettingsSnapshot}.
 */
public final class VisualProfile
{
    public final boolean top;
    public final boolean bottom;
    public final boolean left;
    public final boolean right;
    /** 0..1 */
    public final float intensity;
    /** 0..1 */
    public final float depth;
    /** 0..1 */
    public final float tint;
    /** 0..1 */
    public final float motion;
    public final ShowerStartLocation showerStart;
    /** 0.1..1 of the screen */
    public final float showerFraction;
    /** 1 = normal speed */
    public final float showerSpeed;
    /** 0.1..1 */
    public final float showerOpacity;

    public VisualProfile(boolean top, boolean bottom, boolean left, boolean right, int intensityPct, int depthPct,
        int tintPct, int motionPct, ShowerStartLocation showerStart, int showerPct, int showerSpeedPct, int showerOpacityPct)
    {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
        this.intensity = intensityPct / 100f;
        this.depth = depthPct / 100f;
        this.tint = tintPct / 100f;
        this.motion = motionPct / 100f;
        this.showerStart = showerStart;
        this.showerFraction = Math.max(0.10f, Math.min(1.0f, showerPct / 100f));
        this.showerSpeed = showerSpeedPct / 100f;
        this.showerOpacity = Math.max(0.10f, Math.min(1f, showerOpacityPct / 100f));
    }

    public boolean anyEdge()
    {
        return top || bottom || left || right;
    }
}
