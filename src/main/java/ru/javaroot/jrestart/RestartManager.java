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
    private boolean restartPhaseStarted;

    public RestartManager(JRestart plugin) {
        this.plugin = plugin;
    }

    public void setManualRestart(int seconds, RestartType type) {
        manualTarget = LocalDateTime.now().plusSeconds(seconds);
        manualType = type;
        resetState();
        showRestartTitle(seconds, type);
        updateBossBar(seconds, type);
    }

    public void cancelManualRestart() {
        if (manualTarget == null) {
            return;
        }
        manualTarget = null;
        manualType = null;
        hideBossBar();
        plugin.getVoteManager().clearVotes();
        Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(plugin.getMessagesConfig().getMsg("restart_cancelled", "&cRestart cancelled.")));
        String titleText = plugin.getMessagesConfig().getMsg("restart_cancelled", "&cRestart cancelled.");
        Title title = Title.title(LegacyComponentSerializer.legacyAmpersand().deserialize(titleText), Component.empty());
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
        }
    }

    private void resetState() {
        lastRunCommandTarget = null;
    }

    @Override
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        RestartTarget targetInfo = getTargetTime(now);
        if (targetInfo == null) {
            return;
        }

        LocalDateTime target = targetInfo.time;
        RestartType type = targetInfo.type;
        long secondsRemaining = ChronoUnit.SECONDS.between(now, target);

        if (secondsRemaining <= 0) {
            executeRestartPhase(type);
            return;
        }

        handleIntervals(secondsRemaining, type);

        int cmdOffset = plugin.getConfig().getInt("settings.restart_cmd_offset", 15);
        if (secondsRemaining <= cmdOffset && (lastRunCommandTarget == null || !lastRunCommandTarget.equals(target))) {
            executeConfigCommands();
            lastRunCommandTarget = target;
        }

        boolean isManual = manualTarget != null;
        if (isManual || secondsRemaining <= 3600) {
            updateBossBar(secondsRemaining, type);
        } else {
            hideBossBar();
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
            if (manualTarget.isBefore(now.minusSeconds(5))) {
                manualTarget = null;
                manualType = null;
            } else {
                return new RestartTarget(manualTarget, manualType);
            }
        }

        List<ConfigManager.ScheduledRestart> schedules = plugin.getConfigManager().getScheduledRestarts();
        if (schedules.isEmpty()) {
            return null;
        }

        LocalDateTime nearest = null;
        for (ConfigManager.ScheduledRestart schedule : schedules) {
            LocalDateTime next = now.with(java.time.temporal.TemporalAdjusters.nextOrSame(schedule.day))
                    .withHour(schedule.time.getHour())
                    .withMinute(schedule.time.getMinute())
                    .withSecond(0)
                    .withNano(0);
            if (next.isBefore(now)) {
                next = next.plusWeeks(1);
            }
            if (nearest == null || next.isBefore(nearest)) {
                nearest = next;
            }
        }

        if (nearest != null) {
            return new RestartTarget(nearest, RestartType.DAILY);
        }
        return null;
    }

    private void handleIntervals(long secondsRemaining, RestartType type) {
        if (secondsRemaining == 20) {
            String titleText = plugin.getMessagesConfig().getRawMsg("title_save", "&cRestart. World saved.");
            String subtitleText = plugin.getMessagesConfig().getRawMsg("subtitle_save",
                    "&fFollowing world changes will not be saved");
            Component mainTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(titleText);
            Component subTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(subtitleText);
            Title title = Title.title(mainTitle, subTitle);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showTitle(title);
            }
        }

        List<Integer> titles = plugin.getConfig().getIntegerList("TitleAtIntervals");
        if (titles.contains((int) secondsRemaining)) {
            showRestartTitle(secondsRemaining, type);
        }

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

        String msgTemplate = plugin.getMessagesConfig().getRawMsg(key, "&fRestart in %time%");
        String msg = TimeUtils.replaceTime(msgTemplate, secondsRemaining, plugin.getMessagesConfig());
        Component name = LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
        BossBar.Color color = BossBar.Color.RED;
        BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;
        float progress = secondsRemaining <= 60 ? Math.max(0f, (float) secondsRemaining / 60f) : 1.0f;

        if (currentBossBar == null) {
            currentBossBar = BossBar.bossBar(name, progress, color, overlay);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showBossBar(currentBossBar);
            }
        } else {
            currentBossBar.name(name);
            currentBossBar.progress(progress);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showBossBar(currentBossBar);
            }
        }
    }

    private void executeConfigCommands() {
        List<String> cmds = plugin.getConfig().getStringList("RestartCMD");
        for (String cmd : cmds) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private void executeRestartPhase(RestartType type) {
        if (restartPhaseStarted) {
            return;
        }
        restartPhaseStarted = true;
        hideBossBar();
        Component byeTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(
                plugin.getMessagesConfig().getRawMsg("title_bye", "&cBye..."));
        Title title = Title.title(byeTitle, Component.empty());
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
            p.kick(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(plugin.getMessagesConfig().getMsg("kick_message", "&cServer restarting.")));
        }
        int delay = plugin.getConfig().getInt("settings.restart_delay_seconds", 3);
        Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, delay * 20L);
    }

    private void hideBossBar() {
        if (currentBossBar == null) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hideBossBar(currentBossBar);
        }
        currentBossBar = null;
    }

    public boolean isRestartScheduled() {
        return manualTarget != null || getTimeUntilNextScheduled() > 0;
    }

    public boolean hasActiveBossBar() {
        return currentBossBar != null;
    }

    public void handlePlayerJoin(Player player) {
        if (currentBossBar != null) {
            player.showBossBar(currentBossBar);
        }
    }

    public void handlePlayerQuit(Player player) {
        if (currentBossBar != null) {
            player.hideBossBar(currentBossBar);
        }
    }

    public long getTimeUntilNextScheduled() {
        if (manualTarget != null) {
            return ChronoUnit.SECONDS.between(LocalDateTime.now(), manualTarget);
        }
        LocalDateTime now = LocalDateTime.now();
        RestartTarget target = getTargetTime(now);
        if (target == null) {
            return -1;
        }
        return ChronoUnit.SECONDS.between(now, target.time);
    }
}
