package com.mrerenk.mapdistancefix.mixin;

import com.mrerenk.mapdistancefix.config.ModConfig;
import com.mrerenk.mapdistancefix.util.MapItemSavedDataUtils;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapItemSavedData.class)
public abstract class MapItemSavedDataMixin {

    @Shadow
    @Final
    @Mutable
    private boolean unlimitedTracking;

    @Shadow
    @Final
    Map<String, MapDecoration> decorations;

    @Shadow
    @Final
    public ResourceKey<Level> dimension;

    @Shadow
    @Final
    public int centerX;

    @Shadow
    @Final
    public int centerZ;

    @Shadow
    @Final
    public byte scale;

    private LevelAccessor mapdistancefix$world;
    private double mapdistancefix$actualX;
    private double mapdistancefix$actualZ;
    private double mapdistancefix$yaw;

    // Force unlimitedTracking = true on construction to prevent the marker from disappearing out of bounds
    @Inject(method = "<init>", at = @At("RETURN"))
    private void forceUnlimitedTracking(CallbackInfo ci) {
        this.unlimitedTracking = true;
    }

    // Capture arguments from the addDecoration method call
    @Inject(method = "addDecoration", at = @At("HEAD"))
    private void mapdistancefix$captureArgs(
        MapDecoration.Type type,
        LevelAccessor world,
        String id,
        double x,
        double z,
        double rot,
        Component name,
        CallbackInfo ci
    ) {
        this.mapdistancefix$world = world;
        this.mapdistancefix$actualX = x;
        this.mapdistancefix$actualZ = z;
        this.mapdistancefix$yaw = rot;
    }

    // Clear captured arguments after the addDecoration method call
    @Inject(method = "addDecoration", at = @At("RETURN"))
    private void mapdistancefix$clearArgs(
        MapDecoration.Type type,
        LevelAccessor world,
        String id,
        double x,
        double z,
        double rot,
        Component name,
        CallbackInfo ci
    ) {
        this.mapdistancefix$world = null;
    }

    // Intercept any out-of-bounds player markers and force them to be the normal PLAYER icon
    @Redirect(
        method = "addDecoration",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/world/level/saveddata/maps/MapDecoration$Type;BBBLnet/minecraft/network/chat/Component;)Lnet/minecraft/world/level/saveddata/maps/MapDecoration;"
        )
    )
    private MapDecoration mapdistancefix$redirectNewMapDecoration(
        MapDecoration.Type decorationType,
        byte x,
        byte y,
        byte rot,
        Component nameOptional
    ) {
        ModConfig config = ModConfig.get();
        boolean isOffMapPlayer =
            decorationType == MapDecoration.Type.PLAYER_OFF_MAP ||
            decorationType == MapDecoration.Type.PLAYER_OFF_LIMITS;
        boolean isNormalPlayer = decorationType == MapDecoration.Type.PLAYER;

        if (isOffMapPlayer || isNormalPlayer) {
            boolean showDistance = false;
            if (config.isShowDistance()) {
                if (isOffMapPlayer && config.isShowDistanceWhenOffMap()) {
                    showDistance = true;
                } else if (
                    isNormalPlayer && config.isShowDistanceInsideBoundaries()
                ) {
                    showDistance = true;
                }
            }

            if (showDistance) {
                double targetX = this.centerX;
                double targetZ = this.centerZ;

                double closestDistanceSq = Double.MAX_VALUE;

                if (config.isShowStructureDistances()) {
                    for (MapDecoration dec : this.decorations.values()) {
                        if (
                            MapItemSavedDataUtils.isSupportedStructure(
                                dec.getType()
                            )
                        ) {
                            int blocksPerPixel = 1 << this.scale;
                            // Convert from signed byte map coordinates (-128 to 127) to world coordinates
                            // The exact center of the map is map coordinate 0,0, which corresponds to this.centerX, this.centerZ
                            // The map coordinates are scaled by 2 compared to the actual block offset at scale=0
                            double structWorldX =
                                ((dec.getX() * blocksPerPixel) / 2.0) +
                                this.centerX;
                            double structWorldZ =
                                ((dec.getY() * blocksPerPixel) / 2.0) +
                                this.centerZ;

                            double dx =
                                this.mapdistancefix$actualX - structWorldX;
                            double dz =
                                this.mapdistancefix$actualZ - structWorldZ;
                            double distSq = dx * dx + dz * dz;

                            if (distSq < closestDistanceSq) {
                                closestDistanceSq = distSq;
                                targetX = structWorldX;
                                targetZ = structWorldZ;
                            }
                        }
                    }
                }

                int distance = MapItemSavedDataUtils.calculateDistance(
                    this.mapdistancefix$actualX,
                    this.mapdistancefix$actualZ,
                    (int) targetX,
                    (int) targetZ
                );

                Component newName = MapItemSavedDataUtils.appendDistanceToName(
                    nameOptional,
                    distance,
                    config
                );
                nameOptional = newName;
            }
        }

        if (isOffMapPlayer) {
            byte calculatedRot = MapItemSavedDataUtils.calculateRotation(
                this.mapdistancefix$world,
                this.mapdistancefix$yaw,
                this.dimension
            );
            return new MapDecoration(
                MapDecoration.Type.PLAYER,
                x,
                y,
                calculatedRot,
                nameOptional
            );
        }
        return new MapDecoration(decorationType, x, y, rot, nameOptional);
    }
}
