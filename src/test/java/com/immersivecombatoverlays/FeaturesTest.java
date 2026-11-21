package com.immersivecombatoverlays;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.CombatSpell;
import com.immersivecombatoverlays.model.Preset;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.immersivecombatoverlays.model.StatusEffect;
import com.immersivecombatoverlays.tracker.CombatSpellTracker;
import com.immersivecombatoverlays.tracker.ParasiteTracker;
import com.immersivecombatoverlays.tracker.SpellRecastProtection;
import com.immersivecombatoverlays.tracker.WarningNotifier;
import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.Notification;
import org.junit.Assert;
import org.junit.Test;

public class FeaturesTest
{
    private static final ImmersiveCombatOverlaysConfig DEFAULTS = new ImmersiveCombatOverlaysConfig() {};

    // ---------------------------------------------------------------- damage threshold

    @Test
    public void damageThresholdDefaultsToEveryHit()
    {
        Assert.assertEquals(0, DEFAULTS.damageMinAmount());
    }

    // ---------------------------------------------------------------- parasite

    @Test
    public void parasiteStartsOnImpregnationAndEndsOnWeakenOrBurst()
    {
        ParasiteTracker parasite = new ParasiteTracker();
        Assert.assertFalse(parasite.isActive());

        parasite.onChatMessage("<col=ff0000>The Nightmare has impregnated you with a deadly parasite!</col>");
        Assert.assertTrue(parasite.isActive());
        float early = parasite.getSeverity();
        for (int i = 0; i < 20; i++)
        {
            parasite.onGameTick();
        }
        Assert.assertTrue("urgency rises towards the burst", parasite.getSeverity() > early);

        parasite.onChatMessage("The parasite within you has been weakened.");
        Assert.assertFalse(parasite.isActive());

        parasite.onChatMessage("The Nightmare has impregnated you with a deadly parasite!");
        parasite.onChatMessage("The parasite bursts out of you, fully grown!");
        Assert.assertFalse(parasite.isActive());

        // Missed end message: fallback timeout clears it
        parasite.onChatMessage("The Nightmare has impregnated you with a deadly parasite!");
        for (int i = 0; i < 31; i++)
        {
            parasite.onGameTick();
        }
        Assert.assertFalse(parasite.isActive());
        Assert.assertEquals(0f, parasite.getSeverity(), 0.001f);

        Assert.assertEquals("PARASITE appended after existing effects", StatusEffect.values().length - 1, StatusEffect.PARASITE.ordinal());
    }

    // ---------------------------------------------------------------- vengeance recast protection

    private static MenuEntry entry(String option, String target, boolean[] flag)
    {
        return (MenuEntry) Proxy.newProxyInstance(MenuEntry.class.getClassLoader(), new Class<?>[]{MenuEntry.class},
            (p, m, a) -> {
                switch (m.getName())
                {
                    case "getOption": return option;
                    case "getTarget": return target;
                    case "setDeprioritized":
                        flag[0] = (Boolean) a[0];
                        return null;
                    default: return FakeClient.defaultValue(m.getReturnType());
                }
            });
    }

    private static Client clientWithMenu(FakeClient state, MenuEntry... entries)
    {
        Menu menu = (Menu) Proxy.newProxyInstance(Menu.class.getClassLoader(), new Class<?>[]{Menu.class},
            (p, m, a) -> "getMenuEntries".equals(m.getName()) ? entries : FakeClient.defaultValue(m.getReturnType()));
        return (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[]{Client.class},
            (p, m, a) -> "getMenu".equals(m.getName()) ? menu : m.invoke(state.client, a));
    }

    @Test
    public void vengeanceCastIsDeprioritizedOnlyWhileChargedOrOnCooldown()
    {
        SpellRecastProtection protection = new SpellRecastProtection();
        CombatSpellTracker spells = new CombatSpellTracker();
        FakeClient state = new FakeClient();
        boolean[] veng = {false};
        boolean[] vengOther = {false};
        boolean[] alch = {false};
        Client client = clientWithMenu(state,
            entry("Cast", "<col=00ff00>Vengeance</col>", veng),
            entry("Cast", "<col=00ff00>Vengeance Other</col>", vengOther),
            entry("Cast", "<col=00ff00>High Level Alchemy</col>", alch));

        protection.apply(client, DEFAULTS, spells);
        Assert.assertFalse("ready -> left-click allowed", veng[0]);

        state.varbit(VarbitID.VENGEANCE_REBOUND, 1);
        protection.apply(client, DEFAULTS, spells);
        Assert.assertTrue("still charged -> deprioritized", veng[0]);
        Assert.assertFalse(vengOther[0]);
        Assert.assertFalse(alch[0]);

        veng[0] = false;
        state.varbit(VarbitID.VENGEANCE_REBOUND, 0);
        spells.startCooldownTimer(CombatSpell.VENGEANCE, 20);
        protection.apply(client, DEFAULTS, spells);
        Assert.assertTrue("on cooldown -> deprioritized", veng[0]);

        veng[0] = false;
        ImmersiveCombatOverlaysConfig off = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean spellVengeanceRecastProtection() { return false; }
        };
        protection.apply(client, off, spells);
        Assert.assertFalse(veng[0]);
    }

    @Test
    public void thrallAndArceuusCastsAreDeprioritizedWhileNotRecastable()
    {
        SpellRecastProtection protection = new SpellRecastProtection();
        CombatSpellTracker spells = new CombatSpellTracker();
        FakeClient state = new FakeClient();
        boolean[] thrall = {false};
        boolean[] veil = {false};
        boolean[] mark = {false};
        boolean[] charge = {false};
        Client client = clientWithMenu(state,
            entry("Cast", "<col=00ff00>Resurrect Greater Ghost</col>", thrall),
            entry("Cast", "<col=00ff00>Shadow Veil</col>", veil),
            entry("Cast", "<col=00ff00>Mark of Darkness</col>", mark),
            entry("Cast", "<col=00ff00>Charge</col>", charge));

        protection.apply(client, DEFAULTS, spells);
        Assert.assertFalse("everything ready -> left-click allowed", thrall[0] || veil[0] || mark[0] || charge[0]);

        spells.startCooldownTimer(CombatSpell.RESURRECT_THRALL, 17);
        spells.startCooldownTimer(CombatSpell.MARK_OF_DARKNESS, 10);
        spells.startCooldownTimer(CombatSpell.CHARGE, 100);
        protection.apply(client, DEFAULTS, spells);
        Assert.assertTrue("thrall not recastable -> deprioritized", thrall[0]);
        Assert.assertFalse("Shadow Veil is ready", veil[0]);
        Assert.assertFalse("Mark of Darkness protection is off by default", mark[0]);
        Assert.assertFalse("Charge has no protection", charge[0]);

        thrall[0] = false;
        ImmersiveCombatOverlaysConfig off = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean spellThrallRecastProtection() { return false; }
        };
        protection.apply(client, off, spells);
        Assert.assertFalse("per-spell toggle off", thrall[0]);
    }

    @Test
    public void everyRecastableSpellExceptChargeHasAProtectionToggle()
    {
        for (CombatSpell spell : CombatSpell.values())
        {
            boolean expected = spell != CombatSpell.CHARGE && spell != CombatSpell.MARK_OF_DARKNESS;
            Assert.assertEquals(spell.name(), expected, ImmersiveCombatOverlaysConfig.isSpellRecastProtected(DEFAULTS, spell));
        }
    }

    @Test
    public void shadowVeilRemindsWhilePickpocketingAndChargeNeedsAGodCape()
    {
        CombatSpellTracker spells = new CombatSpellTracker();
        com.immersivecombatoverlays.tracker.CombatStateTracker combat = new com.immersivecombatoverlays.tracker.CombatStateTracker();
        combat.onCombatActivity(1L, 120);
        Assert.assertFalse("in combat but not thieving", spells.contextAllows(CombatSpell.SHADOW_VEIL, combat, DEFAULTS));
        combat.onMenuOptionClicked(net.runelite.api.MenuAction.NPC_THIRD_OPTION, "Pickpocket", "Knight of Ardougne", null, 1L, 120);
        Assert.assertTrue(spells.contextAllows(CombatSpell.SHADOW_VEIL, combat, DEFAULTS));

        Assert.assertFalse("no god cape", spells.contextAllows(CombatSpell.CHARGE, combat, DEFAULTS));
        spells.setGodCapeWorn(true);
        Assert.assertTrue(spells.contextAllows(CombatSpell.CHARGE, combat, DEFAULTS));
        Assert.assertTrue(spells.contextAllows(CombatSpell.VENGEANCE, combat, DEFAULTS));

        Assert.assertTrue(ImmersiveCombatOverlaysPlugin.isGodCape("Imbued saradomin cape"));
        Assert.assertTrue(ImmersiveCombatOverlaysPlugin.isGodCape("Zamorak max cape"));
        Assert.assertFalse(ImmersiveCombatOverlaysPlugin.isGodCape("Fire cape"));
        Assert.assertFalse(ImmersiveCombatOverlaysPlugin.isGodCape("Saradomin d'hide body"));
    }

    @Test
    public void everyDebugKeyIsResetOnStartup()
    {
        Set<String> declared = new HashSet<>();
        for (Method m : ImmersiveCombatOverlaysConfig.class.getDeclaredMethods())
        {
            net.runelite.client.config.ConfigItem item = m.getAnnotation(net.runelite.client.config.ConfigItem.class);
            if (item != null && item.keyName().startsWith("debug"))
            {
                declared.add(item.keyName());
            }
        }
        Assert.assertEquals(declared, new HashSet<>(Arrays.asList(ImmersiveCombatOverlaysConfig.DEBUG_KEYS)));
    }

    // ---------------------------------------------------------------- notifications

    @Test
    public void notifierFiresOnceOnRisingEdgeWithCooldown()
    {
        WarningNotifier notifier = new WarningNotifier();
        long t = 1_000_000_000L;
        Notification on = Notification.ON;

        Assert.assertFalse("inactive", notifier.update(WarningNotifier.Warning.LOW_HEALTH, false, on, null, t));
        Assert.assertTrue("rising edge fires", notifier.update(WarningNotifier.Warning.LOW_HEALTH, true, on, null, t));
        Assert.assertFalse("staying active does not repeat", notifier.update(WarningNotifier.Warning.LOW_HEALTH, true, on, null, t + 1_000_000_000L));

        notifier.update(WarningNotifier.Warning.LOW_HEALTH, false, on, null, t + 2_000_000_000L);
        Assert.assertFalse("flapping within cooldown is suppressed",
            notifier.update(WarningNotifier.Warning.LOW_HEALTH, true, on, null, t + 3_000_000_000L));
        notifier.update(WarningNotifier.Warning.LOW_HEALTH, false, on, null, t + 4_000_000_000L);
        Assert.assertTrue("fires again after cooldown",
            notifier.update(WarningNotifier.Warning.LOW_HEALTH, true, on, null, t + 20_000_000_000L));

        Assert.assertFalse("OFF never fires", notifier.update(WarningNotifier.Warning.FREEZE, true, Notification.OFF, null, t));
        Assert.assertTrue("independent per warning", notifier.update(WarningNotifier.Warning.PARASITE, true, on, null, t));

        Assert.assertTrue(notifier.fire(WarningNotifier.Warning.SPELL_READY, on, null, t));
        Assert.assertFalse("one-shot fire still honours cooldown", notifier.fire(WarningNotifier.Warning.SPELL_READY, on, null, t + 1_000_000_000L));

        notifier.reset();
        Assert.assertTrue(notifier.update(WarningNotifier.Warning.LOW_HEALTH, true, on, null, t + 21_000_000_000L));
    }

    @Test
    public void allNotificationsDefaultOff()
    {
        Assert.assertFalse(DEFAULTS.lowHealthNotification().isEnabled());
        Assert.assertFalse(DEFAULTS.lowPrayerNotification().isEnabled());
        Assert.assertFalse(DEFAULTS.freezeNotification().isEnabled());
        Assert.assertFalse(DEFAULTS.unpottedNotification().isEnabled());
        Assert.assertFalse(DEFAULTS.specNotification().isEnabled());
        Assert.assertFalse(DEFAULTS.heartNotification().isEnabled());
        Assert.assertFalse(DEFAULTS.buffNotification().isEnabled());
        for (CombatSpell spell : CombatSpell.values())
        {
            Assert.assertFalse(ImmersiveCombatOverlaysConfig.getSpellNotification(DEFAULTS, spell).isEnabled());
        }
        Assert.assertFalse(DEFAULTS.poisonNotification().isEnabled());
        Assert.assertFalse(DEFAULTS.boostNotification().isEnabled());
        Assert.assertFalse(DEFAULTS.parasiteNotification().isEnabled());
        Assert.assertFalse(DEFAULTS.idleNotification().isEnabled());
    }
    // ---------------------------------------------------------------- presets & HP-scaled flash

    @Test
    public void presetsOnlyTouchRealSettingsWithMatchingTypes() throws Exception
    {
        Set<String> managed = new HashSet<>(Arrays.asList(Preset.MANAGED_KEYS));
        Assert.assertTrue(Preset.CUSTOM.settings().isEmpty());
        Assert.assertEquals(Preset.CUSTOM, DEFAULTS.preset());

        for (Preset preset : Preset.values())
        {
            for (Map.Entry<String, Object> e : preset.settings().entrySet())
            {
                Method m = ImmersiveCombatOverlaysConfig.class.getMethod(e.getKey());
                Assert.assertTrue(preset + " touches unmanaged key " + e.getKey(), managed.contains(e.getKey()));
                if (e.getValue() != null)
                {
                    Class<?> t = m.getReturnType();
                    Class<?> boxed = t == boolean.class ? Boolean.class : t == int.class ? Integer.class : t;
                    Assert.assertEquals(preset + "." + e.getKey(), boxed, e.getValue().getClass());
                }
            }
        }
        // STANDARD resets every managed key to its default
        Assert.assertEquals(managed, Preset.STANDARD.settings().keySet());
        for (Object v : Preset.STANDARD.settings().values())
        {
            Assert.assertNull(v);
        }
        Assert.assertEquals(Boolean.FALSE, Preset.MINIMAL.settings().get("allowIconShowers"));
        Assert.assertEquals(Boolean.TRUE, Preset.EVERYTHING.settings().get("specEnabled"));
    }

    @Test
    public void damageFlashScalesWithShareOfMaxHp()
    {
        Assert.assertEquals(0f, ImmersiveCombatOverlaysPlugin.damageFlashStrength(0, 99, true), 0.001f);
        float lowLevel = ImmersiveCombatOverlaysPlugin.damageFlashStrength(10, 40, true);
        float highLevel = ImmersiveCombatOverlaysPlugin.damageFlashStrength(25, 99, true);
        Assert.assertEquals("same share of HP -> same flash", lowLevel, highLevel, 0.02f);
        Assert.assertEquals("40% of max HP is a full flash", 1f, ImmersiveCombatOverlaysPlugin.damageFlashStrength(40, 99, true), 0.02f);
        Assert.assertTrue(ImmersiveCombatOverlaysPlugin.damageFlashStrength(10, 40, true)
            > ImmersiveCombatOverlaysPlugin.damageFlashStrength(10, 99, true));
        Assert.assertEquals("fixed scale unchanged when off", 0.5f, ImmersiveCombatOverlaysPlugin.damageFlashStrength(10, 99, false), 0.001f);
        Assert.assertTrue(DEFAULTS.damageScaleWithHp());
    }
}
