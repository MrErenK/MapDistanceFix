package com.mrerenk.mapdistancefix.network;

import com.mrerenk.mapdistancefix.MapdistancefixMod;
import com.mrerenk.mapdistancefix.util.ComponentHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Handles network communication between client and server for map center data.
 * Server has the real centerX and centerZ values, which the client needs for distance calculation.
 */
public class MapCenterNetworking {

    public static final Identifier MAP_CENTER_REQUEST_ID = Identifier.tryParse(
        "mapdistancefix:map_center_request"
    );
    public static final Identifier MAP_CENTER_RESPONSE_ID = Identifier.tryParse(
        "mapdistancefix:map_center_response"
    );

    /**
     * Request packet sent from client to server asking for map center coordinates.
     */
    public record MapCenterRequestPayload(int mapId) implements CustomPayload {
        public static final CustomPayload.Id<MapCenterRequestPayload> ID =
            new CustomPayload.Id<>(MAP_CENTER_REQUEST_ID);
        public static final PacketCodec<
            RegistryByteBuf,
            MapCenterRequestPayload
        > CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT,
            MapCenterRequestPayload::mapId,
            MapCenterRequestPayload::new
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Response packet sent from server to client with map center data.
     */
    public record MapCenterResponsePayload(
        int mapId,
        int centerX,
        int centerZ,
        byte scale,
        String dimension
    ) implements CustomPayload {
        public static final CustomPayload.Id<MapCenterResponsePayload> ID =
            new CustomPayload.Id<>(MAP_CENTER_RESPONSE_ID);
        public static final PacketCodec<
            RegistryByteBuf,
            MapCenterResponsePayload
        > CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT,
            MapCenterResponsePayload::mapId,
            PacketCodecs.VAR_INT,
            MapCenterResponsePayload::centerX,
            PacketCodecs.VAR_INT,
            MapCenterResponsePayload::centerZ,
            PacketCodecs.BYTE,
            MapCenterResponsePayload::scale,
            PacketCodecs.STRING,
            MapCenterResponsePayload::dimension,
            MapCenterResponsePayload::new
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Register server-side packet handlers.
     * Called during server initialization.
     */
    public static void registerServerHandlers() {
        // Register the payload types
        PayloadTypeRegistry.playC2S().register(
            MapCenterRequestPayload.ID,
            MapCenterRequestPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
            MapCenterResponsePayload.ID,
            MapCenterResponsePayload.CODEC
        );

        // Handle map center requests from clients
        ServerPlayNetworking.registerGlobalReceiver(
            MapCenterRequestPayload.ID,
            (payload, context) -> {
                int mapId = payload.mapId();

                context
                    .server()
                    .execute(() -> {
                        try {
                            // Get the player's held item to verify they actually have this map
                            ItemStack heldItem = context
                                .player()
                                .getMainHandStack();
                            if (
                                !heldItem.isOf(Items.FILLED_MAP) &&
                                !heldItem.isOf(Items.MAP)
                            ) {
                                // Check offhand
                                heldItem = context.player().getOffHandStack();
                                if (
                                    !heldItem.isOf(Items.FILLED_MAP) &&
                                    !heldItem.isOf(Items.MAP)
                                ) {
                                    return; // Not holding a map
                                }
                            }

                            // Get the map component using ComponentHelper for cross-version compatibility
                            Integer heldMapId = ComponentHelper.getMapId(
                                heldItem
                            );
                            if (heldMapId == null || heldMapId != mapId) {
                                return; // Map ID mismatch
                            }

                            // Get the MapIdComponent for getMapState call
                            // We need to construct this properly - use reflection to get it
                            MapIdComponent mapIdComponent = null;
                            try {
                                Class<?> mapIdComponentClass = Class.forName(
                                    "net.minecraft.class_9209"
                                );
                                java.lang.reflect.Constructor<?> constructor =
                                    mapIdComponentClass.getConstructor(
                                        int.class
                                    );
                                mapIdComponent =
                                    (MapIdComponent) constructor.newInstance(
                                        mapId
                                    );
                            } catch (Exception e) {
                                MapdistancefixMod.LOGGER.error(
                                    "Failed to create MapIdComponent",
                                    e
                                );
                                return;
                            }

                            // Get the map state from the server world
                            MapState mapState = context
                                .server()
                                .getOverworld()
                                .getMapState(mapIdComponent);

                            if (mapState != null) {
                                // Send the center coordinates back to the client
                                String dimension = mapState.dimension
                                    .getValue()
                                    .toString();
                                MapCenterResponsePayload response =
                                    new MapCenterResponsePayload(
                                        mapId,
                                        mapState.centerX,
                                        mapState.centerZ,
                                        mapState.scale,
                                        dimension
                                    );

                                ServerPlayNetworking.send(
                                    context.player(),
                                    response
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
}
