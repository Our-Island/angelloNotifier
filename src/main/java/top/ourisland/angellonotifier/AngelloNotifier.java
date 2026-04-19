package top.ourisland.angellonotifier;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

@Plugin(
        id = "angello-notifier",
        name = "angelloNotifier",
        version = "0.1.0-SNAPSHOT",
        description = "A velocity announcement and notification sending and managing plugin.",
        url = "https://github.com/Our-Island/angelloNotifier",
        authors = {"Our-Island", "Chiloven945"}
)
public class AngelloNotifier {

    @Inject private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Plugin initialization logic goes here
        logger.info("angelloNotifier loaded");
    }

}
