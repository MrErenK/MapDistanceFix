package com.mrerenk.mapdistancefix.network;

import com.mrerenk.mapdistancefix.MapdistancefixMod;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Handles network communication between client and server for map center data.
 * Server has the real centerX and centerZ values, which the client needs for distance calculation.
 *
 * This version is compatible with Minecraft 1.20.1 using PacketByteBuf.
 */
public class MapCenterNetworking {

    public static final Identifier MAP_CENTER_REQUEST_ID = new Identifier(
        "mapdistancefix",
        "map_center_request"
    );
    public static final Identifier MAP_CENTER_RESPONSE_ID = new Identifier(
        "mapdistancefix",
        "map_center_response"
    );

    // Rate limiting: Track last request time per player
    private static final Map<UUID, Long> lastRequestTime =
        new ConcurrentHashMap<>();
    private static final long REQUEST_COOLDOWN_MS = 1000; // 1 second cooldown per player
    private static final int MAX_DIMENSION_LENGTH = 256; // Prevent excessive string sizes

    /**
     * Gets map ID from ItemStack NBT data.
     */
    private static Integer getMapIdFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        if (!stack.isOf(Items.FILLED_MAP) && !stack.isOf(Items.MAP)) {
            return null;
        }

        try {
            if (stack.hasNbt() && stack.getNbt().contains("map")) {
                return stack.getNbt().getInt("map");
            }
        } catch (Exception e) {
            // Silently fail
        }

        return null;
    }

    /**
     * Register server-side packet handlers.
     * Called during server initialization.
     */
    public static void registerServerHandlers() {
        // Handle map center requests from clients
        ServerPlayNetworking.registerGlobalReceiver(
            MAP_CENTER_REQUEST_ID,
            (server, player, handler, buf, responseSender) -> {
                // Rate limiting check
                UUID playerId = player.getUuid();
                long currentTime = System.currentTimeMillis();
                Long lastRequest = lastRequestTime.get(playerId);

                if (
                    lastRequest != null &&
                    (currentTime - lastRequest) < REQUEST_COOLDOWN_MS
                ) {
                    // Too many requests - silently drop
                    MapdistancefixMod.LOGGER.debug(
                        "Rate limited map center request from player {}",
                        player.getName().getString()
                    );
                    return;
                }

                lastRequestTime.put(playerId, currentTime);

                // Read the map ID from the packet
                int mapId;
                try {
                    mapId = buf.readVarInt();

                    // Validate map ID is reasonable (prevent integer overflow attacks)
                    if (mapId < 0 || mapId > 1000000) {
                        MapdistancefixMod.LOGGER.warn(
                            "Invalid map ID {} from player {}",
                            mapId,
                            player.getName().getString()
                        );
                        return;
                    }
                } catch (Exception e) {
                    MapdistancefixMod.LOGGER.warn(
                        "Malformed packet from player {}",
                        player.getName().getString()
                    );
                    return;
                }

                server.execute(() -> {
                    try {
                        // Get the player's held item to verify they actually have this map
                        ItemStack heldItem = player.getMainHandStack();
                        if (
                            !heldItem.isOf(Items.FILLED_MAP) &&
                            !heldItem.isOf(Items.MAP)
                        ) {
                            // Check offhand
                            heldItem = player.getOffHandStack();
                            if (
                                !heldItem.isOf(Items.FILLED_MAP) &&
                                !heldItem.isOf(Items.MAP)
                            ) {
                                return; // Not holding a map
                            }
                        }

                        // Get the map ID from held item
                        Integer heldMapId = getMapIdFromStack(heldItem);
                        if (heldMapId == null || heldMapId != mapId) {
                            return; // Map ID mismatch
                        }

                        // Get the map state from the server world
                        String mapKey = "map_" + mapId;
                        MapState mapState = server
                            .getOverworld()
                            .getMapState(mapKey);

                        if (mapState != null) {
                            // Send the center coordinates back to the client
                            String dimension = mapState.dimension
                                .getValue()
                                .toString();

                            // Validate dimension string length to prevent excessive data
                            if (dimension.length() > MAX_DIMENSION_LENGTH) {
                                dimension = dimension.substring(
                                    0,
                                    MAX_DIMENSION_LENGTH
                                );
                            }

                            // Create response packet
                            PacketByteBuf responseBuf = PacketByteBufs.create();
                            responseBuf.writeVarInt(mapId);
                            responseBuf.writeVarInt(mapState.centerX);
                            responseBuf.writeVarInt(mapState.centerZ);
                            responseBuf.writeByte(mapState.scale);
                            responseBuf.writeString(dimension);

                            ServerPlayNetworking.send(
                                player,
                                MAP_CENTER_RESPONSE_ID,
                                responseBuf
                            );

                            MapdistancefixMod.LOGGER.debug(
                                "Sent map center to client: mapId={}, center=({}, {}), scale={}, dimension={}",
                                mapId,
                                mapState.centerX,
                                mapState.centerZ,
                                mapState.scale,
                                dimension
                            );
                        }
                    } catch (Exception e) {
                        MapdistancefixMod.LOGGER.error(
                            "Error handling map center request",
                            e
                        );
                    }
                });
            }
        );

        MapdistancefixMod.LOGGER.info(
            "Registered server-side map center networking"
        );
    }

    /**
     * Clean up old rate limit entries to prevent memory leak.
     * Should be called periodically or on player disconnect.
     */
    public static void cleanupRateLimitCache(ServerPlayerEntity player) {
        if (player != null) {
            lastRequestTime.remove(player.getUuid());
        }
    }

    /**
     * Clean up all entries older than the cooldown period.
     * Can be called periodically to prevent unbounded growth.
     */
    public static void cleanupOldEntries() {
        long currentTime = System.currentTimeMillis();
        lastRequestTime
            .entrySet()
            .removeIf(
                entry ->
                    (currentTime - entry.getValue()) > REQUEST_COOLDOWN_MS * 60 // Keep for 60x cooldown
            );
    }
}
