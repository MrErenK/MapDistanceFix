package com.mrerenk.mapdistancefix.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mrerenk.mapdistancefix.config.ModConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class MapDistanceFixCommand {

    public static void register(
        CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(
            "mapdistancefix"
        ).requires(source -> source.hasPermission(2));

        // 1. Status Command
        root.then(
            Commands.literal("status").executes(
                MapDistanceFixCommand::executeStatus
            )
        );

        // 2. Reload Command
        root.then(
            Commands.literal("reload").executes(
                MapDistanceFixCommand::executeReload
            )
        );

        // 3. Config Commands
        LiteralArgumentBuilder<CommandSourceStack> configNode =
            Commands.literal("config");

        // Booleans
        registerBooleanConfig(configNode, "showDistance");
        registerBooleanConfig(configNode, "showDistanceWhenOffMap");
        registerBooleanConfig(configNode, "showDistanceInsideBoundaries");
        registerBooleanConfig(configNode, "showStructureDistances");
        registerBooleanConfig(configNode, "useShortUnits");

        // String
        configNode.then(
            Commands.literal("distanceFormat")
                .then(
                    Commands.literal("get").executes(ctx ->
                        executeGetConfig(ctx, "distanceFormat")
                    )
                )
                .then(
                    Commands.literal("set").then(
                        Commands.argument(
                            "value",
                            StringArgumentType.string()
                        ).executes(ctx ->
                            executeSetConfigString(
                                ctx,
                                "distanceFormat",
                                StringArgumentType.getString(ctx, "value")
                            )
                        )
                    )
                )
                .then(
                    Commands.literal("reset").executes(ctx ->
                        executeResetConfig(ctx, "distanceFormat")
                    )
                )
        );

        // Integer
        configNode.then(
            Commands.literal("shortUnitThreshold")
                .then(
                    Commands.literal("get").executes(ctx ->
                        executeGetConfig(ctx, "shortUnitThreshold")
                    )
                )
                .then(
                    Commands.literal("set").then(
                        Commands.argument(
                            "value",
                            IntegerArgumentType.integer()
                        ).executes(ctx ->
                            executeSetConfigInt(
                                ctx,
                                "shortUnitThreshold",
                                IntegerArgumentType.getInteger(ctx, "value")
                            )
                        )
                    )
                )
                .then(
                    Commands.literal("reset").executes(ctx ->
                        executeResetConfig(ctx, "shortUnitThreshold")
                    )
                )
        );

        root.then(configNode);
        var registeredRoot = dispatcher.register(root);

        // Add "mdf" alias
        dispatcher.register(
            Commands.literal("mdf")
                .requires(source -> source.hasPermission(2))
                .redirect(registeredRoot)
        );
    }

    private static void registerBooleanConfig(
        LiteralArgumentBuilder<CommandSourceStack> node,
        String name
    ) {
        node.then(
            Commands.literal(name)
                .then(
                    Commands.literal("get").executes(ctx ->
                        executeGetConfig(ctx, name)
                    )
                )
                .then(
                    Commands.literal("set").then(
                        Commands.argument(
                            "value",
                            BoolArgumentType.bool()
                        ).executes(ctx ->
                            executeSetConfigBool(
                                ctx,
                                name,
                                BoolArgumentType.getBool(ctx, "value")
                            )
                        )
                    )
                )
                .then(
                    Commands.literal("reset").executes(ctx ->
                        executeResetConfig(ctx, name)
                    )
                )
        );
    }

    private static int executeStatus(
        CommandContext<CommandSourceStack> context
    ) {
        ModConfig config = ModConfig.get();
        context
            .getSource()
            .sendSuccess(
                () -> Component.literal("MapDistanceFix Configuration Status:"),
                false
            );
        context
            .getSource()
            .sendSuccess(
                () ->
                    Component.literal(
                        "- showDistance: " + config.isShowDistance()
                    ),
                false
            );
        context
            .getSource()
            .sendSuccess(
                () ->
                    Component.literal(
                        "- showDistanceWhenOffMap: " +
                            config.isShowDistanceWhenOffMap()
                    ),
                false
            );
        context
            .getSource()
            .sendSuccess(
                () ->
                    Component.literal(
                        "- showDistanceInsideBoundaries: " +
                            config.isShowDistanceInsideBoundaries()
                    ),
                false
            );
        context
            .getSource()
            .sendSuccess(
                () ->
                    Component.literal(
                        "- showStructureDistances: " +
                            config.isShowStructureDistances()
                    ),
                false
            );
        context
            .getSource()
            .sendSuccess(
                () ->
                    Component.literal(
                        "- distanceFormat: " + config.getDistanceFormat()
                    ),
                false
            );
        context
            .getSource()
            .sendSuccess(
                () ->
                    Component.literal(
                        "- useShortUnits: " + config.isUseShortUnits()
                    ),
                false
            );
        context
            .getSource()
            .sendSuccess(
                () ->
                    Component.literal(
                        "- shortUnitThreshold: " +
                            config.getShortUnitThreshold()
                    ),
                false
            );
        return 1;
    }

    private static int executeReload(
        CommandContext<CommandSourceStack> context
    ) {
        ModConfig.load();
        context
            .getSource()
            .sendSuccess(
                () ->
                    Component.literal("MapDistanceFix configuration reloaded."),
                true
            );
        return 1;
    }

    private static int executeGetConfig(
        CommandContext<CommandSourceStack> context,
        String option
    ) {
        ModConfig config = ModConfig.get();
        Object value = switch (option) {
            case "showDistance" -> config.isShowDistance();
            case "showDistanceWhenOffMap" -> config.isShowDistanceWhenOffMap();
            case "showDistanceInsideBoundaries" -> config.isShowDistanceInsideBoundaries();
            case "showStructureDistances" -> config.isShowStructureDistances();
            case "distanceFormat" -> config.getDistanceFormat();
            case "useShortUnits" -> config.isUseShortUnits();
            case "shortUnitThreshold" -> config.getShortUnitThreshold();
            default -> "Unknown";
        };
        context
            .getSource()
            .sendSuccess(
                () ->
                    Component.literal(
                        option + " is currently set to: " + value
                    ),
                false
            );
        return 1;
    }

    private static int executeSetConfigBool(
        CommandContext<CommandSourceStack> context,
        String option,
        boolean value
    ) {
        ModConfig config = ModConfig.get();
        switch (option) {
            case "showDistance" -> config.setShowDistance(value);
            case "showDistanceWhenOffMap" -> config.setShowDistanceWhenOffMap(
                value
            );
            case "showDistanceInsideBoundaries" -> config.setShowDistanceInsideBoundaries(
                value
            );
            case "showStructureDistances" -> config.setShowStructureDistances(
                value
            );
            case "useShortUnits" -> config.setUseShortUnits(value);
        }
        config.save();
        context
            .getSource()
            .sendSuccess(
                () -> Component.literal("Set " + option + " to " + value),
                true
            );
        return 1;
    }

    private static int executeSetConfigString(
        CommandContext<CommandSourceStack> context,
        String option,
        String value
    ) {
        ModConfig config = ModConfig.get();
        if ("distanceFormat".equals(option)) {
            config.setDistanceFormat(value);
        }
        config.save();
        context
            .getSource()
            .sendSuccess(
                () -> Component.literal("Set " + option + " to " + value),
                true
            );
        return 1;
    }

    private static int executeSetConfigInt(
        CommandContext<CommandSourceStack> context,
        String option,
        int value
    ) {
        ModConfig config = ModConfig.get();
        if ("shortUnitThreshold".equals(option)) {
            config.setShortUnitThreshold(value);
        }
        config.save();
        context
            .getSource()
            .sendSuccess(
                () -> Component.literal("Set " + option + " to " + value),
                true
            );
        return 1;
    }

    private static int executeResetConfig(
        CommandContext<CommandSourceStack> context,
        String option
    ) {
        ModConfig config = ModConfig.get();
        ModConfig defaults = new ModConfig(); // new instance has default values
        Object value = null;
        switch (option) {
            case "showDistance" -> {
                value = defaults.isShowDistance();
                config.setShowDistance((boolean) value);
            }
            case "showDistanceWhenOffMap" -> {
                value = defaults.isShowDistanceWhenOffMap();
                config.setShowDistanceWhenOffMap((boolean) value);
            }
            case "showDistanceInsideBoundaries" -> {
                value = defaults.isShowDistanceInsideBoundaries();
                config.setShowDistanceInsideBoundaries((boolean) value);
            }
            case "showStructureDistances" -> {
                value = defaults.isShowStructureDistances();
                config.setShowStructureDistances((boolean) value);
            }
            case "distanceFormat" -> {
                value = defaults.getDistanceFormat();
                config.setDistanceFormat((String) value);
            }
            case "useShortUnits" -> {
                value = defaults.isUseShortUnits();
                config.setUseShortUnits((boolean) value);
            }
            case "shortUnitThreshold" -> {
                value = defaults.getShortUnitThreshold();
                config.setShortUnitThreshold((int) value);
            }
        }
        config.save();
        final Object finalValue = value;
        context
            .getSource()
            .sendSuccess(
                () ->
                    Component.literal(
                        "Reset " + option + " to default: " + finalValue
                    ),
                true
            );
        return 1;
    }
}
