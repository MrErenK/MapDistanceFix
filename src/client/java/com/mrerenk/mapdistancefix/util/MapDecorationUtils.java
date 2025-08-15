package com.mrerenk.mapdistancefix.util;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;

/**
 * Thread-safe utility class for managing map decorations
 */
public final class MapDecorationUtils {

    // Constants
    public static final float DEGREES_PER_ROTATION = 22.5f;
    public static final int ROTATION_MASK = 15;

    // Pre-calculated rotation lookup table for better performance
    private static final byte[] ROTATION_LOOKUP = new byte[360];

    // Thread-local variable to track player context
    private static final ThreadLocal<Boolean> IS_PLAYER_CONTEXT =
        ThreadLocal.withInitial(() -> false);

    static {
        // Pre-calculate all possible rotations
        for (int i = 0; i < 360; i++) {
            ROTATION_LOOKUP[i] = (byte) (Math.round(i / DEGREES_PER_ROTATION) &
                ROTATION_MASK);
        }
    }

    // Thread-safe cache for player decoration type using AtomicReference
    private static final AtomicReference<
        WeakReference<RegistryEntry<MapDecorationType>>
    > cachedPlayerTypeRef = new AtomicReference<>(null);

    /**
     * Enum for player decoration types for better type safety
     */
    public enum PlayerDecorationType {
        PLAYER(MapDecorationTypes.PLAYER, "player"),
        OFF_MAP(MapDecorationTypes.PLAYER_OFF_MAP, "off-map"),
        OFF_LIMITS(MapDecorationTypes.PLAYER_OFF_LIMITS, "off-limits");

        private final RegistryEntry<MapDecorationType> type;
        private final String displayName;

        PlayerDecorationType(
            RegistryEntry<MapDecorationType> type,
            String displayName
        ) {
            this.type = type;
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static Optional<PlayerDecorationType> fromType(
            RegistryEntry<MapDecorationType> type
        ) {
            for (PlayerDecorationType decorationType : values()) {
                if (decorationType.type.equals(type)) {
                    return Optional.of(decorationType);
                }
            }
            return Optional.empty();
        }
    }

    private MapDecorationUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts player yaw to map rotation (0-15 range) using pre-calculated lookup table
     */
    public static byte calculateMapRotation(float playerYaw) {
        int normalizedYaw =
            ((int) MathHelper.wrapDegrees(playerYaw) + 360) % 360;
        return ROTATION_LOOKUP[normalizedYaw];
    }

    /**
     * Converts an off-map decoration to a regular player decoration
     */
    public static Optional<MapDecoration> convertOffMapDecoration(
        MapDecoration original,
        byte newRotation
    ) {
        return Optional.ofNullable(original)
            .filter(MapDecorationUtils::isPlayerOffMapAny)
            .map(decoration -> {
                PlayerDecorationType.fromType(decoration.type()).ifPresent(
                    type ->
                        MapdistancefixClient.LOGGER.debug(
                            "Converting {} decoration to player decoration",
                            type.getDisplayName()
                        )
                );

                return new MapDecoration(
                    getPlayerType(),
                    decoration.x(),
                    decoration.z(),
                    newRotation,
                    decoration.name()
                );
            });
    }

    /**
     * Check if decoration type should be converted
     */
    public static boolean shouldConvertDecorationType(
        RegistryEntry<MapDecorationType> type
    ) {
        if (type == null) return false;

        if (
            type == MapDecorationTypes.PLAYER_OFF_MAP ||
            type == MapDecorationTypes.PLAYER_OFF_LIMITS
        ) {
            return true;
        }

        return (
            type.equals(MapDecorationTypes.PLAYER_OFF_MAP) ||
            type.equals(MapDecorationTypes.PLAYER_OFF_LIMITS)
        );
    }

    /**
     * Check if decoration is any type of off-map player (off_map or off_limits)
     */
    public static boolean isPlayerOffMapAny(MapDecoration decoration) {
        return (
            decoration != null && shouldConvertDecorationType(decoration.type())
        );
    }

    /**
     * Check if we're currently in a player context
     * This is used by mixins to determine if they should apply player-specific behavior
     * First checks ThreadLocal, then falls back to stack trace inspection for compatibility
     *
     * @return true if currently in player context, false otherwise
     */
    public static boolean isPlayerContext() {
        if (IS_PLAYER_CONTEXT.get()) {
            return true;
        }

        // Fallback to stack trace inspection for compatibility
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            String className = element.getClassName();

            if (
                className.contains("MapState") || className.contains("class_22")
            ) {
                return true;
            }
        }

        return false;
    }

    /**
     * Manually set player context
     *
     * @param inPlayerContext whether we're in player context
     */
    public static void setPlayerContext(boolean inPlayerContext) {
        IS_PLAYER_CONTEXT.set(inPlayerContext);
    }

    /**
     * Clear the player context for the current thread
     */
    public static void clearPlayerContext() {
        IS_PLAYER_CONTEXT.remove();
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
            WeakReference<RegistryEntry<MapDecorationType>> newRef =
                new WeakReference<>(decoration.type());

            cachedPlayerTypeRef.compareAndSet(null, newRef);

            MapdistancefixClient.LOGGER.debug(
                "Cached player decoration type from game instance"
            );
        }
    }

    /**
     * Get the cached player type, falling back to the default if not cached
     */
    public static RegistryEntry<MapDecorationType> getPlayerType() {
        WeakReference<RegistryEntry<MapDecorationType>> ref =
            cachedPlayerTypeRef.get();
        if (ref != null) {
            RegistryEntry<MapDecorationType> cached = ref.get();
            if (cached != null) {
                return cached;
            }
            // Clean up dead reference
            cachedPlayerTypeRef.compareAndSet(ref, null);
        }
        return MapDecorationTypes.PLAYER;
    }

}
