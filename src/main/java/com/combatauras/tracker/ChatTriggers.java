package com.combatauras.tracker;

import com.combatauras.model.MessageSource;
import com.combatauras.settings.SettingsSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;

/**
 * The three "Message" sections: a chat line matching one of a section's phrases lights its screen glow
 * for the section's duration. Phrases are comma-separated, case-insensitive, match anywhere in the line,
 * and {@code *} matches any text (like RuneLite's Chat Notifications).
 */
@Singleton
public class ChatTriggers
{
    public static final int SLOTS = 3;
    private static final Pattern[] NONE = new Pattern[0];
    // Fade out over the last part of the duration instead of snapping off
    private static final long FADE_OUT_NANOS = 400_000_000L;

    private final long[] lastMatchNanos = new long[SLOTS];

    public void reset()
    {
        java.util.Arrays.fill(lastMatchNanos, 0L);
    }

    /** @param message chat text with tags and macros already removed ({@link ChatText#clean}) */
    public void onChatMessage(ChatMessageType type, String message, SettingsSnapshot s, long nowNanos)
    {
        MessageSource source = MessageSource.of(type);
        if (source == null || message == null || message.isEmpty())
        {
            return;
        }
        for (int i = 0; i < SLOTS; i++)
        {
            if (s.messageEnabled(i) && (s.messageSourceMask(i) & source.bit()) != 0 && matches(s.messagePatterns(i), message))
            {
                lastMatchNanos[i] = nowNanos;
            }
        }
    }

    /** 1 while the section's glow is up (its duration after the last match), easing out at the end; else 0. */
    public float getSeverity(int slot, SettingsSnapshot s, long nowNanos)
    {
        long since = lastMatchNanos[slot];
        if (since == 0L)
        {
            return 0f;
        }
        long duration = s.messageDurationNanos(slot);
        long left = since + duration - nowNanos;
        if (left <= 0L)
        {
            lastMatchNanos[slot] = 0L;
            return 0f;
        }
        long fade = Math.min(FADE_OUT_NANOS, duration / 2);
        return left >= fade ? 1f : (float) left / (float) fade;
    }

    static boolean matches(Pattern[] patterns, String message)
    {
        for (Pattern p : patterns)
        {
            if (p.matcher(message).find())
            {
                return true;
            }
        }
        return false;
    }

    /** Parses a section's comma-separated phrase list; blank entries are skipped. */
    public static Pattern[] compile(String phrases)
    {
        if (phrases == null || phrases.trim().isEmpty())
        {
            return NONE;
        }
        List<Pattern> out = new ArrayList<>();
        for (String raw : phrases.split(","))
        {
            String phrase = raw.trim();
            if (phrase.isEmpty() || phrase.replace("*", "").trim().isEmpty())
            {
                continue; // "*" alone would match every line
            }
            StringBuilder regex = new StringBuilder();
            String[] parts = phrase.split("\\*", -1);
            for (int i = 0; i < parts.length; i++)
            {
                if (i > 0)
                {
                    regex.append(".*");
                }
                regex.append(Pattern.quote(parts[i]));
            }
            out.add(Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        }
        return out.toArray(NONE);
    }

    /** Test hook. */
    public static boolean matchesForTest(String phrases, String message)
    {
        return matches(compile(phrases), message);
    }
}
