package com.immersivecombatoverlays;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.BoostPotion;
import com.immersivecombatoverlays.model.PotionEffect;
import com.immersivecombatoverlays.overlay.InventoryCooldownOverlay;
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
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import org.junit.Assert;
import org.junit.Test;

public class TrackersTest
{
    @Test
    public void testPotionItemMatchingAllFamilies()
    {
        Assert.assertEquals(PotionEffect.SURGE, PotionItems.getEffect(1, "Surge potion(4)"));
        Assert.assertEquals(PotionEffect.OVERLOAD, PotionItems.getEffect(1, "Overload (+) (4)"));
        Assert.assertEquals(PotionEffect.BLIGHTED_OVERLOAD, PotionItems.getEffect(1, "Blighted overload(4)"));
        Assert.assertEquals(PotionEffect.SMELLING_SALTS, PotionItems.getEffect(1, "Smelling salts(2)"));
        Assert.assertEquals(PotionEffect.LIQUID_ADRENALINE, PotionItems.getEffect(1, "Liquid adrenaline(2)"));
        Assert.assertEquals(PotionEffect.SILK_DRESSING, PotionItems.getEffect(1, "Silk dressing(2)"));
        Assert.assertEquals(PotionEffect.BLESSED_CRYSTAL_SCARAB, PotionItems.getEffect(1, "Blessed crystal scarab(2)"));
        Assert.assertEquals(PotionEffect.DIVINE_COMBAT, PotionItems.getEffect(1, "Divine super combat potion(4)"));
        Assert.assertEquals(PotionEffect.DIVINE_BASTION, PotionItems.getEffect(1, "Divine bastion potion(4)"));
        Assert.assertEquals(PotionEffect.SUPER_ANTIFIRE, PotionItems.getEffect(1, "Extended super antifire(4)"));
        Assert.assertEquals(PotionEffect.SUPER_ANTIFIRE, PotionItems.getEffect(1, "Super antifire potion(4)"));
        Assert.assertEquals(PotionEffect.ANTIFIRE, PotionItems.getEffect(1, "Antifire potion(4)"));
        Assert.assertEquals(PotionEffect.ANTIFIRE, PotionItems.getEffect(1, "Extended antifire(4)"));
        Assert.assertEquals(PotionEffect.ANTIVENOM, PotionItems.getEffect(1, "Anti-venom+(4)"));
        Assert.assertEquals(PotionEffect.PRAYER_REGENERATION, PotionItems.getEffect(1, "Prayer regeneration potion(4)"));
        Assert.assertEquals(PotionEffect.GUTHIX_REST, PotionItems.getEffect(1, "Guthix rest(4)"));

        Assert.assertEquals(BoostPotion.SUPER_COMBAT, PotionItems.getBoost(1, "Super combat potion(4)"));
        Assert.assertEquals(BoostPotion.BASTION, PotionItems.getBoost(1, "Bastion potion(4)"));
        Assert.assertEquals(BoostPotion.MAGIC, PotionItems.getBoost(1, "Forgotten brew(4)"));
        Assert.assertEquals(BoostPotion.RANGED, PotionItems.getBoost(1, "Armadyl brew(4)"));
        Assert.assertNull(PotionItems.getBoost(1, "Divine super combat potion(4)"));

        Assert.assertTrue(InventoryCooldownOverlay.isPrayerRestoreItem("Super restore(4)"));
        Assert.assertTrue(InventoryCooldownOverlay.isPrayerRestoreItem("Prayer potion(4)"));
        Assert.assertFalse(InventoryCooldownOverlay.isPrayerRestoreItem("Shark"));
    }

    @Test
    public void testHeartAndPotionDebugTimers()
    {
        HeartCooldownTracker heart = new HeartCooldownTracker();
        Assert.assertTrue(heart.getCooldown(HeartCooldownTracker.IMBUED_HEART_ID).isReady());
        heart.triggerDebugCooldown();
        Assert.assertFalse(heart.getCooldown(HeartCooldownTracker.SATURATED_HEART_ID).isReady());
        heart.reset();
        Assert.assertTrue(heart.getCooldown(HeartCooldownTracker.IMBUED_HEART_ID).isReady());

        PotionTimers potions = new PotionTimers();
        potions.onChatMessage("You drink some of your surge potion.");
        Assert.assertFalse(potions.getCooldown(PotionEffect.SURGE).isReady());
        potions.onChatMessage("You apply a silk dressing.");
        Assert.assertFalse(potions.getCooldown(PotionEffect.SILK_DRESSING).isReady());
        potions.triggerDebugCooldowns();
        Assert.assertFalse(potions.getCooldown(PotionEffect.STAMINA).isReady());
        potions.reset();
        Assert.assertTrue(potions.getCooldown(PotionEffect.STAMINA).isReady());
    }

    @Test
    public void testFreezeSpotAnimsAndImpactTrackers()
    {
        FreezeTracker freeze = new FreezeTracker();
        long t0 = 10_000_000_000L;
        freeze.onGraphicChanged(null, 181, t0); // Bind (8t)
        Assert.assertTrue(freeze.getSeverity(t0 + 100_000_000L) > 0.2f);
        freeze.reset();
        freeze.onGraphicChanged(null, 369, t0); // Ice Barrage (33t)
        Assert.assertTrue(freeze.getSeverity(t0 + 1_000_000_000L) > 0.2f);
        freeze.onChatMessage(null, "You have been unfrozen.", t0 + 1_200_000_000L);
        Assert.assertEquals(0f, freeze.getSeverity(t0 + 1_200_000_000L), 0.001f);
        freeze.onChatMessage(null, "The Whisperer binds you in place!", t0 + 2_000_000_000L);
        Assert.assertTrue(freeze.getSeverity(t0 + 2_100_000_000L) > 0.2f);
        ImmersiveCombatOverlaysConfig fzCapCfg = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public int freezeMaxDisplaySeconds() { return 3; }
        };
        Assert.assertTrue(freeze.getSeverity(fzCapCfg, t0 + 4_000_000_000L) > 0.2f);
        Assert.assertEquals(0f, freeze.getSeverity(fzCapCfg, t0 + 5_100_000_000L), 0.001f);
        freeze.reset();
        Assert.assertEquals(0f, freeze.getSeverity(t0 + 2_100_000_000L), 0.001f);

        ImpactTracker impact = new ImpactTracker();
        impact.trigger(t0, 0.9f);
        Assert.assertTrue(impact.getSeverity(t0 + 100_000_000L, 600_000_000L) > 0.4f);
        Assert.assertEquals(0f, impact.getSeverity(t0 + 650_000_000L, 600_000_000L), 0.001f);
    }

    @Test
    public void testIdleTrackerMovementAndRampAndAutoHide()
    {
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean idleAutoHide() { return true; }

            @Override
            public int idleAutoHideSeconds() { return 5; }
        };

        final int[] pose = new int[]{808, 808}; // poseAnimation, idlePoseAnimation
        final int[] anim = new int[]{-1};
        final int[] kbIdle = new int[]{200};
        final int[] mouseIdle = new int[]{200};
        final Object[] dest = new Object[]{null};

        Player local = (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[]{Player.class},
            (p, m, a) -> {
                if ("getPoseAnimation".equals(m.getName())) return pose[0];
                if ("getIdlePoseAnimation".equals(m.getName())) return pose[1];
                if ("getAnimation".equals(m.getName())) return anim[0];
                return null;
            }
        );
        Client client = (Client) Proxy.newProxyInstance(
            Client.class.getClassLoader(),
            new Class<?>[]{Client.class},
            (p, m, a) -> {
                if ("getLocalPlayer".equals(m.getName())) return local;
                if ("getLocalDestinationLocation".equals(m.getName())) return dest[0];
                if ("getMouseLastPressedMillis".equals(m.getName())) return 0L;
                if ("getKeyboardIdleTicks".equals(m.getName())) return kbIdle[0];
                if ("getMouseIdleTicks".equals(m.getName())) return mouseIdle[0];
                return null;
            }
        );

        CombatStateTracker combat = new CombatStateTracker();
        IdleTracker idle = new IdleTracker();
        long t0 = 100_000_000_000L;

        // Walking (pose != idlePose) must stay 0f
        pose[0] = 824;
        idle.onGameTick(client, cfg, combat, null, t0);
        Assert.assertEquals(0f, idle.getSeverity(cfg, t0 + 10_000_000_000L), 0.001f);
        pose[0] = 808;

        // Perform a skilling/combat animation, then stop
        anim[0] = 867;
        idle.onAnimationChanged(client, local);
        idle.onGameTick(client, cfg, combat, null, t0 + 2_000_000_000L);
        anim[0] = -1;
        long stopAt = t0 + 3_000_000_000L;
        idle.onGameTick(client, cfg, combat, null, stopAt);
        Assert.assertEquals(0f, idle.getSeverity(cfg, stopAt + 2_000_000_000L), 0.001f); // < 5s
        Assert.assertTrue(idle.getSeverity(cfg, stopAt + 6_500_000_000L) > 0.3f); // between 5s and 8s
        Assert.assertEquals(1.0f, idle.getSeverity(cfg, stopAt + 8_500_000_000L), 0.001f); // >= 8s critical
        Assert.assertEquals(0f, idle.getSeverity(cfg, stopAt + 14_000_000_000L), 0.001f); // auto-hidden after 5s

        // Moving the camera/keyboard/mouse must immediately dismiss an active idle warning
        kbIdle[0] = 5;
        idle.onGameTick(client, cfg, combat, null, stopAt + 9_000_000_000L);
        Assert.assertEquals(0f, idle.getSeverity(cfg, stopAt + 9_000_000_000L), 0.001f);
        kbIdle[0] = 200;

        // Moving pose must suppress idle even after an action
        anim[0] = 867;
        idle.onAnimationChanged(client, local);
        anim[0] = -1;
        pose[0] = 824;
        idle.onGameTick(client, cfg, combat, null, stopAt + 15_000_000_000L);
        Assert.assertEquals(0f, idle.getSeverity(cfg, stopAt + 25_000_000_000L), 0.001f);
    }

    @Test
    public void testComboEatFoodCooldownsAndHunterMeatDelayedHeal()
    {
        FoodCooldownTracker food = new FoodCooldownTracker();
        food.onConsume("Eat", "Shark");
        Assert.assertNotNull(food.getCooldown("Shark"));
        Assert.assertNull(food.getCooldown("Saradomin brew(4)"));
        Assert.assertNull(food.getCooldown("Cooked karambwan"));

        food.onConsume("Drink", "Saradomin brew(4)");
        Assert.assertNotNull(food.getCooldown("Saradomin brew(4)"));
        Assert.assertNull(food.getCooldown("Cooked karambwan"));

        food.onConsume("Eat", "Cooked karambwan");
        Assert.assertNotNull(food.getCooldown("Cooked karambwan"));

        food.reset();
        food.onConsume("Eat", "Cooked sunlight antelope");
        Assert.assertEquals(3, food.getCooldown("Cooked sunlight antelope").getRemainingTicks());
        food.onGameTick();
        food.onGameTick();
        food.onGameTick();
        Assert.assertEquals(4, food.getCooldown("Cooked sunlight antelope").getRemainingTicks());
    }

    @Test
    public void testUnpottedTrackerIgnoresAlchingAndSupportsCannonRanged()
    {
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig() {};
        CombatStateTracker combat = new CombatStateTracker();
        UnpottedTracker unpotted = new UnpottedTracker();
        long now = 50_000_000_000L;

        unpotted.onStatChanged(null, cfg, combat, null, Skill.MAGIC, 1000, 99, 99, now);
        unpotted.onStatChanged(null, cfg, combat, null, Skill.RANGED, 1000, 99, 99, now);

        unpotted.onStatChanged(null, cfg, combat, null, Skill.MAGIC, 1065, 99, 99, now + 1_000_000L);
        Assert.assertEquals(0f, unpotted.getSeverity(cfg, now + 1_000_000L), 0.001f);

        unpotted.onStatChanged(null, cfg, combat, null, Skill.RANGED, 1120, 99, 99, now + 2_000_000L);
        Assert.assertTrue(unpotted.getSeverity(cfg, now + 3_000_000L) > 0.5f);

        unpotted.onStatChanged(null, cfg, combat, null, Skill.RANGED, 1150, 112, 99, now + 4_000_000L);
        Assert.assertEquals(0f, unpotted.getSeverity(cfg, now + 4_000_000L), 0.001f);
    }

    @Test
    public void testIdleTrackerAfkLogoutWarning()
    {
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig() {};
        final int[] kbIdle = new int[]{240 * 50};
        final int[] mouseIdle = new int[]{240 * 50};

        Player local = (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[]{Player.class},
            (p, m, a) -> {
                if ("getPoseAnimation".equals(m.getName())) return 808;
                if ("getIdlePoseAnimation".equals(m.getName())) return 808;
                if ("getAnimation".equals(m.getName())) return -1;
                return null;
            }
        );
        Client client = (Client) Proxy.newProxyInstance(
            Client.class.getClassLoader(),
            new Class<?>[]{Client.class},
            (p, m, a) -> {
                if ("getLocalPlayer".equals(m.getName())) return local;
                if ("getMouseLastPressedMillis".equals(m.getName())) return 0L;
                if ("getKeyboardIdleTicks".equals(m.getName())) return kbIdle[0];
                if ("getMouseIdleTicks".equals(m.getName())) return mouseIdle[0];
                return null;
            }
        );

        IdleTracker idle = new IdleTracker();
        idle.onGameTick(client, cfg, new CombatStateTracker(), null, 50_000_000_000L);
        Assert.assertEquals(1.0f, idle.getSeverity(cfg, 50_000_000_000L + 9_000_000_000L), 0.001f);
    }

    @Test
    public void testUnpottedIgnoresLongrangeDefenceAndOverloadBrewDip()
    {
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig() {};
        CombatStateTracker combat = new CombatStateTracker();
        combat.onCombatActivity(1_000_000_000L, 120);
        PotionTimers potions = new PotionTimers();
        UnpottedTracker unpotted = new UnpottedTracker();
        long now = 2_000_000_000L;

        // Longrange Defence XP must not false-trigger unpotted alert
        unpotted.onStatChanged(null, cfg, combat, potions, Skill.DEFENCE, 1000, 99, 99, now);
        unpotted.onStatChanged(null, cfg, combat, potions, Skill.DEFENCE, 1100, 99, 99, now + 1_000_000L);
        Assert.assertEquals(0f, unpotted.getSeverity(cfg, now + 1_000_000L), 0.001f);

        // Active Overload (via varbit) suppresses unpotted alert even if boostedLevel <= realLevel
        Client ovlClient = (Client) Proxy.newProxyInstance(
            Client.class.getClassLoader(),
            new Class<?>[]{Client.class},
            (prx, m, a) -> "getVarbitValue".equals(m.getName()) && (int) a[0] == PotionEffect.OVERLOAD.getVarId() ? 5 : 0
        );
        potions.onGameTick(ovlClient);
        unpotted.onStatChanged(null, cfg, combat, potions, Skill.ATTACK, 1000, 99, 99, now + 2_000_000L);
        unpotted.onStatChanged(null, cfg, combat, potions, Skill.ATTACK, 1100, 95, 99, now + 3_000_000L);
        Assert.assertEquals(0f, unpotted.getSeverity(cfg, now + 3_000_000L), 0.001f);
    }

    @Test
    public void testRepotProtectionStripsColorTagsAndDeprioritizesSuperAntifire()
    {
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig() {};
        HeartCooldownTracker heart = new HeartCooldownTracker();
        PotionTimers potions = new PotionTimers();
        BoostPotionTimers boosts = new BoostPotionTimers();
        RepotProtection repot = new RepotProtection();

        Client afClient = (Client) Proxy.newProxyInstance(
            Client.class.getClassLoader(),
            new Class<?>[]{Client.class},
            (prx, m, a) -> "getVarbitValue".equals(m.getName()) && (int) a[0] == PotionEffect.SUPER_ANTIFIRE.getVarId() ? 12 : 0
        );
        potions.onGameTick(afClient);
        final boolean[] deprioritized = new boolean[]{false};
        MenuEntry entry = (MenuEntry) Proxy.newProxyInstance(
            MenuEntry.class.getClassLoader(),
            new Class<?>[]{MenuEntry.class},
            (p, m, a) -> {
                if ("getOption".equals(m.getName())) return "Drink";
                if ("getItemId".equals(m.getName())) return 22209;
                if ("getTarget".equals(m.getName())) return "<col=ff9040>Super antifire potion(4)</col>";
                if ("setDeprioritized".equals(m.getName()))
                {
                    deprioritized[0] = (Boolean) a[0];
                    return null;
                }
                return null;
            }
        );
        Client client = (Client) Proxy.newProxyInstance(
            Client.class.getClassLoader(),
            new Class<?>[]{Client.class},
            (p, m, a) -> "getMenuEntries".equals(m.getName()) ? new MenuEntry[]{entry} : null
        );

        repot.onPostMenuSort(client, cfg, heart, potions, boosts);
        Assert.assertTrue(deprioritized[0]);
    }
}
