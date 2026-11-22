package com.combatauras.tracker;

import com.combatauras.config.CombatAurasConfig;
import com.combatauras.model.PotionEffect;
import java.util.Arrays;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * Raises the "unpotted" alert when combat-style XP arrives while the matching stat is not boosted.
 * <p>
 * XP only counts as combat XP when a hit dealt by the player lands around it, so teleports, alchemy,
 * enchanting, thrall casts, XP lamps and Ranging Guild shots never trigger it, with no spell list to maintain.
 * Ranged and Magic XP is granted when the attack starts while the hitsplat lands after the projectile flies,
 * so the decision waits up to {@link #HIT_WAIT_TICKS} ticks for that hit. Magic XP is also vetoed when a
 * non-attack spell was cast in the last few ticks (alching while a slow weapon hits).
 */
@javax.inject.Singleton
public class UnpottedTracker
{
    /** Longest projectile flight we wait for (long-range spells and bolts take up to ~5 ticks). */
    static final int HIT_WAIT_TICKS = 6;
    /** Magic XP within this many ticks of a non-attack cast is that cast's XP. */
    static final int UTILITY_VETO_TICKS = 3;

    private static final Skill[] TRACKED = {Skill.ATTACK, Skill.STRENGTH, Skill.RANGED, Skill.MAGIC};

    private final int[] lastXp = new int[Skill.values().length];
    private final boolean[] xpInitialized = new boolean[Skill.values().length];
    /** Ticks since unconfirmed unboosted XP in that skill, or -1 when nothing is pending. */
    private final int[] pendingAge = new int[Skill.values().length];

    private long alertUntilNanos = 0L;
    private Skill lastTriggeredSkill = null;

    public UnpottedTracker()
    {
        Arrays.fill(pendingAge, -1);
    }

    public void reset()
    {
        Arrays.fill(lastXp, 0);
        Arrays.fill(xpInitialized, false);
        Arrays.fill(pendingAge, -1);
        alertUntilNanos = 0L;
        lastTriggeredSkill = null;
    }

    public void onStatChanged(
        CombatAurasConfig config,
        CombatStateTracker combatState,
        PotionTimers potionTimers,
        Skill skill,
        int xp,
        int boostedLevel,
        int realLevel,
        long nowNanos
    )
    {
        if (skill == null || !isCombatStyleSkill(skill))
        {
            return;
        }

        int ord = skill.ordinal();
        if (!xpInitialized[ord])
        {
            lastXp[ord] = xp;
            xpInitialized[ord] = true;
            return;
        }

        int delta = xp - lastXp[ord];
        lastXp[ord] = xp;
        if (delta <= 0 || !config.unpottedEnabled())
        {
            return;
        }
        if (skill == Skill.MAGIC && combatState.castUtilityMagicWithin(UTILITY_VETO_TICKS))
        {
            return;
        }

        // Suppress when Overload or Smelling Salts is active (prevents false alerts after brewing down in CoX/ToA)
        if (potionTimers != null && (
            !potionTimers.getCooldown(PotionEffect.OVERLOAD).isReady()
            || !potionTimers.getCooldown(PotionEffect.BLIGHTED_OVERLOAD).isReady()
            || !potionTimers.getCooldown(PotionEffect.SMELLING_SALTS).isReady()))
        {
            alertUntilNanos = 0L;
            pendingAge[ord] = -1;
            return;
        }

        if (boostedLevel > realLevel + config.unpottedThreshold())
        {
            pendingAge[ord] = -1;
            if (skill == lastTriggeredSkill)
            {
                alertUntilNanos = 0L;
            }
            return;
        }

        if (combatState.dealtHitWithin(1))
        {
            raise(skill, config, nowNanos); // melee: the hit lands with the XP
        }
        else
        {
            pendingAge[ord] = 0;
        }
    }

    private void raise(Skill skill, CombatAurasConfig config, long nowNanos)
    {
        lastTriggeredSkill = skill;
        alertUntilNanos = nowNanos + Math.max(1L, (long) config.unpottedAlertDurationSeconds()) * 1_000_000_000L;
    }

    /**
     * Call after {@link CombatStateTracker#onGameTick} so this tick's hits are already recorded.
     */
    public void onGameTick(Client client, CombatAurasConfig config, CombatStateTracker combatState,
        long nowNanos)
    {
        for (Skill skill : TRACKED)
        {
            int ord = skill.ordinal();
            if (pendingAge[ord] < 0)
            {
                continue;
            }
            pendingAge[ord]++;
            if (combatState.dealtHitWithin(pendingAge[ord]))
            {
                pendingAge[ord] = -1;
                raise(skill, config, nowNanos);
            }
            else if (pendingAge[ord] >= HIT_WAIT_TICKS)
            {
                pendingAge[ord] = -1; // no hit followed: not combat XP
            }
        }
        if (alertUntilNanos == 0L || lastTriggeredSkill == null || client == null)
        {
            return;
        }
        int real = client.getRealSkillLevel(lastTriggeredSkill);
        int boosted = client.getBoostedSkillLevel(lastTriggeredSkill);
        if (boosted > real + config.unpottedThreshold())
        {
            alertUntilNanos = 0L;
        }
    }

    public float getSeverity(CombatAurasConfig config, long nowNanos)
    {
        if (!config.unpottedEnabled() || alertUntilNanos == 0L)
        {
            return 0f;
        }
        if (nowNanos >= alertUntilNanos)
        {
            alertUntilNanos = 0L;
            return 0f;
        }
        return 0.72f;
    }

    private static boolean isCombatStyleSkill(Skill skill)
    {
        // Exclude DEFENCE so Longrange Magic/Ranged (e.g. Tumeken's Shadow) never false-triggers on Defence
        return skill == Skill.ATTACK
            || skill == Skill.STRENGTH
            || skill == Skill.RANGED
            || skill == Skill.MAGIC;
    }
}
