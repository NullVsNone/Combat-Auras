package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.BoostPotion;
import com.immersivecombatoverlays.model.Cooldown;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * Estimates time until a useful re-dose on combat boost potions using boosted stats
 * and the shared stat decay clock (accounting for Preserve prayer).
 */
public class BoostPotionTimers
{
    private static final int VARBIT_PRAYER_PRESERVE = 5466;
    private static final int NORMAL_DECAY_TICKS = 100;
    private static final int PRESERVE_DECAY_TICKS = 150;

    private int ticksUntilNextDecay = NORMAL_DECAY_TICKS;
    private boolean previousPreserve = false;
    private int lastBoostSum = 0;
    private final boolean[] usedThisSession = new boolean[BoostPotion.values().length];

    public void reset()
    {
        ticksUntilNextDecay = NORMAL_DECAY_TICKS;
        previousPreserve = false;
        lastBoostSum = 0;
        java.util.Arrays.fill(usedThisSession, false);
    }

    public void onGameTick(Client client)
    {
        boolean preserve = client.getVarbitValue(VARBIT_PRAYER_PRESERVE) == 1;
        int decayPeriod = preserve ? PRESERVE_DECAY_TICKS : NORMAL_DECAY_TICKS;

        int boostSum = getPositiveBoostSum(client);
        if (boostSum > lastBoostSum)
        {
            ticksUntilNextDecay = decayPeriod;
        }
        else if (boostSum > 0)
        {
            if (preserve != previousPreserve)
            {
                ticksUntilNextDecay = preserve
                    ? Math.min(PRESERVE_DECAY_TICKS, Math.round(ticksUntilNextDecay * 1.5f))
                    : Math.max(1, Math.round(ticksUntilNextDecay / 1.5f));
            }
            ticksUntilNextDecay--;
            if (ticksUntilNextDecay <= 0)
            {
                ticksUntilNextDecay = decayPeriod;
            }
        }
        else
        {
            ticksUntilNextDecay = decayPeriod;
        }

        previousPreserve = preserve;
        lastBoostSum = boostSum;
    }

    private int getPositiveBoostSum(Client client)
    {
        int sum = 0;
        sum += Math.max(0, client.getBoostedSkillLevel(Skill.ATTACK) - client.getRealSkillLevel(Skill.ATTACK));
        sum += Math.max(0, client.getBoostedSkillLevel(Skill.STRENGTH) - client.getRealSkillLevel(Skill.STRENGTH));
        sum += Math.max(0, client.getBoostedSkillLevel(Skill.DEFENCE) - client.getRealSkillLevel(Skill.DEFENCE));
        sum += Math.max(0, client.getBoostedSkillLevel(Skill.RANGED) - client.getRealSkillLevel(Skill.RANGED));
        sum += Math.max(0, client.getBoostedSkillLevel(Skill.MAGIC) - client.getRealSkillLevel(Skill.MAGIC));
        return sum;
    }

    public Cooldown getCooldown(Client client, BoostPotion potion, int redoseThreshold)
    {
        if (potion == null)
        {
            return Cooldown.ready();
        }

        int maxRemainingLevels = 0;
        for (Skill skill : potion.getSkills())
        {
            int real = client.getRealSkillLevel(skill);
            int boosted = client.getBoostedSkillLevel(skill);
            int diff = boosted - real;
            int expectedMax = getExpectedMaxBoost(potion, skill, real);
            if (diff > expectedMax - redoseThreshold)
            {
                maxRemainingLevels = Math.max(maxRemainingLevels, diff - (expectedMax - redoseThreshold));
            }
        }

        if (maxRemainingLevels <= 0)
        {
            return Cooldown.ready();
        }

        int decayPeriod = previousPreserve ? PRESERVE_DECAY_TICKS : NORMAL_DECAY_TICKS;
        int estimatedTicks = ticksUntilNextDecay + (maxRemainingLevels - 1) * decayPeriod;
        int totalTicks = Math.max(estimatedTicks, decayPeriod * 2);
        usedThisSession[potion.ordinal()] = true;
        return Cooldown.active(estimatedTicks, totalTicks);
    }

    private int getExpectedMaxBoost(BoostPotion potion, Skill skill, int realLevel)
    {
        if (potion == BoostPotion.RANGED)
        {
            return 4 + (int) Math.floor(realLevel * 0.10);
        }
        if (potion == BoostPotion.MAGIC)
        {
            return 4;
        }
        if (potion == BoostPotion.COMBAT)
        {
            return 3 + (int) Math.floor(realLevel * 0.10);
        }
        if (potion == BoostPotion.BASTION && skill == Skill.RANGED)
        {
            return 4 + (int) Math.floor(realLevel * 0.10);
        }
        if (potion == BoostPotion.BATTLEMAGE && skill == Skill.MAGIC)
        {
            return 4;
        }
        return 5 + (int) Math.floor(realLevel * 0.15);
    }

    public boolean wasUsedThisSession(BoostPotion potion)
    {
        return potion != null && usedThisSession[potion.ordinal()];
    }
}
