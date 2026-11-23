package com.combatauras.settings;

import com.combatauras.config.CombatAurasConfig;
import com.combatauras.model.AnimationMode;
import com.combatauras.model.BorderStyle;
import com.combatauras.model.CombatSpell;
import com.combatauras.model.DisplayMode;
import com.combatauras.model.GlowAnimation;
import com.combatauras.model.GlowColorMode;
import com.combatauras.model.PotionEffect;
import com.combatauras.model.ReminderContext;
import com.combatauras.model.ReminderFamily;
import com.combatauras.model.ReadyStyle;
import com.combatauras.model.ShowerStartLocation;
import com.combatauras.model.SpellCooldownDisplayMode;
import com.combatauras.model.StatusEffect;
import java.awt.Color;
import net.runelite.api.Skill;
import net.runelite.client.config.Notification;

/**
 * Immutable view of every setting the per-tick and per-frame code reads, built once per config change.
 * <p>
 * Hot paths index plain arrays by enum ordinal instead of calling config proxy methods, and the global
 * "allow screen glows / icon showers / item glows" switches are already folded into the per-entry flags.
 * Arrays are never written after construction; share the snapshot freely.
 */
public final class SettingsSnapshot
{
    private static final int SPELLS = CombatSpell.values().length;
    private static final int STATUSES = StatusEffect.values().length;
    private static final int FAMILIES = ReminderFamily.values().length;
    private static final int EFFECTS = PotionEffect.values().length;

    // ---- general display
    public final boolean allowScreenGlows;
    public final boolean allowIconShowers;
    public final boolean allowItemGlows;
    public final float effectIntensity;
    public final float edgeDepth;
    public final float screenTint;
    public final float motionStrength;
    public final GlowColorMode glowColorMode;
    public final boolean glowTop;
    public final boolean glowBottom;
    public final boolean glowLeft;
    public final boolean glowRight;
    public final float previewIntensity;

    // ---- icon shower
    public final ShowerStartLocation showerStartLocation;
    public final float showerScreenFraction;
    public final int showerMaxIcons;
    public final int showerIconSize;
    public final float showerSpeed;
    public final float showerOpacity;

    // ---- item & spellbook icons (drawn every frame per visible item; "allow item glows" is folded in)
    /** Multiplier for icon glow alpha, 0.25..2 (above 1 draws a second layer). */
    public final float iconGlowStrength;
    public final DisplayMode cooldownStyle;
    public final Color cooldownTint;
    public final boolean showCooldownTimer;
    public final ReadyStyle readyStyle;
    public final boolean readyPermanent;
    public final int readyDurationSeconds;
    public final Color readyColor;
    /** Inventory ready border, NONE when item glows are off. */
    public final BorderStyle readyBorder;
    public final boolean foodCooldownsEnabled;
    public final boolean lowPrayerInventoryGlow;
    public final Color lowPrayerColor;
    public final BorderStyle spellBorder;
    public final GlowAnimation spellGlowAnimation;
    public final GlowAnimation specOrbAnimation;
    public final GlowAnimation lowPrayerGlowAnimation;
    public final SpellCooldownDisplayMode spellCooldownStyle;
    public final Color spellCooldownTint;
    public final boolean spellbookTabGlow;
    public final boolean specOrbGlow;
    public final BorderStyle specOrbStyle;
    public final Color specColor;
    public final boolean debugPreviewInventory;
    public final boolean debugPreviewSpellGrid;
    public final boolean debugSpecReady;

    // ---- damage flash
    public final float damageFlashScale;
    public final long damageFlashNanos;

    // ---- warnings (index: StatusEffect.ordinal())
    private final boolean[] statusEnabled = new boolean[STATUSES];
    private final int[] statusColor = new int[STATUSES];
    private final boolean[] statusGlow = new boolean[STATUSES];
    private final boolean[] statusShower = new boolean[STATUSES];
    private final int[] statusDurationSeconds = new int[STATUSES];
    private final Notification[] statusNotification = new Notification[STATUSES];
    private final boolean[] statusDebug = new boolean[STATUSES];
    /** Heartbeat pulse strength 0..1 (Low Health's "Heartbeat pulse", and the same option on every other glow). */
    private final float[] statusPulse = new float[STATUSES];

    // ---- spells (index: CombatSpell.ordinal())
    public final boolean spellsEnabled;
    public final boolean spellOnlyInCombat;
    public final boolean spellOnlyRemindAfterCast;
    public final boolean thrallRemindOnCombatStart;
    public final ReminderContext shadowVeilContext;
    public final boolean chargeNeedsGodCape;
    public final int combatBreakReRemindSeconds;
    private final boolean[] spellEnabled = new boolean[SPELLS];
    private final int[] spellColor = new int[SPELLS];
    /** Same colours as {@link #spellColor}, as Color objects so render code never allocates one. */
    private final Color[] spellAwtColor = new Color[SPELLS];
    private final boolean[] spellGlow = new boolean[SPELLS];
    private final boolean[] spellShower = new boolean[SPELLS];
    private final boolean[] spellbookGlow = new boolean[SPELLS];
    private final Notification[] spellNotification = new Notification[SPELLS];
    private final int[] spellDurationSeconds = new int[SPELLS];
    private final int[] spellLeadTicks = new int[SPELLS];
    private final boolean[] spellRecastProtection = new boolean[SPELLS];
    private final boolean[] spellDebug = new boolean[SPELLS];
    private final float[] spellPulse = new float[SPELLS];

    // ---- item reminder families (index: ReminderFamily.ordinal())
    private final boolean[] familyEnabled = new boolean[FAMILIES];
    private final int[] familyColor = new int[FAMILIES];
    private final boolean[] familyGlow = new boolean[FAMILIES];
    private final boolean[] familyShower = new boolean[FAMILIES];
    private final Notification[] familyNotification = new Notification[FAMILIES];
    private final int[] familyDurationSeconds = new int[FAMILIES];
    private final boolean[] familyOnlyInCombat = new boolean[FAMILIES];
    private final int[] familyLeadTicks = new int[FAMILIES];
    private final boolean[] familyDebug = new boolean[FAMILIES];
    private final float[] familyPulse = new float[FAMILIES];

    // ---- potion effects (index: PotionEffect.ordinal())
    private final boolean[] potionRemind = new boolean[EFFECTS];
    private final int[] potionLeadTicks = new int[EFFECTS];
    public final boolean potionCooldownsEnabled;
    public final boolean potionEffectsEnabled;
    public final boolean raidPotionsEnabled;

    // ---- boosts (index: Skill.ordinal())
    private final int[] redoseLevel = new int[Skill.values().length];

    /** Any debug preview is on (live warnings are hidden while previewing). */
    public final boolean anyDebugPreview;

    private SettingsSnapshot(CombatAurasConfig c)
    {
        allowScreenGlows = c.allowScreenGlows();
        allowIconShowers = c.allowIconShowers();
        allowItemGlows = c.allowItemGlows();
        effectIntensity = c.effectIntensity() / 100f;
        edgeDepth = c.edgeDepth() / 100f;
        screenTint = c.screenTint() / 100f;
        motionStrength = c.motionStrength() / 100f;
        glowColorMode = c.glowColorMode();
        glowTop = c.glowTop();
        glowBottom = c.glowBottom();
        glowLeft = c.glowLeft();
        glowRight = c.glowRight();
        previewIntensity = c.previewIntensity() / 100f;

        showerStartLocation = c.showerStartLocation();
        showerScreenFraction = Math.max(0.10f, Math.min(1.0f, c.showerScreenPercent() / 100f));
        showerMaxIcons = c.spellShowerMaxIcons();
        showerIconSize = c.spellShowerIconSize();
        showerSpeed = c.spellShowerSpeed() / 100f;
        showerOpacity = Math.max(0.10f, Math.min(1f, c.showerOpacity() / 100f));

        iconGlowStrength = Math.max(0.25f, Math.min(2f, c.iconGlowStrength() / 100f));
        cooldownStyle = c.displayMode();
        cooldownTint = c.cooldownOverlayColor();
        showCooldownTimer = c.showCooldownTimer();
        readyStyle = c.readyStyle();
        readyPermanent = c.animationMode() == AnimationMode.PERMANENT;
        readyDurationSeconds = c.readyDurationSeconds();
        readyColor = c.readyColor();
        readyBorder = allowItemGlows ? c.borderStyle() : BorderStyle.NONE;
        foodCooldownsEnabled = c.foodCooldownsEnabled();
        lowPrayerInventoryGlow = allowItemGlows && c.lowPrayerInventoryGlow();
        lowPrayerColor = c.lowPrayerColor();
        spellBorder = c.spellBorderStyle();
        spellGlowAnimation = c.spellGlowAnimation();
        specOrbAnimation = c.specOrbAnimation();
        lowPrayerGlowAnimation = c.lowPrayerGlowAnimation();
        spellCooldownStyle = c.spellCooldownDisplayMode();
        spellCooldownTint = c.spellCooldownOverlayColor();
        spellbookTabGlow = allowItemGlows && c.spellsEnabled() && c.spellbookTabGlow();
        specOrbGlow = allowItemGlows && c.specOrbGlow();
        specOrbStyle = c.specOrbStyle();
        specColor = c.specColor();
        debugPreviewInventory = c.debugPreviewInventory();
        debugPreviewSpellGrid = allowItemGlows && c.debugPreviewSpellGrid();
        debugSpecReady = c.debugSpecReady();

        damageFlashScale = c.damageFeedbackStrength() / 100f;
        damageFlashNanos = c.damageFeedbackDuration() * 1_000_000L;

        boolean debug = false;
        for (StatusEffect s : StatusEffect.values())
        {
            Bindings.Status b = Bindings.STATUSES.get(s);
            int i = s.ordinal();
            statusEnabled[i] = b.enabled.test(c);
            statusColor[i] = b.color.apply(c).getRGB();
            statusGlow[i] = allowScreenGlows && b.glow.test(c);
            statusShower[i] = allowIconShowers && b.shower.test(c);
            statusDurationSeconds[i] = b.durationSeconds.applyAsInt(c);
            statusNotification[i] = b.notification.apply(c);
            statusDebug[i] = b.debug.test(c);
            statusPulse[i] = b.pulse.applyAsInt(c) / 100f;
            debug |= statusDebug[i];
        }

        spellsEnabled = c.spellsEnabled();
        spellOnlyInCombat = c.spellOnlyInCombat();
        spellOnlyRemindAfterCast = c.spellOnlyRemindAfterCast();
        thrallRemindOnCombatStart = c.thrallRemindOnCombatStart();
        shadowVeilContext = c.spellShadowVeilContext();
        chargeNeedsGodCape = c.spellChargeNeedsGodCape();
        combatBreakReRemindSeconds = c.combatBreakReRemindSeconds();
        boolean masterGlow = allowScreenGlows && c.spellScreenGlow();
        boolean masterShower = allowIconShowers && c.spellShowerIcons();
        boolean masterBook = allowItemGlows && c.spellGlowInSpellbook();
        for (CombatSpell s : CombatSpell.values())
        {
            Bindings.Spell b = Bindings.SPELLS.get(s);
            int i = s.ordinal();
            spellEnabled[i] = b.enabled.test(c);
            spellAwtColor[i] = b.color.apply(c);
            spellColor[i] = spellAwtColor[i].getRGB();
            spellGlow[i] = masterGlow && b.glow.test(c);
            spellShower[i] = masterShower && b.shower.test(c);
            spellbookGlow[i] = masterBook && b.spellbook.test(c);
            spellNotification[i] = b.notification.apply(c);
            spellDurationSeconds[i] = b.durationSeconds.applyAsInt(c);
            spellLeadTicks[i] = secondsToTicks(b.leadSeconds.applyAsInt(c));
            spellRecastProtection[i] = b.recastProtection.test(c);
            spellDebug[i] = b.debug.test(c);
            spellPulse[i] = b.pulse.applyAsInt(c) / 100f;
            debug |= spellDebug[i];
        }

        for (ReminderFamily f : ReminderFamily.values())
        {
            Bindings.Family b = Bindings.FAMILIES.get(f);
            int i = f.ordinal();
            familyEnabled[i] = b.enabled.test(c);
            familyColor[i] = b.color.apply(c).getRGB();
            familyGlow[i] = allowScreenGlows && b.glow.test(c);
            familyShower[i] = allowIconShowers && b.shower.test(c);
            familyNotification[i] = b.notification.apply(c);
            familyDurationSeconds[i] = b.durationSeconds.applyAsInt(c);
            familyOnlyInCombat[i] = b.onlyInCombat.test(c);
            familyLeadTicks[i] = secondsToTicks(b.leadSeconds.applyAsInt(c));
            familyDebug[i] = b.debug.test(c);
            familyPulse[i] = b.pulse.applyAsInt(c) / 100f;
            debug |= familyDebug[i];
        }
        anyDebugPreview = debug;

        potionCooldownsEnabled = c.potionCooldownsEnabled();
        potionEffectsEnabled = c.potionEffectsEnabled();
        raidPotionsEnabled = c.raidPotionsEnabled();
        for (PotionEffect e : PotionEffect.values())
        {
            Bindings.Potion b = Bindings.POTIONS.get(e.getFamily());
            potionRemind[e.ordinal()] = isTracked(e) && b.remind.test(c);
            potionLeadTicks[e.ordinal()] = secondsToTicks(b.leadSeconds.applyAsInt(c));
        }

        redoseLevel[Skill.ATTACK.ordinal()] = c.boostRedoseAttack();
        redoseLevel[Skill.STRENGTH.ordinal()] = c.boostRedoseStrength();
        redoseLevel[Skill.DEFENCE.ordinal()] = c.boostRedoseDefence();
        redoseLevel[Skill.RANGED.ordinal()] = c.boostRedoseRanged();
        redoseLevel[Skill.MAGIC.ordinal()] = c.boostRedoseMagic();
    }

    public static SettingsSnapshot of(CombatAurasConfig config)
    {
        return new SettingsSnapshot(config);
    }

    /** Whether this effect's timer is tracked at all (its category toggle in the Potions section). */
    public boolean isTracked(PotionEffect effect)
    {
        switch (effect.getCategory())
        {
            case COOLDOWN:
                return potionCooldownsEnabled;
            case RAID:
                return raidPotionsEnabled;
            default:
                return potionEffectsEnabled;
        }
    }

    public int leadTicks(PotionEffect effect)
    {
        return potionLeadTicks[effect.ordinal()];
    }

    public int redoseLevel(Skill skill)
    {
        return redoseLevel[skill.ordinal()];
    }

    public boolean familyEnabled(ReminderFamily family)
    {
        return familyEnabled[family.ordinal()];
    }

    /** Settings are in seconds; timers are in 0.6 s game ticks. */
    public static int secondsToTicks(int seconds)
    {
        return Math.round(Math.max(0, seconds) / 0.6f);
    }

    // ---- read-only accessors (the arrays never leave this class, so a snapshot cannot be modified)

    public boolean statusEnabled(int index)
    {
        return statusEnabled[index];
    }

    public int statusColor(int index)
    {
        return statusColor[index];
    }

    public boolean statusGlow(int index)
    {
        return statusGlow[index];
    }

    public boolean statusShower(int index)
    {
        return statusShower[index];
    }

    public int statusDurationSeconds(int index)
    {
        return statusDurationSeconds[index];
    }

    public Notification statusNotification(int index)
    {
        return statusNotification[index];
    }

    public boolean statusDebug(int index)
    {
        return statusDebug[index];
    }

    public boolean spellEnabled(int index)
    {
        return spellEnabled[index];
    }

    public int spellColor(int index)
    {
        return spellColor[index];
    }

    public Color spellAwtColor(int index)
    {
        return spellAwtColor[index];
    }

    public boolean spellGlow(int index)
    {
        return spellGlow[index];
    }

    public boolean spellShower(int index)
    {
        return spellShower[index];
    }

    public boolean spellbookGlow(int index)
    {
        return spellbookGlow[index];
    }

    public Notification spellNotification(int index)
    {
        return spellNotification[index];
    }

    public int spellDurationSeconds(int index)
    {
        return spellDurationSeconds[index];
    }

    public int spellLeadTicks(int index)
    {
        return spellLeadTicks[index];
    }

    public boolean spellRecastProtection(int index)
    {
        return spellRecastProtection[index];
    }

    public boolean spellDebug(int index)
    {
        return spellDebug[index];
    }

    public boolean familyEnabled(int index)
    {
        return familyEnabled[index];
    }

    public int familyColor(int index)
    {
        return familyColor[index];
    }

    public boolean familyGlow(int index)
    {
        return familyGlow[index];
    }

    public boolean familyShower(int index)
    {
        return familyShower[index];
    }

    public Notification familyNotification(int index)
    {
        return familyNotification[index];
    }

    public int familyDurationSeconds(int index)
    {
        return familyDurationSeconds[index];
    }

    public boolean familyOnlyInCombat(int index)
    {
        return familyOnlyInCombat[index];
    }

    public int familyLeadTicks(int index)
    {
        return familyLeadTicks[index];
    }

    public boolean familyDebug(int index)
    {
        return familyDebug[index];
    }

    public boolean potionRemind(int index)
    {
        return potionRemind[index];
    }

    public int potionLeadTicks(int index)
    {
        return potionLeadTicks[index];
    }

    public int redoseLevel(int index)
    {
        return redoseLevel[index];
    }

    public float statusPulse(int index)
    {
        return statusPulse[index];
    }

    public float spellPulse(int index)
    {
        return spellPulse[index];
    }

    public float familyPulse(int index)
    {
        return familyPulse[index];
    }
}
