package com.immersivecombatoverlays;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ImmersiveCombatOverlaysPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(ImmersiveCombatOverlaysPlugin.class);
        RuneLite.main(args);
    }
}
