package com.immersivecombatoverlays.tracker;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.CombatSpell;
import com.immersivecombatoverlays.model.Cooldown;
import java.util.EnumMap;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;

/**
 * Tracks all combat spells that expire or have recast cooldowns.
 * Always tracks next recast time (e.g., Death Charge stays on recast cooldown
 * even after refunding special attack).
 */
public class CombatSpellTracker
{
    private static final int SPELL_COUNT = CombatSpell.values().length;

    private final int[] remainingTicks = new int[SPELL_COUNT];
    private final int[] totalTicks = new int[SPELL_COUNT];
    private final int[] lastCastSessionId = new int[SPELL_COUNT];
    private final long[] reminderStartNanos = new long[SPELL_COUNT];
    private int lastObservedCombatSession = -1;
    private final EnumMap<CombatSpell, Cooldown> cooldownCache = new EnumMap<>(CombatSpell.class);
    private final int[] liveSpriteIds = new int[SPELL_COUNT];
    private final int[] liveWidgetIds = new int[CombatSpell.values().length];
    private final boolean[] castThisSession = new boolean[SPELL_COUNT];

    public CombatSpellTracker()
    {
        reset();
    }

    public void reset()
    {
        for (int i = 0; i < SPELL_COUNT; i++)
        {
            remainingTicks[i] = 0;
            totalTicks[i] = 0;
            lastCastSessionId[i] = -1;
            reminderStartNanos[i] = 0L;
        }
        cooldownCache.clear();
        for (CombatSpell spell : CombatSpell.values())
        {
            cooldownCache.put(spell, Cooldown.ready());
            liveSpriteIds[spell.ordinal()] = spell.getFallbackSpriteId();
        }
    }

    public void onGameTick(Client client)
    {
        CombatSpell[] spells = CombatSpell.values();
        for (int i = 0; i < spells.length; i++)
        {
            if (remainingTicks[i] > 0)
            {
                remainingTicks[i]--;
                if (remainingTicks[i] == 0)
                {
                    cooldownCache.put(spells[i], Cooldown.ready());
                }
                else
                {
                    cooldownCache.put(spells[i], Cooldown.active(remainingTicks[i], totalTicks[i]));
                }
            }
        }
        syncVarbits(client);
    }

    public void onVarbitChanged(Client client, int varbitId)
    {
        for (CombatSpell spell : CombatSpell.values())
        {
            if (spell.getCooldownVarbitId() == varbitId)
            {
                syncSpellVarbit(client, spell);
            }
        }
    }

    private void syncVarbits(Client client)
    {
        for (CombatSpell spell : CombatSpell.values())
        {
            if (spell.getCooldownVarbitId() > 0)
            {
                syncSpellVarbit(client, spell);
            }
        }
    }

    private void syncSpellVarbit(Client client, CombatSpell spell)
    {
        int varbitId = spell.getCooldownVarbitId();
        if (varbitId <= 0)
        {
            return;
        }

        int value = client.getVarbitValue(varbitId);
        int idx = spell.ordinal();

        if (spell == CombatSpell.VENGEANCE || spell == CombatSpell.VENGEANCE_OTHER)
        {
            // Varbit 2451: 1 while on 30s recast cooldown, 0 when ready
            if (value == 1 && remainingTicks[idx] <= 0)
            {
                startCooldown(spell, spell.getDefaultRecastTicks(), -1);
            }
            else if (value == 0 && remainingTicks[idx] > 0)
            {
                clearCooldown(spell);
            }
        }
        else if (spell == CombatSpell.DEATH_CHARGE)
        {
            // Varbit 12411: >0 while Death Charge buff/recast lockout is active
            if (value > 0 && remainingTicks[idx] <= 0)
            {
                startCooldown(spell, spell.getDefaultRecastTicks(), -1);
            }
        }
        else if (spell == CombatSpell.RESURRECT_THRALL
            || spell == CombatSpell.MARK_OF_DARKNESS
            || spell == CombatSpell.WARD_OF_ARCEUUS
            || spell == CombatSpell.SHADOW_VEIL)
        {
            if (value == 0 && remainingTicks[idx] > 0)
            {
                clearCooldown(spell);
            }
        }
    }

    public void onChatMessage(Client client, String message, int combatSessionId, ImmersiveCombatOverlaysConfig config)
    {
        if (message == null || message.isEmpty())
        {
            return;
        }
        String lower = message.toLowerCase();

        // Thralls: active duration = 0.6s per Magic level * CA tier (100% / 150% / 200%)
        if (lower.contains("you resurrect a") && lower.contains("thrall"))
        {
            int magicLevel = Math.max(1, client.getBoostedSkillLevel(Skill.MAGIC));
            int baseTicks = Math.min(100, magicLevel);
            float mult = config != null ? config.thrallDurationTier().getMultiplier() : 1.0f;
            int ticks = Math.round(baseTicks * mult);
            startCooldown(CombatSpell.RESURRECT_THRALL, ticks, combatSessionId);
            return;
        }
        if (lower.contains("your thrall has returned to the grave"))
        {
            clearCooldown(CombatSpell.RESURRECT_THRALL);
            return;
        }

        // Death Charge & Greater Death Charge: track recast cooldown (varbit 12411 handles 2-charge state)
        if (lower.contains("you have cast death charge") || lower.contains("greater death charge"))
        {
            int varbit = client.getVarbitValue(CombatSpell.DEATH_CHARGE.getCooldownVarbitId());
            if (varbit != 2)
            {
                startCooldown(CombatSpell.DEATH_CHARGE, 100, combatSessionId);
            }
            return;
        }

        // Mark of Darkness (5x duration when wielding Purging Staff)
        if (lower.contains("you cast mark of darkness") || lower.contains("have placed a mark of darkness"))
        {
            int magicLevel = Math.max(1, client.getBoostedSkillLevel(Skill.MAGIC));
            int baseTicks = Math.min(300, Math.max(100, magicLevel * 3));
            int ticks = isWieldingPurgingStaff(client) ? Math.min(500, baseTicks * 5) : baseTicks;
            startCooldown(CombatSpell.MARK_OF_DARKNESS, ticks, combatSessionId);
            return;
        }
        if (lower.contains("your mark of darkness has faded"))
        {
            clearCooldown(CombatSpell.MARK_OF_DARKNESS);
            return;
        }

        // Ward of Arceuus
        if (lower.contains("you cast ward of arceuus") || lower.contains("your ward of arceuus"))
        {
            if (lower.contains("faded") || lower.contains("expired"))
            {
                clearCooldown(CombatSpell.WARD_OF_ARCEUUS);
            }
            else
            {
                int magicLevel = Math.max(1, client.getBoostedSkillLevel(Skill.MAGIC));
                startCooldown(CombatSpell.WARD_OF_ARCEUUS, Math.max(50, magicLevel), combatSessionId);
            }
            return;
        }

        // Shadow Veil
        if (lower.contains("your shadow veil") || lower.contains("you cast shadow veil"))
        {
            if (lower.contains("faded") || lower.contains("expired"))
            {
                clearCooldown(CombatSpell.SHADOW_VEIL);
            }
            else
            {
                int magicLevel = Math.max(1, client.getBoostedSkillLevel(Skill.MAGIC));
                startCooldown(CombatSpell.SHADOW_VEIL, Math.max(50, magicLevel), combatSessionId);
            }
            return;
        }

        // Corruption
        if (lower.contains("lesser corruption") || lower.contains("greater corruption"))
        {
            startCooldown(CombatSpell.CORRUPTION, 100, combatSessionId);
            return;
        }

        // Vengeance / Vengeance Other (recast cooldown tracked from cast / varbit 2451, not proc)
        if (lower.contains("you cast vengeance"))
        {
            startCooldown(CombatSpell.VENGEANCE, 50, combatSessionId);
            return;
        }

        // Charge
        if (lower.contains("you feel charged with magical power"))
        {
            startCooldown(CombatSpell.CHARGE, 100, combatSessionId);
        }
        else if (lower.contains("your magical charge fades away"))
        {
            clearCooldown(CombatSpell.CHARGE);
        }
    }

    public void startCooldown(CombatSpell spell, int ticks, int combatSessionId)
    {
        int idx = spell.ordinal();
        int clamped = Math.max(1, ticks);
        remainingTicks[idx] = clamped;
        totalTicks[idx] = clamped;
        if (combatSessionId >= 0)
        {
            lastCastSessionId[idx] = combatSessionId;
        }
        reminderStartNanos[idx] = 0L;
        castThisSession[idx] = true;
        cooldownCache.put(spell, Cooldown.active(clamped, clamped));
    }

    public void triggerDebugCooldowns(int combatSessionId)
    {
        CombatSpell[] spells = CombatSpell.values();
        for (int i = 0; i < spells.length; i++)
        {
            int ticks = 15 + (i % 4) * 5; // 9s - 18s staggered test timers
            startCooldown(spells[i], ticks, combatSessionId);
        }
    }

    public void onPlayerDeath()
    {
        clearCooldown(CombatSpell.RESURRECT_THRALL);
        clearCooldown(CombatSpell.MARK_OF_DARKNESS);
        clearCooldown(CombatSpell.WARD_OF_ARCEUUS);
        clearCooldown(CombatSpell.SHADOW_VEIL);
        clearCooldown(CombatSpell.CHARGE);
    }

    public void clearCooldown(CombatSpell spell)
    {
        int idx = spell.ordinal();
        remainingTicks[idx] = 0;
        totalTicks[idx] = 0;
        if (reminderStartNanos[idx] == 0L)
        {
            reminderStartNanos[idx] = System.nanoTime();
        }
        cooldownCache.put(spell, Cooldown.ready());
    }

    public Cooldown getCooldown(CombatSpell spell)
    {
        return cooldownCache.getOrDefault(spell, Cooldown.ready());
    }

    /**
     * Returns true if this spell is enabled, off cooldown (ready to recast),
     * and passes the combat-only check.
     */
    public void registerWidgetSpriteId(CombatSpell spell, int spriteId)
    {
        if (spell != null && spriteId > 0)
        {
            liveSpriteIds[spell.ordinal()] = spriteId;
        }
    }

    public void registerWidget(CombatSpell spell, int widgetId, int spriteId)
    {
        if (spell != null)
        {
            if (widgetId > 0)
            {
                liveWidgetIds[spell.ordinal()] = widgetId;
            }
            if (spriteId > 0)
            {
                liveSpriteIds[spell.ordinal()] = spriteId;
            }
        }
    }

    public int getResolvedWidgetId(CombatSpell spell)
    {
        return spell != null ? liveWidgetIds[spell.ordinal()] : -1;
    }

    public int getResolvedSpriteId(CombatSpell spell)
    {
        int id = liveSpriteIds[spell.ordinal()];
        return id > 0 ? id : spell.getFallbackSpriteId();
    }

    public boolean isSpellReadyReminder(
        CombatSpell spell,
        CombatStateTracker combatState,
        ImmersiveCombatOverlaysConfig config
    )
    {
        if (ImmersiveCombatOverlaysConfig.isDebugSpellActive(config, spell))
        {
            return true;
        }
        if (!config.spellsEnabled() || !ImmersiveCombatOverlaysConfig.isSpellEnabled(config, spell))
        {
            return false;
        }
        if (config.spellOnlyInCombat() && !combatState.isInCombat())
        {
            return false;
        }
        int idx = spell.ordinal();
        if (config.spellOnlyRemindAfterCast() && !castThisSession[idx])
        {
            if (!(spell == CombatSpell.RESURRECT_THRALL && config.thrallRemindOnCombatStart()))
            {
                return false;
            }
        }
        return remainingTicks[idx] <= 0;
    }

    /**
     * Populates pre-allocated primitive arrays with all currently ready/expired spells.
     * Zero allocations per frame.
     *
     * @return number of active ready spells written
     */
    private static boolean isWieldingPurgingStaff(Client client)
    {
        ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equip == null)
        {
            return false;
        }
        Item weapon = equip.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
        if (weapon == null)
        {
            return false;
        }
        var comp = client.getItemDefinition(weapon.getId());
        return comp != null && comp.getName() != null && comp.getName().toLowerCase().contains("purging staff");
    }

    public boolean isWithinShowerDuration(CombatSpell spell, int combatSessionId, int maxSeconds, long nowNanos)
    {
        if (maxSeconds <= 0)
        {
            return true;
        }
        int idx = spell.ordinal();
        if (combatSessionId != lastObservedCombatSession)
        {
            lastObservedCombatSession = combatSessionId;
            for (int i = 0; i < SPELL_COUNT; i++)
            {
                if (remainingTicks[i] <= 0)
                {
                    reminderStartNanos[i] = nowNanos;
                }
            }
        }
        if (reminderStartNanos[idx] == 0L)
        {
            reminderStartNanos[idx] = nowNanos;
        }
        return (nowNanos - reminderStartNanos[idx]) <= (maxSeconds * 1_000_000_000L);
    }

    public int collectReadySpells(
        CombatStateTracker combatState,
        ImmersiveCombatOverlaysConfig config,
        int currentSpellbookVarbit,
        boolean filterBySpellbook,
        int[] outSpellOrdinals,
        int[] outColorsArgb
    )
    {
        int count = 0;
        int leadTicks = 0;
        boolean liveAllowed = config.spellsEnabled() && (!config.spellOnlyInCombat() || combatState.isInCombat());
        CombatSpell[] spells = CombatSpell.values();
        for (int i = 0; i < spells.length; i++)
        {
            CombatSpell spell = spells[i];
            if (ImmersiveCombatOverlaysConfig.isDebugSpellActive(config, spell))
            {
                outSpellOrdinals[count] = i;
                outColorsArgb[count] = ImmersiveCombatOverlaysConfig.getSpellColor(config, spell).getRGB();
                count++;
                continue;
            }
            if (!liveAllowed || !ImmersiveCombatOverlaysConfig.isSpellEnabled(config, spell))
            {
                continue;
            }
            if (filterBySpellbook && !castThisSession[i] && spell.getSpellbook().getVarbitValue() != currentSpellbookVarbit)
            {
                continue;
            }
            if (config.spellOnlyRemindAfterCast() && !castThisSession[i])
            {
                if (!(spell == CombatSpell.RESURRECT_THRALL && config.thrallRemindOnCombatStart()))
                {
                    continue;
                }
            }
            if (remainingTicks[i] <= leadTicks)
            {
                outSpellOrdinals[count] = i;
                outColorsArgb[count] = ImmersiveCombatOverlaysConfig.getSpellColor(config, spell).getRGB();
                count++;
            }
        }
        return count;
    }
}
