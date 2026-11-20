package com.immersivecombatoverlays.model;

/**
 * Immutable cooldown snapshot. {@link #READY} is a shared instance so the common "nothing on
 * cooldown" path never allocates.
 */
public final class Cooldown
{
    public static final Cooldown READY = new Cooldown(0, 0, false, true);

    private final int remainingTicks;
    private final int totalTicks;
    private final boolean unknownDuration;
    private final boolean ready;

    public Cooldown(int remainingTicks, int totalTicks, boolean unknownDuration, boolean ready)
    {
        this.remainingTicks = Math.max(0, remainingTicks);
        this.totalTicks = Math.max(0, totalTicks);
        this.unknownDuration = unknownDuration;
        this.ready = ready;
    }

    public static Cooldown ready()
    {
        return READY;
    }

    public static Cooldown active(int remainingTicks, int totalTicks)
    {
        if (remainingTicks <= 0)
        {
            return READY;
        }
        return new Cooldown(remainingTicks, totalTicks, false, false);
    }

    public static Cooldown unknown(int remainingTicks)
    {
        return new Cooldown(remainingTicks, 0, true, false);
    }

    public int getRemainingTicks()
    {
        return remainingTicks;
    }

    public int getTotalTicks()
    {
        return totalTicks;
    }

    public boolean isUnknownDuration()
    {
        return unknownDuration;
    }

    public boolean isReady()
    {
        return ready || remainingTicks <= 0;
    }

    public float getRemainingFraction()
    {
        if (isReady() || totalTicks <= 0)
        {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, (float) remainingTicks / (float) totalTicks));
    }

    public float getRemainingSeconds()
    {
        return remainingTicks * 0.6f;
    }
}
