package com.mrerenk.mapdistancefix.config;

import com.mrerenk.mapdistancefix.config.ModConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

/**
 * ModMenu integration for MapDistanceFix.
 * Provides access to the Cloth Config screen from the ModMenu.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Only provide config screen if Cloth Config is installed
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return parent -> ModConfigScreen.create(parent);
        }
        return parent -> null;
    }
}
