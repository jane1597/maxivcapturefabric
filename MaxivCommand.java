package com.maxivcapture;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * /maxiv status               shows every switch
 * /maxiv set <name> <true|false>   changes one live and saves it (battle switches apply from the NEXT battle)
 * Handy for finding which battle option clashes with another mod: turn them off one by one.
 */
public final class MaxivCommand {
    private MaxivCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("maxiv")
                    .requires(src -> src.hasPermission(2) || src.getServer().isSingleplayer())
                    .then(Commands.literal("status").executes(ctx -> {
                        StringBuilder sb = new StringBuilder("Max IV Capture:");
                        for (String n : Config.TOGGLES) sb.append("\n  ").append(n).append(" = ").append(Config.get(n));
                        sb.append("\n  cropGrowthMultiplier = ").append(Config.cropGrowth());
                        sb.append("\n  ballMultiplier = ").append(Config.ballMultiplier());
                        sb.append("\n  pastureDropsPerHour = ").append(Config.pastureDropsPerHour());
                        String msg = sb.toString();
                        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                        return 1;
                    }))
                    .then(Commands.literal("ball")
                            .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(1.0, 20.0)).executes(ctx -> {
                                double v = DoubleArgumentType.getDouble(ctx, "multiplier");
                                Config.setBallMultiplier(v);
                                BallBuff.apply();
                                ctx.getSource().sendSuccess(() -> Component.literal("Poke Ball catch rate multiplier = x" + v + " (1 = vanilla)"), false);
                                return 1;
                            })))
                    .then(Commands.literal("set")
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .suggests((c, b) -> SharedSuggestionProvider.suggest(Config.TOGGLES, b))
                                    .then(Commands.argument("value", BoolArgumentType.bool()).executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "name");
                                        boolean v = BoolArgumentType.getBool(ctx, "value");
                                        CommandSourceStack src = ctx.getSource();
                                        if (!Config.set(name, v)) {
                                            src.sendFailure(Component.literal("Unknown option. Use /maxiv status to see them."));
                                            return 0;
                                        }
                                        src.sendSuccess(() -> Component.literal(name + " = " + v + " (battle options apply from the next battle; drops/ride stats from the next reload or restart)"), false);
                                        return 1;
                                    })))));
        });
    }
}
