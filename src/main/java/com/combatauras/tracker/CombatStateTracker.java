package com.combatauras.tracker;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;

/**
 * Decides whether the local player is in combat from intent plus evidence, never from "interacting" alone.
 * <ul>
 *     <li>Intent: an Attack (or boss opening verb) click on an NPC or player, or a combat spell cast on one,
 *     engages that target. Following, trading, talking, pickpocketing and utility spells (Vengeance Other,
 *     Teleother, Heal Other...) never do.</li>
 *     <li>Sustained: while the player keeps interacting with the engaged target, combat stays on.</li>
 *     <li>Evidence: a hit dealt by the player ({@link Hitsplat#isMine()}) or a combat-relevant hit taken.
 *     Poison, venom, disease and heal splats are ignored, as is the stun damage of a failed pickpocket.</li>
 * </ul>
 * It also remembers recent pickpocketing ("thieving context", used by Shadow Veil reminders) and recent
 * non-combat magic casts (used to veto Magic XP in the Unpotted alert).
 * Time is counted in game ticks so lag and loading screens cannot skew it.
 */
@javax.inject.Singleton
public class CombatStateTracker
{
    /**
     * Ticks without combat activity before leaving combat (~10 s). Longer than the slowest monster attack
     * speed (8 ticks in RuneLite's Idle Notifier) so a slow enemy between hits never drops the flag.
     */
    static final int COMBAT_TIMEOUT_TICKS = 17;
    /** Ticks after a non-combat NPC click (Pickpocket, Talk-to...) during which that NPC's hits are not combat. */
    static final int NON_COMBAT_GRACE_TICKS = 5;
    /** Thieving context lasts ~60 s after the last Pickpocket click. */
    static final int THIEVING_CONTEXT_TICKS = 100;

    /** Opening verbs that start a fight (Kraken and whirlpools start with Disturb). */
    private static final Set<String> ENGAGE_OPTIONS = new HashSet<>(Arrays.asList("Attack", "Disturb"));
    private static final Set<String> THIEVING_OPTIONS = new HashSet<>(Arrays.asList("Pickpocket", "Steal-from"));
    /**
     * Spells cast on an NPC or player that are not attacks. Every other spell cast on a target defaults to combat,
     * so new combat spells are covered automatically.
     */
    private static final Set<String> UTILITY_TARGET_SPELLS = new HashSet<>(Arrays.asList(
        "Monster Examine", "Stat Spy", "Cure Other", "Heal Other", "Energy Transfer", "Vengeance Other",
        "Teleother Lumbridge", "Teleother Falador", "Teleother Camelot", "Dark Lure"));

    private int tick = 0;
    private boolean inCombat = false;
    private int lastActivityTick = Integer.MIN_VALUE / 2;
    private long lastCombatExitNanos = 0L;
    private int combatSessionId = 0;
    private boolean simulatedCombat = false;

    private Actor engaged;
    private Actor nonCombatTarget;
    private int nonCombatUntilTick = Integer.MIN_VALUE / 2;
    private int lastPickpocketTick = Integer.MIN_VALUE / 2;
    private int lastMyHitTick = Integer.MIN_VALUE / 2;
    private int lastUtilityMagicTick = Integer.MIN_VALUE / 2;

    public void reset()
    {
        inCombat = false;
        lastActivityTick = Integer.MIN_VALUE / 2;
        lastCombatExitNanos = 0L;
        combatSessionId = 0;
        engaged = null;
        nonCombatTarget = null;
        nonCombatUntilTick = Integer.MIN_VALUE / 2;
        lastPickpocketTick = Integer.MIN_VALUE / 2;
        lastMyHitTick = Integer.MIN_VALUE / 2;
        lastUtilityMagicTick = Integer.MIN_VALUE / 2;
    }

    public void onCombatActivity(long nowNanos, int breakReRemindSeconds)
    {
        if (!inCombat)
        {
            if (lastCombatExitNanos == 0L)
            {
                combatSessionId++;
            }
            else
            {
                long breakNanos = nowNanos - lastCombatExitNanos;
                long thresholdNanos = Math.max(1L, (long) breakReRemindSeconds) * 1_000_000_000L;
                if (breakNanos >= thresholdNanos)
                {
                    combatSessionId++;
                }
            }
            inCombat = true;
        }
        lastActivityTick = tick;
    }

    /**
     * @param action    the clicked entry's menu action
     * @param option    menu option text ("Attack", "Pickpocket", "Cast"...)
     * @param target    menu target text (for spells "Spell -> Target", tags allowed)
     * @param actor     the NPC or player the entry targets, or null
     */
    public void onMenuOptionClicked(MenuAction action, String option, String target, Actor actor,
        long nowNanos, int breakReRemindSeconds)
    {
        if (action == null || option == null)
        {
            return;
        }
        switch (action)
        {
            case NPC_FIRST_OPTION:
            case NPC_SECOND_OPTION:
            case NPC_THIRD_OPTION:
            case NPC_FOURTH_OPTION:
            case NPC_FIFTH_OPTION:
                if (ENGAGE_OPTIONS.contains(option))
                {
                    engage(actor, nowNanos, breakReRemindSeconds);
                }
                else
                {
                    if (THIEVING_OPTIONS.contains(option))
                    {
                        lastPickpocketTick = tick;
                    }
                    nonCombatTarget = actor;
                    nonCombatUntilTick = tick + NON_COMBAT_GRACE_TICKS;
                    if (engaged == actor)
                    {
                        engaged = null;
                    }
                }
                break;
            case PLAYER_FIRST_OPTION:
            case PLAYER_SECOND_OPTION:
            case PLAYER_THIRD_OPTION:
            case PLAYER_FOURTH_OPTION:
            case PLAYER_FIFTH_OPTION:
            case PLAYER_SIXTH_OPTION:
            case PLAYER_SEVENTH_OPTION:
            case PLAYER_EIGHTH_OPTION:
                // Follow, Trade with, Challenge... never arm combat; a duel's first hit does
                if ("Attack".equals(option))
                {
                    engage(actor, nowNanos, breakReRemindSeconds);
                }
                break;
            case WIDGET_TARGET_ON_NPC:
            case WIDGET_TARGET_ON_PLAYER:
                if ("Cast".equals(option))
                {
                    if (isUtilityTargetSpell(target))
                    {
                        lastUtilityMagicTick = tick;
                    }
                    else
                    {
                        engage(actor, nowNanos, breakReRemindSeconds);
                    }
                }
                break;
            case WIDGET_TARGET_ON_WIDGET:
            case WIDGET_TARGET_ON_GAME_OBJECT:
            case WIDGET_TARGET_ON_GROUND_ITEM:
                // Alchemy, enchanting, superheat, charge orbs, telegrab...
                if ("Cast".equals(option))
                {
                    lastUtilityMagicTick = tick;
                }
                break;
            case CC_OP:
                // Self-cast spells from the spellbook (teleports, thralls, Vengeance, offerings...)
                if ("Cast".equals(option))
                {
                    lastUtilityMagicTick = tick;
                }
                break;
            default:
                break;
        }
    }

    private void engage(Actor actor, long nowNanos, int breakReRemindSeconds)
    {
        if (actor == null)
        {
            return;
        }
        engaged = actor;
        if (nonCombatTarget == actor)
        {
            nonCombatTarget = null;
        }
        onCombatActivity(nowNanos, breakReRemindSeconds);
    }

    /**
     * @param hitsplatType {@link Hitsplat#getHitsplatType()}
     * @param mine         {@link Hitsplat#isMine()}: damage dealt by the local player
     */
    public void onHitsplat(Actor actor, int hitsplatType, boolean mine, Player local, long nowNanos, int breakReRemindSeconds)
    {
        if (actor == null || local == null)
        {
            return;
        }
        if (actor == local)
        {
            if (!isCombatRelevant(hitsplatType))
            {
                return;
            }
            Actor interacting = local.getInteracting();
            if (nonCombatTarget != null && tick <= nonCombatUntilTick && interacting == nonCombatTarget)
            {
                return; // stun damage from a failed pickpocket
            }
            if (engaged == null && interacting != null && interacting.getInteracting() == local)
            {
                engaged = interacting; // retaliating against something that attacked first
            }
            onCombatActivity(nowNanos, breakReRemindSeconds);
        }
        else if (mine)
        {
            // Hits from other players, thralls or pets on the same target do not count
            lastMyHitTick = tick;
            onCombatActivity(nowNanos, breakReRemindSeconds);
        }
    }

    public void onGameTick(Client client, long nowNanos, int breakReRemindSeconds)
    {
        tick++;
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return;
        }
        if (engaged != null)
        {
            if (engaged.isDead())
            {
                engaged = null;
            }
            else if (local.getInteracting() == engaged)
            {
                onCombatActivity(nowNanos, breakReRemindSeconds);
            }
        }
        if (inCombat && tick - lastActivityTick >= COMBAT_TIMEOUT_TICKS)
        {
            inCombat = false;
            engaged = null;
            lastCombatExitNanos = nowNanos;
        }
    }

    static boolean isCombatRelevant(int hitsplatType)
    {
        switch (hitsplatType)
        {
            case HitsplatID.POISON:
            case HitsplatID.VENOM:
            case HitsplatID.DISEASE:
            case HitsplatID.DISEASE_BLOCKED:
            case HitsplatID.HEAL:
            case HitsplatID.CYAN_UP:
            case HitsplatID.CYAN_DOWN:
                return false;
            default:
                return true; // damage, blocks (zero hits), bleed, burn, corruption, prayer drain, doom...
        }
    }

    static boolean isUtilityTargetSpell(String target)
    {
        if (target == null)
        {
            return false;
        }
        // Split before stripping tags: the tag stripper would also eat the '>' of "->"
        int arrow = target.indexOf("->");
        String spell = ChatText.clean(arrow >= 0 ? target.substring(0, arrow) : target).trim();
        return UTILITY_TARGET_SPELLS.contains(spell);
    }

    public void setSimulatedCombat(boolean simulated)
    {
        this.simulatedCombat = simulated;
    }

    public boolean isInCombat()
    {
        return inCombat || simulatedCombat;
    }

    /** Pickpocketed within the last ~60 s (Shadow Veil's reminder context). */
    public boolean isThieving()
    {
        return tick - lastPickpocketTick <= THIEVING_CONTEXT_TICKS;
    }

    /** A hit dealt by the player landed within {@code ticks} ticks. */
    public boolean dealtHitWithin(int ticks)
    {
        return tick - lastMyHitTick <= ticks;
    }

    /** A non-attack spell (teleport, alchemy, enchant, utility-on-target...) was cast within {@code ticks} ticks. */
    public boolean castUtilityMagicWithin(int ticks)
    {
        return tick - lastUtilityMagicTick <= ticks;
    }

    public int getCombatSessionId()
    {
        return combatSessionId;
    }
}
