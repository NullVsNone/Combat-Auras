package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.model.Cooldown;
import com.immersivecombatoverlays.model.PotionEffect;
import java.util.EnumMap;
import net.runelite.api.Client;

/**
 * Tracks active potion and raid consumable timers independently of display settings.
 */
public class PotionTimers
{
    private static final class State
    {
        int remainingTicks;
        int totalTicks;
        int lastVarValue;
        boolean usedThisSession;
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
        }
    }

    public void onGameTick(Client client)
    {
        for (PotionEffect effect : PotionEffect.values())
        {
            State state = states.get(effect);
            if (effect.getVarId() > 0)
            {
                int val;
                try
                {
                    val = effect.isVarPlayer()
                        ? client.getVarpValue(effect.getVarId())
                        : client.getVarbitValue(effect.getVarId());
                }
                catch (IndexOutOfBoundsException ignored)
                {
                    continue;
                }

                // Poison varplayer: negative values indicate antipoison/antivenom immunity
                if (effect == PotionEffect.ANTIPOISON || effect == PotionEffect.ANTIVENOM)
                {
                    if (val < 0)
                    {
                        int immunityTicks = Math.abs(val) * 30;
                        state.remainingTicks = immunityTicks;
                        state.usedThisSession = true;
                        if (state.totalTicks < immunityTicks)
                        {
                            state.totalTicks = Math.max(effect.getStandardTicks(), immunityTicks);
                        }
                    }
                    else
                    {
                        state.remainingTicks = 0;
                    }
                    continue;
                }

                if (val <= 0)
                {
                    if (state.lastVarValue > 0)
                    {
                        state.remainingTicks = 0;
                        state.lastVarValue = 0;
                    }
                    else if (state.remainingTicks > 0)
                    {
                        state.remainingTicks--;
                    }
                }
                else if (val != state.lastVarValue)
                {
                    state.lastVarValue = val;
                    state.remainingTicks = val * effect.getBucketTicks();
                    state.usedThisSession = true;
                    state.totalTicks = Math.max(effect.getStandardTicks(), state.remainingTicks);
                }
                else if (state.remainingTicks > 0)
                {
                    state.remainingTicks--;
                }
            }
            else if (state.remainingTicks > 0)
            {
                state.remainingTicks--;
            }
        }
    }

    public void onChatMessage(String message)
    {
        if (message == null || message.isEmpty())
        {
            return;
        }
        String lower = message.toLowerCase();
        if (lower.contains("surge potion") || lower.contains("feel a surge"))
        {
            triggerManual(PotionEffect.SURGE, 500);
        }
        else if (lower.contains("liquid adrenaline"))
        {
            triggerManual(PotionEffect.LIQUID_ADRENALINE, 250);
        }
        else if (lower.contains("silk dressing"))
        {
            triggerManual(PotionEffect.SILK_DRESSING, 100);
        }
        else if (lower.contains("blessed crystal scarab"))
        {
            triggerManual(PotionEffect.BLESSED_CRYSTAL_SCARAB, 100);
        }
    }

    public void triggerDebugCooldowns()
    {
        for (PotionEffect effect : PotionEffect.values())
        {
            triggerManual(effect, 25);
        }
    }

    private void triggerManual(PotionEffect effect, int ticks)
    {
        State state = states.get(effect);
        if (state != null)
        {
            state.remainingTicks = ticks;
            state.usedThisSession = true;
            state.totalTicks = ticks;
        }
    }

    public Cooldown getCooldown(PotionEffect effect)
    {
        if (effect == null)
        {
            return Cooldown.ready();
        }
        State state = states.get(effect);
        if (state == null || state.remainingTicks <= 0)
        {
            return Cooldown.ready();
        }
        return Cooldown.active(state.remainingTicks, Math.max(state.totalTicks, state.remainingTicks));
    }

    public boolean wasUsedThisSession(PotionEffect effect)
    {
        State state = states.get(effect);
        return state != null && state.usedThisSession;
    }
}
