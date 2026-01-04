package ru.javaroot.jrestart.utils;

import ru.javaroot.jrestart.MessagesConfig;

public class TimeUtils {

    public static String replaceTime(String message, long totalSeconds, MessagesConfig config) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return message
                .replace("%days%", String.valueOf(days))
                .replace("%hours%", String.valueOf(hours))
                .replace("%minutes%", String.valueOf(minutes))
                .replace("%seconds%", String.valueOf(seconds))
                .replace("%time%", formatTime(totalSeconds, config)); // Keep support for smart format just in case
    }

    public static String formatTime(long totalSeconds, MessagesConfig messagesConfig) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        String splitter = messagesConfig.getRawMsg("format.splitter", " ");

        boolean added = false;
        if (days > 0) {
            sb.append(days).append(" ").append(getUnit(days, messagesConfig, "days", "day"));
            added = true;
        }
        if (hours > 0) {
            if (added)
                sb.append(" ").append(splitter);
            sb.append(hours).append(" ").append(getUnit(hours, messagesConfig, "hours", "hour"));
            added = true;
        }
        if (minutes > 0) {
            if (added)
                sb.append(" ").append(splitter);
            sb.append(minutes).append(" ").append(getUnit(minutes, messagesConfig, "minutes", "minute"));
            added = true;
        }
        if (seconds > 0 || !added) { // Show seconds if nothing else or if it is seconds
            if (added)
                sb.append(" ").append(splitter);
            sb.append(seconds).append(" ").append(getUnit(seconds, messagesConfig, "seconds", "second"));
        }

        return sb.toString().trim();
    }

    private static String getUnit(long value, MessagesConfig config, String pluralKey, String singularKey) {
        if (value == 1) {
            return config.getRawMsg("format." + singularKey, singularKey);
        } else {
            return config.getRawMsg("format." + pluralKey, pluralKey);
        }
    }
}
