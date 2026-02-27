package com.mrerenk.mapdistancefix.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Configuration class for MapDistanceFix mod.
 * Settings are saved to and loaded from a JSON file.
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("mapdistancefix.json");

    private static ModConfig INSTANCE;

    // Config fields with defaults
    private Boolean showDistance = null;
    private Boolean showDistanceWhenOffMap = null;
    private Boolean showDistanceInsideBoundaries = null;
    private String distanceFormat = null;
    private Boolean useShortUnits = null;
    private Integer shortUnitThreshold = null;
    private Boolean showStructureDistances = null;

    // Default values
    private static final boolean DEFAULT_SHOW_DISTANCE = true;
    private static final boolean DEFAULT_SHOW_DISTANCE_WHEN_OFF_MAP = true;
    private static final boolean DEFAULT_SHOW_DISTANCE_INSIDE_BOUNDARIES =
        false;
    private static final String DEFAULT_DISTANCE_FORMAT = "%dm";
    private static final boolean DEFAULT_USE_SHORT_UNITS = true;
    private static final int DEFAULT_SHORT_UNIT_THRESHOLD = 1000;
    private static final boolean DEFAULT_SHOW_STRUCTURE_DISTANCES = true;

    /**
     * Get the singleton config instance, loading from file if necessary.
     */
    public static ModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    /**
     * Load config from file, or create default if not exists.
     */
    public static ModConfig load() {
        ModConfig config = null;
        boolean needsSave = false;

        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                config = GSON.fromJson(json, ModConfig.class);
            } catch (IOException e) {
                MapdistancefixClient.LOGGER.error("Failed to load config", e);
            } catch (Exception e) {
                MapdistancefixClient.LOGGER.error(
                    "Failed to parse config, using defaults",
                    e
                );
            }
        }

        if (config == null) {
            config = new ModConfig();
            needsSave = true;
        }

        // Apply defaults for any missing fields and track if we need to save
        needsSave |= config.applyDefaults();

        if (needsSave) {
            config.save();
            MapdistancefixClient.LOGGER.info(
                "Config saved with updated defaults"
            );
        } else {
            MapdistancefixClient.LOGGER.info(
                "Loaded config from {}",
                CONFIG_PATH
            );
        }

        return config;
    }

    /**
     * Apply default values to any null fields.
     *
     * @return true if any defaults were applied (config needs saving)
     */
    private boolean applyDefaults() {
        boolean changed = false;

        if (showDistance == null) {
            showDistance = DEFAULT_SHOW_DISTANCE;
            changed = true;
        }
        if (showDistanceWhenOffMap == null) {
            showDistanceWhenOffMap = DEFAULT_SHOW_DISTANCE_WHEN_OFF_MAP;
            changed = true;
        }
        if (showDistanceInsideBoundaries == null) {
            showDistanceInsideBoundaries =
                DEFAULT_SHOW_DISTANCE_INSIDE_BOUNDARIES;
            changed = true;
        }
        if (distanceFormat == null) {
            distanceFormat = DEFAULT_DISTANCE_FORMAT;
            changed = true;
        }
        if (useShortUnits == null) {
            useShortUnits = DEFAULT_USE_SHORT_UNITS;
            changed = true;
        }
        if (shortUnitThreshold == null) {
            shortUnitThreshold = DEFAULT_SHORT_UNIT_THRESHOLD;
            changed = true;
        }
        if (showStructureDistances == null) {
            showStructureDistances = DEFAULT_SHOW_STRUCTURE_DISTANCES;
            changed = true;
        }

        return changed;
    }

    /**
     * Save current config to file.
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
            MapdistancefixClient.LOGGER.debug(
                "Saved config to {}",
                CONFIG_PATH
            );
        } catch (IOException e) {
            MapdistancefixClient.LOGGER.error("Failed to save config", e);
        }
    }

    /**
     * Reload config from file.
     */
    public static void reload() {
        INSTANCE = load();
        MapdistancefixClient.LOGGER.info("Config reloaded");
    }

    // Getters (return primitives, guaranteed non-null after applyDefaults)

    public boolean isShowDistance() {
        return showDistance != null ? showDistance : DEFAULT_SHOW_DISTANCE;
    }

    public boolean isShowDistanceWhenOffMap() {
        return showDistanceWhenOffMap != null
            ? showDistanceWhenOffMap
            : DEFAULT_SHOW_DISTANCE_WHEN_OFF_MAP;
    }

    public boolean isShowDistanceInsideBoundaries() {
        return showDistanceInsideBoundaries != null
            ? showDistanceInsideBoundaries
            : DEFAULT_SHOW_DISTANCE_INSIDE_BOUNDARIES;
    }

    public String getDistanceFormat() {
        return distanceFormat != null
            ? distanceFormat
            : DEFAULT_DISTANCE_FORMAT;
    }

    public boolean isUseShortUnits() {
        return useShortUnits != null ? useShortUnits : DEFAULT_USE_SHORT_UNITS;
    }

    public int getShortUnitThreshold() {
        return shortUnitThreshold != null
            ? shortUnitThreshold
            : DEFAULT_SHORT_UNIT_THRESHOLD;
    }

    // Setters

    public void setShowDistance(boolean showDistance) {
        this.showDistance = showDistance;
    }

    public void setShowDistanceWhenOffMap(boolean showDistanceWhenOffMap) {
        this.showDistanceWhenOffMap = showDistanceWhenOffMap;
    }

    public void setShowDistanceInsideBoundaries(
        boolean showDistanceInsideBoundaries
    ) {
        this.showDistanceInsideBoundaries = showDistanceInsideBoundaries;
    }

    public void setDistanceFormat(String distanceFormat) {
        this.distanceFormat = distanceFormat;
    }

    public void setUseShortUnits(boolean useShortUnits) {
        this.useShortUnits = useShortUnits;
    }

    public void setShortUnitThreshold(int shortUnitThreshold) {
        this.shortUnitThreshold = shortUnitThreshold;
    }

    public boolean isShowStructureDistances() {
        return showStructureDistances != null
            ? showStructureDistances
            : DEFAULT_SHOW_STRUCTURE_DISTANCES;
    }

    public void setShowStructureDistances(boolean showStructureDistances) {
        this.showStructureDistances = showStructureDistances;
    }

    /**
     * Formats the distance value according to the configured format.
     * Handles unit conversion if useShortUnits is enabled.
     *
     * @param distance the distance in blocks
     * @return the formatted distance string
     */
    public String formatDistance(double distance) {
        int distanceInt = (int) Math.round(distance);

        if (isUseShortUnits() && distanceInt >= getShortUnitThreshold()) {
            // Use short format like "1.2k"
            double shortened = distanceInt / 1000.0;
            if (shortened >= 10) {
                return String.format("%.0fk", shortened);
            } else {
                return String.format("%.1fk", shortened);
            }
        }

        // Apply format string
        try {
            return String.format(getDistanceFormat(), distanceInt);
        } catch (Exception e) {
            // Fallback if format is invalid
            return distanceInt + "m";
        }
    }
}
