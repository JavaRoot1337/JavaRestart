package ru.javaroot.restart;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.javaroot.cfg.ConfigMgr;
import ru.javaroot.cfg.MsgCfg;
import ru.javaroot.util.TimeUtil;

public class RestartMgr implements Runnable {
    private final JavaPlugin plugin;
    private final ConfigMgr configMgr;
    private final MsgCfg msgCfg;
    private final Set<Integer> titleIntervals = new HashSet<>();
    private final Set<Integer> soundIntervals = new HashSet<>();
    private final Set<Integer> shownTitles = new HashSet<>();
    private final Set<Integer> playedSounds = new HashSet<>();
    private LocalDateTime manualTarget;
    private RestartType manualType;
    private LocalDateTime activeTarget;
    private LocalDateTime commandTarget;
    private BossBar bossBar;
    private BukkitTask timerTask;
    private BukkitTask shutdownTask;
    private boolean saveTitleShown;
    private boolean restartPhaseStarted;

    public RestartMgr(JavaPlugin plugin, ConfigMgr configMgr, MsgCfg msgCfg) {
        this.plugin = plugin;
        this.configMgr = configMgr;
        this.msgCfg = msgCfg;
        loadIntervals();
    }

    public void start() {
        if (timerTask == null) {
            timerTask = Bukkit.getScheduler().runTaskTimer(plugin, this, 20L, 20L);
        }
    }

    public void reload() {
        loadIntervals();
        activeTarget = null;
        commandTarget = null;
        resetAnnouncements();
        hideBossBar();
    }

    public void stop() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (shutdownTask != null) {
            shutdownTask.cancel();
            shutdownTask = null;
        }
        hideBossBar();
        resetAnnouncements();
    }

    public void setManualRestart(int seconds, RestartType type) {
        manualTarget = LocalDateTime.now().plusSeconds(seconds);
        manualType = type;
        activeTarget = null;
        commandTarget = null;
        restartPhaseStarted = false;
        resetAnnouncements();
        showRestartTitle(seconds, type);
        updateBossBar(seconds, type);
    }

    public boolean cancelManualRestart() {
        if (manualTarget == null) {
            return false;
        }

        manualTarget = null;
        manualType = null;
        activeTarget = null;
        commandTarget = null;
        restartPhaseStarted = false;
        if (shutdownTask != null) {
            shutdownTask.cancel();
            shutdownTask = null;
        }
        resetAnnouncements();
        hideBossBar();

        Component message = color(msgCfg.message("restart_cancelled"));
        Bukkit.broadcast(message);
        Title title = Title.title(message, Component.empty());
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
        }
        return true;
    }

    public boolean isManualRestartScheduled() {
        return manualTarget != null;
    }

    public long secondsToRestart() {
        LocalDateTime now = LocalDateTime.now();
        Target target = findTarget(now);
        return target == null ? -1 : ChronoUnit.SECONDS.between(now, target.time());
    }

    @Override
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        Target target = findTarget(now);
        if (target == null) {
            activeTarget = null;
            commandTarget = null;
            resetAnnouncements();
            hideBossBar();
            return;
        }

        long seconds = ChronoUnit.SECONDS.between(now, target.time());
        if (seconds <= 0) {
            executeRestart();
            return;
        }

        activateTarget(target.time());
        announce(seconds, target.type());
        runRestartCommands(seconds, target.time());
        updateBossBarIfNeeded(seconds, target.type());
    }

    private Target findTarget(LocalDateTime now) {
        if (manualTarget != null) {
            return new Target(manualTarget, manualType);
        }

        LocalDateTime nearest = null;
        for (ScheduledRestart schedule : configMgr.schedules()) {
            LocalDateTime next = now.with(TemporalAdjusters.nextOrSame(schedule.day()))
                .withHour(schedule.time().getHour())
                .withMinute(schedule.time().getMinute())
                .withSecond(0)
                .withNano(0);
            if (next.isBefore(now)) {
                next = next.plusWeeks(1);
            }
            if (nearest == null || next.isBefore(nearest)) {
                nearest = next;
            }
        }
        return nearest == null ? null : new Target(nearest, RestartType.DAILY);
    }

    private void activateTarget(LocalDateTime target) {
        if (!target.equals(activeTarget)) {
            activeTarget = target;
            commandTarget = null;
            resetAnnouncements();
        }
    }

    private void announce(long seconds, RestartType type) {
        int interval = (int) seconds;
        if (seconds == 20 && !saveTitleShown) {
            showSaveTitle();
            saveTitleShown = true;
        }
        if (titleIntervals.contains(interval) && shownTitles.add(interval)) {
            showRestartTitle(seconds, type);
        }
        if (soundIntervals.contains(interval) && playedSounds.add(interval)) {
            playSound();
        }
    }

    private void runRestartCommands(long seconds, LocalDateTime target) {
        int offset = configMgr.config().getInt("settings.restart_cmd_offset");
        if (seconds > offset || target.equals(commandTarget)) {
            return;
        }
        for (String command : configMgr.config().getStringList("RestartCMD")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        commandTarget = target;
    }

    private void updateBossBarIfNeeded(long seconds, RestartType type) {
        if (manualTarget != null || seconds <= 3600) {
            updateBossBar(seconds, type);
            return;
        }
        hideBossBar();
    }

    private void showSaveTitle() {
        Title title = Title.title(color(msgCfg.message("title_save")), color(msgCfg.message("subtitle_save")));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
        }
    }

    private void showRestartTitle(long seconds, RestartType type) {
        String key = switch (type) {
            case TECHNICAL -> "title_technical";
            case DAILY -> "title_daily";
            case VOTE -> "title_vote";
        };
        String subtitle = TimeUtil.replaceTime("%time%", seconds, msgCfg);
        Title title = Title.title(color(msgCfg.message(key)), color(subtitle));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
        }
    }

    private void playSound() {
        String name = configMgr.config().getString("settings.sound");
        if (name == null || name.isBlank()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(name.toUpperCase(Locale.ROOT));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, 1f, 1f);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + name);
        }
    }

    private void updateBossBar(long seconds, RestartType type) {
        String key = switch (type) {
            case TECHNICAL -> "bossbar_technical";
            case DAILY -> "bossbar_daily";
            case VOTE -> "bossbar_vote";
        };
        String text = TimeUtil.replaceTime(msgCfg.message(key), seconds, msgCfg);
        float progress = seconds <= 60 ? Math.max(0f, (float) seconds / 60f) : 1f;
        if (bossBar == null) {
            bossBar = BossBar.bossBar(color(text), progress, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        } else {
            bossBar.name(color(text));
            bossBar.progress(progress);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(bossBar);
        }
    }

    private void executeRestart() {
        if (restartPhaseStarted) {
            return;
        }
        restartPhaseStarted = true;
        hideBossBar();

        Title title = Title.title(color(msgCfg.message("title_bye")), Component.empty());
        Component kickMessage = color(msgCfg.message("kick_message"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
            player.kick(kickMessage);
        }

        int delay = configMgr.config().getInt("settings.restart_delay_seconds");
        shutdownTask = Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, delay * 20L);
    }

    private void hideBossBar() {
        if (bossBar == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
        bossBar = null;
    }

    private void loadIntervals() {
        titleIntervals.clear();
        titleIntervals.addAll(configMgr.config().getIntegerList("TitleAtIntervals"));
        soundIntervals.clear();
        soundIntervals.addAll(configMgr.config().getIntegerList("soundAtIntervals"));
    }

    private void resetAnnouncements() {
        shownTitles.clear();
        playedSounds.clear();
        saveTitleShown = false;
    }

    private Component color(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text);
    }

    private record Target(LocalDateTime time, RestartType type) {
    }
}
