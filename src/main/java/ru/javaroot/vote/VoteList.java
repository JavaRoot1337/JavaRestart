package ru.javaroot.vote;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class VoteList implements Listener {
    private final VoteSvc voteSvc;

    public VoteList(VoteSvc voteSvc) {
        this.voteSvc = voteSvc;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        voteSvc.removeVote(event.getPlayer().getUniqueId());
    }
}
