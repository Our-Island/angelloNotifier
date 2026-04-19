package top.ourisland.angellonotifier.util;

import org.slf4j.Logger;
import top.ourisland.angellonotifier.config.ConfigManager;

import java.util.*;
import java.util.stream.Collectors;

public class I18n {

    static ResourceBundle bundle;
    static ResourceBundle fallbackBundle;
    static Logger logger;
    static ConfigManager configManager;

    private I18n() {
    }

    public static void init(ConfigManager configManager, Logger logger) {
        I18n.configManager = configManager;
        I18n.logger = logger;
        reload();
    }

    public static void reload() {
        fallbackBundle = loadBundle("en_us", null);
        String lang = configManager != null ? configManager.config().lang() : "en_us";
        bundle = loadBundle(lang, fallbackBundle);
    }

    static ResourceBundle loadBundle(String lang, ResourceBundle fallback) {
        try {
            return ResourceBundle.getBundle("lang." + lang);
        } catch (Exception ex) {
            if (logger != null) {
                logger.warn("Failed to load language '{}', fallback to en_us", lang, ex);
            }
            if (fallback != null) {
                return fallback;
            }
            try {
                return ResourceBundle.getBundle("lang.en_us");
            } catch (Exception e) {
                throw new IllegalStateException("Unable to load fallback language bundle lang.en_us", e);
            }
        }
    }

    public static List<String> helpLines() {
        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= 99; i++) {
            String key = "command.help." + i;
            if (!contains(key)) {
                break;
            }
            lines.add(langNP(key));
        }
        return lines;
    }

    static boolean contains(String key) {
        return bundle != null && bundle.containsKey(key)
                || fallbackBundle != null && fallbackBundle.containsKey(key);
    }

    public static String langNP(String key, Object... args) {
        String text = pattern(key);
        if (args == null || args.length == 0) {
            return text;
        }
        for (int i = 0; i < args.length; i++) {
            String replacement = args[i] == null ? "null" : String.valueOf(args[i]);
            text = text.replace("{" + i + "}", replacement);
        }
        return text;
    }

    static String pattern(String key) {
        try {
            if (bundle != null && bundle.containsKey(key)) {
                return bundle.getString(key);
            }
            if (fallbackBundle != null && fallbackBundle.containsKey(key)) {
                return fallbackBundle.getString(key);
            }
        } catch (MissingResourceException _) {
        }
        return "!" + key + "!";
    }

    public static String block(String... lines) {
        List<String> filtered = new ArrayList<>();
        if (lines != null) filtered = Arrays.stream(lines)
                .filter(line -> line != null && !line.isEmpty())
                .collect(Collectors.toList());
        return block(filtered);
    }

    public static String block(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return withPrefix("");
        }

        List<String> filtered = new ArrayList<>();
        for (String line : lines) {
            if (line != null && !line.isEmpty()) {
                filtered.add(line);
            }
        }

        if (filtered.isEmpty()) {
            return withPrefix("");
        }
        if (filtered.size() == 1) {
            return withPrefix(filtered.getFirst());
        }
        return prefixLine() + "\n" + String.join("\n", filtered);
    }

    public static String withPrefix(String text) {
        return prefix() + (text == null ? "" : text);
    }

    static String prefixLine() {
        return pattern("angello.prefix");
    }

    static String prefix() {
        return pattern("angello.prefix");
    }

    public static String lang(String key, Object... args) {
        return prefix() + langNP(key, args);
    }

}
