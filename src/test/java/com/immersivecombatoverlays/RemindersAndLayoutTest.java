package com.immersivecombatoverlays;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.BoostPotion;
import com.immersivecombatoverlays.model.Cooldown;
import com.immersivecombatoverlays.model.PotionEffect;
import com.immersivecombatoverlays.tracker.BoostPotionTimers;
import com.immersivecombatoverlays.tracker.ExpiryReminders;
import com.immersivecombatoverlays.tracker.PotionTimers;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import org.junit.Assert;
import org.junit.Test;

public class RemindersAndLayoutTest
{
    private static final ImmersiveCombatOverlaysConfig DEFAULTS = new ImmersiveCombatOverlaysConfig() {};
    private static final long SEC = 1_000_000_000L;

    // ---------------------------------------------------------------- expiry latch

    @Test
    public void reminderFiresOnlyOnExpiryNotForUnusedItems()
    {
        ExpiryReminders r = new ExpiryReminders();
        int k = ExpiryReminders.key(PotionEffect.ANTIVENOM);
        long t = 100 * SEC;

        // Never active (potion just sitting in the inventory): no reminder, ever
        r.observe(k, false, t);
        r.observe(k, false, t + SEC);
        Assert.assertFalse(r.isShowing(k, 10, t + SEC));
        Assert.assertFalse("even with unlimited duration", r.isShowing(k, 0, t + SEC));

        // Drink -> active -> expires: reminder for the configured duration only
        r.observe(k, true, t + 2 * SEC);
        Assert.assertFalse(r.isShowing(k, 10, t + 2 * SEC));
        r.observe(k, false, t + 3 * SEC);
        Assert.assertTrue(r.isShowing(k, 10, t + 5 * SEC));
        Assert.assertFalse("times out", r.isShowing(k, 10, t + 14 * SEC));
        Assert.assertTrue("0 = until re-applied", r.isShowing(k, 0, t + 500 * SEC));

        // Re-applying clears it immediately
        r.observe(k, true, t + 6 * SEC);
        Assert.assertFalse(r.isShowing(k, 0, t + 6 * SEC));

        r.reset();
        Assert.assertFalse(r.isShowing(k, 0, t + 7 * SEC));
        Assert.assertNotEquals(ExpiryReminders.key(PotionEffect.SURGE), ExpiryReminders.key(BoostPotion.ATTACK));
        Assert.assertNotEquals(ExpiryReminders.HEART, ExpiryReminders.key(PotionEffect.SURGE));
    }

    @Test
    public void preRemindLeadStartsReminderEarly()
    {
        Cooldown cd = Cooldown.active(10, 500); // 6s left
        Assert.assertTrue("no lead: still active", ImmersiveCombatOverlaysPlugin.isActiveBeyondLead(cd, 0));
        Assert.assertFalse("10s lead: already inside the pre-remind window", ImmersiveCombatOverlaysPlugin.isActiveBeyondLead(cd, 10));
        Assert.assertTrue(ImmersiveCombatOverlaysPlugin.isActiveBeyondLead(Cooldown.active(100, 500), 10));
        Assert.assertFalse(ImmersiveCombatOverlaysPlugin.isActiveBeyondLead(Cooldown.READY, 0));
        Assert.assertFalse(ImmersiveCombatOverlaysPlugin.isActiveBeyondLead(null, 0));
    }

    @Test
    public void preRemindOnlyAppliesToBoostingPotions()
    {
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public int divinePreRemindSeconds() { return 8; }
        };
        Assert.assertEquals(8, ImmersiveCombatOverlaysConfig.getPotionPreRemindSeconds(cfg, PotionEffect.DIVINE_COMBAT));
        Assert.assertEquals(8, ImmersiveCombatOverlaysConfig.getPotionPreRemindSeconds(cfg, PotionEffect.DIVINE_BASTION));
        Assert.assertEquals(8, ImmersiveCombatOverlaysConfig.getPotionPreRemindSeconds(cfg, PotionEffect.OVERLOAD));
        Assert.assertEquals(8, ImmersiveCombatOverlaysConfig.getPotionPreRemindSeconds(cfg, PotionEffect.SMELLING_SALTS));
        Assert.assertEquals("protection potions remind on expiry only", 0, ImmersiveCombatOverlaysConfig.getPotionPreRemindSeconds(cfg, PotionEffect.ANTIVENOM));
        Assert.assertEquals(0, ImmersiveCombatOverlaysConfig.getPotionPreRemindSeconds(cfg, PotionEffect.STAMINA));
        Assert.assertEquals(0, ImmersiveCombatOverlaysConfig.getPotionPreRemindSeconds(cfg, PotionEffect.SUPER_ANTIFIRE));
        Assert.assertEquals("off by default", 0, ImmersiveCombatOverlaysConfig.getPotionPreRemindSeconds(DEFAULTS, PotionEffect.DIVINE_COMBAT));
        Assert.assertEquals(0, DEFAULTS.heartPreRemindSeconds());
        Assert.assertEquals(0, DEFAULTS.boostPreRemindSeconds());
    }

    @Test
    public void perFamilyReminderToggles()
    {
        Assert.assertFalse("stamina expiry reminder off by default", ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(DEFAULTS, PotionEffect.STAMINA));
        Assert.assertTrue(ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(DEFAULTS, PotionEffect.ANTIVENOM));
        Assert.assertTrue(ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(DEFAULTS, PotionEffect.DIVINE_MAGIC));
        Assert.assertTrue(ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(DEFAULTS, PotionEffect.OVERLOAD));
        Assert.assertTrue(ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(DEFAULTS, PotionEffect.SURGE));
        Assert.assertTrue(ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(DEFAULTS, PotionEffect.GOADING));
        Assert.assertTrue(ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(DEFAULTS, PotionEffect.PRAYER_REGENERATION));
        ImmersiveCombatOverlaysConfig separate = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean remindSurge() { return false; }

            @Override
            public boolean remindGoading() { return false; }

            @Override
            public boolean remindPrayerRegeneration() { return false; }
        };
        Assert.assertFalse(ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(separate, PotionEffect.SURGE));
        Assert.assertFalse(ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(separate, PotionEffect.GOADING));
        Assert.assertFalse(ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(separate, PotionEffect.PRAYER_REGENERATION));
        Assert.assertTrue("other potions toggle unaffected", ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(separate, PotionEffect.MENAPHITE_REMEDY));
        ImmersiveCombatOverlaysConfig noAnti = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean remindAntipoison() { return false; }

            @Override
            public boolean raidPotionsEnabled() { return false; }
        };
        Assert.assertFalse(ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(noAnti, PotionEffect.ANTIPOISON));
        Assert.assertFalse(ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(noAnti, PotionEffect.ANTIVENOM));
        Assert.assertFalse(ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(noAnti, PotionEffect.SMELLING_SALTS));
        Assert.assertTrue(ImmersiveCombatOverlaysConfig.isPotionReminderEnabled(noAnti, PotionEffect.ANTIFIRE));
    }

    // ---------------------------------------------------------------- stamina stale varbit

    @Test
    public void staleStaminaDurationIsNotTreatedAsActive()
    {
        FakeClient fc = new FakeClient().varbit(VarbitID.STAMINA_DURATION, 12).varbit(VarbitID.STAMINA_ACTIVE, 0);
        PotionTimers potions = new PotionTimers();
        potions.onGameTick(fc.client);
        Assert.assertTrue("duration without STAMINA_ACTIVE is stale", potions.getCooldown(PotionEffect.STAMINA).isReady());
        Assert.assertFalse(potions.wasUsedThisSession(PotionEffect.STAMINA));

        fc.varbit(VarbitID.STAMINA_ACTIVE, 1);
        potions.onGameTick(fc.client);
        Assert.assertEquals(120, potions.getCooldown(PotionEffect.STAMINA).getRemainingTicks());
    }

    // ---------------------------------------------------------------- per-stat re-dose

    @Test
    public void perStatRedoseLevels()
    {
        FakeClient fc = new FakeClient().level(Skill.RANGED, 99, 112).level(Skill.DEFENCE, 99, 117);
        BoostPotionTimers boosts = new BoostPotionTimers();
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public int boostRedoseRanged() { return 7; }

            @Override
            public int boostRedoseDefence() { return 5; }
        };
        Assert.assertEquals(7, ImmersiveCombatOverlaysConfig.getRedoseLevel(cfg, Skill.RANGED));
        Assert.assertEquals(0, ImmersiveCombatOverlaysConfig.getRedoseLevel(cfg, Skill.HITPOINTS));

        // Bastion: ranged +13 > 7 and defence +18 > 5 -> not ready yet
        Assert.assertFalse(boosts.getCooldown(fc.client, BoostPotion.BASTION, cfg).isReady());
        fc.level(Skill.RANGED, 99, 106).level(Skill.DEFENCE, 99, 104);
        Assert.assertTrue("both at/below their own levels", boosts.getCooldown(fc.client, BoostPotion.BASTION, cfg).isReady());
        fc.level(Skill.DEFENCE, 99, 110);
        Assert.assertFalse("defence still above +5", boosts.getCooldown(fc.client, BoostPotion.BASTION, cfg).isReady());

        ImmersiveCombatOverlaysConfig untracked = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public int boostRedoseRanged() { return 0; }
        };
        Assert.assertNull("0 = don't track", boosts.getCooldown(fc.client, BoostPotion.RANGED, untracked));
        Assert.assertNotNull(boosts.getCooldown(fc.client, BoostPotion.BASTION, untracked));
    }

    // ---------------------------------------------------------------- settings layout

    @Test
    public void everySectionFollowsTheStandardLayout() throws Exception
    {
        Map<String, Integer> sectionPos = new HashMap<>();
        for (Field f : ImmersiveCombatOverlaysConfig.class.getDeclaredFields())
        {
            ConfigSection cs = f.getAnnotation(ConfigSection.class);
            if (cs != null)
            {
                sectionPos.put((String) f.get(null), cs.position());
            }
        }

        Map<String, Map<Integer, String>> bySection = new HashMap<>();
        for (Method m : ImmersiveCombatOverlaysConfig.class.getDeclaredMethods())
        {
            if (Modifier.isStatic(m.getModifiers()))
            {
                continue;
            }
            ConfigItem item = m.getAnnotation(ConfigItem.class);
            bySection.computeIfAbsent(item.section(), x -> new HashMap<>()).put(item.position(), item.name());
            Assert.assertFalse("every setting belongs to a section: " + item.keyName(), item.section().isEmpty());
        }

        for (Map.Entry<String, Map<Integer, String>> e : bySection.entrySet())
        {
            int pos = sectionPos.get(e.getKey());
            if (pos < 10)
            {
                continue; // shared sections have their own layout
            }
            Map<Integer, String> slots = e.getValue();
            Assert.assertEquals(e.getKey() + " slot 0", "Enable", slots.get(0));
            check(e.getKey(), slots, 10, "Color");
            check(e.getKey(), slots, 11, "Screen glow");
            check(e.getKey(), slots, 12, "Shower icon");
            check(e.getKey(), slots, 14, "Notification");
            check(e.getKey(), slots, 15, "Reminder duration");
            check(e.getKey(), slots, 19, "Debug preview");
            for (Map.Entry<Integer, String> s : slots.entrySet())
            {
                if (s.getValue().equals("Debug preview"))
                {
                    Assert.assertEquals(e.getKey() + ": debug preview must be last", 19, (int) s.getKey());
                }
                if (s.getValue().equals("Enable"))
                {
                    Assert.assertEquals(e.getKey() + ": Enable must be first", 0, (int) s.getKey());
                }
            }
        }

        // Shared sections are at the top
        int maxShared = -1;
        int minFeature = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> e : sectionPos.entrySet())
        {
            if (e.getKey().startsWith("shared") || e.getKey().equals("generalDisplaySection")
                || e.getKey().equals("debugSection") || e.getKey().equals("inventoryStyleSection"))
            {
                maxShared = Math.max(maxShared, e.getValue());
            }
            else
            {
                minFeature = Math.min(minFeature, e.getValue());
            }
        }
        Assert.assertTrue("shared sections first", maxShared < minFeature);
    }

    private static void check(String section, Map<Integer, String> slots, int pos, String expected)
    {
        String actual = slots.get(pos);
        if (actual != null)
        {
            Assert.assertEquals(section + " slot " + pos, expected, actual);
        }
    }
}
