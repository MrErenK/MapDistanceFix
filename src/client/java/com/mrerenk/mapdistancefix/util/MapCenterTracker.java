package com.mrerenk.mapdistancefix.util;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.item.map.MapState;

/**
 * Tracks estimated map centers for client-side distance calculation.
 *
 * Since the client doesn't receive map center coordinates from the server
 * (they're always 0,0 on client), we estimate the center by observing player
 * positions when they are ON the map, or by estimating from off-map positions.
 *
 * Map centers are stored in memory cache for the current session.
 * Accurate centers are received from the server via networking packets.
 */
public final class MapCenterTracker {

    // Maximum number of entries in session cache before eviction
    private static final int MAX_SESSION_CACHE_SIZE = 1000;

    // Runtime cache: MapState instance -> MapCenter
    // Using WeakHashMap to allow MapState instances to be garbage collected
    // Synchronized wrapper for thread safety
    private static final Map<MapState, MapCenter> instanceCache =
        Collections.synchronizedMap(new WeakHashMap<>());

    // Session cache: "dimension_scale_centerX_centerZ" -> MapCenter
    // This allows each unique map to be cached during the current session
    // Uses LRU eviction to prevent unbounded growth
    private static final Map<String, MapCenter> sessionCache =
        Collections.synchronizedMap(
            new LinkedHashMap<String, MapCenter>(
                MAX_SESSION_CACHE_SIZE + 1,
                0.75f,
                true
            ) {
                @Override
                protected boolean removeEldestEntry(
                    Map.Entry<String, MapCenter> eldest
                ) {
                    boolean shouldRemove = size() > MAX_SESSION_CACHE_SIZE;
                    if (shouldRemove) {
                        MapdistancefixClient.LOGGER.debug(
                            "Evicting oldest map center from cache: ({}, {}) dimension={} scale={}",
                            eldest.getValue().x,
                            eldest.getValue().z,
                            eldest.getValue().dimension,
                            eldest.getValue().scale
                        );
                    }
                    return shouldRemove;
                }
            }
        );

    /**
     * Represents a map center.
     */
    public static class MapCenter {

        public final int x;
        public final int z;
        public final boolean isAccurate; // true if calculated from on-map position
        public final String dimension;
        public final byte scale;

        public MapCenter(
            int x,
            int z,
            boolean isAccurate,
            String dimension,
            byte scale
        ) {
            this.x = x;
            this.z = z;
            this.isAccurate = isAccurate;
            this.dimension = dimension;
            this.scale = scale;
        }
    }

    private MapCenterTracker() {
        // Utility class
    }

    /**
     * Round a coordinate to the nearest multiple of 8 blocks.
     * This prevents saving multiple centers for the same map due to small calculation variations.
     */
    private static int roundToGrid(int coord) {
        return Math.round(coord / 8.0f) * 8;
    }

    /**
     * Generate a unique key for a map center.
     * Uses dimension, scale, and the calculated center coordinates (rounded to grid).
     */
    private static String generateMapKey(MapCenter center) {
        return String.format(
            "%s_scale%d_x%d_z%d",
            center.dimension.replace(":", "_"),
            center.scale,
            center.x,
            center.z
        );
    }

    /**
     * Update and cache the map center based on a player decoration that is ON the map.
     * This gives accurate estimation when the player is within map boundaries.
     *
     * @param mapState   the map state
     * @param decoration the player decoration (must be a regular PLAYER type, not off-map)
     * @param playerX    the player's world X coordinate
     * @param playerZ    the player's world Z coordinate
     * @param scale      the map scale (0-4)
     */
    public static void updateFromOnMapDecoration(
        MapState mapState,
        MapDecoration decoration,
        double playerX,
        double playerZ,
        byte scale
    ) {
        // Only use regular PLAYER decorations (not off-map variants)
        if (!decoration.type().equals(MapDecorationTypes.PLAYER)) {
            return;
        }

        // Check if we already have a center for this MapState
        MapCenter existing = instanceCache.get(mapState);
        if (existing != null && existing.isAccurate) {
            // Already have an accurate center for this map, no need to recalculate
            return;
        }

        int blocksPerPixel = 1 << scale;

        // Decoration coordinates: -128 to 127 represent positions on the 128x128 map
        // decoration / 2.0 gives pixel offset from center
        // multiply by blocksPerPixel to get world offset from map center
        double pixelOffsetX = decoration.x() / 2.0;
        double pixelOffsetZ = decoration.z() / 2.0;

        double worldOffsetX = pixelOffsetX * blocksPerPixel;
        double worldOffsetZ = pixelOffsetZ * blocksPerPixel;

        // mapCenter = playerPosition - worldOffset
        int rawCenterX = (int) Math.round(playerX - worldOffsetX);
        int rawCenterZ = (int) Math.round(playerZ - worldOffsetZ);

        String dimension = mapState.dimension.getValue().toString();

        // Check if we have a nearby accurate center within 16 blocks (before rounding)
        // This will match centers from the current session (received from server or estimated)
        MapCenter nearbyCenter = findNearbyAccurateCenter(
            rawCenterX,
            rawCenterZ,
            dimension,
            scale,
            16
        );

        MapCenter center;
        if (nearbyCenter != null) {
            // Use the existing nearby center instead of creating a new one
            // This matches the MapState to a saved center from disk
            center = nearbyCenter;
            instanceCache.put(mapState, center);
            MapdistancefixClient.LOGGER.debug(
                "Matched MapState to saved center at ({}, {}) - calculated would be ({}, {})",
                nearbyCenter.x,
                nearbyCenter.z,
                rawCenterX,
                rawCenterZ
            );
        } else {
            // Round to nearest 8 blocks to prevent duplicate entries for the same map
            int roundedCenterX = roundToGrid(rawCenterX);
            int roundedCenterZ = roundToGrid(rawCenterZ);

            center = new MapCenter(
                roundedCenterX,
                roundedCenterZ,
                true,
                dimension,
                scale
            );

            // Store in persistent cache
            // Save to session cache if not already present
            String key = generateMapKey(center);
            if (!sessionCache.containsKey(key)) {
                sessionCache.put(key, center);

                MapdistancefixClient.LOGGER.debug(
                    "Cached map center: ({}, {}) for dimension {} scale {}",
                    roundedCenterX,
                    roundedCenterZ,
                    dimension,
                    scale
                );
            }

            // Store in runtime cache
            instanceCache.put(mapState, center);
        }
    }

    /**
     * Estimate the map center from an off-map decoration.
     * This is less accurate but allows distance to work when player has never been on the map.
     *
     * @param mapState   the map state
     * @param decoration the off-map player decoration
     * @param playerX    the player's world X coordinate
     * @param playerZ    the player's world Z coordinate
     * @param scale      the map scale (0-4)
     */
    public static void estimateFromOffMapDecoration(
        MapState mapState,
        MapDecoration decoration,
        double playerX,
        double playerZ,
        byte scale
    ) {
        // Don't overwrite a more accurate on-map estimation
        MapCenter existing = instanceCache.get(mapState);
        if (existing != null && existing.isAccurate) {
            return;
        }

        int blocksPerPixel = 1 << scale;
        int mapHalfSizeBlocks = 64 * blocksPerPixel; // Half the map size in world blocks

        byte decX = decoration.x();
        byte decZ = decoration.z();

        int estimatedCenterX;
        int estimatedCenterZ;

        // When at edge (-128 or 127), we know the player is beyond the map edge
        // The center is approximately mapHalfSizeBlocks away in the opposite direction

        if (decX <= -127) {
            // Player is off the left edge (negative X direction)
            estimatedCenterX = (int) Math.round(playerX + mapHalfSizeBlocks);
        } else if (decX >= 127) {
            // Player is off the right edge (positive X direction)
            estimatedCenterX = (int) Math.round(playerX - mapHalfSizeBlocks);
        } else {
            // Player is within X bounds on map, calculate precisely
            double pixelOffsetX = decX / 2.0;
            double worldOffsetX = pixelOffsetX * blocksPerPixel;
            estimatedCenterX = (int) Math.round(playerX - worldOffsetX);
        }

        if (decZ <= -127) {
            // Player is off the top edge (negative Z direction)
            estimatedCenterZ = (int) Math.round(playerZ + mapHalfSizeBlocks);
        } else if (decZ >= 127) {
            // Player is off the bottom edge (positive Z direction)
            estimatedCenterZ = (int) Math.round(playerZ - mapHalfSizeBlocks);
        } else {
            // Player is within Z bounds on map, calculate precisely
            double pixelOffsetZ = decZ / 2.0;
            double worldOffsetZ = pixelOffsetZ * blocksPerPixel;
            estimatedCenterZ = (int) Math.round(playerZ - worldOffsetZ);
        }

        // Round to nearest 8 blocks to prevent duplicate entries
        estimatedCenterX = roundToGrid(estimatedCenterX);
        estimatedCenterZ = roundToGrid(estimatedCenterZ);

        String dimension = mapState.dimension.getValue().toString();
        MapCenter center = new MapCenter(
            estimatedCenterX,
            estimatedCenterZ,
            false,
            dimension,
            scale
        );

        // Store in runtime cache
        instanceCache.put(mapState, center);

        // Check if we have a nearby accurate center within 128 blocks (before rounding)
        MapCenter nearbyCenter = findNearbyAccurateCenter(
            estimatedCenterX,
            estimatedCenterZ,
            dimension,
            scale,
            128
        );

        if (nearbyCenter != null) {
            // Use the nearby accurate center instead
            instanceCache.put(mapState, nearbyCenter);
            MapdistancefixClient.LOGGER.debug(
                "Found nearby accurate center at ({}, {}), using that instead of estimate",
                nearbyCenter.x,
                nearbyCenter.z
            );
        } else {
            // Don't cache inaccurate estimates - we'll just not show distance
            // This prevents showing wrong distances
            MapdistancefixClient.LOGGER.debug(
                "No accurate center within 128 blocks, not showing distance for this map"
            );
        }
    }

    /**
     * Find a nearby accurate center within the given radius using raw coordinates.
     * This helps prevent creating duplicate entries for the same map.
     */
    private static MapCenter findNearbyAccurateCenter(
        int centerX,
        int centerZ,
        String dimension,
        byte scale,
        int radius
    ) {
        MapCenter closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (MapCenter candidate : sessionCache.values()) {
            if (
                candidate.isAccurate &&
                candidate.dimension.equals(dimension) &&
                candidate.scale == scale
            ) {
                int dx = candidate.x - centerX;
                int dz = candidate.z - centerZ;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance <= radius && distance < closestDistance) {
                    closest = candidate;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    /**
     * Get the cached center for a map.
     * Checks runtime cache only (persistent cache is merged into runtime on estimation).
     *
     * @param mapState the map state
     * @return the center, or null if not yet calculated
     */
    public static MapCenter getCenter(MapState mapState) {
        return instanceCache.get(mapState);
    }

    /**
     * Check if we have an accurate center for this map that should be used for distance display.
     * Returns false if we only have an inaccurate off-map estimate.
     *
     * @param mapState the map state
     * @return true if we have an accurate center, false otherwise
     */
    public static boolean hasAccurateCenter(MapState mapState) {
        MapCenter center = instanceCache.get(mapState);
        // Return true if we have an accurate center (either calculated now or loaded from disk)
        return center != null && center.isAccurate;
    }

    /**
     * Calculate the distance from the player to the map center.
     *
     * @param mapState the map state
     * @param playerX  the player's world X coordinate
     * @param playerZ  the player's world Z coordinate
     * @return the distance in blocks, or -1 if center is not known
     */
    public static double calculateDistance(
        MapState mapState,
        double playerX,
        double playerZ
    ) {
        MapCenter center = getCenter(mapState);
        if (center == null) {
            return -1;
        }

        double dx = playerX - center.x;
        double dz = playerZ - center.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Set the center for a map state.
     *
     * @param mapState the map state
     * @param center the center to set
     */
    public static void setCenter(MapState mapState, MapCenter center) {
        instanceCache.put(mapState, center);

        String key = generateMapKey(center);
        sessionCache.put(key, center);

        MapdistancefixClient.LOGGER.debug(
            "Updated map center cache: mapId={}, center=({}, {}), dimension={}, scale={}",
            System.identityHashCode(mapState),
            center.x,
            center.z,
            center.dimension,
            center.scale
        );
    }

    /**
     * Check if we have a cached center for this map.
     *
     * @param mapState the map state
     * @return true if we have a cached center
     */
    public static boolean hasCenter(MapState mapState) {
        return getCenter(mapState) != null;
    }

    /**
     * Clear all cached map centers.
     * Called when disconnecting from a server or exiting a world.
     */
    public static void clearCache() {
        instanceCache.clear();
        sessionCache.clear();
        MapdistancefixClient.LOGGER.info("Cleared map center cache");
    }
}
