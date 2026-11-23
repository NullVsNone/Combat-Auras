package com.combatauras.settings;

import com.combatauras.config.CombatAurasConfig;
import com.combatauras.model.CombatSpell;
import com.combatauras.model.PotionFamily;
import com.combatauras.model.ReminderFamily;
import com.combatauras.model.StatusEffect;
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

    private static final Predicate<CombatAurasConfig> NEVER = c -> false;
    private static final ToIntFunction<CombatAurasConfig> ZERO = c -> 0;
    private static final Function<CombatAurasConfig, Notification> NO_NOTIFICATION = c -> Notification.OFF;

    /** Per-spell settings. */
    public static final class Spell
    {
        final Predicate<CombatAurasConfig> enabled;
        final Function<CombatAurasConfig, Color> color;
        final Predicate<CombatAurasConfig> glow;
        final Predicate<CombatAurasConfig> shower;
        final Predicate<CombatAurasConfig> spellbook;
        final Function<CombatAurasConfig, Notification> notification;
        final ToIntFunction<CombatAurasConfig> durationSeconds;
        final ToIntFunction<CombatAurasConfig> leadSeconds;
        final Predicate<CombatAurasConfig> recastProtection;
        final Predicate<CombatAurasConfig> debug;

        Spell(Predicate<CombatAurasConfig> enabled, Function<CombatAurasConfig, Color> color,
            Predicate<CombatAurasConfig> glow, Predicate<CombatAurasConfig> shower,
            Predicate<CombatAurasConfig> spellbook, Function<CombatAurasConfig, Notification> notification,
            ToIntFunction<CombatAurasConfig> durationSeconds, ToIntFunction<CombatAurasConfig> leadSeconds,
            Predicate<CombatAurasConfig> recastProtection, Predicate<CombatAurasConfig> debug)
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
        final Predicate<CombatAurasConfig> enabled;
        final Function<CombatAurasConfig, Color> color;
        final Predicate<CombatAurasConfig> glow;
        final Predicate<CombatAurasConfig> shower;
        final ToIntFunction<CombatAurasConfig> durationSeconds;
        final Function<CombatAurasConfig, Notification> notification;
        final Predicate<CombatAurasConfig> debug;

        Status(Predicate<CombatAurasConfig> enabled, Function<CombatAurasConfig, Color> color,
            Predicate<CombatAurasConfig> glow, Predicate<CombatAurasConfig> shower,
            ToIntFunction<CombatAurasConfig> durationSeconds,
            Function<CombatAurasConfig, Notification> notification, Predicate<CombatAurasConfig> debug)
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
        final Predicate<CombatAurasConfig> enabled;
        final Function<CombatAurasConfig, Color> color;
        final Predicate<CombatAurasConfig> glow;
        final Predicate<CombatAurasConfig> shower;
        final Function<CombatAurasConfig, Notification> notification;
        final ToIntFunction<CombatAurasConfig> durationSeconds;
        final Predicate<CombatAurasConfig> onlyInCombat;
        final ToIntFunction<CombatAurasConfig> leadSeconds;
        final Predicate<CombatAurasConfig> debug;

        Family(Predicate<CombatAurasConfig> enabled, Function<CombatAurasConfig, Color> color,
            Predicate<CombatAurasConfig> glow, Predicate<CombatAurasConfig> shower,
            Function<CombatAurasConfig, Notification> notification,
            ToIntFunction<CombatAurasConfig> durationSeconds, Predicate<CombatAurasConfig> onlyInCombat,
            ToIntFunction<CombatAurasConfig> leadSeconds, Predicate<CombatAurasConfig> debug)
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
        final Predicate<CombatAurasConfig> remind;
        final ToIntFunction<CombatAurasConfig> leadSeconds;

        Potion(Predicate<CombatAurasConfig> remind, ToIntFunction<CombatAurasConfig> leadSeconds)
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
            CombatAurasConfig::spellThrallEnabled, CombatAurasConfig::spellThrallColor,
            CombatAurasConfig::spellThrallGlow, CombatAurasConfig::spellThrallShower,
            CombatAurasConfig::spellThrallSpellbook, CombatAurasConfig::spellThrallNotification,
            CombatAurasConfig::spellThrallDurationSeconds, CombatAurasConfig::spellThrallLeadSeconds,
            CombatAurasConfig::spellThrallRecastProtection, CombatAurasConfig::debugSpellThrall));
        SPELLS.put(CombatSpell.DEATH_CHARGE, new Spell(
            CombatAurasConfig::spellDeathChargeEnabled, CombatAurasConfig::spellDeathChargeColor,
            CombatAurasConfig::spellDeathChargeGlow, CombatAurasConfig::spellDeathChargeShower,
            CombatAurasConfig::spellDeathChargeSpellbook, CombatAurasConfig::spellDeathChargeNotification,
            CombatAurasConfig::spellDeathChargeDurationSeconds, CombatAurasConfig::spellDeathChargeLeadSeconds,
            CombatAurasConfig::spellDeathChargeRecastProtection, CombatAurasConfig::debugSpellDeathCharge));
        SPELLS.put(CombatSpell.MARK_OF_DARKNESS, new Spell(
            CombatAurasConfig::spellMarkOfDarknessEnabled, CombatAurasConfig::spellMarkOfDarknessColor,
            CombatAurasConfig::spellMarkOfDarknessGlow, CombatAurasConfig::spellMarkOfDarknessShower,
            CombatAurasConfig::spellMarkOfDarknessSpellbook, CombatAurasConfig::spellMarkOfDarknessNotification,
            CombatAurasConfig::spellMarkOfDarknessDurationSeconds, CombatAurasConfig::spellMarkOfDarknessLeadSeconds,
            CombatAurasConfig::spellMarkOfDarknessRecastProtection, CombatAurasConfig::debugSpellMarkOfDarkness));
        SPELLS.put(CombatSpell.WARD_OF_ARCEUUS, new Spell(
            CombatAurasConfig::spellWardOfArceuusEnabled, CombatAurasConfig::spellWardOfArceuusColor,
            CombatAurasConfig::spellWardOfArceuusGlow, CombatAurasConfig::spellWardOfArceuusShower,
            CombatAurasConfig::spellWardOfArceuusSpellbook, CombatAurasConfig::spellWardOfArceuusNotification,
            CombatAurasConfig::spellWardOfArceuusDurationSeconds, CombatAurasConfig::spellWardOfArceuusLeadSeconds,
            CombatAurasConfig::spellWardOfArceuusRecastProtection, CombatAurasConfig::debugSpellWardOfArceuus));
        SPELLS.put(CombatSpell.SHADOW_VEIL, new Spell(
            CombatAurasConfig::spellShadowVeilEnabled, CombatAurasConfig::spellShadowVeilColor,
            CombatAurasConfig::spellShadowVeilGlow, CombatAurasConfig::spellShadowVeilShower,
            CombatAurasConfig::spellShadowVeilSpellbook, CombatAurasConfig::spellShadowVeilNotification,
            CombatAurasConfig::spellShadowVeilDurationSeconds, CombatAurasConfig::spellShadowVeilLeadSeconds,
            CombatAurasConfig::spellShadowVeilRecastProtection, CombatAurasConfig::debugSpellShadowVeil));
        SPELLS.put(CombatSpell.CORRUPTION, new Spell(
            CombatAurasConfig::spellCorruptionEnabled, CombatAurasConfig::spellCorruptionColor,
            CombatAurasConfig::spellCorruptionGlow, CombatAurasConfig::spellCorruptionShower,
            CombatAurasConfig::spellCorruptionSpellbook, CombatAurasConfig::spellCorruptionNotification,
            CombatAurasConfig::spellCorruptionDurationSeconds, CombatAurasConfig::spellCorruptionLeadSeconds,
            CombatAurasConfig::spellCorruptionRecastProtection, CombatAurasConfig::debugSpellCorruption));
        SPELLS.put(CombatSpell.VENGEANCE, new Spell(
            CombatAurasConfig::spellVengeanceEnabled, CombatAurasConfig::spellVengeanceColor,
            CombatAurasConfig::spellVengeanceGlow, CombatAurasConfig::spellVengeanceShower,
            CombatAurasConfig::spellVengeanceSpellbook, CombatAurasConfig::spellVengeanceNotification,
            CombatAurasConfig::spellVengeanceDurationSeconds, CombatAurasConfig::spellVengeanceLeadSeconds,
            CombatAurasConfig::spellVengeanceRecastProtection, CombatAurasConfig::debugSpellVengeance));
        SPELLS.put(CombatSpell.VENGEANCE_OTHER, new Spell(
            CombatAurasConfig::spellVengeanceOtherEnabled, CombatAurasConfig::spellVengeanceOtherColor,
            CombatAurasConfig::spellVengeanceOtherGlow, CombatAurasConfig::spellVengeanceOtherShower,
            CombatAurasConfig::spellVengeanceOtherSpellbook, CombatAurasConfig::spellVengeanceOtherNotification,
            CombatAurasConfig::spellVengeanceOtherDurationSeconds, CombatAurasConfig::spellVengeanceOtherLeadSeconds,
            CombatAurasConfig::spellVengeanceOtherRecastProtection, CombatAurasConfig::debugSpellVengeanceOther));
        SPELLS.put(CombatSpell.CHARGE, new Spell(
            CombatAurasConfig::spellChargeEnabled, CombatAurasConfig::spellChargeColor,
            CombatAurasConfig::spellChargeGlow, CombatAurasConfig::spellChargeShower,
            CombatAurasConfig::spellChargeSpellbook, CombatAurasConfig::spellChargeNotification,
            CombatAurasConfig::spellChargeDurationSeconds, CombatAurasConfig::spellChargeLeadSeconds,
            NEVER, // Charge can always be recast to refresh it
            CombatAurasConfig::debugSpellCharge));

        STATUSES.put(StatusEffect.LOW_HEALTH, new Status(
            CombatAurasConfig::lowHealthEnabled, CombatAurasConfig::lowHealthColor,
            CombatAurasConfig::lowHealthGlow, CombatAurasConfig::lowHealthShower,
            CombatAurasConfig::lowHealthDurationSeconds, CombatAurasConfig::lowHealthNotification,
            CombatAurasConfig::debugLowHealth));
        STATUSES.put(StatusEffect.DAMAGE, new Status(
            CombatAurasConfig::damageFeedbackEnabled, CombatAurasConfig::damageFlashColor,
            CombatAurasConfig::damageGlow, CombatAurasConfig::damageShower,
            ZERO, NO_NOTIFICATION, CombatAurasConfig::debugDamageFlash));
        STATUSES.put(StatusEffect.LOW_PRAYER, new Status(
            CombatAurasConfig::lowPrayerEnabled, CombatAurasConfig::lowPrayerColor,
            CombatAurasConfig::lowPrayerGlow, CombatAurasConfig::lowPrayerShower,
            CombatAurasConfig::lowPrayerDurationSeconds, CombatAurasConfig::lowPrayerNotification,
            CombatAurasConfig::debugLowPrayer));
        STATUSES.put(StatusEffect.POISON, new Status(
            CombatAurasConfig::poisonEnabled, CombatAurasConfig::poisonColor,
            CombatAurasConfig::poisonGlow, CombatAurasConfig::poisonShower,
            CombatAurasConfig::poisonDurationSeconds, CombatAurasConfig::poisonNotification,
            CombatAurasConfig::debugPoison));
        STATUSES.put(StatusEffect.POISON_TICK, new Status(
            CombatAurasConfig::poisonEnabled, CombatAurasConfig::poisonColor,
            CombatAurasConfig::poisonGlow, NEVER, ZERO, NO_NOTIFICATION, NEVER));
        STATUSES.put(StatusEffect.VENOM, new Status(
            CombatAurasConfig::venomEnabled, CombatAurasConfig::venomColor,
            CombatAurasConfig::venomGlow, CombatAurasConfig::venomShower,
            CombatAurasConfig::venomDurationSeconds, CombatAurasConfig::venomNotification,
            CombatAurasConfig::debugVenom));
        STATUSES.put(StatusEffect.VENOM_TICK, new Status(
            CombatAurasConfig::venomEnabled, CombatAurasConfig::venomColor,
            CombatAurasConfig::venomGlow, NEVER, ZERO, NO_NOTIFICATION, NEVER));
        STATUSES.put(StatusEffect.DISEASE, new Status(
            CombatAurasConfig::diseaseEnabled, CombatAurasConfig::diseaseColor,
            CombatAurasConfig::diseaseGlow, CombatAurasConfig::diseaseShower,
            CombatAurasConfig::diseaseDurationSeconds, CombatAurasConfig::diseaseNotification,
            CombatAurasConfig::debugDisease));
        STATUSES.put(StatusEffect.FREEZE, new Status(
            CombatAurasConfig::freezeEnabled, CombatAurasConfig::freezeColor,
            CombatAurasConfig::freezeGlow, CombatAurasConfig::freezeShower,
            ZERO, // the freeze tracker applies its own max display time
            CombatAurasConfig::freezeNotification, CombatAurasConfig::debugFreeze));
        STATUSES.put(StatusEffect.IDLE, new Status(
            CombatAurasConfig::idleEnabled, CombatAurasConfig::idleColor,
            CombatAurasConfig::idleGlow, CombatAurasConfig::idleShower,
            ZERO, NO_NOTIFICATION, // the idle tracker sends its own notification and auto-hides itself
            CombatAurasConfig::debugIdle));
        STATUSES.put(StatusEffect.SPEC_READY, new Status(
            CombatAurasConfig::specEnabled, CombatAurasConfig::specColor,
            CombatAurasConfig::specGlow, CombatAurasConfig::specShower,
            CombatAurasConfig::specDurationSeconds, CombatAurasConfig::specNotification,
            CombatAurasConfig::debugSpecReady));
        STATUSES.put(StatusEffect.LOW_RUN_ENERGY, new Status(
            CombatAurasConfig::lowRunEnabled, CombatAurasConfig::lowRunColor,
            CombatAurasConfig::lowRunGlow, CombatAurasConfig::lowRunShower,
            CombatAurasConfig::lowRunDurationSeconds, CombatAurasConfig::lowRunNotification,
            CombatAurasConfig::debugLowRun));
        STATUSES.put(StatusEffect.UNPOTTED, new Status(
            CombatAurasConfig::unpottedEnabled, CombatAurasConfig::unpottedColor,
            CombatAurasConfig::unpottedGlow, CombatAurasConfig::unpottedShower,
            ZERO, // the unpotted tracker applies its own alert duration
            CombatAurasConfig::unpottedNotification, CombatAurasConfig::debugUnpotted));

        FAMILIES.put(ReminderFamily.HEART, new Family(
            CombatAurasConfig::heartsEnabled, CombatAurasConfig::heartReminderColor,
            CombatAurasConfig::heartScreenReminder, CombatAurasConfig::heartShower,
            CombatAurasConfig::heartNotification, CombatAurasConfig::heartDurationSeconds,
            CombatAurasConfig::heartOnlyInCombat, CombatAurasConfig::heartPreRemindSeconds,
            CombatAurasConfig::debugHeartReady));
        FAMILIES.put(ReminderFamily.POTION, new Family(
            CombatAurasConfig::potionRemindersEnabled, CombatAurasConfig::buffReminderColor,
            CombatAurasConfig::buffScreenReminder, CombatAurasConfig::buffShower,
            CombatAurasConfig::buffNotification, CombatAurasConfig::buffDurationSeconds,
            CombatAurasConfig::buffOnlyInCombat, ZERO, // per potion family, see POTIONS
            CombatAurasConfig::debugBuffExpired));
        FAMILIES.put(ReminderFamily.BOOST, new Family(
            CombatAurasConfig::boostPotionsEnabled, CombatAurasConfig::boostReminderColor,
            CombatAurasConfig::boostGlow, CombatAurasConfig::boostShower,
            CombatAurasConfig::boostNotification, CombatAurasConfig::boostDurationSeconds,
            CombatAurasConfig::boostOnlyInCombat, CombatAurasConfig::boostPreRemindSeconds,
            NEVER));

        POTIONS.put(PotionFamily.STAMINA, new Potion(
            CombatAurasConfig::remindStamina, CombatAurasConfig::staminaLeadSeconds));
        POTIONS.put(PotionFamily.ANTIPOISON, new Potion(
            CombatAurasConfig::remindAntipoison, CombatAurasConfig::antipoisonLeadSeconds));
        POTIONS.put(PotionFamily.ANTIFIRE, new Potion(
            CombatAurasConfig::remindAntifire, CombatAurasConfig::antifireLeadSeconds));
        POTIONS.put(PotionFamily.DIVINE, new Potion(
            CombatAurasConfig::remindDivine, CombatAurasConfig::divinePreRemindSeconds));
        POTIONS.put(PotionFamily.RAID_BOOST, new Potion(
            CombatAurasConfig::remindRaidBoosts, CombatAurasConfig::raidBoostLeadSeconds));
        POTIONS.put(PotionFamily.SURGE, new Potion(
            CombatAurasConfig::remindSurge, CombatAurasConfig::surgeLeadSeconds));
        POTIONS.put(PotionFamily.GOADING, new Potion(
            CombatAurasConfig::remindGoading, CombatAurasConfig::goadingLeadSeconds));
        POTIONS.put(PotionFamily.PRAYER_REGENERATION, new Potion(
            CombatAurasConfig::remindPrayerRegeneration, CombatAurasConfig::prayerRegenerationLeadSeconds));
        POTIONS.put(PotionFamily.OTHER, new Potion(
            CombatAurasConfig::remindOtherPotions, CombatAurasConfig::otherPotionLeadSeconds));
    }
}
