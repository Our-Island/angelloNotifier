package top.ourisland.angellonotifier.config;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static top.ourisland.angellonotifier.util.DataUtils.*;

public class ConfigManager {

    Logger logger;
    Path configPath;
    volatile PluginConfig config;

    public ConfigManager(Logger logger, Path configPath) {
        this.logger = logger;
        this.configPath = configPath;
    }

    public synchronized void reload() {
        load();
    }

    public synchronized void load() {
        try {
            ensureConfigExists();

            Yaml yaml = new Yaml();
            Map<String, Object> root;
            try (InputStream inputStream = Files.newInputStream(configPath)) {
                Object loaded = yaml.load(inputStream);
                root = (loaded instanceof Map<?, ?> map)
                        ? castMap(map)
                        : new LinkedHashMap<>();
            }

            Map<String, Object> settings = map(root.get("settings"));
            Map<String, Object> sound = map(root.get("sound"));

            Set<String> ignoredServers = new LinkedHashSet<>();
            for (String server : stringList(settings.get("ignored-servers"))) {
                ignoredServers.add(server.toLowerCase(Locale.ROOT));
            }

            config = new PluginConfig(
                    string(root.get("lang"), "en_us"),
                    integer(settings.get("inbox-page-size"), 10),
                    integer(settings.get("join-preview-batch-size"), 3),
                    integer(settings.get("default-notification-lifetime"), 2),
                    ignoredServers,
                    bool(sound.get("enabled"), true),
                    string(sound.get("name"), "minecraft:block.note_block.pling"),
                    floating(sound.get("volume"), 1.0f),
                    floating(sound.get("pitch"), 1.2f)
            );
            logger.info("angelloNotifier config loaded from {}", configPath.toAbsolutePath());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load config.yml", ex);
        }
    }

    void ensureConfigExists() throws IOException {
        if (Files.exists(configPath)) {
            return;
        }
        Files.createDirectories(configPath.getParent());
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing packaged resource config.yml");
            }
            Files.copy(inputStream, configPath);
        }
    }

    public PluginConfig config() {
        return Objects.requireNonNull(config, "config not loaded");
    }

}
