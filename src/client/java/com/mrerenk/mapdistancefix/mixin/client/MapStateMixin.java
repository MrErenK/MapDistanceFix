package com.mrerenk.mapdistancefix.mixin.client;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import com.mrerenk.mapdistancefix.config.ModConfig;
import com.mrerenk.mapdistancefix.network.MapCenterNetworkingClient;
import com.mrerenk.mapdistancefix.util.MapCenterTracker;
import com.mrerenk.mapdistancefix.util.MapIconUtils;
import com.mrerenk.mapdistancefix.util.MapItemHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapState.class)
public class MapStateMixin {

    // Track which MapStates we've already requested from the server
    // Using WeakHashMap to allow garbage collection when MapState is no longer referenced
    private static final Map<MapState, Long> requestedMaps =
        new WeakHashMap<>();

    // Rate limiting: minimum time between requests (in milliseconds)
    private static final long REQUEST_COOLDOWN_MS = 5000; // 5 seconds

    @Shadow
    @Final
    @Mutable
    private boolean unlimitedTracking;

    @Shadow
    @Final
    public byte scale;

    // Force unlimitedTracking = true on construction
    @Inject(method = "<init>", at = @At("RETURN"))
    private void enableUnlimitedTracking(
        int centerX,
        int centerZ,
        byte scale,
        boolean showIcons,
        boolean unlimitedTracking,
        boolean locked,
        RegistryKey<?> dimension,
        CallbackInfo ci
    ) {
        this.unlimitedTracking = true;
    }

    @Inject(method = "getIcons", at = @At("RETURN"), cancellable = true)
    private void convertOffMapPlayerIcons(
        CallbackInfoReturnable<Iterable<MapIcon>> cir
    ) {
        Iterable<MapIcon> originalIcons = cir.getReturnValue();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.player == null || originalIcons == null) {
            return;
        }

        MapState self = (MapState) (Object) this;
        double playerX = client.player.getX();
        double playerZ = client.player.getZ();
        ModConfig config = ModConfig.get();

        List<MapIcon> modifiedIcons = null;
        byte playerRotation = 0;
        boolean rotationCalculated = false;

        // Request map center from server if we don't have an accurate one yet
        // with rate limiting to prevent spam
        if (!MapCenterTracker.hasAccurateCenter(self)) {
            synchronized (requestedMaps) {
                Long lastRequestTime = requestedMaps.get(self);
                long currentTime = System.currentTimeMillis();

                // Only request if we haven't requested before, or if cooldown has passed
                if (
                    lastRequestTime == null ||
                    (currentTime - lastRequestTime) > REQUEST_COOLDOWN_MS
                ) {
                    requestMapCenterFromServer(client, self);
                    requestedMaps.put(self, currentTime);
                }
            }
        }

        // First pass: look for on-map player icons to update the center tracker
        for (MapIcon icon : originalIcons) {
            if (icon.getType() == MapIcon.Type.PLAYER) {
                MapCenterTracker.updateFromOnMapIcon(
                    self,
                    icon,
                    playerX,
                    playerZ,
                    scale
                );
                break;
            }
        }

        // Calculate map boundary (half the map size in blocks)
        // Map size = 128 * 2^scale blocks
        int mapHalfSize = 64 * (1 << scale);

        double distance = MapCenterTracker.calculateDistance(
            self,
            playerX,
            playerZ
        );
        boolean hasValidDistance = distance >= 0;

        boolean isOffMap = false;
        MapCenterTracker.MapCenter center = MapCenterTracker.getCenter(self);
        if (center != null) {
            double dx = playerX - center.x;
            double dz = playerZ - center.z;
            isOffMap = Math.abs(dx) > mapHalfSize || Math.abs(dz) > mapHalfSize;
        }

        // Second pass: find the nearest structure decoration distance to use as
        // the player arrow label. Falls back to map center distance if none exist.
        if (config.isShowStructureDistances() && center != null) {
            double nearestStructureDist = -1;
            for (MapIcon icon : originalIcons) {
                if (!MapIconUtils.isStructureDecoration(icon)) continue;
                double d = MapIconUtils.calculateStructureDistance(
                    icon,
                    playerX,
                    playerZ,
                    center,
                    scale
                );
                if (
                    d >= 0 &&
                    (nearestStructureDist < 0 || d < nearestStructureDist)
                ) {
                    nearestStructureDist = d;
                }
            }
            if (nearestStructureDist >= 0) {
                distance = nearestStructureDist;
                hasValidDistance = true;
            }
        }

        MapIconUtils.setPlayerContext(true);
        try {
            for (MapIcon icon : originalIcons) {
                MapIconUtils.cachePlayerTypeFromIcon(icon);

                if (MapIconUtils.isPlayerOffMapAny(icon)) {
                    // Handle off-map player icons
                    if (modifiedIcons == null) {
                        modifiedIcons = new ArrayList<>();
                        // Backfill previous icons
                        for (MapIcon prev : originalIcons) {
                            if (prev == icon) break;
                            modifiedIcons.add(prev);
                        }
                    }

                    if (!rotationCalculated) {
                        playerRotation = MapIconUtils.calculateMapRotation(
                            client.player.getYaw()
                        );
                        rotationCalculated = true;
                    }

                    // Try to estimate center from off-map icon if we don't have it yet
                    if (!MapCenterTracker.hasCenter(self)) {
                        MapCenterTracker.estimateFromOffMapIcon(
                            self,
                            icon,
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

                    Optional<MapIcon> convertedOpt =
                        MapIconUtils.convertOffMapIcon(icon, playerRotation);

                    if (convertedOpt.isPresent()) {
                        MapIcon converted = convertedOpt.get();

                        // Always add the player arrow at its correct position first
                        modifiedIcons.add(converted);

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
                                addDistanceIcon(
                                    modifiedIcons,
                                    converted,
                                    distance,
                                    config
                                );
                            }
                        }
                    }
                } else if (icon.getType() == MapIcon.Type.PLAYER) {
                    // Handle on-map player icons - show distance inside boundaries if configured
                    if (
                        config.isShowDistance() &&
                        config.isShowDistanceInsideBoundaries() &&
                        hasValidDistance
                    ) {
                        if (modifiedIcons == null) {
                            modifiedIcons = new ArrayList<>();
                            // Backfill previous icons
                            for (MapIcon prev : originalIcons) {
                                if (prev == icon) break;
                                modifiedIcons.add(prev);
                            }
                        }

                        // Add the original player icon
                        modifiedIcons.add(icon);

                        // Add distance text
                        addDistanceIcon(modifiedIcons, icon, distance, config);
                    } else if (modifiedIcons != null) {
                        modifiedIcons.add(icon);
                    }
                } else if (modifiedIcons != null) {
                    modifiedIcons.add(icon);
                }
            }

            if (modifiedIcons != null) {
                cir.setReturnValue(modifiedIcons);
            }
        } finally {
            MapIconUtils.clearPlayerContext();
        }
    }

    /**
     * Helper method to add distance text to the player icon.
     */
    private void addDistanceIcon(
        List<MapIcon> icons,
        MapIcon playerIcon,
        double distance,
        ModConfig config
    ) {
        String distanceText = config.formatDistance(distance);

        // Replace the icon we just added with one that has text
        icons.remove(icons.size() - 1);
        MapIcon arrowWithText = new MapIcon(
            playerIcon.getType(),
            playerIcon.getX(),
            playerIcon.getZ(),
            playerIcon.getRotation(),
            Text.literal(distanceText)
        );
        icons.add(arrowWithText);
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
