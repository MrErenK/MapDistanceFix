package com.mrerenk.mapdistancefix.mixin;

import com.mrerenk.mapdistancefix.config.ModConfig;
import com.mrerenk.mapdistancefix.util.MapItemSavedDataUtils;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
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

    @Inject(method = "addDecoration", at = @At("HEAD"))
    private void mapdistancefix$captureArgs(
        Holder<MapDecorationType> type,
        LevelAccessor world,
        String key,
        double x,
        double z,
        double yaw,
        Component name,
        CallbackInfo ci
    ) {
        this.mapdistancefix$world = world;
        this.mapdistancefix$actualX = x;
        this.mapdistancefix$actualZ = z;
        this.mapdistancefix$yaw = yaw;
    }

    @Inject(method = "addDecoration", at = @At("RETURN"))
    private void mapdistancefix$clearArgs(
        Holder<MapDecorationType> type,
        LevelAccessor world,
        String key,
        double x,
        double z,
        double yaw,
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
            target = "(Lnet/minecraft/core/Holder;BBBLjava/util/Optional;)Lnet/minecraft/world/level/saveddata/maps/MapDecoration;"
        )
    )
    private MapDecoration mapdistancefix$redirectNewMapDecoration(
        Holder<MapDecorationType> decorationType,
        byte x,
        byte y,
        byte rot,
        Optional<Component> nameOptional
    ) {
        ModConfig config = ModConfig.get();
        boolean isOffMapPlayer =
            decorationType
                .value()
                .equals(MapDecorationTypes.PLAYER_OFF_MAP.value()) ||
            decorationType
                .value()
                .equals(MapDecorationTypes.PLAYER_OFF_LIMITS.value());
        boolean isNormalPlayer = decorationType
            .value()
            .equals(MapDecorationTypes.PLAYER.value());

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
                                dec.type()
                            )
                        ) {
                            int blocksPerPixel = 1 << this.scale;
                            // Convert from signed byte map coordinates (-128 to 127) to world coordinates
                            // The exact center of the map is map coordinate 0,0, which corresponds to this.centerX, this.centerZ
                            // The map coordinates are scaled by 2 compared to the actual block offset at scale=0
                            double structWorldX =
                                ((dec.x() * blocksPerPixel) / 2.0) +
                                this.centerX;
                            double structWorldZ =
                                ((dec.y() * blocksPerPixel) / 2.0) +
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
                    nameOptional.orElse(null),
                    distance,
                    config
                );
                nameOptional = Optional.of(newName);
            }
        }

        if (isOffMapPlayer) {
            byte calculatedRot = MapItemSavedDataUtils.calculateRotation(
                this.mapdistancefix$world,
                this.mapdistancefix$yaw,
                this.dimension
            );
            return new MapDecoration(
                MapDecorationTypes.PLAYER,
                x,
                y,
                calculatedRot,
                nameOptional
            );
        }
        return new MapDecoration(decorationType, x, y, rot, nameOptional);
    }

    // Disables the spinning indicator in the Nether by making the game think we're in the Overworld
    @Redirect(
        method = "calculateRotation",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;dimension:Lnet/minecraft/resources/ResourceKey;"
        ),
        require = 0
    )
    private ResourceKey<Level> redirectDimensionCheckNew(
        MapItemSavedData instance
    ) {
        // Always return OVERWORLD to prevent the spinning indicator in Nether
        if (this.dimension == Level.NETHER) {
            return Level.OVERWORLD;
        }
        return this.dimension;
    }

    // For older versions where the dimension check is in addDecoration
    @Redirect(
        method = "addDecoration",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;dimension:Lnet/minecraft/resources/ResourceKey;"
        ),
        require = 0
    )
    private ResourceKey<Level> redirectDimensionCheckOld(
        MapItemSavedData instance
    ) {
        // Always return OVERWORLD to prevent the spinning indicator in Nether
        if (this.dimension == Level.NETHER) {
            return Level.OVERWORLD;
        }
        return this.dimension;
    }

    // To fix multiple decorations in item frames
    @Inject(method = "addDecoration", at = @At("HEAD"), cancellable = true)
    private void suppressOutOfBoundsFrameDecorations(
        Holder<MapDecorationType> type,
        LevelAccessor world,
        String key,
        double x,
        double z,
        double yaw,
        Component name,
        CallbackInfo ci
    ) {
        int blocksPerPixel = 1 << this.scale;
        float offsetX = (float) ((x - this.centerX) / blocksPerPixel);
        float offsetZ = (float) ((z - this.centerZ) / blocksPerPixel);

        if (MapItemSavedDataUtils.isOutOfBoundsFrame(type, offsetX, offsetZ)) {
            this.decorations.remove(key);
            ci.cancel();
        }
    }
}
