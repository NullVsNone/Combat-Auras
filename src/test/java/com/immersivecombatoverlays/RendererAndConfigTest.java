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
            renderer.render(g, 400, 300, t, cfg, null, null, null, null, severities, colors,
                0, new int[0], 0, new int[0], new int[0], 0, new int[0], new int[0]);
            g.dispose();
            t += 50_000_000L;
        }
        return canvas;
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
                g, 400, 300, System.nanoTime(), cfg,
                null, null, null, null,
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
            Assert.assertNotNull(ImmersiveCombatOverlaysConfig.getSpellColor(cfg, spell));
            Assert.assertFalse(ImmersiveCombatOverlaysConfig.isDebugSpellActive(cfg, spell));
            Assert.assertTrue(ImmersiveCombatOverlaysConfig.isSpellGlowEnabled(cfg, spell));
            Assert.assertTrue(ImmersiveCombatOverlaysConfig.isSpellShowerEnabled(cfg, spell));
            Assert.assertTrue(ImmersiveCombatOverlaysConfig.isSpellbookGlowEnabled(cfg, spell));
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
            Assert.assertFalse(ImmersiveCombatOverlaysConfig.isSpellGlowEnabled(noGlow, spell));
            Assert.assertTrue("glow off must not disable icons", ImmersiveCombatOverlaysConfig.isSpellShowerEnabled(noGlow, spell));
            Assert.assertFalse(ImmersiveCombatOverlaysConfig.isSpellShowerEnabled(noIcons, spell));
            Assert.assertTrue("icons off must not disable glow", ImmersiveCombatOverlaysConfig.isSpellGlowEnabled(noIcons, spell));
            Assert.assertFalse(ImmersiveCombatOverlaysConfig.isSpellbookGlowEnabled(noItemGlow, spell));
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
        Assert.assertFalse(ImmersiveCombatOverlaysConfig.isSpellGlowEnabled(veilOff, CombatSpell.SHADOW_VEIL));
        Assert.assertFalse(ImmersiveCombatOverlaysConfig.isSpellShowerEnabled(veilOff, CombatSpell.CORRUPTION));
        Assert.assertFalse(ImmersiveCombatOverlaysConfig.isSpellbookGlowEnabled(veilOff, CombatSpell.CHARGE));
        Assert.assertFalse(ImmersiveCombatOverlaysConfig.isSpellGlowEnabled(veilOff, CombatSpell.VENGEANCE_OTHER));
        Assert.assertTrue(ImmersiveCombatOverlaysConfig.isSpellShowerEnabled(veilOff, CombatSpell.SHADOW_VEIL));

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
