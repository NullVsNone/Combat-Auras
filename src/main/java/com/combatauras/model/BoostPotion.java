package com.combatauras.model;

import net.runelite.api.Skill;

/**
 * Combat stat boost potions with their boost formulas (flat + percent of real level).
 */
public enum BoostPotion
{
    ATTACK(3, 10, Skill.ATTACK),
    STRENGTH(3, 10, Skill.STRENGTH),
    DEFENCE(3, 10, Skill.DEFENCE),
    SUPER_ATTACK(5, 15, Skill.ATTACK),
    SUPER_STRENGTH(5, 15, Skill.STRENGTH),
    SUPER_DEFENCE(5, 15, Skill.DEFENCE),
    RANGED(4, 10, Skill.RANGED),
    SUPER_RANGED(5, 15, Skill.RANGED),
    MAGIC(4, 0, Skill.MAGIC),
    SUPER_MAGIC(5, 15, Skill.MAGIC),
    ANCIENT_BREW(2, 5, Skill.MAGIC),
    FORGOTTEN_BREW(3, 8, Skill.MAGIC),
    COMBAT(3, 10, Skill.ATTACK, Skill.STRENGTH),
    SUPER_COMBAT(5, 15, Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE),
    BASTION(5, 15, Skill.RANGED, Skill.DEFENCE),
    BATTLEMAGE(5, 15, Skill.MAGIC, Skill.DEFENCE),
    GUTHIXIAN_ROOT(5, 15, Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE),
    // Chambers of Xeric: (-) / normal / (+) strengths
    ELDER_WEAK(4, 10, Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE),
    ELDER(5, 13, Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE),
    ELDER_STRONG(6, 16, Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE),
    TWISTED_WEAK(4, 10, Skill.RANGED, Skill.DEFENCE),
    TWISTED(5, 13, Skill.RANGED, Skill.DEFENCE),
    TWISTED_STRONG(6, 16, Skill.RANGED, Skill.DEFENCE),
    KODAI_WEAK(4, 10, Skill.MAGIC),
    KODAI(5, 13, Skill.MAGIC),
    KODAI_STRONG(6, 16, Skill.MAGIC);

    private final int flat;
    private final int percent;
    private final Skill[] skills;

    BoostPotion(int flat, int percent, Skill... skills)
    {
        this.flat = flat;
        this.percent = percent;
        this.skills = skills;
    }

    public Skill[] getSkills()
    {
        return skills;
    }

    /**
     * Maximum boost this potion gives to {@code skill} at the given real level.
     */
    public int getMaxBoost(Skill skill, int realLevel)
    {
        // Bastion boosts Ranged like a ranging potion; Battlemage boosts Magic like a magic potion
        if (this == BASTION && skill == Skill.RANGED)
        {
            return RANGED.getMaxBoost(skill, realLevel);
        }
        if (this == BATTLEMAGE && skill == Skill.MAGIC)
        {
            return MAGIC.getMaxBoost(skill, realLevel);
        }
        return flat + (realLevel * percent) / 100;
    }
}
