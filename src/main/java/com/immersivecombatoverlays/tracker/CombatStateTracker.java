package com.immersivecombatoverlays.tracker;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.gameval.VarbitID;

/**
 * Tracks whether the local player is in combat and detects extended breaks
 * so that re-entering combat after a configurable pause triggers fresh spell reminders.
 */
public class CombatStateTracker
{
    private static final int COMBAT_TIMEOUT_TICKS = 17; // ~10.2 seconds

    private boolean inCombat = false;
    private int ticksSinceCombatAction = COMBAT_TIMEOUT_TICKS;
    private long lastCombatExitNanos = 0L;
    private int combatSessionId = 0;
    private boolean simulatedCombat = false;

    public void reset()
    {
        inCombat = false;
        ticksSinceCombatAction = COMBAT_TIMEOUT_TICKS;
        lastCombatExitNanos = 0L;
        combatSessionId = 0;
    }

    public void onCombatActivity(long nowNanos, int breakReRemindSeconds)
    {
        if (!inCombat)
        {
            if (lastCombatExitNanos == 0L)
            {
                combatSessionId++;
            }
            else
            {
                long breakNanos = nowNanos - lastCombatExitNanos;
                long thresholdNanos = Math.max(1L, (long) breakReRemindSeconds) * 1_000_000_000L;
                if (breakNanos >= thresholdNanos)
                {
                    combatSessionId++;
                }
            }
            inCombat = true;
        }
        ticksSinceCombatAction = 0;
    }

    public void onGameTick(Client client, long nowNanos, int breakReRemindSeconds)
    {
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return;
        }

        Actor interacting = local.getInteracting();
        // Following / trading another player also sets "interacting", so only count players where PvP is possible
        boolean pvpArea = client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1
            || client.getVarbitValue(VarbitID.PVP_AREA_CLIENT) == 1;
        boolean activelyFighting = (interacting instanceof Player && pvpArea)
            || (interacting instanceof NPC && ((NPC) interacting).getCombatLevel() > 0);

        if (activelyFighting)
        {
            onCombatActivity(nowNanos, breakReRemindSeconds);
            return;
        }

        if (inCombat)
        {
            ticksSinceCombatAction++;
            if (ticksSinceCombatAction >= COMBAT_TIMEOUT_TICKS)
            {
                inCombat = false;
                lastCombatExitNanos = nowNanos;
            }
        }
    }

    public void setSimulatedCombat(boolean simulated)
    {
        this.simulatedCombat = simulated;
    }

    public boolean isInCombat()
    {
        return inCombat || simulatedCombat;
    }

    public int getCombatSessionId()
    {
        return combatSessionId;
    }

    public long getSecondsOutOfCombat(long nowNanos)
    {
        if (inCombat || lastCombatExitNanos == 0L)
        {
            return 0L;
        }
        return Math.max(0L, (nowNanos - lastCombatExitNanos) / 1_000_000_000L);
    }
}
