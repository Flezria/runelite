package net.runelite.client.plugins.realtimetracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("realtimetracker")
public interface RealtimeTrackerConfig extends Config {

    @ConfigItem(
            keyName = "apiKey",
            name = "Api key",
            description = "Api key for OSRS Realtime Tracker",
            position = 1
    )
    default String apiKey()
    {
        return "";
    }
}
