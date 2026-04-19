package top.ourisland.angellonotifier.config;

import java.util.Set;

public record PluginConfig(
        String lang,
        int inboxPageSize,
        int joinPreviewBatchSize,
        int defaultNotificationLifetime,
        Set<String> ignoredServers,
        boolean playSound,
        String soundName,
        float soundVolume,
        float soundPitch
) {

}
