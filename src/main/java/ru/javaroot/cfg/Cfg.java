package ru.javaroot.cfg;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.javaroot.plat.snd.SoundKey;
import ru.javaroot.restart.RestartType;

public final class Cfg {
    private final JavaPlugin plug;
    private volatile Snapshot snap;

    public Cfg(JavaPlugin plug) {
        this.plug = plug;
        reload();
    }

    public void reload() {
        plug.reloadConfig();
        FileConfiguration raw = plug.getConfig();

        boolean tech = raw.getBoolean("types.technical", true);
        boolean daily = raw.getBoolean("types.daily", true);
        boolean vote = raw.getBoolean("types.vote", false);

        List<String> cmds = List.copyOf(raw.getStringList("RestartCMD"));
        List<ScheduleSlot> schedules = parseSchedules(raw.getStringList("restarts"), daily, plug.getLogger());
        Set<Integer> titleAt = Set.copyOf(raw.getIntegerList("TitleAtIntervals"));
        Set<Integer> soundAt = Set.copyOf(raw.getIntegerList("soundAtIntervals"));

        String soundName = raw.getString("settings.sound");
        Sound sound = SoundKey.resolve(soundName);
        if (soundName != null && !soundName.isBlank() && sound == null) {
            plug.getLogger().warning("Некорректный звук в config.yml: " + soundName);
        }

        int delaySec = Math.max(0, raw.getInt("settings.restart_delay_seconds", 3));
        int cmdOffset = Math.max(0, raw.getInt("settings.restart_cmd_offset", 15));
        int voteMin = Math.max(1, raw.getInt("vote.min_players", 10));
        int votePct = Math.clamp(raw.getInt("vote.percentage", 70), 1, 100);
        int voteTime = Math.max(1, raw.getInt("vote.restart_time", 60));

        snap = new Snapshot(
            cmds,
            schedules,
            titleAt,
            soundAt,
            tech,
            daily,
            vote,
            sound,
            delaySec,
            cmdOffset,
            voteMin,
            votePct,
            voteTime
        );
    }

    public Snapshot get() {
        return snap;
    }

    public boolean isTypeEnabled(RestartType type) {
        Snapshot s = snap;
        return switch (type) {
            case TECHNICAL -> s.technicalEnabled();
            case DAILY -> s.dailyEnabled();
            case VOTE -> s.voteEnabled();
        };
    }

    public static List<ScheduleSlot> parseSchedules(List<String> rows, boolean dailyEnabled, Logger log) {
        if (!dailyEnabled || rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ScheduleSlot> out = new ArrayList<>();
        for (String row : rows) {
            String[] parts = row.split(";", -1);
            if (parts.length != 3) {
                log.warning("Некорректная строка расписания: " + row);
                continue;
            }
            try {
                int hour = Integer.parseInt(parts[1].trim());
                int minute = Integer.parseInt(parts[2].trim());
                LocalTime time = LocalTime.of(hour, minute);
                String day = parts[0].trim().toLowerCase(Locale.ROOT);
                addDaySlots(out, day, time);
            } catch (IllegalArgumentException | DateTimeException ex) {
                log.warning("Некорректная строка расписания: " + row);
            }
        }
        return List.copyOf(out);
    }

    private static void addDaySlots(List<ScheduleSlot> out, String day, LocalTime time) {
        if (isDaily(day)) {
            for (DayOfWeek dow : DayOfWeek.values()) {
                out.add(new ScheduleSlot(dow, time));
            }
            return;
        }
        out.add(new ScheduleSlot(parseDay(day), time));
    }

    private static DayOfWeek parseDay(String day) {
        return switch (day) {
            case "monday", "понедельник" -> DayOfWeek.MONDAY;
            case "tuesday", "вторник" -> DayOfWeek.TUESDAY;
            case "wednesday", "среда" -> DayOfWeek.WEDNESDAY;
            case "thursday", "четверг" -> DayOfWeek.THURSDAY;
            case "friday", "пятница" -> DayOfWeek.FRIDAY;
            case "saturday", "суббота" -> DayOfWeek.SATURDAY;
            case "sunday", "воскресенье" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Неизвестный день: " + day);
        };
    }

    private static boolean isDaily(String day) {
        return day.equals("daily")
            || day.equals("ежедневно")
            || day.equals("каждый день")
            || day.equals("каждыйдень");
    }

    public record Snapshot(
        List<String> restartCmds,
        List<ScheduleSlot> schedules,
        Set<Integer> titleIntervals,
        Set<Integer> soundIntervals,
        boolean technicalEnabled,
        boolean dailyEnabled,
        boolean voteEnabled,
        Sound sound,
        int restartDelaySec,
        int restartCmdOffset,
        int voteMinPlayers,
        int votePercentage,
        int voteRestartTime
    ) {
    }
}
