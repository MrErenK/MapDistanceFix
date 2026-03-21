package com.mrerenk.mapdistancefix.util;

import com.mojang.datafixers.util.Pair;
import com.mrerenk.mapdistancefix.config.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

public final class MapItemSavedDataUtils {

    private MapItemSavedDataUtils() {
        // Utility class
    }

    /**
     * Overrides out-of-bounds player markers to use the normal player icon
     * while retaining the calculated rotation.
     */
    public static Pair<MapDecoration.Type, Byte> overridePlayerMarker(
        Pair<MapDecoration.Type, Byte> currentVal,
        byte calculatedRotation
    ) {
        if (currentVal == null) {
            return null;
        }

        MapDecoration.Type currentType = currentVal.getFirst();
        if (
            currentType == MapDecoration.Type.PLAYER_OFF_MAP ||
            currentType == MapDecoration.Type.PLAYER_OFF_LIMITS
        ) {
            return Pair.of(MapDecoration.Type.PLAYER, calculatedRotation);
        }

        return currentVal;
    }

    /**
     * Determines whether a frame decoration is outside the viewable map bounds.
     */
    public static boolean isOutOfBoundsFrame(
        MapDecoration.Type type,
        float offsetX,
        float offsetZ
    ) {
        if (type != MapDecoration.Type.FRAME) {
            return false;
        }

        boolean inBounds =
            offsetX >= -63f &&
            offsetX <= 63f &&
            offsetZ >= -63f &&
            offsetZ <= 63f;

        return !inBounds;
    }

    /**
     * Calculates the rotation manually
     */
    public static byte calculateRotation(
        LevelAccessor world,
        double rotation,
        ResourceKey<Level> dimension
    ) {
        if (dimension == Level.NETHER) {
            if (world != null) {
                int i = (int) (world.getLevelData().getDayTime() / 10L);
                return (byte) (((i * i * 34187121 + i * 121) >> 15) & 15);
            } else {
                return 0;
            }
        } else {
            double d = rotation < 0.0 ? rotation - 8.0 : rotation + 8.0;
            return (byte) ((int) ((d * 16.0) / 360.0));
        }
    }

    /**
     * Calculates the distance to a given target on a map
     */
    public static int calculateDistance(
        double targetX,
        double targetZ,
        int mapCenterX,
        int mapCenterZ
    ) {
        double deltaX = targetX - mapCenterX;
        double deltaZ = targetZ - mapCenterZ;
        return (int) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    /**
     * Formats a distance value according to the user's ModConfig settings
     */
    public static Component formatDistanceComponent(
        int distance,
        ModConfig config
    ) {
        String formattedString;

        if (
            config.isUseShortUnits() &&
            distance >= config.getShortUnitThreshold()
        ) {
            // e.g., 1200 -> "1.2k"
            double shortDist = distance / 1000.0;
            // Format to 1 decimal place, e.g., "1.2km" if the format is "%skm"
            // Wait, format is usually like "%dm", so we'll just replace the %d with our formatted float
            String numberStr = String.format("%.1f", shortDist);

            // We strip off the .0 if it's exact
            if (numberStr.endsWith(".0")) {
                numberStr = numberStr.substring(0, numberStr.length() - 2);
            }
            // Append "k"
            numberStr += "k";

            // If the user's string is "%dm", we replace "%d" with our string
            formattedString = config
                .getDistanceFormat()
                .replace("%d", numberStr);
        } else {
            formattedString = config
                .getDistanceFormat()
                .replace("%d", String.valueOf(distance));
        }

        return Component.literal(formattedString);
    }

    /**
     * Appends distance to a map decoration's name if applicable.
     */
    public static Component appendDistanceToName(
        Component originalName,
        int distance,
        ModConfig config
    ) {
        Component distanceComponent = formatDistanceComponent(distance, config);

        if (originalName == null || originalName.getString().isEmpty()) {
            return distanceComponent;
        }

        // e.g. "Mansion - 1200m"
        return originalName
            .copy()
            .append(Component.literal(" - "))
            .append(distanceComponent);
    }

    public static boolean isSupportedStructure(MapDecoration.Type type) {
        return (
            type == MapDecoration.Type.MANSION ||
            type == MapDecoration.Type.MONUMENT ||
            type == MapDecoration.Type.RED_X ||
            type == MapDecoration.Type.TARGET_X ||
            type == MapDecoration.Type.TARGET_POINT
        );
    }
}
