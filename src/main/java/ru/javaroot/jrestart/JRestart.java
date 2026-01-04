package ru.javaroot.jrestart;

import org.bukkit.plugin.java.JavaPlugin;

public class JRestart extends JavaPlugin {
    private ConfigManager configManager;
    private MessagesConfig messagesConfig;
    private RestartManager restartManager;
    private VoteManager voteManager;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Keep this as it's standard practice and not explicitly removed
        // 1. Load MessagesConfig FIRST
        this.messagesConfig = new MessagesConfig(this);

        // 2. Load ConfigManager (depends on MessagesConfig implicitly if we passed it,
        // but here it reloads config)
        this.configManager = new ConfigManager(this);

        // 3. Initialize managers
        this.restartManager = new RestartManager(this);
        this.voteManager = new VoteManager(this);

        // 4. Register commands
        getCommand("jrestart").setExecutor(new RestartCommand(this));
        getCommand("jrestart").setTabCompleter(new RestartCommand(this));

        // 5. Start tasks
        getServer().getScheduler().runTaskTimer(this, restartManager, 20L, 20L);

        getLogger().info("JavaRestart enabled!");
    }

    @Override
    public void onDisable() {
        if (restartManager != null) {
            // Logic to save state if needed?
        }
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
