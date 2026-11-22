package com.immersivecombatoverlays;

import com.immersivecombatoverlays.tracker.ImpactTrackers;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.HitsplatID;

/**
 * Turns hitsplats on the local player into the damage / poison / venom flashes.
 */
@Singleton
public class DamageFeedback
{
    private final ImpactTrackers impacts;

    @Inject
    public DamageFeedback(ImpactTrackers impacts)
    {
        this.impacts = impacts;
    }

    /** A hitsplat landed on the local player. */
    public void onLocalHitsplat(int type, int amount, int minAmount, int maxHp, boolean scaleWithHp, long nowNanos)
    {
        if (type == HitsplatID.POISON)
        {
            impacts.poisonTick.trigger(nowNanos, 0.85f);
        }
        else if (type == HitsplatID.VENOM)
        {
            impacts.venomTick.trigger(nowNanos, 0.95f);
        }
        else if (amount > 0 && amount >= minAmount && isDamageHitsplat(type))
        {
            impacts.damage.trigger(nowNanos, damageFlashStrength(amount, maxHp, scaleWithHp));
        }
    }

    /**
     * 0..1 damage flash strength. With {@code byPercent}, a hit of 40% of max HP (or more) is a full flash,
     * so the flash means the same thing at any Hitpoints level.
     */
    static float damageFlashStrength(int amount, int maxHp, boolean byPercent)
    {
        if (amount <= 0)
        {
            return 0f;
        }
        if (!byPercent)
        {
            return Math.min(1f, 0.25f + amount / 40f); // original fixed scale
        }
        float frac = amount / (float) Math.max(1, maxHp);
        return Math.min(1f, 0.25f + frac * 1.875f);
    }

    /**
     * Only real damage flashes the screen; heals, shields, prayer/sanity drains etc. do not.
     */
    static boolean isDamageHitsplat(int type)
    {
        switch (type)
        {
            case HitsplatID.DAMAGE_ME:
            case HitsplatID.DAMAGE_OTHER:
            case HitsplatID.DAMAGE_ME_CYAN:
            case HitsplatID.DAMAGE_OTHER_CYAN:
            case HitsplatID.DAMAGE_ME_ORANGE:
            case HitsplatID.DAMAGE_OTHER_ORANGE:
            case HitsplatID.DAMAGE_ME_YELLOW:
            case HitsplatID.DAMAGE_OTHER_YELLOW:
            case HitsplatID.DAMAGE_ME_WHITE:
            case HitsplatID.DAMAGE_OTHER_WHITE:
            case HitsplatID.DAMAGE_MAX_ME:
            case HitsplatID.DAMAGE_MAX_ME_CYAN:
            case HitsplatID.DAMAGE_MAX_ME_ORANGE:
            case HitsplatID.DAMAGE_MAX_ME_YELLOW:
            case HitsplatID.DAMAGE_MAX_ME_WHITE:
            case HitsplatID.DAMAGE_ME_POISE:
            case HitsplatID.DAMAGE_OTHER_POISE:
            case HitsplatID.DAMAGE_MAX_ME_POISE:
            case HitsplatID.BLEED:
            case HitsplatID.BURN:
                return true;
            default:
                return false;
        }
    }
}
