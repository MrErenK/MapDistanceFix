package com.mrerenk.mapdistancefix.util;

import com.mojang.logging.LogUtils;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import org.slf4j.Logger;

/**
 * Thread-safe utility class for managing map decorations
 */
public final class MapDecorationUtils {

    private static final Logger LOGGER = LogUtils.getLogger();

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
        WeakReference<MapDecoration.Type>
    > cachedPlayerTypeRef = new AtomicReference<>(null);

    /**
     * Enum for player icon types for better type safety
     */
    public enum PlayerIconType {
        PLAYER(MapDecoration.Type.PLAYER, "player"),
        OFF_MAP(MapDecoration.Type.PLAYER_OFF_MAP, "off-map"),
        OFF_LIMITS(MapDecoration.Type.PLAYER_OFF_LIMITS, "off-limits");

        private final MapDecoration.Type type;
        private final String displayName;

        PlayerIconType(MapDecoration.Type type, String displayName) {
            this.type = type;
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static Optional<PlayerIconType> fromType(
            MapDecoration.Type type
        ) {
            for (PlayerIconType iconType : values()) {
                if (iconType.type.equals(type)) {
                    return Optional.of(iconType);
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
        int normalizedYaw = ((int) Mth.wrapDegrees(playerYaw) + 360) % 360;
        return ROTATION_LOOKUP[normalizedYaw];
    }

    /**
     * Helper method to get MapDecoration type using getter only.
     */
    private static MapDecoration.Type getIconType(MapDecoration icon) {
        return icon.getType();
    }

    /**
     * Helper method to get MapDecoration X coordinate using getter only.
     */
    private static byte getIconX(MapDecoration icon) {
        return icon.getX();
    }

    /**
     * Helper method to get MapDecoration Y coordinate using getter only.
     */
    private static byte getIconY(MapDecoration icon) {
        return icon.getY();
    }

    /**
     * Helper method to get MapDecoration text using getter only.
     */
    private static Component getIconText(MapDecoration icon) {
        return icon.getName();
    }

    /**
     * Converts an off-map icon to a regular player icon
     */
    public static Optional<MapDecoration> convertOffMapIcon(
        MapDecoration original,
        byte newRotation
    ) {
        return Optional.ofNullable(original)
            .filter(MapDecorationUtils::isPlayerOffMapAny)
            .map(icon -> {
                PlayerIconType.fromType(getIconType(icon)).ifPresent(type ->
                    LOGGER.debug(
                        "Converting {} icon to player icon",
                        type.getDisplayName()
                    )
                );

                return new MapDecoration(
                    getPlayerType(),
                    getIconX(icon),
                    getIconY(icon),
                    newRotation,
                    getIconText(icon)
                );
            });
    }

    /**
     * Check if icon type should be converted
     */
    public static boolean shouldConvertIconType(MapDecoration.Type type) {
        if (type == null) return false;

        return (
            type == MapDecoration.Type.PLAYER_OFF_MAP ||
            type == MapDecoration.Type.PLAYER_OFF_LIMITS
        );
    }

    /**
     * Check if icon is any type of off-map player (off_map or off_limits)
     */
    public static boolean isPlayerOffMapAny(MapDecoration icon) {
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
                className.contains("MapItemSavedData") ||
                className.contains("class_22")
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
    public static void cachePlayerTypeFromIcon(MapDecoration icon) {
        if (icon == null) {
            return;
        }

        // Only cache if it's a regular player icon (not off-map variants)
        MapDecoration.Type iconType = getIconType(icon);
        if (iconType == MapDecoration.Type.PLAYER) {
            WeakReference<MapDecoration.Type> newRef = new WeakReference<>(
                iconType
            );
            cachedPlayerTypeRef.compareAndSet(null, newRef);

            LOGGER.debug("Cached player icon type from game instance");
        }
    }

    /**
     * Get the cached player type, falling back to the default if not cached
     */
    public static MapDecoration.Type getPlayerType() {
        WeakReference<MapDecoration.Type> ref = cachedPlayerTypeRef.get();
        if (ref != null) {
            MapDecoration.Type cached = ref.get();
            if (cached != null) {
                return cached;
            }
            // Clean up dead reference
            cachedPlayerTypeRef.compareAndSet(ref, null);
        }

        return MapDecoration.Type.PLAYER;
    }

    /**
     * Helper method to determine if a MapDecoration.Type represents a player
     */
    public static boolean isPlayerType(MapDecoration.Type type) {
        if (type == null) return false;

        return (
            type == MapDecoration.Type.PLAYER ||
            type == MapDecoration.Type.PLAYER_OFF_MAP ||
            type == MapDecoration.Type.PLAYER_OFF_LIMITS
        );
    }
}
