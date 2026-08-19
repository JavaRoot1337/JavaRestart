package ru.javaroot.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.javaroot.cfg.ConfigMgr;
import ru.javaroot.cfg.MsgCfg;
import ru.javaroot.restart.RestartMgr;
import ru.javaroot.restart.RestartType;
import ru.javaroot.util.TimeUtil;
import ru.javaroot.vote.VoteMgr;

public class RestartCmd implements CommandExecutor, TabCompleter {
    private final ConfigMgr configMgr;
    private final MsgCfg msgCfg;
    private final RestartMgr restartMgr;
    private final VoteMgr voteMgr;

    public RestartCmd(ConfigMgr configMgr, MsgCfg msgCfg, RestartMgr restartMgr, VoteMgr voteMgr) {
        this.configMgr = configMgr;
        this.msgCfg = msgCfg;
        this.restartMgr = restartMgr;
        this.voteMgr = voteMgr;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jrestart.use")) {
            sender.sendMessage(message("no_permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(message("usage"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "vote" -> vote(sender);
            case "reload" -> reload(sender);
            case "cancel" -> cancel(sender);
            case "time" -> time(sender);
            case "now" -> now(sender, args);
            default -> sender.sendMessage(message("usage"));
        }
        return true;
    }

    private void vote(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message("only_players"));
            return;
        }
        voteMgr.handleVote(player);
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("jrestart.admin")) {
            sender.sendMessage(message("no_permission"));
            return;
        }
        configMgr.reload();
        restartMgr.reload();
        sender.sendMessage(message("reload"));
    }

    private void cancel(CommandSender sender) {
        if (!sender.hasPermission("jrestart.admin")) {
            sender.sendMessage(message("no_permission"));
            return;
        }
        if (restartMgr.cancelManualRestart()) {
            voteMgr.clearVotes();
        }
    }

    private void time(CommandSender sender) {
        long seconds = restartMgr.secondsToRestart();
        if (seconds <= 0) {
            sender.sendMessage(message("no_restart_scheduled"));
            return;
        }
        String text = TimeUtil.replaceTime(msgCfg.message("time_remaining"), seconds, msgCfg);
        sender.sendMessage(component(text));
    }

    private void now(CommandSender sender, String[] args) {
        if (!sender.hasPermission("jrestart.admin")) {
            sender.sendMessage(message("no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(message("usage"));
            return;
        }

        if (args[1].equalsIgnoreCase("vote") && args.length == 2) {
            if (!configMgr.isTypeEnabled(RestartType.VOTE)) {
                sender.sendMessage(typeDisabled(RestartType.VOTE));
                return;
            }
            schedule(sender, configMgr.config().getInt("vote.restart_time"), RestartType.VOTE);
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(message("usage"));
            return;
        }

        RestartType type = parseType(args[1]);
        if (type == null) {
            sender.sendMessage(message("invalid_type"));
            return;
        }
        if (!configMgr.isTypeEnabled(type)) {
            sender.sendMessage(typeDisabled(type));
            return;
        }

        try {
            int seconds = Integer.parseInt(args[2]);
            if (seconds <= 0) {
                sender.sendMessage(message("invalid_time"));
                return;
            }
            schedule(sender, seconds, type);
        } catch (NumberFormatException e) {
            sender.sendMessage(message("invalid_time"));
        }
    }

    private void schedule(CommandSender sender, int seconds, RestartType type) {
        restartMgr.setManualRestart(seconds, type);
        String text = TimeUtil.replaceTime(msgCfg.message("manual_restart_scheduled"), seconds, msgCfg);
        sender.sendMessage(component(replace(text, "%type%", type.name())));
    }

    private RestartType parseType(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "technical", "тех", "технический" -> RestartType.TECHNICAL;
            case "daily", "ежедневный" -> RestartType.DAILY;
            case "vote" -> RestartType.VOTE;
            default -> null;
        };
    }

    private Component typeDisabled(RestartType type) {
        return component(replace(msgCfg.message("type_disabled"), "%type%", type.name()));
    }

    private Component message(String key) {
        return component(msgCfg.message(key));
    }

    private Component component(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text);
    }

    private String replace(String text, String key, String value) {
        return text == null ? null : text.replace(key, value);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> values = new ArrayList<>(List.of("time", "vote"));
            if (sender.hasPermission("jrestart.admin")) {
                values.addAll(List.of("reload", "now", "cancel"));
            }
            return matching(values, args[0]);
        }
        if (sender.hasPermission("jrestart.admin") && args.length == 2 && args[0].equalsIgnoreCase("now")) {
            return matching(List.of("technical", "daily", "vote"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("now")) {
            return List.of("60", "300", "1800");
        }
        return List.of();
    }

    private List<String> matching(List<String> values, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}
