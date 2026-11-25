package ru.javaroot.restart;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.javaroot.cfg.Cfg;
import ru.javaroot.cfg.Msg;
import ru.javaroot.plat.sched.Exec;

public final class RestartSvc {
    private final Exec exec;
    private final Cfg cfg;
    private final Msg msg;
    private final RestartState state = new RestartState();
    private final AtomicReference<ScheduledTask> timerTask = new AtomicReference<>();
    private final AtomicReference<ScheduledTask> shutdownTask = new AtomicReference<>();
    private volatile BossBar bossBar;

    public RestartSvc(Exec exec, Cfg cfg, Msg msg) {
        this.exec = exec;
        this.cfg = cfg;
        this.msg = msg;
    }

    public void start() {
        if (timerTask.get() != null) {
            return;
        }
        ScheduledTask task = exec.runGlobalTimer(this::tick, 20L, 20L);
        if (!timerTask.compareAndSet(null, task)) {
            task.cancel();
        }
    }

    public void reload() {
        state.resetOnReload();
        hideBossBar();
    }

    public void stop() {
        ScheduledTask timer = timerTask.getAndSet(null);
        if (timer != null) {
            timer.cancel();
        }
        ScheduledTask shutdown = shutdownTask.getAndSet(null);
        if (shutdown != null) {
            shutdown.cancel();
        }
        hideBossBar();
        state.clearAll();
    }

    public void setManualRestart(int seconds, RestartType type) {
        LocalDateTime target = LocalDateTime.now().plusSeconds(seconds);
        state.setManual(target, type, seconds);
        showRestartTitle(seconds, type);
        updateBossBar(seconds, type);
    }

    public boolean cancelManualRestart() {
        if (!state.cancelManual()) {
            return false;
        }
        ScheduledTask shutdown = shutdownTask.getAndSet(null);
        if (shutdown != null) {
            shutdown.cancel();
        }
        hideBossBar();

        Component text = msg.comp("restart_cancelled");
        Bukkit.broadcast(text);
        Title title = Title.title(text, Component.empty());
        for (Player p : Bukkit.getOnlinePlayers()) {
            exec.runAtPlayer(p, () -> p.showTitle(title));
        }
        return true;
    }

    public boolean isManualRestartScheduled() {
        return state.isManualScheduled();
    }

    public RestartType manualType() {
        return state.manualType();
    }

    public long secondsToRestart() {
        LocalDateTime now = LocalDateTime.now();
        RestartState.Target target = state.findTarget(now, cfg.get().schedules());
        return target == null ? -1L : state.secondsUntil(now, target.time());
    }

    private void tick() {
        Cfg.Snapshot snap = cfg.get();
        LocalDateTime now = LocalDateTime.now();
        RestartState.Target target = state.findTarget(now, snap.schedules());
        if (target == null) {
            state.resetOnReload();
            hideBossBar();
            return;
        }

        long seconds = state.secondsUntil(now, target.time());
        if (seconds <= 0) {
            executeRestart(snap);
            return;
        }

        state.activateTarget(target.time());
        announce(seconds, target.type(), snap);
        runRestartCommands(seconds, target.time(), snap);
        updateBossBarIfNeeded(seconds, target.type());
    }

    private void announce(long seconds, RestartType type, Cfg.Snapshot snap) {
        int interval = (int) seconds;
        if (seconds == 20 && state.markSaveTitle()) {
            showSaveTitle();
        }
        if (snap.titleIntervals().contains(interval) && state.markTitle(interval)) {
            showRestartTitle(seconds, type);
        }
        if (snap.soundIntervals().contains(interval) && state.markSound(interval)) {
            playSound(snap.sound());
        }
    }

    private void runRestartCommands(long seconds, LocalDateTime target, Cfg.Snapshot snap) {
        if (seconds > snap.restartCmdOffset() || !state.markCommands(target)) {
            return;
        }
        for (String command : snap.restartCmds()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    private void updateBossBarIfNeeded(long seconds, RestartType type) {
        if (state.isManualScheduled() || seconds <= 3600) {
            updateBossBar(seconds, type);
            return;
        }
        hideBossBar();
    }

    private void showSaveTitle() {
        Title title = Title.title(msg.comp("title_save"), msg.comp("subtitle_save"));
        for (Player p : Bukkit.getOnlinePlayers()) {
            exec.runAtPlayer(p, () -> p.showTitle(title));
        }
    }

    private void showRestartTitle(long seconds, RestartType type) {
        String key = switch (type) {
            case TECHNICAL -> "title_technical";
            case DAILY -> "title_daily";
            case VOTE -> "title_vote";
        };
        Title title = Title.title(msg.comp(key), msg.formatTime(seconds));
        for (Player p : Bukkit.getOnlinePlayers()) {
            exec.runAtPlayer(p, () -> p.showTitle(title));
        }
    }

    private void playSound(Sound sound) {
        if (sound == null) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            exec.runAtPlayer(p, () -> p.playSound(sound));
        }
    }

    private synchronized void updateBossBar(long seconds, RestartType type) {
        String key = switch (type) {
            case TECHNICAL -> "bossbar_technical";
            case DAILY -> "bossbar_daily";
            case VOTE -> "bossbar_vote";
        };
        Component title = msg.time(key, seconds);
        float progress = seconds <= 60 ? Math.max(0f, (float) seconds / 60f) : 1f;
        BossBar bar = bossBar;
        if (bar == null) {
            bar = BossBar.bossBar(title, progress, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
            bossBar = bar;
        } else {
            bar.name(title);
            bar.progress(progress);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bar);
        }
    }

    private synchronized void hideBossBar() {
        BossBar bar = bossBar;
        if (bar == null) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hideBossBar(bar);
        }
        bossBar = null;
    }

    private void executeRestart(Cfg.Snapshot snap) {
        if (!state.startRestartPhase()) {
            return;
        }
        hideBossBar();

        Title title = Title.title(msg.comp("title_bye"), Component.empty());
        Component kickMsg = msg.comp("kick_message");
        for (Player p : Bukkit.getOnlinePlayers()) {
            exec.runAtPlayer(p, () -> {
                p.showTitle(title);
                p.kick(kickMsg);
            });
        }

        ScheduledTask task = exec.runGlobalLater(Bukkit::shutdown, snap.restartDelaySec() * 20L);
        shutdownTask.set(task);
    }
}
