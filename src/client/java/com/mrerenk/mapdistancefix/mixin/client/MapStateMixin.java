package com.mrerenk.mapdistancefix.mixin.client;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import com.mrerenk.mapdistancefix.config.ModConfig;
import com.mrerenk.mapdistancefix.network.MapCenterNetworkingClient;
import com.mrerenk.mapdistancefix.util.MapCenterTracker;
import com.mrerenk.mapdistancefix.util.MapDecorationUtils;
import com.mrerenk.mapdistancefix.util.MapItemHelper;
import com.mrerenk.mapdistancefix.util.RequestedMapsCache;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.item.map.MapState;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapState.class)
public class MapStateMixin {

    @Shadow
    @Final
    public byte scale;

    @Inject(method = "getDecorations", at = @At("RETURN"), cancellable = true)
    private void convertOffMapPlayerDecorations(
        CallbackInfoReturnable<Iterable<MapDecoration>> cir
    ) {
        Iterable<MapDecoration> originalDecorations = cir.getReturnValue();
        MinecraftClient client = MinecraftClient.getInstance();

        if (
            client == null ||
            client.player == null ||
            originalDecorations == null
        ) {
            return;
        }

        MapState self = (MapState) (Object) this;
        double playerX = client.player.getX();
        double playerZ = client.player.getZ();
        ModConfig config = ModConfig.get();

        // Request map center from server if we don't have an accurate one yet (and haven't requested it before)
        if (
            !MapCenterTracker.hasAccurateCenter(self) &&
            !RequestedMapsCache.hasRequested(self)
        ) {
            requestMapCenterFromServer(client, self);
            RequestedMapsCache.markRequested(self);
        }

        List<MapDecoration> modifiedDecorations = null;
        byte playerRotation = 0;
        boolean rotationCalculated = false;

        // First pass: look for on-map player decorations to update the center tracker
        for (MapDecoration decoration : originalDecorations) {
            if (decoration.type().equals(MapDecorationTypes.PLAYER)) {
                // Player is on the map - use this to estimate the map center
                MapCenterTracker.updateFromOnMapDecoration(
                    self,
                    decoration,
                    playerX,
                    playerZ,
                    scale
                );
                break; // Only need one on-map decoration
            }
        }
        // Calculate map boundary (half the map size in blocks)
        // Map size = 128 * 2^scale blocks
        int mapHalfSize = 64 * (1 << scale);

        // Calculate distance using the tracked center
        double distance = MapCenterTracker.calculateDistance(
            self,
            playerX,
            playerZ
        );
        // Show distance if we have any center (estimated or accurate from server)
        boolean hasValidDistance = distance >= 0;

        // Check if player is off-map based on the map center
        boolean isOffMap = false;
        MapCenterTracker.MapCenter center = MapCenterTracker.getCenter(self);
        if (center != null) {
            double dx = playerX - center.x;
            double dz = playerZ - center.z;
            isOffMap = Math.abs(dx) > mapHalfSize || Math.abs(dz) > mapHalfSize;
        }

        MapDecorationUtils.setPlayerContext(true);
        try {
            for (MapDecoration decoration : originalDecorations) {
                MapDecorationUtils.cachePlayerTypeFromDecoration(decoration);

                if (MapDecorationUtils.isPlayerOffMapAny(decoration)) {
                    // Handle off-map player decorations
                    if (modifiedDecorations == null) {
                        modifiedDecorations = new ArrayList<>();
                        // Backfill previous decorations
                        for (MapDecoration prev : originalDecorations) {
                            if (prev == decoration) break;
                            modifiedDecorations.add(prev);
                        }
                    }

                    if (!rotationCalculated) {
                        playerRotation =
                            MapDecorationUtils.calculateMapRotation(
                                client.player.getYaw()
                            );
                        rotationCalculated = true;
                    }

                    // Try to estimate center from off-map decoration if we don't have it yet
                    if (!MapCenterTracker.hasCenter(self)) {
                        MapCenterTracker.estimateFromOffMapDecoration(
                            self,
                            decoration,
                            playerX,
                            playerZ,
                            scale
                        );
                        // Recalculate distance after estimation
                        distance = MapCenterTracker.calculateDistance(
                            self,
                            playerX,
                            playerZ
                        );
                        // Update hasValidDistance after estimation
                        hasValidDistance = distance >= 0;

                        // Recalculate if player is off-map with the new center
                        center = MapCenterTracker.getCenter(self);
                        if (center != null) {
                            double dx = playerX - center.x;
                            double dz = playerZ - center.z;
                            isOffMap =
                                Math.abs(dx) > mapHalfSize ||
                                Math.abs(dz) > mapHalfSize;
                        }
                    }

                    Optional<MapDecoration> convertedOpt =
                        MapDecorationUtils.convertOffMapDecoration(
                            decoration,
                            playerRotation
                        );

                    if (convertedOpt.isPresent()) {
                        MapDecoration converted = convertedOpt.get();

                        // Always add the player arrow at its correct position first
                        modifiedDecorations.add(converted);

                        // Then add distance text if configured
                        if (config.isShowDistance() && hasValidDistance) {
                            // Show distance when:
                            // - showDistanceWhenOffMap is true AND player is off-map, OR
                            // - showDistanceInsideBoundaries is true (regardless of position)
                            boolean shouldShow =
                                (config.isShowDistanceWhenOffMap() &&
                                    isOffMap) ||
                                config.isShowDistanceInsideBoundaries();
                            if (shouldShow) {
                                addDistanceDecoration(
                                    modifiedDecorations,
                                    converted,
                                    distance,
                                    config
                                );
                            }
                        }
                    }
                } else if (
                    decoration.type().equals(MapDecorationTypes.PLAYER)
                ) {
                    // Handle on-map player decorations - show distance inside boundaries if configured
                    if (
                        config.isShowDistance() &&
                        config.isShowDistanceInsideBoundaries() &&
                        hasValidDistance
                    ) {
                        if (modifiedDecorations == null) {
                            modifiedDecorations = new ArrayList<>();
                            // Backfill previous decorations
                            for (MapDecoration prev : originalDecorations) {
                                if (prev == decoration) break;
                                modifiedDecorations.add(prev);
                            }
                        }

                        // Add the original player decoration
                        modifiedDecorations.add(decoration);

                        // Add distance text
                        addDistanceDecoration(
                            modifiedDecorations,
                            decoration,
                            distance,
                            config
                        );
                    } else if (modifiedDecorations != null) {
                        modifiedDecorations.add(decoration);
                    }
                } else if (modifiedDecorations != null) {
                    modifiedDecorations.add(decoration);
                }
            }

            if (modifiedDecorations != null) {
                cir.setReturnValue(modifiedDecorations);
            }
        } finally {
            MapDecorationUtils.clearPlayerContext();
        }
    }

    /**
     * Helper method to add distance text to the player decoration.
     */
    private void addDistanceDecoration(
        List<MapDecoration> decorations,
        MapDecoration playerDecoration,
        double distance,
        ModConfig config
    ) {
        String distanceText = config.formatDistance(distance);

        // Replace the decoration we just added with one that has text
        decorations.remove(decorations.size() - 1);
        MapDecoration arrowWithText = new MapDecoration(
            playerDecoration.type(),
            playerDecoration.x(),
            playerDecoration.z(),
            playerDecoration.rotation(),
            Optional.of(Text.literal(distanceText))
        );
        decorations.add(arrowWithText);
    }

    /**
     * Request map center from the server for the currently held map.
     */
    private void requestMapCenterFromServer(
        MinecraftClient client,
        MapState mapState
    ) {
        try {
            Integer mapId = MapItemHelper.getHeldMapId(client);
            if (mapId != null) {
                MapCenterNetworkingClient.requestMapCenter(mapId);
            }
        } catch (Exception e) {
            MapdistancefixClient.LOGGER.debug(
                "Failed to request map center from server",
                e
            );
        }
    }
}
