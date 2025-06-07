package com.mrerenk.mapdistancefix.util;

import java.util.Optional;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/**
 * Manages decoration type replacement for map distance fix
 */
public class DecorationScaleManager {

    private static final Identifier PLAYER_ID = Identifier.of(
        "minecraft",
        "player"
    );
    private static final Identifier PLAYER_OFF_MAP_ID = Identifier.of(
        "minecraft",
        "player_off_map"
    );

    private static RegistryEntry<MapDecorationType> cachedPlayerType;

    /**
     * Creates a player decoration with proper type and rotation
     */
    public static MapDecoration createPlayerDecoration(
        byte x,
        byte z,
        byte rotation
    ) {
        try {
            // Get or cache the player decoration type
            if (cachedPlayerType == null) {
                var playerTypeOptional =
                    Registries.MAP_DECORATION_TYPE.getEntry(PLAYER_ID);
                if (playerTypeOptional.isEmpty()) {
                    return null;
                }
                cachedPlayerType = playerTypeOptional.get();
            }

            return new MapDecoration(
                cachedPlayerType,
                x,
                z,
                rotation,
                Optional.empty()
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts an off-map decoration to a regular player decoration
     */
    public static MapDecoration convertOffMapDecoration(
        MapDecoration original,
        byte newRotation
    ) {
        if (!original.type().matchesId(PLAYER_OFF_MAP_ID)) {
            return original;
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
}
