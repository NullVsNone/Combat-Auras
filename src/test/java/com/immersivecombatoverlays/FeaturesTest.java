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

        protection.onPostMenuSort(client, DEFAULTS, spells);
        Assert.assertFalse("ready -> left-click allowed", veng[0]);

        state.varbit(VarbitID.VENGEANCE_REBOUND, 1);
        protection.onPostMenuSort(client, DEFAULTS, spells);
        Assert.assertTrue("still charged -> deprioritized", veng[0]);
        Assert.assertFalse(vengOther[0]);
        Assert.assertFalse(alch[0]);

        veng[0] = false;
        state.varbit(VarbitID.VENGEANCE_REBOUND, 0);
        spells.startCooldownTimer(CombatSpell.VENGEANCE, 20);
        protection.onPostMenuSort(client, DEFAULTS, spells);
        Assert.assertTrue("on cooldown -> deprioritized", veng[0]);

        veng[0] = false;
        ImmersiveCombatOverlaysConfig off = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean vengeanceRecastProtection() { return false; }
        };
        protection.onPostMenuSort(client, off, spells);
        Assert.assertFalse(veng[0]);
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
        Assert.assertFalse(DEFAULTS.spellNotification().isEnabled());
        Assert.assertFalse(DEFAULTS.parasiteNotification().isEnabled());
        Assert.assertFalse(DEFAULTS.idleNotification().isEnabled());
    }
    // ---------------------------------------------------------------- presets & HP-scaled flash

    @Test
    public void presetsOnlyTouchRealSettingsWithMatchingTypes() throws Exception
    {
        Set<String> managed = new HashSet<>(Arrays.asList(Preset.MANAGED_KEYS));
        Assert.assertTrue(Preset.CUSTOM.values().isEmpty());
        Assert.assertEquals(Preset.CUSTOM, DEFAULTS.preset());

        for (Preset preset : Preset.values())
        {
            for (Map.Entry<String, Object> e : preset.values().entrySet())
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
        Assert.assertEquals(managed, Preset.STANDARD.values().keySet());
        for (Object v : Preset.STANDARD.values().values())
        {
            Assert.assertNull(v);
        }
        Assert.assertEquals(Boolean.FALSE, Preset.MINIMAL.values().get("allowIconShowers"));
        Assert.assertEquals(Boolean.TRUE, Preset.EVERYTHING.values().get("specEnabled"));
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
