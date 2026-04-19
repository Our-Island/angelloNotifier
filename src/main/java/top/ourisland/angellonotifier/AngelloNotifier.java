package top.ourisland.angellonotifier;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import top.ourisland.angellonotifier.command.NotifierCommand;
import top.ourisland.angellonotifier.config.ConfigManager;
import top.ourisland.angellonotifier.listener.PlayerConnectionListener;
import top.ourisland.angellonotifier.service.NotificationService;
import top.ourisland.angellonotifier.storage.DataManager;
import top.ourisland.angellonotifier.util.I18n;

import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(
        id = "angello-notifier",
        name = "angelloNotifier",
        version = "0.1.0-SNAPSHOT",
        description = "A velocity announcement and notification sending and managing plugin.",
        url = "https://github.com/Our-Island/angelloNotifier",
        authors = {"Our-Island", "Chiloven945"}
)
public class AngelloNotifier {

    ProxyServer server;
    Logger logger;
    Path dataDirectory;

    ConfigManager configManager;
    DataManager dataManager;
    NotificationService notificationService;

    @Inject
    public AngelloNotifier(
            ProxyServer server,
            Logger logger,
            @DataDirectory Path dataDirectory
    ) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            Files.createDirectories(dataDirectory);

            configManager = new ConfigManager(logger, dataDirectory.resolve("config.yml"));
            configManager.load();
            I18n.init(configManager, logger);

            dataManager = new DataManager(logger, dataDirectory.resolve("data.yml"));
            dataManager.load();

            notificationService = new NotificationService(
                    this,
                    server,
                    logger,
                    configManager,
                    dataManager
            );
            notificationService.startAutoSaveTask();

            server.getEventManager().register(this, new PlayerConnectionListener(notificationService));
            registerCommands();

            logger.info("angelloNotifier loaded. dataDir={}", dataDirectory.toAbsolutePath());
        } catch (Exception ex) {
            logger.error("Failed to initialize angelloNotifier", ex);
        }
    }

    void registerCommands() {
        var commandManager = server.getCommandManager();
        var meta = commandManager.metaBuilder("angello")
                .aliases("angellonotifier", "anno", "noticeboard")
                .plugin(this)
                .build();
        commandManager.register(meta, new NotifierCommand(notificationService));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (notificationService != null) {
            notificationService.stopAutoSaveTask();
        }
        if (dataManager != null) {
            dataManager.saveNow();
        }
    }

    public Logger logger() {
        return logger;
    }

    public ProxyServer server() {
        return server;
    }

}
