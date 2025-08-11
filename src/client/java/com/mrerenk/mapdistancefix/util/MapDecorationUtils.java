package com.mrerenk.mapdistancefix.util;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import java.util.Optional;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.util.math.MathHelper;

/**
 * Thread-safe utility class for managing map decorations
 */
public final class MapDecorationUtils {

    // Constants
    public static final float DEGREES_PER_ROTATION = 22.5f;
    public static final int ROTATION_MASK = 15;

    private MapDecorationUtils() {
        // Utility class
    }

    /**
     * Converts player yaw to map rotation (0-15 range)
     */
    public static byte calculateMapRotation(float playerYaw) {
        float normalizedYaw = MathHelper.wrapDegrees(playerYaw);
        int mapRotation =
            Math.round(normalizedYaw / DEGREES_PER_ROTATION) & ROTATION_MASK;
        return (byte) mapRotation;
    }

    /**
     * Creates a player decoration with proper type and rotation
     */
    public static MapDecoration createPlayerDecoration(
        byte x,
        byte z,
        byte rotation
    ) {
        return new MapDecoration(
            MapDecorationTypes.PLAYER,
            x,
            z,
            rotation,
            Optional.empty()
        );
    }

    /**
     * Converts an off-map decoration to a regular player decoration
     */
    public static Optional<MapDecoration> convertOffMapDecoration(
        MapDecoration original,
        byte newRotation
    ) {
        if (!isPlayerOffMapAny(original)) {
            return Optional.of(original);
        }

        MapdistancefixClient.LOGGER.debug(
            "Converting off-map/off-limits decoration to player decoration"
        );

        return Optional.of(
            new MapDecoration(
                MapDecorationTypes.PLAYER,
                original.x(),
                original.z(),
                newRotation,
                Optional.empty()
            )
        );
    }

    /**
     * Check if decoration is a player off-map type
     */
    public static boolean isPlayerOffMap(MapDecoration decoration) {
        return decoration.type().equals(MapDecorationTypes.PLAYER_OFF_MAP);
    }

    /**
     * Check if decoration is a player off-limits type
     */
    public static boolean isPlayerOffLimits(MapDecoration decoration) {
        return decoration.type().equals(MapDecorationTypes.PLAYER_OFF_LIMITS);
    }

    /**
     * Check if decoration is any type of off-map player (off_map or off_limits)
     */
    public static boolean isPlayerOffMapAny(MapDecoration decoration) {
        return isPlayerOffMap(decoration) || isPlayerOffLimits(decoration);
    }

    /**
     * Check if decoration is a player type
     */
    public static boolean isPlayer(MapDecoration decoration) {
        return decoration.type().equals(MapDecorationTypes.PLAYER);
    }

    /**
     * Gets the correct decoration type for off-map conversions
     */
    public static boolean shouldConvertType(MapDecoration decoration) {
        return (
            decoration.type().equals(MapDecorationTypes.PLAYER_OFF_MAP) ||
            decoration.type().equals(MapDecorationTypes.PLAYER_OFF_LIMITS)
        );
    }
}
