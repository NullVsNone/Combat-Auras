package com.combatauras.config;

import com.combatauras.model.AnimationMode;
import com.combatauras.model.BorderStyle;
import com.combatauras.model.DisplayMode;
import com.combatauras.model.GlowColorMode;
import com.combatauras.model.Preset;
import com.combatauras.model.ReadyStyle;
import com.combatauras.model.ReminderContext;
import com.combatauras.model.ShowerStartLocation;
import com.combatauras.model.SpellCooldownDisplayMode;
import com.combatauras.model.ThrallDurationTier;
import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("combatauras")
public interface CombatAurasConfig extends Config
{
    // =========================================================================
    // Sections (shared first, then one per warning, potion family and spell)
    // Standard layout in every warning/spell section:
    //   0 Enable | 1-8 section options | 10 Color | 11 Screen glow | 12 Shower icon | 13 Extra glow
    //   14 Notification | 15 Reminder duration | 19 Debug preview
    // =========================================================================

    @ConfigSection(name = "General Display & Performance", description = "Shared master switches, presets, glow intensity/depth/tint/motion and combat break timing.", position = 0)
    String generalDisplaySection = "generalDisplaySection";

    @ConfigSection(name = "Icon Shower", description = "Shared icon shower layout, size and speed.", position = 1, closedByDefault = true)
    String sharedShowerSection = "sharedShowerSection";

    @ConfigSection(name = "Inventory Cooldowns", description = "How cooldowns and ready highlights are drawn on inventory items, and re-dose protection.", position = 2, closedByDefault = true)
    String inventoryStyleSection = "inventoryStyleSection";

    @ConfigSection(name = "Shared Combat Spells", description = "Settings shared by every tracked spell.", position = 3, closedByDefault = true)
    String sharedSpellsSection = "sharedSpellsSection";

    @ConfigSection(name = "Shared Debug Tools", description = "Preview intensity, simulate combat, and test cooldown triggers.", position = 4, closedByDefault = true)
    String debugSection = "debugSection";

    @ConfigSection(name = "Low Health", description = "Low HP warning.", position = 10, closedByDefault = true)
    String warnLowHealthSection = "warnLowHealthSection";

    @ConfigSection(name = "Damage Flash", description = "Hit flash when you take damage.", position = 11, closedByDefault = true)
    String warnDamageSection = "warnDamageSection";

    @ConfigSection(name = "Low Prayer", description = "Low prayer warning.", position = 12, closedByDefault = true)
    String warnPrayerSection = "warnPrayerSection";

    @ConfigSection(name = "Poison", description = "Poison warning.", position = 13, closedByDefault = true)
    String warnPoisonSection = "warnPoisonSection";

    @ConfigSection(name = "Venom", description = "Venom warning.", position = 14, closedByDefault = true)
    String warnVenomSection = "warnVenomSection";

    @ConfigSection(name = "Disease", description = "Disease warning.", position = 15, closedByDefault = true)
    String warnDiseaseSection = "warnDiseaseSection";

    @ConfigSection(name = "Freeze", description = "Freeze / bind warning.", position = 16, closedByDefault = true)
    String warnFreezeSection = "warnFreezeSection";

    @ConfigSection(name = "Idle Warning", description = "Skilling/combat idle warning.", position = 17, closedByDefault = true)
    String warnIdleSection = "warnIdleSection";

    @ConfigSection(name = "Special Attack Ready", description = "Special attack energy reminder.", position = 18, closedByDefault = true)
    String warnSpecSection = "warnSpecSection";

    @ConfigSection(name = "Low Run Energy", description = "Low run energy warning.", position = 19, closedByDefault = true)
    String warnLowRunSection = "warnLowRunSection";

    @ConfigSection(name = "Unpotted Alert", description = "Alert when gaining combat XP without a stat boost.", position = 20, closedByDefault = true)
    String warnUnpottedSection = "warnUnpottedSection";

    @ConfigSection(name = "Imbued / Saturated Heart", description = "Heart cooldown overlay and expiry reminder.", position = 22, closedByDefault = true)
    String warnHeartSection = "warnHeartSection";

    @ConfigSection(name = "Potions & Raid Consumables", description = "Potion/raid consumable cooldown overlays and expiry reminders.", position = 23, closedByDefault = true)
    String warnPotionsBuffsSection = "warnPotionsBuffsSection";

    @ConfigSection(name = "Boost Potions", description = "Per-stat re-dose reminders for combat boost potions (Preserve-aware).", position = 25, closedByDefault = true)
    String boostSection = "boostSection";

    @ConfigSection(name = "Potion Reminders", description = "Which potion effects get an expiry reminder, and how many seconds early each one warns.", position = 24, closedByDefault = true)
    String potionRemindersSection = "potionRemindersSection";

    @ConfigSection(name = "Food", description = "Combo-eat and Hunter meat timers.", position = 26, closedByDefault = true)
    String foodSection = "foodSection";

    @ConfigSection(name = "Spell: Resurrect Thrall", description = "Resurrect Thrall tracking and reminder.", position = 30, closedByDefault = true)
    String spellThrallSection = "spellThrallSection";

    @ConfigSection(name = "Spell: Death Charge", description = "Death Charge tracking and reminder.", position = 31, closedByDefault = true)
    String spellDeathChargeSection = "spellDeathChargeSection";

    @ConfigSection(name = "Spell: Mark of Darkness", description = "Mark of Darkness tracking and reminder.", position = 32, closedByDefault = true)
    String spellMarkOfDarknessSection = "spellMarkOfDarknessSection";

    @ConfigSection(name = "Spell: Ward of Arceuus", description = "Ward of Arceuus tracking and reminder.", position = 33, closedByDefault = true)
    String spellWardOfArceuusSection = "spellWardOfArceuusSection";

    @ConfigSection(name = "Spell: Shadow Veil", description = "Shadow Veil tracking and reminder.", position = 34, closedByDefault = true)
    String spellShadowVeilSection = "spellShadowVeilSection";

    @ConfigSection(name = "Spell: Corruption", description = "Corruption tracking and reminder.", position = 35, closedByDefault = true)
    String spellCorruptionSection = "spellCorruptionSection";

    @ConfigSection(name = "Spell: Vengeance", description = "Vengeance tracking and reminder.", position = 36, closedByDefault = true)
    String spellVengeanceSection = "spellVengeanceSection";

    @ConfigSection(name = "Spell: Vengeance Other", description = "Vengeance Other tracking and reminder.", position = 37, closedByDefault = true)
    String spellVengeanceOtherSection = "spellVengeanceOtherSection";

    @ConfigSection(name = "Spell: Charge", description = "Charge tracking and reminder.", position = 38, closedByDefault = true)
    String spellChargeSection = "spellChargeSection";

    // =========================================================================
    // General Display & Performance
    // =========================================================================

    @ConfigItem(keyName = "preset", name = "Apply preset", description = "Pick Minimal / Standard / Everything to set the main on/off and intensity options in one go. Resets to Custom afterwards so you can keep tweaking.", section = generalDisplaySection, position = 0)
    default Preset preset() { return Preset.CUSTOM; }

    @ConfigItem(keyName = "allowScreenGlows", name = "Allow screen glows", description = "Master switch for every screen-edge glow and tint. Turn off to keep only icons/item highlights.", section = generalDisplaySection, position = 1)
    default boolean allowScreenGlows() { return true; }

    @ConfigItem(keyName = "allowIconShowers", name = "Allow icon showers", description = "Master switch for every showering icon. Turn off to keep only glows.", section = generalDisplaySection, position = 2)
    default boolean allowIconShowers() { return true; }

    @ConfigItem(keyName = "allowItemGlows", name = "Allow item & spellbook glows", description = "Master switch for outlines/glows on inventory items, spell icons, the Magic tab and the spec orb.", section = generalDisplaySection, position = 3)
    default boolean allowItemGlows() { return true; }

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "effectIntensity", name = "Effect intensity", description = "Master opacity/intensity for screen edge glows.", section = generalDisplaySection, position = 4)
    default int effectIntensity() { return 75; }

    @Range(min = 10, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "edgeDepth", name = "Edge depth", description = "How far inward from the screen edges the glow extends.", section = generalDisplaySection, position = 5)
    default int edgeDepth() { return 20; }

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "screenTint", name = "Screen tint", description = "Subtle full-screen tint strength during active warnings.", section = generalDisplaySection, position = 6)
    default int screenTint() { return 5; }

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "motionStrength", name = "Motion strength", description = "How much everything animates: screen glow pulses, the low-HP heartbeat, icon shower sway, and pulsing, breathing or sweeping item and spellbook glows. 0% holds them steady at full strength.", section = generalDisplaySection, position = 7)
    default int motionStrength() { return 65; }

    @ConfigItem(keyName = "glowColorMode", name = "Glow colour mixing", description = "When several warnings are active: blend their colours, or use the strongest warning's colour.", section = generalDisplaySection, position = 8)
    default GlowColorMode glowColorMode() { return GlowColorMode.BLEND; }

    @ConfigItem(keyName = "glowTop", name = "Glow edge: Top", description = "Draw the screen glow along the top edge. Any combination works; corners blend only where two enabled edges meet.", section = generalDisplaySection, position = 9)
    default boolean glowTop() { return true; }

    @ConfigItem(keyName = "glowBottom", name = "Glow edge: Bottom", description = "Draw the screen glow along the bottom edge. Any combination works; corners blend only where two enabled edges meet.", section = generalDisplaySection, position = 10)
    default boolean glowBottom() { return true; }

    @ConfigItem(keyName = "glowLeft", name = "Glow edge: Left", description = "Draw the screen glow along the left edge. Any combination works; corners blend only where two enabled edges meet.", section = generalDisplaySection, position = 11)
    default boolean glowLeft() { return true; }

    @ConfigItem(keyName = "glowRight", name = "Glow edge: Right", description = "Draw the screen glow along the right edge. Any combination works; corners blend only where two enabled edges meet.", section = generalDisplaySection, position = 12)
    default boolean glowRight() { return true; }

    @Range(min = 25, max = 200)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "iconGlowStrength", name = "Item & spellbook glow strength", description = "Strength of glows and outlines on inventory items, spell icons, the Magic tab and the spec orb. Above 100% layers the glow for extra brightness.", section = generalDisplaySection, position = 13)
    default int iconGlowStrength() { return 100; }

    // =========================================================================
    // Icon Shower
    // =========================================================================

    @ConfigItem(keyName = "showerStartLocation", name = "Start edge", description = "Screen edge where showering icons spawn.", section = sharedShowerSection, position = 0)
    default ShowerStartLocation showerStartLocation() { return ShowerStartLocation.BOTTOM; }

    @Range(min = 10, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "showerScreenPercent", name = "Travel distance", description = "Percentage of screen width/height showering icons travel.", section = sharedShowerSection, position = 1)
    default int showerScreenPercent() { return 33; }

    @Range(min = 1, max = 36)
    @ConfigItem(keyName = "spellShowerMaxIcons", name = "Max icons", description = "Maximum simultaneous showering icons across all active warnings.", section = sharedShowerSection, position = 2)
    default int spellShowerMaxIcons() { return 16; }

    @Range(min = 16, max = 48)
    @ConfigItem(keyName = "spellShowerIconSize", name = "Icon size (px)", description = "Rendered pixel size of showering icons.", section = sharedShowerSection, position = 3)
    default int spellShowerIconSize() { return 22; }

    @Range(min = 20, max = 250)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "spellShowerSpeed", name = "Speed", description = "Speed multiplier for showering icons.", section = sharedShowerSection, position = 4)
    default int spellShowerSpeed() { return 100; }

    @Range(min = 10, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "showerOpacity", name = "Icon opacity", description = "Peak opacity of showering icons.", section = sharedShowerSection, position = 5)
    default int showerOpacity() { return 66; }

    // =========================================================================
    // Inventory Cooldowns
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

    @ConfigItem(keyName = "menaphiteDivineGuard", name = "Menaphite divine guard", description = "Deprioritize left-click Drink on a Menaphite remedy while a divine potion or saturated heart boost is active, since the remedy removes divine effects (right-click still drinks).", section = inventoryStyleSection, position = 9)
    default boolean menaphiteDivineGuard() { return true; }

    // =========================================================================
    // Shared Combat Spells
    // =========================================================================

    @ConfigItem(keyName = "spellsEnabled", name = "Master enable", description = "Master toggle for combat spell cooldowns, glows, and reminders.", section = sharedSpellsSection, position = 0)
    default boolean spellsEnabled() { return true; }

    @ConfigItem(keyName = "spellOnlyInCombat", name = "Only remind in combat", description = "Only show expired spell/buff reminders while in combat.", section = sharedSpellsSection, position = 1)
    default boolean spellOnlyInCombat() { return true; }

    @ConfigItem(keyName = "spellOnlyRemindAfterCast", name = "Only remind after cast", description = "Only remind when a spell expires after you cast it (prevents alerts on combat start for uncast spells).", section = sharedSpellsSection, position = 2)
    default boolean spellOnlyRemindAfterCast() { return true; }

    @ConfigItem(keyName = "spellScreenGlow", name = "Shared screen glow", description = "Default screen-edge glow for expired spells.", section = sharedSpellsSection, position = 3)
    default boolean spellScreenGlow() { return true; }

    @ConfigItem(keyName = "spellShowerIcons", name = "Shared shower icons", description = "Default icon shower for expired spells.", section = sharedSpellsSection, position = 4)
    default boolean spellShowerIcons() { return true; }

    @ConfigItem(keyName = "spellGlowInSpellbook", name = "Shared spellbook glow", description = "Default glow on expired spell widgets in the spellbook.", section = sharedSpellsSection, position = 5)
    default boolean spellGlowInSpellbook() { return true; }

    @ConfigItem(keyName = "spellbookTabGlow", name = "Glow Magic tab icon", description = "Glow the Magic spellbook book icon when any tracked spell is ready.", section = sharedSpellsSection, position = 6)
    default boolean spellbookTabGlow() { return true; }

    @ConfigItem(keyName = "spellBorderStyle", name = "Spellbook glow style", description = "Glow style for ready spells and the Magic tab (independent of the inventory ready border).", section = sharedSpellsSection, position = 7)
    default BorderStyle spellBorderStyle() { return BorderStyle.SOFT_GLOW; }

    @ConfigItem(keyName = "spellCooldownDisplayMode", name = "Spell cooldown style", description = "How remaining recast time is drawn over spell widgets.", section = sharedSpellsSection, position = 8)
    default SpellCooldownDisplayMode spellCooldownDisplayMode() { return SpellCooldownDisplayMode.FILL_AND_TEXT; }

    @Alpha
    @ConfigItem(keyName = "spellCooldownOverlayColor", name = "Spell cooldown tint", description = "Translucent tint drawn over spells while on cooldown.", section = sharedSpellsSection, position = 9)
    default Color spellCooldownOverlayColor() { return new Color(0, 0, 0, 145); }

    @Range(min = 5, max = 600)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "combatBreakReRemindSeconds", name = "Combat break re-remind", description = "After this long out of combat, re-entering combat shows ready-spell reminders again.", section = sharedSpellsSection, position = 10)
    default int combatBreakReRemindSeconds() { return 120; }

    // =========================================================================
    // Shared Debug Tools
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

    // =========================================================================
    // Low Health
    // =========================================================================

    @ConfigItem(keyName = "lowHealthEnabled", name = "Enable", description = "Track low HP warning.", section = warnLowHealthSection, position = 0)
    default boolean lowHealthEnabled() { return true; }

    @Range(min = 5, max = 95)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "lowHealthThreshold", name = "Threshold", description = "HP percentage below which low-health activates.", section = warnLowHealthSection, position = 1)
    default int lowHealthThreshold() { return 35; }

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "healthPulseStrength", name = "Heartbeat pulse", description = "Strength of the low-health heartbeat pulse.", section = warnLowHealthSection, position = 2)
    default int healthPulseStrength() { return 60; }

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "lowHealthEscalation", name = "Danger escalation", description = "Extra intensity as HP drops further below the threshold.", section = warnLowHealthSection, position = 3)
    default int lowHealthEscalation() { return 60; }

    @Alpha
    @ConfigItem(keyName = "lowHealthColor", name = "Color", description = "Color of the low-health screen vignette.", section = warnLowHealthSection, position = 10)
    default Color lowHealthColor() { return new Color(195, 22, 26, 210); }

    @ConfigItem(keyName = "lowHealthGlow", name = "Screen glow", description = "Show screen-edge glow when HP is low.", section = warnLowHealthSection, position = 11)
    default boolean lowHealthGlow() { return true; }

    @ConfigItem(keyName = "lowHealthShower", name = "Shower icon", description = "Shower Hitpoints icon when HP is low.", section = warnLowHealthSection, position = 12)
    default boolean lowHealthShower() { return true; }

    @ConfigItem(keyName = "lowHealthNotification", name = "Notification", description = "RuneLite notification (sound/tray/flash) when HP drops below the threshold.", section = warnLowHealthSection, position = 14)
    default Notification lowHealthNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "lowHealthDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for low HP, then hide until it happens again (0 = while active).", section = warnLowHealthSection, position = 15)
    default int lowHealthDurationSeconds() { return 0; }

    @ConfigItem(keyName = "debugLowHealth", name = "Debug preview", description = "Preview low health vignette, blood splatter & Hitpoints icon.", section = warnLowHealthSection, position = 19)
    default boolean debugLowHealth() { return false; }

    // =========================================================================
    // Damage Flash
    // =========================================================================

    @ConfigItem(keyName = "damageFeedbackEnabled", name = "Enable", description = "Flash and/or shower hitsplat when taking damage.", section = warnDamageSection, position = 0)
    default boolean damageFeedbackEnabled() { return true; }

    @Range(min = 0, max = 99)
    @ConfigItem(keyName = "damageMinAmount", name = "Minimum damage", description = "Only flash for hits of at least this much damage (0 = every hit).", section = warnDamageSection, position = 1)
    default int damageMinAmount() { return 0; }

    @ConfigItem(keyName = "damageScaleWithHp", name = "Scale by % of max HP", description = "Flash strength follows the hit as a share of your max HP (a 20 damage hit reads the same at 40 HP as a 50 hit at 99 HP). Off: fixed scale.", section = warnDamageSection, position = 2)
    default boolean damageScaleWithHp() { return true; }

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "damageFeedbackStrength", name = "Strength", description = "Strength of the damage flash.", section = warnDamageSection, position = 3)
    default int damageFeedbackStrength() { return 65; }

    @Range(min = 100, max = 2000)
    @Units(Units.MILLISECONDS)
    @ConfigItem(keyName = "damageFeedbackDuration", name = "Duration", description = "Duration of the damage flash decay.", section = warnDamageSection, position = 4)
    default int damageFeedbackDuration() { return 600; }

    @Alpha
    @ConfigItem(keyName = "damageFlashColor", name = "Color", description = "Color used for damage flashes.", section = warnDamageSection, position = 10)
    default Color damageFlashColor() { return new Color(225, 35, 35, 200); }

    @ConfigItem(keyName = "damageGlow", name = "Screen glow", description = "Show screen-edge flash when taking damage.", section = warnDamageSection, position = 11)
    default boolean damageGlow() { return true; }

    @ConfigItem(keyName = "damageShower", name = "Shower icon", description = "Shower red hitsplat icon when taking damage.", section = warnDamageSection, position = 12)
    default boolean damageShower() { return false; }

    @ConfigItem(keyName = "debugDamageFlash", name = "Debug preview", description = "Preview damage hit flash & red hitsplat icon.", section = warnDamageSection, position = 19)
    default boolean debugDamageFlash() { return false; }

    // =========================================================================
    // Low Prayer
    // =========================================================================

    @ConfigItem(keyName = "lowPrayerEnabled", name = "Enable", description = "Track low prayer warning.", section = warnPrayerSection, position = 0)
    default boolean lowPrayerEnabled() { return true; }

    @Range(min = 5, max = 95)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "lowPrayerThreshold", name = "Threshold", description = "Prayer percentage below which the warning activates.", section = warnPrayerSection, position = 1)
    default int lowPrayerThreshold() { return 25; }

    @ConfigItem(keyName = "prayerActiveOnly", name = "Only when prayers active", description = "Only trigger when at least one prayer is active.", section = warnPrayerSection, position = 2)
    default boolean prayerActiveOnly() { return false; }

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "lowPrayerEscalation", name = "Danger escalation", description = "Extra intensity as prayer drops further below the threshold.", section = warnPrayerSection, position = 3)
    default int lowPrayerEscalation() { return 60; }

    @Alpha
    @ConfigItem(keyName = "lowPrayerColor", name = "Color", description = "Color for low prayer vignette and inventory glow.", section = warnPrayerSection, position = 10)
    default Color lowPrayerColor() { return new Color(52, 158, 224, 200); }

    @ConfigItem(keyName = "lowPrayerGlow", name = "Screen glow", description = "Show screen-edge glow when prayer is low.", section = warnPrayerSection, position = 11)
    default boolean lowPrayerGlow() { return true; }

    @ConfigItem(keyName = "lowPrayerShower", name = "Shower icon", description = "Shower Prayer skill icon when prayer is low.", section = warnPrayerSection, position = 12)
    default boolean lowPrayerShower() { return true; }

    @ConfigItem(keyName = "lowPrayerInventoryGlow", name = "Glow inventory restores", description = "Highlight prayer-restoring potions in your inventory when prayer is low.", section = warnPrayerSection, position = 13)
    default boolean lowPrayerInventoryGlow() { return true; }

    @ConfigItem(keyName = "lowPrayerNotification", name = "Notification", description = "RuneLite notification (sound/tray/flash) when prayer drops below the threshold.", section = warnPrayerSection, position = 14)
    default Notification lowPrayerNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "lowPrayerDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for low prayer, then hide until it happens again (0 = while active).", section = warnPrayerSection, position = 15)
    default int lowPrayerDurationSeconds() { return 0; }

    @ConfigItem(keyName = "debugLowPrayer", name = "Debug preview", description = "Preview low prayer vignette, Prayer icon & inventory restore glow.", section = warnPrayerSection, position = 19)
    default boolean debugLowPrayer() { return false; }

    // =========================================================================
    // Poison
    // =========================================================================

    @ConfigItem(keyName = "poisonEnabled", name = "Enable", description = "Track poison status.", section = warnPoisonSection, position = 0)
    default boolean poisonEnabled() { return true; }

    @Alpha
    @ConfigItem(keyName = "poisonColor", name = "Color", description = "Color for poison vignette.", section = warnPoisonSection, position = 10)
    default Color poisonColor() { return new Color(54, 178, 62, 195); }

    @ConfigItem(keyName = "poisonGlow", name = "Screen glow", description = "Show screen-edge glow when poisoned.", section = warnPoisonSection, position = 11)
    default boolean poisonGlow() { return true; }

    @ConfigItem(keyName = "poisonShower", name = "Shower icon", description = "Shower green poison hitsplat icon when poisoned.", section = warnPoisonSection, position = 12)
    default boolean poisonShower() { return true; }

    @ConfigItem(keyName = "poisonNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when you are poisoned.", section = warnPoisonSection, position = 14)
    default Notification poisonNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "poisonDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for poison, then hide until it happens again (0 = while active).", section = warnPoisonSection, position = 15)
    default int poisonDurationSeconds() { return 0; }

    @ConfigItem(keyName = "debugPoison", name = "Debug preview", description = "Preview poison vignette & green hitsplat icon.", section = warnPoisonSection, position = 19)
    default boolean debugPoison() { return false; }

    // =========================================================================
    // Venom
    // =========================================================================

    @ConfigItem(keyName = "venomEnabled", name = "Enable", description = "Track venom status.", section = warnVenomSection, position = 0)
    default boolean venomEnabled() { return true; }

    @Alpha
    @ConfigItem(keyName = "venomColor", name = "Color", description = "Color for venom vignette.", section = warnVenomSection, position = 10)
    default Color venomColor() { return new Color(24, 128, 68, 210); }

    @ConfigItem(keyName = "venomGlow", name = "Screen glow", description = "Show screen-edge glow when envenomed.", section = warnVenomSection, position = 11)
    default boolean venomGlow() { return true; }

    @ConfigItem(keyName = "venomShower", name = "Shower icon", description = "Shower anti-venom icon when envenomed.", section = warnVenomSection, position = 12)
    default boolean venomShower() { return true; }

    @ConfigItem(keyName = "venomNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when you are envenomed.", section = warnVenomSection, position = 14)
    default Notification venomNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "venomDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for venom, then hide until it happens again (0 = while active).", section = warnVenomSection, position = 15)
    default int venomDurationSeconds() { return 0; }

    @ConfigItem(keyName = "debugVenom", name = "Debug preview", description = "Preview venom vignette & anti-venom icon.", section = warnVenomSection, position = 19)
    default boolean debugVenom() { return false; }

    // =========================================================================
    // Disease
    // =========================================================================

    @ConfigItem(keyName = "diseaseEnabled", name = "Enable", description = "Track disease status.", section = warnDiseaseSection, position = 0)
    default boolean diseaseEnabled() { return true; }

    @Alpha
    @ConfigItem(keyName = "diseaseColor", name = "Color", description = "Color for disease vignette.", section = warnDiseaseSection, position = 10)
    default Color diseaseColor() { return new Color(186, 152, 42, 195); }

    @ConfigItem(keyName = "diseaseGlow", name = "Screen glow", description = "Show screen-edge glow when diseased.", section = warnDiseaseSection, position = 11)
    default boolean diseaseGlow() { return true; }

    @ConfigItem(keyName = "diseaseShower", name = "Shower icon", description = "Shower disease icon when diseased.", section = warnDiseaseSection, position = 12)
    default boolean diseaseShower() { return true; }

    @ConfigItem(keyName = "diseaseNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when you are diseased.", section = warnDiseaseSection, position = 14)
    default Notification diseaseNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "diseaseDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for disease, then hide until it happens again (0 = while active).", section = warnDiseaseSection, position = 15)
    default int diseaseDurationSeconds() { return 0; }

    @ConfigItem(keyName = "debugDisease", name = "Debug preview", description = "Preview disease vignette & disease hitsplat icon.", section = warnDiseaseSection, position = 19)
    default boolean debugDisease() { return false; }

    // =========================================================================
    // Freeze
    // =========================================================================

    @ConfigItem(keyName = "freezeEnabled", name = "Enable", description = "Track freeze/entangle status.", section = warnFreezeSection, position = 0)
    default boolean freezeEnabled() { return true; }

    @Alpha
    @ConfigItem(keyName = "freezeColor", name = "Color", description = "Color for freeze vignette.", section = warnFreezeSection, position = 10)
    default Color freezeColor() { return new Color(110, 205, 248, 205); }

    @ConfigItem(keyName = "freezeGlow", name = "Screen glow", description = "Show screen-edge glow when frozen.", section = warnFreezeSection, position = 11)
    default boolean freezeGlow() { return true; }

    @ConfigItem(keyName = "freezeShower", name = "Shower icon", description = "Shower Ice Barrage icon when frozen.", section = warnFreezeSection, position = 12)
    default boolean freezeShower() { return true; }

    @ConfigItem(keyName = "freezeNotification", name = "Notification", description = "RuneLite notification (sound/tray/flash) when you are frozen.", section = warnFreezeSection, position = 14)
    default Notification freezeNotification() { return Notification.OFF; }

    @Range(min = 0, max = 30)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "freezeMaxDisplaySeconds", name = "Reminder duration", description = "Seconds to show the freeze warning (0 = for the whole freeze).", section = warnFreezeSection, position = 15)
    default int freezeMaxDisplaySeconds() { return 0; }

    @ConfigItem(keyName = "debugFreeze", name = "Debug preview", description = "Preview freeze vignette & Ice Barrage icon.", section = warnFreezeSection, position = 19)
    default boolean debugFreeze() { return false; }

    // =========================================================================
    // Idle Warning
    // =========================================================================

    @ConfigItem(keyName = "idleEnabled", name = "Enable", description = "Warn when you stop skilling or fighting.", section = warnIdleSection, position = 0)
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

    @ConfigItem(keyName = "idleAutoHide", name = "Auto-hide", description = "Fade out the idle warning once it has been at full strength for the Reminder duration.", section = warnIdleSection, position = 4)
    default boolean idleAutoHide() { return false; }

    @Alpha
    @ConfigItem(keyName = "idleColor", name = "Color", description = "Color of the idle screen-edge glow.", section = warnIdleSection, position = 10)
    default Color idleColor() { return new Color(235, 135, 35, 210); }

    @ConfigItem(keyName = "idleGlow", name = "Screen glow", description = "Show screen-edge glow when idle.", section = warnIdleSection, position = 11)
    default boolean idleGlow() { return true; }

    @ConfigItem(keyName = "idleShower", name = "Shower icon", description = "Shower Lunar Dream icon when idle.", section = warnIdleSection, position = 12)
    default boolean idleShower() { return true; }

    @ConfigItem(keyName = "idleNotification", name = "Notification", description = "RuneLite notification (sound, tray, flash - configurable) sent once at critical idle time.", section = warnIdleSection, position = 14)
    default Notification idleNotification() { return Notification.OFF; }

    @Range(min = 1, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "idleAutoHideSeconds", name = "Reminder duration", description = "Seconds the idle warning stays at full strength before auto-hiding (when Auto-hide is on).", section = warnIdleSection, position = 15)
    default int idleAutoHideSeconds() { return 5; }

    @ConfigItem(keyName = "debugIdle", name = "Debug preview", description = "Preview idle vignette & Lunar Dream icon.", section = warnIdleSection, position = 19)
    default boolean debugIdle() { return false; }

    // =========================================================================
    // Special Attack Ready
    // =========================================================================

    @ConfigItem(keyName = "specEnabled", name = "Enable", description = "Remind when special attack energy reaches the threshold.", section = warnSpecSection, position = 0)
    default boolean specEnabled() { return false; }

    @Range(min = 10, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "specThresholdPercent", name = "Threshold", description = "Minimum special attack percentage to trigger.", section = warnSpecSection, position = 1)
    default int specThresholdPercent() { return 100; }

    @ConfigItem(keyName = "specOnlyInCombat", name = "Only in combat", description = "Only trigger while in combat.", section = warnSpecSection, position = 2)
    default boolean specOnlyInCombat() { return true; }

    @ConfigItem(keyName = "specOrbStyle", name = "Orb glow style", description = "Glow style used around the special attack orb.", section = warnSpecSection, position = 3)
    default BorderStyle specOrbStyle() { return BorderStyle.SOFT_GLOW; }

    @Alpha
    @ConfigItem(keyName = "specColor", name = "Color", description = "Color of the spec ready glow.", section = warnSpecSection, position = 10)
    default Color specColor() { return new Color(45, 212, 191, 190); }

    @ConfigItem(keyName = "specGlow", name = "Screen glow", description = "Show screen-edge glow when spec is ready.", section = warnSpecSection, position = 11)
    default boolean specGlow() { return true; }

    @ConfigItem(keyName = "specShower", name = "Shower icon", description = "Shower crossed swords icon when spec is ready.", section = warnSpecSection, position = 12)
    default boolean specShower() { return true; }

    @ConfigItem(keyName = "specOrbGlow", name = "Glow spec orb", description = "Glow the minimap Special Attack orb when spec is ready.", section = warnSpecSection, position = 13)
    default boolean specOrbGlow() { return true; }

    @ConfigItem(keyName = "specNotification", name = "Notification", description = "RuneLite notification (sound/tray/flash) when special attack reaches the threshold.", section = warnSpecSection, position = 14)
    default Notification specNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "specDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for special attack ready (also the orb glow), then hide until it happens again (0 = while active).", section = warnSpecSection, position = 15)
    default int specDurationSeconds() { return 0; }

    @ConfigItem(keyName = "debugSpecReady", name = "Debug preview", description = "Preview spec ready vignette, orb glow & crossed swords icon.", section = warnSpecSection, position = 19)
    default boolean debugSpecReady() { return false; }

    // =========================================================================
    // Low Run Energy
    // =========================================================================

    @ConfigItem(keyName = "lowRunEnabled", name = "Enable", description = "Track low run energy warning.", section = warnLowRunSection, position = 0)
    default boolean lowRunEnabled() { return false; }

    @Range(min = 5, max = 95)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "lowRunThreshold", name = "Threshold", description = "Run energy percentage below which warning activates.", section = warnLowRunSection, position = 1)
    default int lowRunThreshold() { return 25; }

    @ConfigItem(keyName = "lowRunOnlyInCombat", name = "Only in combat", description = "Only trigger while in combat.", section = warnLowRunSection, position = 2)
    default boolean lowRunOnlyInCombat() { return true; }

    @Alpha
    @ConfigItem(keyName = "lowRunColor", name = "Color", description = "Color of the low run energy glow.", section = warnLowRunSection, position = 10)
    default Color lowRunColor() { return new Color(234, 179, 8, 195); }

    @ConfigItem(keyName = "lowRunGlow", name = "Screen glow", description = "Show screen-edge glow when run energy is low.", section = warnLowRunSection, position = 11)
    default boolean lowRunGlow() { return true; }

    @ConfigItem(keyName = "lowRunShower", name = "Shower icon", description = "Shower Run orb icon when run energy is low.", section = warnLowRunSection, position = 12)
    default boolean lowRunShower() { return true; }

    @ConfigItem(keyName = "lowRunNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when run energy drops below the threshold.", section = warnLowRunSection, position = 14)
    default Notification lowRunNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "lowRunDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for low run energy, then hide until it happens again (0 = while active).", section = warnLowRunSection, position = 15)
    default int lowRunDurationSeconds() { return 0; }

    @ConfigItem(keyName = "debugLowRun", name = "Debug preview", description = "Preview low run energy vignette & Run orb icon.", section = warnLowRunSection, position = 19)
    default boolean debugLowRun() { return false; }

    // =========================================================================
    // Unpotted Alert
    // =========================================================================

    @ConfigItem(keyName = "unpottedEnabled", name = "Enable", description = "Warn when gaining combat XP without a stat boost (ignores alching and utility spells; supports cannon).", section = warnUnpottedSection, position = 0)
    default boolean unpottedEnabled() { return true; }

    @Range(min = 0, max = 10)
    @ConfigItem(keyName = "unpottedThreshold", name = "Boost threshold", description = "Trigger if boosted level is at or below real level + this value.", section = warnUnpottedSection, position = 1)
    default int unpottedThreshold() { return 1; }

    @Alpha
    @ConfigItem(keyName = "unpottedColor", name = "Color", description = "Color of the unpotted-in-combat glow.", section = warnUnpottedSection, position = 10)
    default Color unpottedColor() { return new Color(249, 115, 22, 200); }

    @ConfigItem(keyName = "unpottedGlow", name = "Screen glow", description = "Show screen-edge glow when unpotted in combat.", section = warnUnpottedSection, position = 11)
    default boolean unpottedGlow() { return true; }

    @ConfigItem(keyName = "unpottedShower", name = "Shower icon", description = "Shower Stats (three bars) icon when unpotted in combat.", section = warnUnpottedSection, position = 12)
    default boolean unpottedShower() { return true; }

    @ConfigItem(keyName = "unpottedNotification", name = "Notification", description = "RuneLite notification (sound/tray/flash) when you are unpotted in combat.", section = warnUnpottedSection, position = 14)
    default Notification unpottedNotification() { return Notification.OFF; }

    @Range(min = 3, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "unpottedAlertDurationSeconds", name = "Reminder duration", description = "Seconds the unpotted alert stays up unless you pot up.", section = warnUnpottedSection, position = 15)
    default int unpottedAlertDurationSeconds() { return 10; }

    @ConfigItem(keyName = "debugUnpotted", name = "Debug preview", description = "Preview unpotted alert vignette & Stats (three bars) icon.", section = warnUnpottedSection, position = 19)
    default boolean debugUnpotted() { return false; }

    // =========================================================================
    // Imbued / Saturated Heart
    // =========================================================================

    @ConfigItem(keyName = "heartsEnabled", name = "Enable", description = "Track the Imbued/Saturated Heart cooldown and show it on the item.", section = warnHeartSection, position = 0)
    default boolean heartsEnabled() { return true; }

    @ConfigItem(keyName = "heartOnlyInCombat", name = "Only remind in combat", description = "Only show the Heart expiry reminder while in combat.", section = warnHeartSection, position = 1)
    default boolean heartOnlyInCombat() { return true; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "heartPreRemindSeconds", name = "Warn early", description = "Start the Heart reminder this many seconds before it can be used again, so you can re-dose without losing uptime (0 = when ready).", section = warnHeartSection, position = 2)
    default int heartPreRemindSeconds() { return 0; }

    @Alpha
    @ConfigItem(keyName = "heartReminderColor", name = "Color", description = "Color when the Heart becomes ready again.", section = warnHeartSection, position = 10)
    default Color heartReminderColor() { return new Color(185, 65, 225, 200); }

    @ConfigItem(keyName = "heartScreenReminder", name = "Screen glow", description = "Show screen-edge glow when the Heart becomes ready again.", section = warnHeartSection, position = 11)
    default boolean heartScreenReminder() { return true; }

    @ConfigItem(keyName = "heartShower", name = "Shower icon", description = "Shower the Heart icon when it becomes ready again.", section = warnHeartSection, position = 12)
    default boolean heartShower() { return true; }

    @ConfigItem(keyName = "heartNotification", name = "Notification", description = "RuneLite notification (sound/tray/flash) when your Heart is ready again.", section = warnHeartSection, position = 14)
    default Notification heartNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "heartDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for the Heart becoming ready again, then hide until it happens again (0 = while active).", section = warnHeartSection, position = 15)
    default int heartDurationSeconds() { return 10; }

    @ConfigItem(keyName = "debugHeartReady", name = "Debug preview", description = "Preview Imbued/Saturated Heart screen glow & icon shower.", section = warnHeartSection, position = 19)
    default boolean debugHeartReady() { return false; }

    // =========================================================================
    // Potions & Raid Consumables
    // =========================================================================

    @ConfigItem(keyName = "potionEffectsEnabled", name = "Enable", description = "Track potion effects (antifire, stamina, divine, anti-venom...) and show them on the items.", section = warnPotionsBuffsSection, position = 0)
    default boolean potionEffectsEnabled() { return true; }

    @ConfigItem(keyName = "potionCooldownsEnabled", name = "Track surge potions", description = "Show the Surge potion cooldown on the item.", section = warnPotionsBuffsSection, position = 1)
    default boolean potionCooldownsEnabled() { return true; }

    @ConfigItem(keyName = "raidPotionsEnabled", name = "Track raid consumables", description = "Overloads, Smelling salts, Liquid adrenaline, Silk dressing, Blessed crystal scarab.", section = warnPotionsBuffsSection, position = 2)
    default boolean raidPotionsEnabled() { return true; }

    @ConfigItem(keyName = "buffOnlyInCombat", name = "Only remind in combat", description = "Only show potion expiry reminders while in combat.", section = warnPotionsBuffsSection, position = 3)
    default boolean buffOnlyInCombat() { return true; }

    @ConfigItem(keyName = "remindStamina", name = "Stamina", description = "Remind when a stamina potion effect expires.", section = potionRemindersSection, position = 1)
    default boolean remindStamina() { return false; }

    @ConfigItem(keyName = "remindAntipoison", name = "Antipoison / anti-venom", description = "Remind when antipoison or anti-venom protection expires.", section = potionRemindersSection, position = 3)
    default boolean remindAntipoison() { return true; }

    @ConfigItem(keyName = "remindAntifire", name = "Antifire", description = "Remind when (super) antifire protection expires.", section = potionRemindersSection, position = 5)
    default boolean remindAntifire() { return true; }

    @ConfigItem(keyName = "remindDivine", name = "Divine potions", description = "Remind when a divine potion expires.", section = potionRemindersSection, position = 7)
    default boolean remindDivine() { return true; }

    @ConfigItem(keyName = "remindOtherPotions", name = "Other (menaphite, moonlight, raid items)", description = "Remind when Menaphite remedy, moonlight potions or other raid consumables expire.", section = potionRemindersSection, position = 27)
    default boolean remindOtherPotions() { return true; }

    @ConfigItem(keyName = "remindSurge", name = "Surge potion", description = "Remind when the surge potion cooldown ends (needs Track surge potions).", section = potionRemindersSection, position = 21)
    default boolean remindSurge() { return true; }

    @ConfigItem(keyName = "remindGoading", name = "Goading potion", description = "Remind when the goading potion effect expires.", section = potionRemindersSection, position = 23)
    default boolean remindGoading() { return true; }

    @ConfigItem(keyName = "remindPrayerRegeneration", name = "Prayer regeneration", description = "Remind when the prayer regeneration potion effect expires.", section = potionRemindersSection, position = 25)
    default boolean remindPrayerRegeneration() { return true; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "divinePreRemindSeconds", name = "Divine: warn early", description = "Start the divine potion reminder this many seconds before it expires, so you can re-dose without losing uptime.", section = potionRemindersSection, position = 8)
    default int divinePreRemindSeconds() { return 0; }

    @Alpha
    @ConfigItem(keyName = "buffReminderColor", name = "Color", description = "Color when a potion you used expires.", section = warnPotionsBuffsSection, position = 10)
    default Color buffReminderColor() { return new Color(65, 215, 145, 200); }

    @ConfigItem(keyName = "buffScreenReminder", name = "Screen glow", description = "Show screen-edge glow when a potion you used expires.", section = warnPotionsBuffsSection, position = 11)
    default boolean buffScreenReminder() { return true; }

    @ConfigItem(keyName = "buffShower", name = "Shower icon", description = "Shower the expired potion's icon.", section = warnPotionsBuffsSection, position = 12)
    default boolean buffShower() { return true; }

    @ConfigItem(keyName = "buffNotification", name = "Notification", description = "RuneLite notification (sound/tray/flash) when a used potion buff expires.", section = warnPotionsBuffsSection, position = 14)
    default Notification buffNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "buffDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for an expired potion, then hide until it happens again (0 = while active).", section = warnPotionsBuffsSection, position = 15)
    default int buffDurationSeconds() { return 10; }

    @ConfigItem(keyName = "debugBuffExpired", name = "Debug preview", description = "Preview expired potion buff screen glow & icon shower.", section = warnPotionsBuffsSection, position = 19)
    default boolean debugBuffExpired() { return false; }

    // =========================================================================
    // Boost Potions
    // =========================================================================

    @ConfigItem(keyName = "boostPotionsEnabled", name = "Enable", description = "Estimate re-dose timing on combat boost potions (Preserve-aware).", section = boostSection, position = 0)
    default boolean boostPotionsEnabled() { return true; }

    @Range(min = 0, max = 30)
    @ConfigItem(keyName = "boostRedoseAttack", name = "Attack: re-dose at +", description = "Remind to re-dose when your Attack boost drops to this many levels (capped just below the potion's max boost). 0 = don't track Attack.", section = boostSection, position = 1)
    default int boostRedoseAttack() { return 15; }

    @Range(min = 0, max = 30)
    @ConfigItem(keyName = "boostRedoseStrength", name = "Strength: re-dose at +", description = "Remind to re-dose when your Strength boost drops to this many levels (capped just below the potion's max boost). 0 = don't track Strength.", section = boostSection, position = 2)
    default int boostRedoseStrength() { return 15; }

    @Range(min = 0, max = 30)
    @ConfigItem(keyName = "boostRedoseDefence", name = "Defence: re-dose at +", description = "Remind to re-dose when your Defence boost drops to this many levels (capped just below the potion's max boost). 0 = don't track Defence.", section = boostSection, position = 3)
    default int boostRedoseDefence() { return 15; }

    @Range(min = 0, max = 30)
    @ConfigItem(keyName = "boostRedoseRanged", name = "Ranged: re-dose at +", description = "Remind to re-dose when your Ranged boost drops to this many levels (capped just below the potion's max boost). 0 = don't track Ranged.", section = boostSection, position = 4)
    default int boostRedoseRanged() { return 10; }

    @Range(min = 0, max = 30)
    @ConfigItem(keyName = "boostRedoseMagic", name = "Magic: re-dose at +", description = "Remind to re-dose when your Magic boost drops to this many levels (capped just below the potion's max boost). 0 = don't track Magic.", section = boostSection, position = 5)
    default int boostRedoseMagic() { return 6; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "boostPreRemindSeconds", name = "Warn early", description = "Start the re-dose reminder this many seconds before a boost reaches its re-dose level (0 = when it does).", section = boostSection, position = 6)
    default int boostPreRemindSeconds() { return 0; }

    @ConfigItem(keyName = "boostOnlyInCombat", name = "Only remind in combat", description = "Only show boost re-dose reminders while in combat.", section = boostSection, position = 7)
    default boolean boostOnlyInCombat() { return true; }

    @Alpha
    @ConfigItem(keyName = "boostReminderColor", name = "Color", description = "Color when a boost potion is ready to re-dose.", section = boostSection, position = 10)
    default Color boostReminderColor() { return new Color(239, 68, 68, 200); }

    @ConfigItem(keyName = "boostGlow", name = "Screen glow", description = "Show screen-edge glow when a boost potion is ready to re-dose.", section = boostSection, position = 11)
    default boolean boostGlow() { return true; }

    @ConfigItem(keyName = "boostShower", name = "Shower icon", description = "Shower the boost potion's icon when it is ready to re-dose.", section = boostSection, position = 12)
    default boolean boostShower() { return true; }

    @ConfigItem(keyName = "boostNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when a boost potion is ready to re-dose.", section = boostSection, position = 14)
    default Notification boostNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "boostDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for a boost potion ready to re-dose, then hide until it happens again (0 = while active).", section = boostSection, position = 15)
    default int boostDurationSeconds() { return 10; }

    // =========================================================================
    // Food
    // =========================================================================

    @ConfigItem(keyName = "foodCooldownsEnabled", name = "Enable", description = "Show 3-tick eat/combo-eat locks and the 7-tick Hunter meat delayed heal.", section = foodSection, position = 0)
    default boolean foodCooldownsEnabled() { return true; }

    // =========================================================================
    // Spell: Resurrect Thrall
    // =========================================================================

    @ConfigItem(keyName = "spellThrallEnabled", name = "Enable", description = "Track Resurrect Thrall.", section = spellThrallSection, position = 0)
    default boolean spellThrallEnabled() { return true; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellThrallLeadSeconds", name = "Warn early", description = "Start the reminder this many seconds before the spell can be recast (0 = when it can).", section = spellThrallSection, position = 4)
    default int spellThrallLeadSeconds() { return 0; }

    @ConfigItem(keyName = "spellThrallRecastProtection", name = "Recast protection", description = "Deprioritize left-click Cast on a Resurrect spell while a thrall is still up or on cooldown (right-click still casts).", section = spellThrallSection, position = 3)
    default boolean spellThrallRecastProtection() { return true; }

    @ConfigItem(keyName = "thrallDurationTier", name = "CA duration", description = "Thrall duration doubles once the Master Combat Achievement tier is complete. Auto-detect reads it from the game.", section = spellThrallSection, position = 1)
    default ThrallDurationTier thrallDurationTier() { return ThrallDurationTier.AUTO; }

    @ConfigItem(keyName = "thrallRemindOnCombatStart", name = "Remind on combat start", description = "Remind to summon a Thrall as soon as combat starts, even before your first cast.", section = spellThrallSection, position = 2)
    default boolean thrallRemindOnCombatStart() { return false; }

    @Alpha
    @ConfigItem(keyName = "spellThrallColor", name = "Color", description = "Glow color for Resurrect Thrall.", section = spellThrallSection, position = 10)
    default Color spellThrallColor() { return new Color(138, 92, 246, 210); }

    @ConfigItem(keyName = "spellThrallGlow", name = "Screen glow", description = "Show screen glow for Thrall.", section = spellThrallSection, position = 11)
    default boolean spellThrallGlow() { return true; }

    @ConfigItem(keyName = "spellThrallShower", name = "Shower icon", description = "Shower Thrall icon.", section = spellThrallSection, position = 12)
    default boolean spellThrallShower() { return true; }

    @ConfigItem(keyName = "spellThrallSpellbook", name = "Spellbook glow", description = "Glow Thrall in spellbook.", section = spellThrallSection, position = 13)
    default boolean spellThrallSpellbook() { return true; }

    @ConfigItem(keyName = "spellThrallNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when Resurrect Thrall is ready to recast.", section = spellThrallSection, position = 14)
    default Notification spellThrallNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellThrallDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for Resurrect Thrall once it is ready, then hide until it happens again (0 = while active).", section = spellThrallSection, position = 15)
    default int spellThrallDurationSeconds() { return 12; }

    @ConfigItem(keyName = "debugSpellThrall", name = "Debug preview", description = "Preview expired Resurrect Thrall.", section = spellThrallSection, position = 19)
    default boolean debugSpellThrall() { return false; }

    // =========================================================================
    // Spell: Death Charge
    // =========================================================================

    @ConfigItem(keyName = "spellDeathChargeEnabled", name = "Enable", description = "Track Death Charge recast cooldown.", section = spellDeathChargeSection, position = 0)
    default boolean spellDeathChargeEnabled() { return true; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellDeathChargeLeadSeconds", name = "Warn early", description = "Start the reminder this many seconds before the spell can be recast (0 = when it can).", section = spellDeathChargeSection, position = 2)
    default int spellDeathChargeLeadSeconds() { return 0; }

    @ConfigItem(keyName = "spellDeathChargeRecastProtection", name = "Recast protection", description = "Deprioritize left-click Cast on Death Charge while it is active or on cooldown (right-click still casts).", section = spellDeathChargeSection, position = 1)
    default boolean spellDeathChargeRecastProtection() { return true; }

    @Alpha
    @ConfigItem(keyName = "spellDeathChargeColor", name = "Color", description = "Glow color for Death Charge.", section = spellDeathChargeSection, position = 10)
    default Color spellDeathChargeColor() { return new Color(220, 60, 90, 210); }

    @ConfigItem(keyName = "spellDeathChargeGlow", name = "Screen glow", description = "Show screen glow for Death Charge.", section = spellDeathChargeSection, position = 11)
    default boolean spellDeathChargeGlow() { return true; }

    @ConfigItem(keyName = "spellDeathChargeShower", name = "Shower icon", description = "Shower Death Charge icon.", section = spellDeathChargeSection, position = 12)
    default boolean spellDeathChargeShower() { return true; }

    @ConfigItem(keyName = "spellDeathChargeSpellbook", name = "Spellbook glow", description = "Glow Death Charge in spellbook.", section = spellDeathChargeSection, position = 13)
    default boolean spellDeathChargeSpellbook() { return true; }

    @ConfigItem(keyName = "spellDeathChargeNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when Death Charge is ready to recast.", section = spellDeathChargeSection, position = 14)
    default Notification spellDeathChargeNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellDeathChargeDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for Death Charge once it is ready, then hide until it happens again (0 = while active).", section = spellDeathChargeSection, position = 15)
    default int spellDeathChargeDurationSeconds() { return 12; }

    @ConfigItem(keyName = "debugSpellDeathCharge", name = "Debug preview", description = "Preview expired Death Charge.", section = spellDeathChargeSection, position = 19)
    default boolean debugSpellDeathCharge() { return false; }

    // =========================================================================
    // Spell: Mark of Darkness
    // =========================================================================

    @ConfigItem(keyName = "spellMarkOfDarknessEnabled", name = "Enable", description = "Track Mark of Darkness (Purging Staff aware).", section = spellMarkOfDarknessSection, position = 0)
    default boolean spellMarkOfDarknessEnabled() { return true; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellMarkOfDarknessLeadSeconds", name = "Warn early", description = "Start the reminder this many seconds before the spell can be recast (0 = when it can).", section = spellMarkOfDarknessSection, position = 2)
    default int spellMarkOfDarknessLeadSeconds() { return 0; }

    @ConfigItem(keyName = "spellMarkOfDarknessRecastProtection", name = "Recast protection", description = "Deprioritize left-click Cast on Mark of Darkness while it is active or on cooldown (off by default: some players refresh it early) (right-click still casts).", section = spellMarkOfDarknessSection, position = 1)
    default boolean spellMarkOfDarknessRecastProtection() { return false; }

    @Alpha
    @ConfigItem(keyName = "spellMarkOfDarknessColor", name = "Color", description = "Glow color for Mark of Darkness.", section = spellMarkOfDarknessSection, position = 10)
    default Color spellMarkOfDarknessColor() { return new Color(80, 175, 255, 210); }

    @ConfigItem(keyName = "spellMarkOfDarknessGlow", name = "Screen glow", description = "Show screen glow for Mark of Darkness.", section = spellMarkOfDarknessSection, position = 11)
    default boolean spellMarkOfDarknessGlow() { return true; }

    @ConfigItem(keyName = "spellMarkOfDarknessShower", name = "Shower icon", description = "Shower Mark of Darkness icon.", section = spellMarkOfDarknessSection, position = 12)
    default boolean spellMarkOfDarknessShower() { return true; }

    @ConfigItem(keyName = "spellMarkOfDarknessSpellbook", name = "Spellbook glow", description = "Glow Mark of Darkness in spellbook.", section = spellMarkOfDarknessSection, position = 13)
    default boolean spellMarkOfDarknessSpellbook() { return true; }

    @ConfigItem(keyName = "spellMarkOfDarknessNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when Mark of Darkness is ready to recast.", section = spellMarkOfDarknessSection, position = 14)
    default Notification spellMarkOfDarknessNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellMarkOfDarknessDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for Mark of Darkness once it is ready, then hide until it happens again (0 = while active).", section = spellMarkOfDarknessSection, position = 15)
    default int spellMarkOfDarknessDurationSeconds() { return 12; }

    @ConfigItem(keyName = "debugSpellMarkOfDarkness", name = "Debug preview", description = "Preview expired Mark of Darkness.", section = spellMarkOfDarknessSection, position = 19)
    default boolean debugSpellMarkOfDarkness() { return false; }

    // =========================================================================
    // Spell: Ward of Arceuus
    // =========================================================================

    @ConfigItem(keyName = "spellWardOfArceuusEnabled", name = "Enable", description = "Track Ward of Arceuus.", section = spellWardOfArceuusSection, position = 0)
    default boolean spellWardOfArceuusEnabled() { return false; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellWardOfArceuusLeadSeconds", name = "Warn early", description = "Start the reminder this many seconds before the spell can be recast (0 = when it can).", section = spellWardOfArceuusSection, position = 2)
    default int spellWardOfArceuusLeadSeconds() { return 0; }

    @ConfigItem(keyName = "spellWardOfArceuusRecastProtection", name = "Recast protection", description = "Deprioritize left-click Cast on Ward of Arceuus while it is on cooldown (right-click still casts).", section = spellWardOfArceuusSection, position = 1)
    default boolean spellWardOfArceuusRecastProtection() { return true; }

    @Alpha
    @ConfigItem(keyName = "spellWardOfArceuusColor", name = "Color", description = "Glow color for Ward of Arceuus.", section = spellWardOfArceuusSection, position = 10)
    default Color spellWardOfArceuusColor() { return new Color(56, 200, 160, 210); }

    @ConfigItem(keyName = "spellWardOfArceuusGlow", name = "Screen glow", description = "Show screen glow for Ward of Arceuus.", section = spellWardOfArceuusSection, position = 11)
    default boolean spellWardOfArceuusGlow() { return true; }

    @ConfigItem(keyName = "spellWardOfArceuusShower", name = "Shower icon", description = "Shower Ward of Arceuus icon.", section = spellWardOfArceuusSection, position = 12)
    default boolean spellWardOfArceuusShower() { return true; }

    @ConfigItem(keyName = "spellWardOfArceuusSpellbook", name = "Spellbook glow", description = "Glow Ward of Arceuus in spellbook.", section = spellWardOfArceuusSection, position = 13)
    default boolean spellWardOfArceuusSpellbook() { return true; }

    @ConfigItem(keyName = "spellWardOfArceuusNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when Ward of Arceuus is ready to recast.", section = spellWardOfArceuusSection, position = 14)
    default Notification spellWardOfArceuusNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellWardOfArceuusDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for Ward of Arceuus once it is ready, then hide until it happens again (0 = while active).", section = spellWardOfArceuusSection, position = 15)
    default int spellWardOfArceuusDurationSeconds() { return 12; }

    @ConfigItem(keyName = "debugSpellWardOfArceuus", name = "Debug preview", description = "Preview expired Ward of Arceuus.", section = spellWardOfArceuusSection, position = 19)
    default boolean debugSpellWardOfArceuus() { return false; }

    // =========================================================================
    // Spell: Shadow Veil
    // =========================================================================

    @ConfigItem(keyName = "spellShadowVeilEnabled", name = "Enable", description = "Track Shadow Veil.", section = spellShadowVeilSection, position = 0)
    default boolean spellShadowVeilEnabled() { return false; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellShadowVeilLeadSeconds", name = "Warn early", description = "Start the reminder this many seconds before the spell can be recast (0 = when it can).", section = spellShadowVeilSection, position = 3)
    default int spellShadowVeilLeadSeconds() { return 0; }

    @ConfigItem(keyName = "spellShadowVeilContext", name = "Remind when", description = "Shadow Veil reduces failed-pickpocket stuns, so by default it is only reminded while pickpocketing (within ~60 s of a Pickpocket click).", section = spellShadowVeilSection, position = 2)
    default ReminderContext spellShadowVeilContext() { return ReminderContext.THIEVING; }

    @ConfigItem(keyName = "spellShadowVeilRecastProtection", name = "Recast protection", description = "Deprioritize left-click Cast on Shadow Veil while it is active or on cooldown (right-click still casts).", section = spellShadowVeilSection, position = 1)
    default boolean spellShadowVeilRecastProtection() { return true; }

    @Alpha
    @ConfigItem(keyName = "spellShadowVeilColor", name = "Color", description = "Glow color for Shadow Veil.", section = spellShadowVeilSection, position = 10)
    default Color spellShadowVeilColor() { return new Color(140, 120, 190, 200); }

    @ConfigItem(keyName = "spellShadowVeilGlow", name = "Screen glow", description = "Show screen glow for Shadow Veil.", section = spellShadowVeilSection, position = 11)
    default boolean spellShadowVeilGlow() { return true; }

    @ConfigItem(keyName = "spellShadowVeilShower", name = "Shower icon", description = "Shower Shadow Veil icon.", section = spellShadowVeilSection, position = 12)
    default boolean spellShadowVeilShower() { return true; }

    @ConfigItem(keyName = "spellShadowVeilSpellbook", name = "Spellbook glow", description = "Glow Shadow Veil in spellbook.", section = spellShadowVeilSection, position = 13)
    default boolean spellShadowVeilSpellbook() { return true; }

    @ConfigItem(keyName = "spellShadowVeilNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when Shadow Veil is ready to recast.", section = spellShadowVeilSection, position = 14)
    default Notification spellShadowVeilNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellShadowVeilDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for Shadow Veil once it is ready, then hide until it happens again (0 = while active).", section = spellShadowVeilSection, position = 15)
    default int spellShadowVeilDurationSeconds() { return 12; }

    @ConfigItem(keyName = "debugSpellShadowVeil", name = "Debug preview", description = "Preview expired Shadow Veil.", section = spellShadowVeilSection, position = 19)
    default boolean debugSpellShadowVeil() { return false; }

    // =========================================================================
    // Spell: Corruption
    // =========================================================================

    @ConfigItem(keyName = "spellCorruptionEnabled", name = "Enable", description = "Track Lesser/Greater Corruption recast cooldown.", section = spellCorruptionSection, position = 0)
    default boolean spellCorruptionEnabled() { return false; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellCorruptionLeadSeconds", name = "Warn early", description = "Start the reminder this many seconds before the spell can be recast (0 = when it can).", section = spellCorruptionSection, position = 2)
    default int spellCorruptionLeadSeconds() { return 0; }

    @ConfigItem(keyName = "spellCorruptionRecastProtection", name = "Recast protection", description = "Deprioritize left-click Cast on Lesser/Greater Corruption while it is on cooldown (right-click still casts).", section = spellCorruptionSection, position = 1)
    default boolean spellCorruptionRecastProtection() { return true; }

    @Alpha
    @ConfigItem(keyName = "spellCorruptionColor", name = "Color", description = "Glow color for Corruption.", section = spellCorruptionSection, position = 10)
    default Color spellCorruptionColor() { return new Color(180, 70, 220, 200); }

    @ConfigItem(keyName = "spellCorruptionGlow", name = "Screen glow", description = "Show screen glow for Corruption.", section = spellCorruptionSection, position = 11)
    default boolean spellCorruptionGlow() { return true; }

    @ConfigItem(keyName = "spellCorruptionShower", name = "Shower icon", description = "Shower Corruption icon.", section = spellCorruptionSection, position = 12)
    default boolean spellCorruptionShower() { return true; }

    @ConfigItem(keyName = "spellCorruptionSpellbook", name = "Spellbook glow", description = "Glow Corruption in spellbook.", section = spellCorruptionSection, position = 13)
    default boolean spellCorruptionSpellbook() { return true; }

    @ConfigItem(keyName = "spellCorruptionNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when Corruption is ready to recast.", section = spellCorruptionSection, position = 14)
    default Notification spellCorruptionNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellCorruptionDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for Corruption once it is ready, then hide until it happens again (0 = while active).", section = spellCorruptionSection, position = 15)
    default int spellCorruptionDurationSeconds() { return 12; }

    @ConfigItem(keyName = "debugSpellCorruption", name = "Debug preview", description = "Preview expired Corruption.", section = spellCorruptionSection, position = 19)
    default boolean debugSpellCorruption() { return false; }

    // =========================================================================
    // Spell: Vengeance
    // =========================================================================

    @ConfigItem(keyName = "spellVengeanceEnabled", name = "Enable", description = "Track Vengeance recast cooldown.", section = spellVengeanceSection, position = 0)
    default boolean spellVengeanceEnabled() { return true; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellVengeanceLeadSeconds", name = "Warn early", description = "Start the reminder this many seconds before the spell can be recast (0 = when it can).", section = spellVengeanceSection, position = 2)
    default int spellVengeanceLeadSeconds() { return 0; }

    @ConfigItem(keyName = "spellVengeanceRecastProtection", name = "Recast protection", description = "Deprioritize left-click Cast on Vengeance while it is still charged or on cooldown (right-click still casts).", section = spellVengeanceSection, position = 1)
    default boolean spellVengeanceRecastProtection() { return true; }

    @Alpha
    @ConfigItem(keyName = "spellVengeanceColor", name = "Color", description = "Glow color for Vengeance.", section = spellVengeanceSection, position = 10)
    default Color spellVengeanceColor() { return new Color(245, 158, 11, 210); }

    @ConfigItem(keyName = "spellVengeanceGlow", name = "Screen glow", description = "Show screen glow for Vengeance.", section = spellVengeanceSection, position = 11)
    default boolean spellVengeanceGlow() { return true; }

    @ConfigItem(keyName = "spellVengeanceShower", name = "Shower icon", description = "Shower Vengeance icon.", section = spellVengeanceSection, position = 12)
    default boolean spellVengeanceShower() { return true; }

    @ConfigItem(keyName = "spellVengeanceSpellbook", name = "Spellbook glow", description = "Glow Vengeance in spellbook.", section = spellVengeanceSection, position = 13)
    default boolean spellVengeanceSpellbook() { return true; }

    @ConfigItem(keyName = "spellVengeanceNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when Vengeance is ready to recast.", section = spellVengeanceSection, position = 14)
    default Notification spellVengeanceNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellVengeanceDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for Vengeance once it is ready, then hide until it happens again (0 = while active).", section = spellVengeanceSection, position = 15)
    default int spellVengeanceDurationSeconds() { return 12; }

    @ConfigItem(keyName = "debugSpellVengeance", name = "Debug preview", description = "Preview expired Vengeance.", section = spellVengeanceSection, position = 19)
    default boolean debugSpellVengeance() { return false; }

    // =========================================================================
    // Spell: Vengeance Other
    // =========================================================================

    @ConfigItem(keyName = "spellVengeanceOtherEnabled", name = "Enable", description = "Track Vengeance Other recast cooldown.", section = spellVengeanceOtherSection, position = 0)
    default boolean spellVengeanceOtherEnabled() { return false; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellVengeanceOtherLeadSeconds", name = "Warn early", description = "Start the reminder this many seconds before the spell can be recast (0 = when it can).", section = spellVengeanceOtherSection, position = 2)
    default int spellVengeanceOtherLeadSeconds() { return 0; }

    @ConfigItem(keyName = "spellVengeanceOtherRecastProtection", name = "Recast protection", description = "Deprioritize left-click Cast on Vengeance Other while it is on cooldown (right-click still casts).", section = spellVengeanceOtherSection, position = 1)
    default boolean spellVengeanceOtherRecastProtection() { return true; }

    @Alpha
    @ConfigItem(keyName = "spellVengeanceOtherColor", name = "Color", description = "Glow color for Vengeance Other.", section = spellVengeanceOtherSection, position = 10)
    default Color spellVengeanceOtherColor() { return new Color(235, 190, 60, 200); }

    @ConfigItem(keyName = "spellVengeanceOtherGlow", name = "Screen glow", description = "Show screen glow for Vengeance Other.", section = spellVengeanceOtherSection, position = 11)
    default boolean spellVengeanceOtherGlow() { return true; }

    @ConfigItem(keyName = "spellVengeanceOtherShower", name = "Shower icon", description = "Shower Vengeance Other icon.", section = spellVengeanceOtherSection, position = 12)
    default boolean spellVengeanceOtherShower() { return true; }

    @ConfigItem(keyName = "spellVengeanceOtherSpellbook", name = "Spellbook glow", description = "Glow Vengeance Other in spellbook.", section = spellVengeanceOtherSection, position = 13)
    default boolean spellVengeanceOtherSpellbook() { return true; }

    @ConfigItem(keyName = "spellVengeanceOtherNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when Vengeance Other is ready to recast.", section = spellVengeanceOtherSection, position = 14)
    default Notification spellVengeanceOtherNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellVengeanceOtherDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for Vengeance Other once it is ready, then hide until it happens again (0 = while active).", section = spellVengeanceOtherSection, position = 15)
    default int spellVengeanceOtherDurationSeconds() { return 12; }

    @ConfigItem(keyName = "debugSpellVengeanceOther", name = "Debug preview", description = "Preview expired Vengeance Other.", section = spellVengeanceOtherSection, position = 19)
    default boolean debugSpellVengeanceOther() { return false; }

    // =========================================================================
    // Spell: Charge
    // =========================================================================

    @ConfigItem(keyName = "spellChargeEnabled", name = "Enable", description = "Track Charge (God Spells) expiry.", section = spellChargeSection, position = 0)
    default boolean spellChargeEnabled() { return false; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellChargeLeadSeconds", name = "Warn early", description = "Start the reminder this many seconds before the spell can be recast (0 = when it can).", section = spellChargeSection, position = 2)
    default int spellChargeLeadSeconds() { return 0; }

    @ConfigItem(keyName = "spellChargeNeedsGodCape", name = "Only with a god cape", description = "Charge only boosts god spells cast while wearing a god cape, so only remind while one is worn.", section = spellChargeSection, position = 1)
    default boolean spellChargeNeedsGodCape() { return true; }

    @Alpha
    @ConfigItem(keyName = "spellChargeColor", name = "Color", description = "Glow color for Charge.", section = spellChargeSection, position = 10)
    default Color spellChargeColor() { return new Color(250, 204, 21, 200); }

    @ConfigItem(keyName = "spellChargeGlow", name = "Screen glow", description = "Show screen glow for Charge.", section = spellChargeSection, position = 11)
    default boolean spellChargeGlow() { return true; }

    @ConfigItem(keyName = "spellChargeShower", name = "Shower icon", description = "Shower Charge icon.", section = spellChargeSection, position = 12)
    default boolean spellChargeShower() { return true; }

    @ConfigItem(keyName = "spellChargeSpellbook", name = "Spellbook glow", description = "Glow Charge in spellbook.", section = spellChargeSection, position = 13)
    default boolean spellChargeSpellbook() { return true; }

    @ConfigItem(keyName = "spellChargeNotification", name = "Notification", description = "RuneLite notification (sound / tray / flash) when Charge is ready to recast.", section = spellChargeSection, position = 14)
    default Notification spellChargeNotification() { return Notification.OFF; }

    @Range(min = 0, max = 120)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "spellChargeDurationSeconds", name = "Reminder duration", description = "Seconds to show the screen glow and icon shower for Charge once it is ready, then hide until it happens again (0 = while active).", section = spellChargeSection, position = 15)
    default int spellChargeDurationSeconds() { return 12; }

    @ConfigItem(keyName = "debugSpellCharge", name = "Debug preview", description = "Preview expired Charge.", section = spellChargeSection, position = 19)
    default boolean debugSpellCharge() { return false; }


    // =========================================================================
    // Potion Reminders (per family: remind toggle + expiring-soon lead)
    // =========================================================================

    @ConfigItem(keyName = "potionRemindersEnabled", name = "Enable", description = "Show expiry reminders for the potion families below (timers and re-dose protection keep working either way).", section = potionRemindersSection, position = 0)
    default boolean potionRemindersEnabled() { return true; }

    @ConfigItem(keyName = "remindRaidBoosts", name = "Overload / smelling salts", description = "Remind when an overload or smelling salts boost expires (needs Track raid consumables).", section = potionRemindersSection, position = 9)
    default boolean remindRaidBoosts() { return true; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "staminaLeadSeconds", name = "Stamina: warn early", description = "Start the stamina reminder this many seconds before it runs out (0 = when it runs out).", section = potionRemindersSection, position = 2)
    default int staminaLeadSeconds() { return 0; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "antipoisonLeadSeconds", name = "Antipoison: warn early", description = "Start the antipoison / anti-venom reminder this many seconds before the protection ends.", section = potionRemindersSection, position = 4)
    default int antipoisonLeadSeconds() { return 0; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "antifireLeadSeconds", name = "Antifire: warn early", description = "Start the antifire reminder this many seconds before the protection ends.", section = potionRemindersSection, position = 6)
    default int antifireLeadSeconds() { return 0; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "raidBoostLeadSeconds", name = "Overload / salts: warn early", description = "Start the overload / smelling salts reminder this many seconds before it expires.", section = potionRemindersSection, position = 20)
    default int raidBoostLeadSeconds() { return 0; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "surgeLeadSeconds", name = "Surge: warn early", description = "Start the surge reminder this many seconds before its cooldown ends.", section = potionRemindersSection, position = 22)
    default int surgeLeadSeconds() { return 0; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "goadingLeadSeconds", name = "Goading: warn early", description = "Start the goading reminder this many seconds before it expires.", section = potionRemindersSection, position = 24)
    default int goadingLeadSeconds() { return 0; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "prayerRegenerationLeadSeconds", name = "Prayer regeneration: warn early", description = "Start the prayer regeneration reminder this many seconds before it expires.", section = potionRemindersSection, position = 26)
    default int prayerRegenerationLeadSeconds() { return 0; }

    @Range(min = 0, max = 60)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "otherPotionLeadSeconds", name = "Other: warn early", description = "Start reminders for the other potions this many seconds before they expire.", section = potionRemindersSection, position = 28)
    default int otherPotionLeadSeconds() { return 0; }

    // =========================================================================
    // Static Helpers (Safe with RuneLite ConfigInvocationHandler)
    // =========================================================================

    /** Every debug/preview key; reset on startup so none survives a restart. */
    String[] DEBUG_KEYS = {
        "debugSimulateCombat",
        "debugTriggerSpellCooldowns",
        "debugTriggerPotionCooldowns",
        "debugPreviewInventory",
        "debugPreviewSpellGrid",
        "debugLowHealth",
        "debugDamageFlash",
        "debugLowPrayer",
        "debugPoison",
        "debugVenom",
        "debugDisease",
        "debugFreeze",
        "debugIdle",
        "debugSpecReady",
        "debugLowRun",
        "debugUnpotted",
        "debugHeartReady",
        "debugBuffExpired",
        "debugSpellThrall",
        "debugSpellDeathCharge",
        "debugSpellMarkOfDarkness",
        "debugSpellWardOfArceuus",
        "debugSpellShadowVeil",
        "debugSpellCorruption",
        "debugSpellVengeance",
        "debugSpellVengeanceOther",
        "debugSpellCharge",
    };
}
