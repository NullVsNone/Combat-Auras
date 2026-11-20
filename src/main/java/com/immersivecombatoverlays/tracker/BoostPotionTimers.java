package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.BoostPotion;
import com.immersivecombatoverlays.model.Cooldown;
import java.util.Arrays;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;

/**
 * Estimates time until a useful re-dose on combat boost potions using boosted stats
 * and the shared stat decay clock (accounting for Preserve prayer).
 *
 * The decay clock is global: drinking a potion does not reset it. We re-sync the clock only
 * when we observe a boosted stat decaying by one level.
 */
public class BoostPotionTimers
{
    private static final int NORMAL_DECAY_TICKS = 100;
    private static final int PRESERVE_DECAY_TICKS = 150;
    private static final Skill[] COMBAT_SKILLS = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC};

    private int ticksUntilNextDecay = NORMAL_DECAY_TICKS;
    private boolean previousPreserve = false;
    private final int[] lastBoost = new int[COMBAT_SKILLS.length];
    private final boolean[] usedThisSession = new boolean[BoostPotion.values().length];

    public void reset()
    {
        ticksUntilNextDecay = NORMAL_DECAY_TICKS;
        previousPreserve = false;
        Arrays.fill(lastBoost, 0);
        Arrays.fill(usedThisSession, false);
    }

    public void onGameTick(Client client)
    {
        boolean preserve = client.getVarbitValue(VarbitID.PRAYER_PRESERVE) == 1;
        int decayPeriod = preserve ? PRESERVE_DECAY_TICKS : NORMAL_DECAY_TICKS;

        if (preserve != previousPreserve)
        {
            ticksUntilNextDecay = preserve
                ? Math.min(PRESERVE_DECAY_TICKS, Math.round(ticksUntilNextDecay * 1.5f))
                : Math.max(1, Math.round(ticksUntilNextDecay / 1.5f));
        }

        boolean decayed = false;
        for (int i = 0; i < COMBAT_SKILLS.length; i++)
        {
            Skill skill = COMBAT_SKILLS[i];
            int boost = Math.max(0, client.getBoostedSkillLevel(skill) - client.getRealSkillLevel(skill));
            if (boost == lastBoost[i] - 1)
            {
                decayed = true;
            }
            lastBoost[i] = boost;
        }

        if (decayed)
        {
            ticksUntilNextDecay = decayPeriod;
        }
        else
        {
            ticksUntilNextDecay--;
            if (ticksUntilNextDecay <= 0)
            {
                ticksUntilNextDecay = decayPeriod;
            }
        }
        previousPreserve = preserve;
    }

    public Cooldown getCooldown(Client client, BoostPotion potion, int redoseThreshold)
    {
        if (potion == null)
        {
            return Cooldown.READY;
        }

        int maxRemainingLevels = 0;
        for (Skill skill : potion.getSkills())
        {
            int real = client.getRealSkillLevel(skill);
            int diff = client.getBoostedSkillLevel(skill) - real;
            int redoseAt = potion.getMaxBoost(skill, real) - redoseThreshold;
            if (diff > redoseAt)
            {
                maxRemainingLevels = Math.max(maxRemainingLevels, diff - redoseAt);
            }
        }

        if (maxRemainingLevels <= 0)
        {
            return Cooldown.READY;
        }

        int decayPeriod = previousPreserve ? PRESERVE_DECAY_TICKS : NORMAL_DECAY_TICKS;
        int estimatedTicks = ticksUntilNextDecay + (maxRemainingLevels - 1) * decayPeriod;
        int totalTicks = Math.max(estimatedTicks, (redoseThreshold + 1) * decayPeriod);
        usedThisSession[potion.ordinal()] = true;
        return Cooldown.active(estimatedTicks, totalTicks);
    }

    public int getTicksUntilNextDecay()
    {
        return ticksUntilNextDecay;
    }

    public boolean wasUsedThisSession(BoostPotion potion)
    {
        return potion != null && usedThisSession[potion.ordinal()];
    }
}
