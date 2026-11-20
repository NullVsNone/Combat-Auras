package com.immersivecombatoverlays;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.AnimationMode;
import com.immersivecombatoverlays.model.BoostPotion;
import com.immersivecombatoverlays.model.PotionEffect;
import com.immersivecombatoverlays.model.ReadyStyle;
import com.immersivecombatoverlays.overlay.InventoryCooldownOverlay;
import com.immersivecombatoverlays.render.CooldownRenderer;
import com.immersivecombatoverlays.tracker.BoostPotionTimers;
import com.immersivecombatoverlays.tracker.CombatStateTracker;
import com.immersivecombatoverlays.tracker.FoodCooldownTracker;
import com.immersivecombatoverlays.tracker.FreezeTracker;
import com.immersivecombatoverlays.tracker.HeartCooldownTracker;
import com.immersivecombatoverlays.tracker.IdleTracker;
import com.immersivecombatoverlays.tracker.ImpactTracker;
import com.immersivecombatoverlays.tracker.PotionItems;
import com.immersivecombatoverlays.tracker.PotionTimers;
import com.immersivecombatoverlays.tracker.RepotProtection;
import com.immersivecombatoverlays.tracker.UnpottedTracker;
import java.lang.reflect.Proxy;
import net.runelite.api.HitsplatID;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Assert;
import org.junit.Test;

public class TrackersTest
{
    private static final ImmersiveCombatOverlaysConfig DEFAULTS = new ImmersiveCombatOverlaysConfig() {};

    // ---------------------------------------------------------------- potion item matching

    @Test
    public void potionItemMatchingAllFamilies()
    {
        Assert.assertEquals(PotionEffect.SURGE, PotionItems.getEffect(1, "Surge potion(4)"));
        Assert.assertEquals(PotionEffect.OVERLOAD, PotionItems.getEffect(1, "Overload (+) (4)"));
        Assert.assertEquals(PotionEffect.OVERLOAD, PotionItems.getEffect(1, "Overload (4)"));
        Assert.assertEquals(PotionEffect.BLIGHTED_OVERLOAD, PotionItems.getEffect(1, "Blighted overload(4)"));
        Assert.assertEquals(PotionEffect.SMELLING_SALTS, PotionItems.getEffect(1, "Smelling salts(2)"));
        Assert.assertEquals(PotionEffect.LIQUID_ADRENALINE, PotionItems.getEffect(1, "Liquid adrenaline(2)"));
        Assert.assertEquals(PotionEffect.SILK_DRESSING, PotionItems.getEffect(1, "Silk dressing(2)"));
        Assert.assertEquals(PotionEffect.BLESSED_CRYSTAL_SCARAB, PotionItems.getEffect(1, "Blessed crystal scarab(2)"));
        Assert.assertEquals(PotionEffect.DIVINE_COMBAT, PotionItems.getEffect(1, "Divine super combat potion(4)"));
        Assert.assertEquals(PotionEffect.DIVINE_BASTION, PotionItems.getEffect(1, "Divine bastion potion(4)"));
        Assert.assertEquals(PotionEffect.SUPER_ANTIFIRE, PotionItems.getEffect(1, "Extended super antifire(4)"));
        Assert.assertEquals(PotionEffect.ANTIFIRE, PotionItems.getEffect(1, "Extended antifire(4)"));
        Assert.assertEquals(PotionEffect.ANTIVENOM, PotionItems.getEffect(1, "Anti-venom+(4)"));
        Assert.assertEquals(PotionEffect.ANTIPOISON, PotionItems.getEffect(1, "Antidote++(4)"));
        Assert.assertEquals(PotionEffect.PRAYER_REGENERATION, PotionItems.getEffect(1, "Prayer regeneration potion(4)"));
        Assert.assertNull("Guthix rest has no timer and must not be treated as a ready buff", PotionItems.getEffect(1, "Guthix rest(4)"));
        Assert.assertNull(PotionItems.getEffect(1, "Shark"));
        Assert.assertNull(PotionItems.getEffect(1, null));

        Assert.assertEquals(BoostPotion.SUPER_COMBAT, PotionItems.getBoost(1, "Super combat potion(4)"));
        Assert.assertEquals(BoostPotion.COMBAT, PotionItems.getBoost(1, "Combat potion(4)"));
        Assert.assertEquals(BoostPotion.ATTACK, PotionItems.getBoost(1, "Attack potion(4)"));
        Assert.assertEquals(BoostPotion.SUPER_ATTACK, PotionItems.getBoost(1, "Super attack(4)"));
        Assert.assertEquals(BoostPotion.SUPER_RANGED, PotionItems.getBoost(1, "Super ranging (4)"));
        Assert.assertEquals(BoostPotion.RANGED, PotionItems.getBoost(1, "Ranging potion(4)"));
        Assert.assertEquals(BoostPotion.BASTION, PotionItems.getBoost(1, "Bastion potion(4)"));
        Assert.assertEquals(BoostPotion.FORGOTTEN_BREW, PotionItems.getBoost(1, "Forgotten brew(4)"));
        Assert.assertEquals(BoostPotion.ANCIENT_BREW, PotionItems.getBoost(1, "Ancient brew(4)"));
        Assert.assertNull(PotionItems.getBoost(1, "Divine super combat potion(4)"));

        // Memoised lookups return identical results
        Assert.assertEquals(BoostPotion.SUPER_COMBAT, PotionItems.getBoost(1, "Super combat potion(4)"));

        Assert.assertTrue(InventoryCooldownOverlay.isPrayerRestoreItem("Super restore(4)"));
        Assert.assertTrue(InventoryCooldownOverlay.isPrayerRestoreItem("Prayer potion(4)"));
        Assert.assertFalse(InventoryCooldownOverlay.isPrayerRestoreItem("Shark"));
    }

    @Test
    public void boostFormulasMatchGame()
    {
        Assert.assertEquals(3 + 9, BoostPotion.ATTACK.getMaxBoost(Skill.ATTACK, 99));
        Assert.assertEquals(5 + 14, BoostPotion.SUPER_ATTACK.getMaxBoost(Skill.ATTACK, 99));
        Assert.assertEquals(4 + 9, BoostPotion.RANGED.getMaxBoost(Skill.RANGED, 99));
        Assert.assertEquals(4, BoostPotion.MAGIC.getMaxBoost(Skill.MAGIC, 99));
        Assert.assertEquals(4 + 9, BoostPotion.BASTION.getMaxBoost(Skill.RANGED, 99));
        Assert.assertEquals(5 + 14, BoostPotion.BASTION.getMaxBoost(Skill.DEFENCE, 99));
        Assert.assertEquals(4, BoostPotion.BATTLEMAGE.getMaxBoost(Skill.MAGIC, 99));
        Assert.assertEquals(3 + 7, BoostPotion.FORGOTTEN_BREW.getMaxBoost(Skill.MAGIC, 99));
    }

    // ---------------------------------------------------------------- potion timers

    @Test
    public void potionVarbitsMatchRuneLiteGameVals()
    {
        Assert.assertArrayEquals(new int[]{VarbitID.RAIDS_OVERLOAD_TIMER, VarbitID.NZONE_OVERLOAD_POTION_EFFECTS}, PotionEffect.OVERLOAD.getVarbitIds());
        Assert.assertArrayEquals(new int[]{VarbitID.DIVINECOMBAT_POTION_TIME}, PotionEffect.DIVINE_COMBAT.getVarbitIds());
        Assert.assertArrayEquals(new int[]{VarbitID.DIVINEBATTLEMAGE_POTION_TIME}, PotionEffect.DIVINE_BATTLEMAGE.getVarbitIds());
        Assert.assertArrayEquals(new int[]{VarbitID.TOA_MIDRAIDLOOT_STATS_TIMER}, PotionEffect.SMELLING_SALTS.getVarbitIds());
        Assert.assertArrayEquals(new int[]{VarbitID.GOADING_POTION_TIMER}, PotionEffect.GOADING.getVarbitIds());
        Assert.assertArrayEquals(new int[]{VarbitID.MOONLIGHT_POTION_TIME}, PotionEffect.MOONLIGHT.getVarbitIds());
        Assert.assertArrayEquals(new int[]{VarbitID.PRAYER_REGENERATION_POTION_TIMER}, PotionEffect.PRAYER_REGENERATION.getVarbitIds());
        Assert.assertArrayEquals(new int[]{VarbitID.STATRENEWAL_POTION_TIMER}, PotionEffect.MENAPHITE_REMEDY.getVarbitIds());
        Assert.assertArrayEquals(new int[]{VarbitID.SURGE_POTION_TIMER}, PotionEffect.SURGE.getVarbitIds());
        Assert.assertEquals(20, PotionEffect.SUPER_ANTIFIRE.getTickMultiplier());
        Assert.assertEquals(30, PotionEffect.ANTIFIRE.getTickMultiplier());
    }

    @Test
    public void varScaledEffectsUseMultiplierAndInterpolateBetweenUpdates()
    {
        FakeClient fc = new FakeClient().varbit(VarbitID.SUPER_ANTIFIRE_POTION, 15);
        PotionTimers potions = new PotionTimers();
        potions.onGameTick(fc.client);
        Assert.assertEquals(300, potions.getCooldown(PotionEffect.SUPER_ANTIFIRE).getRemainingTicks());
        Assert.assertTrue(potions.wasUsedThisSession(PotionEffect.SUPER_ANTIFIRE));

        // Varbit unchanged for a few ticks: count down smoothly, never below the bucket floor
        potions.onGameTick(fc.client);
        potions.onGameTick(fc.client);
        Assert.assertEquals(298, potions.getCooldown(PotionEffect.SUPER_ANTIFIRE).getRemainingTicks());
        for (int i = 0; i < 40; i++)
        {
            potions.onGameTick(fc.client);
        }
        Assert.assertEquals(14 * 20 + 1, potions.getCooldown(PotionEffect.SUPER_ANTIFIRE).getRemainingTicks());

        fc.varbit(VarbitID.SUPER_ANTIFIRE_POTION, 0);
        potions.onGameTick(fc.client);
        Assert.assertTrue(potions.getCooldown(PotionEffect.SUPER_ANTIFIRE).isReady());
    }

    @Test
    public void overloadReadsEitherRaidOrNmzVarbitAndDivinesAreInTicks()
    {
        FakeClient fc = new FakeClient().varbit(VarbitID.NZONE_OVERLOAD_POTION_EFFECTS, 20).varbit(VarbitID.DIVINECOMBAT_POTION_TIME, 480);
        PotionTimers potions = new PotionTimers();
        potions.onGameTick(fc.client);
        Assert.assertEquals(500, potions.getCooldown(PotionEffect.OVERLOAD).getRemainingTicks());
        Assert.assertEquals(480, potions.getCooldown(PotionEffect.DIVINE_COMBAT).getRemainingTicks());
        Assert.assertTrue(potions.getCooldown(PotionEffect.DIVINE_ATTACK).isReady());
    }

    @Test
    public void antipoisonAndAntivenomSplitOnPoisonVarp()
    {
        PotionTimers potions = new PotionTimers();
        FakeClient fc = new FakeClient().varp(VarPlayerID.POISON, -10);
        potions.onGameTick(fc.client);
        Assert.assertFalse(potions.getCooldown(PotionEffect.ANTIPOISON).isReady());
        Assert.assertTrue(potions.getCooldown(PotionEffect.ANTIVENOM).isReady());

        fc.varp(VarPlayerID.POISON, -50);
        potions.onGameTick(fc.client);
        Assert.assertTrue(potions.getCooldown(PotionEffect.ANTIPOISON).isReady());
        Assert.assertFalse(potions.getCooldown(PotionEffect.ANTIVENOM).isReady());

        fc.varp(VarPlayerID.POISON, 5); // actually poisoned
        potions.onGameTick(fc.client);
        Assert.assertTrue(potions.getCooldown(PotionEffect.ANTIPOISON).isReady());
        Assert.assertTrue(potions.getCooldown(PotionEffect.ANTIVENOM).isReady());
    }

    @Test
    public void flagAndChatEffectsAndDebugTimers()
    {
        FakeClient fc = new FakeClient().varbit(VarbitID.TOA_MIDRAIDLOOT_ENERGY_ACTIVE, 1);
        PotionTimers potions = new PotionTimers();
        potions.onGameTick(fc.client);
        Assert.assertEquals(250, potions.getCooldown(PotionEffect.LIQUID_ADRENALINE).getRemainingTicks());
        fc.varbit(VarbitID.TOA_MIDRAIDLOOT_ENERGY_ACTIVE, 0);
        potions.onGameTick(fc.client);
        Assert.assertTrue(potions.getCooldown(PotionEffect.LIQUID_ADRENALINE).isReady());

        potions.onChatMessage("You quickly apply the dressing to your wounds.");
        Assert.assertEquals(100, potions.getCooldown(PotionEffect.SILK_DRESSING).getRemainingTicks());
        potions.onChatMessage("You crack the crystal in your hand.");
        Assert.assertEquals(40, potions.getCooldown(PotionEffect.BLESSED_CRYSTAL_SCARAB).getRemainingTicks());

        fc.varbit(VarbitID.STAMINA_DURATION, 10);
        potions.onGameTick(fc.client);
        potions.triggerDebugCooldowns();
        Assert.assertEquals(25, potions.getCooldown(PotionEffect.STAMINA).getRemainingTicks());
        potions.clearDebugCooldowns();
        potions.onGameTick(fc.client);
        Assert.assertEquals("Real stamina timer resumes after debug is cleared", 100, potions.getCooldown(PotionEffect.STAMINA).getRemainingTicks());

        potions.onPlayerDeath();
        Assert.assertTrue(potions.getCooldown(PotionEffect.SILK_DRESSING).isReady());
    }

    // ---------------------------------------------------------------- hearts

    @Test
    public void heartFollowsVarbitResetsOnDeathAndDebugIsIsolated()
    {
        FakeClient fc = new FakeClient().varbit(VarbitID.IMBUED_HEART_TIMER, 70);
        HeartCooldownTracker heart = new HeartCooldownTracker();
        Assert.assertTrue(heart.getCooldown(HeartCooldownTracker.IMBUED_HEART_ID).isReady());

        heart.onGameTick(fc.client, false);
        Assert.assertEquals(700, heart.getCooldown(HeartCooldownTracker.IMBUED_HEART_ID).getRemainingTicks());
        Assert.assertEquals(700, heart.getCooldown(HeartCooldownTracker.IMBUED_HEART_ID).getTotalTicks());
        heart.onGameTick(fc.client, false);
        Assert.assertEquals(699, heart.getCooldown(HeartCooldownTracker.IMBUED_HEART_ID).getRemainingTicks());
        Assert.assertTrue(heart.wasUsedThisSession());

        // Death clears the game varbit -> heart ready immediately (Imbued Heart Notifier issue #5)
        fc.varbit(VarbitID.IMBUED_HEART_TIMER, 0);
        heart.onGameTick(fc.client, false);
        Assert.assertTrue(heart.getCooldown(HeartCooldownTracker.IMBUED_HEART_ID).isReady());

        heart.triggerDebugCooldown();
        heart.onGameTick(fc.client, true);
        Assert.assertEquals(24, heart.getCooldown(HeartCooldownTracker.SATURATED_HEART_ID).getRemainingTicks());
        Assert.assertEquals(500, heart.getCooldown(HeartCooldownTracker.SATURATED_HEART_ID).getTotalTicks());
        heart.clearDebugCooldown();
        Assert.assertTrue(heart.getCooldown(HeartCooldownTracker.IMBUED_HEART_ID).isReady());
    }

    // ---------------------------------------------------------------- boosts

    @Test
    public void boostDecayClockIsNotResetByDrinking()
    {
        FakeClient fc = new FakeClient().level(Skill.STRENGTH, 99, 99);
        BoostPotionTimers boosts = new BoostPotionTimers();
        for (int i = 0; i < 30; i++)
        {
            boosts.onGameTick(fc.client);
        }
        int before = boosts.getTicksUntilNextDecay();

        fc.level(Skill.STRENGTH, 99, 118); // drink a super strength
        boosts.onGameTick(fc.client);
        Assert.assertEquals("Drinking must not restart the global decay clock", before - 1, boosts.getTicksUntilNextDecay());

        fc.level(Skill.STRENGTH, 99, 117); // natural decay observed -> resync
        boosts.onGameTick(fc.client);
        Assert.assertEquals(100, boosts.getTicksUntilNextDecay());

        Assert.assertFalse(boosts.getCooldown(fc.client, BoostPotion.SUPER_STRENGTH, 3).isReady());
        Assert.assertTrue(boosts.wasUsedThisSession(BoostPotion.SUPER_STRENGTH));
        // A regular strength potion's max boost (12) is already exceeded by 18 -> still "not ready" to re-dose
        Assert.assertFalse(boosts.getCooldown(fc.client, BoostPotion.STRENGTH, 3).isReady());

        fc.level(Skill.STRENGTH, 99, 103);
        Assert.assertTrue(boosts.getCooldown(fc.client, BoostPotion.SUPER_STRENGTH, 3).isReady());
    }

    @Test
    public void preserveStretchesDecayClock()
    {
        FakeClient fc = new FakeClient();
        BoostPotionTimers boosts = new BoostPotionTimers();
        boosts.onGameTick(fc.client);
        int normal = boosts.getTicksUntilNextDecay();
        fc.varbit(VarbitID.PRAYER_PRESERVE, 1);
        boosts.onGameTick(fc.client);
        Assert.assertTrue(boosts.getTicksUntilNextDecay() > normal);
    }

    // ---------------------------------------------------------------- re-dose protection

    @Test
    public void repotProtectionStripsColorTagsAndDeprioritizesActivePotions()
    {
        HeartCooldownTracker heart = new HeartCooldownTracker();
        PotionTimers potions = new PotionTimers();
        BoostPotionTimers boosts = new BoostPotionTimers();
        RepotProtection repot = new RepotProtection();

        FakeClient state = new FakeClient().varbit(VarbitID.SUPER_ANTIFIRE_POTION, 12);
        potions.onGameTick(state.client);

        final boolean[] deprioritized = {false, false};
        MenuEntry antifire = menuEntry("Drink", 22209, "<col=ff9040>Super antifire potion(4)</col>", deprioritized, 0);
        MenuEntry stamina = menuEntry("Drink", 12625, "<col=ff9040>Stamina potion(4)</col>", deprioritized, 1);
        FakeClient menuClient = new FakeClient();
        net.runelite.api.Menu menu = (net.runelite.api.Menu) Proxy.newProxyInstance(
            net.runelite.api.Menu.class.getClassLoader(),
            new Class<?>[]{net.runelite.api.Menu.class},
            (p, m, a) -> "getMenuEntries".equals(m.getName())
                ? new MenuEntry[]{antifire, stamina}
                : FakeClient.defaultValue(m.getReturnType())
        );
        Object proxy = Proxy.newProxyInstance(
            net.runelite.api.Client.class.getClassLoader(),
            new Class<?>[]{net.runelite.api.Client.class},
            (p, m, a) -> "getMenu".equals(m.getName())
                ? menu
                : m.getName().equals("getVarbitValue") || m.getName().endsWith("SkillLevel") ? 1 : FakeClient.defaultValue(m.getReturnType())
        );

        repot.onPostMenuSort((net.runelite.api.Client) proxy, DEFAULTS, heart, potions, boosts);
        Assert.assertTrue("Active super antifire is deprioritized", deprioritized[0]);
        Assert.assertFalse("Inactive stamina stays left-click", deprioritized[1]);

        ImmersiveCombatOverlaysConfig off = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean repotProtectionEnabled() { return false; }
        };
        deprioritized[0] = false;
        repot.onPostMenuSort((net.runelite.api.Client) proxy, off, heart, potions, boosts);
        Assert.assertFalse(deprioritized[0]);
        Assert.assertNotNull(menuClient.client);
    }

    private static MenuEntry menuEntry(String option, int itemId, String target, boolean[] flags, int idx)
    {
        return (MenuEntry) Proxy.newProxyInstance(
            MenuEntry.class.getClassLoader(),
            new Class<?>[]{MenuEntry.class},
            (p, m, a) -> {
                switch (m.getName())
                {
                    case "getOption": return option;
                    case "getItemId": return itemId;
                    case "getTarget": return target;
                    case "setDeprioritized":
                        flags[idx] = (Boolean) a[0];
                        return null;
                    default:
                        return FakeClient.defaultValue(m.getReturnType());
                }
            }
        );
    }

    // ---------------------------------------------------------------- food

    @Test
    public void comboEatFoodCooldownsAndHunterMeatDelayedHeal()
    {
        FoodCooldownTracker food = new FoodCooldownTracker();
        food.onConsume("Eat", "Shark");
        Assert.assertNotNull(food.getCooldown("Shark"));
        Assert.assertNull(food.getCooldown("Saradomin brew(4)"));
        Assert.assertNull("Karambwan can still be combo-eaten after normal food", food.getCooldown("Cooked karambwan"));

        food.onConsume("Drink", "Saradomin brew(4)");
        Assert.assertNotNull(food.getCooldown("Saradomin brew(4)"));
        food.onConsume("Eat", "Cooked karambwan");
        Assert.assertNotNull(food.getCooldown("Cooked karambwan"));
        food.onConsume("Wield", "Shark");

        food.reset();
        food.onConsume("Eat", "Cooked sunlight antelope");
        Assert.assertEquals(3, food.getCooldown("Cooked sunlight antelope").getRemainingTicks());
        food.onGameTick();
        food.onGameTick();
        food.onGameTick();
        Assert.assertEquals("Second heal pending", 4, food.getCooldown("Cooked sunlight antelope").getRemainingTicks());
        Assert.assertNull(FoodCooldownTracker.classify("Rune platebody"));
    }

    // ---------------------------------------------------------------- freeze & impact

    @Test
    public void freezeSpotAnimsChatAndMovement()
    {
        FreezeTracker freeze = new FreezeTracker();
        long t0 = 10_000_000_000L;
        freeze.onGraphicChanged(null, 181, t0); // Bind
        Assert.assertTrue(freeze.getSeverity(t0 + 100_000_000L) > 0.2f);
        freeze.reset();
        freeze.onGraphicChanged(null, 369, t0); // Ice Barrage
        Assert.assertTrue(freeze.getSeverity(t0 + 1_000_000_000L) > 0.2f);
        freeze.onChatMessage(null, "You have been unfrozen.", t0 + 1_200_000_000L);
        Assert.assertEquals(0f, freeze.getSeverity(t0 + 1_200_000_000L), 0.001f);

        freeze.onChatMessage(null, "The Whisperer binds you in place!", t0 + 2_000_000_000L);
        Assert.assertTrue(freeze.getSeverity(t0 + 2_100_000_000L) > 0.2f);
        ImmersiveCombatOverlaysConfig capped = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public int freezeMaxDisplaySeconds() { return 3; }
        };
        Assert.assertTrue(freeze.getSeverity(capped, t0 + 4_000_000_000L) > 0.2f);
        Assert.assertEquals(0f, freeze.getSeverity(capped, t0 + 5_100_000_000L), 0.001f);

        // Moving off the frozen tile ends the freeze; turning in place (pose change) does not
        final WorldPoint[] loc = {new WorldPoint(3200, 3200, 0)};
        final int[] pose = {808};
        Player local = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
            (p, m, a) -> {
                switch (m.getName())
                {
                    case "getWorldLocation": return loc[0];
                    case "getPoseAnimation": return pose[0];
                    case "getIdlePoseAnimation": return 808;
                    default: return FakeClient.defaultValue(m.getReturnType());
                }
            });
        FakeClient fc = new FakeClient();
        fc.localPlayer = local;
        freeze.reset();
        freeze.onGraphicChanged(local, t0); // no freeze spotanim on the player -> nothing
        Assert.assertEquals(0f, freeze.getSeverity(t0), 0.001f);
        freeze.onGraphicChanged(local, 369, t0);
        pose[0] = 823; // rotate
        freeze.onGameTick(fc.client);
        Assert.assertTrue(freeze.getSeverity(t0 + 600_000_000L) > 0f);
        loc[0] = new WorldPoint(3201, 3200, 0);
        freeze.onGameTick(fc.client);
        Assert.assertEquals(0f, freeze.getSeverity(t0 + 1_200_000_000L), 0.001f);
    }

    @Test
    public void impactTrackerDecaysAndStacks()
    {
        ImpactTracker impact = new ImpactTracker();
        long t0 = 10_000_000_000L;
        Assert.assertEquals(0f, impact.getSeverity(t0, 600_000_000L), 0.001f);
        impact.trigger(t0, 0.9f);
        Assert.assertEquals(0.9f, impact.getSeverity(t0, 600_000_000L), 0.001f);
        Assert.assertTrue(impact.getSeverity(t0 + 100_000_000L, 600_000_000L) > 0.4f);
        Assert.assertEquals(0f, impact.getSeverity(t0 + 650_000_000L, 600_000_000L), 0.001f);
    }

    @Test
    public void onlyRealDamageHitsplatsFlash()
    {
        Assert.assertTrue(ImmersiveCombatOverlaysPlugin.isDamageHitsplat(HitsplatID.DAMAGE_ME));
        Assert.assertTrue(ImmersiveCombatOverlaysPlugin.isDamageHitsplat(HitsplatID.DAMAGE_MAX_ME));
        Assert.assertTrue(ImmersiveCombatOverlaysPlugin.isDamageHitsplat(HitsplatID.BURN));
        Assert.assertFalse(ImmersiveCombatOverlaysPlugin.isDamageHitsplat(HitsplatID.HEAL));
        Assert.assertFalse(ImmersiveCombatOverlaysPlugin.isDamageHitsplat(HitsplatID.PRAYER_DRAIN));
        Assert.assertFalse(ImmersiveCombatOverlaysPlugin.isDamageHitsplat(HitsplatID.CYAN_UP));
        Assert.assertFalse(ImmersiveCombatOverlaysPlugin.isDamageHitsplat(HitsplatID.BLOCK_ME));
    }

    // ---------------------------------------------------------------- idle

    @Test
    public void idleTrackerMovementRampAutoHideAndInputDismiss()
    {
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean idleAutoHide() { return true; }

            @Override
            public int idleAutoHideSeconds() { return 5; }
        };

        final int[] pose = {808};
        final int[] anim = {-1};
        final int[] kbIdle = {200};
        Player local = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
            (p, m, a) -> {
                switch (m.getName())
                {
                    case "getPoseAnimation": return pose[0];
                    case "getIdlePoseAnimation": return 808;
                    case "getAnimation": return anim[0];
                    default: return FakeClient.defaultValue(m.getReturnType());
                }
            });
        net.runelite.api.Client client = (net.runelite.api.Client) Proxy.newProxyInstance(
            net.runelite.api.Client.class.getClassLoader(), new Class<?>[]{net.runelite.api.Client.class},
            (p, m, a) -> {
                switch (m.getName())
                {
                    case "getLocalPlayer": return local;
                    case "getKeyboardIdleTicks": return kbIdle[0];
                    case "getMouseIdleTicks": return 200;
                    default: return FakeClient.defaultValue(m.getReturnType());
                }
            });

        CombatStateTracker combat = new CombatStateTracker();
        IdleTracker idle = new IdleTracker();
        long t0 = 100_000_000_000L;

        pose[0] = 824; // walking
        idle.onGameTick(client, cfg, combat, null, t0);
        Assert.assertEquals(0f, idle.getSeverity(cfg, t0 + 10_000_000_000L), 0.001f);
        pose[0] = 808;

        anim[0] = 867;
        idle.onAnimationChanged(client, local);
        idle.onGameTick(client, cfg, combat, null, t0 + 2_000_000_000L);
        anim[0] = -1;
        long stopAt = t0 + 3_000_000_000L;
        idle.onGameTick(client, cfg, combat, null, stopAt);
        Assert.assertEquals(0f, idle.getSeverity(cfg, stopAt + 2_000_000_000L), 0.001f);
        Assert.assertTrue(idle.getSeverity(cfg, stopAt + 6_500_000_000L) > 0.3f);
        Assert.assertEquals(1.0f, idle.getSeverity(cfg, stopAt + 8_500_000_000L), 0.001f);
        Assert.assertEquals("auto-hidden", 0f, idle.getSeverity(cfg, stopAt + 14_000_000_000L), 0.001f);

        kbIdle[0] = 5; // keyboard input dismisses immediately
        idle.onGameTick(client, cfg, combat, null, stopAt + 9_000_000_000L);
        Assert.assertEquals(0f, idle.getSeverity(cfg, stopAt + 9_000_000_000L), 0.001f);

        ImmersiveCombatOverlaysConfig combatOnly = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean idleOnlyInCombat() { return true; }
        };
        kbIdle[0] = 200;
        idle.onGameTick(client, combatOnly, combat, null, stopAt + 30_000_000_000L);
        Assert.assertEquals(0f, idle.getSeverity(combatOnly, stopAt + 60_000_000_000L), 0.001f);
    }

    // ---------------------------------------------------------------- unpotted

    @Test
    public void unpottedIgnoresAlchAndLongrangeButSupportsCannon()
    {
        CombatStateTracker combat = new CombatStateTracker();
        UnpottedTracker unpotted = new UnpottedTracker();
        long now = 50_000_000_000L;

        unpotted.onStatChanged(null, DEFAULTS, combat, null, Skill.MAGIC, 1000, 99, 99, now);
        unpotted.onStatChanged(null, DEFAULTS, combat, null, Skill.RANGED, 1000, 99, 99, now);
        unpotted.onStatChanged(null, DEFAULTS, combat, null, Skill.DEFENCE, 1000, 99, 99, now);

        // Magic XP out of combat (alching) is ignored
        unpotted.onStatChanged(null, DEFAULTS, combat, null, Skill.MAGIC, 1065, 99, 99, now + 1_000_000L);
        unpotted.onGameTick(null, DEFAULTS, 0L, now + 2_000_000L);
        Assert.assertEquals(0f, unpotted.getSeverity(DEFAULTS, now + 2_000_000L), 0.001f);

        // Defence XP never triggers
        unpotted.onStatChanged(null, DEFAULTS, combat, null, Skill.DEFENCE, 1100, 99, 99, now + 1_000_000L);
        Assert.assertEquals(0f, unpotted.getSeverity(DEFAULTS, now + 2_000_000L), 0.001f);

        // Ranged XP without combat interaction (cannon) does trigger
        unpotted.onStatChanged(null, DEFAULTS, combat, null, Skill.RANGED, 1120, 99, 99, now + 2_000_000L);
        Assert.assertTrue(unpotted.getSeverity(DEFAULTS, now + 3_000_000L) > 0.5f);

        // Potting up clears it
        unpotted.onStatChanged(null, DEFAULTS, combat, null, Skill.RANGED, 1150, 112, 99, now + 4_000_000L);
        Assert.assertEquals(0f, unpotted.getSeverity(DEFAULTS, now + 4_000_000L), 0.001f);
    }

    @Test
    public void unpottedIgnoresMagicXpFromUtilitySpellCasts()
    {
        CombatStateTracker combat = new CombatStateTracker();
        combat.setSimulatedCombat(true);
        UnpottedTracker unpotted = new UnpottedTracker();
        long now = 50_000_000_000L;
        unpotted.onStatChanged(null, DEFAULTS, combat, null, Skill.MAGIC, 1000, 99, 99, now);

        // Thrall cast: XP drop, then the spell tracker records a cast on the same tick
        unpotted.onStatChanged(null, DEFAULTS, combat, null, Skill.MAGIC, 1050, 99, 99, now + 100_000_000L);
        unpotted.onGameTick(null, DEFAULTS, now + 150_000_000L, now + 600_000_000L);
        Assert.assertEquals("Thrall/Death Charge XP must not trigger (Unpotted Reminder #23)", 0f, unpotted.getSeverity(DEFAULTS, now + 700_000_000L), 0.001f);

        // Real combat magic XP with no utility cast nearby does trigger
        unpotted.onStatChanged(null, DEFAULTS, combat, null, Skill.MAGIC, 1100, 99, 99, now + 10_000_000_000L);
        unpotted.onGameTick(null, DEFAULTS, now + 150_000_000L, now + 10_600_000_000L);
        Assert.assertTrue(unpotted.getSeverity(DEFAULTS, now + 10_700_000_000L) > 0.5f);
    }

    @Test
    public void unpottedSuppressedWhileOverloadOrSaltsActive()
    {
        CombatStateTracker combat = new CombatStateTracker();
        combat.onCombatActivity(1_000_000_000L, 120);
        PotionTimers potions = new PotionTimers();
        UnpottedTracker unpotted = new UnpottedTracker();
        long now = 2_000_000_000L;

        potions.onGameTick(new FakeClient().varbit(VarbitID.TOA_MIDRAIDLOOT_STATS_TIMER, 10).client);
        unpotted.onStatChanged(null, DEFAULTS, combat, potions, Skill.ATTACK, 1000, 99, 99, now);
        unpotted.onStatChanged(null, DEFAULTS, combat, potions, Skill.ATTACK, 1100, 95, 99, now + 1_000_000L);
        Assert.assertEquals(0f, unpotted.getSeverity(DEFAULTS, now + 1_000_000L), 0.001f);
    }

    // ---------------------------------------------------------------- inventory ready highlight

    @Test
    public void readyStrengthSemantics()
    {
        long sec = 1_000_000_000L;
        Assert.assertEquals(0f, CooldownRenderer.readyStrength(ReadyStyle.NONE, false, 4, 0L, 0L), 0.001f);
        Assert.assertEquals(1f, CooldownRenderer.readyStrength(ReadyStyle.SINGLE_FLASH, false, 4, 0L, 0L), 0.001f);
        float mid = CooldownRenderer.readyStrength(ReadyStyle.SINGLE_FLASH, false, 4, 2 * sec, 0L);
        Assert.assertTrue("single flash fades", mid > 0f && mid < 1f);
        Assert.assertEquals("timed highlight ends", 0f, CooldownRenderer.readyStrength(ReadyStyle.SOLID, false, 4, 5 * sec, 0L), 0.001f);
        Assert.assertEquals("permanent highlight stays", 1f, CooldownRenderer.readyStrength(ReadyStyle.SOLID, true, 4, 500 * sec, 0L), 0.001f);
        Assert.assertTrue("permanent single flash settles dim", CooldownRenderer.readyStrength(ReadyStyle.SINGLE_FLASH, true, 4, 500 * sec, 0L) > 0.3f);
        float pulse = CooldownRenderer.readyStrength(ReadyStyle.PULSE, true, 4, sec, 123 * sec);
        Assert.assertTrue(pulse >= 0.45f && pulse <= 1f);
    }

    @Test
    public void inventoryReadyHighlightOnlyAfterACooldownEnds()
    {
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public ReadyStyle readyStyle() { return ReadyStyle.SOLID; }

            @Override
            public AnimationMode animationMode() { return AnimationMode.TIMED; }
        };
        InventoryCooldownOverlay overlay = new InventoryCooldownOverlay(null, cfg, null, null, null, null, null, null, null);
        long t = 10_000_000_000L;

        Assert.assertEquals("Unused potion never glows", 0f, overlay.trackReady(3, 2434, true, t), 0.001f);
        Assert.assertEquals(0f, overlay.trackReady(3, 2434, false, t + 1), 0.001f);
        Assert.assertEquals("Glows when cooldown ends", 1f, overlay.trackReady(3, 2434, true, t + 2), 0.001f);
        Assert.assertEquals("Timed highlight expires", 0f, overlay.trackReady(3, 2434, true, t + 10_000_000_000L), 0.001f);
        Assert.assertEquals("...and does not come back", 0f, overlay.trackReady(3, 2434, true, t + 11_000_000_000L), 0.001f);

        overlay.trackReady(4, 2434, false, t);
        Assert.assertEquals("Different item in the slot resets tracking", 0f, overlay.trackReady(4, 2436, true, t + 1), 0.001f);
        Assert.assertEquals("Out-of-range slots are ignored", 0f, overlay.trackReady(-1, 2434, true, t), 0.001f);
    }
}
