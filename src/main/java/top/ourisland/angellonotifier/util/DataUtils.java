package top.ourisland.angellonotifier.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DataUtils {

    private DataUtils() {
    }

    public static Map<String, Object> castMap(Map<?, ?> input) {
        return castToMap(input, String::valueOf, v -> v);
    }

    public static <K, V> Map<K, V> castToMap(
            Object value,
            Function<Object, K> keyMapper,
            Function<Object, V> valMapper
    ) {
        if (value instanceof Map<?, ?> input) {
            Map<K, V> result = new LinkedHashMap<>();
            input.forEach((k, v) -> result.put(keyMapper.apply(k), valMapper.apply(v)));
            return result;
        }
        return new LinkedHashMap<>();
    }

    public static Map<String, Object> map(Object value) {
        return castToMap(value, String::valueOf, v -> v);
    }

    public static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public static String string(Object value, String fallback) {
        return convert(value, fallback, String::valueOf);
    }

    public static <T> T convert(
            Object value,
            T fallback,
            Function<Object, T> converter
    ) {
        if (value == null) return fallback;
        try {
            return converter.apply(value);
        } catch (Exception _) {
            return fallback;
        }
    }

    public static int integer(Object value, int fallback) {
        return convert(
                value,
                fallback,
                o -> (o instanceof Number n) ? n.intValue() : Integer.parseInt(o.toString())
        );
    }

    public static boolean bool(Object value, boolean fallback) {
        return convert(
                value,
                fallback,
                o -> (o instanceof Boolean b) ? b : Boolean.parseBoolean(o.toString())
        );
    }

    public static float floating(Object value, float fallback) {
        return convert(
                value,
                fallback,
                o -> (o instanceof Number n) ? n.floatValue() : Float.parseFloat(o.toString())
        );
    }

}
