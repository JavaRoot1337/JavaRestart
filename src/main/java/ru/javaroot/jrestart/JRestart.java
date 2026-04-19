package ru.javaroot.jrestart;

import org.bukkit.plugin.java.JavaPlugin;

public class JRestart extends JavaPlugin {
    private ConfigManager configManager;
    private MessagesConfig messagesConfig;
    private RestartManager restartManager;
    private VoteManager voteManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.messagesConfig = new MessagesConfig(this);
        this.configManager = new ConfigManager(this);
        this.restartManager = new RestartManager(this);
        this.voteManager = new VoteManager(this);
        RestartCommand restartCommand = new RestartCommand(this);
        getCommand("jrestart").setExecutor(restartCommand);
        getCommand("jrestart").setTabCompleter(restartCommand);
        getServer().getPluginManager().registerEvents(new RestartListener(this), this);
        getServer().getScheduler().runTaskTimer(this, restartManager, 20L, 20L);
        getLogger().info("JavaRestart enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("JavaRestart disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RestartManager getRestartManager() {
        return restartManager;
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }
}
