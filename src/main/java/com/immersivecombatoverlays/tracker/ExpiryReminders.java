package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.BoostPotion;
import com.immersivecombatoverlays.model.PotionEffect;
import java.util.Arrays;

/**
 * Expiry latch for Heart / potion / boost reminders.
 *
 * A reminder starts only on the tick an effect goes from active to expired (or reaches its pre-remind lead),
 * lasts for that section's reminder duration, and clears as soon as the effect is re-applied. Simply having an
 * unused potion in the inventory never triggers anything.
 */
public class ExpiryReminders
{
    public static final int HEART = 0;
    private static final int EFFECT_BASE = 1;
    private static final int BOOST_BASE = EFFECT_BASE + PotionEffect.values().length;
    private static final int KEY_COUNT = BOOST_BASE + BoostPotion.values().length;

    private final boolean[] wasActive = new boolean[KEY_COUNT];
    private final long[] expiredAtNanos = new long[KEY_COUNT];

    public static int key(PotionEffect effect)
    {
        return EFFECT_BASE + effect.ordinal();
    }

    public static int key(BoostPotion boost)
    {
        return BOOST_BASE + boost.ordinal();
    }

    public void reset()
    {
        Arrays.fill(wasActive, false);
        Arrays.fill(expiredAtNanos, 0L);
    }

    /**
     * Records this tick's state for one key.
     *
     * @param active whether the effect is still running (beyond its pre-remind lead)
     */
    public void observe(int key, boolean active, long nowNanos)
    {
        if (active)
        {
            expiredAtNanos[key] = 0L;
        }
        else if (wasActive[key])
        {
            expiredAtNanos[key] = nowNanos;
        }
        wasActive[key] = active;
    }

    /**
     * @param durationSeconds how long the reminder lasts (0 = until the effect is re-applied)
     */
    public boolean isShowing(int key, int durationSeconds, long nowNanos)
    {
        long at = expiredAtNanos[key];
        if (at == 0L)
        {
            return false;
        }
        return durationSeconds <= 0 || nowNanos - at <= durationSeconds * 1_000_000_000L;
    }
}
