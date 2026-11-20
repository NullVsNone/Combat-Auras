package com.immersivecombatoverlays.model;

import net.runelite.api.Skill;

public enum BoostPotion
{
    ATTACK(Skill.ATTACK),
    STRENGTH(Skill.STRENGTH),
    DEFENCE(Skill.DEFENCE),
    RANGED(Skill.RANGED),
    MAGIC(Skill.MAGIC),
    COMBAT(Skill.ATTACK, Skill.STRENGTH),
    SUPER_COMBAT(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE),
    BASTION(Skill.RANGED, Skill.DEFENCE),
    BATTLEMAGE(Skill.MAGIC, Skill.DEFENCE),
    GUTHIXIAN_ROOT(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE);

    private final Skill[] skills;

    BoostPotion(Skill... skills)
    {
        this.skills = skills;
    }

    public Skill[] getSkills()
    {
        return skills;
    }
}
