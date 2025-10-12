package com.mrerenk.mapdistancefix.client;

import com.mojang.logging.LogUtils;
import com.mrerenk.mapdistancefix.Mapdistancefix;
import java.util.Optional;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(
    modid = Mapdistancefix.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public class ClientHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        Optional<? extends ModContainer> container =
            ModList.get().getModContainerById(Mapdistancefix.MODID);
        String version = container
            .map(c -> c.getModInfo().getVersion().toString())
            .orElse("Unknown");

        LOGGER.info(
            "Initializing {} v{} (Client)",
            Mapdistancefix.MOD_NAME,
            version
        );
        LOGGER.info(
            "{} v{} client initialized successfully",
            Mapdistancefix.MOD_NAME,
            version
        );
    }
}
