package ru.javaroot.cmd;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.javaroot.cfg.Cfg;
import ru.javaroot.cfg.Msg;
import ru.javaroot.restart.RestartSvc;
import ru.javaroot.restart.RestartType;
import ru.javaroot.vote.VoteSvc;

public final class RestartCmd {
    private static final String PERM_USE = "jrestart.use";
    private static final String PERM_ADMIN = "jrestart.admin";

    private final Cfg cfg;
    private final Msg msg;
    private final RestartSvc rstSvc;
    private final VoteSvc voteSvc;

    public RestartCmd(Cfg cfg, Msg msg, RestartSvc rstSvc, VoteSvc voteSvc) {
        this.cfg = cfg;
        this.msg = msg;
        this.rstSvc = rstSvc;
        this.voteSvc = voteSvc;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("jrestart")
            .requires(src -> src.getSender().hasPermission(PERM_USE))
            .executes(ctx -> sendUsage(ctx.getSource().getSender()))
            .then(Commands.literal("time")
                .executes(ctx -> handleTime(ctx.getSource().getSender())))
            .then(Commands.literal("vote")
                .executes(ctx -> handleVote(ctx.getSource().getSender())))
            .then(Commands.literal("reload")
                .requires(src -> src.getSender().hasPermission(PERM_ADMIN))
                .executes(ctx -> handleReload(ctx.getSource().getSender())))
            .then(Commands.literal("cancel")
                .requires(src -> src.getSender().hasPermission(PERM_ADMIN))
                .executes(ctx -> handleCancel(ctx.getSource().getSender())))
            .then(Commands.literal("now")
                .requires(src -> src.getSender().hasPermission(PERM_ADMIN))
                .executes(ctx -> sendUsage(ctx.getSource().getSender()))
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        suggestPrefix(builder, "technical", "daily", "vote");
                        return builder.buildFuture();
                    })
                    .executes(this::handleNowDefault)
                    .then(Commands.argument("seconds", IntegerArgumentType.integer())
                        .suggests((ctx, builder) -> {
                            suggestPrefix(builder, "60", "300", "1800");
                            return builder.buildFuture();
                        })
                        .executes(this::handleNowTimed))))
            .build();
    }

    private int sendUsage(CommandSender snd) {
        snd.sendMessage(msg.comp("usage"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleTime(CommandSender snd) {
        long seconds = rstSvc.secondsToRestart();
        if (seconds <= 0) {
            snd.sendMessage(msg.comp("no_restart_scheduled"));
            return Command.SINGLE_SUCCESS;
        }
        snd.sendMessage(msg.time("time_remaining", seconds));
        return Command.SINGLE_SUCCESS;
    }

    private int handleVote(CommandSender snd) {
        if (!(snd instanceof Player p)) {
            snd.sendMessage(msg.comp("only_players"));
            return Command.SINGLE_SUCCESS;
        }
        voteSvc.handleVote(p);
        return Command.SINGLE_SUCCESS;
    }

    private int handleReload(CommandSender snd) {
        cfg.reload();
        msg.reload();
        rstSvc.reload();
        snd.sendMessage(msg.comp("reload"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleCancel(CommandSender snd) {
        if (rstSvc.cancelManualRestart()) {
            voteSvc.clearVotes();
        } else {
            snd.sendMessage(msg.comp("no_restart_scheduled"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int handleNowDefault(CommandContext<CommandSourceStack> ctx) {
        CommandSender snd = ctx.getSource().getSender();
        String rawType = StringArgumentType.getString(ctx, "type");
        if (!rawType.equalsIgnoreCase("vote")) {
            snd.sendMessage(msg.comp("usage"));
            return Command.SINGLE_SUCCESS;
        }
        if (!cfg.isTypeEnabled(RestartType.VOTE)) {
            snd.sendMessage(msg.comp("type_disabled", "%type%", RestartType.VOTE.name()));
            return Command.SINGLE_SUCCESS;
        }
        scheduleManual(snd, cfg.get().voteRestartTime(), RestartType.VOTE);
        return Command.SINGLE_SUCCESS;
    }

    private int handleNowTimed(CommandContext<CommandSourceStack> ctx) {
        CommandSender snd = ctx.getSource().getSender();
        RestartType type = parseType(StringArgumentType.getString(ctx, "type"));
        if (type == null) {
            snd.sendMessage(msg.comp("invalid_type"));
            return Command.SINGLE_SUCCESS;
        }
        if (!cfg.isTypeEnabled(type)) {
            snd.sendMessage(msg.comp("type_disabled", "%type%", type.name()));
            return Command.SINGLE_SUCCESS;
        }
        int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
        if (seconds <= 0) {
            snd.sendMessage(msg.comp("invalid_time"));
            return Command.SINGLE_SUCCESS;
        }
        scheduleManual(snd, seconds, type);
        return Command.SINGLE_SUCCESS;
    }

    private void scheduleManual(CommandSender snd, int seconds, RestartType type) {
        rstSvc.setManualRestart(seconds, type);
        snd.sendMessage(msg.time("manual_restart_scheduled", seconds, "%type%", type.name()));
    }

    public static RestartType parseType(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "technical", "тех", "технический" -> RestartType.TECHNICAL;
            case "daily", "ежедневный" -> RestartType.DAILY;
            case "vote" -> RestartType.VOTE;
            default -> null;
        };
    }

    private static void suggestPrefix(SuggestionsBuilder builder, String... options) {
        String prefix = builder.getRemainingLowerCase();
        for (String opt : options) {
            if (opt.startsWith(prefix)) {
                builder.suggest(opt);
            }
        }
    }
}
