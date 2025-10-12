package com.mrerenk.mapdistancefix.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapItemSavedData.class)
public class MapItemSavedDataMixin {

    @Shadow
    @Final
    @Mutable
    private boolean unlimitedTracking;

    // Force unlimitedTracking = true on construction (server-side)
    @Inject(method = "<init>", at = @At("RETURN"))
    private void enableUnlimitedTrackingServer(
        int centerX,
        int centerZ,
        byte scale,
        boolean showIcons,
        boolean unlimitedTracking,
        boolean locked,
        ResourceKey<?> dimension,
        CallbackInfo ci
    ) {
        this.unlimitedTracking = true;
    }
}
