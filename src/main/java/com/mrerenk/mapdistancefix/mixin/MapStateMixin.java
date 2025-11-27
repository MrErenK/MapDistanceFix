package com.mrerenk.mapdistancefix.mixin;

import java.util.Map;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapState.class)
public class MapStateMixin {

    @Shadow
    @Final
    @Mutable
    private boolean unlimitedTracking;

    @Shadow
    @Final
    Map<String, MapDecoration> decorations;

    @Shadow
    @Final
    public RegistryKey<World> dimension;

    // Force unlimitedTracking = true on construction
    @Inject(
        method = "<init>(IIBZZZLnet/minecraft/registry/RegistryKey;)V",
        at = @At("RETURN")
    )
    private void enableUnlimitedTrackingServer(
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

    // Disables the spinning indicator in the Nether by making the game think we're in the Overworld
    // In newer versions (1.21.2+), the check is in getPlayerMarkerRotation
    // In older versions (1.21.1-1.20.5), the check is in addDecoration
    // We target both with require=0 so it works across versions
    @Redirect(
        method = "getPlayerMarkerRotation",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/item/map/MapState;dimension:Lnet/minecraft/registry/RegistryKey;"
        ),
        require = 0
    )
    private RegistryKey<World> redirectDimensionCheckNew(MapState instance) {
        // Always return OVERWORLD to prevent the spinning indicator in Nether
        if (this.dimension == World.NETHER) {
            return World.OVERWORLD;
        }
        return this.dimension;
    }

    // For older versions where the dimension check is in addDecoration
    @Redirect(
        method = "addDecoration",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/item/map/MapState;dimension:Lnet/minecraft/registry/RegistryKey;"
        ),
        require = 0
    )
    private RegistryKey<World> redirectDimensionCheckOld(MapState instance) {
        // Always return OVERWORLD to prevent the spinning indicator in Nether
        if (this.dimension == World.NETHER) {
            return World.OVERWORLD;
        }
        return this.dimension;
    }
}
