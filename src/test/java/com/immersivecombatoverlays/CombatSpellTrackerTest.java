package com.immersivecombatoverlays;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.CombatSpell;
import com.immersivecombatoverlays.model.Cooldown;
import com.immersivecombatoverlays.model.ThrallDurationTier;
import com.immersivecombatoverlays.tracker.CombatSpellTracker;
import com.immersivecombatoverlays.tracker.CombatStateTracker;
import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import org.junit.Assert;
import org.junit.Test;

public class CombatSpellTrackerTest
{
    private static Client mockClient(int magicLevel)
    {
        return (Client) Proxy.newProxyInstance(
            Client.class.getClassLoader(),
            new Class<?>[]{Client.class},
            (p, m, a) -> {
                if ("getBoostedSkillLevel".equals(m.getName()) && a[0] == Skill.MAGIC) return magicLevel;
                if ("getVarbitValue".equals(m.getName())) return 0;
                return null;
            }
        );
    }

    @Test
    public void testSpellCooldownLifecycleAndDebugOverrides()
    {
        CombatSpellTracker tracker = new CombatSpellTracker();
        CombatStateTracker combatState = new CombatStateTracker();

        for (CombatSpell spell : CombatSpell.values())
        {
            Assert.assertTrue(tracker.getCooldown(spell).isReady());
        }

        tracker.startCooldown(CombatSpell.DEATH_CHARGE, 100, 1);
        Cooldown dc = tracker.getCooldown(CombatSpell.DEATH_CHARGE);
        Assert.assertFalse(dc.isReady());
        Assert.assertEquals(100, dc.getRemainingTicks());
        Assert.assertEquals(1.0f, dc.getRemainingFraction(), 0.001f);

        tracker.clearCooldown(CombatSpell.DEATH_CHARGE);
        Assert.assertTrue(tracker.getCooldown(CombatSpell.DEATH_CHARGE).isReady());

        ImmersiveCombatOverlaysConfig debugCfg = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean spellOnlyInCombat() { return true; }

            @Override
            public boolean debugSpellVengeance() { return true; }
        };

        int[] ords = new int[CombatSpell.values().length];
        int[] cols = new int[CombatSpell.values().length];
        int count = tracker.collectReadySpells(combatState, debugCfg, 0, false, ords, cols);
        Assert.assertEquals(1, count);
        Assert.assertEquals(CombatSpell.VENGEANCE.ordinal(), ords[0]);
    }

    @Test
    public void testShowerDurationCapAndSessionReset()
    {
        CombatSpellTracker tracker = new CombatSpellTracker();
        long now = 50_000_000_000L;

        Assert.assertTrue(tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 1, 12, now));
        Assert.assertTrue(tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 1, 12, now + 10_000_000_000L));
        Assert.assertFalse(tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 1, 12, now + 13_000_000_000L));

        Assert.assertTrue(tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 2, 12, now + 135_000_000_000L));
    }

    @Test
    public void testThrallCombatAchievementDurationTiersAndChatTriggers()
    {
        Client c = mockClient(99);
        CombatSpellTracker tracker = new CombatSpellTracker();

        ImmersiveCombatOverlaysConfig masterCfg = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public ThrallDurationTier thrallDurationTier() { return ThrallDurationTier.MASTER_CA; }
        };
        ImmersiveCombatOverlaysConfig gmCfg = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public ThrallDurationTier thrallDurationTier() { return ThrallDurationTier.MASTER_CA; }
        };

        tracker.onChatMessage(c, "You resurrect a greater ghost thrall.", 1, masterCfg);
        Assert.assertEquals(149, tracker.getCooldown(CombatSpell.RESURRECT_THRALL).getRemainingTicks());

        tracker.onChatMessage(c, "You resurrect a greater skeleton thrall.", 1, gmCfg);
        Assert.assertEquals(198, tracker.getCooldown(CombatSpell.RESURRECT_THRALL).getRemainingTicks());

        tracker.onChatMessage(c, "Your thrall has returned to the grave.", 1, gmCfg);
        Assert.assertTrue(tracker.getCooldown(CombatSpell.RESURRECT_THRALL).isReady());

        tracker.onChatMessage(c, "You have cast Death Charge.", 1, gmCfg);
        Assert.assertEquals(100, tracker.getCooldown(CombatSpell.DEATH_CHARGE).getRemainingTicks());

        tracker.onChatMessage(c, "You cast Mark of Darkness.", 1, gmCfg);
        Assert.assertFalse(tracker.getCooldown(CombatSpell.MARK_OF_DARKNESS).isReady());
    }

    @Test
    public void testWidgetNameMatching()
    {
        Assert.assertTrue(CombatSpell.RESURRECT_THRALL.matchesWidgetName("Resurrect Greater Ghost"));
        Assert.assertTrue(CombatSpell.RESURRECT_THRALL.matchesWidgetName("Resurrect Lesser Skeleton"));
        Assert.assertTrue(CombatSpell.DEATH_CHARGE.matchesWidgetName("Death Charge"));
        Assert.assertTrue(CombatSpell.MARK_OF_DARKNESS.matchesWidgetName("Mark of Darkness"));
        Assert.assertTrue(CombatSpell.VENGEANCE.matchesWidgetName("Vengeance"));
        Assert.assertFalse(CombatSpell.VENGEANCE.matchesWidgetName("High Level Alchemy"));
    }

    @Test
    public void testTasteVengeanceProcDoesNotResetRecastCooldownAndDeathClearsBuffs()
    {
        CombatSpellTracker tracker = new CombatSpellTracker();
        tracker.startCooldown(CombatSpell.VENGEANCE, 12, 1);
        tracker.onChatMessage(null, "Taste Vengeance!", 1, null);
        Assert.assertEquals(12, tracker.getCooldown(CombatSpell.VENGEANCE).getRemainingTicks());

        tracker.startCooldown(CombatSpell.RESURRECT_THRALL, 80, 1);
        tracker.onPlayerDeath();
        Assert.assertTrue(tracker.getCooldown(CombatSpell.RESURRECT_THRALL).isReady());
    }

    @Test
    public void testSpellsRemindStrictlyOnExpiry()
    {
        CombatSpellTracker tracker = new CombatSpellTracker();
        CombatStateTracker combat = new CombatStateTracker();
        combat.setSimulatedCombat(true);
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig() {};

        tracker.startCooldown(CombatSpell.RESURRECT_THRALL, 4, 1);
        Assert.assertFalse(tracker.isSpellReadyReminder(CombatSpell.RESURRECT_THRALL, combat, cfg));

        tracker.clearCooldown(CombatSpell.RESURRECT_THRALL);
        Assert.assertTrue(tracker.isSpellReadyReminder(CombatSpell.RESURRECT_THRALL, combat, cfg));
    }
}
