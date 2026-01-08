package com.mrerenk.mapdistancefix.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;

/**
 * Helper class for handling map item operations.
 */
public class MapItemHelper {

    /**
     * Gets the map ID from an ItemStack.
     *
     * @param stack The ItemStack to check
     * @return The map ID if present, null otherwise
     */
    public static Integer getMapId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        if (!stack.isOf(Items.FILLED_MAP) && !stack.isOf(Items.MAP)) {
            return null;
        }

        try {
            // Try to get map ID using the MapState.getId method
            // This works by getting the NBT data from the item
            if (stack.hasNbt() && stack.getNbt().contains("map")) {
                return stack.getNbt().getInt("map");
            }
        } catch (Exception e) {
            // Silently fail - map might not have ID yet
        }

        return null;
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

    /**
     * Gets the MapState from an ItemStack.
     *
     * @param stack The ItemStack to check
     * @param client The Minecraft client instance
     * @return The MapState if present, null otherwise
     */
    public static MapState getMapState(ItemStack stack, MinecraftClient client) {
        if (client == null || client.world == null) {
            return null;
        }

        Integer mapId = getMapId(stack);
        if (mapId == null) {
            return null;
        }

        return client.world.getMapState("map_" + mapId);
    }

    /**
     * Gets the currently held MapState from the player's hands.
     *
     * @param client The Minecraft client instance
     * @return The MapState if a map is held, null otherwise
     */
    public static MapState getHeldMapState(MinecraftClient client) {
        if (client == null || client.player == null) {
            return null;
        }

        // Check main hand
        MapState mapState = getMapState(client.player.getMainHandStack(), client);
        if (mapState != null) {
            return mapState;
        }

        // Check off hand
        return getMapState(client.player.getOffHandStack(), client);
    }
}
