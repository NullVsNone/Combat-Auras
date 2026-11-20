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
import net.runelite.client.Notifier;
import com.immersivecombatoverlays.tracker.PotionTimers;
import com.immersivecombatoverlays.tracker.RepotProtection;
import javax.inject.Inject;
import javax.inject.Named;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
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
import net.runelite.api.events.PostMenuSort;
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
    @Inject
    private Client client;

    @Inject
    private ImmersiveCombatOverlaysConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private Notifier notifier;

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
    UnpottedTracker provideUnpottedTracker()
    {
        return unpottedTracker;
    }

    @Override
    protected void startUp()
    {
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
        heartTracker.onGameTick(client, hasSaturatedHeart());
        potionTimers.onGameTick(client);
        boostTimers.onGameTick(client);
        freezeTracker.onGameTick(client);
        idleTracker.onGameTick(client, config, combatState, notifier, nowNanos);
        foodTracker.onGameTick();
        unpottedTracker.onGameTick(client, config, spellTracker.getLastCastNanos(), nowNanos);
        statusOverlay.updateClientState();
        inventoryOverlay.cacheInventoryItemNames();
        spellOverlay.updateSpellbook(combatState.isInCombat());
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
        freezeTracker.onChatMessage(client.getLocalPlayer(), msg, nowNanos);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        if (client.getGameState() == GameState.LOGGED_IN)
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
        if (event.getActor() == local)
        {
            combatState.onCombatActivity(nowNanos, config.combatBreakReRemindSeconds());
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
            else if (amount > 0 && isDamageHitsplat(type))
            {
                float strength = Math.min(1f, 0.25f + (amount / 40f));
                damageTracker.trigger(nowNanos, strength);
            }
        }
        else if (local.getInteracting() == event.getActor())
        {
            combatState.onCombatActivity(nowNanos, config.combatBreakReRemindSeconds());
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (!config.foodCooldownsEnabled() || event.getItemId() <= 0)
        {
            return;
        }
        String opt = event.getMenuOption();
        if ("Eat".equalsIgnoreCase(opt) || "Drink".equalsIgnoreCase(opt))
        {
            String target = Text.removeTags(event.getMenuTarget());
            foodTracker.onConsume(opt, target);
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
            client,
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
        }
    }

    @Subscribe(priority = -1)
    public void onPostMenuSort(PostMenuSort event)
    {
        repotProtection.onPostMenuSort(client, config, heartTracker, potionTimers, boostTimers);
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
        if (!"immersivecombatoverlays".equals(event.getGroup()))
        {
            return;
        }
        proceduralRenderer.clearCaches();
        combatState.setSimulatedCombat(config.debugSimulateCombat());

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

    private boolean hasSaturatedHeart()
    {
        ItemContainer inv = client.getItemContainer(InventoryID.INV);
        if (inv == null)
        {
            return false;
        }
        for (Item item : inv.getItems())
        {
            if (item != null && item.getId() == HeartCooldownTracker.SATURATED_HEART_ID)
            {
                return true;
            }
        }
        return false;
    }
}
