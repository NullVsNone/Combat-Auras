package com.immersivecombatoverlays.settings;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.CombatSpell;
import com.immersivecombatoverlays.model.PotionFamily;
import com.immersivecombatoverlays.model.ReminderFamily;
import com.immersivecombatoverlays.model.StatusEffect;
import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.runelite.client.config.Notification;

/**
 * The single place where each spell, warning and reminder family is wired to its config items.
 * <p>
 * Adding a spell / warning / potion family = one enum entry, its {@code @ConfigItem} block, and one row here.
 * Every row is a compile-checked method reference (no string keys, no reflection); a unit test asserts each table
 * covers every enum constant.
 */
public final class Bindings
{
    private Bindings()
    {
    }

    private static final Predicate<ImmersiveCombatOverlaysConfig> NEVER = c -> false;
    private static final ToIntFunction<ImmersiveCombatOverlaysConfig> ZERO = c -> 0;
    private static final Function<ImmersiveCombatOverlaysConfig, Notification> NO_NOTIFICATION = c -> Notification.OFF;

    /** Per-spell settings. */
    public static final class Spell
    {
        final Predicate<ImmersiveCombatOverlaysConfig> enabled;
        final Function<ImmersiveCombatOverlaysConfig, Color> color;
        final Predicate<ImmersiveCombatOverlaysConfig> glow;
        final Predicate<ImmersiveCombatOverlaysConfig> shower;
        final Predicate<ImmersiveCombatOverlaysConfig> spellbook;
        final Function<ImmersiveCombatOverlaysConfig, Notification> notification;
        final ToIntFunction<ImmersiveCombatOverlaysConfig> durationSeconds;
        final ToIntFunction<ImmersiveCombatOverlaysConfig> leadSeconds;
        final Predicate<ImmersiveCombatOverlaysConfig> recastProtection;
        final Predicate<ImmersiveCombatOverlaysConfig> debug;

        Spell(Predicate<ImmersiveCombatOverlaysConfig> enabled, Function<ImmersiveCombatOverlaysConfig, Color> color,
            Predicate<ImmersiveCombatOverlaysConfig> glow, Predicate<ImmersiveCombatOverlaysConfig> shower,
            Predicate<ImmersiveCombatOverlaysConfig> spellbook, Function<ImmersiveCombatOverlaysConfig, Notification> notification,
            ToIntFunction<ImmersiveCombatOverlaysConfig> durationSeconds, ToIntFunction<ImmersiveCombatOverlaysConfig> leadSeconds,
            Predicate<ImmersiveCombatOverlaysConfig> recastProtection, Predicate<ImmersiveCombatOverlaysConfig> debug)
        {
            this.enabled = enabled;
            this.color = color;
            this.glow = glow;
            this.shower = shower;
            this.spellbook = spellbook;
            this.notification = notification;
            this.durationSeconds = durationSeconds;
            this.leadSeconds = leadSeconds;
            this.recastProtection = recastProtection;
            this.debug = debug;
        }
    }

    /** Per-warning settings. */
    public static final class Status
    {
        final Predicate<ImmersiveCombatOverlaysConfig> enabled;
        final Function<ImmersiveCombatOverlaysConfig, Color> color;
        final Predicate<ImmersiveCombatOverlaysConfig> glow;
        final Predicate<ImmersiveCombatOverlaysConfig> shower;
        final ToIntFunction<ImmersiveCombatOverlaysConfig> durationSeconds;
        final Function<ImmersiveCombatOverlaysConfig, Notification> notification;
        final Predicate<ImmersiveCombatOverlaysConfig> debug;

        Status(Predicate<ImmersiveCombatOverlaysConfig> enabled, Function<ImmersiveCombatOverlaysConfig, Color> color,
            Predicate<ImmersiveCombatOverlaysConfig> glow, Predicate<ImmersiveCombatOverlaysConfig> shower,
            ToIntFunction<ImmersiveCombatOverlaysConfig> durationSeconds,
            Function<ImmersiveCombatOverlaysConfig, Notification> notification, Predicate<ImmersiveCombatOverlaysConfig> debug)
        {
            this.enabled = enabled;
            this.color = color;
            this.glow = glow;
            this.shower = shower;
            this.durationSeconds = durationSeconds;
            this.notification = notification;
            this.debug = debug;
        }
    }

    /** Shared settings of an item reminder family (heart, potions, boosts). */
    public static final class Family
    {
        final Predicate<ImmersiveCombatOverlaysConfig> enabled;
        final Function<ImmersiveCombatOverlaysConfig, Color> color;
        final Predicate<ImmersiveCombatOverlaysConfig> glow;
        final Predicate<ImmersiveCombatOverlaysConfig> shower;
        final Function<ImmersiveCombatOverlaysConfig, Notification> notification;
        final ToIntFunction<ImmersiveCombatOverlaysConfig> durationSeconds;
        final Predicate<ImmersiveCombatOverlaysConfig> onlyInCombat;
        final ToIntFunction<ImmersiveCombatOverlaysConfig> leadSeconds;
        final Predicate<ImmersiveCombatOverlaysConfig> debug;

        Family(Predicate<ImmersiveCombatOverlaysConfig> enabled, Function<ImmersiveCombatOverlaysConfig, Color> color,
            Predicate<ImmersiveCombatOverlaysConfig> glow, Predicate<ImmersiveCombatOverlaysConfig> shower,
            Function<ImmersiveCombatOverlaysConfig, Notification> notification,
            ToIntFunction<ImmersiveCombatOverlaysConfig> durationSeconds, Predicate<ImmersiveCombatOverlaysConfig> onlyInCombat,
            ToIntFunction<ImmersiveCombatOverlaysConfig> leadSeconds, Predicate<ImmersiveCombatOverlaysConfig> debug)
        {
            this.enabled = enabled;
            this.color = color;
            this.glow = glow;
            this.shower = shower;
            this.notification = notification;
            this.durationSeconds = durationSeconds;
            this.onlyInCombat = onlyInCombat;
            this.leadSeconds = leadSeconds;
            this.debug = debug;
        }
    }

    /** A potion family's remind toggle and expiring-soon lead. */
    public static final class Potion
    {
        final Predicate<ImmersiveCombatOverlaysConfig> remind;
        final ToIntFunction<ImmersiveCombatOverlaysConfig> leadSeconds;

        Potion(Predicate<ImmersiveCombatOverlaysConfig> remind, ToIntFunction<ImmersiveCombatOverlaysConfig> leadSeconds)
        {
            this.remind = remind;
            this.leadSeconds = leadSeconds;
        }
    }

    public static final Map<CombatSpell, Spell> SPELLS = new EnumMap<>(CombatSpell.class);
    public static final Map<StatusEffect, Status> STATUSES = new EnumMap<>(StatusEffect.class);
    public static final Map<ReminderFamily, Family> FAMILIES = new EnumMap<>(ReminderFamily.class);
    public static final Map<PotionFamily, Potion> POTIONS = new EnumMap<>(PotionFamily.class);

    static
    {
        SPELLS.put(CombatSpell.RESURRECT_THRALL, new Spell(
            ImmersiveCombatOverlaysConfig::spellThrallEnabled, ImmersiveCombatOverlaysConfig::spellThrallColor,
            ImmersiveCombatOverlaysConfig::spellThrallGlow, ImmersiveCombatOverlaysConfig::spellThrallShower,
            ImmersiveCombatOverlaysConfig::spellThrallSpellbook, ImmersiveCombatOverlaysConfig::spellThrallNotification,
            ImmersiveCombatOverlaysConfig::spellThrallDurationSeconds, ImmersiveCombatOverlaysConfig::spellThrallLeadSeconds,
            ImmersiveCombatOverlaysConfig::spellThrallRecastProtection, ImmersiveCombatOverlaysConfig::debugSpellThrall));
        SPELLS.put(CombatSpell.DEATH_CHARGE, new Spell(
            ImmersiveCombatOverlaysConfig::spellDeathChargeEnabled, ImmersiveCombatOverlaysConfig::spellDeathChargeColor,
            ImmersiveCombatOverlaysConfig::spellDeathChargeGlow, ImmersiveCombatOverlaysConfig::spellDeathChargeShower,
            ImmersiveCombatOverlaysConfig::spellDeathChargeSpellbook, ImmersiveCombatOverlaysConfig::spellDeathChargeNotification,
            ImmersiveCombatOverlaysConfig::spellDeathChargeDurationSeconds, ImmersiveCombatOverlaysConfig::spellDeathChargeLeadSeconds,
            ImmersiveCombatOverlaysConfig::spellDeathChargeRecastProtection, ImmersiveCombatOverlaysConfig::debugSpellDeathCharge));
        SPELLS.put(CombatSpell.MARK_OF_DARKNESS, new Spell(
            ImmersiveCombatOverlaysConfig::spellMarkOfDarknessEnabled, ImmersiveCombatOverlaysConfig::spellMarkOfDarknessColor,
            ImmersiveCombatOverlaysConfig::spellMarkOfDarknessGlow, ImmersiveCombatOverlaysConfig::spellMarkOfDarknessShower,
            ImmersiveCombatOverlaysConfig::spellMarkOfDarknessSpellbook, ImmersiveCombatOverlaysConfig::spellMarkOfDarknessNotification,
            ImmersiveCombatOverlaysConfig::spellMarkOfDarknessDurationSeconds, ImmersiveCombatOverlaysConfig::spellMarkOfDarknessLeadSeconds,
            ImmersiveCombatOverlaysConfig::spellMarkOfDarknessRecastProtection, ImmersiveCombatOverlaysConfig::debugSpellMarkOfDarkness));
        SPELLS.put(CombatSpell.WARD_OF_ARCEUUS, new Spell(
            ImmersiveCombatOverlaysConfig::spellWardOfArceuusEnabled, ImmersiveCombatOverlaysConfig::spellWardOfArceuusColor,
            ImmersiveCombatOverlaysConfig::spellWardOfArceuusGlow, ImmersiveCombatOverlaysConfig::spellWardOfArceuusShower,
            ImmersiveCombatOverlaysConfig::spellWardOfArceuusSpellbook, ImmersiveCombatOverlaysConfig::spellWardOfArceuusNotification,
            ImmersiveCombatOverlaysConfig::spellWardOfArceuusDurationSeconds, ImmersiveCombatOverlaysConfig::spellWardOfArceuusLeadSeconds,
            ImmersiveCombatOverlaysConfig::spellWardOfArceuusRecastProtection, ImmersiveCombatOverlaysConfig::debugSpellWardOfArceuus));
        SPELLS.put(CombatSpell.SHADOW_VEIL, new Spell(
            ImmersiveCombatOverlaysConfig::spellShadowVeilEnabled, ImmersiveCombatOverlaysConfig::spellShadowVeilColor,
            ImmersiveCombatOverlaysConfig::spellShadowVeilGlow, ImmersiveCombatOverlaysConfig::spellShadowVeilShower,
            ImmersiveCombatOverlaysConfig::spellShadowVeilSpellbook, ImmersiveCombatOverlaysConfig::spellShadowVeilNotification,
            ImmersiveCombatOverlaysConfig::spellShadowVeilDurationSeconds, ImmersiveCombatOverlaysConfig::spellShadowVeilLeadSeconds,
            ImmersiveCombatOverlaysConfig::spellShadowVeilRecastProtection, ImmersiveCombatOverlaysConfig::debugSpellShadowVeil));
        SPELLS.put(CombatSpell.CORRUPTION, new Spell(
            ImmersiveCombatOverlaysConfig::spellCorruptionEnabled, ImmersiveCombatOverlaysConfig::spellCorruptionColor,
            ImmersiveCombatOverlaysConfig::spellCorruptionGlow, ImmersiveCombatOverlaysConfig::spellCorruptionShower,
            ImmersiveCombatOverlaysConfig::spellCorruptionSpellbook, ImmersiveCombatOverlaysConfig::spellCorruptionNotification,
            ImmersiveCombatOverlaysConfig::spellCorruptionDurationSeconds, ImmersiveCombatOverlaysConfig::spellCorruptionLeadSeconds,
            ImmersiveCombatOverlaysConfig::spellCorruptionRecastProtection, ImmersiveCombatOverlaysConfig::debugSpellCorruption));
        SPELLS.put(CombatSpell.VENGEANCE, new Spell(
            ImmersiveCombatOverlaysConfig::spellVengeanceEnabled, ImmersiveCombatOverlaysConfig::spellVengeanceColor,
            ImmersiveCombatOverlaysConfig::spellVengeanceGlow, ImmersiveCombatOverlaysConfig::spellVengeanceShower,
            ImmersiveCombatOverlaysConfig::spellVengeanceSpellbook, ImmersiveCombatOverlaysConfig::spellVengeanceNotification,
            ImmersiveCombatOverlaysConfig::spellVengeanceDurationSeconds, ImmersiveCombatOverlaysConfig::spellVengeanceLeadSeconds,
            ImmersiveCombatOverlaysConfig::spellVengeanceRecastProtection, ImmersiveCombatOverlaysConfig::debugSpellVengeance));
        SPELLS.put(CombatSpell.VENGEANCE_OTHER, new Spell(
            ImmersiveCombatOverlaysConfig::spellVengeanceOtherEnabled, ImmersiveCombatOverlaysConfig::spellVengeanceOtherColor,
            ImmersiveCombatOverlaysConfig::spellVengeanceOtherGlow, ImmersiveCombatOverlaysConfig::spellVengeanceOtherShower,
            ImmersiveCombatOverlaysConfig::spellVengeanceOtherSpellbook, ImmersiveCombatOverlaysConfig::spellVengeanceOtherNotification,
            ImmersiveCombatOverlaysConfig::spellVengeanceOtherDurationSeconds, ImmersiveCombatOverlaysConfig::spellVengeanceOtherLeadSeconds,
            ImmersiveCombatOverlaysConfig::spellVengeanceOtherRecastProtection, ImmersiveCombatOverlaysConfig::debugSpellVengeanceOther));
        SPELLS.put(CombatSpell.CHARGE, new Spell(
            ImmersiveCombatOverlaysConfig::spellChargeEnabled, ImmersiveCombatOverlaysConfig::spellChargeColor,
            ImmersiveCombatOverlaysConfig::spellChargeGlow, ImmersiveCombatOverlaysConfig::spellChargeShower,
            ImmersiveCombatOverlaysConfig::spellChargeSpellbook, ImmersiveCombatOverlaysConfig::spellChargeNotification,
            ImmersiveCombatOverlaysConfig::spellChargeDurationSeconds, ImmersiveCombatOverlaysConfig::spellChargeLeadSeconds,
            NEVER, // Charge can always be recast to refresh it
            ImmersiveCombatOverlaysConfig::debugSpellCharge));

        STATUSES.put(StatusEffect.LOW_HEALTH, new Status(
            ImmersiveCombatOverlaysConfig::lowHealthEnabled, ImmersiveCombatOverlaysConfig::lowHealthColor,
            ImmersiveCombatOverlaysConfig::lowHealthGlow, ImmersiveCombatOverlaysConfig::lowHealthShower,
            ImmersiveCombatOverlaysConfig::lowHealthDurationSeconds, ImmersiveCombatOverlaysConfig::lowHealthNotification,
            ImmersiveCombatOverlaysConfig::debugLowHealth));
        STATUSES.put(StatusEffect.DAMAGE, new Status(
            ImmersiveCombatOverlaysConfig::damageFeedbackEnabled, ImmersiveCombatOverlaysConfig::damageFlashColor,
            ImmersiveCombatOverlaysConfig::damageGlow, ImmersiveCombatOverlaysConfig::damageShower,
            ZERO, NO_NOTIFICATION, ImmersiveCombatOverlaysConfig::debugDamageFlash));
        STATUSES.put(StatusEffect.LOW_PRAYER, new Status(
            ImmersiveCombatOverlaysConfig::lowPrayerEnabled, ImmersiveCombatOverlaysConfig::lowPrayerColor,
            ImmersiveCombatOverlaysConfig::lowPrayerGlow, ImmersiveCombatOverlaysConfig::lowPrayerShower,
            ImmersiveCombatOverlaysConfig::lowPrayerDurationSeconds, ImmersiveCombatOverlaysConfig::lowPrayerNotification,
            ImmersiveCombatOverlaysConfig::debugLowPrayer));
        STATUSES.put(StatusEffect.POISON, new Status(
            ImmersiveCombatOverlaysConfig::poisonEnabled, ImmersiveCombatOverlaysConfig::poisonColor,
            ImmersiveCombatOverlaysConfig::poisonGlow, ImmersiveCombatOverlaysConfig::poisonShower,
            ImmersiveCombatOverlaysConfig::poisonDurationSeconds, ImmersiveCombatOverlaysConfig::poisonNotification,
            ImmersiveCombatOverlaysConfig::debugPoison));
        STATUSES.put(StatusEffect.POISON_TICK, new Status(
            ImmersiveCombatOverlaysConfig::poisonEnabled, ImmersiveCombatOverlaysConfig::poisonColor,
            ImmersiveCombatOverlaysConfig::poisonGlow, NEVER, ZERO, NO_NOTIFICATION, NEVER));
        STATUSES.put(StatusEffect.VENOM, new Status(
            ImmersiveCombatOverlaysConfig::venomEnabled, ImmersiveCombatOverlaysConfig::venomColor,
            ImmersiveCombatOverlaysConfig::venomGlow, ImmersiveCombatOverlaysConfig::venomShower,
            ImmersiveCombatOverlaysConfig::venomDurationSeconds, ImmersiveCombatOverlaysConfig::venomNotification,
            ImmersiveCombatOverlaysConfig::debugVenom));
        STATUSES.put(StatusEffect.VENOM_TICK, new Status(
            ImmersiveCombatOverlaysConfig::venomEnabled, ImmersiveCombatOverlaysConfig::venomColor,
            ImmersiveCombatOverlaysConfig::venomGlow, NEVER, ZERO, NO_NOTIFICATION, NEVER));
        STATUSES.put(StatusEffect.DISEASE, new Status(
            ImmersiveCombatOverlaysConfig::diseaseEnabled, ImmersiveCombatOverlaysConfig::diseaseColor,
            ImmersiveCombatOverlaysConfig::diseaseGlow, ImmersiveCombatOverlaysConfig::diseaseShower,
            ImmersiveCombatOverlaysConfig::diseaseDurationSeconds, ImmersiveCombatOverlaysConfig::diseaseNotification,
            ImmersiveCombatOverlaysConfig::debugDisease));
        STATUSES.put(StatusEffect.FREEZE, new Status(
            ImmersiveCombatOverlaysConfig::freezeEnabled, ImmersiveCombatOverlaysConfig::freezeColor,
            ImmersiveCombatOverlaysConfig::freezeGlow, ImmersiveCombatOverlaysConfig::freezeShower,
            ZERO, // the freeze tracker applies its own max display time
            ImmersiveCombatOverlaysConfig::freezeNotification, ImmersiveCombatOverlaysConfig::debugFreeze));
        STATUSES.put(StatusEffect.IDLE, new Status(
            ImmersiveCombatOverlaysConfig::idleEnabled, ImmersiveCombatOverlaysConfig::idleColor,
            ImmersiveCombatOverlaysConfig::idleGlow, ImmersiveCombatOverlaysConfig::idleShower,
            ZERO, NO_NOTIFICATION, // the idle tracker sends its own notification and auto-hides itself
            ImmersiveCombatOverlaysConfig::debugIdle));
        STATUSES.put(StatusEffect.SPEC_READY, new Status(
            ImmersiveCombatOverlaysConfig::specEnabled, ImmersiveCombatOverlaysConfig::specColor,
            ImmersiveCombatOverlaysConfig::specGlow, ImmersiveCombatOverlaysConfig::specShower,
            ImmersiveCombatOverlaysConfig::specDurationSeconds, ImmersiveCombatOverlaysConfig::specNotification,
            ImmersiveCombatOverlaysConfig::debugSpecReady));
        STATUSES.put(StatusEffect.LOW_RUN_ENERGY, new Status(
            ImmersiveCombatOverlaysConfig::lowRunEnabled, ImmersiveCombatOverlaysConfig::lowRunColor,
            ImmersiveCombatOverlaysConfig::lowRunGlow, ImmersiveCombatOverlaysConfig::lowRunShower,
            ImmersiveCombatOverlaysConfig::lowRunDurationSeconds, ImmersiveCombatOverlaysConfig::lowRunNotification,
            ImmersiveCombatOverlaysConfig::debugLowRun));
        STATUSES.put(StatusEffect.UNPOTTED, new Status(
            ImmersiveCombatOverlaysConfig::unpottedEnabled, ImmersiveCombatOverlaysConfig::unpottedColor,
            ImmersiveCombatOverlaysConfig::unpottedGlow, ImmersiveCombatOverlaysConfig::unpottedShower,
            ZERO, // the unpotted tracker applies its own alert duration
            ImmersiveCombatOverlaysConfig::unpottedNotification, ImmersiveCombatOverlaysConfig::debugUnpotted));
        STATUSES.put(StatusEffect.PARASITE, new Status(
            ImmersiveCombatOverlaysConfig::parasiteEnabled, ImmersiveCombatOverlaysConfig::parasiteColor,
            ImmersiveCombatOverlaysConfig::parasiteGlow, ImmersiveCombatOverlaysConfig::parasiteShower,
            ImmersiveCombatOverlaysConfig::parasiteDurationSeconds, ImmersiveCombatOverlaysConfig::parasiteNotification,
            ImmersiveCombatOverlaysConfig::debugParasite));

        FAMILIES.put(ReminderFamily.HEART, new Family(
            ImmersiveCombatOverlaysConfig::heartsEnabled, ImmersiveCombatOverlaysConfig::heartReminderColor,
            ImmersiveCombatOverlaysConfig::heartScreenReminder, ImmersiveCombatOverlaysConfig::heartShower,
            ImmersiveCombatOverlaysConfig::heartNotification, ImmersiveCombatOverlaysConfig::heartDurationSeconds,
            ImmersiveCombatOverlaysConfig::heartOnlyInCombat, ImmersiveCombatOverlaysConfig::heartPreRemindSeconds,
            ImmersiveCombatOverlaysConfig::debugHeartReady));
        FAMILIES.put(ReminderFamily.POTION, new Family(
            ImmersiveCombatOverlaysConfig::potionRemindersEnabled, ImmersiveCombatOverlaysConfig::buffReminderColor,
            ImmersiveCombatOverlaysConfig::buffScreenReminder, ImmersiveCombatOverlaysConfig::buffShower,
            ImmersiveCombatOverlaysConfig::buffNotification, ImmersiveCombatOverlaysConfig::buffDurationSeconds,
            ImmersiveCombatOverlaysConfig::buffOnlyInCombat, ZERO, // per potion family, see POTIONS
            ImmersiveCombatOverlaysConfig::debugBuffExpired));
        FAMILIES.put(ReminderFamily.BOOST, new Family(
            ImmersiveCombatOverlaysConfig::boostPotionsEnabled, ImmersiveCombatOverlaysConfig::boostReminderColor,
            ImmersiveCombatOverlaysConfig::boostGlow, ImmersiveCombatOverlaysConfig::boostShower,
            ImmersiveCombatOverlaysConfig::boostNotification, ImmersiveCombatOverlaysConfig::boostDurationSeconds,
            ImmersiveCombatOverlaysConfig::boostOnlyInCombat, ImmersiveCombatOverlaysConfig::boostPreRemindSeconds,
            NEVER));

        POTIONS.put(PotionFamily.STAMINA, new Potion(
            ImmersiveCombatOverlaysConfig::remindStamina, ImmersiveCombatOverlaysConfig::staminaLeadSeconds));
        POTIONS.put(PotionFamily.ANTIPOISON, new Potion(
            ImmersiveCombatOverlaysConfig::remindAntipoison, ImmersiveCombatOverlaysConfig::antipoisonLeadSeconds));
        POTIONS.put(PotionFamily.ANTIFIRE, new Potion(
            ImmersiveCombatOverlaysConfig::remindAntifire, ImmersiveCombatOverlaysConfig::antifireLeadSeconds));
        POTIONS.put(PotionFamily.DIVINE, new Potion(
            ImmersiveCombatOverlaysConfig::remindDivine, ImmersiveCombatOverlaysConfig::divinePreRemindSeconds));
        POTIONS.put(PotionFamily.RAID_BOOST, new Potion(
            ImmersiveCombatOverlaysConfig::remindRaidBoosts, ImmersiveCombatOverlaysConfig::raidBoostLeadSeconds));
        POTIONS.put(PotionFamily.SURGE, new Potion(
            ImmersiveCombatOverlaysConfig::remindSurge, ImmersiveCombatOverlaysConfig::surgeLeadSeconds));
        POTIONS.put(PotionFamily.GOADING, new Potion(
            ImmersiveCombatOverlaysConfig::remindGoading, ImmersiveCombatOverlaysConfig::goadingLeadSeconds));
        POTIONS.put(PotionFamily.PRAYER_REGENERATION, new Potion(
            ImmersiveCombatOverlaysConfig::remindPrayerRegeneration, ImmersiveCombatOverlaysConfig::prayerRegenerationLeadSeconds));
        POTIONS.put(PotionFamily.OTHER, new Potion(
            ImmersiveCombatOverlaysConfig::remindOtherPotions, ImmersiveCombatOverlaysConfig::otherPotionLeadSeconds));
    }
}
