package com.combatauras.tracker;

import com.combatauras.model.BoostPotion;
import com.combatauras.model.PotionEffect;
import java.util.Locale;

/**
 * Name rules that map OSRS item names to PotionEffect and BoostPotion families. Every dose count, mixes and raid
 * variants match without a large ID table. Callers go through {@link ConsumableCatalog}, which caches the result
 * per item id, so these string scans run once per distinct item.
 */
public final class PotionItems
{
    private PotionItems()
    {
    }

    public static PotionEffect getEffect(String itemName)
    {
        return itemName == null || itemName.isEmpty() ? null : classifyEffect(itemName.toLowerCase(Locale.ROOT));
    }

    public static BoostPotion getBoost(String itemName)
    {
        return itemName == null || itemName.isEmpty() ? null : classifyBoost(itemName.toLowerCase(Locale.ROOT));
    }

    static PotionEffect classifyEffect(String lower)
    {
        // Cooldown & raid consumables
        if (lower.contains("surge potion")) return PotionEffect.SURGE;
        if (lower.contains("blighted overload")) return PotionEffect.BLIGHTED_OVERLOAD;
        if (lower.contains("overload")) return PotionEffect.OVERLOAD;
        if (lower.contains("prayer enhance")) return PotionEffect.PRAYER_ENHANCE;
        if (lower.contains("smelling salts")) return PotionEffect.SMELLING_SALTS;
        if (lower.contains("liquid adrenaline")) return PotionEffect.LIQUID_ADRENALINE;
        if (lower.contains("silk dressing")) return PotionEffect.SILK_DRESSING;
        if (lower.contains("blessed crystal scarab")) return PotionEffect.BLESSED_CRYSTAL_SCARAB;
        if (lower.contains("moonlight potion")) return PotionEffect.MOONLIGHT;

        // Divine potions (must check before non-divine)
        if (lower.contains("divine super combat")) return PotionEffect.DIVINE_COMBAT;
        if (lower.contains("divine bastion")) return PotionEffect.DIVINE_BASTION;
        if (lower.contains("divine battlemage")) return PotionEffect.DIVINE_BATTLEMAGE;
        if (lower.contains("divine super attack")) return PotionEffect.DIVINE_ATTACK;
        if (lower.contains("divine super strength")) return PotionEffect.DIVINE_STRENGTH;
        if (lower.contains("divine super defence")) return PotionEffect.DIVINE_DEFENCE;
        if (lower.contains("divine ranging")) return PotionEffect.DIVINE_RANGING;
        if (lower.contains("divine magic")) return PotionEffect.DIVINE_MAGIC;

        // Standard duration effects
        if (lower.contains("stamina")) return PotionEffect.STAMINA;
        if (lower.contains("goading potion")) return PotionEffect.GOADING;
        if (lower.contains("menaphite remedy")) return PotionEffect.MENAPHITE_REMEDY;
        if (lower.contains("prayer regeneration")) return PotionEffect.PRAYER_REGENERATION;
        if (lower.contains("super antifire")) return PotionEffect.SUPER_ANTIFIRE;
        if (lower.contains("antifire")) return PotionEffect.ANTIFIRE;
        if (lower.contains("anti-venom") || lower.contains("antivenom")) return PotionEffect.ANTIVENOM;
        if (lower.contains("antipoison") || lower.contains("antidote") || lower.contains("sanfew serum")) return PotionEffect.ANTIPOISON;

        return null;
    }

    static BoostPotion classifyBoost(String lower)
    {
        if (lower.contains("divine"))
        {
            return null;
        }
        if (lower.contains("guthixian root")) return BoostPotion.GUTHIXIAN_ROOT;
        BoostPotion raid = classifyRaidBoost(lower);
        if (raid != null) return raid;
        if (lower.contains("super combat")) return BoostPotion.SUPER_COMBAT;
        if (lower.contains("combat potion") || lower.contains("combat mix")) return BoostPotion.COMBAT;
        if (lower.contains("bastion potion")) return BoostPotion.BASTION;
        if (lower.contains("battlemage potion")) return BoostPotion.BATTLEMAGE;
        if (lower.contains("super attack") || lower.contains("super attack mix")) return BoostPotion.SUPER_ATTACK;
        if (lower.contains("super strength")) return BoostPotion.SUPER_STRENGTH;
        if (lower.contains("super defence")) return BoostPotion.SUPER_DEFENCE;
        if (lower.contains("super ranging")) return BoostPotion.SUPER_RANGED;
        if (lower.contains("super magic")) return BoostPotion.SUPER_MAGIC;
        if (lower.contains("attack potion") || lower.contains("attack mix")) return BoostPotion.ATTACK;
        if (lower.contains("strength potion") || lower.contains("strength mix")) return BoostPotion.STRENGTH;
        if (lower.contains("defence potion") || lower.contains("defence mix")) return BoostPotion.DEFENCE;
        if (lower.contains("ranging potion") || lower.contains("ranging mix")) return BoostPotion.RANGED;
        if (lower.contains("ancient brew") || lower.contains("ancient mix")) return BoostPotion.ANCIENT_BREW;
        if (lower.contains("forgotten brew")) return BoostPotion.FORGOTTEN_BREW;
        if (lower.contains("magic potion") || lower.contains("magic mix")) return BoostPotion.MAGIC;

        return null;
    }

    /** Chambers of Xeric combat potions: "Elder (-)(4)", "Twisted potion (2)", "Kodai (+)(1)" and so on. */
    private static BoostPotion classifyRaidBoost(String lower)
    {
        int tier;
        if (lower.contains("(+)")) tier = 2;
        else if (lower.contains("(-)")) tier = 0;
        else if (lower.contains(" potion")) tier = 1;
        else return null;
        if (lower.startsWith("elder ")) return tier == 2 ? BoostPotion.ELDER_STRONG : tier == 1 ? BoostPotion.ELDER : BoostPotion.ELDER_WEAK;
        if (lower.startsWith("twisted ")) return tier == 2 ? BoostPotion.TWISTED_STRONG : tier == 1 ? BoostPotion.TWISTED : BoostPotion.TWISTED_WEAK;
        if (lower.startsWith("kodai ")) return tier == 2 ? BoostPotion.KODAI_STRONG : tier == 1 ? BoostPotion.KODAI : BoostPotion.KODAI_WEAK;
        return null;
    }
}
