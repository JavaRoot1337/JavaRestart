package ru.javaroot.util;

import ru.javaroot.cfg.MsgCfg;

public class TimeUtil {
    private TimeUtil() {
    }

    public static String replaceTime(String message, long totalSeconds, MsgCfg msgCfg) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        long days = totalSeconds / 86400;
        long hours = totalSeconds % 86400 / 3600;
        long minutes = totalSeconds % 3600 / 60;
        long seconds = totalSeconds % 60;
        return message
            .replace("%days%", String.valueOf(days))
            .replace("%hours%", String.valueOf(hours))
            .replace("%minutes%", String.valueOf(minutes))
            .replace("%seconds%", String.valueOf(seconds))
            .replace("%time%", formatTime(totalSeconds, msgCfg));
    }

    public static String formatTime(long totalSeconds, MsgCfg msgCfg) {
        long days = totalSeconds / 86400;
        long hours = totalSeconds % 86400 / 3600;
        long minutes = totalSeconds % 3600 / 60;
        long seconds = totalSeconds % 60;
        String splitter = msgCfg.format("splitter");

        StringBuilder text = new StringBuilder();
        boolean hasValue = false;
        if (days > 0) {
            append(text, days, unit(days, msgCfg, "days", "day"), splitter, hasValue);
            hasValue = true;
        }
        if (hours > 0) {
            append(text, hours, unit(hours, msgCfg, "hours", "hour"), splitter, hasValue);
            hasValue = true;
        }
        if (minutes > 0) {
            append(text, minutes, unit(minutes, msgCfg, "minutes", "minute"), splitter, hasValue);
            hasValue = true;
        }
        if (seconds > 0 || !hasValue) {
            append(text, seconds, unit(seconds, msgCfg, "seconds", "second"), splitter, hasValue);
        }
        return text.toString().trim();
    }

    private static void append(StringBuilder text, long value, String unit, String splitter, boolean hasValue) {
        if (hasValue) {
            text.append(' ').append(splitter);
        }
        text.append(value).append(' ').append(unit);
    }

    private static String unit(long value, MsgCfg msgCfg, String plural, String singular) {
        return value == 1 ? msgCfg.format(singular) : msgCfg.format(plural);
    }
}
