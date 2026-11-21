package com.immersivecombatoverlays.tracker;

/**
 * Tracks the Nightmare / Phosani's Nightmare parasite (requested in Health & Status Overlays issue #3).
 * Starts on the impregnation message and ends when the parasite is weakened (Sanfew / Relicym's) or bursts.
 * A tick-based fallback timeout guarantees the warning never sticks if an end message is missed.
 */
public class ParasiteTracker
{
    static final String MSG_IMPREGNATED = "impregnated you with a deadly parasite";
    static final int FALLBACK_TICKS = 30; // parasite bursts roughly 18s after impregnation

    private int remainingTicks = 0;

    public void reset()
    {
        remainingTicks = 0;
    }

    public void onChatMessage(String rawMessage)
    {
        if (rawMessage == null || rawMessage.isEmpty())
        {
            return;
        }
        String lower = CombatSpellTracker.stripTags(rawMessage).toLowerCase(java.util.Locale.ROOT);
        if (lower.contains(MSG_IMPREGNATED))
        {
            remainingTicks = FALLBACK_TICKS;
        }
        else if (lower.contains("parasite") && (lower.contains("weaken") || lower.contains("bursts out")))
        {
            remainingTicks = 0;
        }
    }

    public void onGameTick()
    {
        if (remainingTicks > 0)
        {
            remainingTicks--;
        }
    }

    public boolean isActive()
    {
        return remainingTicks > 0;
    }

    public float getSeverity()
    {
        // Urgency rises as the burst approaches
        return remainingTicks > 0 ? 0.6f + 0.4f * (1f - remainingTicks / (float) FALLBACK_TICKS) : 0f;
    }
}
