package com.immersivecombatoverlays;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.CombatSpell;
import com.immersivecombatoverlays.model.Cooldown;
import com.immersivecombatoverlays.model.ThrallDurationTier;
import com.immersivecombatoverlays.tracker.CombatSpellTracker;
import com.immersivecombatoverlays.tracker.CombatStateTracker;
import java.lang.reflect.Proxy;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Assert;
import org.junit.Test;

public class CombatSpellTrackerTest
{
    private static final ImmersiveCombatOverlaysConfig DEFAULTS = new ImmersiveCombatOverlaysConfig() {};

    private static ImmersiveCombatOverlaysConfig tier(ThrallDurationTier t)
    {
        return new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public ThrallDurationTier thrallDurationTier() { return t; }
        };
    }

    @Test
    public void spellVarbitsMatchRuneLiteGameVals()
    {
        // Guards against the original bug where Mark of Darkness / Shadow Veil / Ward pointed at unrelated varbits
        Assert.assertEquals(VarbitID.ARCEUUS_RESURRECTION_ACTIVE, CombatSpell.RESURRECT_THRALL.getActiveVarbitId());
        Assert.assertEquals(VarbitID.ARCEUUS_RESURRECTION_COOLDOWN, CombatSpell.RESURRECT_THRALL.getCooldownVarbitId());
        Assert.assertEquals(VarbitID.ARCEUUS_DEATH_CHARGE_COOLDOWN, CombatSpell.DEATH_CHARGE.getCooldownVarbitId());
        Assert.assertEquals(VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE, CombatSpell.SHADOW_VEIL.getActiveVarbitId());
        Assert.assertEquals(VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN, CombatSpell.SHADOW_VEIL.getCooldownVarbitId());
        Assert.assertEquals(VarbitID.ARCEUUS_WARD_COOLDOWN, CombatSpell.WARD_OF_ARCEUUS.getCooldownVarbitId());
        Assert.assertEquals(VarbitID.ARCEUUS_CORRUPTION_COOLDOWN, CombatSpell.CORRUPTION.getCooldownVarbitId());
        Assert.assertEquals(VarbitID.VENGEANCE_TIMELIMIT, CombatSpell.VENGEANCE.getCooldownVarbitId());
        Assert.assertEquals(-1, CombatSpell.MARK_OF_DARKNESS.getActiveVarbitId());

        Assert.assertEquals(17, CombatSpell.RESURRECT_THRALL.getCooldownTicks());
        Assert.assertEquals(100, CombatSpell.DEATH_CHARGE.getCooldownTicks());
        Assert.assertEquals(50, CombatSpell.CORRUPTION.getCooldownTicks());
        Assert.assertEquals(50, CombatSpell.VENGEANCE.getCooldownTicks());
        Assert.assertEquals(10, CombatSpell.MARK_OF_DARKNESS.getCooldownTicks());
    }

    @Test
    public void thrallDurationUsesBoostedMagicAndMasterTier()
    {
        FakeClient fc = new FakeClient().level(Skill.MAGIC, 99, 109);
        CombatSpellTracker tracker = new CombatSpellTracker();

        tracker.onChatMessage(fc.client, "<col=ef0083>You resurrect a greater ghost thrall.</col>", tier(ThrallDurationTier.STANDARD));
        Assert.assertEquals(109, tracker.getCooldown(CombatSpell.RESURRECT_THRALL).getRemainingTicks());

        tracker.onChatMessage(fc.client, "You resurrect a greater skeleton thrall.", tier(ThrallDurationTier.MASTER_CA));
        Assert.assertEquals(218, tracker.getCooldown(CombatSpell.RESURRECT_THRALL).getRemainingTicks());

        // AUTO reads CA_TIER_STATUS_MASTER
        tracker.onChatMessage(fc.client, "You resurrect a lesser zombie thrall.", tier(ThrallDurationTier.AUTO));
        Assert.assertEquals(109, tracker.getCooldown(CombatSpell.RESURRECT_THRALL).getRemainingTicks());
        fc.varbit(VarbitID.CA_TIER_STATUS_MASTER, 2);
        tracker.onChatMessage(fc.client, "You resurrect a lesser zombie thrall.", tier(ThrallDurationTier.AUTO));
        Assert.assertEquals(218, tracker.getCooldown(CombatSpell.RESURRECT_THRALL).getRemainingTicks());
    }

    @Test
    public void thrallEndsWhenActiveVarbitClearsButStaysOnCooldown()
    {
        FakeClient fc = new FakeClient().level(Skill.MAGIC, 99, 99);
        CombatSpellTracker tracker = new CombatSpellTracker();

        fc.varbit(VarbitID.ARCEUUS_RESURRECTION_ACTIVE, 1).varbit(VarbitID.ARCEUUS_RESURRECTION_COOLDOWN, 1);
        tracker.onChatMessage(fc.client, "You resurrect a greater ghost thrall.", DEFAULTS);
        tracker.onGameTick(fc.client);
        Assert.assertFalse(tracker.getCooldown(CombatSpell.RESURRECT_THRALL).isReady());

        // Thrall dies early (varbit 0) but recast cooldown is still running -> not ready yet
        fc.varbit(VarbitID.ARCEUUS_RESURRECTION_ACTIVE, 0);
        tracker.onGameTick(fc.client);
        Cooldown cd = tracker.getCooldown(CombatSpell.RESURRECT_THRALL);
        Assert.assertFalse(cd.isReady());
        Assert.assertTrue(cd.getRemainingTicks() <= 17);

        fc.varbit(VarbitID.ARCEUUS_RESURRECTION_COOLDOWN, 0);
        tracker.onGameTick(fc.client);
        Assert.assertTrue(tracker.getCooldown(CombatSpell.RESURRECT_THRALL).isReady());
    }

    @Test
    public void markOfDarknessUsesRealMagicTimesThreeAndPurgingStaff()
    {
        FakeClient fc = new FakeClient().level(Skill.MAGIC, 90, 99);
        CombatSpellTracker tracker = new CombatSpellTracker();

        tracker.onChatMessage(fc.client, "<col=ef0083>You have placed a Mark of Darkness upon yourself.</col>", DEFAULTS);
        Assert.assertEquals(270, tracker.getCooldown(CombatSpell.MARK_OF_DARKNESS).getRemainingTicks());

        Item staff = new Item(ItemID.PURGING_STAFF, 1);
        ItemContainer worn = (ItemContainer) Proxy.newProxyInstance(
            ItemContainer.class.getClassLoader(),
            new Class<?>[]{ItemContainer.class},
            (p, m, a) -> "getItem".equals(m.getName()) ? staff : FakeClient.defaultValue(m.getReturnType())
        );
        fc.containers.put(InventoryID.WORN, worn);
        tracker.onChatMessage(fc.client, "You have placed a Mark of Darkness upon yourself.", DEFAULTS);
        Assert.assertEquals(1350, tracker.getCooldown(CombatSpell.MARK_OF_DARKNESS).getRemainingTicks());

        // Mark of Darkness has no varbit: ticking must not clear it (original bug cleared it every tick)
        tracker.onGameTick(fc.client);
        Assert.assertEquals(1349, tracker.getCooldown(CombatSpell.MARK_OF_DARKNESS).getRemainingTicks());

        tracker.onChatMessage(fc.client, "Your Mark of Darkness has faded away.", DEFAULTS);
        // Only the 10-tick recast cooldown can remain
        Assert.assertTrue(tracker.getCooldown(CombatSpell.MARK_OF_DARKNESS).getRemainingTicks() <= 10);
    }

    @Test
    public void wardAndShadowVeilStartFromTheirRealGameMessages()
    {
        FakeClient fc = new FakeClient().level(Skill.MAGIC, 95, 99);
        CombatSpellTracker tracker = new CombatSpellTracker();

        tracker.onChatMessage(fc.client, "<col=ef0083>Your defence against Arceuus magic has been strengthened.</col>", DEFAULTS);
        Assert.assertEquals(95, tracker.getCooldown(CombatSpell.WARD_OF_ARCEUUS).getRemainingTicks());

        tracker.onChatMessage(fc.client, "<col=ef0083>Your thieving abilities have been enhanced.</col>", DEFAULTS);
        Assert.assertEquals(95, tracker.getCooldown(CombatSpell.SHADOW_VEIL).getRemainingTicks());

        // Shadow Veil ends when its active varbit is 0
        tracker.onGameTick(fc.client);
        Assert.assertTrue(tracker.getCooldown(CombatSpell.SHADOW_VEIL).isReady());
    }

    @Test
    public void cooldownVarbitsDriveDeathChargeCorruptionAndVengeance()
    {
        FakeClient fc = new FakeClient();
        CombatSpellTracker tracker = new CombatSpellTracker();

        fc.varbit(VarbitID.ARCEUUS_DEATH_CHARGE_COOLDOWN, 1)
            .varbit(VarbitID.ARCEUUS_CORRUPTION_COOLDOWN, 1)
            .varbit(VarbitID.VENGEANCE_TIMELIMIT, 1);
        tracker.syncVars(fc.client);
        Assert.assertEquals(100, tracker.getCooldown(CombatSpell.DEATH_CHARGE).getRemainingTicks());
        Assert.assertEquals(50, tracker.getCooldown(CombatSpell.CORRUPTION).getRemainingTicks());
        Assert.assertEquals(50, tracker.getCooldown(CombatSpell.VENGEANCE).getRemainingTicks());
        Assert.assertTrue(tracker.wasCastSinceLogin(CombatSpell.VENGEANCE));

        // Repeated syncs while the varbit stays 1 must not restart the countdown
        tracker.onGameTick(fc.client);
        tracker.syncVars(fc.client);
        Assert.assertEquals(49, tracker.getCooldown(CombatSpell.VENGEANCE).getRemainingTicks());

        // Countdown elapsed but server still reports cooldown: stay not-ready, do not restart at 50
        for (int i = 0; i < 60; i++)
        {
            tracker.onGameTick(fc.client);
        }
        Assert.assertEquals(1, tracker.getCooldown(CombatSpell.VENGEANCE).getRemainingTicks());

        fc.varbit(VarbitID.VENGEANCE_TIMELIMIT, 0);
        tracker.onGameTick(fc.client);
        Assert.assertTrue(tracker.getCooldown(CombatSpell.VENGEANCE).isReady());
    }

    @Test
    public void chargeFollowsMageArenaVarp()
    {
        FakeClient fc = new FakeClient().varp(VarPlayerID.MAGEARENA_CHARGE, 300);
        CombatSpellTracker tracker = new CombatSpellTracker();
        tracker.syncVars(fc.client);
        Assert.assertEquals(600, tracker.getCooldown(CombatSpell.CHARGE).getRemainingTicks());
        fc.varp(VarPlayerID.MAGEARENA_CHARGE, 0);
        tracker.syncVars(fc.client);
        Assert.assertTrue(tracker.getCooldown(CombatSpell.CHARGE).isReady());
    }

    @Test
    public void debugTimersNeverWipeRealCooldowns()
    {
        FakeClient fc = new FakeClient().varbit(VarbitID.VENGEANCE_TIMELIMIT, 1);
        CombatSpellTracker tracker = new CombatSpellTracker();
        tracker.syncVars(fc.client);

        tracker.triggerDebugCooldowns();
        Assert.assertFalse(tracker.getCooldown(CombatSpell.CHARGE).isReady());
        tracker.clearDebugCooldowns();
        Assert.assertTrue(tracker.getCooldown(CombatSpell.CHARGE).isReady());
        // The real Vengeance recast cooldown survives turning the debug toggle off
        Assert.assertEquals(50, tracker.getCooldown(CombatSpell.VENGEANCE).getRemainingTicks());

        fc.varbit(VarbitID.VENGEANCE_TIMELIMIT, 0);
        tracker.syncVars(fc.client);
        Assert.assertTrue(tracker.getCooldown(CombatSpell.VENGEANCE).isReady());
    }

    @Test
    public void onlyRemindAfterCastResetsOnLogin()
    {
        CombatSpellTracker tracker = new CombatSpellTracker();
        CombatStateTracker combat = new CombatStateTracker();
        combat.setSimulatedCombat(true);

        Assert.assertFalse("Never cast -> no reminder", tracker.isSpellReadyReminder(CombatSpell.DEATH_CHARGE, combat, DEFAULTS));

        tracker.startCooldownTimer(CombatSpell.DEATH_CHARGE, 3);
        Assert.assertFalse(tracker.isSpellReadyReminder(CombatSpell.DEATH_CHARGE, combat, DEFAULTS));
        tracker.clearCooldown(CombatSpell.DEATH_CHARGE);
        Assert.assertTrue(tracker.isSpellReadyReminder(CombatSpell.DEATH_CHARGE, combat, DEFAULTS));

        tracker.reset();
        Assert.assertFalse("Cast history must clear on login/hop", tracker.isSpellReadyReminder(CombatSpell.DEATH_CHARGE, combat, DEFAULTS));
    }

    @Test
    public void thrallCombatStartReminderAndCombatOnlyGate()
    {
        CombatSpellTracker tracker = new CombatSpellTracker();
        CombatStateTracker combat = new CombatStateTracker();
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean thrallRemindOnCombatStart() { return true; }
        };
        Assert.assertFalse("Out of combat", tracker.isSpellReadyReminder(CombatSpell.RESURRECT_THRALL, combat, cfg));
        combat.setSimulatedCombat(true);
        Assert.assertTrue(tracker.isSpellReadyReminder(CombatSpell.RESURRECT_THRALL, combat, cfg));
        Assert.assertFalse(tracker.isSpellReadyReminder(CombatSpell.DEATH_CHARGE, combat, cfg));
    }

    @Test
    public void deathEndsBuffsButNotCooldowns()
    {
        CombatSpellTracker tracker = new CombatSpellTracker();
        tracker.startActive(CombatSpell.MARK_OF_DARKNESS, 200);
        tracker.startCooldownTimer(CombatSpell.CORRUPTION, 40);
        tracker.onPlayerDeath();
        Assert.assertTrue(tracker.getCooldown(CombatSpell.MARK_OF_DARKNESS).isReady());
        Assert.assertEquals(40, tracker.getCooldown(CombatSpell.CORRUPTION).getRemainingTicks());
    }

    @Test
    public void collectReadySpellsHonoursDebugSpellbookAndEnableFlags()
    {
        CombatSpellTracker tracker = new CombatSpellTracker();
        CombatStateTracker combat = new CombatStateTracker();
        int[] ords = new int[CombatSpell.values().length];
        int[] cols = new int[CombatSpell.values().length];

        ImmersiveCombatOverlaysConfig debugCfg = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean debugSpellVengeance() { return true; }
        };
        Assert.assertEquals(1, tracker.collectReadySpells(combat, debugCfg, 0, false, ords, cols));
        Assert.assertEquals(CombatSpell.VENGEANCE.ordinal(), ords[0]);

        combat.setSimulatedCombat(true);
        tracker.startCooldownTimer(CombatSpell.DEATH_CHARGE, 1);
        tracker.clearCooldown(CombatSpell.DEATH_CHARGE);
        // On the Lunar book (2) the Arceuus spell still shows because it was cast this login
        Assert.assertEquals(1, tracker.collectReadySpells(combat, DEFAULTS, 2, true, ords, cols));

        ImmersiveCombatOverlaysConfig disabled = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean spellDeathChargeEnabled() { return false; }
        };
        Assert.assertEquals(0, tracker.collectReadySpells(combat, disabled, 3, true, ords, cols));
    }

    @Test
    public void showerDurationCapAndSessionReset()
    {
        CombatSpellTracker tracker = new CombatSpellTracker();
        long now = 50_000_000_000L;

        Assert.assertTrue(tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 1, 12, now));
        Assert.assertTrue(tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 1, 12, now + 10_000_000_000L));
        Assert.assertFalse(tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 1, 12, now + 13_000_000_000L));
        Assert.assertTrue("New combat session re-arms", tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 2, 12, now + 135_000_000_000L));
        Assert.assertTrue("0 = unlimited", tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 2, 0, now + 999_000_000_000L));
    }

    @Test
    public void widgetNameMatching()
    {
        Assert.assertTrue(CombatSpell.RESURRECT_THRALL.matchesWidgetName("Resurrect Greater Ghost"));
        Assert.assertTrue(CombatSpell.RESURRECT_THRALL.matchesWidgetName("resurrect lesser skeleton"));
        Assert.assertTrue(CombatSpell.CORRUPTION.matchesWidgetName("Greater Corruption"));
        Assert.assertTrue(CombatSpell.VENGEANCE_OTHER.matchesWidgetName("Vengeance Other"));
        Assert.assertFalse(CombatSpell.VENGEANCE.matchesWidgetName("Vengeance Other"));
        Assert.assertFalse(CombatSpell.VENGEANCE.matchesWidgetName(""));
        Assert.assertFalse(CombatSpell.VENGEANCE.matchesWidgetName(null));
    }
}
