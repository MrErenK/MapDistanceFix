package com.mrerenk.mapdistancefix.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles the loading, saving, and storing of Mod configuration values.
 */
public class ModConfig {

    private static final Path CONFIG_PATH = Paths.get(
        "config",
        "mapdistancefix.json"
    );
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    private static ModConfig instance;

    // Default configuration values
    private boolean showDistance = true;
    private boolean showDistanceWhenOffMap = true;
    private boolean showDistanceInsideBoundaries = false;
    private boolean showStructureDistances = true;
    private String distanceFormat = "%dm";
    private boolean useShortUnits = true;
    private int shortUnitThreshold = 1000;

    /**
     * Gets the current configuration instance, loading it if necessary.
     */
    public static ModConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    /**
     * Loads the configuration from disk, or creates default if it doesn't exist.
     */
    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
                instance = GSON.fromJson(reader, ModConfig.class);
            } catch (Exception e) {
                System.err.println(
                    "Failed to load MapDistanceFix config, using defaults."
                );
                e.printStackTrace();
                instance = new ModConfig();
            }
        } else {
            instance = new ModConfig();
            instance.save();
        }
    }

    /**
     * Saves the current configuration to disk.
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save MapDistanceFix config!");
            e.printStackTrace();
        }
    }

    // --- Getters and Setters ---

    public boolean isShowDistance() {
        return showDistance;
    }

    public void setShowDistance(boolean showDistance) {
        this.showDistance = showDistance;
    }

    public boolean isShowDistanceWhenOffMap() {
        return showDistanceWhenOffMap;
    }

    public void setShowDistanceWhenOffMap(boolean showDistanceWhenOffMap) {
        this.showDistanceWhenOffMap = showDistanceWhenOffMap;
    }

    public boolean isShowDistanceInsideBoundaries() {
        return showDistanceInsideBoundaries;
    }

    public void setShowDistanceInsideBoundaries(
        boolean showDistanceInsideBoundaries
    ) {
        this.showDistanceInsideBoundaries = showDistanceInsideBoundaries;
    }

    public boolean isShowStructureDistances() {
        return showStructureDistances;
    }

    public void setShowStructureDistances(boolean showStructureDistances) {
        this.showStructureDistances = showStructureDistances;
    }

    public String getDistanceFormat() {
        return distanceFormat;
    }

    public void setDistanceFormat(String distanceFormat) {
        this.distanceFormat = distanceFormat;
    }

    public boolean isUseShortUnits() {
        return useShortUnits;
    }

    public void setUseShortUnits(boolean useShortUnits) {
        this.useShortUnits = useShortUnits;
    }

    public int getShortUnitThreshold() {
        return shortUnitThreshold;
    }

    public void setShortUnitThreshold(int shortUnitThreshold) {
        this.shortUnitThreshold = shortUnitThreshold;
    }
}
