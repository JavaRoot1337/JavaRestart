package ru.javaroot.cfg;

import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class MsgCfg {
    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public MsgCfg(JavaPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public String message(String key) {
        return config.getString("messages." + key);
    }

    public String format(String key) {
        return config.getString("messages.format." + key);
    }
}
