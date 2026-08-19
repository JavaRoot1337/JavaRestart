package ru.javaroot.cfg;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.javaroot.restart.RestartType;
import ru.javaroot.restart.ScheduledRestart;

public class ConfigMgr {
    private final JavaPlugin plugin;
    private final MsgCfg msgCfg;
    private List<ScheduledRestart> schedules = List.of();

    public ConfigMgr(JavaPlugin plugin, MsgCfg msgCfg) {
        this.plugin = plugin;
        this.msgCfg = msgCfg;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        msgCfg.reload();
        schedules = readSchedules();
    }

    public FileConfiguration config() {
        return plugin.getConfig();
    }

    public List<ScheduledRestart> schedules() {
        return schedules;
    }

    public boolean isTypeEnabled(RestartType type) {
        return switch (type) {
            case TECHNICAL -> config().getBoolean("types.technical");
            case DAILY -> config().getBoolean("types.daily");
            case VOTE -> config().getBoolean("types.vote");
        };
    }

    private List<ScheduledRestart> readSchedules() {
        if (!isTypeEnabled(RestartType.DAILY)) {
            return List.of();
        }

        List<ScheduledRestart> result = new ArrayList<>();
        for (String row : config().getStringList("restarts")) {
            String[] parts = row.split(";", -1);
            if (parts.length != 3) {
                warn(row);
                continue;
            }

            try {
                LocalTime time = LocalTime.of(
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
                );
                String day = parts[0].trim().toLowerCase(Locale.ROOT);
                addSchedules(result, day, time);
            } catch (IllegalArgumentException e) {
                warn(row);
            }
        }
        return List.copyOf(result);
    }

    private void addSchedules(List<ScheduledRestart> result, String day, LocalTime time) {
        if (isDaily(day)) {
            for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
                result.add(new ScheduledRestart(dayOfWeek, time));
            }
            return;
        }
        result.add(new ScheduledRestart(parseDay(day), time));
    }

    private DayOfWeek parseDay(String day) {
        return switch (day) {
            case "monday", "понедельник" -> DayOfWeek.MONDAY;
            case "tuesday", "вторник" -> DayOfWeek.TUESDAY;
            case "wednesday", "среда" -> DayOfWeek.WEDNESDAY;
            case "thursday", "четверг" -> DayOfWeek.THURSDAY;
            case "friday", "пятница" -> DayOfWeek.FRIDAY;
            case "saturday", "суббота" -> DayOfWeek.SATURDAY;
            case "sunday", "воскресенье" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Unknown day");
        };
    }

    private boolean isDaily(String day) {
        return day.equals("daily")
            || day.equals("ежедневно")
            || day.equals("каждый день")
            || day.equals("каждыйдень");
    }

    private void warn(String row) {
        plugin.getLogger().warning("Invalid schedule row: " + row);
    }
}
