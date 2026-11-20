package com.immersivecombatoverlays.config;

import com.immersivecombatoverlays.model.AnimationMode;
import com.immersivecombatoverlays.model.BorderStyle;
import com.immersivecombatoverlays.model.CombatSpell;
import com.immersivecombatoverlays.model.DisplayMode;
import com.immersivecombatoverlays.model.GlowColorMode;
import com.immersivecombatoverlays.model.ReadyStyle;
import com.immersivecombatoverlays.model.ShowerStartLocation;
import com.immersivecombatoverlays.model.SpellCooldownDisplayMode;
import com.immersivecombatoverlays.model.ThrallDurationTier;
import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("immersivecombatoverlays")
public interface ImmersiveCombatOverlaysConfig extends Config
{
    // =========================================================================
    // Sections: Shared Settings at Top -> Individual Warning & Spell Sections
    // =========================================================================

    @ConfigSection(name = "General Display & Performance", description = "Master screen-edge glow intensity, depth, tint, motion, and combat break timing.", position = 0)
    String generalDisplaySection = "generalDisplaySection";

    @ConfigSection(name = "Icon Shower", description = "Shared icon shower layout, size, speed and duration.", position = 1, closedByDefault = true)
    String sharedShowerSection = "sharedShowerSection";

    @ConfigSection(name = "Inventory Cooldowns", description = "How cooldowns and ready indicators are drawn on inventory items, and re-dose protection.", position = 2, closedByDefault = true)
    String inventoryStyleSection = "inventoryStyleSection";

    @ConfigSection(name = "Shared Combat Spells", description = "Master spellbook, recast-after-cast, tab glow, and shared spell display settings.", position = 3, closedByDefault = true)
    String sharedSpellsSection = "sharedSpellsSection";

    @ConfigSection(name = "Shared Debug Tools", description = "Global preview intensity, simulate combat, and test cooldown/grid triggers.", position = 4, closedByDefault = true)
    String debugSection = "debugSection";

    @ConfigSection(name = "Low Health", description = "Low health threshold, pulse, color, glow, shower, and debug preview.", position = 10, closedByDefault = true)
    String warnLowHealthSection = "warnLowHealthSection";

    @ConfigSection(name = "Damage Flash", description = "Damage hit flash duration, strength, color, glow, shower, and debug preview.", position = 11, closedByDefault = true)
    String warnDamageSection = "warnDamageSection";

    @ConfigSection(name = "Low Prayer", description = "Low prayer threshold, color, glow, shower, restore highlight, and debug preview.", position = 12, closedByDefault = true)
    String warnPrayerSection = "warnPrayerSection";

    @ConfigSection(name = "Poison", description = "Poison color, glow, shower, and debug preview.", position = 13, closedByDefault = true)
    String warnPoisonSection = "warnPoisonSection";

    @ConfigSection(name = "Venom", description = "Venom color, glow, shower, and debug preview.", position = 14, closedByDefault = true)
    String warnVenomSection = "warnVenomSection";

    @ConfigSection(name = "Disease", description = "Disease color, glow, shower, and debug preview.", position = 15, closedByDefault = true)
    String warnDiseaseSection = "warnDiseaseSection";

    @ConfigSection(name = "Freeze", description = "Freeze color, glow, shower, max display duration, and debug preview.", position = 16, closedByDefault = true)
    String warnFreezeSection = "warnFreezeSection";

    @ConfigSection(name = "Idle Warning", description = "Skilling/combat idle ramp, AFK logout, color, glow, shower, and debug preview.", position = 17, closedByDefault = true)
    String warnIdleSection = "warnIdleSection";

    @ConfigSection(name = "Special Attack Ready", description = "Spec threshold, orb glow, screen glow, shower, and debug preview.", position = 18, closedByDefault = true)
    String warnSpecSection = "warnSpecSection";

    @ConfigSection(name = "Low Run Energy", description = "Low run threshold, color, glow, shower, and debug preview.", position = 19, closedByDefault = true)
    String warnLowRunSection = "warnLowRunSection";

    @ConfigSection(name = "Unpotted Alert", description = "Unpotted threshold, duration, color, glow, shower, and debug preview.", position = 20, closedByDefault = true)
    String warnUnpottedSection = "warnUnpottedSection";

    @ConfigSection(name = "Imbued / Saturated Heart", description = "Heart cooldown tracking, color, screen glow, shower, and debug preview.", position = 21, closedByDefault = true)
    String warnHeartSection = "warnHeartSection";

    @ConfigSection(name = "Potions, Boosts & Food", description = "Potion/boost/raid/food cooldowns, pre-remind, color, glow, shower, and debug preview.", position = 22, closedByDefault = true)
    String warnPotionsBuffsSection = "warnPotionsBuffsSection";

    @ConfigSection(name = "Spell: Resurrect Thrall", description = "Thrall CA tier, combat-start reminder, color, glow, shower, and debug preview.", position = 30, closedByDefault = true)
    String spellThrallSection = "spellThrallSection";

    @ConfigSection(name = "Spell: Death Charge", description = "Death Charge enable, color, glow, shower, and debug preview.", position = 31, closedByDefault = true)
    String spellDeathChargeSection = "spellDeathChargeSection";

    @ConfigSection(name = "Spell: Mark of Darkness", description = "Mark of Darkness enable, color, glow, shower, and debug preview.", position = 32, closedByDefault = true)
    String spellMarkOfDarknessSection = "spellMarkOfDarknessSection";

    @ConfigSection(name = "Spell: Ward of Arceuus", description = "Ward of Arceuus enable, color, glow, shower, and debug preview.", position = 33, closedByDefault = true)
    String spellWardOfArceuusSection = "spellWardOfArceuusSection";

    @ConfigSection(name = "Spell: Shadow Veil", description = "Shadow Veil enable, color, glow, shower, and debug preview.", position = 34, closedByDefault = true)
    String spellShadowVeilSection = "spellShadowVeilSection";

    @ConfigSection(name = "Spell: Corruption", description = "Corruption enable, color, glow, shower, and debug preview.", position = 35, closedByDefault = true)
    String spellCorruptionSection = "spellCorruptionSection";

    @ConfigSection(name = "Spell: Vengeance", description = "Vengeance enable, color, glow, shower, and debug preview.", position = 36, closedByDefault = true)
    String spellVengeanceSection = "spellVengeanceSection";

    @ConfigSection(name = "Spell: Vengeance Other", description = "Vengeance Other enable, color, glow, shower, and debug preview.", position = 37, closedByDefault = true)
    String spellVengeanceOtherSection = "spellVengeanceOtherSection";

    @ConfigSection(name = "Spell: Charge", description = "Charge enable, color, glow, shower, and debug preview.", position = 38, closedByDefault = true)
    String spellChargeSection = "spellChargeSection";

    // =========================================================================
    // 1. General Display & Performance
    // =========================================================================

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "effectIntensity", name = "Effect intensity", description = "Master opacity/intensity for screen edge glows.", section = generalDisplaySection, position = 0)
    default int effectIntensity() { return 75; }

    @Range(min = 10, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "edgeDepth", name = "Edge depth", description = "How far inward from the screen edges the glow extends.", section = generalDisplaySection, position = 1)
    default int edgeDepth() { return 45; }

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "dangerEscalation", name = "Danger escalation", description = "Extra intensity scaling as health/prayer drops further below threshold.", section = generalDisplaySection, position = 2)
    default int dangerEscalation() { return 60; }

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "screenTint", name = "Screen tint", description = "Subtle full-screen tint strength during active warnings.", section = generalDisplaySection, position = 3)
    default int screenTint() { return 20; }

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "motionStrength", name = "Motion strength", description = "Strength of pulsing and breathing animations.", section = generalDisplaySection, position = 4)
    default int motionStrength() { return 65; }

    @Range(min = 5, max = 600)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "combatBreakReRemindSeconds", name = "Combat break re-remind", description = "Out-of-combat break duration before re-entering combat triggers a fresh reminder.", section = generalDisplaySection, position = 5)
    default int combatBreakReRemindSeconds() { return 120; }

    @ConfigItem(keyName = "allowScreenGlows", name = "Allow screen glows", description = "Master switch for every screen-edge glow and tint. Turn off to keep only icons/item highlights.", section = generalDisplaySection, position = 6)
    default boolean allowScreenGlows() { return true; }

    @ConfigItem(keyName = "allowIconShowers", name = "Allow icon showers", description = "Master switch for every showering icon. Turn off to keep only glows.", section = generalDisplaySection, position = 7)
    default boolean allowIconShowers() { return true; }

    @ConfigItem(keyName = "allowItemGlows", name = "Allow item & spellbook glows", description = "Master switch for outlines/glows on inventory items, spell icons, the Magic tab and the spec orb.", section = generalDisplaySection, position = 8)
    default boolean allowItemGlows() { return true; }

    @ConfigItem(keyName = "glowColorMode", name = "Glow colour mixing", description = "When several warnings are active: blend their colours, or use the strongest warning's colour.", section = generalDisplaySection, position = 9)
    default GlowColorMode glowColorMode() { return GlowColorMode.BLEND; }

    // =========================================================================
    // 2. Icon Shower
    // =========================================================================

    @ConfigItem(keyName = "showerStartLocation", name = "Start edge", description = "Screen edge where showering icons spawn.", section = sharedShowerSection, position = 0)
    default ShowerStartLocation showerStartLocation() { return ShowerStartLocation.TOP; }

    @Range(min = 10, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "showerScreenPercent", name = "Travel distance", description = "Percentage of screen width/height showering icons travel.", section = sharedShowerSection, position = 1)
    default int showerScreenPercent() { return 65; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "showerDurationSeconds", name = "Spell shower duration (0 = always)", description = "Max seconds to shower a spell's icon after it expires or is re-reminded. Other warnings shower while active.", section = sharedShowerSection, position = 2)
    default int showerDurationSeconds() { return 12; }

    @Range(min = 1, max = 36)
    @ConfigItem(keyName = "spellShowerMaxIcons", name = "Max icons", description = "Maximum simultaneous showering icons across all active warnings.", section = sharedShowerSection, position = 3)
    default int spellShowerMaxIcons() { return 14; }

    @Range(min = 16, max = 48)
    @ConfigItem(keyName = "spellShowerIconSize", name = "Icon size (px)", description = "Rendered pixel size of showering icons.", section = sharedShowerSection, position = 4)
    default int spellShowerIconSize() { return 26; }

    @Range(min = 20, max = 250)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "spellShowerSpeed", name = "Speed", description = "Speed multiplier for showering icons.", section = sharedShowerSection, position = 5)
    default int spellShowerSpeed() { return 100; }

    // =========================================================================
    // 2b. Inventory Cooldowns
    // =========================================================================

    @ConfigItem(keyName = "displayMode", name = "Cooldown style", description = "How cooldowns are rendered over inventory items.", section = inventoryStyleSection, position = 0)
    default DisplayMode displayMode() { return DisplayMode.ITEM_FILL; }

    @Alpha
    @ConfigItem(keyName = "cooldownOverlayColor", name = "Cooldown tint", description = "Color used for inventory item cooldown fills/circles.", section = inventoryStyleSection, position = 1)
    default Color cooldownOverlayColor() { return new Color(0, 0, 0, 140); }

    @ConfigItem(keyName = "showCooldownTimer", name = "Show timer text", description = "Draw remaining seconds over inventory items on cooldown.", section = inventoryStyleSection, position = 2)
    default boolean showCooldownTimer() { return true; }

    @ConfigItem(keyName = "readyStyle", name = "Ready animation", description = "Single flash: one fade-out. Pulse: repeating. Solid: steady. None: no ready highlight.", section = inventoryStyleSection, position = 3)
    default ReadyStyle readyStyle() { return ReadyStyle.SINGLE_FLASH; }

    @ConfigItem(keyName = "borderStyle", name = "Ready border", description = "Outline style around inventory items whose cooldown just finished.", section = inventoryStyleSection, position = 4)
    default BorderStyle borderStyle() { return BorderStyle.ITEM_OUTLINE; }

    @ConfigItem(keyName = "animationMode", name = "Ready duration", description = "Timed: highlight for the duration below after the cooldown ends. Permanent: until the item is used again.", section = inventoryStyleSection, position = 5)
    default AnimationMode animationMode() { return AnimationMode.TIMED; }

    @Range(min = 1, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "readyDurationSeconds", name = "Timed duration", description = "How long the ready highlight lasts when Ready duration is Timed.", section = inventoryStyleSection, position = 6)
    default int readyDurationSeconds() { return 4; }

    @Alpha
    @ConfigItem(keyName = "readyColor", name = "Ready color", description = "Color used for inventory item ready flash/outline.", section = inventoryStyleSection, position = 7)
    default Color readyColor() { return new Color(56, 224, 130, 210); }

    @ConfigItem(keyName = "repotProtectionEnabled", name = "Re-dose protection", description = "Deprioritize left-click Drink/Invigorate/Apply/Crush on items still on cooldown.", section = inventoryStyleSection, position = 8)
    default boolean repotProtectionEnabled() { return true; }

    // =========================================================================
    // 3. Warning: Health & Damage
    // =========================================================================

    @ConfigItem(keyName = "lowHealthEnabled", name = "Enable", description = "Track low HP warning.", section = warnLowHealthSection, position = 0)
    default boolean lowHealthEnabled() { return true; }

    @Range(min = 5, max = 95)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "lowHealthThreshold", name = "Threshold", description = "HP percentage below which low-health activates.", section = warnLowHealthSection, position = 1)
    default int lowHealthThreshold() { return 35; }

    @Alpha
    @ConfigItem(keyName = "lowHealthColor", name = "Color", description = "Color of the low-health screen vignette.", section = warnLowHealthSection, position = 2)
    default Color lowHealthColor() { return new Color(195, 22, 26, 210); }

    @ConfigItem(keyName = "lowHealthGlow", name = "Screen glow", description = "Show screen-edge glow when HP is low.", section = warnLowHealthSection, position = 3)
    default boolean lowHealthGlow() { return true; }

    @ConfigItem(keyName = "lowHealthShower", name = "Shower icon", description = "Shower Hitpoints icon when HP is low.", section = warnLowHealthSection, position = 4)
    default boolean lowHealthShower() { return true; }

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "healthPulseStrength", name = "Heartbeat pulse", description = "Strength of the low-health heartbeat pulse.", section = warnLowHealthSection, position = 5)
    default int healthPulseStrength() { return 60; }

    @ConfigItem(keyName = "damageFeedbackEnabled", name = "Enable", description = "Flash and/or shower hitsplat when taking damage.", section = warnDamageSection, position = 10)
    default boolean damageFeedbackEnabled() { return true; }

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "damageFeedbackStrength", name = "Strength", description = "Strength of the damage flash.", section = warnDamageSection, position = 11)
    default int damageFeedbackStrength() { return 65; }

    @Range(min = 100, max = 2000)
    @Units(Units.MILLISECONDS)
    @ConfigItem(keyName = "damageFeedbackDuration", name = "Duration", description = "Duration of the damage flash decay.", section = warnDamageSection, position = 12)
    default int damageFeedbackDuration() { return 600; }

    @Alpha
    @ConfigItem(keyName = "damageFlashColor", name = "Color", description = "Color used for damage flashes.", section = warnDamageSection, position = 13)
    default Color damageFlashColor() { return new Color(225, 35, 35, 200); }

    @ConfigItem(keyName = "damageGlow", name = "Screen glow", description = "Show screen-edge flash when taking damage.", section = warnDamageSection, position = 14)
    default boolean damageGlow() { return true; }

    @ConfigItem(keyName = "damageShower", name = "Shower icon", description = "Shower red hitsplat icon when taking damage.", section = warnDamageSection, position = 15)
    default boolean damageShower() { return false; }

    // =========================================================================
    // 4. Warning: Low Prayer
    // =========================================================================

    @ConfigItem(keyName = "lowPrayerEnabled", name = "Enable", description = "Track low prayer warning.", section = warnPrayerSection, position = 0)
    default boolean lowPrayerEnabled() { return true; }

    @ConfigItem(keyName = "prayerActiveOnly", name = "Only when prayers active", description = "Only trigger when at least one prayer is active.", section = warnPrayerSection, position = 1)
    default boolean prayerActiveOnly() { return false; }

    @Range(min = 5, max = 95)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "lowPrayerThreshold", name = "Threshold", description = "Prayer percentage below which the warning activates.", section = warnPrayerSection, position = 2)
    default int lowPrayerThreshold() { return 25; }

    @Alpha
    @ConfigItem(keyName = "lowPrayerColor", name = "Color", description = "Color for low prayer vignette and inventory glow.", section = warnPrayerSection, position = 3)
    default Color lowPrayerColor() { return new Color(52, 158, 224, 200); }

    @ConfigItem(keyName = "lowPrayerGlow", name = "Screen glow", description = "Show screen-edge glow when prayer is low.", section = warnPrayerSection, position = 4)
    default boolean lowPrayerGlow() { return true; }

    @ConfigItem(keyName = "lowPrayerShower", name = "Shower icon", description = "Shower Prayer skill icon when prayer is low.", section = warnPrayerSection, position = 5)
    default boolean lowPrayerShower() { return true; }

    @ConfigItem(keyName = "lowPrayerInventoryGlow", name = "Glow inventory restores", description = "Highlight prayer-restoring potions in your inventory when prayer is low.", section = warnPrayerSection, position = 6)
    default boolean lowPrayerInventoryGlow() { return true; }

    // =========================================================================
    // 5. Warning: Poison, Venom, Disease & Freeze
    // =========================================================================

    @ConfigItem(keyName = "poisonEnabled", name = "Enable", description = "Track poison status.", section = warnPoisonSection, position = 0)
    default boolean poisonEnabled() { return true; }

    @Alpha
    @ConfigItem(keyName = "poisonColor", name = "Color", description = "Color for poison vignette.", section = warnPoisonSection, position = 1)
    default Color poisonColor() { return new Color(54, 178, 62, 195); }

    @ConfigItem(keyName = "poisonGlow", name = "Screen glow", description = "Show screen-edge glow when poisoned.", section = warnPoisonSection, position = 2)
    default boolean poisonGlow() { return true; }

    @ConfigItem(keyName = "poisonShower", name = "Shower icon", description = "Shower green poison hitsplat icon when poisoned.", section = warnPoisonSection, position = 3)
    default boolean poisonShower() { return true; }

    @ConfigItem(keyName = "venomEnabled", name = "Enable", description = "Track venom status.", section = warnVenomSection, position = 10)
    default boolean venomEnabled() { return true; }

    @Alpha
    @ConfigItem(keyName = "venomColor", name = "Color", description = "Color for venom vignette.", section = warnVenomSection, position = 11)
    default Color venomColor() { return new Color(24, 128, 68, 210); }

    @ConfigItem(keyName = "venomGlow", name = "Screen glow", description = "Show screen-edge glow when envenomed.", section = warnVenomSection, position = 12)
    default boolean venomGlow() { return true; }

    @ConfigItem(keyName = "venomShower", name = "Shower icon", description = "Shower anti-venom icon when envenomed.", section = warnVenomSection, position = 13)
    default boolean venomShower() { return true; }

    @ConfigItem(keyName = "diseaseEnabled", name = "Enable", description = "Track disease status.", section = warnDiseaseSection, position = 20)
    default boolean diseaseEnabled() { return true; }

    @Alpha
    @ConfigItem(keyName = "diseaseColor", name = "Color", description = "Color for disease vignette.", section = warnDiseaseSection, position = 21)
    default Color diseaseColor() { return new Color(186, 152, 42, 195); }

    @ConfigItem(keyName = "diseaseGlow", name = "Screen glow", description = "Show screen-edge glow when diseased.", section = warnDiseaseSection, position = 22)
    default boolean diseaseGlow() { return true; }

    @ConfigItem(keyName = "diseaseShower", name = "Shower icon", description = "Shower disease icon when diseased.", section = warnDiseaseSection, position = 23)
    default boolean diseaseShower() { return true; }

    @ConfigItem(keyName = "freezeEnabled", name = "Enable", description = "Track freeze/entangle status.", section = warnFreezeSection, position = 30)
    default boolean freezeEnabled() { return true; }

    @Alpha
    @ConfigItem(keyName = "freezeColor", name = "Color", description = "Color for freeze vignette.", section = warnFreezeSection, position = 31)
    default Color freezeColor() { return new Color(110, 205, 248, 205); }

    @ConfigItem(keyName = "freezeGlow", name = "Screen glow", description = "Show screen-edge glow when frozen.", section = warnFreezeSection, position = 32)
    default boolean freezeGlow() { return true; }

    @ConfigItem(keyName = "freezeShower", name = "Shower icon", description = "Shower Ice Barrage icon when frozen.", section = warnFreezeSection, position = 33)
    default boolean freezeShower() { return true; }

    @Range(min = 0, max = 30)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "freezeMaxDisplaySeconds", name = "Max display duration", description = "Hide freeze overlay after this many seconds (0 = show for full freeze duration).", section = warnFreezeSection, position = 34)
    default int freezeMaxDisplaySeconds() { return 0; }

    // =========================================================================
    // 6. Warning: Idle, Spec & Low Run
    // =========================================================================

    @ConfigItem(keyName = "idleEnabled", name = "Enable (Skilling & Combat)", description = "Works for skilling and combat unless 'Only warn idle in combat' is checked.", section = warnIdleSection, position = 0)
    default boolean idleEnabled() { return true; }

    @ConfigItem(keyName = "idleOnlyInCombat", name = "Only in combat", description = "Restrict idle warnings to combat only.", section = warnIdleSection, position = 1)
    default boolean idleOnlyInCombat() { return false; }

    @Range(min = 1, max = 299)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "idleStartSeconds", name = "Ramp start", description = "Seconds after stopping skilling/combat before the idle glow starts ramping.", section = warnIdleSection, position = 2)
    default int idleStartSeconds() { return 5; }

    @Range(min = 2, max = 300)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "idleCriticalSeconds", name = "Critical time", description = "Seconds after stopping skilling/combat to reach full idle glow.", section = warnIdleSection, position = 3)
    default int idleCriticalSeconds() { return 8; }

    @ConfigItem(keyName = "idleAutoHide", name = "Auto-hide", description = "Fade out idle warning after remaining idle past critical time.", section = warnIdleSection, position = 4)
    default boolean idleAutoHide() { return false; }

    @Range(min = 1, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "idleAutoHideSeconds", name = "Auto-hide delay", description = "Seconds after critical time before auto-hiding.", section = warnIdleSection, position = 5)
    default int idleAutoHideSeconds() { return 5; }

    @ConfigItem(keyName = "idleNotification", name = "Notification", description = "RuneLite notification (sound, tray, flash - configurable) sent once at critical idle time.", section = warnIdleSection, position = 6)
    default Notification idleNotification() { return Notification.OFF; }

    @Alpha
    @ConfigItem(keyName = "idleColor", name = "Color", description = "Color of the idle screen-edge glow.", section = warnIdleSection, position = 7)
    default Color idleColor() { return new Color(235, 135, 35, 210); }

    @ConfigItem(keyName = "idleGlow", name = "Screen glow", description = "Show screen-edge glow when idle.", section = warnIdleSection, position = 8)
    default boolean idleGlow() { return true; }

    @ConfigItem(keyName = "idleShower", name = "Shower icon", description = "Shower Lunar Dream icon when idle.", section = warnIdleSection, position = 9)
    default boolean idleShower() { return true; }

    @ConfigItem(keyName = "specEnabled", name = "Enable", description = "Track special attack energy threshold.", section = warnSpecSection, position = 15)
    default boolean specEnabled() { return false; }

    @ConfigItem(keyName = "specOnlyInCombat", name = "Only in combat", description = "Only trigger while in combat.", section = warnSpecSection, position = 16)
    default boolean specOnlyInCombat() { return true; }

    @Range(min = 10, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "specThresholdPercent", name = "Threshold", description = "Minimum special attack percentage to trigger.", section = warnSpecSection, position = 17)
    default int specThresholdPercent() { return 100; }

    @Alpha
    @ConfigItem(keyName = "specColor", name = "Color", description = "Color of the spec ready glow.", section = warnSpecSection, position = 18)
    default Color specColor() { return new Color(45, 212, 191, 190); }

    @ConfigItem(keyName = "specGlow", name = "Screen glow", description = "Show screen-edge glow when spec is ready.", section = warnSpecSection, position = 19)
    default boolean specGlow() { return true; }

    @ConfigItem(keyName = "specShower", name = "Shower icon", description = "Shower crossed swords icon when spec is ready.", section = warnSpecSection, position = 20)
    default boolean specShower() { return true; }

    @ConfigItem(keyName = "specOrbGlow", name = "Glow spec orb", description = "Glow the minimap Special Attack orb when spec is ready.", section = warnSpecSection, position = 21)
    default boolean specOrbGlow() { return true; }

    @ConfigItem(keyName = "lowRunEnabled", name = "Enable", description = "Track low run energy warning.", section = warnLowRunSection, position = 30)
    default boolean lowRunEnabled() { return false; }

    @ConfigItem(keyName = "lowRunOnlyInCombat", name = "Only in combat", description = "Only trigger while in combat.", section = warnLowRunSection, position = 31)
    default boolean lowRunOnlyInCombat() { return true; }

    @Range(min = 5, max = 95)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "lowRunThreshold", name = "Threshold", description = "Run energy percentage below which warning activates.", section = warnLowRunSection, position = 32)
    default int lowRunThreshold() { return 25; }

    @Alpha
    @ConfigItem(keyName = "lowRunColor", name = "Color", description = "Color of the low run energy glow.", section = warnLowRunSection, position = 33)
    default Color lowRunColor() { return new Color(234, 179, 8, 195); }

    @ConfigItem(keyName = "lowRunGlow", name = "Screen glow", description = "Show screen-edge glow when run energy is low.", section = warnLowRunSection, position = 34)
    default boolean lowRunGlow() { return true; }

    @ConfigItem(keyName = "lowRunShower", name = "Shower icon", description = "Shower Run orb icon when run energy is low.", section = warnLowRunSection, position = 35)
    default boolean lowRunShower() { return true; }

    // =========================================================================
    // 7. Warning: Heart, Buffs, Unpotted & Food
    // =========================================================================

    @ConfigItem(keyName = "heartsEnabled", name = "Inventory overlay", description = "Show cooldown overlays on Imbued/Saturated Hearts.", section = warnHeartSection, position = 0)
    default boolean heartsEnabled() { return true; }

    @ConfigItem(keyName = "heartScreenReminder", name = "Screen glow", description = "Show screen-edge glow when Heart is ready.", section = warnHeartSection, position = 1)
    default boolean heartScreenReminder() { return true; }

    @ConfigItem(keyName = "heartShower", name = "Shower icon", description = "Shower Heart icon when Heart is ready.", section = warnHeartSection, position = 2)
    default boolean heartShower() { return true; }

    @Alpha
    @ConfigItem(keyName = "heartReminderColor", name = "Color", description = "Color when Imbued/Saturated Heart is ready.", section = warnHeartSection, position = 3)
    default Color heartReminderColor() { return new Color(185, 65, 225, 200); }

    @ConfigItem(keyName = "potionCooldownsEnabled", name = "Potions: Track cooldowns", description = "Show inventory overlays on Surge potions and cooldown consumables.", section = warnPotionsBuffsSection, position = 10)
    default boolean potionCooldownsEnabled() { return true; }

    @ConfigItem(keyName = "potionEffectsEnabled", name = "Potions: Track effects", description = "Show inventory overlays on antifire, stamina, divine, antivenom, etc.", section = warnPotionsBuffsSection, position = 11)
    default boolean potionEffectsEnabled() { return true; }

    @ConfigItem(keyName = "raidPotionsEnabled", name = "Potions: Track raid consumables", description = "Show inventory overlays on Overloads, Salts, Adrenaline, Silk Dressing, Scarabs.", section = warnPotionsBuffsSection, position = 12)
    default boolean raidPotionsEnabled() { return true; }

    @ConfigItem(keyName = "boostPotionsEnabled", name = "Potions: Track boost potions", description = "Estimate re-dose timing on combat stat boost potions (Preserve-aware).", section = warnPotionsBuffsSection, position = 13)
    default boolean boostPotionsEnabled() { return true; }

    @Range(min = 0, max = 10)
    @ConfigItem(keyName = "boostRedoseThreshold", name = "Potions: Boost re-dose threshold", description = "Levels below max boost at which a boost potion is ready to re-dose.", section = warnPotionsBuffsSection, position = 14)
    default int boostRedoseThreshold() { return 3; }

    @ConfigItem(keyName = "buffScreenReminder", name = "Potions: Screen glow", description = "Show screen-edge glow when an inventory buff/potion expires.", section = warnPotionsBuffsSection, position = 15)
    default boolean buffScreenReminder() { return true; }

    @ConfigItem(keyName = "buffShower", name = "Potions: Shower icon", description = "Shower the expired potion's icon when ready.", section = warnPotionsBuffsSection, position = 16)
    default boolean buffShower() { return true; }

    @Alpha
    @ConfigItem(keyName = "buffReminderColor", name = "Potions: Color", description = "Color when an inventory buff/potion expires.", section = warnPotionsBuffsSection, position = 17)
    default Color buffReminderColor() { return new Color(65, 215, 145, 200); }

    @ConfigItem(keyName = "buffOnlyRemindAfterUse", name = "Heart/Buffs: Only remind after use", description = "Only remind when a Heart or buff/utility potion expires after you use it (prevents alerts on combat start for unused items in inventory).", section = warnPotionsBuffsSection, position = 18)
    default boolean buffOnlyRemindAfterUse() { return true; }

    @ConfigItem(keyName = "buffOnlyInCombat", name = "Heart/Buffs: Only remind in combat", description = "Only show Heart and potion screen reminders while in combat.", section = warnPotionsBuffsSection, position = 19)
    default boolean buffOnlyInCombat() { return true; }

    @ConfigItem(keyName = "boostPreRemind", name = "Boosts: Pre-remind before expiry", description = "Remind before boost potions drop below threshold (off = remind on expiry only).", section = warnPotionsBuffsSection, position = 20)
    default boolean boostPreRemind() { return false; }

    @Range(min = 0, max = 30)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "buffExpiryLeadSeconds", name = "Boosts: Pre-expiry lead", description = "Seconds BEFORE boost potion drops below threshold when pre-remind is enabled.", section = warnPotionsBuffsSection, position = 21)
    default int buffExpiryLeadSeconds() { return 3; }

    @ConfigItem(keyName = "unpottedEnabled", name = "Enable", description = "Warn when gaining combat XP without a stat boost (ignores alching; supports cannon).", section = warnUnpottedSection, position = 20)
    default boolean unpottedEnabled() { return true; }

    @Range(min = 0, max = 10)
    @ConfigItem(keyName = "unpottedThreshold", name = "Boost threshold", description = "Trigger if boosted level is at or below real level + this value.", section = warnUnpottedSection, position = 21)
    default int unpottedThreshold() { return 1; }

    @Range(min = 3, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "unpottedAlertDurationSeconds", name = "Alert duration", description = "How long unpotted alert stays active unless you pot up.", section = warnUnpottedSection, position = 22)
    default int unpottedAlertDurationSeconds() { return 10; }

    @Alpha
    @ConfigItem(keyName = "unpottedColor", name = "Color", description = "Color of the unpotted-in-combat glow.", section = warnUnpottedSection, position = 23)
    default Color unpottedColor() { return new Color(249, 115, 22, 200); }

    @ConfigItem(keyName = "unpottedGlow", name = "Screen glow", description = "Show screen-edge glow when unpotted in combat.", section = warnUnpottedSection, position = 24)
    default boolean unpottedGlow() { return true; }

    @ConfigItem(keyName = "unpottedShower", name = "Shower icon", description = "Shower Stats (three bars) icon when unpotted in combat.", section = warnUnpottedSection, position = 25)
    default boolean unpottedShower() { return true; }

    @ConfigItem(keyName = "foodCooldownsEnabled", name = "Food: Combo-eat & Hunter meat timers", description = "Show 3-tick eat/combo-eat locks and 7-tick Hunter meat delayed heal.", section = warnPotionsBuffsSection, position = 30)
    default boolean foodCooldownsEnabled() { return true; }

    // =========================================================================
    // 8. Warning: Combat Spells (Shared + Per-Spell)
    // =========================================================================

    @ConfigItem(keyName = "spellsEnabled", name = "Master enable", description = "Master toggle for combat spell cooldowns, glows, and reminders.", section = sharedSpellsSection, position = 0)
    default boolean spellsEnabled() { return true; }

    @ConfigItem(keyName = "spellOnlyInCombat", name = "Only remind in combat", description = "Only show expired spell/buff reminders while in combat.", section = sharedSpellsSection, position = 1)
    default boolean spellOnlyInCombat() { return true; }

    @ConfigItem(keyName = "spellOnlyRemindAfterCast", name = "Only remind after cast", description = "Only remind when a spell expires after you cast it (prevents alerts on combat start for uncast spells).", section = sharedSpellsSection, position = 2)
    default boolean spellOnlyRemindAfterCast() { return true; }

    @ConfigItem(keyName = "thrallRemindOnCombatStart", name = "Remind on combat start", description = "Remind to summon a Thrall as soon as combat starts, even before your first cast.", section = spellThrallSection, position = 2)
    default boolean thrallRemindOnCombatStart() { return false; }

    @ConfigItem(keyName = "spellScreenGlow", name = "Shared screen glow", description = "Default screen-edge glow for expired spells.", section = sharedSpellsSection, position = 3)
    default boolean spellScreenGlow() { return true; }

    @ConfigItem(keyName = "spellShowerIcons", name = "Shared shower icons", description = "Default icon shower for expired spells.", section = sharedSpellsSection, position = 4)
    default boolean spellShowerIcons() { return true; }

    @ConfigItem(keyName = "spellGlowInSpellbook", name = "Shared spellbook glow", description = "Default glow on expired spell widgets in the spellbook.", section = sharedSpellsSection, position = 5)
    default boolean spellGlowInSpellbook() { return true; }

    @ConfigItem(keyName = "spellbookTabGlow", name = "Glow Magic tab icon", description = "Glow the Magic spellbook book icon when any tracked spell is ready.", section = sharedSpellsSection, position = 6)
    default boolean spellbookTabGlow() { return true; }

    @ConfigItem(keyName = "spellBorderStyle", name = "Spellbook glow style", description = "Glow style for ready spells, the Magic tab and the spec orb (independent of the inventory ready border).", section = sharedSpellsSection, position = 7)
    default BorderStyle spellBorderStyle() { return BorderStyle.SOFT_GLOW; }

    @ConfigItem(keyName = "spellCooldownDisplayMode", name = "Spell cooldown style", description = "How remaining recast time is drawn over spell widgets.", section = sharedSpellsSection, position = 8)
    default SpellCooldownDisplayMode spellCooldownDisplayMode() { return SpellCooldownDisplayMode.FILL_AND_TEXT; }

    @Alpha
    @ConfigItem(keyName = "spellCooldownOverlayColor", name = "Spell cooldown tint", description = "Translucent tint drawn over spells while on cooldown.", section = sharedSpellsSection, position = 9)
    default Color spellCooldownOverlayColor() { return new Color(0, 0, 0, 145); }

    // Per-spell controls
    @ConfigItem(keyName = "spellThrallEnabled", name = "Enable", description = "Track Resurrect Thrall.", section = spellThrallSection, position = 10)
    default boolean spellThrallEnabled() { return true; }

    @ConfigItem(keyName = "thrallDurationTier", name = "CA duration", description = "Thrall duration doubles once the Master Combat Achievement tier is complete. Auto-detect reads it from the game.", section = spellThrallSection, position = 11)
    default ThrallDurationTier thrallDurationTier() { return ThrallDurationTier.AUTO; }

    @Alpha
    @ConfigItem(keyName = "spellThrallColor", name = "Color", description = "Glow color for Resurrect Thrall.", section = spellThrallSection, position = 12)
    default Color spellThrallColor() { return new Color(138, 92, 246, 210); }

    @ConfigItem(keyName = "spellThrallGlow", name = "Screen glow", description = "Show screen glow for Thrall.", section = spellThrallSection, position = 13)
    default boolean spellThrallGlow() { return true; }

    @ConfigItem(keyName = "spellThrallShower", name = "Shower icon", description = "Shower Thrall icon.", section = spellThrallSection, position = 14)
    default boolean spellThrallShower() { return true; }

    @ConfigItem(keyName = "spellThrallSpellbook", name = "Spellbook glow", description = "Glow Thrall in spellbook.", section = spellThrallSection, position = 15)
    default boolean spellThrallSpellbook() { return true; }

    @ConfigItem(keyName = "spellDeathChargeEnabled", name = "Enable", description = "Track Death Charge recast cooldown.", section = spellDeathChargeSection, position = 20)
    default boolean spellDeathChargeEnabled() { return true; }

    @Alpha
    @ConfigItem(keyName = "spellDeathChargeColor", name = "Color", description = "Glow color for Death Charge.", section = spellDeathChargeSection, position = 21)
    default Color spellDeathChargeColor() { return new Color(220, 60, 90, 210); }

    @ConfigItem(keyName = "spellDeathChargeGlow", name = "Screen glow", description = "Show screen glow for Death Charge.", section = spellDeathChargeSection, position = 22)
    default boolean spellDeathChargeGlow() { return true; }

    @ConfigItem(keyName = "spellDeathChargeShower", name = "Shower icon", description = "Shower Death Charge icon.", section = spellDeathChargeSection, position = 23)
    default boolean spellDeathChargeShower() { return true; }

    @ConfigItem(keyName = "spellDeathChargeSpellbook", name = "Spellbook glow", description = "Glow Death Charge in spellbook.", section = spellDeathChargeSection, position = 24)
    default boolean spellDeathChargeSpellbook() { return true; }

    @ConfigItem(keyName = "spellMarkOfDarknessEnabled", name = "Enable", description = "Track Mark of Darkness (Purging Staff aware).", section = spellMarkOfDarknessSection, position = 30)
    default boolean spellMarkOfDarknessEnabled() { return true; }

    @Alpha
    @ConfigItem(keyName = "spellMarkOfDarknessColor", name = "Color", description = "Glow color for Mark of Darkness.", section = spellMarkOfDarknessSection, position = 31)
    default Color spellMarkOfDarknessColor() { return new Color(80, 175, 255, 210); }

    @ConfigItem(keyName = "spellMarkOfDarknessGlow", name = "Screen glow", description = "Show screen glow for Mark of Darkness.", section = spellMarkOfDarknessSection, position = 32)
    default boolean spellMarkOfDarknessGlow() { return true; }

    @ConfigItem(keyName = "spellMarkOfDarknessShower", name = "Shower icon", description = "Shower Mark of Darkness icon.", section = spellMarkOfDarknessSection, position = 33)
    default boolean spellMarkOfDarknessShower() { return true; }

    @ConfigItem(keyName = "spellMarkOfDarknessSpellbook", name = "Spellbook glow", description = "Glow Mark of Darkness in spellbook.", section = spellMarkOfDarknessSection, position = 34)
    default boolean spellMarkOfDarknessSpellbook() { return true; }

    @ConfigItem(keyName = "spellWardOfArceuusEnabled", name = "Enable", description = "Track Ward of Arceuus.", section = spellWardOfArceuusSection, position = 40)
    default boolean spellWardOfArceuusEnabled() { return false; }

    @Alpha
    @ConfigItem(keyName = "spellWardOfArceuusColor", name = "Color", description = "Glow color for Ward of Arceuus.", section = spellWardOfArceuusSection, position = 41)
    default Color spellWardOfArceuusColor() { return new Color(56, 200, 160, 210); }

    @ConfigItem(keyName = "spellWardOfArceuusGlow", name = "Screen glow", description = "Show screen glow for Ward of Arceuus.", section = spellWardOfArceuusSection, position = 42)
    default boolean spellWardOfArceuusGlow() { return true; }

    @ConfigItem(keyName = "spellWardOfArceuusShower", name = "Shower icon", description = "Shower Ward of Arceuus icon.", section = spellWardOfArceuusSection, position = 43)
    default boolean spellWardOfArceuusShower() { return true; }

    @ConfigItem(keyName = "spellWardOfArceuusSpellbook", name = "Spellbook glow", description = "Glow Ward of Arceuus in spellbook.", section = spellWardOfArceuusSection, position = 44)
    default boolean spellWardOfArceuusSpellbook() { return true; }

    @ConfigItem(keyName = "spellShadowVeilEnabled", name = "Enable", description = "Track Shadow Veil.", section = spellShadowVeilSection, position = 50)
    default boolean spellShadowVeilEnabled() { return false; }

    @Alpha
    @ConfigItem(keyName = "spellShadowVeilColor", name = "Color", description = "Glow color for Shadow Veil.", section = spellShadowVeilSection, position = 51)
    default Color spellShadowVeilColor() { return new Color(140, 120, 190, 200); }

    @ConfigItem(keyName = "spellShadowVeilGlow", name = "Screen glow", description = "Show screen glow for Shadow Veil.", section = spellShadowVeilSection, position = 52)
    default boolean spellShadowVeilGlow() { return true; }

    @ConfigItem(keyName = "spellShadowVeilShower", name = "Shower icon", description = "Shower Shadow Veil icon.", section = spellShadowVeilSection, position = 53)
    default boolean spellShadowVeilShower() { return true; }

    @ConfigItem(keyName = "spellShadowVeilSpellbook", name = "Spellbook glow", description = "Glow Shadow Veil in spellbook.", section = spellShadowVeilSection, position = 54)
    default boolean spellShadowVeilSpellbook() { return true; }

    @ConfigItem(keyName = "spellCorruptionEnabled", name = "Enable", description = "Track Lesser/Greater Corruption recast cooldown.", section = spellCorruptionSection, position = 55)
    default boolean spellCorruptionEnabled() { return false; }

    @Alpha
    @ConfigItem(keyName = "spellCorruptionColor", name = "Color", description = "Glow color for Corruption.", section = spellCorruptionSection, position = 56)
    default Color spellCorruptionColor() { return new Color(180, 70, 220, 200); }

    @ConfigItem(keyName = "spellCorruptionGlow", name = "Screen glow", description = "Show screen glow for Corruption.", section = spellCorruptionSection, position = 57)
    default boolean spellCorruptionGlow() { return true; }

    @ConfigItem(keyName = "spellCorruptionShower", name = "Shower icon", description = "Shower Corruption icon.", section = spellCorruptionSection, position = 58)
    default boolean spellCorruptionShower() { return true; }

    @ConfigItem(keyName = "spellCorruptionSpellbook", name = "Spellbook glow", description = "Glow Corruption in spellbook.", section = spellCorruptionSection, position = 59)
    default boolean spellCorruptionSpellbook() { return true; }

    @ConfigItem(keyName = "spellVengeanceEnabled", name = "Enable", description = "Track Vengeance recast cooldown.", section = spellVengeanceSection, position = 60)
    default boolean spellVengeanceEnabled() { return true; }

    @Alpha
    @ConfigItem(keyName = "spellVengeanceColor", name = "Color", description = "Glow color for Vengeance.", section = spellVengeanceSection, position = 61)
    default Color spellVengeanceColor() { return new Color(245, 158, 11, 210); }

    @ConfigItem(keyName = "spellVengeanceGlow", name = "Screen glow", description = "Show screen glow for Vengeance.", section = spellVengeanceSection, position = 62)
    default boolean spellVengeanceGlow() { return true; }

    @ConfigItem(keyName = "spellVengeanceShower", name = "Shower icon", description = "Shower Vengeance icon.", section = spellVengeanceSection, position = 63)
    default boolean spellVengeanceShower() { return true; }

    @ConfigItem(keyName = "spellVengeanceSpellbook", name = "Spellbook glow", description = "Glow Vengeance in spellbook.", section = spellVengeanceSection, position = 64)
    default boolean spellVengeanceSpellbook() { return true; }

    @ConfigItem(keyName = "spellVengeanceOtherEnabled", name = "Enable", description = "Track Vengeance Other recast cooldown.", section = spellVengeanceOtherSection, position = 70)
    default boolean spellVengeanceOtherEnabled() { return false; }

    @Alpha
    @ConfigItem(keyName = "spellVengeanceOtherColor", name = "Color", description = "Glow color for Vengeance Other.", section = spellVengeanceOtherSection, position = 71)
    default Color spellVengeanceOtherColor() { return new Color(235, 190, 60, 200); }

    @ConfigItem(keyName = "spellVengeanceOtherGlow", name = "Screen glow", description = "Show screen glow for Vengeance Other.", section = spellVengeanceOtherSection, position = 72)
    default boolean spellVengeanceOtherGlow() { return true; }

    @ConfigItem(keyName = "spellVengeanceOtherShower", name = "Shower icon", description = "Shower Vengeance Other icon.", section = spellVengeanceOtherSection, position = 73)
    default boolean spellVengeanceOtherShower() { return true; }

    @ConfigItem(keyName = "spellVengeanceOtherSpellbook", name = "Spellbook glow", description = "Glow Vengeance Other in spellbook.", section = spellVengeanceOtherSection, position = 74)
    default boolean spellVengeanceOtherSpellbook() { return true; }

    @ConfigItem(keyName = "spellChargeEnabled", name = "Enable", description = "Track Charge (God Spells) expiry.", section = spellChargeSection, position = 80)
    default boolean spellChargeEnabled() { return false; }

    @Alpha
    @ConfigItem(keyName = "spellChargeColor", name = "Color", description = "Glow color for Charge.", section = spellChargeSection, position = 81)
    default Color spellChargeColor() { return new Color(250, 204, 21, 200); }

    @ConfigItem(keyName = "spellChargeGlow", name = "Screen glow", description = "Show screen glow for Charge.", section = spellChargeSection, position = 82)
    default boolean spellChargeGlow() { return true; }

    @ConfigItem(keyName = "spellChargeShower", name = "Shower icon", description = "Shower Charge icon.", section = spellChargeSection, position = 83)
    default boolean spellChargeShower() { return true; }

    @ConfigItem(keyName = "spellChargeSpellbook", name = "Spellbook glow", description = "Glow Charge in spellbook.", section = spellChargeSection, position = 84)
    default boolean spellChargeSpellbook() { return true; }

    // =========================================================================
    // 9. Debug Preview
    // =========================================================================

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "previewIntensity", name = "Preview intensity", description = "Severity used for checked debug overlays.", section = debugSection, position = 0)
    default int previewIntensity() { return 75; }

    @ConfigItem(keyName = "debugSimulateCombat", name = "Simulate in-combat", description = "Pretend you are in combat so you can safely test live reminders at a bank.", section = debugSection, position = 1)
    default boolean debugSimulateCombat() { return false; }

    @ConfigItem(keyName = "debugTriggerSpellCooldowns", name = "Test spell cooldowns (toggle)", description = "Start 9-18s test cooldowns on all enabled spells without spending runes.", section = debugSection, position = 2)
    default boolean debugTriggerSpellCooldowns() { return false; }

    @ConfigItem(keyName = "debugTriggerPotionCooldowns", name = "Test heart & potion cooldowns (toggle)", description = "Start 15s test cooldowns on hearts and potions without drinking doses.", section = debugSection, position = 3)
    default boolean debugTriggerPotionCooldowns() { return false; }

    @ConfigItem(keyName = "debugPreviewInventory", name = "Debug: Preview on Inventory Slots", description = "Looping countdown & ready outline on your first 4 inventory slots.", section = debugSection, position = 4)
    default boolean debugPreviewInventory() { return false; }

    @ConfigItem(keyName = "debugPreviewSpellGrid", name = "Debug: Preview Spell Grid", description = "Compact floating spell preview panel with looping timers & glows.", section = debugSection, position = 5)
    default boolean debugPreviewSpellGrid() { return false; }

    @ConfigItem(keyName = "debugLowHealth", name = "Debug preview", description = "Preview low health vignette, blood splatter & Hitpoints icon.", section = warnLowHealthSection, position = 10)
    default boolean debugLowHealth() { return false; }

    @ConfigItem(keyName = "debugLowPrayer", name = "Debug preview", description = "Preview low prayer vignette, Prayer icon & inventory restore glow.", section = warnPrayerSection, position = 11)
    default boolean debugLowPrayer() { return false; }

    @ConfigItem(keyName = "debugPoison", name = "Debug preview", description = "Preview poison vignette & green hitsplat icon.", section = warnPoisonSection, position = 12)
    default boolean debugPoison() { return false; }

    @ConfigItem(keyName = "debugVenom", name = "Debug preview", description = "Preview venom vignette & anti-venom icon.", section = warnVenomSection, position = 14)
    default boolean debugVenom() { return false; }

    @ConfigItem(keyName = "debugDisease", name = "Debug preview", description = "Preview disease vignette & disease hitsplat icon.", section = warnDiseaseSection, position = 14)
    default boolean debugDisease() { return false; }

    @ConfigItem(keyName = "debugFreeze", name = "Debug preview", description = "Preview freeze vignette & Ice Barrage icon.", section = warnFreezeSection, position = 15)
    default boolean debugFreeze() { return false; }

    @ConfigItem(keyName = "debugDamageFlash", name = "Debug preview", description = "Preview damage hit flash & red hitsplat icon.", section = warnDamageSection, position = 16)
    default boolean debugDamageFlash() { return false; }

    @ConfigItem(keyName = "debugIdle", name = "Debug preview", description = "Preview idle vignette & Lunar Dream icon.", section = warnIdleSection, position = 17)
    default boolean debugIdle() { return false; }

    @ConfigItem(keyName = "debugSpecReady", name = "Debug preview", description = "Preview spec ready vignette, orb glow & crossed swords icon.", section = warnSpecSection, position = 22)
    default boolean debugSpecReady() { return false; }

    @ConfigItem(keyName = "debugLowRun", name = "Debug preview", description = "Preview low run energy vignette & Run orb icon.", section = warnLowRunSection, position = 19)
    default boolean debugLowRun() { return false; }

    @ConfigItem(keyName = "debugUnpotted", name = "Debug preview", description = "Preview unpotted alert vignette & Stats (three bars) icon.", section = warnUnpottedSection, position = 26)
    default boolean debugUnpotted() { return false; }

    @ConfigItem(keyName = "debugHeartReady", name = "Debug preview", description = "Preview Imbued/Saturated Heart screen glow & icon shower.", section = warnHeartSection, position = 21)
    default boolean debugHeartReady() { return false; }

    @ConfigItem(keyName = "debugBuffExpired", name = "Debug preview", description = "Preview expired potion buff screen glow & icon shower.", section = warnPotionsBuffsSection, position = 22)
    default boolean debugBuffExpired() { return false; }

    @ConfigItem(keyName = "debugSpellThrall", name = "Debug preview", description = "Preview expired Resurrect Thrall.", section = spellThrallSection, position = 25)
    default boolean debugSpellThrall() { return false; }

    @ConfigItem(keyName = "debugSpellDeathCharge", name = "Debug preview", description = "Preview expired Death Charge.", section = spellDeathChargeSection, position = 26)
    default boolean debugSpellDeathCharge() { return false; }

    @ConfigItem(keyName = "debugSpellMarkOfDarkness", name = "Debug preview", description = "Preview expired Mark of Darkness.", section = spellMarkOfDarknessSection, position = 27)
    default boolean debugSpellMarkOfDarkness() { return false; }

    @ConfigItem(keyName = "debugSpellWardOfArceuus", name = "Debug preview", description = "Preview expired Ward of Arceuus.", section = spellWardOfArceuusSection, position = 28)
    default boolean debugSpellWardOfArceuus() { return false; }

    @ConfigItem(keyName = "debugSpellShadowVeil", name = "Debug preview", description = "Preview expired Shadow Veil.", section = spellShadowVeilSection, position = 29)
    default boolean debugSpellShadowVeil() { return false; }

    @ConfigItem(keyName = "debugSpellCorruption", name = "Debug preview", description = "Preview expired Corruption.", section = spellCorruptionSection, position = 30)
    default boolean debugSpellCorruption() { return false; }

    @ConfigItem(keyName = "debugSpellVengeance", name = "Debug preview", description = "Preview expired Vengeance.", section = spellVengeanceSection, position = 31)
    default boolean debugSpellVengeance() { return false; }

    @ConfigItem(keyName = "debugSpellVengeanceOther", name = "Debug preview", description = "Preview expired Vengeance Other.", section = spellVengeanceOtherSection, position = 32)
    default boolean debugSpellVengeanceOther() { return false; }

    @ConfigItem(keyName = "debugSpellCharge", name = "Debug preview", description = "Preview expired Charge.", section = spellChargeSection, position = 34)
    default boolean debugSpellCharge() { return false; }

    // =========================================================================
    // Static Helpers (Safe with RuneLite ConfigInvocationHandler)
    // =========================================================================

    static boolean isSpellEnabled(ImmersiveCombatOverlaysConfig config, CombatSpell spell)
    {
        switch (spell)
        {
            case RESURRECT_THRALL: return config.spellThrallEnabled();
            case DEATH_CHARGE: return config.spellDeathChargeEnabled();
            case MARK_OF_DARKNESS: return config.spellMarkOfDarknessEnabled();
            case WARD_OF_ARCEUUS: return config.spellWardOfArceuusEnabled();
            case SHADOW_VEIL: return config.spellShadowVeilEnabled();
            case CORRUPTION: return config.spellCorruptionEnabled();
            case VENGEANCE: return config.spellVengeanceEnabled();
            case VENGEANCE_OTHER: return config.spellVengeanceOtherEnabled();
            case CHARGE: return config.spellChargeEnabled();
            default: return false;
        }
    }

    static Color getSpellColor(ImmersiveCombatOverlaysConfig config, CombatSpell spell)
    {
        switch (spell)
        {
            case RESURRECT_THRALL: return config.spellThrallColor();
            case DEATH_CHARGE: return config.spellDeathChargeColor();
            case MARK_OF_DARKNESS: return config.spellMarkOfDarknessColor();
            case WARD_OF_ARCEUUS: return config.spellWardOfArceuusColor();
            case SHADOW_VEIL: return config.spellShadowVeilColor();
            case CORRUPTION: return config.spellCorruptionColor();
            case VENGEANCE: return config.spellVengeanceColor();
            case VENGEANCE_OTHER: return config.spellVengeanceOtherColor();
            case CHARGE: return config.spellChargeColor();
            default: return Color.CYAN;
        }
    }

    static boolean isSpellGlowEnabled(ImmersiveCombatOverlaysConfig config, CombatSpell spell)
    {
        if (!config.allowScreenGlows() || !config.spellScreenGlow())
        {
            return false;
        }
        switch (spell)
        {
            case RESURRECT_THRALL: return config.spellThrallGlow();
            case DEATH_CHARGE: return config.spellDeathChargeGlow();
            case MARK_OF_DARKNESS: return config.spellMarkOfDarknessGlow();
            case WARD_OF_ARCEUUS: return config.spellWardOfArceuusGlow();
            case SHADOW_VEIL: return config.spellShadowVeilGlow();
            case CORRUPTION: return config.spellCorruptionGlow();
            case VENGEANCE: return config.spellVengeanceGlow();
            case VENGEANCE_OTHER: return config.spellVengeanceOtherGlow();
            case CHARGE: return config.spellChargeGlow();
            default: return false;
        }
    }

    static boolean isSpellShowerEnabled(ImmersiveCombatOverlaysConfig config, CombatSpell spell)
    {
        if (!config.allowIconShowers() || !config.spellShowerIcons())
        {
            return false;
        }
        switch (spell)
        {
            case RESURRECT_THRALL: return config.spellThrallShower();
            case DEATH_CHARGE: return config.spellDeathChargeShower();
            case MARK_OF_DARKNESS: return config.spellMarkOfDarknessShower();
            case WARD_OF_ARCEUUS: return config.spellWardOfArceuusShower();
            case SHADOW_VEIL: return config.spellShadowVeilShower();
            case CORRUPTION: return config.spellCorruptionShower();
            case VENGEANCE: return config.spellVengeanceShower();
            case VENGEANCE_OTHER: return config.spellVengeanceOtherShower();
            case CHARGE: return config.spellChargeShower();
            default: return false;
        }
    }

    static boolean isSpellbookGlowEnabled(ImmersiveCombatOverlaysConfig config, CombatSpell spell)
    {
        if (!config.allowItemGlows() || !config.spellGlowInSpellbook())
        {
            return false;
        }
        switch (spell)
        {
            case RESURRECT_THRALL: return config.spellThrallSpellbook();
            case DEATH_CHARGE: return config.spellDeathChargeSpellbook();
            case MARK_OF_DARKNESS: return config.spellMarkOfDarknessSpellbook();
            case WARD_OF_ARCEUUS: return config.spellWardOfArceuusSpellbook();
            case SHADOW_VEIL: return config.spellShadowVeilSpellbook();
            case CORRUPTION: return config.spellCorruptionSpellbook();
            case VENGEANCE: return config.spellVengeanceSpellbook();
            case VENGEANCE_OTHER: return config.spellVengeanceOtherSpellbook();
            case CHARGE: return config.spellChargeSpellbook();
            default: return false;
        }
    }

    static boolean isDebugSpellActive(ImmersiveCombatOverlaysConfig config, CombatSpell spell)
    {
        switch (spell)
        {
            case RESURRECT_THRALL: return config.debugSpellThrall();
            case DEATH_CHARGE: return config.debugSpellDeathCharge();
            case MARK_OF_DARKNESS: return config.debugSpellMarkOfDarkness();
            case WARD_OF_ARCEUUS: return config.debugSpellWardOfArceuus();
            case SHADOW_VEIL: return config.debugSpellShadowVeil();
            case CORRUPTION: return config.debugSpellCorruption();
            case VENGEANCE: return config.debugSpellVengeance();
            case VENGEANCE_OTHER: return config.debugSpellVengeanceOther();
            case CHARGE: return config.debugSpellCharge();
            default: return false;
        }
    }
}
