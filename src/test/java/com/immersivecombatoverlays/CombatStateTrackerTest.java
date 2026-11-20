package com.immersivecombatoverlays;

import com.immersivecombatoverlays.tracker.CombatStateTracker;
import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import org.junit.Assert;
import org.junit.Test;

public class CombatStateTrackerTest
{
    @Test
    public void testCombatEntryTimeoutAndBreakReRemind()
    {
        CombatStateTracker state = new CombatStateTracker();
        Assert.assertFalse(state.isInCombat());
        Assert.assertEquals(0, state.getCombatSessionId());

        long t0 = 1_000_000_000L;
        state.onCombatActivity(t0, 120);
        Assert.assertTrue(state.isInCombat());
        Assert.assertEquals(1, state.getCombatSessionId());

        // Simulated combat flag
        state.setSimulatedCombat(true);
        Assert.assertTrue(state.isInCombat());
        state.setSimulatedCombat(false);
        state.reset();
        Assert.assertFalse(state.isInCombat());
        Assert.assertEquals(0, state.getCombatSessionId());
    }

    @Test
    public void testTalkingToBankerNpcDoesNotTriggerCombat()
    {
        NPC banker = (NPC) Proxy.newProxyInstance(
            NPC.class.getClassLoader(),
            new Class<?>[]{NPC.class},
            (p, m, a) -> "getCombatLevel".equals(m.getName()) ? 0 : null
        );
        NPC boss = (NPC) Proxy.newProxyInstance(
            NPC.class.getClassLoader(),
            new Class<?>[]{NPC.class},
            (p, m, a) -> "getCombatLevel".equals(m.getName()) ? 250 : null
        );

        final NPC[] target = new NPC[]{banker};
        Player local = (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[]{Player.class},
            (p, m, a) -> "getInteracting".equals(m.getName()) ? target[0] : null
        );
        Client client = (Client) Proxy.newProxyInstance(
            Client.class.getClassLoader(),
            new Class<?>[]{Client.class},
            (p, m, a) -> "getLocalPlayer".equals(m.getName()) ? local : FakeClient.defaultValue(m.getReturnType())
        );

        CombatStateTracker state = new CombatStateTracker();
        state.onGameTick(client, 1_000_000_000L, 120);
        Assert.assertFalse("Banker interaction should not count as combat", state.isInCombat());

        target[0] = boss;
        state.onGameTick(client, 2_000_000_000L, 120);
        Assert.assertTrue("Combat NPC interaction should trigger combat", state.isInCombat());
    }

    @Test
    public void followingAPlayerIsOnlyCombatInPvpAreas()
    {
        Player friend = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
            (p, m, a) -> FakeClient.defaultValue(m.getReturnType()));
        Player local = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
            (p, m, a) -> "getInteracting".equals(m.getName()) ? friend : FakeClient.defaultValue(m.getReturnType()));
        FakeClient fc = new FakeClient();
        fc.localPlayer = local;

        CombatStateTracker state = new CombatStateTracker();
        state.onGameTick(fc.client, 1_000_000_000L, 120);
        Assert.assertFalse("following / trading at the GE is not combat", state.isInCombat());

        fc.varbit(net.runelite.api.gameval.VarbitID.INSIDE_WILDERNESS, 1);
        state.onGameTick(fc.client, 2_000_000_000L, 120);
        Assert.assertTrue("player target in the Wilderness is combat", state.isInCombat());
    }
}
