package com.immersivecombatoverlays;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.Preset;
import com.immersivecombatoverlays.overlay.ImmersiveStatusOverlay;
import com.immersivecombatoverlays.overlay.InventoryCooldownOverlay;
import com.immersivecombatoverlays.overlay.SpellCooldownOverlay;
import com.immersivecombatoverlays.render.CooldownRenderer;
import com.immersivecombatoverlays.render.ProceduralRenderer;
import com.immersivecombatoverlays.settings.Settings;
import com.immersivecombatoverlays.tracker.BoostPotionTimers;
import com.immersivecombatoverlays.tracker.CombatSpellTracker;
import com.immersivecombatoverlays.tracker.CombatStateTracker;
import com.immersivecombatoverlays.tracker.ConsumableCatalog;
import com.immersivecombatoverlays.tracker.FoodCooldownTracker;
import com.immersivecombatoverlays.tracker.FreezeTracker;
import com.immersivecombatoverlays.tracker.HeartCooldownTracker;
import com.immersivecombatoverlays.tracker.IdleTracker;
import com.immersivecombatoverlays.tracker.ImpactTrackers;
import com.immersivecombatoverlays.tracker.ItemCooldowns;
import com.immersivecombatoverlays.tracker.ParasiteTracker;
import com.immersivecombatoverlays.tracker.PotionTimers;
import com.immersivecombatoverlays.tracker.ReminderPipeline;
import com.immersivecombatoverlays.tracker.RepotProtection;
import com.immersivecombatoverlays.tracker.SpellRecastProtection;
import com.immersivecombatoverlays.tracker.UnpottedTracker;
import com.immersivecombatoverlays.tracker.WarningNotifier;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

/**
 * Event router. Every feature is an injected {@code @Singleton} module; this class only subscribes to client events,
 * forwards them, and runs the per-tick modules in one explicit order (trackers, then reminders, then notifications).
 * A new feature is a new module plus one line in the matching handler.
 */
@PluginDescriptor(
    name = "Combat Auras",
    description = "Screen glows, icon showers and inventory cooldowns for low HP and prayer, potions, spells and other combat warnings.",
    tags = {"combat", "aura", "cooldown", "overlay", "thrall", "spell", "health", "prayer", "status", "potions"}
)
public class ImmersiveCombatOverlaysPlugin extends Plugin
{
    static final String CONFIG_GROUP = "immersivecombatoverlays";

    @Inject private Client client;
    @Inject private ImmersiveCombatOverlaysConfig config;
    @Inject private ConfigManager configManager;
    @Inject private OverlayManager overlayManager;
    @Inject private Notifier notifier;
    @Inject private Settings settings;

    @Inject private CombatStateTracker combatState;
    @Inject private CombatSpellTracker spellTracker;
    @Inject private HeartCooldownTracker heartTracker;
    @Inject private PotionTimers potionTimers;
    @Inject private BoostPotionTimers boostTimers;
    @Inject private ImpactTrackers impacts;
    @Inject private FreezeTracker freezeTracker;
    @Inject private IdleTracker idleTracker;
    @Inject private FoodCooldownTracker foodTracker;
    @Inject private UnpottedTracker unpottedTracker;
    @Inject private ParasiteTracker parasiteTracker;
    @Inject private ConsumableCatalog catalog;
    @Inject private ReminderPipeline reminders;
    @Inject private NotificationDispatcher notifications;
    @Inject private RepotProtection repotProtection;
    @Inject private SpellRecastProtection spellRecastProtection;

    @Inject private CooldownRenderer cooldownRenderer;
    @Inject private ProceduralRenderer proceduralRenderer;
    @Inject private InventoryCooldownOverlay inventoryOverlay;
    @Inject private SpellCooldownOverlay spellOverlay;
    @Inject private ImmersiveStatusOverlay statusOverlay;

    @Provides
    ImmersiveCombatOverlaysConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ImmersiveCombatOverlaysConfig.class);
    }

    /**
     * Binds every module in this plugin's own (child) injector. Without this, Guice would create just-in-time
     * bindings for dependency-free singletons in the client's root injector, keeping them (and this plugin's
     * classes) alive after the plugin is turned off or uninstalled.
     */
    @Override
    public void configure(Binder binder)
    {
        binder.bind(Settings.class);
        binder.bind(CombatStateTracker.class);
        binder.bind(CombatSpellTracker.class);
        binder.bind(HeartCooldownTracker.class);
        binder.bind(PotionTimers.class);
        binder.bind(BoostPotionTimers.class);
        binder.bind(ImpactTrackers.class);
        binder.bind(FreezeTracker.class);
        binder.bind(IdleTracker.class);
        binder.bind(FoodCooldownTracker.class);
        binder.bind(UnpottedTracker.class);
        binder.bind(ParasiteTracker.class);
        binder.bind(ConsumableCatalog.class);
        binder.bind(ItemCooldowns.class);
        binder.bind(ReminderPipeline.class);
        binder.bind(WarningNotifier.class);
        binder.bind(NotificationDispatcher.class);
        binder.bind(RepotProtection.class);
        binder.bind(SpellRecastProtection.class);
        binder.bind(CooldownRenderer.class);
        binder.bind(ProceduralRenderer.class);
        binder.bind(ImmersiveStatusOverlay.class);
        binder.bind(InventoryCooldownOverlay.class);
        binder.bind(SpellCooldownOverlay.class);
    }

    @Override
    protected void startUp()
    {
        resetDebugToggles();
        settings.refresh();
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
        clearRenderCaches();
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
        impacts.reset();
        freezeTracker.reset();
        idleTracker.reset();
        foodTracker.reset();
        unpottedTracker.reset();
        parasiteTracker.reset();
        reminders.reset();
        notifications.reset();
    }

    private void clearRenderCaches()
    {
        proceduralRenderer.clearCaches();
        cooldownRenderer.clearCache();
        spellOverlay.clearCache();
        inventoryOverlay.clearCache();
        catalog.clear();
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
        int breakSeconds = config.combatBreakReRemindSeconds();

        // 1. Trackers
        combatState.onGameTick(client, nowNanos, breakSeconds);
        spellTracker.onGameTick(client);
        spellTracker.setGodCapeWorn(isGodCapeWorn());
        heartTracker.onGameTick(client);
        potionTimers.onGameTick(client);
        boostTimers.onGameTick(client);
        freezeTracker.onGameTick(client);
        idleTracker.onGameTick(client, config, combatState, notifier, nowNanos);
        foodTracker.onGameTick();
        unpottedTracker.onGameTick(client, config, combatState, nowNanos);
        parasiteTracker.onGameTick();

        // 2. Derived state for the overlays
        reminders.update(nowNanos);
        statusOverlay.updateClientState();
        spellOverlay.updateSpellbook(combatState.isInCombat());

        // 3. Notifications
        notifications.update(nowNanos);
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
        spellTracker.onChatMessage(client, msg, config);
        potionTimers.onChatMessage(msg);
        parasiteTracker.onChatMessage(msg);
        freezeTracker.onChatMessage(client.getLocalPlayer(), msg, System.nanoTime());
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
        Hitsplat hit = event.getHitsplat();
        if (local == null || hit == null)
        {
            return;
        }
        long nowNanos = System.nanoTime();
        combatState.onHitsplat(event.getActor(), hit.getHitsplatType(), hit.isMine(), local, nowNanos,
            config.combatBreakReRemindSeconds());
        if (event.getActor() != local)
        {
            return;
        }
        int type = hit.getHitsplatType();
        int amount = hit.getAmount();
        if (type == HitsplatID.POISON)
        {
            impacts.poisonTick.trigger(nowNanos, 0.85f);
        }
        else if (type == HitsplatID.VENOM)
        {
            impacts.venomTick.trigger(nowNanos, 0.95f);
        }
        else if (amount > 0 && amount >= config.damageMinAmount() && isDamageHitsplat(type))
        {
            impacts.damage.trigger(nowNanos,
                damageFlashStrength(amount, client.getRealSkillLevel(Skill.HITPOINTS), config.damageScaleWithHp()));
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
            foodTracker.onConsumeClick(event.getParam0(), itemId, catalog.get(itemId).food);
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
        unpottedTracker.onStatChanged(config, combatState, potionTimers, event.getSkill(), event.getXp(),
            event.getBoostedLevel(), event.getLevel(), System.nanoTime());
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
     */
    @Subscribe
    public void onClientTick(ClientTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen())
        {
            return;
        }
        repotProtection.apply();
        spellRecastProtection.apply();
    }

    @Subscribe
    public void onPluginChanged(PluginChanged event)
    {
        clearRenderCaches();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if ("resourcepacks".equals(event.getGroup()))
        {
            clearRenderCaches();
            return;
        }
        if (!CONFIG_GROUP.equals(event.getGroup()))
        {
            return;
        }
        settings.refresh();
        proceduralRenderer.clearCaches();
        combatState.setSimulatedCombat(config.debugSimulateCombat());

        String key = event.getKey();
        if ("preset".equals(key))
        {
            applyPreset(config.preset());
        }
        else if ("debugTriggerSpellCooldowns".equals(key))
        {
            if (config.debugTriggerSpellCooldowns())
            {
                spellTracker.triggerDebugCooldowns();
            }
            else
            {
                spellTracker.clearDebugCooldowns(); // only the test timers; real cooldowns keep running
            }
        }
        else if ("debugTriggerPotionCooldowns".equals(key))
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
     * Resets every key a preset manages, writes the preset's values, then flips the dropdown back to CUSTOM, so the
     * result never depends on which preset was applied before.
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
        for (Map.Entry<String, Object> e : preset.settings().entrySet())
        {
            if (e.getValue() != null)
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
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("cape") && (lower.contains("saradomin") || lower.contains("guthix") || lower.contains("zamorak"));
    }
}
