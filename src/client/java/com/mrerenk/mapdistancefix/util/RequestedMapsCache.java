package com.mrerenk.mapdistancefix.util;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.item.map.MapState;

/**
 * Manages the cache of MapStates that have already had their centers requested from the server.
 * Using WeakHashMap to allow MapState instances to be garbage collected.
 */
public final class RequestedMapsCache {

    // Track which MapStates we've already requested from the server
    // Using WeakHashMap to allow MapState instances to be garbage collected
    // Synchronized map wrapper for thread safety
    private static final Map<MapState, Boolean> requestedMaps =
        Collections.synchronizedMap(new WeakHashMap<>());

    private RequestedMapsCache() {
        // Utility class
    }

    /**
     * Check if a map center has been requested for this MapState.
     */
    public static boolean hasRequested(MapState mapState) {
        return requestedMaps.containsKey(mapState);
    }

    /**
     * Mark a MapState as having been requested.
     */
    public static void markRequested(MapState mapState) {
        requestedMaps.put(mapState, Boolean.TRUE);
    }

    /**
     * Clear the requested maps cache.
     * Called when disconnecting from a server or clearing cache.
     */
    public static void clearRequestedMaps() {
        requestedMaps.clear();
    }
}
