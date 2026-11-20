package com.immersivecombatoverlays.model;

import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Potion and raid consumable effects. Variable IDs and tick multipliers mirror RuneLite's
 * core Timers & Buffs plugin so remaining time matches what the game reports.
 */
public enum PotionEffect
{
    SURGE("Surge potion", Source.VAR_SCALED, 10, 500, Category.COOLDOWN, VarbitID.SURGE_POTION_TIMER),
    OVERLOAD("Overload", Source.VAR_SCALED, 25, 500, Category.RAID,
        VarbitID.RAIDS_OVERLOAD_TIMER, VarbitID.NZONE_OVERLOAD_POTION_EFFECTS),
    BLIGHTED_OVERLOAD("Blighted overload", Source.VAR_SCALED, 25, 500, Category.RAID, VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS),
    PRAYER_ENHANCE("Prayer enhance", Source.VAR_FLAG, 0, 483, Category.RAID, VarbitID.RAIDS_PRAYERENHANCE_TIMER),
    SMELLING_SALTS("Smelling salts", Source.VAR_SCALED, 25, 800, Category.RAID, VarbitID.TOA_MIDRAIDLOOT_STATS_TIMER),
    LIQUID_ADRENALINE("Liquid adrenaline", Source.VAR_FLAG, 0, 250, Category.RAID, VarbitID.TOA_MIDRAIDLOOT_ENERGY_ACTIVE),
    SILK_DRESSING("Silk dressing", Source.CHAT, 0, 100, Category.RAID),
    BLESSED_CRYSTAL_SCARAB("Blessed crystal scarab", Source.CHAT, 0, 40, Category.RAID),
    STAMINA("Stamina", Source.VAR_SCALED, 10, 200, Category.EFFECT, VarbitID.STAMINA_DURATION),
    GOADING("Goading", Source.VAR_SCALED, 6, 600, Category.EFFECT, VarbitID.GOADING_POTION_TIMER),
    MENAPHITE_REMEDY("Menaphite remedy", Source.VAR_SCALED, 25, 500, Category.EFFECT, VarbitID.STATRENEWAL_POTION_TIMER),
    MOONLIGHT("Moonlight", Source.VAR_SCALED, 1, 350, Category.EFFECT, VarbitID.MOONLIGHT_POTION_TIME),
    PRAYER_REGENERATION("Prayer regeneration", Source.VAR_SCALED, 12, 800, Category.EFFECT, VarbitID.PRAYER_REGENERATION_POTION_TIMER),
    ANTIFIRE("Antifire", Source.VAR_SCALED, 30, 600, Category.EFFECT, VarbitID.ANTIFIRE_POTION),
    SUPER_ANTIFIRE("Super antifire", Source.VAR_SCALED, 20, 300, Category.EFFECT, VarbitID.SUPER_ANTIFIRE_POTION),
    ANTIPOISON("Antipoison", Source.POISON_VARP, 0, 150, Category.EFFECT),
    ANTIVENOM("Anti-venom", Source.POISON_VARP, 0, 300, Category.EFFECT),
    DIVINE_ATTACK("Divine attack", Source.VAR_SCALED, 1, 500, Category.EFFECT, VarbitID.DIVINEATTACK_POTION_TIME),
    DIVINE_STRENGTH("Divine strength", Source.VAR_SCALED, 1, 500, Category.EFFECT, VarbitID.DIVINESTRENGTH_POTION_TIME),
    DIVINE_DEFENCE("Divine defence", Source.VAR_SCALED, 1, 500, Category.EFFECT, VarbitID.DIVINEDEFENCE_POTION_TIME),
    DIVINE_RANGING("Divine ranging", Source.VAR_SCALED, 1, 500, Category.EFFECT, VarbitID.DIVINERANGE_POTION_TIME),
    DIVINE_MAGIC("Divine magic", Source.VAR_SCALED, 1, 500, Category.EFFECT, VarbitID.DIVINEMAGIC_POTION_TIME),
    DIVINE_COMBAT("Divine super combat", Source.VAR_SCALED, 1, 500, Category.EFFECT, VarbitID.DIVINECOMBAT_POTION_TIME),
    DIVINE_BASTION("Divine bastion", Source.VAR_SCALED, 1, 500, Category.EFFECT, VarbitID.DIVINEBASTION_POTION_TIME),
    DIVINE_BATTLEMAGE("Divine battlemage", Source.VAR_SCALED, 1, 500, Category.EFFECT, VarbitID.DIVINEBATTLEMAGE_POTION_TIME);

    /** Poison varp value at or below which the immunity is anti-venom rather than antipoison. */
    public static final int VENOM_VALUE_CUTOFF = -38;
    public static final int POISON_VARP = VarPlayerID.POISON;
    private static final int POISON_TICK_LENGTH = 30;

    public enum Category
    {
        COOLDOWN,
        EFFECT,
        RAID
    }

    public enum Source
    {
        /** Remaining ticks = varbit value x multiplier. */
        VAR_SCALED,
        /** Varbit is a flag; a fixed duration counts down from when it turns on. */
        VAR_FLAG,
        /** Started by a game message; fixed duration. */
        CHAT,
        /** Derived from the poison varp's negative (immunity) range. */
        POISON_VARP
    }

    private final String displayName;
    private final Source source;
    private final int tickMultiplier;
    private final int standardTicks;
    private final Category category;
    private final int[] varbitIds;

    PotionEffect(String displayName, Source source, int tickMultiplier, int standardTicks, Category category, int... varbitIds)
    {
        this.displayName = displayName;
        this.source = source;
        this.tickMultiplier = tickMultiplier;
        this.standardTicks = standardTicks;
        this.category = category;
        this.varbitIds = varbitIds;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public Source getSource()
    {
        return source;
    }

    public int getTickMultiplier()
    {
        return tickMultiplier;
    }

    public int getStandardTicks()
    {
        return standardTicks;
    }

    public Category getCategory()
    {
        return category;
    }

    /**
     * Varbits that drive this effect. For effects with several (e.g. CoX and NMZ overloads), the largest value wins.
     */
    public int[] getVarbitIds()
    {
        return varbitIds;
    }

    public boolean isDivine()
    {
        return name().startsWith("DIVINE_");
    }

    /**
     * Varbit that must be 1 for the effect to count as active, or -1. Stamina keeps a stale STAMINA_DURATION after
     * the effect ends; only STAMINA_ACTIVE says whether it is really running (matches the game's buff bar script).
     */
    public int getGateVarbitId()
    {
        return this == STAMINA ? VarbitID.STAMINA_ACTIVE : -1;
    }

    /**
     * Remaining immunity ticks for this effect given the poison varp, or 0 if it is not active.
     */
    public int poisonImmunityTicks(int poisonVarp)
    {
        if (this == ANTIPOISON && poisonVarp < 0 && poisonVarp >= VENOM_VALUE_CUTOFF)
        {
            return Math.abs(poisonVarp + 1) * POISON_TICK_LENGTH + POISON_TICK_LENGTH;
        }
        if (this == ANTIVENOM && poisonVarp < VENOM_VALUE_CUTOFF)
        {
            return Math.abs(poisonVarp + 1 - VENOM_VALUE_CUTOFF) * POISON_TICK_LENGTH + POISON_TICK_LENGTH;
        }
        return 0;
    }
}
