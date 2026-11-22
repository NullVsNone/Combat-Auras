package com.combatauras;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CombatAurasPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(CombatAurasPlugin.class);
        RuneLite.main(args);
    }
}
