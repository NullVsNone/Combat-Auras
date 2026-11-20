package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.PotionEffect;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;

/**
 * Tracks unpotted combat state based on actual combat style XP gains:
 * - Melee (Attack/Strength/Defence): requires active combat interaction.
 * - Ranged: triggers on Ranged XP even without HP XP (e.g., Dwarf Multicannon).
 * - Magic: ignores non-combat Magic XP (alching/teleports/enchanting) by requiring active combat
 *   and excluding High/Low Alchemy animations (712, 713).
 */
public class UnpottedTracker
{
    private static final int ANIM_LOW_ALCH = 712;
    private static final int ANIM_HIGH_ALCH = 713;

    private final int[] lastXp = new int[Skill.values().length];
    private final boolean[] xpInitialized = new boolean[Skill.values().length];
    private long alertUntilNanos = 0L;
    private Skill lastTriggeredSkill = null;

    public void reset()
    {
        java.util.Arrays.fill(lastXp, 0);
        java.util.Arrays.fill(xpInitialized, false);
        alertUntilNanos = 0L;
        lastTriggeredSkill = null;
    }

    public void onStatChanged(
        Client client,
        ImmersiveCombatOverlaysConfig config,
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

        // Ignore non-combat Magic XP (alching, teleporting, enchanting)
        if (skill == Skill.MAGIC)
        {
            if (!combatState.isInCombat())
            {
                return;
            }
            Player local = client != null ? client.getLocalPlayer() : null;
            if (local != null)
            {
                int anim = local.getAnimation();
                if (anim == ANIM_LOW_ALCH || anim == ANIM_HIGH_ALCH)
                {
                    return;
                }
            }
        }
        else if (skill != Skill.RANGED && !combatState.isInCombat())
        {
            // Melee XP requires combat interaction; Ranged allows cannon XP without HP XP
            return;
        }

        // Suppress when Overload or Smelling Salts is active (prevents false alerts after brewing down in CoX/ToA)
        if (potionTimers != null && (
            !potionTimers.getCooldown(PotionEffect.OVERLOAD).isReady()
            || !potionTimers.getCooldown(PotionEffect.BLIGHTED_OVERLOAD).isReady()
            || !potionTimers.getCooldown(PotionEffect.SMELLING_SALTS).isReady()))
        {
            alertUntilNanos = 0L;
            return;
        }

        int threshold = config.unpottedThreshold();
        if (boostedLevel <= realLevel + threshold)
        {
            lastTriggeredSkill = skill;
            alertUntilNanos = nowNanos + Math.max(1L, (long) config.unpottedAlertDurationSeconds()) * 1_000_000_000L;
        }
        else if (skill == lastTriggeredSkill)
        {
            alertUntilNanos = 0L;
        }
    }

    public void onGameTick(Client client, ImmersiveCombatOverlaysConfig config)
    {
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

    public float getSeverity(ImmersiveCombatOverlaysConfig config, long nowNanos)
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
