package com.mrerenk.mapdistancefix;

import com.mrerenk.mapdistancefix.command.MapDistanceFixCommand;
import com.mrerenk.mapdistancefix.config.ModConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MapdistancefixForge.MOD_ID)
public class MapdistancefixForge {

    public static final String MOD_ID = "mapdistancefix";
    public static final String MOD_NAME = "MapDistanceFix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public MapdistancefixForge() {
        LOGGER.info("Initializing {} (Forge)", MOD_NAME);

        // Register the Cloth Config screen to Forge's mod list GUI
        DistExecutor.safeRunWhenOn(
            Dist.CLIENT,
            () ->
                () ->
                    ModLoadingContext.get().registerExtensionPoint(
                        ConfigScreenHandler.ConfigScreenFactory.class,
                        () ->
                            new ConfigScreenHandler.ConfigScreenFactory(
                                (client, parent) ->
                                    ModConfigScreen.create(parent)
                            )
                    )
        );

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        LOGGER.info("{} initialized successfully!", MOD_NAME);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        MapDistanceFixCommand.register(event.getDispatcher());
    }
}
