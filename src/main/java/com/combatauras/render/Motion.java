package com.combatauras.render;

import com.combatauras.model.GlowAnimation;

/**
 * Shared animation curves. Every pulse, breath, sweep and sway goes through here so the one
 * "Motion strength" setting governs all of them: at 0 each animation holds steady at full strength.
 * Pure functions of time; nothing allocates.
 */
public final class Motion
{
    /** Pulse of ready spells, the Magic tab, the spec orb and low-prayer restores (radians per second). */
    public static final double ICON_PULSE_RATE = 4.2;
    /** "Pulse" inventory ready animation. */
    public static final double READY_PULSE_RATE = 3.2;
    /** Breathing glow border style. */
    public static final double BREATH_RATE = 3.0;
    /** Rotating light of the animated border style. */
    public static final double SWEEP_RATE = 3.6;
    /** Screen-edge glow while a spell or reminder is ready. */
    public static final double SCREEN_PULSE_RATE = 3.4;
    /** Low-HP heartbeat on the screen glow. */
    public static final double HEARTBEAT_RATE = 5.2;
    /** Side-to-side sway of showering icons. */
    public static final double SWAY_RATE = 2.5;

    private Motion()
    {
    }

    /** A 0..1 sine wave. */
    public static float wave(long nowNanos, double radPerSec)
    {
        return (float) (0.5 + 0.5 * Math.sin(nowNanos / 1_000_000_000.0 * radPerSec));
    }

    /**
     * Oscillates between {@code floor} and 1 at full motion, narrowing towards a steady 1 as motion drops.
     *
     * @param motion 0..1 "Motion strength"
     */
    public static float pulse(long nowNanos, double radPerSec, float floor, float motion)
    {
        if (motion <= 0f)
        {
            return 1f;
        }
        return 1f - (1f - floor) * motion * (1f - wave(nowNanos, radPerSec));
    }

    /** Strength of an icon glow this frame: its pulse, or a steady 1 when the glow is set to Steady. */
    public static float iconGlow(GlowAnimation animation, long nowNanos, float motion)
    {
        return animation == GlowAnimation.STEADY ? 1f : pulse(nowNanos, ICON_PULSE_RATE, 0.10f, motion);
    }

    /** Elapsed animation angle in radians, slowed by motion (0 = frozen). */
    public static double angle(long nowNanos, double radPerSec, float motion)
    {
        return nowNanos / 1_000_000_000.0 * radPerSec * motion;
    }
}
