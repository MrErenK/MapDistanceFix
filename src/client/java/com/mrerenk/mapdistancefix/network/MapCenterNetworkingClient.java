package com.mrerenk.mapdistancefix.network;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import com.mrerenk.mapdistancefix.util.ComponentHelper;
import com.mrerenk.mapdistancefix.util.MapCenterTracker;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;

/**
 * Client-side networking handler for receiving map center data from the server.
 */
public class MapCenterNetworkingClient {

    /**
     * Register client-side packet handlers.
     * Called during client initialization.
     */
    public static void registerClientHandlers() {
        // Handle map center responses from server
        ClientPlayNetworking.registerGlobalReceiver(
            MapCenterNetworking.MapCenterResponsePayload.ID,
            (payload, context) -> {
                context
                    .client()
                    .execute(() -> {
                        try {
                            // Find the MapState instance for this map ID
                            MinecraftClient client = context.client();
                            if (client.player == null || client.world == null) {
                                return;
                            }

                            // Try to find the map state by checking held items
                            MapState mapState = findMapStateById(
                                client,
                                payload.mapId()
                            );

                            if (mapState != null) {
                                // Create a MapCenter with the server-provided data
                                MapCenterTracker.MapCenter center =
                                    new MapCenterTracker.MapCenter(
                                        payload.centerX(),
                                        payload.centerZ(),
                                        true, // Server data is always accurate
                                        payload.dimension(),
                                        payload.scale()
                                    );

                                // Store it in the tracker
                                MapCenterTracker.setCenter(mapState, center);

                                MapdistancefixClient.LOGGER.info(
                                    "Received map center from server: mapId={}, center=({}, {}), scale={}, dimension={}",
                                    payload.mapId(),
                                    payload.centerX(),
                                    payload.centerZ(),
                                    payload.scale(),
                                    payload.dimension()
                                );
                            } else {
                                MapdistancefixClient.LOGGER.warn(
                                    "Received map center for unknown map ID: {}",
                                    payload.mapId()
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
                MapCenterNetworking.MapCenterRequestPayload.ID
            )
        ) {
            MapCenterNetworking.MapCenterRequestPayload payload =
                new MapCenterNetworking.MapCenterRequestPayload(mapId);
            ClientPlayNetworking.send(payload);

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

        // Use ComponentHelper to get map ID for cross-version compatibility
        Integer stackMapId = ComponentHelper.getMapId(stack);
        if (stackMapId != null && stackMapId == mapId) {
            // Need to create MapIdComponent for getMapState call
            try {
                Class<?> mapIdComponentClass = Class.forName(
                    "net.minecraft.class_9209"
                );
                java.lang.reflect.Constructor<?> constructor =
                    mapIdComponentClass.getConstructor(int.class);
                MapIdComponent mapIdComponent =
                    (MapIdComponent) constructor.newInstance(mapId);
                return client.world.getMapState(mapIdComponent);
            } catch (Exception e) {
                MapdistancefixClient.LOGGER.error(
                    "Failed to create MapIdComponent",
                    e
                );
            }
        }

        return null;
    }
}
