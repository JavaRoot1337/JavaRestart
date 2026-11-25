package ru.javaroot.vote;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.javaroot.cfg.Cfg;
import ru.javaroot.cfg.Msg;
import ru.javaroot.restart.RestartSvc;
import ru.javaroot.restart.RestartType;

public final class VoteSvc {
    private final Cfg cfg;
    private final Msg msg;
    private final RestartSvc rstSvc;
    private final Set<UUID> votes = ConcurrentHashMap.newKeySet();

    public VoteSvc(Cfg cfg, Msg msg, RestartSvc rstSvc) {
        this.cfg = cfg;
        this.msg = msg;
        this.rstSvc = rstSvc;
    }

    public synchronized void handleVote(Player p) {
        if (!cfg.isTypeEnabled(RestartType.VOTE)) {
            p.sendMessage(msg.comp("type_disabled", "%type%", RestartType.VOTE.name()));
            return;
        }
        if (rstSvc.isManualRestartScheduled()) {
            RestartType active = rstSvc.manualType();
            String typeName = active != null ? active.name() : RestartType.TECHNICAL.name();
            p.sendMessage(msg.time("manual_restart_scheduled", rstSvc.secondsToRestart(), "%type%", typeName));
            return;
        }

        votes.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        Cfg.Snapshot snap = cfg.get();
        int online = Bukkit.getOnlinePlayers().size();
        int minPlayers = snap.voteMinPlayers();
        if (online < minPlayers) {
            p.sendMessage(msg.comp("vote_not_enough_players", "%min%", String.valueOf(minPlayers)));
            return;
        }
        if (!votes.add(p.getUniqueId())) {
            p.sendMessage(msg.comp("vote_already"));
            return;
        }

        int needed = neededVotes(online, snap.votePercentage());
        if (votes.size() >= needed) {
            rstSvc.setManualRestart(snap.voteRestartTime(), RestartType.VOTE);
            votes.clear();
            return;
        }

        String current = String.valueOf(votes.size());
        String target = String.valueOf(needed);
        Bukkit.broadcast(msg.comp("vote_broadcast", "%player%", p.getName(), "%current%", current, "%needed%", target));
        p.sendMessage(msg.comp("vote_started", "%current%", current, "%needed%", target));
    }

    public void removeVote(UUID id) {
        votes.remove(id);
    }

    public void clearVotes() {
        votes.clear();
    }

    public static int neededVotes(int online, int percentage) {
        return Math.max(1, (int) Math.ceil(online * (percentage / 100.0)));
    }
}
