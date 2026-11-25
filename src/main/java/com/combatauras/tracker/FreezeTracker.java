package com.combatauras.tracker;

import com.combatauras.config.CombatAurasConfig;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;

/**
 * Tracks freeze / bind / entangle state from spotanims, chat messages, and movement/unfreeze breaks.
 */
@javax.inject.Singleton
public class FreezeTracker
{
    private static final long GAME_TICK_NANOS = 600_000_000L;

    private int remainingTicks = 0;
    private int totalTicks = 0;
    private WorldPoint frozenTile = null;
    private long freezeStartNanos = 0L;

    public void reset()
    {
        remainingTicks = 0;
        totalTicks = 0;
        frozenTile = null;
        freezeStartNanos = 0L;
    }

    private static final int[] FREEZE_SPOTANIMS = {181, 180, 179, 361, 363, 367, 369};

    /**
     * Checks every spotanim on the player (an actor can have several at once) for a freeze/bind graphic.
     */
    public void onGraphicChanged(Player localPlayer, long nowNanos)
    {
        onGraphicChanged(null, localPlayer, nowNanos);
    }

    /** @param client used to spot a caster wielding an Ice ancient sceptre (ice freezes last 10% longer); may be null */
    public void onGraphicChanged(Client client, Player localPlayer, long nowNanos)
    {
        if (localPlayer == null)
        {
            return;
        }
        for (int id : FREEZE_SPOTANIMS)
        {
            if (localPlayer.hasSpotAnim(id))
            {
                onGraphicChanged(localPlayer, id, nowNanos);
                if (remainingTicks == totalTicks && isIceSpell(id) && iceSceptreCaster(client, localPlayer))
                {
                    remainingTicks += remainingTicks / 10;
                    totalTicks = remainingTicks;
                }
                return;
            }
        }
    }

    private static boolean isIceSpell(int spotAnimId)
    {
        return spotAnimId == 361 || spotAnimId == 363 || spotAnimId == 367 || spotAnimId == 369;
    }

    /** Whether a player attacking us wields an Ice ancient sceptre. Runs once per freeze, not per frame. */
    static boolean iceSceptreCaster(Client client, Player localPlayer)
    {
        if (client == null)
        {
            return false;
        }
        for (Player p : client.getTopLevelWorldView().players())
        {
            if (p == null || p == localPlayer || p.getInteracting() != localPlayer || p.getPlayerComposition() == null)
            {
                continue;
            }
            int weapon = p.getPlayerComposition().getEquipmentId(KitType.WEAPON);
            if (weapon == ItemID.ANCIENT_SCEPTRE_ICE || weapon == ItemID.ANCIENT_SCEPTRE_ICE_TROUVER)
            {
                return true;
            }
        }
        return false;
    }

    public void onGraphicChanged(Player localPlayer, int spotAnimId, long nowNanos)
    {
        int ticks = getFreezeTicksForSpotAnim(spotAnimId);
        if (ticks > 0 && remainingTicks <= 0)
        {
            startFreeze(localPlayer, ticks, nowNanos);
        }
    }

    public void onChatMessage(Player localPlayer, String message, long nowNanos)
    {
        if (message == null || message.isEmpty())
        {
            return;
        }
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("unfrozen") || lower.contains("no longer frozen") || lower.contains("freeze has worn off") || lower.contains("break free"))
        {
            reset();
            return;
        }
        if (remainingTicks <= 0)
        {
            if (lower.contains("you have been frozen"))
            {
                startFreeze(localPlayer, 32, nowNanos);
            }
            else if (lower.contains("binds you") || lower.contains("entangled") || lower.contains("you are bound"))
            {
                startFreeze(localPlayer, 16, nowNanos);
            }
        }
    }

    private void startFreeze(Player localPlayer, int ticks, long nowNanos)
    {
        remainingTicks = ticks;
        totalTicks = ticks;
        frozenTile = localPlayer != null ? localPlayer.getWorldLocation() : null;
        freezeStartNanos = nowNanos;
    }

    public void onGameTick(Client client)
    {
        if (remainingTicks <= 0)
        {
            return;
        }
        Player local = client != null ? client.getLocalPlayer() : null;
        if (local != null)
        {
            // Only a tile change proves the freeze is gone (immunity hits still play the spotanim).
            // Pose is not used: turning in place while frozen changes the pose animation.
            if (frozenTile != null)
            {
                WorldPoint current = local.getWorldLocation();
                if (current != null && !current.equals(frozenTile))
                {
                    reset();
                    return;
                }
            }
        }
        remainingTicks--;
        if (remainingTicks <= 0)
        {
            reset();
        }
    }

    public float getSeverity(long nowNanos)
    {
        return getSeverity(null, nowNanos);
    }

    public float getSeverity(CombatAurasConfig config, long nowNanos)
    {
        if (remainingTicks <= 0 || totalTicks <= 0)
        {
            return 0f;
        }
        long elapsed = Math.max(0L, nowNanos - freezeStartNanos);
        if (config != null && config.freezeMaxDisplaySeconds() > 0
            && elapsed >= config.freezeMaxDisplaySeconds() * 1_000_000_000L)
        {
            return 0f;
        }
        float totalNanos = totalTicks * (float) GAME_TICK_NANOS;
        float frac = Math.max(0f, Math.min(1f, 1f - (elapsed / totalNanos)));
        return Math.max(0.25f, frac);
    }

    private static int getFreezeTicksForSpotAnim(int spotAnimId)
    {
        switch (spotAnimId)
        {
            case 181: return 8;  // Bind
            case 180: return 16; // Snare
            case 179: return 24; // Entangle
            case 361: return 8;  // Ice Rush
            case 363: return 16; // Ice Burst
            case 367: return 24; // Ice Blitz
            case 369: return 32; // Ice Barrage
            default:
                return 0;
        }
    }
}
