package com.combatauras.tracker;

import com.combatauras.model.Cooldown;
import com.combatauras.model.PotionEffect;
import java.util.EnumMap;
import net.runelite.api.Client;

/**
 * Tracks active potion and raid consumable timers independently of display settings.
 * Varbit-driven effects are re-read every tick, so the game is always the source of truth.
 */
@javax.inject.Singleton
public class PotionTimers
{
    static final String MSG_SILK_DRESSING = "You quickly apply the dressing to your wounds.";
    static final String MSG_BLESSED_CRYSTAL_SCARAB = "You crack the crystal in your hand.";

    private static final class State
    {
        int remainingTicks;
        int totalTicks;
        int lastVarValue;
        boolean usedThisSession;
        boolean debug;
    }

    private final EnumMap<PotionEffect, State> states = new EnumMap<>(PotionEffect.class);

    public PotionTimers()
    {
        for (PotionEffect effect : PotionEffect.values())
        {
            states.put(effect, new State());
        }
    }

    public void reset()
    {
        for (State state : states.values())
        {
            state.remainingTicks = 0;
            state.totalTicks = 0;
            state.lastVarValue = 0;
            state.usedThisSession = false;
            state.debug = false;
        }
    }

    /**
     * Death ends every potion effect.
     */
    public void onPlayerDeath()
    {
        for (State state : states.values())
        {
            state.remainingTicks = 0;
            state.lastVarValue = 0;
            state.debug = false;
        }
    }

    public void onGameTick(Client client)
    {
        int poisonVarp = client.getVarpValue(PotionEffect.POISON_VARP);
        for (PotionEffect effect : PotionEffect.values())
        {
            State state = states.get(effect);
            if (state.debug)
            {
                tickDown(state);
                continue;
            }

            switch (effect.getSource())
            {
                case VAR_SCALED:
                {
                    int val = maxVarbit(client, effect.getVarbitIds());
                    int gate = effect.getGateVarbitId();
                    if (gate > 0 && client.getVarbitValue(gate) != 1)
                    {
                        val = 0; // stale duration value: effect is not actually running
                    }
                    if (val <= 0)
                    {
                        state.remainingTicks = 0;
                    }
                    else
                    {
                        int mult = effect.getTickMultiplier();
                        int ticks = val * mult;
                        if (val > state.lastVarValue)
                        {
                            // Fresh dose (or first observation): restart the bar
                            state.totalTicks = Math.max(effect.getStandardTicks(), ticks);
                            state.usedThisSession = true;
                            state.remainingTicks = ticks;
                        }
                        else if (val == state.lastVarValue && state.remainingTicks > 0)
                        {
                            // Coarse varbits (x10..x30) only change every few seconds: interpolate per tick
                            state.remainingTicks = Math.max((val - 1) * mult + 1, state.remainingTicks - 1);
                        }
                        else
                        {
                            state.remainingTicks = ticks;
                        }
                    }
                    state.lastVarValue = val;
                    break;
                }
                case VAR_FLAG:
                {
                    int val = maxVarbit(client, effect.getVarbitIds());
                    if (val <= 0)
                    {
                        state.remainingTicks = 0;
                    }
                    else if (state.lastVarValue <= 0)
                    {
                        start(state, effect.getStandardTicks());
                    }
                    else
                    {
                        // Keep at least 1 tick while the game still reports the effect
                        state.remainingTicks = Math.max(1, state.remainingTicks - 1);
                    }
                    state.lastVarValue = val;
                    break;
                }
                case POISON_VARP:
                {
                    int ticks = effect.poisonImmunityTicks(poisonVarp);
                    if (ticks <= 0)
                    {
                        state.remainingTicks = 0;
                    }
                    else
                    {
                        if (state.remainingTicks <= 0 || ticks > state.remainingTicks + 30)
                        {
                            state.totalTicks = Math.max(effect.getStandardTicks(), ticks);
                            state.usedThisSession = true;
                        }
                        state.remainingTicks = ticks;
                    }
                    break;
                }
                default:
                    tickDown(state);
                    break;
            }
        }
    }

    public void onChatMessage(String rawMessage)
    {
        if (rawMessage == null || rawMessage.isEmpty())
        {
            return;
        }
        String message = CombatSpellTracker.stripTags(rawMessage);
        if (message.equals(MSG_SILK_DRESSING))
        {
            start(states.get(PotionEffect.SILK_DRESSING), PotionEffect.SILK_DRESSING.getStandardTicks());
        }
        else if (message.equals(MSG_BLESSED_CRYSTAL_SCARAB))
        {
            start(states.get(PotionEffect.BLESSED_CRYSTAL_SCARAB), PotionEffect.BLESSED_CRYSTAL_SCARAB.getStandardTicks());
        }
    }

    public void triggerDebugCooldowns()
    {
        for (State state : states.values())
        {
            start(state, 25);
            state.debug = true;
        }
    }

    /**
     * Clears only the debug timers, leaving real effects untouched.
     */
    public void clearDebugCooldowns()
    {
        for (State state : states.values())
        {
            if (state.debug)
            {
                state.debug = false;
                state.remainingTicks = 0;
                state.lastVarValue = 0;
            }
        }
    }

    public Cooldown getCooldown(PotionEffect effect)
    {
        if (effect == null)
        {
            return Cooldown.READY;
        }
        State state = states.get(effect);
        if (state == null || state.remainingTicks <= 0)
        {
            return Cooldown.READY;
        }
        return Cooldown.active(state.remainingTicks, Math.max(state.totalTicks, state.remainingTicks));
    }

    public boolean wasUsedThisSession(PotionEffect effect)
    {
        State state = states.get(effect);
        return state != null && state.usedThisSession;
    }

    private static void start(State state, int ticks)
    {
        state.remainingTicks = ticks;
        state.totalTicks = ticks;
        state.usedThisSession = true;
    }

    private static void tickDown(State state)
    {
        if (state.remainingTicks > 0)
        {
            state.remainingTicks--;
        }
    }

    private static int maxVarbit(Client client, int[] varbitIds)
    {
        int max = 0;
        for (int id : varbitIds)
        {
            max = Math.max(max, client.getVarbitValue(id));
        }
        return max;
    }
}
