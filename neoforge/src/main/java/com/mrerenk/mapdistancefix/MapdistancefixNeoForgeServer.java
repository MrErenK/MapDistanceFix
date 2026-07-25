package com.mrerenk.mapdistancefix;

import com.mrerenk.mapdistancefix.command.MapDistanceFixCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = "mapdistancefix", dist = Dist.DEDICATED_SERVER)
public class MapdistancefixNeoForgeServer {

    public static final String MOD_ID = "mapdistancefix";
    public static final String MOD_NAME = "MapDistanceFix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public MapdistancefixNeoForgeServer(
        IEventBus modEventBus,
        ModContainer modContainer
    ) {
        LOGGER.info("Initializing {} (NeoForge Dedicated Server)", MOD_NAME);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        LOGGER.info("{} initialized successfully!", MOD_NAME);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        MapDistanceFixCommand.register(event.getDispatcher());
    }
}
