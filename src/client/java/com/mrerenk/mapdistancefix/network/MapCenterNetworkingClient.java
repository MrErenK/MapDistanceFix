package com.mrerenk.mapdistancefix.network;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import com.mrerenk.mapdistancefix.util.MapCenterTracker;
import com.mrerenk.mapdistancefix.util.MapItemHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.network.PacketByteBuf;

/**
 * Client-side networking handler for receiving map center data from the server.
 * Compatible with Minecraft 1.20.1 using PacketByteBuf.
 */
public class MapCenterNetworkingClient {

    /**
     * Register client-side packet handlers.
     * Called during client initialization.
     */
    public static void registerClientHandlers() {
        // Handle map center responses from server
        ClientPlayNetworking.registerGlobalReceiver(
            MapCenterNetworking.MAP_CENTER_RESPONSE_ID,
            (client, handler, buf, responseSender) -> {
                // Read packet data on network thread
                int mapId = buf.readVarInt();
                int centerX = buf.readVarInt();
                int centerZ = buf.readVarInt();
                byte scale = buf.readByte();
                String dimension = buf.readString();

                // Process on main thread
                client.execute(() -> {
                    try {
                        if (client.player == null || client.world == null) {
                            return;
                        }

                        // Try to find the map state by checking held items
                        MapState mapState = findMapStateById(client, mapId);

                        if (mapState != null) {
                            // Create a MapCenter with the server-provided data
                            MapCenterTracker.MapCenter center =
                                new MapCenterTracker.MapCenter(
                                    centerX,
                                    centerZ,
                                    true, // Server data is always accurate
                                    dimension,
                                    scale
                                );

                            // Store it in the tracker
                            MapCenterTracker.setCenter(mapState, center);

                            MapdistancefixClient.LOGGER.info(
                                "Received map center from server: mapId={}, center=({}, {}), scale={}, dimension={}",
                                mapId,
                                centerX,
                                centerZ,
                                scale,
                                dimension
                            );
                        } else {
                            MapdistancefixClient.LOGGER.warn(
                                "Received map center for unknown map ID: {}",
                                mapId
                            );
                        }
                    } catch (Exception e) {
                        MapdistancefixClient.LOGGER.error(
                            "Error handling map center response",
                            e
                        );
                    }
                });
            }
        );

        MapdistancefixClient.LOGGER.info(
            "Registered client-side map center networking"
        );
    }

    /**
     * Request map center data from the server for a specific map.
     *
     * @param mapId the map ID
     */
    public static void requestMapCenter(int mapId) {
        if (
            ClientPlayNetworking.canSend(
                MapCenterNetworking.MAP_CENTER_REQUEST_ID
            )
        ) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(mapId);

            ClientPlayNetworking.send(
                MapCenterNetworking.MAP_CENTER_REQUEST_ID,
                buf
            );

            MapdistancefixClient.LOGGER.debug(
                "Requested map center from server for map ID: {}",
                mapId
            );
        }
    }

    /**
     * Find a MapState instance by checking the player's held items and inventory.
     *
     * @param client the Minecraft client
     * @param mapId the map ID to find
     * @return the MapState, or null if not found
     */
    private static MapState findMapStateById(
        MinecraftClient client,
        int mapId
    ) {
        if (client.player == null || client.world == null) {
            return null;
        }

        // Check main hand
        ItemStack mainHand = client.player.getMainHandStack();
        MapState mapState = getMapStateFromStack(mainHand, mapId, client);
        if (mapState != null) {
            return mapState;
        }

        // Check off hand
        ItemStack offHand = client.player.getOffHandStack();
        mapState = getMapStateFromStack(offHand, mapId, client);
        if (mapState != null) {
            return mapState;
        }

        // Check inventory
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            mapState = getMapStateFromStack(stack, mapId, client);
            if (mapState != null) {
                return mapState;
            }
        }

        return null;
    }

    /**
     * Get the MapState from an ItemStack if it matches the given map ID.
     */
    private static MapState getMapStateFromStack(
        ItemStack stack,
        int mapId,
        MinecraftClient client
    ) {
        if (
            stack.isEmpty() ||
            (!stack.isOf(Items.FILLED_MAP) && !stack.isOf(Items.MAP))
        ) {
            return null;
        }

        Integer stackMapId = MapItemHelper.getMapId(stack);
        if (stackMapId != null && stackMapId == mapId) {
            return client.world.getMapState("map_" + mapId);
        }

        return null;
    }
}
