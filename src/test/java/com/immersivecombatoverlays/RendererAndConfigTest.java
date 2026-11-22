package com.immersivecombatoverlays;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.BorderStyle;
import com.immersivecombatoverlays.model.CombatSpell;
import com.immersivecombatoverlays.model.Cooldown;
import com.immersivecombatoverlays.model.GlowColorMode;
import com.immersivecombatoverlays.model.ShowerStartLocation;
import com.immersivecombatoverlays.model.StatusEffect;
import com.immersivecombatoverlays.overlay.ImmersiveStatusOverlay;
import com.immersivecombatoverlays.overlay.InventoryCooldownOverlay;
import com.immersivecombatoverlays.overlay.SpellCooldownOverlay;
import com.immersivecombatoverlays.render.CooldownRenderer;
import com.immersivecombatoverlays.render.ItemOutlineCache;
import com.immersivecombatoverlays.render.ProceduralRenderer;
import com.immersivecombatoverlays.tracker.HeartCooldownTracker;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.client.config.ConfigSection;
import java.lang.reflect.Field;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigItem;
import org.junit.Assert;
import org.junit.Test;

public class RendererAndConfigTest
{
    @Test
    public void testCooldownMathAndFormatting()
    {
        Cooldown cd = Cooldown.active(50, 100);
        Assert.assertFalse(cd.isReady());
        Assert.assertEquals(0.5f, cd.getRemainingFraction(), 0.001f);
        Assert.assertEquals(30.0f, cd.getRemainingSeconds(), 0.01f);

        Assert.assertEquals("5.4", CooldownRenderer.formatSeconds(5.4f));
        Assert.assertEquals("15", CooldownRenderer.formatSeconds(15.0f));
        Assert.assertEquals("1:05", CooldownRenderer.formatSeconds(65.0f));

        Assert.assertSame("ready() is a shared instance (no allocation)", Cooldown.READY, Cooldown.ready());
        Assert.assertSame(Cooldown.READY, Cooldown.active(0, 10));
        Assert.assertEquals(0f, Cooldown.READY.getRemainingFraction(), 0.001f);
        Assert.assertTrue(Cooldown.unknown(5).isUnknownDuration());
    }

    private static float[] sev(StatusEffect e, float v)
    {
        float[] s = new float[StatusEffect.values().length];
        s[e.ordinal()] = v;
        return s;
    }

    private static BufferedImage renderGlow(ImmersiveCombatOverlaysConfig cfg, float[] severities, int[] colors, int frames)
    {
        ProceduralRenderer renderer = new ProceduralRenderer();
        BufferedImage canvas = null;
        long t = 1_000_000_000L;
        for (int i = 0; i < frames; i++)
        {
            canvas = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            renderer.render(g, 400, 300, t, com.immersivecombatoverlays.settings.SettingsSnapshot.of(cfg), null, null, null, severities, colors,
                0, new int[0], 0, new int[0], new int[0], 0, new int[0], new int[0]);
            g.dispose();
            t += 50_000_000L;
        }
        return canvas;
    }

    @Test
    public void pulsingLowHealthDoesNotRebuildGradientsEveryFrame()
    {
        float[] sev = new float[StatusEffect.values().length];
        sev[StatusEffect.LOW_HEALTH.ordinal()] = 0.9f;
        sev[StatusEffect.POISON.ordinal()] = 0.6f;
        int[] colors = new int[StatusEffect.values().length];
        colors[StatusEffect.LOW_HEALTH.ordinal()] = 0xD2C3161A;
        colors[StatusEffect.POISON.ordinal()] = 0xC336B23E;
        ProceduralRenderer renderer = new ProceduralRenderer();
        com.immersivecombatoverlays.settings.SettingsSnapshot s = com.immersivecombatoverlays.settings.SettingsSnapshot.of(new ImmersiveCombatOverlaysConfig() {});
        long t = 1_000_000_000L;
        for (int i = 0; i < 40; i++)
        {
            BufferedImage canvas = new BufferedImage(200, 150, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            renderer.render(g, 200, 150, t, s, null, null, null, sev, colors,
                0, new int[0], 0, new int[0], new int[0], 0, new int[0], new int[0]);
            g.dispose();
            t += 16_000_000L; // 60 fps across a full heartbeat
        }
        Assert.assertEquals("blended colour is stable while the pulse animates", 1, renderer.getPaintBuilds());
    }

    @Test
    public void transparentColoursFadeTheGlowInsteadOfFreezingIt()
    {
        int[] colors = new int[StatusEffect.values().length];
        colors[StatusEffect.LOW_HEALTH.ordinal()] = 0x00FF0000; // alpha 0
        BufferedImage img = renderGlow(new ImmersiveCombatOverlaysConfig() {}, sev(StatusEffect.LOW_HEALTH, 1f), colors, 5);
        Assert.assertEquals(0, (img.getRGB(0, 150) >>> 24) & 0xFF);
    }

    @Test
    public void lruCacheEvictsOnlyTheOldestEntry()
    {
        com.immersivecombatoverlays.render.LruCache<Integer, String> cache = new com.immersivecombatoverlays.render.LruCache<>(3);
        cache.put(1, "a");
        cache.put(2, "b");
        cache.put(3, "c");
        cache.get(1); // touch: 2 is now the oldest
        cache.put(4, "d");
        Assert.assertEquals(3, cache.size());
        Assert.assertFalse(cache.containsKey(2));
        Assert.assertTrue(cache.containsKey(1) && cache.containsKey(3) && cache.containsKey(4));
    }

    @Test
    public void snapshotInvariantsCoverEveryEnumEntry()
    {
        com.immersivecombatoverlays.settings.SettingsSnapshot s = com.immersivecombatoverlays.settings.SettingsSnapshot.of(new ImmersiveCombatOverlaysConfig() {});
        for (StatusEffect e : StatusEffect.values())
        {
            Assert.assertNotNull(e.name(), s.statusNotification(e.ordinal()));
        }
        for (CombatSpell spell : CombatSpell.values())
        {
            Assert.assertNotNull(spell.name(), s.spellAwtColor(spell.ordinal()));
            Assert.assertNotNull(spell.name(), s.spellNotification(spell.ordinal()));
        }
        for (com.immersivecombatoverlays.model.ReminderFamily f : com.immersivecombatoverlays.model.ReminderFamily.values())
        {
            Assert.assertNotNull(f.name(), s.familyNotification(f.ordinal()));
        }
        for (com.immersivecombatoverlays.model.PotionEffect e : com.immersivecombatoverlays.model.PotionEffect.values())
        {
            Assert.assertTrue(e.name(), s.potionLeadTicks(e.ordinal()) >= 0);
        }
        // The status overlay's item buffer holds every pipeline reminder plus the debug previews
        Assert.assertTrue(com.immersivecombatoverlays.tracker.ReminderPipeline.MAX_ITEMS >= 2);
    }

    @Test
    public void glowEdgesCanBeAnyCombination()
    {
        int[] colors = new int[StatusEffect.values().length];
        colors[StatusEffect.LOW_HEALTH.ordinal()] = 0xFFFF0000;
        ImmersiveCombatOverlaysConfig onlyBottom = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean glowTop() { return false; }

            @Override
            public boolean glowLeft() { return false; }

            @Override
            public boolean glowRight() { return false; }

            @Override
            public int screenTint() { return 0; }
        };
        BufferedImage img = renderGlow(onlyBottom, sev(StatusEffect.LOW_HEALTH, 1f), colors, 5);
        Assert.assertEquals("top edge off", 0, (img.getRGB(200, 1) >>> 24) & 0xFF);
        Assert.assertEquals("left edge off", 0, (img.getRGB(1, 150) >>> 24) & 0xFF);
        Assert.assertTrue("bottom edge on", ((img.getRGB(200, 298) >>> 24) & 0xFF) > 0);
        Assert.assertTrue("bottom strip runs through the corner (no hard cut)", ((img.getRGB(1, 298) >>> 24) & 0xFF) > 0);
        Assert.assertEquals("top-left corner stays empty", 0, (img.getRGB(1, 1) >>> 24) & 0xFF);

        Assert.assertEquals("both edges: radial corner", "corner",
            com.immersivecombatoverlays.render.ProceduralRenderer.pickCornerPaint(true, true, "corner", "h", "v"));
        Assert.assertEquals("h", com.immersivecombatoverlays.render.ProceduralRenderer.pickCornerPaint(true, false, "corner", "h", "v"));
        Assert.assertEquals("v", com.immersivecombatoverlays.render.ProceduralRenderer.pickCornerPaint(false, true, "corner", "h", "v"));
        Assert.assertNull(com.immersivecombatoverlays.render.ProceduralRenderer.pickCornerPaint(false, false, "corner", "h", "v"));
    }

    @Test
    public void everyEnumEntryHasASettingsBinding()
    {
        Assert.assertEquals(CombatSpell.values().length, com.immersivecombatoverlays.settings.Bindings.SPELLS.size());
        Assert.assertEquals(StatusEffect.values().length, com.immersivecombatoverlays.settings.Bindings.STATUSES.size());
        Assert.assertEquals(com.immersivecombatoverlays.model.ReminderFamily.values().length, com.immersivecombatoverlays.settings.Bindings.FAMILIES.size());
        Assert.assertEquals(com.immersivecombatoverlays.model.PotionFamily.values().length, com.immersivecombatoverlays.settings.Bindings.POTIONS.size());
        // Building a snapshot from the defaults exercises every binding
        Assert.assertNotNull(com.immersivecombatoverlays.settings.SettingsSnapshot.of(new ImmersiveCombatOverlaysConfig() {}));
    }

    @Test
    public void vignetteCornersAreSeamlessNotDoubled()
    {
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public int screenTint() { return 0; }
        };
        int[] colors = new int[StatusEffect.values().length];
        colors[StatusEffect.LOW_HEALTH.ordinal()] = 0xFFFF0000;
        BufferedImage img = renderGlow(cfg, sev(StatusEffect.LOW_HEALTH, 1f), colors, 40);

        int corner = (img.getRGB(0, 0) >>> 24) & 0xFF;
        int topEdge = (img.getRGB(200, 0) >>> 24) & 0xFF;
        int leftEdge = (img.getRGB(0, 150) >>> 24) & 0xFF;
        Assert.assertTrue(topEdge > 100);
        // Old renderer overlapped strips in the corners (~2x alpha). Corner must match the edges.
        Assert.assertTrue("corner " + corner + " vs edge " + topEdge, Math.abs(corner - topEdge) <= 6);
        Assert.assertTrue(Math.abs(corner - leftEdge) <= 6);
        Assert.assertEquals("centre stays clear", 0, (img.getRGB(200, 150) >>> 24) & 0xFF);
        // Falloff is monotonic from edge to centre
        int a1 = (img.getRGB(200, 5) >>> 24) & 0xFF;
        int a2 = (img.getRGB(200, 25) >>> 24) & 0xFF;
        int a3 = (img.getRGB(200, 50) >>> 24) & 0xFF;
        Assert.assertTrue(a1 >= a2 && a2 >= a3);
    }

    @Test
    public void strongestColourModeUsesDominantWarning()
    {
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public GlowColorMode glowColorMode() { return GlowColorMode.STRONGEST; }

            @Override
            public int screenTint() { return 0; }
        };
        float[] s = new float[StatusEffect.values().length];
        int[] colors = new int[StatusEffect.values().length];
        s[StatusEffect.LOW_HEALTH.ordinal()] = 1f;
        colors[StatusEffect.LOW_HEALTH.ordinal()] = 0xFFFF0000;
        s[StatusEffect.LOW_PRAYER.ordinal()] = 0.3f;
        colors[StatusEffect.LOW_PRAYER.ordinal()] = 0xFF0000FF;
        int px = renderGlow(cfg, s, colors, 20).getRGB(200, 0);
        Assert.assertTrue("red dominant", ((px >>> 16) & 0xFF) > 200);
        Assert.assertTrue("no blue mixed in", (px & 0xFF) < 10);
    }

    @Test
    public void testProceduralRendererAllShowerEdgesAndCombinedGlow()
    {
        for (ShowerStartLocation loc : ShowerStartLocation.values())
        {
            ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig()
            {
                @Override
                public ShowerStartLocation showerStartLocation() { return loc; }

                @Override
                public int showerScreenPercent() { return 75; }

                @Override
                public boolean debugLowHealth() { return true; }

                @Override
                public boolean debugLowPrayer() { return true; }

                @Override
                public boolean debugFreeze() { return true; }
            };

            ProceduralRenderer renderer = new ProceduralRenderer();
            BufferedImage canvas = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();

            float[] severities = new float[StatusEffect.values().length];
            int[] colors = new int[StatusEffect.values().length];
            severities[StatusEffect.LOW_HEALTH.ordinal()] = 0.8f;
            colors[StatusEffect.LOW_HEALTH.ordinal()] = cfg.lowHealthColor().getRGB();
            severities[StatusEffect.LOW_PRAYER.ordinal()] = 0.7f;
            colors[StatusEffect.LOW_PRAYER.ordinal()] = cfg.lowPrayerColor().getRGB();
            severities[StatusEffect.FREEZE.ordinal()] = 0.6f;
            colors[StatusEffect.FREEZE.ordinal()] = cfg.freezeColor().getRGB();
            severities[StatusEffect.IDLE.ordinal()] = 0.5f;
            colors[StatusEffect.IDLE.ordinal()] = cfg.idleColor().getRGB();
            severities[StatusEffect.SPEC_READY.ordinal()] = 0.65f;
            colors[StatusEffect.SPEC_READY.ordinal()] = cfg.specColor().getRGB();

            int[] spriteIds = new int[]{
                CombatSpell.RESURRECT_THRALL.getFallbackSpriteId(),
                CombatSpell.DEATH_CHARGE.getFallbackSpriteId(),
                328,
                201
            };
            int[] spriteCols = new int[]{
                cfg.spellThrallColor().getRGB(),
                cfg.spellDeathChargeColor().getRGB(),
                cfg.freezeColor().getRGB(),
                cfg.lowPrayerColor().getRGB()
            };
            int[] itemIds = new int[]{HeartCooldownTracker.SATURATED_HEART_ID, 23685};
            int[] itemCols = new int[]{cfg.heartReminderColor().getRGB(), cfg.buffReminderColor().getRGB()};

            renderer.render(
                g, 400, 300, System.nanoTime(), com.immersivecombatoverlays.settings.SettingsSnapshot.of(cfg),
                null, null, null,
                severities, colors,
                spriteIds.length, spriteCols,
                spriteIds.length, spriteIds, spriteCols,
                itemIds.length, itemIds, itemCols
            );
            g.dispose();

            int edgeAlpha = (canvas.getRGB(2, 2) >>> 24) & 0xFF;
            Assert.assertTrue("Expected edge glow alpha > 0 for " + loc, edgeAlpha > 0);
            int rightEdgeAlpha = (canvas.getRGB(397, 150) >>> 24) & 0xFF;
            Assert.assertTrue("Expected right edge glow alpha > 0 for " + loc, rightEdgeAlpha > 0);
        }
    }

    @Test
    public void testTightCropIconScalingUniformSize()
    {
        // 36x32 padded sprite with only a 12x12 opaque square in the center
        BufferedImage padded = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = padded.createGraphics();
        pg.setColor(Color.MAGENTA);
        pg.fillRect(12, 10, 12, 12);
        pg.dispose();

        BufferedImage scaled = ProceduralRenderer.scalePreserveAspect(padded, 26);
        Assert.assertEquals(26, scaled.getWidth());
        Assert.assertEquals(26, scaled.getHeight());
        // Pixel at (2, 13) should be opaque after tight-cropping and scaling to 24x24
        int alpha = (scaled.getRGB(2, 13) >>> 24) & 0xFF;
        Assert.assertTrue("Expected tight-cropped artwork to fill target box", alpha > 200);
    }

    @Test
    public void testConfigMappingsAndProxySafety()
    {
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig() {};
        Assert.assertEquals(9, CombatSpell.values().length);
        for (CombatSpell spell : CombatSpell.values())
        {
            Assert.assertNotNull(com.immersivecombatoverlays.settings.SettingsSnapshot.of(cfg).spellAwtColor(spell.ordinal()));
            Assert.assertFalse(com.immersivecombatoverlays.settings.SettingsSnapshot.of(cfg).spellDebug(spell.ordinal()));
            Assert.assertTrue(com.immersivecombatoverlays.settings.SettingsSnapshot.of(cfg).spellGlow(spell.ordinal()));
            Assert.assertTrue(com.immersivecombatoverlays.settings.SettingsSnapshot.of(cfg).spellShower(spell.ordinal()));
            Assert.assertTrue(com.immersivecombatoverlays.settings.SettingsSnapshot.of(cfg).spellbookGlow(spell.ordinal()));
        }

        Set<String> sections = new HashSet<>();
        for (Field f : ImmersiveCombatOverlaysConfig.class.getDeclaredFields())
        {
            if (f.isAnnotationPresent(ConfigSection.class))
            {
                try
                {
                    sections.add((String) f.get(null));
                }
                catch (IllegalAccessException e)
                {
                    throw new AssertionError(e);
                }
            }
        }

        Set<String> keys = new HashSet<>();
        Map<String, String> positions = new HashMap<>();
        for (Method m : ImmersiveCombatOverlaysConfig.class.getDeclaredMethods())
        {
            if (Modifier.isStatic(m.getModifiers()))
            {
                continue;
            }
            ConfigItem item = m.getAnnotation(ConfigItem.class);
            Assert.assertNotNull("Config method missing @ConfigItem: " + m.getName(), item);
            Assert.assertTrue("Duplicate keyName " + item.keyName(), keys.add(item.keyName()));
            Assert.assertEquals("keyName should match method name", m.getName(), item.keyName());
            Assert.assertFalse("Empty description: " + item.keyName(), item.description().isEmpty());
            Assert.assertTrue("Unknown section for " + item.keyName(), item.section().isEmpty() || sections.contains(item.section()));
            String slot = item.section() + "#" + item.position();
            String clash = positions.put(slot, item.keyName());
            Assert.assertNull("Position clash in " + item.section() + ": " + clash + " / " + item.keyName(), clash);
        }
    }

    @Test
    public void globalAllowTogglesGateEveryEffect()
    {
        ImmersiveCombatOverlaysConfig noGlow = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean allowScreenGlows() { return false; }
        };
        ImmersiveCombatOverlaysConfig noIcons = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean allowIconShowers() { return false; }
        };
        ImmersiveCombatOverlaysConfig noItemGlow = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean allowItemGlows() { return false; }
        };
        for (CombatSpell spell : CombatSpell.values())
        {
            Assert.assertFalse(com.immersivecombatoverlays.settings.SettingsSnapshot.of(noGlow).spellGlow(spell.ordinal()));
            Assert.assertTrue("glow off must not disable icons", com.immersivecombatoverlays.settings.SettingsSnapshot.of(noGlow).spellShower(spell.ordinal()));
            Assert.assertFalse(com.immersivecombatoverlays.settings.SettingsSnapshot.of(noIcons).spellShower(spell.ordinal()));
            Assert.assertTrue("icons off must not disable glow", com.immersivecombatoverlays.settings.SettingsSnapshot.of(noIcons).spellGlow(spell.ordinal()));
            Assert.assertFalse(com.immersivecombatoverlays.settings.SettingsSnapshot.of(noItemGlow).spellbookGlow(spell.ordinal()));
        }

        // Every spell has its own glow/shower/spellbook toggle (previously 4 spells were hard-wired on)
        ImmersiveCombatOverlaysConfig veilOff = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public boolean spellShadowVeilGlow() { return false; }

            @Override
            public boolean spellCorruptionShower() { return false; }

            @Override
            public boolean spellChargeSpellbook() { return false; }

            @Override
            public boolean spellVengeanceOtherGlow() { return false; }
        };
        Assert.assertFalse(com.immersivecombatoverlays.settings.SettingsSnapshot.of(veilOff).spellGlow(CombatSpell.SHADOW_VEIL.ordinal()));
        Assert.assertFalse(com.immersivecombatoverlays.settings.SettingsSnapshot.of(veilOff).spellShower(CombatSpell.CORRUPTION.ordinal()));
        Assert.assertFalse(com.immersivecombatoverlays.settings.SettingsSnapshot.of(veilOff).spellbookGlow(CombatSpell.CHARGE.ordinal()));
        Assert.assertFalse(com.immersivecombatoverlays.settings.SettingsSnapshot.of(veilOff).spellGlow(CombatSpell.VENGEANCE_OTHER.ordinal()));
        Assert.assertTrue(com.immersivecombatoverlays.settings.SettingsSnapshot.of(veilOff).spellShower(CombatSpell.SHADOW_VEIL.ordinal()));

        // With zero master intensity the renderer draws nothing at the screen edge
        int[] colors = new int[StatusEffect.values().length];
        colors[StatusEffect.LOW_HEALTH.ordinal()] = 0xFFFF0000;
        ImmersiveCombatOverlaysConfig zeroIntensity = new ImmersiveCombatOverlaysConfig()
        {
            @Override
            public int effectIntensity() { return 0; }
        };
        Assert.assertEquals(0, (renderGlow(zeroIntensity, sev(StatusEffect.LOW_HEALTH, 1f), colors, 5).getRGB(0, 150) >>> 24) & 0xFF);
    }

    @Test
    public void testCooldownRendererAllBorderStylesAndSingletons() throws Exception
    {
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig() {};
        CooldownRenderer cr = new CooldownRenderer();
        BufferedImage canvas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        Rectangle bounds = new Rectangle(10, 10, 32, 32);

        cr.renderItemCooldown(g, bounds, 23685, Cooldown.active(10, 20), cfg, null, System.nanoTime(), 0f);
        cr.renderItemCooldown(g, bounds, 23685, Cooldown.ready(), cfg, null, System.nanoTime(), 1f);
        BufferedImage sprite = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 5; y < 11; y++)
        {
            for (int x = 5; x < 11; x++)
            {
                sprite.setRGB(x, y, 0xFFFFFFFF);
            }
        }
        BufferedImage outline = ItemOutlineCache.buildOutline(sprite, Color.CYAN);
        BufferedImage aura = ItemOutlineCache.buildOutwardAura(sprite, Color.CYAN, ItemOutlineCache.AURA_PAD);
        Assert.assertEquals(0, (outline.getRGB(8, 8) >>> 24) & 0xFF);
        Assert.assertTrue(((outline.getRGB(4, 8) >>> 24) & 0xFF) > 0);
        Assert.assertEquals(0, (aura.getRGB(8 + ItemOutlineCache.AURA_PAD, 8 + ItemOutlineCache.AURA_PAD) >>> 24) & 0xFF);
        Assert.assertTrue(((aura.getRGB(3 + ItemOutlineCache.AURA_PAD, 8 + ItemOutlineCache.AURA_PAD) >>> 24) & 0xFF) > 0);

        for (BorderStyle bs : BorderStyle.values())
        {
            CooldownRenderer.drawStyledGlow(g, bounds, outline, aura, ItemOutlineCache.AURA_PAD, cfg.readyColor(), bs, 0.8, System.nanoTime());
        }
        g.dispose();

        Assert.assertNotNull(SpellCooldownOverlay.class.getDeclaredMethod("renderDebugSpellGrid", Graphics2D.class, double.class));
        Assert.assertTrue(ImmersiveStatusOverlay.class.isAnnotationPresent(Singleton.class));
        Assert.assertTrue(InventoryCooldownOverlay.class.isAnnotationPresent(Singleton.class));
        Assert.assertTrue(SpellCooldownOverlay.class.isAnnotationPresent(Singleton.class));
    }
}
