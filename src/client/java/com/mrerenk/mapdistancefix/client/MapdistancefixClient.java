package com.mrerenk.mapdistancefix.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mrerenk.mapdistancefix.config.ModConfig;
import com.mrerenk.mapdistancefix.network.MapCenterNetworkingClient;
import com.mrerenk.mapdistancefix.util.MapCenterTracker;
import com.mrerenk.mapdistancefix.util.RequestedMapsCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapdistancefixClient implements ClientModInitializer {

    public static final String MOD_ID = "map-distance-fix";
    public static final String MOD_NAME = "MapDistanceFix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onInitializeClient() {
        String version = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container ->
                container.getMetadata().getVersion().getFriendlyString()
            )
            .orElse("Unknown");

        LOGGER.info("Initializing {} v{}", MOD_NAME, version);

        // Load config
        ModConfig.get();

        // Register client-side networking
        MapCenterNetworkingClient.registerClientHandlers();

        // Register disconnect handler to clear cache
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            MapCenterTracker.clearCache();
            RequestedMapsCache.clearRequestedMaps();
            LOGGER.info(
                "Cleared map center cache and requested maps on disconnect"
            );
        });

        // Register commands
        ClientCommandRegistrationCallback.EVENT.register(
            this::registerCommands
        );

        LOGGER.info("{} v{} initialized successfully", MOD_NAME, version);
    }

    private void registerCommands(
        CommandDispatcher<FabricClientCommandSource> dispatcher,
        CommandRegistryAccess registryAccess
    ) {
        dispatcher.register(
            ClientCommandManager.literal("mapdistancefix")
                .then(
                    ClientCommandManager.literal("reload").executes(context -> {
                        ModConfig.reload();
                        context
                            .getSource()
                            .sendFeedback(
                                Text.literal(
                                    "§a[MapDistanceFix] Config reloaded!"
                                )
                            );
                        return 1;
                    })
                )
                .then(
                    ClientCommandManager.literal("status").executes(context -> {
                        ModConfig config = ModConfig.get();
                        context
                            .getSource()
                            .sendFeedback(
                                Text.literal(
                                    "§6[MapDistanceFix] Current settings:"
                                )
                            );
                        context
                            .getSource()
                            .sendFeedback(
                                Text.literal(
                                    "§7  showDistance: §f" +
                                        config.isShowDistance()
                                )
                            );
                        context
                            .getSource()
                            .sendFeedback(
                                Text.literal(
                                    "§7  showDistanceWhenOffMap: §f" +
                                        config.isShowDistanceWhenOffMap()
                                )
                            );
                        context
                            .getSource()
                            .sendFeedback(
                                Text.literal(
                                    "§7  showDistanceInsideBoundaries: §f" +
                                        config.isShowDistanceInsideBoundaries()
                                )
                            );
                        context
                            .getSource()
                            .sendFeedback(
                                Text.literal(
                                    "§7  distanceFormat: §f" +
                                        config.getDistanceFormat()
                                )
                            );
                        context
                            .getSource()
                            .sendFeedback(
                                Text.literal(
                                    "§7  useShortUnits: §f" +
                                        config.isUseShortUnits()
                                )
                            );
                        return 1;
                    })
                )
                .then(
                    ClientCommandManager.literal("clearcache").executes(
                        context -> {
                            MapCenterTracker.clearCache();
                            RequestedMapsCache.clearRequestedMaps();
                            context
                                .getSource()
                                .sendFeedback(
                                    Text.literal(
                                        "§a[MapDistanceFix] Map center cache and requested maps cleared!"
                                    )
                                );
                            return 1;
                        }
                    )
                )
        );

        // Also register short alias
        dispatcher.register(
            ClientCommandManager.literal("mdf").redirect(
                dispatcher.getRoot().getChild("mapdistancefix")
            )
        );
    }
}
