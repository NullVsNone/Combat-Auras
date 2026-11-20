package com.immersivecombatoverlays.tracker;

/**
 * Tracks exponentially decaying impact flashes (damage hits, poison ticks, venom ticks)
 * without allocating objects per frame.
 */
public class ImpactTracker
{
    private long lastImpactNanos = 0L;
    private float peakStrength = 0f;

    public void reset()
    {
        lastImpactNanos = 0L;
        peakStrength = 0f;
    }

    public void trigger(long nowNanos, float strength)
    {
        lastImpactNanos = nowNanos;
        peakStrength = Math.max(peakStrength * 0.35f, Math.max(0f, Math.min(1f, strength)));
    }

    public float getSeverity(long nowNanos, long durationNanos)
    {
        if (lastImpactNanos == 0L || durationNanos <= 0L || peakStrength <= 0.001f)
        {
            return 0f;
        }
        long elapsed = nowNanos - lastImpactNanos;
        if (elapsed <= 0L)
        {
            return peakStrength;
        }
        if (elapsed >= durationNanos)
        {
            peakStrength = 0f;
            lastImpactNanos = 0L;
            return 0f;
        }
        float t = (float) elapsed / (float) durationNanos;
        float decay = (1f - t);
        return peakStrength * decay * decay;
    }
}
