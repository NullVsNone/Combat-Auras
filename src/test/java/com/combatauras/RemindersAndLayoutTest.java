package com.combatauras;

import com.combatauras.config.CombatAurasConfig;
import com.combatauras.model.BoostPotion;
import com.combatauras.model.Cooldown;
import com.combatauras.model.PotionEffect;
import com.combatauras.tracker.BoostPotionTimers;
import com.combatauras.tracker.ReminderPipeline;
import com.combatauras.tracker.ReminderPolicy;
import com.combatauras.tracker.PotionTimers;
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
    private static final CombatAurasConfig DEFAULTS = new CombatAurasConfig() {};
    private static final long SEC = 1_000_000_000L;

    // ---------------------------------------------------------------- expiry latch

    @Test
    public void reminderFiresOnlyOnExpiryNotForUnusedItems()
    {
        ReminderPipeline r = new ReminderPipeline(null, null, null, null, null, null, null);
        int k = ReminderPipeline.key(PotionEffect.ANTIVENOM);
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
        Assert.assertNotEquals(ReminderPipeline.key(PotionEffect.SURGE), ReminderPipeline.key(BoostPotion.ATTACK));
        Assert.assertNotEquals(ReminderPipeline.HEART, ReminderPipeline.key(PotionEffect.SURGE));
    }

    @Test
    public void preRemindLeadStartsReminderEarly()
    {
        Cooldown cd = Cooldown.active(10, 500); // 6s left
        Assert.assertTrue("no lead: still active", ReminderPolicy.activeBeyondLead(cd, com.combatauras.settings.SettingsSnapshot.secondsToTicks(0)));
        Assert.assertFalse("10s lead: already inside the pre-remind window", ReminderPolicy.activeBeyondLead(cd, com.combatauras.settings.SettingsSnapshot.secondsToTicks(10)));
        Assert.assertTrue(ReminderPolicy.activeBeyondLead(Cooldown.active(100, 500), com.combatauras.settings.SettingsSnapshot.secondsToTicks(10)));
        Assert.assertFalse(ReminderPolicy.activeBeyondLead(Cooldown.READY, com.combatauras.settings.SettingsSnapshot.secondsToTicks(0)));
        Assert.assertFalse(ReminderPolicy.activeBeyondLead(null, com.combatauras.settings.SettingsSnapshot.secondsToTicks(0)));
    }

    @Test
    public void everyPotionFamilyHasItsOwnExpiringSoonLead()
    {
        CombatAurasConfig cfg = new CombatAurasConfig()
        {
            @Override
            public int divinePreRemindSeconds() { return 6; }

            @Override
            public int raidBoostLeadSeconds() { return 12; }

            @Override
            public int antifireLeadSeconds() { return 30; }

            @Override
            public int staminaLeadSeconds() { return 3; }
        };
        com.combatauras.settings.SettingsSnapshot s = com.combatauras.settings.SettingsSnapshot.of(cfg);
        Assert.assertEquals(10, s.leadTicks(PotionEffect.DIVINE_COMBAT));
        Assert.assertEquals(10, s.leadTicks(PotionEffect.DIVINE_BASTION));
        Assert.assertEquals(20, s.leadTicks(PotionEffect.OVERLOAD));
        Assert.assertEquals(20, s.leadTicks(PotionEffect.SMELLING_SALTS));
        Assert.assertEquals(50, s.leadTicks(PotionEffect.SUPER_ANTIFIRE));
        Assert.assertEquals(5, s.leadTicks(PotionEffect.STAMINA));
        Assert.assertEquals("untouched families warn at expiry", 0, s.leadTicks(PotionEffect.ANTIVENOM));
        com.combatauras.settings.SettingsSnapshot d = com.combatauras.settings.SettingsSnapshot.of(DEFAULTS);
        for (PotionEffect e : PotionEffect.values())
        {
            Assert.assertEquals("every lead is off by default: " + e, 0, d.leadTicks(e));
        }
        for (com.combatauras.model.CombatSpell spell : com.combatauras.model.CombatSpell.values())
        {
            Assert.assertEquals(0, d.spellLeadTicks(spell.ordinal()));
        }
        Assert.assertEquals(0, DEFAULTS.heartPreRemindSeconds());
        Assert.assertEquals(0, DEFAULTS.boostPreRemindSeconds());
    }

    @Test
    public void perFamilyReminderToggles()
    {
        Assert.assertFalse("stamina expiry reminder off by default", com.combatauras.settings.SettingsSnapshot.of(DEFAULTS).potionRemind(PotionEffect.STAMINA.ordinal()));
        Assert.assertTrue(com.combatauras.settings.SettingsSnapshot.of(DEFAULTS).potionRemind(PotionEffect.ANTIVENOM.ordinal()));
        Assert.assertTrue(com.combatauras.settings.SettingsSnapshot.of(DEFAULTS).potionRemind(PotionEffect.DIVINE_MAGIC.ordinal()));
        Assert.assertTrue(com.combatauras.settings.SettingsSnapshot.of(DEFAULTS).potionRemind(PotionEffect.OVERLOAD.ordinal()));
        Assert.assertTrue(com.combatauras.settings.SettingsSnapshot.of(DEFAULTS).potionRemind(PotionEffect.SURGE.ordinal()));
        Assert.assertTrue(com.combatauras.settings.SettingsSnapshot.of(DEFAULTS).potionRemind(PotionEffect.GOADING.ordinal()));
        Assert.assertTrue(com.combatauras.settings.SettingsSnapshot.of(DEFAULTS).potionRemind(PotionEffect.PRAYER_REGENERATION.ordinal()));
        CombatAurasConfig separate = new CombatAurasConfig()
        {
            @Override
            public boolean remindSurge() { return false; }

            @Override
            public boolean remindGoading() { return false; }

            @Override
            public boolean remindPrayerRegeneration() { return false; }
        };
        Assert.assertFalse(com.combatauras.settings.SettingsSnapshot.of(separate).potionRemind(PotionEffect.SURGE.ordinal()));
        Assert.assertFalse(com.combatauras.settings.SettingsSnapshot.of(separate).potionRemind(PotionEffect.GOADING.ordinal()));
        Assert.assertFalse(com.combatauras.settings.SettingsSnapshot.of(separate).potionRemind(PotionEffect.PRAYER_REGENERATION.ordinal()));
        Assert.assertTrue("other potions toggle unaffected", com.combatauras.settings.SettingsSnapshot.of(separate).potionRemind(PotionEffect.MENAPHITE_REMEDY.ordinal()));
        CombatAurasConfig noAnti = new CombatAurasConfig()
        {
            @Override
            public boolean remindAntipoison() { return false; }

            @Override
            public boolean raidPotionsEnabled() { return false; }
        };
        Assert.assertFalse(com.combatauras.settings.SettingsSnapshot.of(noAnti).potionRemind(PotionEffect.ANTIPOISON.ordinal()));
        Assert.assertTrue("anti-venom has its own toggle in the Venom section",
            com.combatauras.settings.SettingsSnapshot.of(noAnti).potionRemind(PotionEffect.ANTIVENOM.ordinal()));
        Assert.assertFalse(com.combatauras.settings.SettingsSnapshot.of(noAnti).potionRemind(PotionEffect.SMELLING_SALTS.ordinal()));
        Assert.assertTrue(com.combatauras.settings.SettingsSnapshot.of(noAnti).potionRemind(PotionEffect.ANTIFIRE.ordinal()));
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
        CombatAurasConfig cfg = new CombatAurasConfig()
        {
            @Override
            public int boostRedoseRanged() { return 7; }

            @Override
            public int boostRedoseDefence() { return 5; }
        };
        Assert.assertEquals(7, com.combatauras.settings.SettingsSnapshot.of(cfg).redoseLevel(Skill.RANGED));
        Assert.assertEquals(0, com.combatauras.settings.SettingsSnapshot.of(cfg).redoseLevel(Skill.HITPOINTS));

        // Bastion: ranged +13 > 7 and defence +18 > 5 -> not ready yet
        Assert.assertFalse(boosts.getCooldown(fc.client, BoostPotion.BASTION, com.combatauras.settings.SettingsSnapshot.of(cfg)).isReady());
        fc.level(Skill.RANGED, 99, 106).level(Skill.DEFENCE, 99, 104);
        Assert.assertTrue("both at/below their own levels", boosts.getCooldown(fc.client, BoostPotion.BASTION, com.combatauras.settings.SettingsSnapshot.of(cfg)).isReady());
        fc.level(Skill.DEFENCE, 99, 110);
        Assert.assertFalse("defence still above +5", boosts.getCooldown(fc.client, BoostPotion.BASTION, com.combatauras.settings.SettingsSnapshot.of(cfg)).isReady());

        CombatAurasConfig untracked = new CombatAurasConfig()
        {
            @Override
            public int boostRedoseRanged() { return 0; }
        };
        Assert.assertNull("0 = don't track", boosts.getCooldown(fc.client, BoostPotion.RANGED, com.combatauras.settings.SettingsSnapshot.of(untracked)));
        Assert.assertNotNull(boosts.getCooldown(fc.client, BoostPotion.BASTION, com.combatauras.settings.SettingsSnapshot.of(untracked)));
    }

    // ---------------------------------------------------------------- settings layout

    @Test
    public void everySectionFollowsTheStandardLayout() throws Exception
    {
        Map<String, Integer> sectionPos = new HashMap<>();
        for (Field f : CombatAurasConfig.class.getDeclaredFields())
        {
            ConfigSection cs = f.getAnnotation(ConfigSection.class);
            if (cs != null)
            {
                sectionPos.put((String) f.get(null), cs.position());
            }
        }

        Map<String, Map<Integer, String>> bySection = new HashMap<>();
        for (Method m : CombatAurasConfig.class.getDeclaredMethods())
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
            if ("Screen glow".equals(slots.get(11)) && !e.getKey().equals("warnDamageSection"))
            {
                // every lasting screen glow can pulse like Low Health
                Assert.assertEquals(e.getKey() + " slot 9", "Heartbeat pulse", slots.get(9));
            }
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
