package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.Notifier;

/**
 * Tracks player idle state modeled after Better Notifications:
 * - Resets immediately on any movement (tile change, non-idle pose, or active destination),
 *   action animation, actor interaction, mouse click, mouse movement, or keyboard/camera input.
 * - Counts inactivity once all activity stops, ramping from idleStartSeconds to idleCriticalSeconds.
 */
@javax.inject.Singleton
public class IdleTracker
{
    private static final int CLIENT_TICKS_PER_GAME_TICK = 30; // 30 * 20ms = 600ms

    private long idleSinceNanos = 0L;
    private long lastMousePressedMillis = 0L;
    private int lastKeyboardIdleTicks = 0;
    private int lastMouseIdleTicks = 0;
    private WorldPoint lastPosition = null;
    private boolean notificationArmed = true;

    public void reset()
    {
        idleSinceNanos = 0L;
        lastMousePressedMillis = 0L;
        lastKeyboardIdleTicks = 0;
        lastMouseIdleTicks = 0;
        lastPosition = null;
        notificationArmed = true;
    }

    public void onAnimationChanged(Client client, Actor actor)
    {
        if (client == null || actor == null)
        {
            return;
        }
        Player local = client.getLocalPlayer();
        if (local != null && actor == local && actor.getAnimation() != -1)
        {
            idleSinceNanos = 0L;
            notificationArmed = true;
        }
    }

    public void onInteractingChanged(Client client, Actor source, Actor target)
    {
        if (client == null || source == null)
        {
            return;
        }
        Player local = client.getLocalPlayer();
        if (local != null && source == local && target != null)
        {
            idleSinceNanos = 0L;
            notificationArmed = true;
        }
    }

    public void onGameTick(
        Client client,
        ImmersiveCombatOverlaysConfig config,
        CombatStateTracker combatState,
        Notifier notifier,
        long nowNanos
    )
    {
        if (!config.idleEnabled())
        {
            reset();
            return;
        }

        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return;
        }

        if (config.idleOnlyInCombat() && !combatState.isInCombat())
        {
            idleSinceNanos = 0L;
            notificationArmed = true;
            return;
        }

        long mouseMillis = client.getMouseLastPressedMillis();
        int kbIdleTicks = client.getKeyboardIdleTicks();
        int mouseIdleTicks = client.getMouseIdleTicks();
        WorldPoint pos = local.getWorldLocation();

        boolean movingPose = local.getPoseAnimation() != local.getIdlePoseAnimation();
        boolean hasDestination = client.getLocalDestinationLocation() != null;
        boolean movedTile = lastPosition != null && pos != null && !lastPosition.equals(pos);
        boolean animating = local.getAnimation() != -1;
        Actor target = local.getInteracting();
        boolean interacting = target != null && !target.isDead();
        boolean mouseClicked = mouseMillis > lastMousePressedMillis;
        boolean kbActive = kbIdleTicks < lastKeyboardIdleTicks || kbIdleTicks < CLIENT_TICKS_PER_GAME_TICK;
        boolean mouseActive = mouseIdleTicks < lastMouseIdleTicks || mouseIdleTicks < CLIENT_TICKS_PER_GAME_TICK;

        lastMousePressedMillis = mouseMillis;
        lastKeyboardIdleTicks = kbIdleTicks;
        lastMouseIdleTicks = mouseIdleTicks;
        lastPosition = pos;

        boolean active = movingPose || hasDestination || movedTile || animating
            || interacting || mouseClicked || kbActive || mouseActive;

        if (active)
        {
            idleSinceNanos = 0L;
            notificationArmed = true;
            return;
        }

        if (idleSinceNanos == 0L)
        {
            idleSinceNanos = nowNanos;
        }

        int startSec = config.idleStartSeconds();
        int critSec = Math.max(startSec + 1, config.idleCriticalSeconds());
        long elapsedNanos = nowNanos - idleSinceNanos;

        if (notificationArmed && elapsedNanos >= critSec * 1_000_000_000L)
        {
            notificationArmed = false;
            if (notifier != null)
            {
                notifier.notify(config.idleNotification(), "You have been idle for " + critSec + " seconds");
            }
        }
    }

    public float getSeverity(ImmersiveCombatOverlaysConfig config, long nowNanos)
    {
        if (!config.idleEnabled() || idleSinceNanos == 0L)
        {
            return 0f;
        }

        int startSec = config.idleStartSeconds();
        int critSec = Math.max(startSec + 1, config.idleCriticalSeconds());
        long elapsedNanos = nowNanos - idleSinceNanos;

        long startNanos = startSec * 1_000_000_000L;
        if (elapsedNanos < startNanos)
        {
            return 0f;
        }

        long critNanos = critSec * 1_000_000_000L;
        if (config.idleAutoHide())
        {
            long hideNanos = critNanos + config.idleAutoHideSeconds() * 1_000_000_000L;
            if (elapsedNanos >= hideNanos)
            {
                return 0f;
            }
        }

        if (elapsedNanos >= critNanos)
        {
            return 1.0f;
        }

        return Math.max(0f, Math.min(1f, (float) (elapsedNanos - startNanos) / (float) (critNanos - startNanos)));
    }
}
