package ru.javaroot.plat.txt;

import java.util.ArrayList;
import java.util.List;

public final class TimeFmt {
    private TimeFmt() {
    }

    public static String replace(String template, long totalSec, Units units) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        long clamped = Math.max(0L, totalSec);
        long days = clamped / 86400;
        long hours = clamped % 86400 / 3600;
        long minutes = clamped % 3600 / 60;
        long seconds = clamped % 60;
        return Txt.apply(
            template,
            "%days%", String.valueOf(days),
            "%hours%", String.valueOf(hours),
            "%minutes%", String.valueOf(minutes),
            "%seconds%", String.valueOf(seconds),
            "%time%", format(clamped, units)
        );
    }

    public static String format(long totalSec, Units units) {
        long clamped = Math.max(0L, totalSec);
        long days = clamped / 86400;
        long hours = clamped % 86400 / 3600;
        long minutes = clamped % 3600 / 60;
        long seconds = clamped % 60;

        List<String> parts = new ArrayList<>(4);
        if (days > 0) {
            parts.add(days + " " + pick(days, units.day(), units.dayFew(), units.days()));
        }
        if (hours > 0) {
            parts.add(hours + " " + pick(hours, units.hour(), units.hourFew(), units.hours()));
        }
        if (minutes > 0) {
            parts.add(minutes + " " + pick(minutes, units.minute(), units.minuteFew(), units.minutes()));
        }
        if (seconds > 0 || parts.isEmpty()) {
            parts.add(seconds + " " + pick(seconds, units.second(), units.secondFew(), units.seconds()));
        }

        if (parts.size() == 1) {
            return parts.getFirst();
        }
        String splitter = units.splitter() == null ? "и " : units.splitter();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                out.append(i == parts.size() - 1 ? " " + splitter : ", ");
            }
            out.append(parts.get(i));
        }
        return out.toString().trim();
    }

    private static String pick(long val, String one, String few, String many) {
        long mod100 = val % 100;
        long mod10 = val % 10;
        if (mod10 == 1 && mod100 != 11) {
            return one;
        }
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
            return few != null && !few.isEmpty() ? few : many;
        }
        return many;
    }

    public record Units(
        String day, String dayFew, String days,
        String hour, String hourFew, String hours,
        String minute, String minuteFew, String minutes,
        String second, String secondFew, String seconds,
        String splitter
    ) {
    }
}
