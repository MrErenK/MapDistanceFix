package com.mrerenk.mapdistancefix;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapdistancefixMod implements ModInitializer {

    public static final String MOD_ID = "map-distance-fix";
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

        LOGGER.info("Initializing {} v{} (Server)", MOD_NAME, version);
        LOGGER.info("{} v{} server initialized successfully", MOD_NAME, version);
    }
}
