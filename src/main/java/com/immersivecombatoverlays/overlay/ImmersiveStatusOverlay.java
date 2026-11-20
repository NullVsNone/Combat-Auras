package com.immersivecombatoverlays.overlay;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.CombatSpell;
import com.immersivecombatoverlays.model.StatusEffect;
import com.immersivecombatoverlays.render.ProceduralRenderer;
import com.immersivecombatoverlays.model.BoostPotion;
import com.immersivecombatoverlays.model.PotionEffect;
import com.immersivecombatoverlays.tracker.BoostPotionTimers;
import com.immersivecombatoverlays.tracker.PotionItems;
import java.util.EnumSet;
import com.immersivecombatoverlays.tracker.CombatSpellTracker;
import com.immersivecombatoverlays.tracker.CombatStateTracker;
import com.immersivecombatoverlays.tracker.FreezeTracker;
import com.immersivecombatoverlays.tracker.HeartCooldownTracker;
import com.immersivecombatoverlays.tracker.IdleTracker;
import com.immersivecombatoverlays.tracker.UnpottedTracker;
import com.immersivecombatoverlays.tracker.ParasiteTracker;
import com.immersivecombatoverlays.tracker.ExpiryReminders;
import net.runelite.api.gameval.ItemID;
import com.immersivecombatoverlays.tracker.ImpactTracker;
import com.immersivecombatoverlays.tracker.PotionTimers;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.inject.Named;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Full-screen procedural status, idle, spec, buff, and spell reminder overlay.
 * Client state is sampled strictly on the Client thread (onGameTick) so render(Graphics2D)
 * never triggers client-thread assertion errors under -ea.
 */
@Singleton
public class ImmersiveStatusOverlay extends Overlay
{
    private static final int SPRITE_SKILL_PRAYER = 201;
    private static final int SPRITE_SKILL_HITPOINTS = 203;
    private static final int SPRITE_ICE_BARRAGE = 328;
    private static final int SPRITE_RUN_ORB = 1070;
    private static final int SPRITE_IDLE_DREAM = 580;
    private static final int SPRITE_DAMAGE_HITSPLAT = 1359;
    private static final int SPRITE_POISON_HITSPLAT = 1360;
    private static final int SPRITE_DISEASE_HITSPLAT = 1361;
    private static final int ITEM_ANTIVENOM_PLUS = 12913;
    private static final int SPRITE_SPEC_SWORDS = 168;
    private static final int SPRITE_STATS_BARS = 898;

    private final Client client;
    private final ImmersiveCombatOverlaysConfig config;
    private final SpriteManager spriteManager;
    private final ItemManager itemManager;
    private final HeartCooldownTracker heartTracker;
    private final PotionTimers potionTimers;
    private final BoostPotionTimers boostTimers;
    private final ProceduralRenderer renderer;
    private final CombatStateTracker combatState;
    private final CombatSpellTracker spellTracker;
    private final ImpactTracker damageTracker;
    private final ImpactTracker poisonTickTracker;
    private final ImpactTracker venomTickTracker;
    private final FreezeTracker freezeTracker;
    private final IdleTracker idleTracker;
    private final UnpottedTracker unpottedTracker;
    private final ParasiteTracker parasiteTracker;
    private volatile boolean liveBuffExpired = false;
    private volatile boolean liveBoostReady = false;
    private volatile boolean liveHeartReady = false;
    private static final int KIND_HEART = 0;
    private static final int KIND_POTION = 1;
    private static final int KIND_BOOST = 2;
    private final int[] liveBuffItemKinds = new int[16];
    private final int[] readyItemKinds = new int[16];
    private final long[] statusActiveSince = new long[StatusEffect.values().length];
    private final ExpiryReminders expiryReminders;
    private boolean frameDebugPreview = false;

    // Client-thread-sampled live values (safe to read on render thread)
    private volatile int cachedSpellbook = 0;
    private volatile float liveLowHealthSev = 0f;
    private volatile float liveLowPrayerSev = 0f;
    private volatile float livePoisonSev = 0f;
    private volatile float liveVenomSev = 0f;
    private volatile float liveDiseaseSev = 0f;
    private volatile float liveSpecSev = 0f;
    private volatile float liveLowRunSev = 0f;
    private volatile int liveBuffItemCount = 0;
    private final int[] liveBuffItemIds = new int[16];
    private final EnumSet<PotionEffect> seenEffects = EnumSet.noneOf(PotionEffect.class);
    private final EnumSet<BoostPotion> seenBoosts = EnumSet.noneOf(BoostPotion.class);
    private final int[] liveBuffItemColors = new int[16];

    // Render-thread reusable primitive buffers (zero per-frame heap allocations)
    private final float[] statusSeverities = new float[StatusEffect.values().length];
    private final float[] rawStatusSeverities = new float[StatusEffect.values().length];
    private final int[] statusColors = new int[StatusEffect.values().length];
    private final int[] readySpellOrdinals = new int[CombatSpell.values().length];
    private final int[] readySpellColors = new int[CombatSpell.values().length];
    private final int[] showerSpriteIds = new int[CombatSpell.values().length + 14];
    private final int[] showerSpriteColors = new int[CombatSpell.values().length + 14];
    private final int[] glowExtraColors = new int[CombatSpell.values().length + 16];
    private final int[] readyItemIds = new int[16];
    private final int[] readyItemColors = new int[16];

    @Inject
    public ImmersiveStatusOverlay(
        Client client,
        ImmersiveCombatOverlaysConfig config,
        SpriteManager spriteManager,
        ItemManager itemManager,
        HeartCooldownTracker heartTracker,
        PotionTimers potionTimers,
        BoostPotionTimers boostTimers,
        ProceduralRenderer renderer,
        CombatStateTracker combatState,
        CombatSpellTracker spellTracker,
        @Named("damage") ImpactTracker damageTracker,
        @Named("poisonTick") ImpactTracker poisonTickTracker,
        @Named("venomTick") ImpactTracker venomTickTracker,
        FreezeTracker freezeTracker,
        IdleTracker idleTracker,
        UnpottedTracker unpottedTracker,
        ParasiteTracker parasiteTracker,
        ExpiryReminders expiryReminders
    )
    {
        this.client = client;
        this.config = config;
        this.spriteManager = spriteManager;
        this.itemManager = itemManager;
        this.heartTracker = heartTracker;
        this.potionTimers = potionTimers;
        this.boostTimers = boostTimers;
        this.renderer = renderer;
        this.combatState = combatState;
        this.spellTracker = spellTracker;
        this.damageTracker = damageTracker;
        this.poisonTickTracker = poisonTickTracker;
        this.venomTickTracker = venomTickTracker;
        this.freezeTracker = freezeTracker;
        this.idleTracker = idleTracker;
        this.unpottedTracker = unpottedTracker;
        this.parasiteTracker = parasiteTracker;
        this.expiryReminders = expiryReminders;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.UNDER_WIDGETS);
        setPriority(PRIORITY_LOW);
    }

    /**
     * Called on the Client thread (onGameTick) to safely sample varbits, varps, skills, prayers, and inventory.
     */
    public void updateClientState()
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return;
        }

        cachedSpellbook = client.getVarbitValue(VarbitID.SPELLBOOK);

        // Low Health
        float hpSev = 0f;
        if (config.lowHealthEnabled())
        {
            int maxHp = Math.max(1, client.getRealSkillLevel(Skill.HITPOINTS));
            int curHp = client.getBoostedSkillLevel(Skill.HITPOINTS);
            float pct = (curHp * 100f) / maxHp;
            int thresh = config.lowHealthThreshold();
            if (pct <= thresh && curHp > 0)
            {
                float raw = (thresh - pct) / Math.max(1f, thresh);
                hpSev = Math.min(1f, 0.25f + raw * (0.75f + 0.35f * (config.lowHealthEscalation() / 100f)));
            }
        }
        liveLowHealthSev = hpSev;

        // Low Prayer
        float praySev = 0f;
        if (config.lowPrayerEnabled() && (!config.prayerActiveOnly() || isAnyPrayerActive()))
        {
            int maxPray = Math.max(1, client.getRealSkillLevel(Skill.PRAYER));
            int curPray = client.getBoostedSkillLevel(Skill.PRAYER);
            float pct = (curPray * 100f) / maxPray;
            int thresh = config.lowPrayerThreshold();
            if (pct <= thresh)
            {
                float raw = (thresh - pct) / Math.max(1f, thresh);
                praySev = Math.min(1f, 0.25f + raw * (0.75f + 0.35f * (config.lowPrayerEscalation() / 100f)));
            }
        }
        liveLowPrayerSev = praySev;

        // Poison & Venom
        int poisonVar = client.getVarpValue(VarPlayerID.POISON);
        liveVenomSev = (poisonVar >= 1_000_000 && config.venomEnabled()) ? 0.75f : 0f;
        livePoisonSev = (poisonVar > 0 && poisonVar < 1_000_000 && config.poisonEnabled()) ? 0.65f : 0f;

        // Disease
        liveDiseaseSev = (config.diseaseEnabled() && client.getVarpValue(VarPlayerID.DISEASE) > 0) ? 0.65f : 0f;

        // Special Attack
        float specSev = 0f;
        if (config.specEnabled() && (!config.specOnlyInCombat() || combatState.isInCombat()))
        {
            int specPct = client.getVarpValue(VarPlayerID.SA_ENERGY) / 10;
            if (specPct >= config.specThresholdPercent())
            {
                specSev = 0.65f;
            }
        }
        liveSpecSev = specSev;

        // Low Run Energy (client.getEnergy() is 0..10000)
        float runSev = 0f;
        if (config.lowRunEnabled() && (!config.lowRunOnlyInCombat() || combatState.isInCombat()))
        {
            int runPct = client.getEnergy() / 100;
            int thresh = config.lowRunThreshold();
            if (runPct <= thresh)
            {
                float raw = (thresh - runPct) / (float) Math.max(1, thresh);
                runSev = Math.min(1f, 0.30f + raw * 0.70f);
            }
        }
        liveLowRunSev = runSev;

        // Ready Heart & Buff Items in inventory
        sampleInventoryBuffItems();
    }

    private void sampleInventoryBuffItems()
    {
        liveBuffExpired = false;
        liveBoostReady = false;
        liveHeartReady = false;
        liveBuffItemCount = 0;

        ItemContainer inv = client.getItemContainer(InventoryID.INV);
        if (inv == null)
        {
            return;
        }

        long now = System.nanoTime();
        boolean inCombat = combatState.isInCombat();
        boolean heartAllowed = config.heartsEnabled() && (!config.heartOnlyInCombat() || inCombat);
        boolean potionAllowed = !config.buffOnlyInCombat() || inCombat;
        boolean boostAllowed = config.boostPotionsEnabled() && (!config.boostOnlyInCombat() || inCombat);
        int count = 0;
        boolean addedHeart = false;
        seenEffects.clear();
        seenBoosts.clear();

        // Reminders come only from the expiry latch: an item merely sitting in the inventory never triggers one
        for (Item item : inv.getItems())
        {
            if (item == null || item.getId() <= 0 || count >= liveBuffItemIds.length)
            {
                continue;
            }
            int id = item.getId();
            if (HeartCooldownTracker.isHeart(id))
            {
                if (!addedHeart && heartAllowed
                    && expiryReminders.isShowing(ExpiryReminders.HEART, config.heartDurationSeconds(), now))
                {
                    addItem(count++, id, config.heartReminderColor().getRGB(), KIND_HEART);
                    addedHeart = true;
                    liveHeartReady = true;
                }
                continue;
            }

            ItemComposition comp = itemManager.getItemComposition(id);
            String name = comp != null ? comp.getName() : null;
            PotionEffect eff = PotionItems.getEffect(id, name);
            if (eff != null)
            {
                if (potionAllowed && !seenEffects.contains(eff)
                    && ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(config, eff)
                    && expiryReminders.isShowing(ExpiryReminders.key(eff), config.buffDurationSeconds(), now))
                {
                    seenEffects.add(eff);
                    addItem(count++, id, config.buffReminderColor().getRGB(), KIND_POTION);
                    liveBuffExpired = true;
                }
                continue;
            }

            BoostPotion boost = PotionItems.getBoost(id, name);
            if (boost != null && boostAllowed && !seenBoosts.contains(boost)
                && expiryReminders.isShowing(ExpiryReminders.key(boost), config.boostDurationSeconds(), now))
            {
                seenBoosts.add(boost);
                addItem(count++, id, config.boostReminderColor().getRGB(), KIND_BOOST);
                liveBoostReady = true;
            }
        }
        liveBuffItemCount = count;
    }

    private void addItem(int idx, int id, int argb, int kind)
    {
        liveBuffItemIds[idx] = id;
        liveBuffItemColors[idx] = argb;
        liveBuffItemKinds[idx] = kind;
    }

    private boolean kindGlow(int kind)
    {
        switch (kind)
        {
            case KIND_HEART: return config.heartScreenReminder();
            case KIND_BOOST: return config.boostGlow();
            default: return config.buffScreenReminder();
        }
    }

    private boolean kindShower(int kind)
    {
        switch (kind)
        {
            case KIND_HEART: return config.heartShower();
            case KIND_BOOST: return config.boostShower();
            default: return config.buffShower();
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return null;
        }

        int width = client.getCanvasWidth();
        int height = client.getCanvasHeight();
        long nowNanos = System.nanoTime();

        // ~22 config reads: evaluate once per frame instead of three times
        frameDebugPreview = isAnyDebugPreviewActive();
        populateStatusArrays(nowNanos);

        int rawSpellCount = frameDebugPreview
            ? collectDebugSpellsOnly()
            : spellTracker.collectReadySpells(
            combatState, config, cachedSpellbook, true, readySpellOrdinals, readySpellColors
        );
        int readySpellCount = filterSpellsByDuration(rawSpellCount, nowNanos);
        int readyItemCount = collectReadyBuffItems();
        int glowExtraCount = buildGlowExtras(readySpellCount, readyItemCount);
        int showerSpriteCount = buildUnifiedSpriteShower(readySpellCount);
        int showerItemCount = buildShowerItems(readyItemCount);

        renderer.render(
            graphics,
            width,
            height,
            nowNanos,
            config,
            client,
            spriteManager,
            itemManager,
            spellTracker,
            statusSeverities,
            statusColors,
            glowExtraCount,
            glowExtraColors,
            showerSpriteCount,
            showerSpriteIds,
            showerSpriteColors,
            showerItemCount,
            readyItemIds,
            readyItemColors
        );

        return null;
    }

    private void populateStatusArrays(long nowNanos)
    {
        java.util.Arrays.fill(statusSeverities, 0f);
        java.util.Arrays.fill(rawStatusSeverities, 0f);
        java.util.Arrays.fill(statusColors, 0);

        if (!frameDebugPreview)
        {
        // Live client-thread-sampled values
        if (liveLowHealthSev > 0.01f)
        {
            rawStatusSeverities[StatusEffect.LOW_HEALTH.ordinal()] = liveLowHealthSev;
            statusColors[StatusEffect.LOW_HEALTH.ordinal()] = config.lowHealthColor().getRGB();
        }
        if (liveLowPrayerSev > 0.01f)
        {
            rawStatusSeverities[StatusEffect.LOW_PRAYER.ordinal()] = liveLowPrayerSev;
            statusColors[StatusEffect.LOW_PRAYER.ordinal()] = config.lowPrayerColor().getRGB();
        }
        if (livePoisonSev > 0.01f)
        {
            rawStatusSeverities[StatusEffect.POISON.ordinal()] = livePoisonSev;
            statusColors[StatusEffect.POISON.ordinal()] = config.poisonColor().getRGB();
        }
        if (liveVenomSev > 0.01f)
        {
            rawStatusSeverities[StatusEffect.VENOM.ordinal()] = liveVenomSev;
            statusColors[StatusEffect.VENOM.ordinal()] = config.venomColor().getRGB();
        }
        if (liveDiseaseSev > 0.01f)
        {
            rawStatusSeverities[StatusEffect.DISEASE.ordinal()] = liveDiseaseSev;
            statusColors[StatusEffect.DISEASE.ordinal()] = config.diseaseColor().getRGB();
        }
        if (liveSpecSev > 0.01f)
        {
            rawStatusSeverities[StatusEffect.SPEC_READY.ordinal()] = liveSpecSev;
            statusColors[StatusEffect.SPEC_READY.ordinal()] = config.specColor().getRGB();
        }
        if (liveLowRunSev > 0.01f)
        {
            rawStatusSeverities[StatusEffect.LOW_RUN_ENERGY.ordinal()] = liveLowRunSev;
            statusColors[StatusEffect.LOW_RUN_ENERGY.ordinal()] = config.lowRunColor().getRGB();
        }

        // Time-based trackers (pure math, zero client-thread calls)
        if (config.damageFeedbackEnabled())
        {
            long durNanos = config.damageFeedbackDuration() * 1_000_000L;
            float dmgSev = damageTracker.getSeverity(nowNanos, durNanos) * (config.damageFeedbackStrength() / 100f);
            if (dmgSev > 0.01f)
            {
                rawStatusSeverities[StatusEffect.DAMAGE.ordinal()] = dmgSev;
                statusColors[StatusEffect.DAMAGE.ordinal()] = config.damageFlashColor().getRGB();
            }
        }

        float pTickSev = poisonTickTracker.getSeverity(nowNanos, 500_000_000L);
        if (pTickSev > 0.01f && config.poisonEnabled())
        {
            rawStatusSeverities[StatusEffect.POISON_TICK.ordinal()] = pTickSev;
            statusColors[StatusEffect.POISON_TICK.ordinal()] = config.poisonColor().getRGB();
        }

        float vTickSev = venomTickTracker.getSeverity(nowNanos, 500_000_000L);
        if (vTickSev > 0.01f && config.venomEnabled())
        {
            rawStatusSeverities[StatusEffect.VENOM_TICK.ordinal()] = vTickSev;
            statusColors[StatusEffect.VENOM_TICK.ordinal()] = config.venomColor().getRGB();
        }

        if (config.freezeEnabled())
        {
            float frzSev = freezeTracker.getSeverity(config, nowNanos);
            if (frzSev > 0.01f)
            {
                rawStatusSeverities[StatusEffect.FREEZE.ordinal()] = frzSev;
                statusColors[StatusEffect.FREEZE.ordinal()] = config.freezeColor().getRGB();
            }
        }

        if (config.idleEnabled())
        {
            float idleSev = idleTracker.getSeverity(config, nowNanos);
            if (idleSev > 0.01f)
            {
                rawStatusSeverities[StatusEffect.IDLE.ordinal()] = idleSev;
                statusColors[StatusEffect.IDLE.ordinal()] = config.idleColor().getRGB();
            }
        }
        if (config.parasiteEnabled() && parasiteTracker.isActive())
        {
            rawStatusSeverities[StatusEffect.PARASITE.ordinal()] = parasiteTracker.getSeverity();
            statusColors[StatusEffect.PARASITE.ordinal()] = config.parasiteColor().getRGB();
        }
        if (config.unpottedEnabled())
        {
            float unpotSev = unpottedTracker.getSeverity(config, nowNanos);
            if (unpotSev > 0.01f)
            {
                rawStatusSeverities[StatusEffect.UNPOTTED.ordinal()] = unpotSev;
                statusColors[StatusEffect.UNPOTTED.ordinal()] = config.unpottedColor().getRGB();
            }
        }

        }

        applyDuration(StatusEffect.LOW_HEALTH, config.lowHealthDurationSeconds(), nowNanos);
        applyDuration(StatusEffect.LOW_PRAYER, config.lowPrayerDurationSeconds(), nowNanos);
        applyDuration(StatusEffect.POISON, config.poisonDurationSeconds(), nowNanos);
        applyDuration(StatusEffect.VENOM, config.venomDurationSeconds(), nowNanos);
        applyDuration(StatusEffect.DISEASE, config.diseaseDurationSeconds(), nowNanos);
        applyDuration(StatusEffect.SPEC_READY, config.specDurationSeconds(), nowNanos);
        applyDuration(StatusEffect.LOW_RUN_ENERGY, config.lowRunDurationSeconds(), nowNanos);
        applyDuration(StatusEffect.PARASITE, config.parasiteDurationSeconds(), nowNanos);

        // Debug preview overrides (isolated when any debug checkbox is checked)
        float pSev = config.previewIntensity() / 100f;
        if (config.debugLowHealth())
        {
            rawStatusSeverities[StatusEffect.LOW_HEALTH.ordinal()] = pSev;
            statusColors[StatusEffect.LOW_HEALTH.ordinal()] = config.lowHealthColor().getRGB();
        }
        if (config.debugLowPrayer())
        {
            rawStatusSeverities[StatusEffect.LOW_PRAYER.ordinal()] = pSev;
            statusColors[StatusEffect.LOW_PRAYER.ordinal()] = config.lowPrayerColor().getRGB();
        }
        if (config.debugPoison())
        {
            rawStatusSeverities[StatusEffect.POISON.ordinal()] = pSev;
            statusColors[StatusEffect.POISON.ordinal()] = config.poisonColor().getRGB();
        }
        if (config.debugVenom())
        {
            rawStatusSeverities[StatusEffect.VENOM.ordinal()] = pSev;
            statusColors[StatusEffect.VENOM.ordinal()] = config.venomColor().getRGB();
        }
        if (config.debugDisease())
        {
            rawStatusSeverities[StatusEffect.DISEASE.ordinal()] = pSev;
            statusColors[StatusEffect.DISEASE.ordinal()] = config.diseaseColor().getRGB();
        }
        if (config.debugFreeze())
        {
            rawStatusSeverities[StatusEffect.FREEZE.ordinal()] = pSev;
            statusColors[StatusEffect.FREEZE.ordinal()] = config.freezeColor().getRGB();
        }
        if (config.debugDamageFlash())
        {
            rawStatusSeverities[StatusEffect.DAMAGE.ordinal()] = pSev;
            statusColors[StatusEffect.DAMAGE.ordinal()] = config.damageFlashColor().getRGB();
        }
        if (config.debugIdle())
        {
            rawStatusSeverities[StatusEffect.IDLE.ordinal()] = pSev;
            statusColors[StatusEffect.IDLE.ordinal()] = config.idleColor().getRGB();
        }
        if (config.debugSpecReady())
        {
            rawStatusSeverities[StatusEffect.SPEC_READY.ordinal()] = pSev;
            statusColors[StatusEffect.SPEC_READY.ordinal()] = config.specColor().getRGB();
        }
        if (config.debugLowRun())
        {
            rawStatusSeverities[StatusEffect.LOW_RUN_ENERGY.ordinal()] = pSev;
            statusColors[StatusEffect.LOW_RUN_ENERGY.ordinal()] = config.lowRunColor().getRGB();
        }
        if (config.debugParasite())
        {
            rawStatusSeverities[StatusEffect.PARASITE.ordinal()] = pSev;
            statusColors[StatusEffect.PARASITE.ordinal()] = config.parasiteColor().getRGB();
        }
        if (config.debugUnpotted())
        {
            rawStatusSeverities[StatusEffect.UNPOTTED.ordinal()] = pSev;
            statusColors[StatusEffect.UNPOTTED.ordinal()] = config.unpottedColor().getRGB();
        }
        applyStatusGlowToggle(StatusEffect.LOW_HEALTH, config.lowHealthGlow());
        applyStatusGlowToggle(StatusEffect.DAMAGE, config.damageGlow());
        applyStatusGlowToggle(StatusEffect.LOW_PRAYER, config.lowPrayerGlow());
        applyStatusGlowToggle(StatusEffect.POISON, config.poisonGlow());
        applyStatusGlowToggle(StatusEffect.POISON_TICK, config.poisonGlow());
        applyStatusGlowToggle(StatusEffect.VENOM, config.venomGlow());
        applyStatusGlowToggle(StatusEffect.VENOM_TICK, config.venomGlow());
        applyStatusGlowToggle(StatusEffect.DISEASE, config.diseaseGlow());
        applyStatusGlowToggle(StatusEffect.FREEZE, config.freezeGlow());
        applyStatusGlowToggle(StatusEffect.IDLE, config.idleGlow());
        applyStatusGlowToggle(StatusEffect.SPEC_READY, config.specGlow());
        applyStatusGlowToggle(StatusEffect.LOW_RUN_ENERGY, config.lowRunGlow());
        applyStatusGlowToggle(StatusEffect.UNPOTTED, config.unpottedGlow());
        applyStatusGlowToggle(StatusEffect.PARASITE, config.parasiteGlow());
    }

    /**
     * Shows a continuous warning for at most {@code seconds} after it starts (0 = while active); it re-arms once the
     * condition clears and comes back.
     */
    private void applyDuration(StatusEffect effect, int seconds, long nowNanos)
    {
        int i = effect.ordinal();
        if (rawStatusSeverities[i] <= 0.01f)
        {
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

    private void applyStatusGlowToggle(StatusEffect effect, boolean glowEnabled)
    {
        if (glowEnabled && config.allowScreenGlows())
        {
            statusSeverities[effect.ordinal()] = rawStatusSeverities[effect.ordinal()];
        }
    }

    private int buildGlowExtras(int spellCount, int itemCount)
    {
        int out = 0;
        CombatSpell[] all = CombatSpell.values();
        for (int i = 0; i < spellCount; i++)
        {
            CombatSpell s = all[readySpellOrdinals[i]];
            if (ImmersiveCombatOverlaysConfig.isSpellGlowEnabled(config, s))
            {
                glowExtraColors[out++] = readySpellColors[i];
            }
        }
        if (!config.allowScreenGlows())
        {
            return out;
        }
        for (int i = 0; i < itemCount; i++)
        {
            if (kindGlow(readyItemKinds[i]))
            {
                glowExtraColors[out++] = readyItemColors[i];
            }
        }
        return out;
    }

    private int buildShowerItems(int rawItemCount)
    {
        int out = 0;
        if (!config.allowIconShowers())
        {
            return 0;
        }
        for (int i = 0; i < rawItemCount; i++)
        {
            if (kindShower(readyItemKinds[i]))
            {
                readyItemIds[out] = readyItemIds[i];
                readyItemColors[out] = readyItemColors[i];
                readyItemKinds[out] = readyItemKinds[i];
                out++;
            }
        }
        if (rawStatusSeverities[StatusEffect.PARASITE.ordinal()] > 0.01f && config.parasiteShower() && out < readyItemIds.length)
        {
            readyItemIds[out] = ItemID.SANFEW_SALVE_4_DOSE;
            readyItemColors[out] = statusColors[StatusEffect.PARASITE.ordinal()];
            out++;
        }
        if (rawStatusSeverities[StatusEffect.VENOM.ordinal()] > 0.01f && config.venomShower() && out < readyItemIds.length)
        {
            readyItemIds[out] = ITEM_ANTIVENOM_PLUS;
            readyItemColors[out] = statusColors[StatusEffect.VENOM.ordinal()];
            out++;
        }
        return out;
    }

    private int buildUnifiedSpriteShower(int spellCount)
    {
        int out = 0;
        if (!config.allowIconShowers())
        {
            return 0;
        }
        CombatSpell[] all = CombatSpell.values();
        for (int i = 0; i < spellCount; i++)
        {
            CombatSpell s = all[readySpellOrdinals[i]];
            if (ImmersiveCombatOverlaysConfig.isSpellShowerEnabled(config, s))
            {
                showerSpriteIds[out] = spellTracker.getResolvedSpriteId(s);
                showerSpriteColors[out] = readySpellColors[i];
                out++;
            }
        }
        if (rawStatusSeverities[StatusEffect.LOW_PRAYER.ordinal()] > 0.01f && config.lowPrayerShower())
        {
            showerSpriteIds[out] = SPRITE_SKILL_PRAYER;
            showerSpriteColors[out] = statusColors[StatusEffect.LOW_PRAYER.ordinal()];
            out++;
        }
        if (rawStatusSeverities[StatusEffect.FREEZE.ordinal()] > 0.01f && config.freezeShower())
        {
            showerSpriteIds[out] = SPRITE_ICE_BARRAGE;
            showerSpriteColors[out] = statusColors[StatusEffect.FREEZE.ordinal()];
            out++;
        }
        if (rawStatusSeverities[StatusEffect.LOW_HEALTH.ordinal()] > 0.01f && config.lowHealthShower())
        {
            showerSpriteIds[out] = SPRITE_SKILL_HITPOINTS;
            showerSpriteColors[out] = statusColors[StatusEffect.LOW_HEALTH.ordinal()];
            out++;
        }
        if (rawStatusSeverities[StatusEffect.DAMAGE.ordinal()] > 0.01f && config.damageShower())
        {
            showerSpriteIds[out] = SPRITE_DAMAGE_HITSPLAT;
            showerSpriteColors[out] = statusColors[StatusEffect.DAMAGE.ordinal()];
            out++;
        }
        if (rawStatusSeverities[StatusEffect.LOW_RUN_ENERGY.ordinal()] > 0.01f && config.lowRunShower())
        {
            showerSpriteIds[out] = SPRITE_RUN_ORB;
            showerSpriteColors[out] = statusColors[StatusEffect.LOW_RUN_ENERGY.ordinal()];
            out++;
        }
        if (rawStatusSeverities[StatusEffect.SPEC_READY.ordinal()] > 0.01f && config.specShower())
        {
            showerSpriteIds[out] = SPRITE_SPEC_SWORDS;
            showerSpriteColors[out] = statusColors[StatusEffect.SPEC_READY.ordinal()];
            out++;
        }
        if (rawStatusSeverities[StatusEffect.IDLE.ordinal()] > 0.01f && config.idleShower())
        {
            showerSpriteIds[out] = SPRITE_IDLE_DREAM;
            showerSpriteColors[out] = statusColors[StatusEffect.IDLE.ordinal()];
            out++;
        }
        if (rawStatusSeverities[StatusEffect.POISON.ordinal()] > 0.01f && config.poisonShower())
        {
            showerSpriteIds[out] = SPRITE_POISON_HITSPLAT;
            showerSpriteColors[out] = statusColors[StatusEffect.POISON.ordinal()];
            out++;
        }
        if (rawStatusSeverities[StatusEffect.DISEASE.ordinal()] > 0.01f && config.diseaseShower())
        {
            showerSpriteIds[out] = SPRITE_DISEASE_HITSPLAT;
            showerSpriteColors[out] = statusColors[StatusEffect.DISEASE.ordinal()];
            out++;
        }
        if (rawStatusSeverities[StatusEffect.UNPOTTED.ordinal()] > 0.01f && config.unpottedShower())
        {
            showerSpriteIds[out] = SPRITE_STATS_BARS;
            showerSpriteColors[out] = statusColors[StatusEffect.UNPOTTED.ordinal()];
            out++;
        }
        return out;
    }

    private int filterSpellsByDuration(int rawCount, long nowNanos)
    {
        int session = combatState.getCombatSessionId();
        CombatSpell[] all = CombatSpell.values();
        int kept = 0;
        for (int i = 0; i < rawCount; i++)
        {
            CombatSpell s = all[readySpellOrdinals[i]];
            int maxSec = ImmersiveCombatOverlaysConfig.getSpellDurationSeconds(config, s);
            if (ImmersiveCombatOverlaysConfig.isDebugSpellActive(config, s) || spellTracker.isWithinShowerDuration(s, session, maxSec, nowNanos))
            {
                readySpellOrdinals[kept] = readySpellOrdinals[i];
                readySpellColors[kept] = readySpellColors[i];
                kept++;
            }
        }
        return kept;
    }

    private int collectReadyBuffItems()
    {
        int count = 0;
        if (config.debugHeartReady())
        {
            readyItemIds[count] = HeartCooldownTracker.SATURATED_HEART_ID;
            readyItemColors[count] = config.heartReminderColor().getRGB();
            readyItemKinds[count] = KIND_HEART;
            count++;
        }
        if (config.debugBuffExpired())
        {
            readyItemIds[count] = ItemID._4DOSEDIVINECOMBAT;
            readyItemColors[count] = config.buffReminderColor().getRGB();
            readyItemKinds[count] = KIND_POTION;
            count++;
        }

        int liveCount = frameDebugPreview ? 0 : liveBuffItemCount;
        for (int i = 0; i < liveCount && count < readyItemIds.length; i++)
        {
            readyItemIds[count] = liveBuffItemIds[i];
            readyItemColors[count] = liveBuffItemColors[i];
            readyItemKinds[count] = liveBuffItemKinds[i];
            count++;
        }
        return count;
    }

    private boolean isAnyDebugPreviewActive()
    {
        if (config.debugLowHealth() || config.debugLowPrayer() || config.debugPoison()
            || config.debugVenom() || config.debugDisease() || config.debugFreeze()
            || config.debugDamageFlash() || config.debugIdle() || config.debugSpecReady()
            || config.debugLowRun() || config.debugUnpotted() || config.debugParasite()
            || config.debugHeartReady() || config.debugBuffExpired())
        {
            return true;
        }
        for (CombatSpell s : CombatSpell.values())
        {
            if (ImmersiveCombatOverlaysConfig.isDebugSpellActive(config, s))
            {
                return true;
            }
        }
        return false;
    }

    private int collectDebugSpellsOnly()
    {
        int count = 0;
        for (CombatSpell s : CombatSpell.values())
        {
            if (ImmersiveCombatOverlaysConfig.isDebugSpellActive(config, s))
            {
                readySpellOrdinals[count] = s.ordinal();
                readySpellColors[count] = ImmersiveCombatOverlaysConfig.getSpellColor(config, s).getRGB();
                count++;
            }
        }
        return count;
    }

    public boolean isLowHealthLive()
    {
        return liveLowHealthSev > 0.01f;
    }

    public boolean isLowPrayerLive()
    {
        return liveLowPrayerSev > 0.01f;
    }

    public boolean isSpecReadyLive()
    {
        return liveSpecSev > 0.01f;
    }

    public boolean isBuffExpiredLive()
    {
        return liveBuffExpired;
    }

    public boolean isBoostReadyLive()
    {
        return liveBoostReady;
    }

    public boolean isHeartReadyLive()
    {
        return liveHeartReady;
    }

    public boolean isPoisonLive()
    {
        return livePoisonSev > 0.01f;
    }

    public boolean isVenomLive()
    {
        return liveVenomSev > 0.01f;
    }

    public boolean isDiseaseLive()
    {
        return liveDiseaseSev > 0.01f;
    }

    public boolean isLowRunLive()
    {
        return liveLowRunSev > 0.01f;
    }

    public boolean isPrayerLow()
    {
        return config.debugLowPrayer() || liveLowPrayerSev > 0.01f;
    }

    private boolean isAnyPrayerActive()
    {
        // Bitfield of every active prayer (covers Deadeye / Mystic Vigour, unlike the deprecated isPrayerActive)
        return client.getVarbitValue(VarbitID.PRAYER_ALLACTIVE) != 0;
    }
}
