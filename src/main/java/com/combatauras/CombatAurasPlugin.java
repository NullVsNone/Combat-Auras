package com.combatauras;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.combatauras.config.CombatAurasConfig;
import com.combatauras.overlay.StatusOverlay;
import com.combatauras.overlay.InventoryCooldownOverlay;
import com.combatauras.overlay.SpellCooldownOverlay;
import com.combatauras.render.CooldownRenderer;
import com.combatauras.render.ProceduralRenderer;
import com.combatauras.settings.Settings;
import com.combatauras.tracker.BoostPotionTimers;
import com.combatauras.tracker.CombatSpellTracker;
import com.combatauras.tracker.CombatStateTracker;
import com.combatauras.tracker.ConsumableCatalog;
import com.combatauras.tracker.FoodCooldownTracker;
import com.combatauras.tracker.FreezeTracker;
import com.combatauras.tracker.HeartCooldownTracker;
import com.combatauras.tracker.IdleTracker;
import com.combatauras.tracker.ImpactTrackers;
import com.combatauras.tracker.ItemCooldowns;
import com.combatauras.tracker.PotionTimers;
import com.combatauras.tracker.ReminderPipeline;
import com.combatauras.tracker.RepotProtection;
import com.combatauras.tracker.SpellRecastProtection;
import com.combatauras.tracker.UnpottedTracker;
import com.combatauras.tracker.WarningNotifier;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
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
import net.runelite.client.callback.ClientThread;
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
public class CombatAurasPlugin extends Plugin
{
    static final String CONFIG_GROUP = "combatauras";

    @Inject private Client client;
    @Inject private CombatAurasConfig config;
    @Inject private ConfigManager configManager;
    @Inject private OverlayManager overlayManager;
    @Inject private Notifier notifier;
    @Inject private Settings settings;
    @Inject private ClientThread clientThread;
    @Inject private DamageFeedback damageFeedback;
    @Inject private EquipmentState equipment;
    @Inject private PresetApplier presets;

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
    @Inject private ConsumableCatalog catalog;
    @Inject private ReminderPipeline reminders;
    @Inject private NotificationDispatcher notifications;
    @Inject private RepotProtection repotProtection;
    @Inject private SpellRecastProtection spellRecastProtection;

    @Inject private CooldownRenderer cooldownRenderer;
    @Inject private ProceduralRenderer proceduralRenderer;
    @Inject private InventoryCooldownOverlay inventoryOverlay;
    @Inject private SpellCooldownOverlay spellOverlay;
    @Inject private StatusOverlay statusOverlay;

    @Provides
    CombatAurasConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(CombatAurasConfig.class);
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
        binder.bind(DamageFeedback.class);
        binder.bind(EquipmentState.class);
        binder.bind(PresetApplier.class);
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
        binder.bind(ConsumableCatalog.class);
        binder.bind(ItemCooldowns.class);
        binder.bind(ReminderPipeline.class);
        binder.bind(WarningNotifier.class);
        binder.bind(NotificationDispatcher.class);
        binder.bind(RepotProtection.class);
        binder.bind(SpellRecastProtection.class);
        binder.bind(CooldownRenderer.class);
        binder.bind(ProceduralRenderer.class);
        binder.bind(StatusOverlay.class);
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
        for (String key : CombatAurasConfig.DEBUG_KEYS)
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
        int breakSeconds = settings.get().combatBreakReRemindSeconds;

        // 1. Trackers
        combatState.onGameTick(client, nowNanos, breakSeconds);
        spellTracker.setGodCapeWorn(equipment.isGodCapeWorn()); // before the spell tick reads it
        spellTracker.onGameTick(client);
        heartTracker.onGameTick(client);
        potionTimers.onGameTick(client);
        boostTimers.onGameTick(client);
        freezeTracker.onGameTick(client);
        idleTracker.onGameTick(client, config, combatState, notifier, nowNanos);
        foodTracker.onGameTick();
        unpottedTracker.onGameTick(client, config, combatState, nowNanos);

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
            settings.get().combatBreakReRemindSeconds);
        if (event.getActor() == local)
        {
            damageFeedback.onLocalHitsplat(hit.getHitsplatType(), hit.getAmount(), config.damageMinAmount(),
                client.getRealSkillLevel(Skill.HITPOINTS), config.damageScaleWithHp(), nowNanos);
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
            System.nanoTime(), settings.get().combatBreakReRemindSeconds);

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

    /**
     * PluginChanged and ConfigChanged are posted on the Swing thread, while overlays render and trackers run on the
     * client thread. Anything that touches caches or tracker state is therefore handed to the client thread.
     */
    @Subscribe
    public void onPluginChanged(PluginChanged event)
    {
        clientThread.invokeLater(this::clearRenderCaches); // e.g. Resource Packs turned on or off
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if ("resourcepacks".equals(event.getGroup()))
        {
            clientThread.invokeLater(this::clearRenderCaches);
            return;
        }
        if (!CONFIG_GROUP.equals(event.getGroup()) || presets.isApplying())
        {
            return; // a preset batch rebuilds once when it finishes
        }
        String key = event.getKey();
        if ("preset".equals(key))
        {
            presets.apply(CONFIG_GROUP, config.preset()); // per-key events are skipped; one rebuild below
        }
        settings.refresh(); // immutable snapshot behind a volatile reference: safe from any thread
        clientThread.invokeLater(() -> onOwnConfigChanged(key));
    }

    private void onOwnConfigChanged(String key)
    {
        proceduralRenderer.clearCaches();
        combatState.setSimulatedCombat(config.debugSimulateCombat());
        if ("debugTriggerSpellCooldowns".equals(key))
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
}
