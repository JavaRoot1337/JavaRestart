package ru.javaroot.vote;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.javaroot.cfg.ConfigMgr;
import ru.javaroot.cfg.MsgCfg;
import ru.javaroot.restart.RestartMgr;
import ru.javaroot.restart.RestartType;
import ru.javaroot.util.TimeUtil;

public class VoteMgr {
    private final ConfigMgr configMgr;
    private final MsgCfg msgCfg;
    private final RestartMgr restartMgr;
    private final Set<UUID> votes = new HashSet<>();

    public VoteMgr(ConfigMgr configMgr, MsgCfg msgCfg, RestartMgr restartMgr) {
        this.configMgr = configMgr;
        this.msgCfg = msgCfg;
        this.restartMgr = restartMgr;
    }

    public void handleVote(Player player) {
        if (!configMgr.isTypeEnabled(RestartType.VOTE)) {
            player.sendMessage(message(replace(msgCfg.message("type_disabled"), "%type%", "VOTE")));
            return;
        }
        if (restartMgr.isManualRestartScheduled()) {
            String text = TimeUtil.replaceTime(
                msgCfg.message("manual_restart_scheduled"),
                restartMgr.secondsToRestart(),
                msgCfg
            );
            player.sendMessage(message(replace(text, "%type%", "ANY")));
            return;
        }

        removeOfflineVotes();
        int online = Bukkit.getOnlinePlayers().size();
        int minimum = configMgr.config().getInt("vote.min_players");
        if (online < minimum) {
            player.sendMessage(message(replace(
                msgCfg.message("vote_not_enough_players"),
                "%min%",
                String.valueOf(minimum)
            )));
            return;
        }
        if (!votes.add(player.getUniqueId())) {
            player.sendMessage(message(msgCfg.message("vote_already")));
            return;
        }

        int percentage = configMgr.config().getInt("vote.percentage");
        int needed = (int) Math.ceil(online * (percentage / 100.0));
        if (votes.size() >= needed) {
            restartMgr.setManualRestart(configMgr.config().getInt("vote.restart_time"), RestartType.VOTE);
            votes.clear();
            return;
        }

        String broadcast = msgCfg.message("vote_broadcast");
        broadcast = replace(broadcast, "%player%", player.getName());
        broadcast = replace(broadcast, "%current%", String.valueOf(votes.size()));
        broadcast = replace(broadcast, "%needed%", String.valueOf(needed));
        Bukkit.broadcast(message(broadcast));

        String started = msgCfg.message("vote_started");
        started = replace(started, "%current%", String.valueOf(votes.size()));
        started = replace(started, "%needed%", String.valueOf(needed));
        player.sendMessage(message(started));
    }

    public void clearVotes() {
        votes.clear();
    }

    private void removeOfflineVotes() {
        votes.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    private String replace(String text, String key, String value) {
        return text == null ? null : text.replace(key, value);
    }

    private Component message(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text);
    }
}
