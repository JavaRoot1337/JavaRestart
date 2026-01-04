package ru.javaroot.jrestart;

import org.bukkit.configuration.file.FileConfiguration;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private final JRestart plugin;
    private List<ScheduledRestart> scheduledRestarts;

    public ConfigManager(JRestart plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        plugin.getMessagesConfig().reloadConfig();
        parseSchedules();
    }

    private void parseSchedules() {
        scheduledRestarts = new ArrayList<>();
        List<String> raw = plugin.getConfig().getStringList("restarts");
        for (String s : raw) {
            try {
                // Format: Day;HH;MM
                String[] parts = s.split(";");
                // Handle localized inputs
                String dayStr = parts[0].trim();
                int hour = Integer.parseInt(parts[1].trim());
                int minute = Integer.parseInt(parts[2].trim());

                if (dayStr.equalsIgnoreCase("Daily") || dayStr.equalsIgnoreCase("Ежедневно")
                        || dayStr.equalsIgnoreCase("Everyday") || dayStr.equalsIgnoreCase("Каждый день")
                        || dayStr.equalsIgnoreCase("Каждыйдень")) {
                    for (DayOfWeek d : DayOfWeek.values()) {
                        scheduledRestarts.add(new ScheduledRestart(d, LocalTime.of(hour, minute)));
                    }
                } else {
                    DayOfWeek day = parseDay(dayStr);
                    scheduledRestarts.add(new ScheduledRestart(day, LocalTime.of(hour, minute)));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid restart schedule: " + s + ". Use Day;HH;MM (e.g. Monday;12;00)");
            }
        }
    }

    private DayOfWeek parseDay(String day) {
        String d = day.trim().toLowerCase();
        switch (d) {
            case "monday":
            case "понедельник":
                return DayOfWeek.MONDAY;
            case "tuesday":
            case "вторник":
                return DayOfWeek.TUESDAY;
            case "wednesday":
            case "среда":
                return DayOfWeek.WEDNESDAY;
            case "thursday":
            case "четверг":
                return DayOfWeek.THURSDAY;
            case "friday":
            case "пятница":
                return DayOfWeek.FRIDAY;
            case "saturday":
            case "суббота":
                return DayOfWeek.SATURDAY;
            case "sunday":
            case "воскресенье":
                return DayOfWeek.SUNDAY;
            default:
                // Fallback to strict enum parsing which throws exception if invalid
                return DayOfWeek.valueOf(day.toUpperCase());
        }
    }

    public List<ScheduledRestart> getScheduledRestarts() {
        return scheduledRestarts;
    }

    public static class ScheduledRestart {
        public final DayOfWeek day;
        public final LocalTime time;

        public ScheduledRestart(DayOfWeek day, LocalTime time) {
            this.day = day;
            this.time = time;
        }
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
