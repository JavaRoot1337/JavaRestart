package ru.javaroot;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.javaroot.cfg.ConfigMgr;
import ru.javaroot.cfg.MsgCfg;
import ru.javaroot.command.RestartCmd;
import ru.javaroot.restart.RestartMgr;
import ru.javaroot.vote.VoteMgr;

public class JRestart extends JavaPlugin {
    private RestartMgr restartMgr;
    private VoteMgr voteMgr;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        MsgCfg msgCfg = new MsgCfg(this);
        ConfigMgr configMgr = new ConfigMgr(this, msgCfg);
        restartMgr = new RestartMgr(this, configMgr, msgCfg);
        voteMgr = new VoteMgr(configMgr, msgCfg, restartMgr);

        PluginCommand command = getCommand("jrestart");
        if (command == null) {
            getLogger().severe("Command jrestart is not registered");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        RestartCmd restartCmd = new RestartCmd(configMgr, msgCfg, restartMgr, voteMgr);
        command.setExecutor(restartCmd);
        command.setTabCompleter(restartCmd);
        restartMgr.start();
        getLogger().info("JavaRestart loaded");
    }

    @Override
    public void onDisable() {
        if (restartMgr != null) {
            restartMgr.stop();
        }
        if (voteMgr != null) {
            voteMgr.clearVotes();
        }
        getLogger().info("JavaRestart disabled");
    }
}
