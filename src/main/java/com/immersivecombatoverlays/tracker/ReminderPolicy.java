package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.Cooldown;
import com.immersivecombatoverlays.model.ReminderContext;

/**
 * Rules shared by every reminder (spells, heart, potions, boosts), so they behave the same everywhere.
 */
public final class ReminderPolicy
{
    private ReminderPolicy()
    {
    }

    /**
     * Whether an effect still counts as running: it has more than its "warn early" lead left. The reminder
     * starts on the tick this turns false, i.e. at the lead (expiring soon) or at expiry when the lead is 0.
     */
    public static boolean activeBeyondLead(Cooldown cd, int leadTicks)
    {
        return cd != null && !cd.isReady() && cd.getRemainingTicks() > Math.max(0, leadTicks);
    }

    /** Where a reminder may show, for reminders with a selectable context. */
    public static boolean contextAllows(ReminderContext context, CombatStateTracker combatState, boolean combatOk)
    {
        if (context == null)
        {
            return combatOk;
        }
        switch (context)
        {
            case THIEVING:
                return combatState.isThieving();
            case ALWAYS:
                return true;
            default:
                return combatOk;
        }
    }

    /** A family's "only remind in combat" gate. */
    public static boolean combatGate(boolean onlyInCombat, CombatStateTracker combatState)
    {
        return !onlyInCombat || combatState.isInCombat();
    }

    /**
     * @param startedNanos    when the reminder started (0 = not started)
     * @param durationSeconds how long it lasts (0 = until the effect is re-applied)
     */
    public static boolean withinDuration(long startedNanos, int durationSeconds, long nowNanos)
    {
        return startedNanos != 0L
            && (durationSeconds <= 0 || nowNanos - startedNanos <= durationSeconds * 1_000_000_000L);
    }
}
