package com.mrerenk.mapdistancefix.util;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;

/**
 * Tracks estimated map centers for client-side distance calculation.
 *
 * Since the client doesn't receive map center coordinates from the server
 * (they're always 0), we estimate the center by observing player positions
 * when they are ON the map, or by reverse-engineering from off-map positions.
 */
public final class MapCenterTracker {

    // Cache of estimated map centers, keyed by MapState instance
    // Using WeakHashMap so entries are automatically removed when MapState is garbage collected
    private static final Map<MapState, EstimatedCenter> centerCache =
        new WeakHashMap<>();

    /**
     * Represents an estimated map center.
     */
    public static class EstimatedCenter {

        public final int x;
        public final int z;
        public final boolean isAccurate; // true if estimated from on-map position

        public EstimatedCenter(int x, int z, boolean isAccurate) {
            this.x = x;
            this.z = z;
            this.isAccurate = isAccurate;
        }
    }

    private MapCenterTracker() {
        // Utility class
    }

    /**
     * Try to estimate and cache the map center based on a player icon that is ON the map.
     * This gives the most accurate estimation.
     *
     * Map coordinate system:
     * - The map is 128x128 pixels
     * - Icon x/z range from -128 to 127
     * - These map to pixel positions: icon / 2 = pixel offset from center (-64 to 63.5)
     * - Each pixel represents (1 << scale) blocks in the world
     *
     * @param mapState   the map state
     * @param icon       the player icon (must be a regular PLAYER type, not off-map)
     * @param playerX    the player's world X coordinate
     * @param playerZ    the player's world Z coordinate
     * @param scale      the map scale (0-4)
     */
    public static void updateFromOnMapIcon(
        MapState mapState,
        MapIcon icon,
        double playerX,
        double playerZ,
        byte scale
    ) {
        // Only use regular PLAYER icons (not off-map variants)
        if (icon.getType() != MapIcon.Type.PLAYER) {
            return;
        }

        int blocksPerPixel = 1 << scale;

        // Icon coordinates: -128 to 127 represent positions on the 128x128 map
        // icon / 2.0 gives pixel offset from center
        // multiply by blocksPerPixel to get world offset from map center
        double pixelOffsetX = icon.getX() / 2.0;
        double pixelOffsetZ = icon.getZ() / 2.0;

        double worldOffsetX = pixelOffsetX * blocksPerPixel;
        double worldOffsetZ = pixelOffsetZ * blocksPerPixel;

        // mapCenter = playerPosition - worldOffset
        int estimatedCenterX = (int) Math.round(playerX - worldOffsetX);
        int estimatedCenterZ = (int) Math.round(playerZ - worldOffsetZ);

        centerCache.put(
            mapState,
            new EstimatedCenter(estimatedCenterX, estimatedCenterZ, true)
        );
    }

    /**
     * Try to estimate the map center from an off-map icon.
     * This is less accurate but allows distance to work for old maps
     * or maps that were zoomed out.
     *
     * When off-map, the icon position is clamped to the edge.
     * We can use this to estimate which direction the map center is.
     *
     * @param mapState   the map state
     * @param icon       the off-map player icon
     * @param playerX    the player's world X coordinate
     * @param playerZ    the player's world Z coordinate
     * @param scale      the map scale (0-4)
     */
    public static void estimateFromOffMapIcon(
        MapState mapState,
        MapIcon icon,
        double playerX,
        double playerZ,
        byte scale
    ) {
        // Don't overwrite a more accurate on-map estimation
        EstimatedCenter existing = centerCache.get(mapState);
        if (existing != null && existing.isAccurate) {
            return;
        }

        int blocksPerPixel = 1 << scale;
        int mapHalfSizeBlocks = 64 * blocksPerPixel; // Half the map size in world blocks

        byte iconX = icon.getX();
        byte iconZ = icon.getZ();

        int estimatedCenterX;
        int estimatedCenterZ;

        // When at edge (-128 or 127), we know the player is beyond the map edge
        // The center is approximately mapHalfSizeBlocks in the opposite direction
        // This is a rough estimate - will be corrected when player enters the map

        if (iconX <= -127) {
            // Player is off the left edge (negative X direction)
            estimatedCenterX = (int) Math.round(playerX + mapHalfSizeBlocks);
        } else if (iconX >= 127) {
            // Player is off the right edge (positive X direction)
            estimatedCenterX = (int) Math.round(playerX - mapHalfSizeBlocks);
        } else {
            // Player is within X bounds on map, calculate precisely
            double pixelOffsetX = iconX / 2.0;
            double worldOffsetX = pixelOffsetX * blocksPerPixel;
            estimatedCenterX = (int) Math.round(playerX - worldOffsetX);
        }

        if (iconZ <= -127) {
            // Player is off the top edge (negative Z direction)
            estimatedCenterZ = (int) Math.round(playerZ + mapHalfSizeBlocks);
        } else if (iconZ >= 127) {
            // Player is off the bottom edge (positive Z direction)
            estimatedCenterZ = (int) Math.round(playerZ - mapHalfSizeBlocks);
        } else {
            // Player is within Z bounds on map, calculate precisely
            double pixelOffsetZ = iconZ / 2.0;
            double worldOffsetZ = pixelOffsetZ * blocksPerPixel;
            estimatedCenterZ = (int) Math.round(playerZ - worldOffsetZ);
        }

        centerCache.put(
            mapState,
            new EstimatedCenter(estimatedCenterX, estimatedCenterZ, false)
        );
    }

    /**
     * Get the cached estimated center for a map.
     *
     * @param mapState the map state
     * @return the estimated center, or null if not yet calculated
     */
    public static EstimatedCenter getEstimatedCenter(MapState mapState) {
        return centerCache.get(mapState);
    }

    /**
     * Calculate the distance from the player to the estimated map center.
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
        EstimatedCenter center = centerCache.get(mapState);
        if (center == null) {
            return -1;
        }

        double dx = playerX - center.x;
        double dz = playerZ - center.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Check if we have a cached center for this map.
     *
     * @param mapState the map state
     * @return true if we have an estimated center
     */
    public static boolean hasCenter(MapState mapState) {
        return centerCache.containsKey(mapState);
    }

    /**
     * Clear all cached centers.
     */
    public static void clearCache() {
        centerCache.clear();
    }
}
