package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.BoostPotion;
import com.immersivecombatoverlays.model.PotionEffect;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps OSRS inventory items (by item name) to PotionEffect and BoostPotion families.
 * Name matching supports every dose count (1)-(4), mixes, and raid variants without a large ID table.
 *
 * Results are memoised per name: these lookups run for every inventory item on every frame and on
 * every menu sort, so the string scanning must only happen once per distinct item.
 * All callers run on the client thread.
 */
public final class PotionItems
{
    private static final int CACHE_LIMIT = 512;
    private static final Object NONE = new Object();
    private static final Map<String, Object> EFFECT_CACHE = new HashMap<>();
    private static final Map<String, Object> BOOST_CACHE = new HashMap<>();

    private PotionItems()
    {
    }

    public static PotionEffect getEffect(int itemId, String itemName)
    {
        if (itemName == null || itemName.isEmpty())
        {
            return null;
        }
        Object cached = EFFECT_CACHE.get(itemName);
        if (cached == null)
        {
            PotionEffect effect = classifyEffect(itemName.toLowerCase(java.util.Locale.ROOT));
            cached = effect != null ? effect : NONE;
            if (EFFECT_CACHE.size() >= CACHE_LIMIT)
            {
                EFFECT_CACHE.clear();
            }
            EFFECT_CACHE.put(itemName, cached);
        }
        return cached == NONE ? null : (PotionEffect) cached;
    }

    public static BoostPotion getBoost(int itemId, String itemName)
    {
        if (itemName == null || itemName.isEmpty())
        {
            return null;
        }
        Object cached = BOOST_CACHE.get(itemName);
        if (cached == null)
        {
            BoostPotion boost = classifyBoost(itemName.toLowerCase(java.util.Locale.ROOT));
            cached = boost != null ? boost : NONE;
            if (BOOST_CACHE.size() >= CACHE_LIMIT)
            {
                BOOST_CACHE.clear();
            }
            BOOST_CACHE.put(itemName, cached);
        }
        return cached == NONE ? null : (BoostPotion) cached;
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
        if (lower.contains("antipoison") || lower.contains("antidote")) return PotionEffect.ANTIPOISON;

        return null;
    }

    static BoostPotion classifyBoost(String lower)
    {
        if (lower.contains("divine"))
        {
            return null;
        }
        if (lower.contains("guthixian root")) return BoostPotion.GUTHIXIAN_ROOT;
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
}
