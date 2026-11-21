package com.immersivecombatoverlays;

import com.google.inject.Provides;
import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.overlay.ImmersiveStatusOverlay;
import com.immersivecombatoverlays.overlay.InventoryCooldownOverlay;
import com.immersivecombatoverlays.overlay.SpellCooldownOverlay;
import com.immersivecombatoverlays.render.CooldownRenderer;
import com.immersivecombatoverlays.render.ProceduralRenderer;
import com.immersivecombatoverlays.tracker.BoostPotionTimers;
import com.immersivecombatoverlays.tracker.CombatSpellTracker;
import com.immersivecombatoverlays.tracker.CombatStateTracker;
import com.immersivecombatoverlays.tracker.FreezeTracker;
import com.immersivecombatoverlays.tracker.HeartCooldownTracker;
import com.immersivecombatoverlays.tracker.FoodCooldownTracker;
import com.immersivecombatoverlays.tracker.IdleTracker;
import com.immersivecombatoverlays.tracker.UnpottedTracker;
import com.immersivecombatoverlays.tracker.ImpactTracker;
import com.immersivecombatoverlays.tracker.ParasiteTracker;
import com.immersivecombatoverlays.tracker.ExpiryReminders;
import com.immersivecombatoverlays.model.PotionEffect;
import com.immersivecombatoverlays.model.BoostPotion;
import com.immersivecombatoverlays.model.Cooldown;
import com.immersivecombatoverlays.tracker.SpellRecastProtection;
import com.immersivecombatoverlays.tracker.WarningNotifier;
import com.immersivecombatoverlays.model.CombatSpell;
import net.runelite.client.Notifier;
import com.immersivecombatoverlays.tracker.PotionTimers;
import com.immersivecombatoverlays.tracker.RepotProtection;
import javax.inject.Inject;
import javax.inject.Named;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import com.immersivecombatoverlays.model.Preset;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.util.Text;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "Immersive Combat Overlays",
    description = "Combines inventory cooldowns, procedural health/status screen glows, and combat spell recast/expiry glows & showering icon reminders.",
    tags = {"combat", "cooldown", "overlay", "thrall", "spell", "health", "prayer", "status", "potions"}
)
public class ImmersiveCombatOverlaysPlugin extends Plugin
{
    static final String CONFIG_GROUP = "immersivecombatoverlays";

    @Inject
    private Client client;

    @Inject
    private ImmersiveCombatOverlaysConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private Notifier notifier;

    @Inject
    private ConfigManager configManager;

    private final CombatStateTracker combatState = new CombatStateTracker();
    private final CombatSpellTracker spellTracker = new CombatSpellTracker();
    private final HeartCooldownTracker heartTracker = new HeartCooldownTracker();
    private final PotionTimers potionTimers = new PotionTimers();
    private final BoostPotionTimers boostTimers = new BoostPotionTimers();
    private final RepotProtection repotProtection = new RepotProtection();
    private final ImpactTracker damageTracker = new ImpactTracker();
    private final ImpactTracker poisonTickTracker = new ImpactTracker();
    private final ImpactTracker venomTickTracker = new ImpactTracker();
    private final FreezeTracker freezeTracker = new FreezeTracker();
    private final IdleTracker idleTracker = new IdleTracker();
    private final FoodCooldownTracker foodTracker = new FoodCooldownTracker();
    private final UnpottedTracker unpottedTracker = new UnpottedTracker();
    private final ParasiteTracker parasiteTracker = new ParasiteTracker();
    private final ExpiryReminders expiryReminders = new ExpiryReminders();
    private final SpellRecastProtection spellRecastProtection = new SpellRecastProtection();
    private final WarningNotifier warningNotifier = new WarningNotifier();
    private final boolean[] spellReadyLastTick = new boolean[CombatSpell.values().length];

    private final CooldownRenderer cooldownRenderer = new CooldownRenderer();
    private final ProceduralRenderer proceduralRenderer = new ProceduralRenderer();

    @Inject
    private InventoryCooldownOverlay inventoryOverlay;

    @Inject
    private SpellCooldownOverlay spellOverlay;

    @Inject
    private ImmersiveStatusOverlay statusOverlay;

    @Provides
    ImmersiveCombatOverlaysConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ImmersiveCombatOverlaysConfig.class);
    }

    @Provides
    CombatStateTracker provideCombatState()
    {
        return combatState;
    }

    @Provides
    CombatSpellTracker provideSpellTracker()
    {
        return spellTracker;
    }

    @Provides
    HeartCooldownTracker provideHeartTracker()
    {
        return heartTracker;
    }

    @Provides
    PotionTimers providePotionTimers()
    {
        return potionTimers;
    }

    @Provides
    BoostPotionTimers provideBoostTimers()
    {
        return boostTimers;
    }

    @Provides
    CooldownRenderer provideCooldownRenderer()
    {
        return cooldownRenderer;
    }

    @Provides
    ProceduralRenderer provideProceduralRenderer()
    {
        return proceduralRenderer;
    }

    @Provides
    @Named("damage")
    ImpactTracker provideDamageTracker()
    {
        return damageTracker;
    }

    @Provides
    @Named("poisonTick")
    ImpactTracker providePoisonTickTracker()
    {
        return poisonTickTracker;
    }

    @Provides
    @Named("venomTick")
    ImpactTracker provideVenomTickTracker()
    {
        return venomTickTracker;
    }

    @Provides
    FreezeTracker provideFreezeTracker()
    {
        return freezeTracker;
    }

    @Provides
    IdleTracker provideIdleTracker()
    {
        return idleTracker;
    }

    @Provides
    FoodCooldownTracker provideFoodTracker()
    {
        return foodTracker;
    }

    @Provides
    ExpiryReminders provideExpiryReminders()
    {
        return expiryReminders;
    }

    @Provides
    ParasiteTracker provideParasiteTracker()
    {
        return parasiteTracker;
    }

    @Provides
    UnpottedTracker provideUnpottedTracker()
    {
        return unpottedTracker;
    }

    @Override
    protected void startUp()
    {
        resetDebugToggles();
        resetAll();
        combatState.setSimulatedCombat(config.debugSimulateCombat());
        overlayManager.add(statusOverlay);
        overlayManager.add(inventoryOverlay);
        overlayManager.add(spellOverlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(statusOverlay);
        overlayManager.remove(inventoryOverlay);
        overlayManager.remove(spellOverlay);
        resetAll();
        cooldownRenderer.clearCache();
        proceduralRenderer.clearCaches();
        spellOverlay.clearCache();
        inventoryOverlay.clearCache();
    }

    /**
     * Debug previews and "Simulate in-combat" are for trying settings out; a forgotten toggle must not leave the
     * player permanently "in combat" (bypassing every combat-only filter) after a restart.
     */
    private void resetDebugToggles()
    {
        for (String key : ImmersiveCombatOverlaysConfig.DEBUG_KEYS)
        {
            configManager.unsetConfiguration(CONFIG_GROUP, key);
        }
    }

    private void resetAll()
    {
        combatState.reset();
        spellTracker.reset();
        heartTracker.reset();
        potionTimers.reset();
        boostTimers.reset();
        damageTracker.reset();
        poisonTickTracker.reset();
        venomTickTracker.reset();
        freezeTracker.reset();
        idleTracker.reset();
        foodTracker.reset();
        unpottedTracker.reset();
        parasiteTracker.reset();
        expiryReminders.reset();
        warningNotifier.reset();
        java.util.Arrays.fill(spellReadyLastTick, false);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        GameState state = event.getGameState();
        if (state == GameState.LOGIN_SCREEN
            || state == GameState.HOPPING
            || state == GameState.CONNECTION_LOST
            || state == GameState.LOGGING_IN)
        {
            resetAll();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        long nowNanos = System.nanoTime();
        combatState.onGameTick(client, nowNanos, config.combatBreakReRemindSeconds());
        spellTracker.onGameTick(client);
        heartTracker.onGameTick(client);
        spellTracker.setGodCapeWorn(isGodCapeWorn());
        potionTimers.onGameTick(client);
        boostTimers.onGameTick(client);
        freezeTracker.onGameTick(client);
        idleTracker.onGameTick(client, config, combatState, notifier, nowNanos);
        foodTracker.onGameTick();
        unpottedTracker.onGameTick(client, config, combatState, nowNanos);
        updateExpiryReminders(nowNanos);
        statusOverlay.updateClientState();
        inventoryOverlay.cacheInventoryItemNames();
        spellOverlay.updateSpellbook(combatState.isInCombat());
        parasiteTracker.onGameTick();
        updateNotifications(nowNanos);
    }

    /**
     * Sends the per-warning RuneLite notifications (each one OFF by default) on the tick a warning starts.
     */
    private void updateNotifications(long nowNanos)
    {
        warningNotifier.update(WarningNotifier.Warning.LOW_HEALTH, statusOverlay.isLowHealthLive(), config.lowHealthNotification(), notifier, nowNanos);
        warningNotifier.update(WarningNotifier.Warning.LOW_PRAYER, statusOverlay.isLowPrayerLive(), config.lowPrayerNotification(), notifier, nowNanos);
        warningNotifier.update(WarningNotifier.Warning.SPEC_READY, statusOverlay.isSpecReadyLive(), config.specNotification(), notifier, nowNanos);
        warningNotifier.update(WarningNotifier.Warning.BUFF_EXPIRED, statusOverlay.isBuffExpiredLive(), config.buffNotification(), notifier, nowNanos);
        warningNotifier.update(WarningNotifier.Warning.FREEZE,
            config.freezeEnabled() && freezeTracker.getSeverity(nowNanos) > 0f, config.freezeNotification(), notifier, nowNanos);
        warningNotifier.update(WarningNotifier.Warning.UNPOTTED,
            unpottedTracker.getSeverity(config, nowNanos) > 0f, config.unpottedNotification(), notifier, nowNanos);
        warningNotifier.update(WarningNotifier.Warning.HEART_READY, statusOverlay.isHeartReadyLive(), config.heartNotification(), notifier, nowNanos);
        warningNotifier.update(WarningNotifier.Warning.BOOST_READY, statusOverlay.isBoostReadyLive(), config.boostNotification(), notifier, nowNanos);
        warningNotifier.update(WarningNotifier.Warning.POISON, statusOverlay.isPoisonLive(), config.poisonNotification(), notifier, nowNanos);
        warningNotifier.update(WarningNotifier.Warning.VENOM, statusOverlay.isVenomLive(), config.venomNotification(), notifier, nowNanos);
        warningNotifier.update(WarningNotifier.Warning.DISEASE, statusOverlay.isDiseaseLive(), config.diseaseNotification(), notifier, nowNanos);
        warningNotifier.update(WarningNotifier.Warning.LOW_RUN, statusOverlay.isLowRunLive(), config.lowRunNotification(), notifier, nowNanos);
        warningNotifier.update(WarningNotifier.Warning.PARASITE,
            config.parasiteEnabled() && parasiteTracker.isActive(), config.parasiteNotification(), notifier, nowNanos);

        int book = client.getVarbitValue(net.runelite.api.gameval.VarbitID.SPELLBOOK);
        for (CombatSpell spell : CombatSpell.values())
        {
            boolean ready = spell.getSpellbook().getVarbitValue() == book
                && spellTracker.wasCastSinceLogin(spell) && spellTracker.isSpellReadyReminder(spell, combatState, config);
            if (ready && !spellReadyLastTick[spell.ordinal()])
            {
                warningNotifier.fireSpell(spell, ImmersiveCombatOverlaysConfig.getSpellNotification(config, spell), notifier, nowNanos);
            }
            spellReadyLastTick[spell.ordinal()] = ready;
        }
    }

    /**
     * Feeds the expiry latch: each effect counts as "active" while it has more than its pre-remind lead left,
     * so the reminder starts exactly at expiry (or at the lead, for boosts with pre-remind enabled).
     */
    private void updateExpiryReminders(long nowNanos)
    {
        Cooldown heart = heartTracker.getCooldown(HeartCooldownTracker.IMBUED_HEART_ID);
        expiryReminders.observe(ExpiryReminders.HEART,
            isActiveBeyondLead(heart, config.heartPreRemindSeconds()), nowNanos);

        for (PotionEffect effect : PotionEffect.values())
        {
            expiryReminders.observe(ExpiryReminders.key(effect),
                isActiveBeyondLead(potionTimers.getCooldown(effect), ImmersiveCombatOverlaysConfig.getPotionPreRemindSeconds(config, effect)),
                nowNanos);
        }

        for (BoostPotion boost : BoostPotion.values())
        {
            expiryReminders.observe(ExpiryReminders.key(boost),
                isActiveBeyondLead(boostTimers.getCooldown(client, boost, config), config.boostPreRemindSeconds()), nowNanos);
        }
    }

    static boolean isActiveBeyondLead(Cooldown cd, int leadSeconds)
    {
        if (cd == null || cd.isReady())
        {
            return false;
        }
        int leadTicks = Math.round(Math.max(0, leadSeconds) / 0.6f);
        return cd.getRemainingTicks() > leadTicks;
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        ChatMessageType type = event.getType();
        if (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM)
        {
            return;
        }

        String msg = event.getMessage();
        long nowNanos = System.nanoTime();
        spellTracker.onChatMessage(client, msg, config);
        potionTimers.onChatMessage(msg);
        parasiteTracker.onChatMessage(msg);
        freezeTracker.onChatMessage(client.getLocalPlayer(), msg, nowNanos);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        if (client.getGameState() == GameState.LOGGED_IN
            && CombatSpellTracker.isTrackedVar(event.getVarbitId(), event.getVarpId()))
        {
            spellTracker.syncVars(client);
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            idleTracker.onAnimationChanged(client, event.getActor());
        }
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            idleTracker.onInteractingChanged(client, event.getSource(), event.getTarget());
        }
    }

    @Subscribe
    public void onGraphicChanged(GraphicChanged event)
    {
        Player local = client.getLocalPlayer();
        if (local != null && event.getActor() == local)
        {
            freezeTracker.onGraphicChanged(local, System.nanoTime());
        }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return;
        }

        long nowNanos = System.nanoTime();
        Hitsplat hit = event.getHitsplat();
        if (hit != null)
        {
            combatState.onHitsplat(event.getActor(), hit.getHitsplatType(), hit.isMine(), local, nowNanos,
                config.combatBreakReRemindSeconds());
        }
        if (event.getActor() == local)
        {
            Hitsplat splat = event.getHitsplat();
            if (splat == null)
            {
                return;
            }
            int type = splat.getHitsplatType();
            int amount = splat.getAmount();
            if (type == HitsplatID.POISON)
            {
                poisonTickTracker.trigger(nowNanos, 0.85f);
            }
            else if (type == HitsplatID.VENOM)
            {
                venomTickTracker.trigger(nowNanos, 0.95f);
            }
            else if (amount > 0 && amount >= config.damageMinAmount() && isDamageHitsplat(type))
            {
                float strength = damageFlashStrength(amount, client.getRealSkillLevel(Skill.HITPOINTS), config.damageScaleWithHp());
                damageTracker.trigger(nowNanos, strength);
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        String opt = Text.removeTags(event.getMenuOption());
        MenuEntry entry = event.getMenuEntry();
        Actor actor = null;
        if (entry != null)
        {
            actor = entry.getNpc() != null ? entry.getNpc() : entry.getPlayer();
        }
        combatState.onMenuOptionClicked(event.getMenuAction(), opt, event.getMenuTarget(), actor,
            System.nanoTime(), config.combatBreakReRemindSeconds());

        int itemId = event.getItemId();
        if (itemId <= 0)
        {
            return;
        }
        if ("Invigorate".equals(opt))
        {
            heartTracker.onInvigorate(itemId);
        }
        if (config.foodCooldownsEnabled() && ("Eat".equals(opt) || "Drink".equals(opt)))
        {
            // For inventory item ops param0 is the slot; the lock starts once that slot changes
            foodTracker.onConsumeClick(event.getParam0(), itemId, foodTracker.classify(itemId, client::getItemDefinition));
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() == InventoryID.INV)
        {
            foodTracker.onInventoryChanged(event.getItemContainer());
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        unpottedTracker.onStatChanged(
            config,
            combatState,
            potionTimers,
            event.getSkill(),
            event.getXp(),
            event.getBoostedLevel(),
            event.getLevel(),
            System.nanoTime()
        );
    }

    @Subscribe
    public void onActorDeath(ActorDeath event)
    {
        if (event.getActor() == client.getLocalPlayer())
        {
            potionTimers.onPlayerDeath();
            freezeTracker.reset();
            spellTracker.onPlayerDeath();
            combatState.reset();
            parasiteTracker.reset();
        }
    }

    /**
     * Deprioritized entries only move when the client sorts the menu, so the flag must be set on ClientTick
     * (before sorting) and only while the menu is closed - the same approach as core Menu Entry Swapper.
     * Setting it in PostMenuSort (as before) left the menu order unchanged in game.
     */
    @Subscribe
    public void onClientTick(ClientTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen())
        {
            return;
        }
        repotProtection.apply(client, config, heartTracker, potionTimers, boostTimers);
        spellRecastProtection.apply(client, config, spellTracker);
    }

    @Subscribe
    public void onPluginChanged(PluginChanged event)
    {
        proceduralRenderer.clearCaches();
        cooldownRenderer.clearCache();
        spellOverlay.clearCache();
        inventoryOverlay.clearCache();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if ("resourcepacks".equals(event.getGroup()))
        {
            proceduralRenderer.clearCaches();
            cooldownRenderer.clearCache();
            spellOverlay.clearCache();
            inventoryOverlay.clearCache();
            return;
        }
        if (!CONFIG_GROUP.equals(event.getGroup()))
        {
            return;
        }
        proceduralRenderer.clearCaches();
        combatState.setSimulatedCombat(config.debugSimulateCombat());

        if ("preset".equals(event.getKey()))
        {
            applyPreset(config.preset());
            return;
        }

        if ("debugTriggerSpellCooldowns".equals(event.getKey()))
        {
            if (config.debugTriggerSpellCooldowns())
            {
                spellTracker.triggerDebugCooldowns();
            }
            else
            {
                // Only remove the test timers; real cooldowns keep running
                spellTracker.clearDebugCooldowns();
            }
        }
        if ("debugTriggerPotionCooldowns".equals(event.getKey()))
        {
            if (config.debugTriggerPotionCooldowns())
            {
                heartTracker.triggerDebugCooldown();
                potionTimers.triggerDebugCooldowns();
            }
            else
            {
                heartTracker.clearDebugCooldown();
                potionTimers.clearDebugCooldowns();
            }
        }
    }

    /**
     * Writes a preset into this plugin's own config group, then flips the dropdown back to CUSTOM.
     */
    private void applyPreset(Preset preset)
    {
        if (preset == null || preset == Preset.CUSTOM)
        {
            return;
        }
        for (String key : Preset.MANAGED_KEYS)
        {
            configManager.unsetConfiguration(CONFIG_GROUP, key);
        }
        for (java.util.Map.Entry<String, Object> e : preset.settings().entrySet())
        {
            if (e.getValue() == null)
            {
                configManager.unsetConfiguration(CONFIG_GROUP, e.getKey());
            }
            else
            {
                configManager.setConfiguration(CONFIG_GROUP, e.getKey(), e.getValue());
            }
        }
        configManager.setConfiguration(CONFIG_GROUP, "preset", Preset.CUSTOM);
    }

    /**
     * 0..1 damage flash strength. With {@code byPercent}, a hit of 40% of max HP (or more) is a full flash,
     * so the flash means the same thing at any Hitpoints level.
     */
    static float damageFlashStrength(int amount, int maxHp, boolean byPercent)
    {
        if (amount <= 0)
        {
            return 0f;
        }
        if (!byPercent)
        {
            return Math.min(1f, 0.25f + amount / 40f); // original fixed scale
        }
        float frac = amount / (float) Math.max(1, maxHp);
        return Math.min(1f, 0.25f + frac * 1.875f);
    }

    /**
     * Only real damage flashes the screen; heals, shields, prayer/sanity drains etc. do not.
     */
    static boolean isDamageHitsplat(int type)
    {
        switch (type)
        {
            case HitsplatID.DAMAGE_ME:
            case HitsplatID.DAMAGE_OTHER:
            case HitsplatID.DAMAGE_ME_CYAN:
            case HitsplatID.DAMAGE_OTHER_CYAN:
            case HitsplatID.DAMAGE_ME_ORANGE:
            case HitsplatID.DAMAGE_OTHER_ORANGE:
            case HitsplatID.DAMAGE_ME_YELLOW:
            case HitsplatID.DAMAGE_OTHER_YELLOW:
            case HitsplatID.DAMAGE_ME_WHITE:
            case HitsplatID.DAMAGE_OTHER_WHITE:
            case HitsplatID.DAMAGE_MAX_ME:
            case HitsplatID.DAMAGE_MAX_ME_CYAN:
            case HitsplatID.DAMAGE_MAX_ME_ORANGE:
            case HitsplatID.DAMAGE_MAX_ME_YELLOW:
            case HitsplatID.DAMAGE_MAX_ME_WHITE:
            case HitsplatID.DAMAGE_ME_POISE:
            case HitsplatID.DAMAGE_OTHER_POISE:
            case HitsplatID.DAMAGE_MAX_ME_POISE:
            case HitsplatID.BLEED:
            case HitsplatID.BURN:
                return true;
            default:
                return false;
        }
    }

    private boolean isGodCapeWorn()
    {
        ItemContainer worn = client.getItemContainer(InventoryID.WORN);
        if (worn == null)
        {
            return false;
        }
        Item cape = worn.getItem(EquipmentInventorySlot.CAPE.getSlotIdx());
        if (cape == null || cape.getId() <= 0)
        {
            return false;
        }
        return isGodCape(client.getItemDefinition(cape.getId()).getName());
    }

    /** Saradomin / Guthix / Zamorak capes, their imbued versions and the god max capes. */
    static boolean isGodCape(String name)
    {
        if (name == null)
        {
            return false;
        }
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("cape") && (lower.contains("saradomin") || lower.contains("guthix") || lower.contains("zamorak"));
    }
}
