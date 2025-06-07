package com.mrerenk.mapdistancefix.util;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Thread-safe utility class for managing map decorations
 */
public final class MapDecorationUtils {

    // Constants
    public static final float DEGREES_PER_ROTATION = 22.5f;
    public static final int ROTATION_MASK = 15;
    public static final byte MAP_EDGE_LIMIT = 127;
    public static final byte MAP_EDGE_LIMIT_NEG = -127;

    private static final Identifier PLAYER_ID = Identifier.of(
        "minecraft",
        "player"
    );
    private static final Identifier PLAYER_OFF_MAP_ID = Identifier.of(
        "minecraft",
        "player_off_map"
    );

    // Thread-safe caching
    private static final AtomicReference<
        RegistryEntry<MapDecorationType>
    > cachedPlayerType = new AtomicReference<>();

    private MapDecorationUtils() {
        // Utility class
    }

    /**
     * Gets the player decoration type, using cached value if available
     */
    public static Optional<
        RegistryEntry<MapDecorationType>
    > getPlayerDecorationType() {
        RegistryEntry<MapDecorationType> cached = cachedPlayerType.get();
        if (cached != null) {
            return Optional.of(cached);
        }

        try {
            MapDecorationType playerType = Registries.MAP_DECORATION_TYPE.get(
                PLAYER_ID
            );
            if (playerType != null) {
                RegistryEntry<MapDecorationType> entry =
                    Registries.MAP_DECORATION_TYPE.getEntry(playerType);
                if (entry != null) {
                    cachedPlayerType.compareAndSet(null, entry);
                    return Optional.of(entry);
                }
            }
        } catch (Exception e) {
            MapdistancefixClient.LOGGER.warn(
                "Failed to get player decoration type: {}",
                e.getMessage()
            );
        }

        return Optional.empty();
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
    public static Optional<MapDecoration> createPlayerDecoration(
        byte x,
        byte z,
        byte rotation
    ) {
        return getPlayerDecorationType()
            .map(type ->
                new MapDecoration(type, x, z, rotation, Optional.empty())
            );
    }

    /**
     * Converts an off-map decoration to a regular player decoration
     */
    public static Optional<MapDecoration> convertOffMapDecoration(
        MapDecoration original,
        byte newRotation
    ) {
        if (!isPlayerOffMap(original)) {
            return Optional.of(original);
        }

        return createPlayerDecoration(original.x(), original.z(), newRotation);
    }

    /**
     * Check if decoration is a player off-map type
     */
    public static boolean isPlayerOffMap(MapDecoration decoration) {
        return decoration.type().matchesId(PLAYER_OFF_MAP_ID);
    }

    /**
     * Check if decoration is a player type
     */
    public static boolean isPlayer(MapDecoration decoration) {
        return decoration.type().matchesId(PLAYER_ID);
    }

    /**
     * Calculates edge position for a player outside map boundaries
     */
    public static EdgePosition calculateEdgePosition(
        double playerX,
        double playerZ
    ) {
        double length = Math.sqrt(playerX * playerX + playerZ * playerZ);

        if (length <= 0) {
            return new EdgePosition((byte) 0, (byte) 0);
        }

        double normalizedX = playerX / length;
        double normalizedZ = playerZ / length;

        double absNormX = Math.abs(normalizedX);
        double absNormZ = Math.abs(normalizedZ);

        byte edgeX, edgeZ;

        if (absNormX >= absNormZ) {
            edgeX = (byte) (normalizedX > 0
                    ? MAP_EDGE_LIMIT
                    : MAP_EDGE_LIMIT_NEG);
            edgeZ = (byte) MathHelper.clamp(
                (normalizedZ * MAP_EDGE_LIMIT) / absNormX,
                MAP_EDGE_LIMIT_NEG,
                MAP_EDGE_LIMIT
            );
        } else {
            edgeX = (byte) MathHelper.clamp(
                (normalizedX * MAP_EDGE_LIMIT) / absNormZ,
                MAP_EDGE_LIMIT_NEG,
                MAP_EDGE_LIMIT
            );
            edgeZ = (byte) (normalizedZ > 0
                    ? MAP_EDGE_LIMIT
                    : MAP_EDGE_LIMIT_NEG);
        }

        return new EdgePosition(edgeX, edgeZ);
    }

    /**
     * Record for edge position coordinates
     */
    public record EdgePosition(byte x, byte z) {}
}
