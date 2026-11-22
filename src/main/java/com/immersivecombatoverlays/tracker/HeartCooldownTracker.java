package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.Cooldown;
import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * Tracks the shared Imbued Heart (700 ticks) / Saturated Heart (500 ticks) cooldown.
 * The game exposes it as IMBUED_HEART_TIMER in 10-tick buckets; we interpolate between buckets.
 */
@javax.inject.Singleton
public class HeartCooldownTracker
{
    public static final int IMBUED_HEART_ID = ItemID.IMBUED_HEART;
    public static final int SATURATED_HEART_ID = ItemID.SATURATED_HEART;
    private static final int HEART_BUCKET_TICKS = 10;
    private static final int IMBUED_TOTAL_TICKS = 700;
    private static final int SATURATED_TOTAL_TICKS = 500;

    private int remainingTicks = 0;
    private int lastVarbitValue = 0;
    private boolean saturated = false;
    private boolean usedThisSession = false;
    private boolean debug = false;

    public void reset()
    {
        remainingTicks = 0;
        lastVarbitValue = 0;
        saturated = false;
        usedThisSession = false;
        debug = false;
    }

    /**
     * Invigorate click: remember which heart started the shared cooldown (carrying both used to guess wrong).
     */
    public void onInvigorate(int itemId)
    {
        if (isHeart(itemId))
        {
            saturated = itemId == SATURATED_HEART_ID;
        }
    }

    public void onGameTick(Client client)
    {
        if (debug)
        {
            if (remainingTicks > 0)
            {
                remainingTicks--;
            }
            return;
        }

        int varbit = client.getVarbitValue(VarbitID.IMBUED_HEART_TIMER);
        if (varbit <= 0)
        {
            // Cooldown finished, or reset by death
            remainingTicks = 0;
        }
        else if (varbit > lastVarbitValue)
        {
            remainingTicks = varbit * HEART_BUCKET_TICKS;
            usedThisSession = true;
            if (remainingTicks > SATURATED_TOTAL_TICKS)
            {
                // Longer than a saturated heart can be: it is the imbued heart, even if we never saw the click
                // (e.g. logging in mid-cooldown)
                saturated = false;
            }
        }
        else if (varbit == lastVarbitValue && remainingTicks > 0)
        {
            remainingTicks = Math.max((varbit - 1) * HEART_BUCKET_TICKS + 1, remainingTicks - 1);
        }
        else
        {
            remainingTicks = varbit * HEART_BUCKET_TICKS;
        }
        lastVarbitValue = Math.max(0, varbit);
    }

    public void triggerDebugCooldown()
    {
        remainingTicks = 25;
        usedThisSession = true;
        debug = true;
    }

    public void clearDebugCooldown()
    {
        if (debug)
        {
            debug = false;
            remainingTicks = 0;
            lastVarbitValue = 0;
        }
    }

    /** The shared heart cooldown (both hearts use the same game timer). */
    public Cooldown getCooldown(int itemId)
    {
        return getCooldown();
    }

    public Cooldown getCooldown()
    {
        if (remainingTicks <= 0)
        {
            return Cooldown.READY;
        }
        int total = saturated ? SATURATED_TOTAL_TICKS : IMBUED_TOTAL_TICKS;
        return Cooldown.active(remainingTicks, Math.max(total, remainingTicks));
    }

    public static boolean isHeart(int itemId)
    {
        return itemId == IMBUED_HEART_ID || itemId == SATURATED_HEART_ID;
    }

    /**
     * Whether a saturated heart's boost (which counts as a divine effect) is still running,
     * approximated by its cooldown after a saturated heart was used (the game exposes no separate flag).
     */
    public boolean isSaturatedBoostActive()
    {
        return saturated && !debug && remainingTicks > 0;
    }

    public boolean wasUsedThisSession()
    {
        return usedThisSession;
    }
}
