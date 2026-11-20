package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.BoostPotion;
import com.immersivecombatoverlays.model.PotionEffect;

/**
 * Maps OSRS inventory items (by item name and known IDs) to PotionEffect and BoostPotion families.
 * Using item name matching avoids fragile ItemID constant renames across RuneLite releases
 * and automatically supports all dose counts (1)-(4), mixes, and raid variants.
 */
public final class PotionItems
{
    private PotionItems()
    {
    }

    public static PotionEffect getEffect(int itemId, String itemName)
    {
        if (itemName == null || itemName.isEmpty())
        {
            return null;
        }
        String lower = itemName.toLowerCase();

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
        if (lower.contains("anti-venom")) return PotionEffect.ANTIVENOM;
        if (lower.contains("antipoison") || lower.contains("antidote")) return PotionEffect.ANTIPOISON;
        if (lower.contains("guthix rest")) return PotionEffect.GUTHIX_REST;

        return null;
    }

    public static BoostPotion getBoost(int itemId, String itemName)
    {
        if (itemName == null || itemName.isEmpty())
        {
            return null;
        }
        String lower = itemName.toLowerCase();
        if (lower.contains("divine"))
        {
            return null;
        }
        if (lower.contains("guthixian root")) return BoostPotion.GUTHIXIAN_ROOT;
        if (lower.contains("super combat")) return BoostPotion.SUPER_COMBAT;
        if (lower.contains("combat potion")) return BoostPotion.COMBAT;
        if (lower.contains("bastion potion")) return BoostPotion.BASTION;
        if (lower.contains("battlemage potion")) return BoostPotion.BATTLEMAGE;
        if (lower.contains("attack potion") || lower.contains("super attack")) return BoostPotion.ATTACK;
        if (lower.contains("strength potion") || lower.contains("super strength")) return BoostPotion.STRENGTH;
        if (lower.contains("defence potion") || lower.contains("super defence")) return BoostPotion.DEFENCE;
        if (lower.contains("ranging potion")) return BoostPotion.RANGED;
        if (lower.contains("magic potion") || lower.contains("ancient brew") || lower.contains("forgotten brew")) return BoostPotion.MAGIC;
        if (lower.contains("armadyl brew")) return BoostPotion.RANGED;

        return null;
    }
}
