package com.mrerenk.mapdistancefix.mixin;

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
    public RegistryKey<World> dimension;

    // Force unlimitedTracking = true on construction (server-side)
    @Inject(method = "<init>", at = @At("RETURN"))
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
    @Redirect(
        method = "addIcon",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/item/map/MapState;dimension:Lnet/minecraft/registry/RegistryKey;"
        )
    )
    private RegistryKey<World> redirectDimensionCheck(MapState instance) {
        // When in the Nether, return OVERWORLD to prevent the spinning indicator
        if (this.dimension == World.NETHER) {
            return World.OVERWORLD;
        }
        return this.dimension;
    }
}
