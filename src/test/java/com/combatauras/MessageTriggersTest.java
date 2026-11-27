package com.combatauras;

import com.combatauras.config.CombatAurasConfig;
import com.combatauras.model.MessageSource;
import com.combatauras.model.StatusEffect;
import com.combatauras.settings.Bindings;
import com.combatauras.settings.SettingsSnapshot;
import com.combatauras.tracker.ChatTriggers;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.ChatMessageType;
import org.junit.Assert;
import org.junit.Test;

public class MessageTriggersTest
{
    private static final long SEC = 1_000_000_000L;

    @Test
    public void phrasesAreCommaSeparatedCaseInsensitiveWithWildcards()
    {
        String phrases = "You have been poisoned, the *ground shakes*,  ";
        Assert.assertTrue(ChatTriggers.matchesForTest(phrases, "You have been poisoned!"));
        Assert.assertTrue(ChatTriggers.matchesForTest(phrases, "YOU HAVE BEEN POISONED"));
        Assert.assertTrue(ChatTriggers.matchesForTest(phrases, "Suddenly the cave ground shakes violently."));
        Assert.assertFalse(ChatTriggers.matchesForTest(phrases, "The ground is quiet."));
        Assert.assertFalse("regex characters are literal", ChatTriggers.matchesForTest("a.b", "axb"));
        Assert.assertFalse("blank list matches nothing", ChatTriggers.matchesForTest("", "anything"));
        Assert.assertFalse("a lone * would match every line, so it is ignored", ChatTriggers.matchesForTest("*", "anything"));
    }

    @Test
    public void chatTypesMapToTheirTabs()
    {
        Assert.assertEquals(MessageSource.GAME, MessageSource.of(ChatMessageType.GAMEMESSAGE));
        Assert.assertEquals(MessageSource.GAME, MessageSource.of(ChatMessageType.SPAM));
        Assert.assertEquals(MessageSource.PUBLIC, MessageSource.of(ChatMessageType.PUBLICCHAT));
        Assert.assertEquals(MessageSource.CLAN, MessageSource.of(ChatMessageType.CLAN_CHAT));
        Assert.assertEquals(MessageSource.FRIENDS_CHAT, MessageSource.of(ChatMessageType.FRIENDSCHAT));
        Assert.assertEquals(MessageSource.NPC, MessageSource.of(ChatMessageType.NPC_SAY));
        Assert.assertNull("your own outgoing PMs never trigger", MessageSource.of(ChatMessageType.PRIVATECHATOUT));
    }

    @Test
    public void matchingMessageGlowsForItsDurationOnItsChosenChannelsOnly()
    {
        CombatAurasConfig cfg = new CombatAurasConfig()
        {
            @Override
            public boolean message2Enabled() { return true; }

            @Override
            public String message2Text() { return "special attack, *olm*"; }

            @Override
            public Set<MessageSource> message2Sources() { return EnumSet.of(MessageSource.CLAN, MessageSource.GAME); }

            @Override
            public int message2DurationSeconds() { return 3; }
        };
        SettingsSnapshot s = SettingsSnapshot.of(cfg);
        ChatTriggers triggers = new ChatTriggers();
        long t = 50 * SEC;

        triggers.onChatMessage(ChatMessageType.PUBLICCHAT, "spec special attack now", s, t);
        Assert.assertEquals("public chat is not one of its channels", 0f, triggers.getSeverity(1, s, t), 0f);

        triggers.onChatMessage(ChatMessageType.CLAN_CHAT, "Great Olm is up", s, t);
        Assert.assertEquals(1f, triggers.getSeverity(1, s, t + SEC), 0f);
        Assert.assertEquals("other sections stay off", 0f, triggers.getSeverity(0, s, t + SEC), 0f);
        Assert.assertTrue("eases out at the end", triggers.getSeverity(1, s, t + 3 * SEC - 100_000_000L) < 1f);
        Assert.assertEquals(0f, triggers.getSeverity(1, s, t + 3 * SEC), 0f);

        triggers.onChatMessage(ChatMessageType.GAMEMESSAGE, "Your special attack is ready", s, t + 10 * SEC);
        triggers.onChatMessage(ChatMessageType.GAMEMESSAGE, "Your special attack is ready", s, t + 12 * SEC);
        Assert.assertEquals("a repeat match restarts the duration", 1f, triggers.getSeverity(1, s, t + 14 * SEC), 0f);

        // Disabled sections never fire, whatever their text
        ChatTriggers off = new ChatTriggers();
        off.onChatMessage(ChatMessageType.GAMEMESSAGE, "special attack", SettingsSnapshot.of(new CombatAurasConfig() { }), t);
        Assert.assertEquals(0f, off.getSeverity(1, s, t), 0f);
    }

    @Test
    public void messageSectionsAreScreenGlowsWithoutShowers()
    {
        CombatAurasConfig cfg = new CombatAurasConfig() { };
        SettingsSnapshot s = SettingsSnapshot.of(cfg);
        for (StatusEffect e : new StatusEffect[]{StatusEffect.MESSAGE_1, StatusEffect.MESSAGE_2, StatusEffect.MESSAGE_3})
        {
            Assert.assertNotNull(Bindings.STATUSES.get(e));
            Assert.assertFalse(s.statusShower(e.ordinal()));
            Assert.assertFalse("off by default", s.statusEnabled(e.ordinal()));
        }
        Assert.assertEquals(EnumSet.of(MessageSource.GAME), cfg.message1Sources());
    }
}
