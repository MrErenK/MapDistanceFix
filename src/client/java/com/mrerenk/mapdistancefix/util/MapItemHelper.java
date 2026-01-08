package com.mrerenk.mapdistancefix.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/**
 * Helper class for handling map item operations.
 * This class delegates to ComponentHelper for cross-version compatible component access.
 */
public class MapItemHelper {

    /**
     * Gets the map ID from an ItemStack.
     *
     * @param stack The ItemStack to check
     * @return The map ID if present, null otherwise
     */
    public static Integer getMapId(ItemStack stack) {
        return ComponentHelper.getMapId(stack);
    }

    /**
     * Gets the map ID from the currently held item in the player's hands.
     * Checks main hand first, then off hand.
     *
     * @param client The Minecraft client instance
     * @return The map ID if a map is held, null otherwise
     */
    public static Integer getHeldMapId(MinecraftClient client) {
        if (client == null || client.player == null) {
            return null;
        }

        // Check main hand
        ItemStack mainHand = client.player.getMainHandStack();
        Integer mapId = getMapId(mainHand);
        if (mapId != null) {
            return mapId;
        }

        // Check off hand
        ItemStack offHand = client.player.getOffHandStack();
        return getMapId(offHand);
    }
}
