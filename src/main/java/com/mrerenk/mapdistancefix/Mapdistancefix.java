package com.mrerenk.mapdistancefix;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Mapdistancefix.MODID)
public class Mapdistancefix {

    public static final String MODID = "mapdistancefix";
    public static final String MOD_NAME = "MapDistanceFix";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Mapdistancefix() {
        LOGGER.info("Initializing {} (Server)", MOD_NAME);

        // Get the version from the mod container
        Optional<? extends ModContainer> container =
            ModList.get().getModContainerById(MODID);
        String version = container
            .map(c -> c.getModInfo().getVersion().toString())
            .orElse("Unknown");

        LOGGER.info("{} v{} initialized successfully", MOD_NAME, version);
    }
}
