package ru.javaroot.plat.txt;

import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Txt {
    private static final Pattern HEX_SHORT = Pattern.compile("&([0-9a-fA-F]{6})");
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.builder()
        .character('&')
        .hexCharacter('#')
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build();

    private Txt() {
    }

    public static Component color(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        String normalized = HEX_SHORT.matcher(raw).replaceAll("&#$1");
        return AMPERSAND.deserialize(normalized);
    }

    public static String apply(String template, String key, String value) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        return template.replace(key, value == null ? "" : value);
    }

    public static String apply(String template, String... pairs) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        String out = template;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            out = out.replace(pairs[i], pairs[i + 1] == null ? "" : pairs[i + 1]);
        }
        return out;
    }
}
