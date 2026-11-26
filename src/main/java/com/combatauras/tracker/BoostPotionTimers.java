package com.combatauras.tracker;

import com.combatauras.settings.SettingsSnapshot;
import com.combatauras.model.BoostPotion;
import com.combatauras.model.Cooldown;
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
@javax.inject.Singleton
public class BoostPotionTimers
{
    private static final int NORMAL_DECAY_TICKS = 100;
    private static final int PRESERVE_DECAY_TICKS = 150;
    private static final Skill[] COMBAT_SKILLS = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC};

    private int ticksUntilNextDecay = NORMAL_DECAY_TICKS;
    private boolean previousPreserve = false;
    private final int[] lastBoost = new int[COMBAT_SKILLS.length];
    private final boolean[] usedThisSession = new boolean[BoostPotion.values().length];
    private final int[] levelScratch = new int[3];

    public static int skillBit(Skill skill)
    {
        return 1 << skill.ordinal();
    }

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

    /**
     * Time until {@code potion} is worth re-dosing (its first tracked skill reaching its re-dose level),
     * using the per-skill re-dose levels from the config.
     *
     * @return null when none of the potion's skills are tracked (re-dose level 0), so callers treat the item as untracked
     */
    public Cooldown getCooldown(Client client, BoostPotion potion, SettingsSnapshot settings)
    {
        return getCooldown(client, potion, settings, 0);
    }

    /**
     * @param heldSkillMask bit {@code 1 << skill.ordinal()} set for skills an active divine potion holds boosted;
     *                      those skills are left out (they reset to base when the divine ends, not by decay)
     */
    public Cooldown getCooldown(Client client, BoostPotion potion, SettingsSnapshot settings, int heldSkillMask)
    {
        if (potion == null)
        {
            return null;
        }
        // Reused scratch buffer (client thread only): this runs per inventory item per frame
        Skill[] skills = potion.getSkills();
        for (int i = 0; i < skills.length; i++)
        {
            levelScratch[i] = (heldSkillMask & (1 << skills[i].ordinal())) != 0 ? 0 : settings.redoseLevel(skills[i]);
        }
        return getCooldown(client, potion, levelScratch);
    }

    /**
     * @param redoseLevels re-dose level (+N) per entry of {@code potion.getSkills()}; 0 = skill not tracked.
     *                     Each level is capped one below the potion's max boost so weaker potions still work.
     */
    public Cooldown getCooldown(Client client, BoostPotion potion, int[] redoseLevels)
    {
        boolean tracked = false;
        // The soonest skill decides: once any of the potion's skills is down to its re-dose level a dose is
        // worth it, e.g. Attack/Strength drained by a brew while Defence is still boosted
        int minRemainingLevels = Integer.MAX_VALUE;
        Skill[] skills = potion.getSkills();
        for (int i = 0; i < skills.length; i++)
        {
            if (redoseLevels[i] <= 0)
            {
                continue;
            }
            tracked = true;
            Skill skill = skills[i];
            int real = client.getRealSkillLevel(skill);
            int boost = client.getBoostedSkillLevel(skill) - real;
            int redoseAt = Math.min(redoseLevels[i], Math.max(0, potion.getMaxBoost(skill, real) - 1));
            minRemainingLevels = Math.min(minRemainingLevels, Math.max(0, boost - redoseAt));
        }
        if (!tracked)
        {
            return null;
        }
        if (minRemainingLevels <= 0)
        {
            return Cooldown.READY;
        }

        int decayPeriod = previousPreserve ? PRESERVE_DECAY_TICKS : NORMAL_DECAY_TICKS;
        int estimatedTicks = ticksUntilNextDecay + (minRemainingLevels - 1) * decayPeriod;
        usedThisSession[potion.ordinal()] = true;
        return Cooldown.active(estimatedTicks, Math.max(estimatedTicks, 4 * decayPeriod));
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
