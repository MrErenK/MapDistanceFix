package com.mrerenk.mapdistancefix.util;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;

/**
 * Thread-safe utility class for managing map decorations
 */
public final class MapDecorationUtils {

    // Constants
    public static final float DEGREES_PER_ROTATION = 22.5f;
    public static final int ROTATION_MASK = 15;

    // Cache for player decoration type
    private static RegistryEntry<MapDecorationType> cachedPlayerType = null;
    private static final Object cacheLock = new Object();

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
            "Converting {} decoration to player decoration",
            getDecorationTypeName(original.type())
        );

        return Optional.of(
            new MapDecoration(
                getPlayerType(),
                original.x(),
                original.z(),
                newRotation,
                original.name()
            )
        );
    }

    /**
     * Check if decoration type should be converted
     */
    public static boolean shouldConvertDecorationType(
        RegistryEntry<MapDecorationType> type
    ) {
        return (
            type.equals(MapDecorationTypes.PLAYER_OFF_MAP) ||
            type.equals(MapDecorationTypes.PLAYER_OFF_LIMITS)
        );
    }

    /**
     * Check if decoration is any type of off-map player (off_map or off_limits)
     */
    public static boolean isPlayerOffMapAny(MapDecoration decoration) {
        return shouldConvertDecorationType(decoration.type());
    }

    /**
     * Gets the correct decoration type for off-map conversions
     * @deprecated Use {@link #shouldConvertDecorationType(RegistryEntry)} instead
     */
    @Deprecated
    public static boolean shouldConvertType(MapDecoration decoration) {
        return shouldConvertDecorationType(decoration.type());
    }

    /**
     * Gets a human-readable name for the decoration type
     */
    private static String getDecorationTypeName(
        RegistryEntry<MapDecorationType> type
    ) {
        if (type.equals(MapDecorationTypes.PLAYER_OFF_MAP)) {
            return "off-map";
        } else if (type.equals(MapDecorationTypes.PLAYER_OFF_LIMITS)) {
            return "off-limits";
        } else if (type.equals(MapDecorationTypes.PLAYER)) {
            return "player";
        }
        return "unknown";
    }

    /**
     * Cache player type from decoration for future use.
     * This helps avoid repeated lookups and ensures we use the correct player type
     * from the actual game instance.
     */
    public static void cachePlayerTypeFromDecoration(MapDecoration decoration) {
        if (decoration == null) {
            return;
        }

        // Only cache if it's a regular player decoration (not off-map variants)
        if (decoration.type().equals(MapDecorationTypes.PLAYER)) {
            synchronized (cacheLock) {
                if (cachedPlayerType == null) {
                    cachedPlayerType = decoration.type();
                    MapdistancefixClient.LOGGER.debug(
                        "Cached player decoration type from game instance"
                    );
                }
            }
        }
    }

    /**
     * Get the cached player type, falling back to the default if not cached
     */
    public static RegistryEntry<MapDecorationType> getPlayerType() {
        synchronized (cacheLock) {
            if (cachedPlayerType != null) {
                return cachedPlayerType;
            }
        }
        return MapDecorationTypes.PLAYER;
    }
}
