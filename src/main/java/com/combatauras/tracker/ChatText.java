package com.combatauras.tracker;

/**
 * Normalises game chat text before it is matched against known messages.
 * <p>
 * The game colours messages two ways: {@code <col=ef0083>...</col>} tags, and (since August 2026) highlight macros
 * such as {@code @mes_hl_mag@} at the start of the message. Both are dropped so matching sees only the words.
 */
public final class ChatText
{
    private ChatText()
    {
    }

    public static String clean(String s)
    {
        if (s == null || (s.indexOf('<') < 0 && s.indexOf('@') < 0))
        {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int n = s.length();
        int i = 0;
        while (i < n)
        {
            char c = s.charAt(i);
            if (c == '<')
            {
                int close = s.indexOf('>', i + 1);
                if (close < 0)
                {
                    break;
                }
                i = close + 1;
                continue;
            }
            if (c == '@')
            {
                int close = macroEnd(s, i);
                if (close > 0)
                {
                    i = close + 1;
                    continue;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /** @return index of the closing '@' of a macro like {@code @mes_hl_mag@} starting at {@code at}, or -1 */
    private static int macroEnd(String s, int at)
    {
        int n = s.length();
        for (int j = at + 1; j < n && j - at <= 24; j++)
        {
            char c = s.charAt(j);
            if (c == '@')
            {
                return j > at + 1 ? j : -1;
            }
            if (!(c == '_' || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')))
            {
                return -1;
            }
        }
        return -1;
    }
}
