package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.Cooldown;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;

/**
 * Eat / drink delays, modelled as the game's independent timers (OSRS Wiki, "Food" and "Combo eating"):
 * <ul>
 *     <li>Regular food and barbarian mixes set the food timer (3 ticks).</li>
 *     <li>Combo food (karambwan, halibut, gnome food...) ignores the food timer but sets it, and sets its own
 *     combo timer, so food-then-karambwan works and karambwan-then-food does not.</li>
 *     <li>Potions use a separate potion timer, so a brew fits into a combo eat.</li>
 *     <li>Varlamore hunter meat sets the food timer plus a delayed second heal 7 ticks later.</li>
 * </ul>
 * Items are classified from their real inventory actions, so raw, burnt, uncooked, noted items and furs never
 * match. A lock starts only when the clicked item actually leaves its slot, and clicks the game would ignore
 * (timer still running) are ignored here too, so spam-clicking never extends a lock.
 */
public class FoodCooldownTracker
{
    public enum ConsumableType
    {
        REGULAR_FOOD,
        HUNTER_MEAT,
        POTION,
        MIX,
        COMBO_FOOD
    }

    static final int LOCK_TICKS = 3;
    static final int HUNTER_HEAL_TICKS = 7;
    /** A click not confirmed by an inventory change within this many ticks was rejected by the game. */
    static final int CONFIRM_TICKS = 2;

    /** Combo foods by exact name; every "Premade ..." gnome food is combo food too. */
    private static final Set<String> COMBO_FOODS = new HashSet<>(Arrays.asList(
        "cooked karambwan", "halibut", "smelly kebab", "paddlefish", "toad's legs",
        "toad crunchies", "spicy crunchies", "worm crunchies", "chocchip crunchies",
        "fruit batta", "toad batta", "worm batta", "vegetable batta", "cheese+tom batta",
        "worm hole", "veg ball", "tangled toad's legs", "chocolate bomb"));
    private static final String[] HUNTER_MEATS = {
        "kebbit", "larupia", "graahk", "kyatt", "pyre fox", "antelope"};

    private int tick = 0;
    private int foodUntil = 0;
    private int comboUntil = 0;
    private int potionUntil = 0;
    private int hunterHealAt = 0;

    private ConsumableType pendingType;
    private int pendingSlot = -1;
    private int pendingItemId = -1;
    private int pendingTick = 0;

    private static final Object NONE = new Object();
    private final Map<Integer, Object> classifyCache = new HashMap<>(); // client thread only

    public void reset()
    {
        foodUntil = comboUntil = potionUntil = hunterHealAt = 0;
        pendingType = null;
        pendingSlot = -1;
    }

    public void onGameTick()
    {
        tick++;
        if (pendingType != null && tick - pendingTick > CONFIRM_TICKS)
        {
            pendingType = null; // the game rejected the click (e.g. still stunned, menu changed)
        }
    }

    /**
     * An Eat / Drink click on an inventory slot. Ignored if the matching timer is still running.
     */
    public void onConsumeClick(int slot, int itemId, ConsumableType type)
    {
        if (type == null || slot < 0 || isBlocked(type))
        {
            return;
        }
        pendingType = type;
        pendingSlot = slot;
        pendingItemId = itemId;
        pendingTick = tick;
    }

    /**
     * Confirms the pending click once its slot no longer holds the same item (dose drunk, bite eaten, item gone).
     */
    public void onInventoryChanged(ItemContainer inventory)
    {
        if (pendingType == null || inventory == null)
        {
            return;
        }
        Item now = inventory.getItem(pendingSlot);
        if (now == null || now.getId() != pendingItemId)
        {
            start(pendingType);
            pendingType = null;
        }
    }

    private boolean isBlocked(ConsumableType type)
    {
        switch (type)
        {
            case POTION:
                return potionUntil > tick;
            case COMBO_FOOD:
                return comboUntil > tick;
            default:
                return foodUntil > tick;
        }
    }

    void start(ConsumableType type)
    {
        switch (type)
        {
            case REGULAR_FOOD:
            case MIX:
                foodUntil = tick + LOCK_TICKS;
                break;
            case HUNTER_MEAT:
                foodUntil = tick + LOCK_TICKS;
                hunterHealAt = tick + HUNTER_HEAL_TICKS; // a newer hunter meat replaces the pending heal
                break;
            case COMBO_FOOD:
                foodUntil = tick + LOCK_TICKS;
                comboUntil = tick + LOCK_TICKS;
                break;
            case POTION:
                potionUntil = tick + LOCK_TICKS;
                break;
            default:
                break;
        }
    }

    /**
     * Remaining lock for an item of this type, or null when it can be consumed now.
     */
    public Cooldown getCooldown(ConsumableType type)
    {
        if (type == null)
        {
            return null;
        }
        switch (type)
        {
            case POTION:
                return lock(potionUntil, LOCK_TICKS);
            case COMBO_FOOD:
                return lock(comboUntil, LOCK_TICKS);
            case HUNTER_MEAT:
                Cooldown food = lock(foodUntil, LOCK_TICKS);
                return food != null ? food : lock(hunterHealAt, HUNTER_HEAL_TICKS);
            default:
                return lock(foodUntil, LOCK_TICKS);
        }
    }

    private Cooldown lock(int until, int total)
    {
        int remaining = until - tick;
        return remaining > 0 ? Cooldown.active(remaining, total) : null;
    }

    /**
     * Memoised classification from item data; null for anything that cannot be eaten or drunk.
     */
    public ConsumableType classify(int itemId, IntFunction<ItemComposition> lookup)
    {
        Object cached = classifyCache.get(itemId);
        if (cached == null)
        {
            ItemComposition comp = lookup.apply(itemId);
            ConsumableType type = comp == null ? null
                : classify(comp.getName(), comp.getInventoryActions(), comp.getNote() != -1);
            cached = type != null ? type : NONE;
            classifyCache.put(itemId, cached);
        }
        return cached == NONE ? null : (ConsumableType) cached;
    }

    static ConsumableType classify(String name, String[] actions, boolean noted)
    {
        if (name == null || noted)
        {
            return null;
        }
        boolean eat = hasAction(actions, "Eat");
        boolean drink = hasAction(actions, "Drink");
        if (!eat && !drink)
        {
            return null; // raw, burnt, uncooked, furs, equipment...
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.startsWith("premade ") || COMBO_FOODS.contains(lower))
        {
            return ConsumableType.COMBO_FOOD;
        }
        for (String meat : HUNTER_MEATS)
        {
            if (lower.contains(meat))
            {
                return ConsumableType.HUNTER_MEAT;
            }
        }
        boolean dosed = lower.length() > 3 && lower.endsWith(")") && Character.isDigit(lower.charAt(lower.length() - 2));
        if (drink && dosed)
        {
            return lower.contains(" mix(") ? ConsumableType.MIX : ConsumableType.POTION;
        }
        return ConsumableType.REGULAR_FOOD; // fish, pies, cakes, pizzas, wine...
    }

    private static boolean hasAction(String[] actions, String verb)
    {
        if (actions == null)
        {
            return false;
        }
        for (String a : actions)
        {
            if (verb.equals(a))
            {
                return true;
            }
        }
        return false;
    }
}
