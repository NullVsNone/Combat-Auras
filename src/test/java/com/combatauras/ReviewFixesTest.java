package com.combatauras;

import com.combatauras.config.CombatAurasConfig;
import com.combatauras.model.BoostPotion;
import com.combatauras.model.CombatSpell;
import com.combatauras.model.PotionEffect;
import com.combatauras.model.StatusEffect;
import com.combatauras.model.VisualStyle;
import com.combatauras.render.ProceduralRenderer;
import com.combatauras.settings.SettingsSnapshot;
import com.combatauras.tracker.BoostPotionTimers;
import com.combatauras.tracker.ChatText;
import com.combatauras.tracker.CombatSpellTracker;
import com.combatauras.tracker.HeartCooldownTracker;
import com.combatauras.tracker.ItemCooldowns;
import com.combatauras.tracker.PotionItems;
import com.combatauras.tracker.PotionTimers;
import com.combatauras.tracker.SpellRecastProtection;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import org.junit.Assert;
import org.junit.Test;

/** Regression tests for the issues found in the external code review. */
public class ReviewFixesTest
{
    private static final long SEC = 1_000_000_000L;

    @Test
    public void chatTextStripsColourTagsAndHighlightMacros()
    {
        Assert.assertEquals("You resurrect a greater zombified thrall.",
            ChatText.clean("@mes_hl_mag@You resurrect a greater zombified thrall.</col>"));
        Assert.assertEquals("Your thieving abilities have been enhanced.",
            ChatText.clean("<col=6800bf>Your thieving abilities have been enhanced.</col>"));
        Assert.assertEquals("Cast", ChatText.clean("<col=ffffff>Cast</col>"));
        Assert.assertEquals("plain text", ChatText.clean("plain text"));
        Assert.assertEquals("not @ a macro", ChatText.clean("not @ a macro"));
        Assert.assertNull(ChatText.clean(null));
    }

    @Test
    public void arceuusMessagesWithHighlightMacrosStartTheirRealDurations()
    {
        FakeClient fc = new FakeClient().level(Skill.MAGIC, 99, 99);
        CombatSpellTracker tracker = new CombatSpellTracker();
        tracker.onChatMessage(fc.client, "@mes_hl_mag@You resurrect a greater zombified thrall.</col>");
        Assert.assertEquals(99, tracker.getCooldown(CombatSpell.RESURRECT_THRALL).getRemainingTicks());
        tracker.onChatMessage(fc.client, "@mes_hl_blu@Your defence against Arceuus magic has been strengthened.</col>");
        Assert.assertEquals(99, tracker.getCooldown(CombatSpell.WARD_OF_ARCEUUS).getRemainingTicks());
        tracker.onChatMessage(fc.client, "@mes_hl_pur@Your thieving abilities have been enhanced.</col>");
        Assert.assertEquals(99, tracker.getCooldown(CombatSpell.SHADOW_VEIL).getRemainingTicks());
        tracker.onChatMessage(fc.client, "@mes_hl_pur@You have placed a Mark of Darkness upon yourself.</col>");
        Assert.assertEquals(297, tracker.getCooldown(CombatSpell.MARK_OF_DARKNESS).getRemainingTicks());
    }

    private static SettingsSnapshot thrallLeadAndDuration(int leadSeconds, int durationSeconds)
    {
        return SettingsSnapshot.of(new CombatAurasConfig()
        {
            @Override
            public int spellThrallLeadSeconds() { return leadSeconds; }

            @Override
            public int spellThrallDurationSeconds() { return durationSeconds; }
        });
    }

    @Test
    public void thrallOutlastingTheEstimateDoesNotRearmTheReminderEvery17Ticks()
    {
        FakeClient fc = new FakeClient().varbit(VarbitID.ARCEUUS_RESURRECTION_ACTIVE, 1);
        CombatSpellTracker tracker = new CombatSpellTracker();
        SettingsSnapshot s = thrallLeadAndDuration(10, 5); // lead covers the whole 17-tick estimate: always due
        tracker.onGameTick(fc.client); // missed the cast message: 17-tick estimate
        Assert.assertFalse(tracker.getCooldown(CombatSpell.RESURRECT_THRALL).isReady());

        long t0 = 100 * SEC;
        Assert.assertTrue(tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 0, s, t0));
        Assert.assertFalse(tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 0, s, t0 + 6 * SEC));
        for (int i = 0; i < 40; i++)
        {
            tracker.onGameTick(fc.client); // the game keeps the thrall up well past our estimate
            Assert.assertFalse("still summoned", tracker.getCooldown(CombatSpell.RESURRECT_THRALL).isReady());
        }
        Assert.assertFalse("the reminder must not restart every 10.2 s",
            tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 0, s, t0 + 7 * SEC));
    }

    @Test
    public void castMessageArrivingBeforeItsVarbitKeepsTheTimer()
    {
        FakeClient fc = new FakeClient().level(Skill.MAGIC, 99, 99);
        CombatSpellTracker tracker = new CombatSpellTracker();
        tracker.onChatMessage(fc.client, "@mes_hl_mag@You resurrect a greater ghost thrall.</col>");
        tracker.onGameTick(fc.client); // varbit still 0 this tick
        Assert.assertEquals(98, tracker.getCooldown(CombatSpell.RESURRECT_THRALL).getRemainingTicks());
        fc.varbit(VarbitID.ARCEUUS_RESURRECTION_ACTIVE, 1);
        tracker.onGameTick(fc.client);
        Assert.assertEquals(97, tracker.getCooldown(CombatSpell.RESURRECT_THRALL).getRemainingTicks());
        fc.varbit(VarbitID.ARCEUUS_RESURRECTION_ACTIVE, 0); // thrall gone
        tracker.onGameTick(fc.client);
        Assert.assertTrue(tracker.getCooldown(CombatSpell.RESURRECT_THRALL).isReady());
    }

    @Test
    public void warnEarlyAndExpiryAreOneReminderNotTwo()
    {
        FakeClient fc = new FakeClient().level(Skill.MAGIC, 20, 20).varbit(VarbitID.ARCEUUS_RESURRECTION_ACTIVE, 1);
        CombatSpellTracker tracker = new CombatSpellTracker();
        SettingsSnapshot s = thrallLeadAndDuration(6, 5); // warn 10 ticks early, show 5 s
        tracker.onChatMessage(fc.client, "You resurrect a lesser ghost thrall.");
        long t = 100 * SEC;
        while (!tracker.isDue(CombatSpell.RESURRECT_THRALL, s))
        {
            tracker.onGameTick(fc.client);
            t += 600_000_000L;
        }
        Assert.assertTrue(tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 0, s, t));
        long warned = t;
        for (int i = 0; i < 12; i++)
        {
            tracker.onGameTick(fc.client);
        }
        fc.varbit(VarbitID.ARCEUUS_RESURRECTION_ACTIVE, 0);
        tracker.onGameTick(fc.client);
        Assert.assertTrue(tracker.getCooldown(CombatSpell.RESURRECT_THRALL).isReady());
        Assert.assertFalse("expiry continues the warn-early reminder instead of starting a second one",
            tracker.isWithinShowerDuration(CombatSpell.RESURRECT_THRALL, 0, s, warned + 6 * SEC));
    }

    @Test
    public void thrallRemindersNeedTheBookOfTheDead()
    {
        CombatSpellTracker tracker = new CombatSpellTracker();
        com.combatauras.tracker.CombatStateTracker combat = new com.combatauras.tracker.CombatStateTracker();
        combat.setSimulatedCombat(true);
        SettingsSnapshot s = SettingsSnapshot.of(new CombatAurasConfig() {});
        Assert.assertTrue(tracker.contextAllows(CombatSpell.RESURRECT_THRALL, combat, s));
        tracker.setHasBookOfTheDead(false);
        Assert.assertFalse(tracker.contextAllows(CombatSpell.RESURRECT_THRALL, combat, s));
        Assert.assertTrue("other Arceuus spells don't need it", tracker.contextAllows(CombatSpell.DEATH_CHARGE, combat, s));
    }

    @Test
    public void activeDivineDecidesWhenARegularPotionForTheSameStatIsWorthDrinking()
    {
        FakeClient fc = new FakeClient().level(Skill.RANGED, 99, 112).varbit(VarbitID.DIVINERANGE_POTION_TIME, 50);
        PotionTimers potions = new PotionTimers();
        potions.onGameTick(fc.client);
        BoostPotionTimers boosts = new BoostPotionTimers();
        SettingsSnapshot s = SettingsSnapshot.of(new CombatAurasConfig() {});

        Assert.assertEquals("re-dose when the divine ends (stat snaps to base), not a decay estimate like 2:16",
            50, ItemCooldowns.boostCooldown(BoostPotion.RANGED, s, fc.client, potions, boosts).getRemainingTicks());

        fc.varbit(VarbitID.DIVINERANGE_POTION_TIME, 0);
        potions.onGameTick(fc.client);
        Assert.assertTrue("without the divine the normal decay estimate applies",
            ItemCooldowns.boostCooldown(BoostPotion.RANGED, s, fc.client, potions, boosts).getRemainingTicks() > 100);
    }

    @Test
    public void raidPotionsAndSanfewAreClassified()
    {
        Assert.assertEquals(BoostPotion.ELDER_STRONG, PotionItems.getBoost("Elder (+)(4)"));
        Assert.assertEquals(BoostPotion.ELDER, PotionItems.getBoost("Elder potion (3)"));
        Assert.assertEquals(BoostPotion.TWISTED_WEAK, PotionItems.getBoost("Twisted (-)(2)"));
        Assert.assertEquals(BoostPotion.KODAI_STRONG, PotionItems.getBoost("Kodai (+)(1)"));
        Assert.assertNull(PotionItems.getBoost("Twisted bow"));
        Assert.assertNull(PotionItems.getBoost("Elder maul"));
        Assert.assertEquals(PotionEffect.ANTIPOISON, PotionItems.getEffect("Sanfew serum(4)"));
        Assert.assertEquals(21, BoostPotion.ELDER_STRONG.getMaxBoost(Skill.ATTACK, 99)); // 6 + 16% of 99
    }

    @Test
    public void saturatedHeartIsReadFromTheGameAfterLoggingInMidCooldown()
    {
        FakeClient fc = new FakeClient().varbit(VarbitID.IMBUED_HEART_TIMER, 30).varbit(VarbitID.SATURATED_HEART_TIME, 300);
        HeartCooldownTracker heart = new HeartCooldownTracker();
        heart.onGameTick(fc.client); // no Invigorate click seen
        heart.onGameTick(fc.client);
        Assert.assertTrue(heart.isSaturatedBoostActive());
        Assert.assertEquals(500, heart.getCooldown().getTotalTicks());

        fc.varbit(VarbitID.SATURATED_HEART_TIME, 0);
        heart.onGameTick(fc.client);
        Assert.assertFalse("imbued heart cooldown", heart.isSaturatedBoostActive());
    }

    @Test
    public void taggedCastOptionsAreStillRecastProtected()
    {
        boolean[] veng = {false};
        MenuEntry entry = (MenuEntry) Proxy.newProxyInstance(MenuEntry.class.getClassLoader(), new Class<?>[]{MenuEntry.class},
            (p, m, a) -> {
                switch (m.getName())
                {
                    case "getOption": return "<col=ffffff>Cast</col>";
                    case "getTarget": return "<col=00ff00>Vengeance</col>";
                    case "setDeprioritized":
                        veng[0] = (Boolean) a[0];
                        return null;
                    default: return FakeClient.defaultValue(m.getReturnType());
                }
            });
        FakeClient state = new FakeClient().varbit(VarbitID.VENGEANCE_REBOUND, 1);
        Menu menu = (Menu) Proxy.newProxyInstance(Menu.class.getClassLoader(), new Class<?>[]{Menu.class},
            (p, m, a) -> "getMenuEntries".equals(m.getName()) ? new MenuEntry[]{entry} : FakeClient.defaultValue(m.getReturnType()));
        Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[]{Client.class},
            (p, m, a) -> "getMenu".equals(m.getName()) ? menu : m.invoke(state.client, a));
        new SpellRecastProtection(null, null, null).apply(client, SettingsSnapshot.of(new CombatAurasConfig() {}), new CombatSpellTracker());
        Assert.assertTrue(veng[0]);
    }

    @Test
    public void everyScreenGlowSectionHasAVisualStylePick() throws Exception
    {
        Map<String, Map<Integer, String>> bySection = new HashMap<>();
        for (Method m : CombatAurasConfig.class.getDeclaredMethods())
        {
            ConfigItem item = m.getAnnotation(ConfigItem.class);
            if (item != null)
            {
                bySection.computeIfAbsent(item.section(), x -> new HashMap<>()).put(item.position(), item.name());
            }
        }
        int checked = 0;
        for (Map.Entry<String, Map<Integer, String>> e : bySection.entrySet())
        {
            if ("Screen glow".equals(e.getValue().get(11)))
            {
                Assert.assertEquals(e.getKey() + " slot 16", "Visual style", e.getValue().get(16));
                checked++;
            }
        }
        Assert.assertTrue(checked >= 20);

        int styleSections = 0;
        for (Field f : CombatAurasConfig.class.getDeclaredFields())
        {
            ConfigSection cs = f.getAnnotation(ConfigSection.class);
            if (cs != null && cs.name().startsWith("Visual Style "))
            {
                Assert.assertTrue("style sections sit with the shared ones", cs.position() < 10);
                Assert.assertEquals(12, bySection.get((String) f.get(null)).size());
                styleSections++;
            }
        }
        Assert.assertEquals(4, styleSections);
    }

    private static int alphaAt(BufferedImage img, int x, int y)
    {
        return (img.getRGB(x, y) >>> 24) & 0xFF;
    }

    @Test
    public void twoStylesOnDisjointEdgesEachDrawTheirOwnEdgesWithoutPerFrameRebuilds()
    {
        CombatAurasConfig cfg = new CombatAurasConfig()
        {
            @Override
            public VisualStyle lowHealthVisualStyle() { return VisualStyle.STYLE_2; } // top + bottom

            @Override
            public VisualStyle lowPrayerVisualStyle() { return VisualStyle.STYLE_3; } // left + right

            @Override
            public int style2Tint() { return 0; }
        };
        SettingsSnapshot s = SettingsSnapshot.of(cfg);
        float[] sev = new float[StatusEffect.values().length];
        int[] colors = new int[StatusEffect.values().length];
        sev[StatusEffect.LOW_HEALTH.ordinal()] = 0.9f;
        colors[StatusEffect.LOW_HEALTH.ordinal()] = 0xD2C3161A;
        sev[StatusEffect.LOW_PRAYER.ordinal()] = 0.9f;
        colors[StatusEffect.LOW_PRAYER.ordinal()] = 0xC8349EE0;

        ProceduralRenderer renderer = new ProceduralRenderer();
        BufferedImage canvas = null;
        long t = SEC;
        for (int i = 0; i < 60; i++)
        {
            canvas = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            renderer.render(g, 400, 300, t, s, null, null, null, sev, colors,
                0, new int[0], new float[0], new float[0], 0, new int[0], new int[0], 0, new int[0], new int[0]);
            g.dispose();
            t += 50_000_000L;
        }
        Assert.assertEquals("one gradient set per style, built once", 2, renderer.getPaintBuilds());
        int top = canvas.getRGB(200, 1);
        int left = canvas.getRGB(1, 150);
        Assert.assertTrue("top edge glows", alphaAt(canvas, 200, 1) > 0);
        Assert.assertTrue("left edge glows", alphaAt(canvas, 1, 150) > 0);
        Assert.assertTrue("top is the red low-HP style", ((top >>> 16) & 0xFF) > (top & 0xFF));
        Assert.assertTrue("left is the blue low-prayer style", (left & 0xFF) > ((left >>> 16) & 0xFF));
        Assert.assertEquals("middle stays clear", 0, alphaAt(canvas, 200, 150));
    }

    @Test
    public void aSingleStyleOnlyDrawsItsOwnEdges()
    {
        CombatAurasConfig cfg = new CombatAurasConfig()
        {
            @Override
            public VisualStyle lowHealthVisualStyle() { return VisualStyle.STYLE_2; }
        };
        float[] sev = new float[StatusEffect.values().length];
        int[] colors = new int[StatusEffect.values().length];
        sev[StatusEffect.LOW_HEALTH.ordinal()] = 0.9f;
        colors[StatusEffect.LOW_HEALTH.ordinal()] = 0xD2C3161A;
        ProceduralRenderer renderer = new ProceduralRenderer();
        BufferedImage canvas = null;
        long t = SEC;
        for (int i = 0; i < 40; i++)
        {
            canvas = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            renderer.render(g, 400, 300, t, SettingsSnapshot.of(cfg), null, null, null, sev, colors,
                0, new int[0], new float[0], new float[0], 0, new int[0], new int[0], 0, new int[0], new int[0]);
            g.dispose();
            t += 50_000_000L;
        }
        Assert.assertTrue(alphaAt(canvas, 200, 1) > 0);
        Assert.assertEquals("Style 2 has no left edge", 0, alphaAt(canvas, 1, 150));
    }
}
