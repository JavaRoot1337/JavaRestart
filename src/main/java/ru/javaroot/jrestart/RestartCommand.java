package ru.javaroot.jrestart;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.javaroot.jrestart.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RestartCommand implements CommandExecutor, TabCompleter {
    private final JRestart plugin;

    public RestartCommand(JRestart plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jrestart.use")) {
            sender.sendMessage(msg("no_permission", "&cYou do not have permission."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(msg("usage", "&cUsage: /jrestart [reload|now|cancel|time|vote]"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "vote":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(toComponent("&cOnly players can vote."));
                    return true;
                }
                plugin.getVoteManager().handleVote((Player) sender);
                break;
            case "reload":
                if (!sender.hasPermission("jrestart.admin")) {
                    sender.sendMessage(msg("no_permission", "&cYou do not have permission."));
                    return true;
                }
                plugin.getMessagesConfig().reloadConfig();
                plugin.getConfigManager().reload();
                sender.sendMessage(msg("reload", "&aConfiguration reloaded."));
                break;
            case "cancel":
                if (!sender.hasPermission("jrestart.admin")) {
                    sender.sendMessage(msg("no_permission", "&cYou do not have permission."));
                    return true;
                }
                plugin.getRestartManager().cancelManualRestart();
                // sender.sendMessage(msg("restart_cancelled", "&aRestart cancelled.")); //
                // Already broadcasted
                break;
            case "time":
                // ... (time logic)
                plugin.getRestartManager().isRestartScheduled(); // Check needed?
                long remaining = plugin.getRestartManager().getTimeUntilNextScheduled();
                if (remaining <= 0) {
                    sender.sendMessage(msg("no_restart_scheduled", "&cNo restart scheduled."));
                } else {
                    String rawMsg = plugin.getMessagesConfig().getRawMsg("time_remaining",
                            "&aTime until restart: &e%time%");
                    sender.sendMessage(
                            toComponent(TimeUtils.replaceTime(rawMsg, remaining, plugin.getMessagesConfig())));
                }
                break;
            case "now":
                if (!sender.hasPermission("jrestart.admin")) {
                    sender.sendMessage(msg("no_permission", "&cYou do not have permission."));
                    return true;
                }
                if (args.length >= 2 && args[1].equalsIgnoreCase("vote")) {
                    // /jrestart now vote
                    // Start vote restart immediately (as if vote passed)
                    int time = plugin.getConfig().getInt("vote.restart_time", 60);
                    plugin.getRestartManager().setManualRestart(time, RestartType.VOTE);
                    String rawMsg = plugin.getMessagesConfig().getRawMsg("manual_restart_scheduled",
                            "&aManual restart scheduled in %time% (%type%).");
                    sender.sendMessage(toComponent(
                            TimeUtils.replaceTime(rawMsg, time, plugin.getMessagesConfig()).replace("%type%", "VOTE")));
                    return true;
                }
                handleNow(sender, args);
                break;
            default:
                sender.sendMessage(msg("usage", "&cUsage: /jrestart [reload|now|cancel|time|vote]"));
                break;
        }
        return true;
    }

    private void handleNow(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(
                    msg("usage", "&cUsage: /jrestart now <technical|daily|vote> <seconds> OR /jrestart now vote"));
            return;
        }
        String typeStr = args[1].toLowerCase();
        RestartType type;
        if (typeStr.equals("technical") || typeStr.equals("тех") || typeStr.equals("технический")) {
            type = RestartType.TECHNICAL;
        } else if (typeStr.equals("daily") || typeStr.equals("ежедневный")) {
            type = RestartType.DAILY;
        } else if (typeStr.equals("vote")) {
            type = RestartType.VOTE;
        } else {
            sender.sendMessage(msg("usage", "&cInvalid type. Use 'technical', 'daily' or 'vote'."));
            return;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(toComponent("&cInvalid time number."));
            return;
        }

        plugin.getRestartManager().setManualRestart(seconds, type);
        String rawMsg = plugin.getMessagesConfig().getRawMsg("manual_restart_scheduled",
                "&aManual restart scheduled in %time% (%type%).");
        String finalMsg = TimeUtils.replaceTime(rawMsg, seconds, plugin.getMessagesConfig())
                .replace("%type%", type.name());
        sender.sendMessage(toComponent(finalMsg));
    }

    private Component msg(String key, String def) {
        return toComponent(plugin.getMessagesConfig().getMsg(key, def));
    }

    private Component toComponent(String legacyText) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacyText);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("time", "vote"));
            if (sender.hasPermission("jrestart.admin")) {
                completions.addAll(Arrays.asList("reload", "now", "cancel"));
            }
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (sender.hasPermission("jrestart.admin") && args.length == 2 && args[0].equalsIgnoreCase("now")) {
            return Arrays.asList("technical", "daily", "vote").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("now")) {
            return Arrays.asList("60", "300", "1800"); // suggestions
        }

        return new ArrayList<>();
    }
}
