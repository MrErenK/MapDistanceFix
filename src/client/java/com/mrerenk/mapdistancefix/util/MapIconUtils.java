package com.mrerenk.mapdistancefix.util;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.item.map.MapIcon;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * Thread-safe utility class for managing map icons
 */
public final class MapIconUtils {

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

    // Thread-safe cache for player icon type using AtomicReference
    private static final AtomicReference<
        WeakReference<MapIcon.Type>
    > cachedPlayerTypeRef = new AtomicReference<>(null);

    /**
     * Enum for player icon types for better type safety
     */
    public enum PlayerIconType {
        PLAYER(MapIcon.Type.PLAYER, "player"),
        OFF_MAP(MapIcon.Type.PLAYER_OFF_MAP, "off-map"),
        OFF_LIMITS(MapIcon.Type.PLAYER_OFF_LIMITS, "off-limits");

        private final MapIcon.Type type;
        private final String displayName;

        PlayerIconType(MapIcon.Type type, String displayName) {
            this.type = type;
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static Optional<PlayerIconType> fromType(MapIcon.Type type) {
            for (PlayerIconType iconType : values()) {
                if (iconType.type.equals(type)) {
                    return Optional.of(iconType);
                }
            }
            return Optional.empty();
        }
    }

    private MapIconUtils() {
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
     * Helper method to get MapIcon type using getter only.
     */
    private static MapIcon.Type getIconType(MapIcon icon) {
        return icon.getType();
    }

    /**
     * Helper method to get MapIcon X coordinate using getter only.
     */
    private static byte getIconX(MapIcon icon) {
        return icon.getX();
    }

    /**
     * Helper method to get MapIcon Z coordinate using getter only.
     */
    private static byte getIconZ(MapIcon icon) {
        return icon.getZ();
    }

    /**
     * Helper method to get MapIcon text using getter only.
     */
    private static Text getIconText(MapIcon icon) {
        return icon.getText();
    }

    /**
     * Converts an off-map icon to a regular player icon
     */
    public static Optional<MapIcon> convertOffMapIcon(
        MapIcon original,
        byte newRotation
    ) {
        return Optional.ofNullable(original)
            .filter(MapIconUtils::isPlayerOffMapAny)
            .map(icon -> {
                PlayerIconType.fromType(getIconType(icon)).ifPresent(type ->
                    MapdistancefixClient.LOGGER.debug(
                        "Converting {} icon to player icon",
                        type.getDisplayName()
                    )
                );

                return new MapIcon(
                    getPlayerType(),
                    getIconX(icon),
                    getIconZ(icon),
                    newRotation,
                    getIconText(icon)
                );
            });
    }

    /**
     * Check if icon type should be converted
     */
    public static boolean shouldConvertIconType(MapIcon.Type type) {
        if (type == null) return false;

        return (
            type == MapIcon.Type.PLAYER_OFF_MAP ||
            type == MapIcon.Type.PLAYER_OFF_LIMITS
        );
    }

    /**
     * Check if icon is any type of off-map player (off_map or off_limits)
     */
    public static boolean isPlayerOffMapAny(MapIcon icon) {
        return icon != null && shouldConvertIconType(getIconType(icon));
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
     * Cache player type from icon for future use.
     * This helps avoid repeated lookups and ensures we use the correct player type
     * from the actual game instance.
     */
    public static void cachePlayerTypeFromIcon(MapIcon icon) {
        if (icon == null) {
            return;
        }

        // Only cache if it's a regular player icon (not off-map variants)
        MapIcon.Type iconType = getIconType(icon);
        if (iconType == MapIcon.Type.PLAYER) {
            WeakReference<MapIcon.Type> newRef = new WeakReference<>(iconType);
            cachedPlayerTypeRef.compareAndSet(null, newRef);

            MapdistancefixClient.LOGGER.debug(
                "Cached player icon type from game instance"
            );
        }
    }

    /**
     * Get the cached player type, falling back to the default if not cached
     */
    public static MapIcon.Type getPlayerType() {
        WeakReference<MapIcon.Type> ref = cachedPlayerTypeRef.get();
        if (ref != null) {
            MapIcon.Type cached = ref.get();
            if (cached != null) {
                return cached;
            }
            // Clean up dead reference
            cachedPlayerTypeRef.compareAndSet(ref, null);
        }

        return MapIcon.Type.PLAYER;
    }

    /**
     * Helper method to determine if a MapIcon.Type represents a player
     */
    public static boolean isPlayerType(MapIcon.Type type) {
        if (type == null) return false;

        return (
            type == MapIcon.Type.PLAYER ||
            type == MapIcon.Type.PLAYER_OFF_MAP ||
            type == MapIcon.Type.PLAYER_OFF_LIMITS
        );
    }
}
