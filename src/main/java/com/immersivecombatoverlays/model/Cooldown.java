package com.immersivecombatoverlays.model;

public final class Cooldown
{
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
        return new Cooldown(0, 0, false, true);
    }

    public static Cooldown active(int remainingTicks, int totalTicks)
    {
        return new Cooldown(remainingTicks, totalTicks, false, remainingTicks <= 0);
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
        if (ready || totalTicks <= 0)
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
