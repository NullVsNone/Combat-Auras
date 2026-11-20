package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.CombatSpell;
import java.util.Arrays;
import net.runelite.client.Notifier;
import net.runelite.client.config.Notification;

/**
 * Fires a RuneLite notification (sound / tray / flash, as configured per warning) once when a warning
 * becomes active. A per-warning cooldown stops flapping values (e.g. HP hovering at the threshold)
 * from spamming sounds.
 */
public class WarningNotifier
{
    public enum Warning
    {
        LOW_HEALTH("Your hitpoints are low!"),
        LOW_PRAYER("Your prayer is low!"),
        FREEZE("You have been frozen!"),
        UNPOTTED("You are unpotted!"),
        SPEC_READY("Special attack is ready."),
        HEART_READY("Your heart is ready."),
        BUFF_EXPIRED("A potion effect has expired."),
        SPELL_READY("A spell is ready to recast."),
        PARASITE("A parasite is growing inside you - drink a Sanfew serum!"),
        POISON("You have been poisoned."),
        VENOM("You have been envenomed."),
        DISEASE("You have been diseased."),
        LOW_RUN("Your run energy is low."),
        BOOST_READY("A boost potion is ready to re-dose.");

        private final String message;

        Warning(String message)
        {
            this.message = message;
        }

        public String getMessage()
        {
            return message;
        }
    }

    static final long RETRIGGER_NANOS = 10_000_000_000L;

    private final boolean[] active = new boolean[Warning.values().length];
    private final long[] lastFiredNanos = new long[Warning.values().length];
    private final long[] lastSpellFiredNanos = new long[CombatSpell.values().length];

    public void reset()
    {
        Arrays.fill(active, false);
        Arrays.fill(lastFiredNanos, 0L);
        Arrays.fill(lastSpellFiredNanos, 0L);
    }

    /**
     * Fires the spell's own notification once when it becomes ready to recast (per-spell cooldown).
     */
    public boolean fireSpell(CombatSpell spell, Notification notification, Notifier notifier, long nowNanos)
    {
        if (notification == null || !notification.isEnabled())
        {
            return false;
        }
        int i = spell.ordinal();
        if (lastSpellFiredNanos[i] != 0L && nowNanos - lastSpellFiredNanos[i] < RETRIGGER_NANOS)
        {
            return false;
        }
        lastSpellFiredNanos[i] = nowNanos;
        if (notifier != null)
        {
            notifier.notify(notification, spell.getDisplayName() + " is ready to recast.");
        }
        return true;
    }

    /**
     * Fires once for an externally detected event (e.g. one specific spell became ready), still honouring the cooldown.
     */
    public boolean fire(Warning warning, Notification notification, Notifier notifier, long nowNanos)
    {
        active[warning.ordinal()] = false;
        return update(warning, true, notification, notifier, nowNanos);
    }

    /**
     * @return true if a notification was sent
     */
    public boolean update(Warning warning, boolean nowActive, Notification notification, Notifier notifier, long nowNanos)
    {
        int i = warning.ordinal();
        boolean rising = nowActive && !active[i];
        active[i] = nowActive;
        if (!rising || notification == null || !notification.isEnabled())
        {
            return false;
        }
        if (lastFiredNanos[i] != 0L && nowNanos - lastFiredNanos[i] < RETRIGGER_NANOS)
        {
            return false;
        }
        lastFiredNanos[i] = nowNanos;
        if (notifier != null)
        {
            notifier.notify(notification, warning.getMessage());
        }
        return true;
    }
}
