package com.combatauras.tracker;

import com.combatauras.config.CombatAurasConfig;
import com.combatauras.model.CombatSpell;
import com.combatauras.model.Cooldown;
import com.combatauras.model.ThrallDurationTier;
import com.combatauras.settings.SettingsSnapshot;
import java.util.Arrays;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Tracks combat spells that expire or have recast cooldowns.
 *
 * Each spell has two independent timers: the buff/summon duration and the recast cooldown.
 * The spell is "ready" (and may be reminded) only when both have finished. Where the game
 * exposes a varbit for either state, the varbit is authoritative; chat messages only supply
 * durations the game does not expose (thrall, Mark of Darkness, Ward, Shadow Veil lengths).
 */
@javax.inject.Singleton
public class CombatSpellTracker
{
    private static final int SPELL_COUNT = CombatSpell.values().length;
    private static final CombatSpell[] SPELLS = CombatSpell.values();
    private boolean godCapeWorn = false;

    // Messages as sent by the game (compared after colour tags are stripped)
    static final String MSG_THRALL_START = "You resurrect a ";
    static final String MSG_THRALL_END = " thrall.";
    static final String MSG_MARK_OF_DARKNESS = "You have placed a Mark of Darkness upon yourself.";
    static final String MSG_WARD_OF_ARCEUUS = "Your defence against Arceuus magic has been strengthened.";
    static final String MSG_SHADOW_VEIL = "Your thieving abilities have been enhanced.";

    private final int[] activeTicks = new int[SPELL_COUNT];
    private final int[] activeTotal = new int[SPELL_COUNT];
    private final int[] cooldownTicks = new int[SPELL_COUNT];
    private final int[] cooldownTotal = new int[SPELL_COUNT];
    private final int[] lastCooldownVarbit = new int[SPELL_COUNT];
    private final boolean[] castSinceLogin = new boolean[SPELL_COUNT];
    private final boolean[] debugTimer = new boolean[SPELL_COUNT];
    private final long[] reminderStartNanos = new long[SPELL_COUNT];
    private final Cooldown[] cooldownCache = new Cooldown[SPELL_COUNT];
    private final int[] liveSpriteIds = new int[SPELL_COUNT];
    private final int[] liveWidgetIds = new int[SPELL_COUNT];
    private int lastObservedCombatSession = -1;

    public CombatSpellTracker()
    {
        reset();
    }

    /**
     * Full reset (login screen, hop, plugin start/stop).
     */
    public void reset()
    {
        Arrays.fill(activeTicks, 0);
        Arrays.fill(activeTotal, 0);
        Arrays.fill(cooldownTicks, 0);
        Arrays.fill(cooldownTotal, 0);
        Arrays.fill(lastCooldownVarbit, 0);
        Arrays.fill(castSinceLogin, false);
        Arrays.fill(debugTimer, false);
        Arrays.fill(reminderStartNanos, 0L);
        Arrays.fill(liveWidgetIds, -1);
        Arrays.fill(cooldownCache, Cooldown.READY);
        for (CombatSpell spell : SPELLS)
        {
            liveSpriteIds[spell.ordinal()] = spell.getFallbackSpriteId();
        }
        lastObservedCombatSession = -1;
        godCapeWorn = false;
    }

    /**
     * Whether a VarbitChanged event concerns a variable this tracker reads (VarbitChanged fires for every game var).
     */
    public static boolean isTrackedVar(int varbitId, int varpId)
    {
        if (varpId == VarPlayerID.MAGEARENA_CHARGE)
        {
            return true;
        }
        if (varbitId <= 0)
        {
            return false;
        }
        for (CombatSpell spell : SPELLS)
        {
            if (spell.getActiveVarbitId() == varbitId || spell.getCooldownVarbitId() == varbitId)
            {
                return true;
            }
        }
        return false;
    }

    public void onGameTick(Client client)
    {
        for (int i = 0; i < SPELL_COUNT; i++)
        {
            if (activeTicks[i] > 0)
            {
                activeTicks[i]--;
            }
            if (cooldownTicks[i] > 0)
            {
                cooldownTicks[i]--;
            }
        }
        syncVars(client);
        for (int i = 0; i < SPELL_COUNT; i++)
        {
            refresh(i);
        }
    }

    /**
     * Re-reads all spell varbits/varps. Cheap (a handful of reads) so it is safe to call on every VarbitChanged.
     */
    public void syncVars(Client client)
    {
        if (client == null)
        {
            return;
        }
        for (CombatSpell spell : SPELLS)
        {
            int idx = spell.ordinal();
            if (debugTimer[idx])
            {
                continue;
            }

            int activeVarbit = spell.getActiveVarbitId();
            if (activeVarbit > 0)
            {
                int value = client.getVarbitValue(activeVarbit);
                if (value == 0)
                {
                    activeTicks[idx] = 0;
                }
                else if (activeTicks[idx] <= 0)
                {
                    // Active but we missed the cast message (e.g. plugin enabled mid-fight).
                    // Hold it "not ready" with a best-effort duration until the varbit clears.
                    int estimate = Math.max(1, spell.getCooldownTicks());
                    activeTicks[idx] = estimate;
                    activeTotal[idx] = estimate;
                    markCast(idx);
                }
            }

            int cooldownVarbit = spell.getCooldownVarbitId();
            if (cooldownVarbit > 0)
            {
                int value = client.getVarbitValue(cooldownVarbit);
                if (value == 0)
                {
                    cooldownTicks[idx] = 0;
                }
                else if (lastCooldownVarbit[idx] == 0)
                {
                    // 0 -> 1 transition: the spell was just cast
                    cooldownTicks[idx] = spell.getCooldownTicks();
                    cooldownTotal[idx] = spell.getCooldownTicks();
                    markCast(idx);
                }
                else if (cooldownTicks[idx] <= 0)
                {
                    // Our countdown finished before the server cleared the varbit; stay "not ready" until it does
                    cooldownTicks[idx] = 1;
                }
                lastCooldownVarbit[idx] = value;
            }
        }

        // Charge: varp holds remaining duration in 2-tick units
        int chargeIdx = CombatSpell.CHARGE.ordinal();
        if (!debugTimer[chargeIdx])
        {
            int charge = client.getVarpValue(VarPlayerID.MAGEARENA_CHARGE);
            if (charge <= 0)
            {
                activeTicks[chargeIdx] = 0;
            }
            else
            {
                int ticks = charge * 2;
                if (activeTicks[chargeIdx] <= 0)
                {
                    activeTotal[chargeIdx] = ticks;
                    markCast(chargeIdx);
                }
                activeTicks[chargeIdx] = ticks;
                activeTotal[chargeIdx] = Math.max(activeTotal[chargeIdx], ticks);
            }
        }

        for (int i = 0; i < SPELL_COUNT; i++)
        {
            refresh(i);
        }
    }

    public void onChatMessage(Client client, String rawMessage, CombatAurasConfig config)
    {
        if (rawMessage == null || rawMessage.isEmpty() || client == null)
        {
            return;
        }
        String message = stripTags(rawMessage);

        if (message.startsWith(MSG_THRALL_START) && message.endsWith(MSG_THRALL_END))
        {
            int magic = Math.max(1, client.getBoostedSkillLevel(Skill.MAGIC));
            ThrallDurationTier tier = config != null ? config.thrallDurationTier() : ThrallDurationTier.AUTO;
            boolean master = client.getVarbitValue(VarbitID.CA_TIER_STATUS_MASTER) == 2;
            startActive(CombatSpell.RESURRECT_THRALL, magic * tier.getMultiplier(master));
        }
        else if (message.equals(MSG_MARK_OF_DARKNESS))
        {
            int magic = Math.max(1, client.getRealSkillLevel(Skill.MAGIC));
            int ticks = magic * 3;
            if (isWieldingPurgingStaff(client))
            {
                ticks *= 5;
            }
            startActive(CombatSpell.MARK_OF_DARKNESS, ticks);
            startCooldownTimer(CombatSpell.MARK_OF_DARKNESS, CombatSpell.MARK_OF_DARKNESS.getCooldownTicks());
        }
        else if (message.equals(MSG_WARD_OF_ARCEUUS))
        {
            startActive(CombatSpell.WARD_OF_ARCEUUS, Math.max(1, client.getRealSkillLevel(Skill.MAGIC)));
        }
        else if (message.equals(MSG_SHADOW_VEIL))
        {
            startActive(CombatSpell.SHADOW_VEIL, Math.max(1, client.getRealSkillLevel(Skill.MAGIC)));
        }
        else
        {
            String lower = message.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("mark of darkness") && lower.contains("faded"))
            {
                activeTicks[CombatSpell.MARK_OF_DARKNESS.ordinal()] = 0;
                refresh(CombatSpell.MARK_OF_DARKNESS.ordinal());
            }
        }
    }

    /**
     * Starts (or restarts) a spell's buff duration. Public for tests and debug tooling.
     */
    public void startActive(CombatSpell spell, int ticks)
    {
        int idx = spell.ordinal();
        int clamped = Math.max(1, ticks);
        activeTicks[idx] = clamped;
        activeTotal[idx] = clamped;
        debugTimer[idx] = false;
        markCast(idx);
        refresh(idx);
    }

    /**
     * Starts (or restarts) a spell's recast cooldown. Public for tests and debug tooling.
     */
    public void startCooldownTimer(CombatSpell spell, int ticks)
    {
        int idx = spell.ordinal();
        int clamped = Math.max(1, ticks);
        cooldownTicks[idx] = clamped;
        cooldownTotal[idx] = clamped;
        markCast(idx);
        refresh(idx);
    }

    /**
     * Starts staggered 9-18s test timers. They are flagged so live varbits do not overwrite them
     * and so clearing them later never wipes real cooldowns.
     */
    public void triggerDebugCooldowns()
    {
        for (int i = 0; i < SPELL_COUNT; i++)
        {
            int ticks = 15 + (i % 4) * 5;
            activeTicks[i] = ticks;
            activeTotal[i] = ticks;
            debugTimer[i] = true;
            markCast(i);
            refresh(i);
        }
    }

    public void clearDebugCooldowns()
    {
        for (int i = 0; i < SPELL_COUNT; i++)
        {
            if (debugTimer[i])
            {
                // Debug timers only ever use the buff slot; real recast cooldowns are left running
                debugTimer[i] = false;
                activeTicks[i] = 0;
                refresh(i);
            }
        }
    }

    /**
     * Buffs and summons end on death; recast cooldowns keep running.
     */
    public void onPlayerDeath()
    {
        for (int i = 0; i < SPELL_COUNT; i++)
        {
            activeTicks[i] = 0;
            refresh(i);
        }
    }

    public void clearCooldown(CombatSpell spell)
    {
        int idx = spell.ordinal();
        activeTicks[idx] = 0;
        cooldownTicks[idx] = 0;
        debugTimer[idx] = false;
        refresh(idx);
    }

    public Cooldown getCooldown(CombatSpell spell)
    {
        return cooldownCache[spell.ordinal()];
    }

    public boolean wasCastSinceLogin(CombatSpell spell)
    {
        return castSinceLogin[spell.ordinal()];
    }

    public void registerWidget(CombatSpell spell, int widgetId, int spriteId)
    {
        if (spell == null)
        {
            return;
        }
        if (widgetId > 0)
        {
            liveWidgetIds[spell.ordinal()] = widgetId;
        }
        if (spriteId > 0)
        {
            liveSpriteIds[spell.ordinal()] = spriteId;
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

    /**
     * Ready to recast, or within its "warn early" lead of becoming ready.
     */
    public boolean isDue(CombatSpell spell, SettingsSnapshot s)
    {
        Cooldown cd = cooldownCache[spell.ordinal()];
        return cd.isReady() || cd.getRemainingTicks() <= s.spellLeadTicks(spell.ordinal());
    }

    /**
     * True if this spell is enabled, due (ready or within its lead), and passes the context / after-cast filters.
     */
    public boolean isSpellReadyReminder(CombatSpell spell, CombatStateTracker combatState, SettingsSnapshot s)
    {
        int idx = spell.ordinal();
        if (s.spellDebug(idx))
        {
            return true;
        }
        return s.spellsEnabled && s.spellEnabled(idx) && contextAllows(spell, combatState, s)
            && passesAfterCastFilter(spell, s) && isDue(spell, s);
    }

    public boolean isWithinShowerDuration(CombatSpell spell, int combatSessionId, SettingsSnapshot s, long nowNanos)
    {
        int maxSeconds = s.spellDurationSeconds(spell.ordinal());
        if (maxSeconds <= 0)
        {
            return true;
        }
        int idx = spell.ordinal();
        if (combatSessionId != lastObservedCombatSession)
        {
            // A new combat session (after a long enough break) re-arms reminders for every due spell
            lastObservedCombatSession = combatSessionId;
            for (int i = 0; i < SPELL_COUNT; i++)
            {
                if (isDue(SPELLS[i], s))
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

    /**
     * Writes every due spell (debug previews first) into pre-allocated arrays. No per-frame allocation.
     *
     * @return number of spells written
     */
    public int collectReadySpells(
        CombatStateTracker combatState,
        SettingsSnapshot s,
        int currentSpellbookVarbit,
        boolean filterBySpellbook,
        int[] outSpellOrdinals,
        int[] outColorsArgb
    )
    {
        int count = 0;
        for (int i = 0; i < SPELL_COUNT; i++)
        {
            CombatSpell spell = SPELLS[i];
            if (s.spellDebug(i))
            {
                outSpellOrdinals[count] = i;
                outColorsArgb[count] = s.spellColor(i);
                count++;
                continue;
            }
            // You can only recast from the matching spellbook, so never remind about another book's spells
            if (filterBySpellbook && spell.getSpellbook().getVarbitValue() != currentSpellbookVarbit)
            {
                continue;
            }
            if (s.spellsEnabled && s.spellEnabled(i) && contextAllows(spell, combatState, s)
                && passesAfterCastFilter(spell, s) && isDue(spell, s))
            {
                outSpellOrdinals[count] = i;
                outColorsArgb[count] = s.spellColor(i);
                count++;
            }
        }
        return count;
    }

    /** Set each tick from the worn equipment; Charge only matters with a god cape. */
    public void setGodCapeWorn(boolean worn)
    {
        godCapeWorn = worn;
    }

    /**
     * Where a reminder for this spell is useful: Shadow Veil while pickpocketing, Charge with a god cape,
     * everything else in combat (or always, with "Only remind in combat" off).
     */
    public boolean contextAllows(CombatSpell spell, CombatStateTracker combatState, SettingsSnapshot s)
    {
        boolean combatOk = !s.spellOnlyInCombat || combatState.isInCombat();
        switch (spell)
        {
            case SHADOW_VEIL:
                return ReminderPolicy.contextAllows(s.shadowVeilContext, combatState, combatOk);
            case CHARGE:
                return combatOk && (godCapeWorn || !s.chargeNeedsGodCape);
            default:
                return combatOk;
        }
    }

    private boolean passesAfterCastFilter(CombatSpell spell, SettingsSnapshot s)
    {
        if (!s.spellOnlyRemindAfterCast || castSinceLogin[spell.ordinal()])
        {
            return true;
        }
        return spell == CombatSpell.RESURRECT_THRALL && s.thrallRemindOnCombatStart;
    }

    private void markCast(int idx)
    {
        castSinceLogin[idx] = true;
        reminderStartNanos[idx] = 0L;
    }

    private void refresh(int idx)
    {
        int active = activeTicks[idx];
        int cd = cooldownTicks[idx];
        boolean wasReady = cooldownCache[idx].isReady();
        if (active <= 0 && cd <= 0)
        {
            cooldownCache[idx] = Cooldown.READY;
            if (!wasReady)
            {
                reminderStartNanos[idx] = 0L;
            }
            return;
        }
        if (active >= cd)
        {
            cooldownCache[idx] = Cooldown.active(active, Math.max(active, activeTotal[idx]));
        }
        else
        {
            cooldownCache[idx] = Cooldown.active(cd, Math.max(cd, cooldownTotal[idx]));
        }
    }

    private static boolean isWieldingPurgingStaff(Client client)
    {
        ItemContainer worn = client.getItemContainer(InventoryID.WORN);
        if (worn == null)
        {
            return false;
        }
        Item weapon = worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
        return weapon != null && weapon.getId() == ItemID.PURGING_STAFF;
    }

    static String stripTags(String s)
    {
        if (s.indexOf('<') < 0)
        {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        boolean inTag = false;
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (c == '<')
            {
                inTag = true;
            }
            else if (c == '>')
            {
                inTag = false;
            }
            else if (!inTag)
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
