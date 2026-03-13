package com.mrerenk.mapdistancefix;

import com.mrerenk.mapdistancefix.command.MapDistanceFixCommand;
import com.mrerenk.mapdistancefix.config.ModConfigScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MapdistancefixNeoForge.MOD_ID)
public class MapdistancefixNeoForge {

    public static final String MOD_ID = "mapdistancefix";
    public static final String MOD_NAME = "MapDistanceFix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public MapdistancefixNeoForge(
        IEventBus modEventBus,
        ModContainer modContainer
    ) {
        LOGGER.info("Initializing {} (NeoForge)", MOD_NAME);

        // Register the Cloth Config screen to NeoForge's mod list GUI
        modContainer.registerExtensionPoint(
            IConfigScreenFactory.class,
            (client, parent) -> ModConfigScreen.create(parent)
        );

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        LOGGER.info("{} initialized successfully!", MOD_NAME);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        MapDistanceFixCommand.register(event.getDispatcher());
    }
}
