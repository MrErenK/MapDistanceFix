package com.mrerenk.mapdistancefix;

import com.mrerenk.mapdistancefix.command.MapDistanceFixCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapdistancefixFabric implements ModInitializer {

    public static final String MOD_ID = "mapdistancefix";
    public static final String MOD_NAME = "MapDistanceFix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onInitialize() {
        String version = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container ->
                container.getMetadata().getVersion().getFriendlyString()
            )
            .orElse("Unknown");

        LOGGER.info("Initializing {} v{} (Fabric)", MOD_NAME, version);

        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess, environment) -> {
                MapDistanceFixCommand.register(dispatcher);
            }
        );

        LOGGER.info("{} initialized successfully!", MOD_NAME);
    }
}
