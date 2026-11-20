package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.Cooldown;

/**
 * Tracks 1-tick combo-eat / 3-tick consumable delays (regular food, potions, combo food)
 * plus the 7-tick delayed second heal on Varlamore hunter meats.
 */
public class FoodCooldownTracker
{
    public enum ConsumableType
    {
        REGULAR_FOOD,
        HUNTER_MEAT,
        POTION,
        COMBO_FOOD
    }

    private int foodLockTicks = 0;
    private int potionLockTicks = 0;
    private int comboLockTicks = 0;
    private int hunterDelayedHealTicks = 0;

    public void reset()
    {
        foodLockTicks = 0;
        potionLockTicks = 0;
        comboLockTicks = 0;
        hunterDelayedHealTicks = 0;
    }

    public void onGameTick()
    {
        if (foodLockTicks > 0) foodLockTicks--;
        if (potionLockTicks > 0) potionLockTicks--;
        if (comboLockTicks > 0) comboLockTicks--;
        if (hunterDelayedHealTicks > 0) hunterDelayedHealTicks--;
    }

    public void onConsume(String option, String itemName)
    {
        if (itemName == null || option == null)
        {
            return;
        }
        if (!"Eat".equalsIgnoreCase(option) && !"Drink".equalsIgnoreCase(option))
        {
            return;
        }
        ConsumableType type = classify(itemName);
        if (type == null)
        {
            return;
        }

        switch (type)
        {
            case REGULAR_FOOD:
                foodLockTicks = 3;
                break;
            case HUNTER_MEAT:
                foodLockTicks = 3;
                hunterDelayedHealTicks = 7;
                break;
            case POTION:
                foodLockTicks = 3;
                potionLockTicks = 3;
                break;
            case COMBO_FOOD:
                foodLockTicks = 3;
                potionLockTicks = 3;
                comboLockTicks = 3;
                break;
        }
    }

    public Cooldown getCooldown(String itemName)
    {
        ConsumableType type = classify(itemName);
        if (type == null)
        {
            return null;
        }
        switch (type)
        {
            case REGULAR_FOOD:
                return foodLockTicks > 0 ? Cooldown.active(foodLockTicks, 3) : null;
            case HUNTER_MEAT:
                if (foodLockTicks > 0)
                {
                    return Cooldown.active(foodLockTicks, 3);
                }
                return hunterDelayedHealTicks > 0 ? Cooldown.active(hunterDelayedHealTicks, 7) : null;
            case POTION:
                return potionLockTicks > 0 ? Cooldown.active(potionLockTicks, 3) : null;
            case COMBO_FOOD:
                return comboLockTicks > 0 ? Cooldown.active(comboLockTicks, 3) : null;
            default:
                return null;
        }
    }

    public static ConsumableType classify(String itemName)
    {
        if (itemName == null || itemName.isEmpty())
        {
            return null;
        }
        String lower = itemName.toLowerCase();

        if (lower.contains("karambwan")
            || lower.contains("toad crunchies")
            || lower.contains("choc bomb")
            || lower.contains("tangled toad")
            || lower.contains("worm hole"))
        {
            return ConsumableType.COMBO_FOOD;
        }

        if (lower.contains("antelope")
            || lower.contains("kebbit")
            || lower.contains("larupia")
            || lower.contains("graahk")
            || lower.contains("kyatt")
            || lower.contains("pyre fox"))
        {
            return ConsumableType.HUNTER_MEAT;
        }

        if (lower.contains("saradomin brew")
            || lower.contains("super restore")
            || lower.contains("prayer potion")
            || lower.contains("sanfew serum")
            || lower.contains("guthix rest"))
        {
            return ConsumableType.POTION;
        }

        if (lower.contains("shark")
            || lower.contains("anglerfish")
            || lower.contains("manta ray")
            || lower.contains("sea turtle")
            || lower.contains("dark crab")
            || lower.contains("monkfish")
            || lower.contains("lobster")
            || lower.contains("swordfish")
            || lower.contains("bass")
            || lower.contains("tuna potato")
            || lower.contains("pineapple pizza")
            || lower.contains("cooked"))
        {
            return ConsumableType.REGULAR_FOOD;
        }

        return null;
    }
}
