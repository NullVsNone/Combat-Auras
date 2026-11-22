package com.immersivecombatoverlays;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.CombatSpell;
import com.immersivecombatoverlays.model.ReminderFamily;
import com.immersivecombatoverlays.model.StatusEffect;
import com.immersivecombatoverlays.overlay.ImmersiveStatusOverlay;
import com.immersivecombatoverlays.settings.Settings;
import com.immersivecombatoverlays.settings.SettingsSnapshot;
import com.immersivecombatoverlays.tracker.CombatSpellTracker;
import com.immersivecombatoverlays.tracker.CombatStateTracker;
import com.immersivecombatoverlays.tracker.FreezeTracker;
import com.immersivecombatoverlays.tracker.ParasiteTracker;
import com.immersivecombatoverlays.tracker.ReminderPipeline;
import com.immersivecombatoverlays.tracker.UnpottedTracker;
import com.immersivecombatoverlays.tracker.WarningNotifier;
import com.immersivecombatoverlays.tracker.WarningNotifier.Warning;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;

/**
 * Sends each warning's own RuneLite notification (all OFF by default) on the tick it starts, using the per-entry
 * settings from the snapshot. Warnings, item reminder families and spells all go through the same rising-edge,
 * rate-limited {@link WarningNotifier}.
 */
@Singleton
public class NotificationDispatcher
{
    private static final StatusEffect[] STATUSES = StatusEffect.values();
    private static final CombatSpell[] SPELLS = CombatSpell.values();

    private final Client client;
    private final ImmersiveCombatOverlaysConfig config;
    private final Settings settings;
    private final Notifier notifier;
    private final WarningNotifier warnings;
    private final ImmersiveStatusOverlay statusOverlay;
    private final ReminderPipeline reminders;
    private final FreezeTracker freezeTracker;
    private final UnpottedTracker unpottedTracker;
    private final ParasiteTracker parasiteTracker;
    private final CombatSpellTracker spellTracker;
    private final CombatStateTracker combatState;
    private final boolean[] spellDueLastTick = new boolean[SPELLS.length];

    @Inject
    public NotificationDispatcher(Client client, ImmersiveCombatOverlaysConfig config, Settings settings, Notifier notifier,
        WarningNotifier warnings, ImmersiveStatusOverlay statusOverlay, ReminderPipeline reminders, FreezeTracker freezeTracker,
        UnpottedTracker unpottedTracker, ParasiteTracker parasiteTracker, CombatSpellTracker spellTracker,
        CombatStateTracker combatState)
    {
        this.client = client;
        this.config = config;
        this.settings = settings;
        this.notifier = notifier;
        this.warnings = warnings;
        this.statusOverlay = statusOverlay;
        this.reminders = reminders;
        this.freezeTracker = freezeTracker;
        this.unpottedTracker = unpottedTracker;
        this.parasiteTracker = parasiteTracker;
        this.spellTracker = spellTracker;
        this.combatState = combatState;
    }

    public void reset()
    {
        warnings.reset();
        java.util.Arrays.fill(spellDueLastTick, false);
    }

    /** Game tick, after every tracker and the reminder pipeline have updated. */
    public void update(long nowNanos)
    {
        SettingsSnapshot s = settings.get();
        for (StatusEffect effect : STATUSES)
        {
            Warning warning = warningFor(effect);
            if (warning != null)
            {
                warnings.update(warning, isLive(s, effect, nowNanos), s.statusNotification(effect.ordinal()), notifier, nowNanos);
            }
        }
        for (ReminderFamily family : ReminderFamily.values())
        {
            warnings.update(warningFor(family), reminders.isFamilyLive(family), s.familyNotification(family.ordinal()), notifier, nowNanos);
        }

        int book = client.getVarbitValue(VarbitID.SPELLBOOK);
        for (CombatSpell spell : SPELLS)
        {
            int i = spell.ordinal();
            boolean due = spell.getSpellbook().getVarbitValue() == book
                && spellTracker.wasCastSinceLogin(spell) && spellTracker.isSpellReadyReminder(spell, combatState, s);
            if (due && !spellDueLastTick[i])
            {
                warnings.fireSpell(spell, s.spellNotification(i), notifier, nowNanos);
            }
            spellDueLastTick[i] = due;
        }
    }

    private boolean isLive(SettingsSnapshot s, StatusEffect effect, long nowNanos)
    {
        int i = effect.ordinal();
        switch (effect)
        {
            case FREEZE:
                return s.statusEnabled(i) && freezeTracker.getSeverity(config, nowNanos) > 0f;
            case UNPOTTED:
                return s.statusEnabled(i) && unpottedTracker.getSeverity(config, nowNanos) > 0f;
            case PARASITE:
                return s.statusEnabled(i) && parasiteTracker.isActive();
            default:
                return statusOverlay.isLive(effect);
        }
    }

    /** Which notification a warning sends, or null (damage flashes and ticks never notify; idle notifies itself). */
    static Warning warningFor(StatusEffect effect)
    {
        switch (effect)
        {
            case LOW_HEALTH: return Warning.LOW_HEALTH;
            case LOW_PRAYER: return Warning.LOW_PRAYER;
            case POISON: return Warning.POISON;
            case VENOM: return Warning.VENOM;
            case DISEASE: return Warning.DISEASE;
            case FREEZE: return Warning.FREEZE;
            case SPEC_READY: return Warning.SPEC_READY;
            case LOW_RUN_ENERGY: return Warning.LOW_RUN;
            case UNPOTTED: return Warning.UNPOTTED;
            case PARASITE: return Warning.PARASITE;
            default: return null;
        }
    }

    static Warning warningFor(ReminderFamily family)
    {
        switch (family)
        {
            case HEART: return Warning.HEART_READY;
            case BOOST: return Warning.BOOST_READY;
            default: return Warning.BUFF_EXPIRED;
        }
    }
}
