package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.Cooldown;
import net.runelite.api.Client;

/**
 * Tracks the shared Imbued Heart (700 ticks) / Saturated Heart (500 ticks) cooldown.
 */
public class HeartCooldownTracker
{
    public static final int IMBUED_HEART_ID = 20724;
    public static final int SATURATED_HEART_ID = 27641;
    private static final int VARBIT_IMBUED_HEART_COOLDOWN = 5361;
    private static final int HEART_BUCKET_TICKS = 10;
    private static final int IMBUED_TOTAL_TICKS = 700;
    private static final int SATURATED_TOTAL_TICKS = 500;

    private int remainingTicks = 0;
    private int lastVarbitValue = 0;
    private boolean saturated = false;
    private boolean usedThisSession = false;

    public void reset()
    {
        remainingTicks = 0;
        lastVarbitValue = 0;
        saturated = false;
        usedThisSession = false;
    }

    public void onGameTick(Client client, boolean hasSaturatedHeart)
    {
        saturated = hasSaturatedHeart;
        int varbit = client.getVarbitValue(VARBIT_IMBUED_HEART_COOLDOWN);
        if (varbit <= 0)
        {
            if (lastVarbitValue == 3 && remainingTicks > 0)
            {
                remainingTicks--;
                if (remainingTicks == 0) lastVarbitValue = 0;
                return;
            }
            remainingTicks = 0;
            lastVarbitValue = 0;
            return;
        }

        if (varbit != lastVarbitValue)
        {
            lastVarbitValue = varbit;
            remainingTicks = varbit * HEART_BUCKET_TICKS;
        }
        else if (remainingTicks > 0)
        {
            remainingTicks--;
        }
    }

    public void triggerDebugCooldown()
    {
        remainingTicks = 25;
        usedThisSession = true;
        lastVarbitValue = 3;
    }

    public Cooldown getCooldown(int itemId)
    {
        if (remainingTicks <= 0)
        {
            return Cooldown.ready();
        }
        int total = (itemId == SATURATED_HEART_ID || saturated)
            ? SATURATED_TOTAL_TICKS
            : IMBUED_TOTAL_TICKS;
        return Cooldown.active(remainingTicks, total);
    }

    public static boolean isHeart(int itemId)
    {
        return itemId == IMBUED_HEART_ID || itemId == SATURATED_HEART_ID;
    }

    public boolean wasUsedThisSession()
    {
        return usedThisSession;
    }
}
