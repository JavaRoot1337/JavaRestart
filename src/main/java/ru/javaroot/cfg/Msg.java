package ru.javaroot.cfg;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.javaroot.plat.txt.TimeFmt;
import ru.javaroot.plat.txt.Txt;

public final class Msg {
    private final JavaPlugin plug;
    private final File file;
    private volatile Bundle bundle;

    public Msg(JavaPlugin plug) {
        this.plug = plug;
        this.file = new File(plug.getDataFolder(), "messages.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            plug.saveResource("messages.yml", false);
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, String> map = new HashMap<>();
        ConfigurationSection sec = yaml.getConfigurationSection("messages");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                if (!key.equals("format") && sec.isString(key)) {
                    map.put(key, sec.getString(key, ""));
                }
            }
        }
        TimeFmt.Units units = new TimeFmt.Units(
            yaml.getString("messages.format.day", "день"),
            yaml.getString("messages.format.days_few", ""),
            yaml.getString("messages.format.days", "дней"),
            yaml.getString("messages.format.hour", "час"),
            yaml.getString("messages.format.hours_few", ""),
            yaml.getString("messages.format.hours", "часов"),
            yaml.getString("messages.format.minute", "минута"),
            yaml.getString("messages.format.minutes_few", ""),
            yaml.getString("messages.format.minutes", "минут"),
            yaml.getString("messages.format.second", "секунда"),
            yaml.getString("messages.format.seconds_few", ""),
            yaml.getString("messages.format.seconds", "секунд"),
            yaml.getString("messages.format.splitter", "и ")
        );
        bundle = new Bundle(Map.copyOf(map), units);
    }

    public String raw(String key) {
        return bundle.messages().getOrDefault(key, "");
    }

    public Component comp(String key) {
        return Txt.color(raw(key));
    }

    public Component comp(String key, String... pairs) {
        return Txt.color(Txt.apply(raw(key), pairs));
    }

    public Component time(String key, long seconds) {
        Bundle b = bundle;
        return Txt.color(TimeFmt.replace(b.messages().getOrDefault(key, ""), seconds, b.units()));
    }

    public Component time(String key, long seconds, String... pairs) {
        Bundle b = bundle;
        String replaced = TimeFmt.replace(b.messages().getOrDefault(key, ""), seconds, b.units());
        return Txt.color(Txt.apply(replaced, pairs));
    }

    public Component formatTime(long seconds) {
        return Txt.color(TimeFmt.format(seconds, bundle.units()));
    }

    private record Bundle(Map<String, String> messages, TimeFmt.Units units) {
    }
}
