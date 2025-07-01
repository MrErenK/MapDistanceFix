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

    // Thread-safe caching for player decoration type
    private static final AtomicReference<
        RegistryEntry<MapDecorationType>
    > cachedPlayerType = new AtomicReference<>();

    // Flag to track if we've attempted registry lookup
    private static volatile boolean registryLookupAttempted = false;

    private MapDecorationUtils() {
        // Utility class
    }

    /**
     * Gets the player decoration type, attempting registry lookup if not cached
     */
    public static Optional<
        RegistryEntry<MapDecorationType>
    > getPlayerDecorationType() {
        RegistryEntry<MapDecorationType> cached = cachedPlayerType.get();

        // If we have a cached value, return it
        if (cached != null) {
            return Optional.of(cached);
        }

        // If we haven't attempted registry lookup yet, try it now
        if (!registryLookupAttempted) {
            attemptRegistryLookup();
        }

        // Return cached value (may still be null if lookup failed)
        cached = cachedPlayerType.get();
        return cached != null ? Optional.of(cached) : Optional.empty();
    }

    /**
     * Attempts to look up player decoration type from registry using version-compatible iteration
     */
    private static void attemptRegistryLookup() {
        registryLookupAttempted = true;

        try {
            // Use direct iteration through registry
            for (MapDecorationType decorationType : Registries.MAP_DECORATION_TYPE) {
                // Check if this is the player decoration type by comparing with known player decoration
                if (isPlayerDecorationType(decorationType)) {
                    // Create registry entry using the reference holder approach
                    RegistryEntry<MapDecorationType> registryEntry =
                        RegistryEntry.of(decorationType);

                    if (cachedPlayerType.compareAndSet(null, registryEntry)) {
                        MapdistancefixClient.LOGGER.info(
                            "Successfully cached player decoration type from registry iteration"
                        );
                    }
                    return;
                }
            }

            MapdistancefixClient.LOGGER.warn(
                "Could not find player decoration type in registry"
            );
        } catch (Exception e) {
            MapdistancefixClient.LOGGER.error(
                "Error during registry lookup for player decoration type: {}",
                e.getMessage()
            );
        }
    }

    /**
     * Checks if a decoration type is the player decoration type by comparing identifiers
     */
    private static boolean isPlayerDecorationType(
        MapDecorationType decorationType
    ) {
        try {
            // Try to get the identifier from the registry
            var identifier = Registries.MAP_DECORATION_TYPE.getId(
                decorationType
            );
            return identifier != null && identifier.equals(PLAYER_ID);
        } catch (Exception e) {
            // If we can't get the identifier, we can't determine the type
            return false;
        }
    }

    /**
     * Attempts to extract and cache player decoration type from an existing player decoration
     * This serves as a backup method when registry lookup fails
     */
    public static void cachePlayerTypeFromDecoration(MapDecoration decoration) {
        if (isPlayer(decoration) && cachedPlayerType.get() == null) {
            if (cachedPlayerType.compareAndSet(null, decoration.type())) {
                MapdistancefixClient.LOGGER.info(
                    "Successfully cached player decoration type from existing decoration"
                );
            }
        }
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
     * Creates a player decoration with proper type and rotation if type is available
     */
    public static Optional<MapDecoration> createPlayerDecoration(
        byte x,
        byte z,
        byte rotation
    ) {
        return getPlayerDecorationType().map(type ->
            new MapDecoration(type, x, z, rotation, Optional.empty())
        );
    }

    /**
     * Converts an off-map decoration to a regular player decoration if possible
     */
    public static Optional<MapDecoration> convertOffMapDecoration(
        MapDecoration original,
        byte newRotation
    ) {
        if (!isPlayerOffMap(original)) {
            return Optional.of(original);
        }

        // Try to get player type (this will attempt registry lookup if needed)
        Optional<RegistryEntry<MapDecorationType>> playerTypeOpt =
            getPlayerDecorationType();

        if (playerTypeOpt.isPresent()) {
            MapdistancefixClient.LOGGER.debug(
                "Converting off-map decoration to player decoration"
            );
            return Optional.of(
                new MapDecoration(
                    playerTypeOpt.get(),
                    original.x(),
                    original.z(),
                    newRotation,
                    Optional.empty()
                )
            );
        }

        // Fallback: return original with updated rotation (still better than vanilla)
        MapdistancefixClient.LOGGER.debug(
            "Updating off-map decoration rotation (no player type available)"
        );
        return Optional.of(
            new MapDecoration(
                original.type(),
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
