package com.mrerenk.mapdistancefix.mixin.client;

import com.mrerenk.mapdistancefix.config.ModConfig;
import com.mrerenk.mapdistancefix.util.MapCenterTracker;
import com.mrerenk.mapdistancefix.util.MapIconUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

        // First pass: look for on-map player icons to update the center tracker
        for (MapIcon icon : originalIcons) {
            if (icon.getType() == MapIcon.Type.PLAYER) {
                // Player is on the map - use this to estimate the map center
                MapCenterTracker.updateFromOnMapIcon(
                    self,
                    icon,
                    playerX,
                    playerZ,
                    scale
                );
                break; // Only need one on-map icon
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
        boolean hasValidDistance = distance >= 0;

        // Check if player is off-map based on estimated center
        boolean isOffMap = false;
        MapCenterTracker.EstimatedCenter center =
            MapCenterTracker.getEstimatedCenter(self);
        if (center != null) {
            double dx = playerX - center.x;
            double dz = playerZ - center.z;
            isOffMap = Math.abs(dx) > mapHalfSize || Math.abs(dz) > mapHalfSize;
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
                        hasValidDistance = distance >= 0;
                    }

                    Optional<MapIcon> convertedOpt =
                        MapIconUtils.convertOffMapIcon(icon, playerRotation);

                    if (convertedOpt.isPresent()) {
                        MapIcon converted = convertedOpt.get();

                        // Check if we should add distance text
                        if (config.isShowDistance() && hasValidDistance) {
                            // Show distance when:
                            // - showDistanceWhenOffMap is true AND player is off-map, OR
                            // - showDistanceInsideBoundaries is true (regardless of position)
                            boolean shouldShow =
                                (config.isShowDistanceWhenOffMap() &&
                                    isOffMap) ||
                                config.isShowDistanceInsideBoundaries();
                            if (shouldShow) {
                                MapIcon iconWithDistance =
                                    createIconWithDistance(
                                        converted,
                                        distance,
                                        config
                                    );
                                modifiedIcons.add(iconWithDistance);
                            } else {
                                modifiedIcons.add(converted);
                            }
                        } else {
                            modifiedIcons.add(converted);
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

                        // Add player icon with distance text
                        MapIcon iconWithDistance = createIconWithDistance(
                            icon,
                            distance,
                            config
                        );
                        modifiedIcons.add(iconWithDistance);
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
     * Helper method to create a MapIcon with distance text.
     */
    private MapIcon createIconWithDistance(
        MapIcon original,
        double distance,
        ModConfig config
    ) {
        String distanceText = config.formatDistance(distance);

        return new MapIcon(
            original.getType(),
            original.getX(),
            original.getZ(),
            original.getRotation(),
            Text.literal(distanceText)
        );
    }
}
