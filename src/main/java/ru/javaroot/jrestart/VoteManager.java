package ru.javaroot.jrestart;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VoteManager {
    private final JRestart plugin;
    private final Set<UUID> votes = new HashSet<>();

    public VoteManager(JRestart plugin) {
        this.plugin = plugin;
    }

    public void handleVote(Player player) {
        if (plugin.getRestartManager().isRestartScheduled()) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getMessagesConfig().getMsg("manual_restart_scheduled", "&cRestart already scheduled.")
                            .replace("%time%", plugin.getRestartManager().getTimeUntilNextScheduled() + "") // simple
                                                                                                            // fallback
                            .replace("%type%", "ANY")));
            return;
        }

        int minPlayers = plugin.getConfig().getInt("vote.min_players", 10);
        if (Bukkit.getOnlinePlayers().size() < minPlayers) {
            String msg = plugin.getMessagesConfig().getMsg("vote_not_enough_players",
                    "&cNot enough players. Min: %min%");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(msg.replace("%min%", String.valueOf(minPlayers))));
            return;
        }

        if (votes.contains(player.getUniqueId())) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(plugin.getMessagesConfig().getMsg("vote_already", "&cAlready voted.")));
            return;
        }

        votes.add(player.getUniqueId());
        checkVotes(player);
    }

    public void checkVotes(Player voter) {
        int online = Bukkit.getOnlinePlayers().size();
        if (online == 0)
            return; // Should not happen if triggered by player

        int percentage = plugin.getConfig().getInt("vote.percentage", 70);
        int needed = (int) Math.ceil(online * (percentage / 100.0));
        int current = votes.size();

        if (current >= needed) {
            int time = plugin.getConfig().getInt("vote.restart_time", 60);
            plugin.getRestartManager().setManualRestart(time, RestartType.VOTE);
            votes.clear(); // Reset votes
        } else {
            if (voter != null) {
                // Broadcast vote
                String msg = plugin.getMessagesConfig().getRawMsg("vote_broadcast",
                        "&ePlayer %player% voted. (%current%/%needed%)");
                msg = msg.replace("%player%", voter.getName())
                        .replace("%current%", String.valueOf(current))
                        .replace("%needed%", String.valueOf(needed));
                Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));

                // Personal message
                // Actually broadcast covers it, but maybe user wants specific feedback?
                // The prompt says "vote_started: &aВы проголосовали..."
                String personal = plugin.getMessagesConfig().getRawMsg("vote_started", "&aVoted. (%current%/%needed%)");
                personal = personal.replace("%current%", String.valueOf(current))
                        .replace("%needed%", String.valueOf(needed));
                voter.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(personal));
            }
        }
    }

    public void clearVotes() {
        votes.clear();
    }
}
