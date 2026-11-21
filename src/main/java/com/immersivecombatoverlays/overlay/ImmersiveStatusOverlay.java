package com.immersivecombatoverlays.overlay;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.CombatSpell;
import com.immersivecombatoverlays.model.ReminderFamily;
import com.immersivecombatoverlays.model.StatusEffect;
import com.immersivecombatoverlays.render.ProceduralRenderer;
import com.immersivecombatoverlays.settings.Settings;
import com.immersivecombatoverlays.settings.SettingsSnapshot;
import com.immersivecombatoverlays.tracker.CombatSpellTracker;
import com.immersivecombatoverlays.tracker.CombatStateTracker;
import com.immersivecombatoverlays.tracker.FreezeTracker;
import com.immersivecombatoverlays.tracker.HeartCooldownTracker;
import com.immersivecombatoverlays.tracker.IdleTracker;
import com.immersivecombatoverlays.tracker.ImpactTrackers;
import com.immersivecombatoverlays.tracker.ParasiteTracker;
import com.immersivecombatoverlays.tracker.ReminderPipeline;
import com.immersivecombatoverlays.tracker.ReminderPolicy;
import com.immersivecombatoverlays.tracker.UnpottedTracker;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Full-screen procedural warnings, spell reminders and item reminders.
 * <p>
 * Client state is sampled on the client thread at game tick ({@link #updateClientState()}); {@link #render} only
 * combines those values with time-based trackers into reusable primitive arrays (no per-frame allocation) and
 * hands them to the {@link ProceduralRenderer}. Every warning is handled by the same indexed loop over the
 * settings snapshot, so a new warning needs no code here beyond its live severity source.
 */
@Singleton
public class ImmersiveStatusOverlay extends Overlay
{
    private static final StatusEffect[] STATUSES = StatusEffect.values();
    private static final CombatSpell[] SPELLS = CombatSpell.values();
    private static final int MAX_ITEMS = ReminderPipeline.MAX_ITEMS + 2;

    private final Client client;
    private final ImmersiveCombatOverlaysConfig config;
    private final Settings settings;
    private final SpriteManager spriteManager;
    private final ItemManager itemManager;
    private final ProceduralRenderer renderer;
    private final CombatStateTracker combatState;
    private final CombatSpellTracker spellTracker;
    private final ImpactTrackers impacts;
    private final FreezeTracker freezeTracker;
    private final IdleTracker idleTracker;
    private final UnpottedTracker unpottedTracker;
    private final ParasiteTracker parasiteTracker;
    private final ReminderPipeline reminders;

    // Client-thread-sampled live severities (index: StatusEffect ordinal; only tick-sampled ones are written)
    private final float[] liveSeverity = new float[STATUSES.length];
    private volatile int cachedSpellbook = 0;

    // Render-thread reusable primitive buffers (zero per-frame heap allocations)
    private final float[] statusSeverities = new float[STATUSES.length];
    private final float[] rawStatusSeverities = new float[STATUSES.length];
    private final int[] statusColors = new int[STATUSES.length];
    private final long[] statusActiveSince = new long[STATUSES.length];
    private final int[] readySpellOrdinals = new int[SPELLS.length];
    private final int[] readySpellColors = new int[SPELLS.length];
    private final int[] showerSpriteIds = new int[SPELLS.length + STATUSES.length];
    private final int[] showerSpriteColors = new int[SPELLS.length + STATUSES.length];
    private final int[] glowExtraColors = new int[SPELLS.length + MAX_ITEMS];
    private final int[] readyItemIds = new int[MAX_ITEMS + STATUSES.length];
    private final int[] readyItemColors = new int[MAX_ITEMS + STATUSES.length];
    private final int[] readyItemFamilies = new int[MAX_ITEMS + STATUSES.length];

    @Inject
    public ImmersiveStatusOverlay(
        Client client,
        ImmersiveCombatOverlaysConfig config,
        Settings settings,
        SpriteManager spriteManager,
        ItemManager itemManager,
        ProceduralRenderer renderer,
        CombatStateTracker combatState,
        CombatSpellTracker spellTracker,
        ImpactTrackers impacts,
        FreezeTracker freezeTracker,
        IdleTracker idleTracker,
        UnpottedTracker unpottedTracker,
        ParasiteTracker parasiteTracker,
        ReminderPipeline reminders
    )
    {
        this.client = client;
        this.config = config;
        this.settings = settings;
        this.spriteManager = spriteManager;
        this.itemManager = itemManager;
        this.renderer = renderer;
        this.combatState = combatState;
        this.spellTracker = spellTracker;
        this.impacts = impacts;
        this.freezeTracker = freezeTracker;
        this.idleTracker = idleTracker;
        this.unpottedTracker = unpottedTracker;
        this.parasiteTracker = parasiteTracker;
        this.reminders = reminders;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.UNDER_WIDGETS);
        setPriority(PRIORITY_LOW);
    }

    /**
     * Client thread (game tick): samples skills, varps and varbits for the threshold warnings.
     */
    public void updateClientState()
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return;
        }
        SettingsSnapshot s = settings.get();
        cachedSpellbook = client.getVarbitValue(VarbitID.SPELLBOOK);

        liveSeverity[StatusEffect.LOW_HEALTH.ordinal()] = enabled(s, StatusEffect.LOW_HEALTH)
            ? thresholdSeverity(client.getBoostedSkillLevel(Skill.HITPOINTS), client.getRealSkillLevel(Skill.HITPOINTS),
                config.lowHealthThreshold(), config.lowHealthEscalation(), true)
            : 0f;

        boolean prayerGate = !config.prayerActiveOnly() || client.getVarbitValue(VarbitID.PRAYER_ALLACTIVE) != 0;
        liveSeverity[StatusEffect.LOW_PRAYER.ordinal()] = enabled(s, StatusEffect.LOW_PRAYER) && prayerGate
            ? thresholdSeverity(client.getBoostedSkillLevel(Skill.PRAYER), client.getRealSkillLevel(Skill.PRAYER),
                config.lowPrayerThreshold(), config.lowPrayerEscalation(), false)
            : 0f;

        int poisonVar = client.getVarpValue(VarPlayerID.POISON);
        liveSeverity[StatusEffect.VENOM.ordinal()] = poisonVar >= 1_000_000 && enabled(s, StatusEffect.VENOM) ? 0.75f : 0f;
        liveSeverity[StatusEffect.POISON.ordinal()] = poisonVar > 0 && poisonVar < 1_000_000 && enabled(s, StatusEffect.POISON) ? 0.65f : 0f;
        liveSeverity[StatusEffect.DISEASE.ordinal()] = enabled(s, StatusEffect.DISEASE) && client.getVarpValue(VarPlayerID.DISEASE) > 0 ? 0.65f : 0f;

        boolean specGate = enabled(s, StatusEffect.SPEC_READY) && ReminderPolicy.combatGate(config.specOnlyInCombat(), combatState);
        liveSeverity[StatusEffect.SPEC_READY.ordinal()] = specGate
            && client.getVarpValue(VarPlayerID.SA_ENERGY) / 10 >= config.specThresholdPercent() ? 0.65f : 0f;

        float run = 0f;
        if (enabled(s, StatusEffect.LOW_RUN_ENERGY) && ReminderPolicy.combatGate(config.lowRunOnlyInCombat(), combatState))
        {
            int runPct = client.getEnergy() / 100;
            int thresh = config.lowRunThreshold();
            if (runPct <= thresh)
            {
                run = Math.min(1f, 0.30f + (thresh - runPct) / (float) Math.max(1, thresh) * 0.70f);
            }
        }
        liveSeverity[StatusEffect.LOW_RUN_ENERGY.ordinal()] = run;
    }

    private static boolean enabled(SettingsSnapshot s, StatusEffect effect)
    {
        return s.statusEnabled[effect.ordinal()];
    }

    /**
     * Severity of a "below threshold %" warning, escalating with depth below it.
     */
    static float thresholdSeverity(int current, int max, int thresholdPercent, int escalation, boolean requireAlive)
    {
        float pct = current * 100f / Math.max(1, max);
        if (pct > thresholdPercent || (requireAlive && current <= 0))
        {
            return 0f;
        }
        float raw = (thresholdPercent - pct) / Math.max(1f, thresholdPercent);
        return Math.min(1f, 0.25f + raw * (0.75f + 0.35f * (escalation / 100f)));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return null;
        }
        SettingsSnapshot s = settings.get();
        long nowNanos = System.nanoTime();

        populateStatusArrays(s, nowNanos);
        int rawSpellCount = s.anyDebugPreview
            ? collectDebugSpells(s)
            : spellTracker.collectReadySpells(combatState, s, cachedSpellbook, true, readySpellOrdinals, readySpellColors);
        int readySpellCount = filterSpellsByDuration(s, rawSpellCount, nowNanos);
        int readyItemCount = collectReadyItems(s);
        int glowExtraCount = buildGlowExtras(s, readySpellCount, readyItemCount);
        int showerSpriteCount = buildSpriteShower(s, readySpellCount);
        int showerItemCount = buildItemShower(s, readyItemCount);

        renderer.render(graphics, client.getCanvasWidth(), client.getCanvasHeight(), nowNanos, s,
            client, spriteManager, itemManager,
            statusSeverities, statusColors,
            glowExtraCount, glowExtraColors,
            showerSpriteCount, showerSpriteIds, showerSpriteColors,
            showerItemCount, readyItemIds, readyItemColors);
        return null;
    }

    private void populateStatusArrays(SettingsSnapshot s, long nowNanos)
    {
        Arrays.fill(statusSeverities, 0f);
        Arrays.fill(rawStatusSeverities, 0f);

        if (s.anyDebugPreview)
        {
            for (StatusEffect e : STATUSES)
            {
                if (s.statusDebug[e.ordinal()])
                {
                    rawStatusSeverities[e.ordinal()] = s.previewIntensity;
                }
            }
        }
        else
        {
            for (StatusEffect e : STATUSES)
            {
                int i = e.ordinal();
                rawStatusSeverities[i] = s.statusEnabled[i] ? liveSeverity(e, nowNanos) : 0f;
                applyDuration(i, s.statusDurationSeconds[i], nowNanos);
            }
        }

        for (int i = 0; i < STATUSES.length; i++)
        {
            statusColors[i] = s.statusColor[i];
            if (s.statusGlow[i])
            {
                statusSeverities[i] = rawStatusSeverities[i];
            }
        }
    }

    /** Current severity of one warning: tick-sampled ones from {@link #updateClientState}, the rest from their trackers. */
    private float liveSeverity(StatusEffect effect, long nowNanos)
    {
        switch (effect)
        {
            case DAMAGE:
                return impacts.damage.getSeverity(nowNanos, config.damageFeedbackDuration() * 1_000_000L)
                    * (config.damageFeedbackStrength() / 100f);
            case POISON_TICK:
                return impacts.poisonTick.getSeverity(nowNanos, 500_000_000L);
            case VENOM_TICK:
                return impacts.venomTick.getSeverity(nowNanos, 500_000_000L);
            case FREEZE:
                return freezeTracker.getSeverity(config, nowNanos);
            case IDLE:
                return idleTracker.getSeverity(config, nowNanos);
            case PARASITE:
                return parasiteTracker.isActive() ? parasiteTracker.getSeverity() : 0f;
            case UNPOTTED:
                return unpottedTracker.getSeverity(config, nowNanos);
            default:
                return liveSeverity[effect.ordinal()];
        }
    }

    /**
     * Shows a continuous warning for at most {@code seconds} after it starts (0 = while active); it re-arms once the
     * condition clears and comes back.
     */
    private void applyDuration(int i, int seconds, long nowNanos)
    {
        if (rawStatusSeverities[i] <= 0.01f)
        {
            rawStatusSeverities[i] = 0f;
            statusActiveSince[i] = 0L;
            return;
        }
        if (statusActiveSince[i] == 0L)
        {
            statusActiveSince[i] = nowNanos;
        }
        if (seconds > 0 && nowNanos - statusActiveSince[i] > seconds * 1_000_000_000L)
        {
            rawStatusSeverities[i] = 0f;
        }
    }

    private int collectDebugSpells(SettingsSnapshot s)
    {
        int count = 0;
        for (int i = 0; i < SPELLS.length; i++)
        {
            if (s.spellDebug[i])
            {
                readySpellOrdinals[count] = i;
                readySpellColors[count] = s.spellColor[i];
                count++;
            }
        }
        return count;
    }

    private int filterSpellsByDuration(SettingsSnapshot s, int rawCount, long nowNanos)
    {
        int session = combatState.getCombatSessionId();
        int kept = 0;
        for (int i = 0; i < rawCount; i++)
        {
            CombatSpell spell = SPELLS[readySpellOrdinals[i]];
            if (s.spellDebug[spell.ordinal()] || spellTracker.isWithinShowerDuration(spell, session, s, nowNanos))
            {
                readySpellOrdinals[kept] = readySpellOrdinals[i];
                readySpellColors[kept] = readySpellColors[i];
                kept++;
            }
        }
        return kept;
    }

    private int collectReadyItems(SettingsSnapshot s)
    {
        int count = 0;
        if (s.familyDebug[ReminderFamily.HEART.ordinal()])
        {
            count = addItem(count, HeartCooldownTracker.SATURATED_HEART_ID, s.familyColor[ReminderFamily.HEART.ordinal()], ReminderFamily.HEART.ordinal());
        }
        if (s.familyDebug[ReminderFamily.POTION.ordinal()])
        {
            count = addItem(count, ItemID._4DOSEDIVINECOMBAT, s.familyColor[ReminderFamily.POTION.ordinal()], ReminderFamily.POTION.ordinal());
        }
        if (s.anyDebugPreview)
        {
            return count;
        }
        int live = reminders.itemCount();
        for (int i = 0; i < live && count < MAX_ITEMS; i++)
        {
            count = addItem(count, reminders.itemId(i), reminders.itemColor(i), reminders.itemFamily(i));
        }
        return count;
    }

    private int addItem(int idx, int itemId, int argb, int family)
    {
        readyItemIds[idx] = itemId;
        readyItemColors[idx] = argb;
        readyItemFamilies[idx] = family;
        return idx + 1;
    }

    private int buildGlowExtras(SettingsSnapshot s, int spellCount, int itemCount)
    {
        int out = 0;
        for (int i = 0; i < spellCount; i++)
        {
            if (s.spellGlow[readySpellOrdinals[i]])
            {
                glowExtraColors[out++] = readySpellColors[i];
            }
        }
        for (int i = 0; i < itemCount; i++)
        {
            if (s.familyGlow[readyItemFamilies[i]])
            {
                glowExtraColors[out++] = readyItemColors[i];
            }
        }
        return out;
    }

    private int buildSpriteShower(SettingsSnapshot s, int spellCount)
    {
        if (!s.allowIconShowers)
        {
            return 0;
        }
        int out = 0;
        for (int i = 0; i < spellCount; i++)
        {
            CombatSpell spell = SPELLS[readySpellOrdinals[i]];
            if (s.spellShower[spell.ordinal()])
            {
                showerSpriteIds[out] = spellTracker.getResolvedSpriteId(spell);
                showerSpriteColors[out] = readySpellColors[i];
                out++;
            }
        }
        for (StatusEffect e : STATUSES)
        {
            int i = e.ordinal();
            if (e.getShowerSpriteId() > 0 && s.statusShower[i] && rawStatusSeverities[i] > 0.01f)
            {
                showerSpriteIds[out] = e.getShowerSpriteId();
                showerSpriteColors[out] = statusColors[i];
                out++;
            }
        }
        return out;
    }

    private int buildItemShower(SettingsSnapshot s, int itemCount)
    {
        if (!s.allowIconShowers)
        {
            return 0;
        }
        int out = 0;
        for (int i = 0; i < itemCount; i++)
        {
            if (s.familyShower[readyItemFamilies[i]])
            {
                readyItemIds[out] = readyItemIds[i];
                readyItemColors[out] = readyItemColors[i];
                readyItemFamilies[out] = readyItemFamilies[i];
                out++;
            }
        }
        for (StatusEffect e : STATUSES)
        {
            int i = e.ordinal();
            if (e.getShowerItemId() > 0 && s.statusShower[i] && rawStatusSeverities[i] > 0.01f && out < readyItemIds.length)
            {
                readyItemIds[out] = e.getShowerItemId();
                readyItemColors[out] = statusColors[i];
                out++;
            }
        }
        return out;
    }

    /** Whether a tick-sampled warning is live (for notifications). */
    public boolean isLive(StatusEffect effect)
    {
        return liveSeverity[effect.ordinal()] > 0.01f;
    }

    public boolean isPrayerLow()
    {
        return settings.get().statusDebug[StatusEffect.LOW_PRAYER.ordinal()] || isLive(StatusEffect.LOW_PRAYER);
    }
}
