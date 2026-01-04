package ru.javaroot.jrestart;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ru.javaroot.jrestart.utils.TimeUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class RestartManager implements Runnable {
    private final JRestart plugin;
    private LocalDateTime manualTarget;
    private RestartType manualType;

    private BossBar currentBossBar;
    private LocalDateTime lastRunCommandTarget;

    public RestartManager(JRestart plugin) {
        this.plugin = plugin;
    }

    public void setManualRestart(int seconds, RestartType type) {
        this.manualTarget = LocalDateTime.now().plusSeconds(seconds);
        this.manualType = type;
        resetState();
        // Show title and update bossbar immediately
        showRestartTitle(seconds, type);
        updateBossBar(seconds, type);
    }

    public void cancelManualRestart() {
        if (manualTarget != null) {
            this.manualTarget = null;
            this.manualType = null;
            if (currentBossBar != null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.hideBossBar(currentBossBar);
                    // Also clear votes if restart was VOTE type?
                    // But here we don't know if it was VOTE type yet if we just set manualType =
                    // null.
                    // Wait, manualType IS stored.
                }
                currentBossBar = null;
            }
            // Clear votes if any
            plugin.getVoteManager().clearVotes();

            Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(plugin.getMessagesConfig().getMsg("restart_cancelled", "&cRestart cancelled.")));

            // Show "Restart Cancelled" title as requested
            String titleText = plugin.getMessagesConfig().getMsg("restart_cancelled", "&cRestart cancelled.");
            Title title = Title.title(LegacyComponentSerializer.legacyAmpersand().deserialize(titleText),
                    Component.empty());
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showTitle(title);
            }
        }
    }

    private void resetState() {
        lastRunCommandTarget = null;
    }

    @Override
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        RestartTarget targetInfo = getTargetTime(now);

        if (targetInfo == null)
            return;

        LocalDateTime target = targetInfo.time;
        RestartType type = targetInfo.type;

        long secondsRemaining = ChronoUnit.SECONDS.between(now, target);

        // Handle Restart Execution
        if (secondsRemaining <= 0) {
            // Treat FORCED as a hard restart if needed, but here we just pass the type for
            // context
            executeRestartPhase(type);
            return;
        }

        // Handle Intervals
        handleIntervals(secondsRemaining, type);

        // Handle Commands (Robust check)
        int cmdOffset = plugin.getConfig().getInt("settings.restart_cmd_offset", 15);
        if (secondsRemaining <= cmdOffset) {
            // Check if we already ran commands for THIS target
            if (lastRunCommandTarget == null || !lastRunCommandTarget.equals(target)) {
                executeConfigCommands();
                lastRunCommandTarget = target;
            }
        } else {
            // Reset if we are far away (e.g. cancelled and rescheduled)
            if (lastRunCommandTarget != null && lastRunCommandTarget.equals(target)) {
                // Do nothing, we already ran it.
                // But wait, if we are > offset, we shouldn't have run it.
                // This case implies we went BACK in time? (e.g. extended manual restart).
                // In that case, we might want to run them AGAIN when it drops back?
                // For safety, let's keep it simple. Once per target instance.
            } else {
                // target changed and is far future
                // no op
            }
        }

        // Handle BossBar
        // Show if Manual OR (Scheduled and time < 3600s/1h)
        boolean isManual = (manualTarget != null);
        if (isManual || secondsRemaining <= 3600) {
            updateBossBar(secondsRemaining, type);
        } else {
            if (currentBossBar != null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.hideBossBar(currentBossBar);
                }
                currentBossBar = null;
            }
        }
    }

    private static class RestartTarget {
        final LocalDateTime time;
        final RestartType type;

        RestartTarget(LocalDateTime time, RestartType type) {
            this.time = time;
            this.type = type;
        }
    }

    private RestartTarget getTargetTime(LocalDateTime now) {
        if (manualTarget != null) {
            // Check if passed?
            if (manualTarget.isBefore(now.minusSeconds(5))) { // tolerance
                manualTarget = null;
                manualType = null;
            } else {
                return new RestartTarget(manualTarget, manualType);
            }
        }

        // Find next scheduled
        List<ConfigManager.ScheduledRestart> schedules = plugin.getConfigManager().getScheduledRestarts();
        if (schedules.isEmpty())
            return null;

        LocalDateTime nearest = null;
        for (ConfigManager.ScheduledRestart schedule : schedules) {
            LocalDateTime next = now.with(java.time.temporal.TemporalAdjusters.nextOrSame(schedule.day))
                    .withHour(schedule.time.getHour())
                    .withMinute(schedule.time.getMinute())
                    .withSecond(0).withNano(0);

            if (next.isBefore(now)) {
                next = next.plusWeeks(1);
            }

            if (nearest == null || next.isBefore(nearest)) {
                nearest = next;
            }
        }

        if (nearest != null) {
            // User requested scheduled restarts to be DAILY type
            return new RestartTarget(nearest, RestartType.DAILY);
        }
        return null;
    }

    private void handleIntervals(long secondsRemaining, RestartType type) {
        if (secondsRemaining == 20) {
            String titleText = plugin.getMessagesConfig().getRawMsg("title_save", "&cРестарт. Мир сохранен.");
            String subtitleText = plugin.getMessagesConfig().getRawMsg("subtitle_save",
                    "&fСледующие изменения в мире не будут сохранены");

            Component mainTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(titleText);
            Component subTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(subtitleText);
            Title title = Title.title(mainTitle, subTitle);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showTitle(title);
            }
        }

        // Check Title Intervals
        List<Integer> titles = plugin.getConfig().getIntegerList("TitleAtIntervals");
        if (titles.contains((int) secondsRemaining)) {
            showRestartTitle(secondsRemaining, type);
        }

        // Check Sound Intervals
        List<Integer> sounds = plugin.getConfig().getIntegerList("soundAtIntervals");
        if (sounds.contains((int) secondsRemaining)) {
            String soundName = plugin.getConfig().getString("settings.sound", "UI_BUTTON_CLICK");
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), sound, 1f, 1f);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound name in config: " + soundName);
            }
        }
    }

    private void showRestartTitle(long secondsRemaining, RestartType type) {
        String titleKey;
        switch (type) {
            case DAILY:
                titleKey = "title_daily";
                break;
            case VOTE:
                titleKey = "title_vote";
                break;
            default:
                titleKey = "title_technical";
                break;
        }

        String titleText = plugin.getMessagesConfig().getRawMsg(titleKey, "&cRestarting...");
        Component mainTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(titleText);
        Component subTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(
                "&f" + TimeUtils.replaceTime("%time%", secondsRemaining, plugin.getMessagesConfig()));
        Title title = Title.title(mainTitle, subTitle);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
        }
    }

    private void updateBossBar(long secondsRemaining, RestartType type) {
        String key;
        switch (type) {
            case DAILY:
                key = "bossbar_daily";
                break;
            case VOTE:
                key = "bossbar_vote";
                break;
            default:
                key = "bossbar_technical";
                break;
        }

        // Similarly for BossBar, we probably don't want the prefix.
        String msgTemplate = plugin.getMessagesConfig().getRawMsg(key, "&fRestart in %time%");
        String msg = TimeUtils.replaceTime(msgTemplate, secondsRemaining, plugin.getMessagesConfig());

        Component name = LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
        BossBar.Color color = BossBar.Color.RED;
        BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;

        float progress = 1.0f;
        if (secondsRemaining <= 60) {
            progress = Math.max(0f, (float) secondsRemaining / 60f);
        }

        if (currentBossBar == null) {
            currentBossBar = BossBar.bossBar(name, progress, color, overlay);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showBossBar(currentBossBar);
            }
        } else {
            currentBossBar.name(name);
            currentBossBar.progress(progress);
            // Ensure all players see it
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showBossBar(currentBossBar); // idempotent usually, but good to ensure newbie joins see it?
                // Actually Adventure BossBar is per-player subscription.
                // I need to handle PlayerJoinEvent to add bossbar if active.
                // For now, I'll just add all online players each tick (safe enough for small
                // servers, but inefficient).
                // Better: Listener.
            }
        }
    }

    private void executeConfigCommands() {
        List<String> cmds = plugin.getConfig().getStringList("RestartCMD");
        for (String cmd : cmds) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private boolean restartPhaseStarted = false;

    private void executeRestartPhase(RestartType type) {
        if (restartPhaseStarted)
            return;
        restartPhaseStarted = true;

        // Remove bossbar
        if (currentBossBar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(currentBossBar);
            }
            currentBossBar = null;
        }

        // Title "Bye..."
        Component byeTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(
                plugin.getMessagesConfig().getRawMsg("title_bye", "&cBye..."));
        Title title = Title.title(byeTitle, Component.empty());

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
            // Kick message? User didn't ask explicitly to kick before shutdown, but
            // Shutdown kicks everyone.
            // Spigot default kic message is "Server closed".
            // If we want custom kick message we can kick manually?
            // The prompt said: "kick_message: &cСервер перезагружается." in config example.
            // So I should kick players.
            p.kick(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(plugin.getMessagesConfig().getMsg("kick_message", "&cServer restarting.")));
        }

        // Wait 3 seconds then stop
        int delay = plugin.getConfig().getInt("settings.restart_delay_seconds", 3);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.shutdown();
        }, delay * 20L);
    }

    public boolean isRestartScheduled() {
        return manualTarget != null || getTimeUntilNextScheduled() > 0;
    }

    public long getTimeUntilNextScheduled() {
        if (manualTarget != null) {
            return ChronoUnit.SECONDS.between(LocalDateTime.now(), manualTarget);
        }
        LocalDateTime now = LocalDateTime.now();
        RestartTarget target = getTargetTime(now);
        if (target == null)
            return -1;
        return ChronoUnit.SECONDS.between(now, target.time);
    }
}
