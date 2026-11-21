package com.immersivecombatoverlays;

import com.immersivecombatoverlays.tracker.CombatStateTracker;
import java.lang.reflect.Proxy;
import net.runelite.api.Actor;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import org.junit.Assert;
import org.junit.Test;

public class CombatStateTrackerTest
{
    private static final long NOW = 1_000_000_000L;

    /** Mutable actor: what it is interacting with, and whether it is dead. */
    private static final class Fake
    {
        Actor interacting;
        boolean dead;
    }

    private static <T> T actor(Class<T> type, Fake state)
    {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
            (p, m, a) -> {
                switch (m.getName())
                {
                    case "getInteracting": return state.interacting;
                    case "isDead": return state.dead;
                    case "hashCode": return System.identityHashCode(p);
                    case "equals": return p == a[0];
                    default: return FakeClient.defaultValue(m.getReturnType());
                }
            }));
    }

    private final Fake me = new Fake();
    private final Player local = actor(Player.class, me);
    private final FakeClient fc = new FakeClient();
    private final CombatStateTracker state = new CombatStateTracker();

    {
        fc.localPlayer = local;
    }

    private void ticks(int n)
    {
        for (int i = 0; i < n; i++)
        {
            state.onGameTick(fc.client, NOW, 120);
        }
    }

    private void click(MenuAction action, String option, String target, Actor actor)
    {
        state.onMenuOptionClicked(action, option, target, actor, NOW, 120);
    }

    private void hit(Actor on, int type)
    {
        state.onHitsplat(on, new Hitsplat(type, 5, 0), local, NOW, 120);
    }

    @Test
    public void sessionIdAndSimulatedCombat()
    {
        Assert.assertFalse(state.isInCombat());
        state.onCombatActivity(NOW, 120);
        Assert.assertTrue(state.isInCombat());
        Assert.assertEquals(1, state.getCombatSessionId());
        state.reset();
        state.setSimulatedCombat(true);
        Assert.assertTrue(state.isInCombat());
        state.setSimulatedCombat(false);
        Assert.assertFalse(state.isInCombat());
    }

    @Test
    public void interactingAloneIsNeverCombat()
    {
        Fake guardState = new Fake();
        NPC guard = actor(NPC.class, guardState);
        me.interacting = guard; // talking to / following a combat-level NPC
        ticks(3);
        Assert.assertFalse(state.isInCombat());
    }

    @Test
    public void attackClickEngagesAndInteractingSustainsUntilTimeout()
    {
        NPC goblin = actor(NPC.class, new Fake());
        click(MenuAction.NPC_SECOND_OPTION, "Attack", "Goblin", goblin);
        Assert.assertTrue(state.isInCombat());
        me.interacting = goblin;
        ticks(40);
        Assert.assertTrue("still fighting the engaged target", state.isInCombat());

        me.interacting = null;
        ticks(16);
        Assert.assertTrue(state.isInCombat());
        ticks(1);
        Assert.assertFalse("17 ticks without activity ends combat", state.isInCombat());
    }

    @Test
    public void failedPickpocketStunIsNotCombatButThievingIsTracked()
    {
        Fake manState = new Fake();
        NPC man = actor(NPC.class, manState);
        click(MenuAction.NPC_THIRD_OPTION, "Pickpocket", "Man", man);
        me.interacting = man;
        manState.interacting = local;
        hit(local, HitsplatID.DAMAGE_ME);
        Assert.assertFalse("stun damage is not combat", state.isInCombat());
        Assert.assertTrue(state.isThieving());

        ticks(6);
        hit(local, HitsplatID.DAMAGE_ME);
        Assert.assertTrue("the NPC keeps hitting after the grace window: real combat", state.isInCombat());
        ticks(200);
        Assert.assertFalse(state.isThieving());
    }

    @Test
    public void poisonVenomAndHealSplatsDoNotKeepCombatOn()
    {
        hit(local, HitsplatID.POISON);
        hit(local, HitsplatID.VENOM);
        hit(local, HitsplatID.HEAL);
        Assert.assertFalse(state.isInCombat());
        hit(local, HitsplatID.BLOCK_ME);
        Assert.assertTrue("a blocked (zero) hit on you is combat", state.isInCombat());
    }

    @Test
    public void onlyMyHitsOnOthersCount()
    {
        NPC boss = actor(NPC.class, new Fake());
        hit(boss, HitsplatID.DAMAGE_OTHER);
        Assert.assertFalse("someone else hitting a monster is not my combat", state.isInCombat());
        hit(boss, HitsplatID.DAMAGE_ME);
        Assert.assertTrue(state.isInCombat());
        Assert.assertTrue(state.dealtHitWithin(0));
    }

    @Test
    public void followingTradingAndUtilitySpellsOnPlayersDoNotArm()
    {
        Player friend = actor(Player.class, new Fake());
        click(MenuAction.PLAYER_THIRD_OPTION, "Follow", "Friend", friend);
        click(MenuAction.PLAYER_FOURTH_OPTION, "Trade with", "Friend", friend);
        click(MenuAction.WIDGET_TARGET_ON_PLAYER, "Cast", "<col=00ff00>Vengeance Other</col> -> Friend", friend);
        me.interacting = friend;
        ticks(2);
        Assert.assertFalse(state.isInCombat());
        Assert.assertTrue(state.castUtilityMagicWithin(2));

        click(MenuAction.WIDGET_TARGET_ON_PLAYER, "Cast", "<col=00ff00>Ice Barrage</col> -> Pker", friend);
        Assert.assertTrue("an attack spell on a player arms combat", state.isInCombat());
    }

    @Test
    public void selfCastAndItemSpellsRecordUtilityMagic()
    {
        click(MenuAction.CC_OP, "Cast", "Varrock Teleport", null);
        Assert.assertTrue(state.castUtilityMagicWithin(0));
        ticks(5);
        Assert.assertFalse(state.castUtilityMagicWithin(3));
        click(MenuAction.WIDGET_TARGET_ON_WIDGET, "Cast", "High Level Alchemy -> Rune platebody", null);
        Assert.assertTrue(state.castUtilityMagicWithin(0));
        Assert.assertFalse(state.isInCombat());
    }
}
