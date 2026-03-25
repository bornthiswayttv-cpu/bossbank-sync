package com.bossbank.sync;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BossBankSyncPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(BossBankSyncPlugin.class);
        RuneLite.main(args);
    }
}
