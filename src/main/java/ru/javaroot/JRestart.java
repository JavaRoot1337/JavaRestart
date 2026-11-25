package ru.javaroot;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;
import ru.javaroot.cfg.Cfg;
import ru.javaroot.cfg.Msg;
import ru.javaroot.cmd.RestartCmd;
import ru.javaroot.plat.sched.Exec;
import ru.javaroot.restart.RestartSvc;
import ru.javaroot.vote.VoteList;
import ru.javaroot.vote.VoteSvc;

public final class JRestart extends JavaPlugin {
    private Exec exec;
    private RestartSvc rstSvc;
    private VoteSvc voteSvc;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        exec = new Exec(this);
        Msg msg = new Msg(this);
        Cfg cfg = new Cfg(this);
        rstSvc = new RestartSvc(exec, cfg, msg);
        voteSvc = new VoteSvc(cfg, msg, rstSvc);

        getServer().getPluginManager().registerEvents(new VoteList(voteSvc), this);

        RestartCmd cmd = new RestartCmd(cfg, msg, rstSvc, voteSvc);
        getLifecycleManager().registerEventHandler(
            LifecycleEvents.COMMANDS,
            event -> event.registrar().register(cmd.build(), "Управление рестартами", List.of("jr"))
        );

        rstSvc.start();
        getLogger().info("JavaRestart enabled");
    }

    @Override
    public void onDisable() {
        if (rstSvc != null) {
            rstSvc.stop();
        }
        if (voteSvc != null) {
            voteSvc.clearVotes();
        }
        if (exec != null) {
            exec.cancelAll();
        }
        getLogger().info("JavaRestart disabled");
    }
}
