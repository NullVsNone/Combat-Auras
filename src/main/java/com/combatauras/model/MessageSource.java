package com.combatauras.model;

import net.runelite.api.ChatMessageType;

/**
 * Chat channels a Message trigger can listen to; each groups the RuneLite chat types that show in that tab.
 */
public enum MessageSource
{
    GAME("Game messages"),
    NPC("NPC chat"),
    PUBLIC("Public chat"),
    PRIVATE("Private messages"),
    FRIENDS_CHAT("Friends chat"),
    CLAN("Clan chat"),
    GROUP_IRONMAN("Group Ironman chat"),
    TRADE("Trade requests"),
    BROADCAST("Broadcasts");

    private final String label;

    MessageSource(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }

    /** The channel a chat message belongs to, or null for types no trigger listens to (examines, your own PMs...). */
    public static MessageSource of(ChatMessageType type)
    {
        if (type == null)
        {
            return null;
        }
        switch (type)
        {
            case GAMEMESSAGE:
            case SPAM:
            case ENGINE:
            case MESBOX:
            case LEVELUPMESSAGE:
                return GAME;
            case NPC_SAY:
            case DIALOG:
                return NPC;
            case PUBLICCHAT:
            case MODCHAT:
            case AUTOTYPER:
            case MODAUTOTYPER:
                return PUBLIC;
            case PRIVATECHAT:
            case MODPRIVATECHAT:
                return PRIVATE;
            case FRIENDSCHAT:
            case FRIENDSCHATNOTIFICATION:
                return FRIENDS_CHAT;
            case CLAN_CHAT:
            case CLAN_MESSAGE:
            case CLAN_GUEST_CHAT:
            case CLAN_GUEST_MESSAGE:
                return CLAN;
            case CLAN_GIM_CHAT:
            case CLAN_GIM_MESSAGE:
                return GROUP_IRONMAN;
            case TRADEREQ:
            case TRADE:
            case CHALREQ_TRADE:
                return TRADE;
            case BROADCAST:
                return BROADCAST;
            default:
                return null;
        }
    }

    public int bit()
    {
        return 1 << ordinal();
    }
}
