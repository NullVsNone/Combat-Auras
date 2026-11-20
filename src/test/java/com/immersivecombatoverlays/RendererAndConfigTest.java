package com.immersivecombatoverlays;

import com.immersivecombatoverlays.config.ImmersiveCombatOverlaysConfig;
import com.immersivecombatoverlays.model.BorderStyle;
import com.immersivecombatoverlays.model.CombatSpell;
import com.immersivecombatoverlays.model.Cooldown;
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

        for (Method m : ImmersiveCombatOverlaysConfig.class.getDeclaredMethods())
        {
            if (!Modifier.isStatic(m.getModifiers()))
            {
                Assert.assertTrue("Config method missing @ConfigItem: " + m.getName(), m.isAnnotationPresent(ConfigItem.class));
            }
        }
    }

    @Test
    public void testCooldownRendererAllBorderStylesAndSingletons() throws Exception
    {
        ImmersiveCombatOverlaysConfig cfg = new ImmersiveCombatOverlaysConfig() {};
        CooldownRenderer cr = new CooldownRenderer();
        BufferedImage canvas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        Rectangle bounds = new Rectangle(10, 10, 32, 32);

        cr.renderItemCooldown(g, bounds, 23685, Cooldown.active(10, 20), cfg, null, System.nanoTime());
        cr.renderItemCooldown(g, bounds, 23685, Cooldown.ready(), cfg, null, System.nanoTime());
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
