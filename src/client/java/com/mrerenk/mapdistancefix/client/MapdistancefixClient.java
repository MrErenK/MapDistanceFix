package com.mrerenk.mapdistancefix.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapdistancefixClient implements ClientModInitializer {

    public static final String MOD_ID = "mapdistancefix";
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
        LOGGER.info("{} v{} initialized successfully", MOD_NAME, version);
    }
}
