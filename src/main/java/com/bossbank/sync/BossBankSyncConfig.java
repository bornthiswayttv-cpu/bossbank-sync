package com.bossbank.sync;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("bossbanksync")
public interface BossBankSyncConfig extends Config
{
    @ConfigItem(
        keyName = "apiBaseUrl",
        name = "API Base URL",
        description = "BossBank API base URL, e.g. https://yourdomain.com"
    )
    default String apiBaseUrl()
    {
        return "http://localhost:3000";
    }

    @ConfigItem(
        keyName = "apiKey",
        name = "API Key",
        description = "Bearer token or API key for BossBank"
    )
    default String apiKey()
    {
        return "";
    }

    @ConfigItem(
        keyName = "autoSyncOnLogin",
        name = "Auto sync on login",
        description = "Automatically sync bank, equipment, inventory, and stats when you log in"
    )
    default boolean autoSyncOnLogin()
    {
        return false;
    }

    @ConfigItem(
        keyName = "autoSyncOnBankOpen",
        name = "Auto sync on bank open",
        description = "Automatically sync when your bank is opened"
    )
    default boolean autoSyncOnBankOpen()
    {
        return false;
    }

    @ConfigItem(
        keyName = "includeInventory",
        name = "Include inventory",
        description = "Include inventory data in the bank sync payload"
    )
    default boolean includeInventory()
    {
        return true;
    }

    @ConfigItem(
        keyName = "debugLogging",
        name = "Debug logging",
        description = "Print sync details to the RuneLite log"
    )
    default boolean debugLogging()
    {
        return false;
    }
}
