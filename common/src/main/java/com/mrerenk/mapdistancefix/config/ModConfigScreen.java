package com.mrerenk.mapdistancefix.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Creates the Cloth Config configuration screen for MapDistanceFix.
 */
public class ModConfigScreen {

    /**
     * Creates and returns the config screen.
     *
     * @param parent The parent screen to return to when closing
     * @return The configured Screen instance
     */
    public static Screen create(Screen parent) {
        ModConfig config = ModConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("config.mapdistancefix.title"))
            .setSavingRunnable(config::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // General category
        ConfigCategory general = builder.getOrCreateCategory(
            Component.translatable("config.mapdistancefix.category.general")
        );

        // Show Distance toggle
        general.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Component.translatable(
                        "config.mapdistancefix.option.showDistance"
                    ),
                    config.isShowDistance()
                )
                .setDefaultValue(true)
                .setTooltip(
                    Component.translatable(
                        "config.mapdistancefix.option.showDistance.tooltip"
                    )
                )
                .setSaveConsumer(config::setShowDistance)
                .build()
        );

        // Show Distance When Off Map toggle
        general.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Component.translatable(
                        "config.mapdistancefix.option.showDistanceWhenOffMap"
                    ),
                    config.isShowDistanceWhenOffMap()
                )
                .setDefaultValue(true)
                .setTooltip(
                    Component.translatable(
                        "config.mapdistancefix.option.showDistanceWhenOffMap.tooltip"
                    )
                )
                .setSaveConsumer(config::setShowDistanceWhenOffMap)
                .build()
        );

        // Show Distance Inside Boundaries toggle
        general.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Component.translatable(
                        "config.mapdistancefix.option.showDistanceInsideBoundaries"
                    ),
                    config.isShowDistanceInsideBoundaries()
                )
                .setDefaultValue(false)
                .setTooltip(
                    Component.translatable(
                        "config.mapdistancefix.option.showDistanceInsideBoundaries.tooltip"
                    )
                )
                .setSaveConsumer(config::setShowDistanceInsideBoundaries)
                .build()
        );

        // Show Distance to Structure toggle
        general.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Component.translatable(
                        "config.mapdistancefix.option.showStructureDistances"
                    ),
                    config.isShowStructureDistances()
                )
                .setDefaultValue(true)
                .setTooltip(
                    Component.translatable(
                        "config.mapdistancefix.option.showStructureDistances.tooltip"
                    )
                )
                .setSaveConsumer(config::setShowStructureDistances)
                .build()
        );

        // Formatting category
        ConfigCategory formatting = builder.getOrCreateCategory(
            Component.translatable("config.mapdistancefix.category.formatting")
        );

        // Distance Format string
        formatting.addEntry(
            entryBuilder
                .startStrField(
                    Component.translatable(
                        "config.mapdistancefix.option.distanceFormat"
                    ),
                    config.getDistanceFormat()
                )
                .setDefaultValue("%dm")
                .setTooltip(
                    Component.translatable(
                        "config.mapdistancefix.option.distanceFormat.tooltip"
                    )
                )
                .setSaveConsumer(config::setDistanceFormat)
                .build()
        );

        // Use Short Units toggle
        formatting.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Component.translatable(
                        "config.mapdistancefix.option.useShortUnits"
                    ),
                    config.isUseShortUnits()
                )
                .setDefaultValue(true)
                .setTooltip(
                    Component.translatable(
                        "config.mapdistancefix.option.useShortUnits.tooltip"
                    )
                )
                .setSaveConsumer(config::setUseShortUnits)
                .build()
        );

        // Short Unit Threshold slider
        formatting.addEntry(
            entryBuilder
                .startIntSlider(
                    Component.translatable(
                        "config.mapdistancefix.option.shortUnitThreshold"
                    ),
                    config.getShortUnitThreshold(),
                    100,
                    10000
                )
                .setDefaultValue(1000)
                .setTooltip(
                    Component.translatable(
                        "config.mapdistancefix.option.shortUnitThreshold.tooltip"
                    )
                )
                .setSaveConsumer(config::setShortUnitThreshold)
                .setTextGetter(value -> Component.literal(value + " blocks"))
                .build()
        );

        return builder.build();
    }
}
