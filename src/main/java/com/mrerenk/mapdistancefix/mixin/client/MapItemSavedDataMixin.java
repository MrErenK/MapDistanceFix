package com.mrerenk.mapdistancefix.mixin.client;

import com.mrerenk.mapdistancefix.util.MapDecorationUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapItemSavedData.class)
public class MapItemSavedDataMixin {

    @Shadow
    @Final
    @Mutable
    private boolean unlimitedTracking;

    // Force unlimitedTracking = true on construction
    @Inject(method = "<init>", at = @At("RETURN"))
    private void enableUnlimitedTracking(
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

    @Inject(method = "getDecorations", at = @At("RETURN"), cancellable = true)
    private void convertOffMapPlayerIcons(
        CallbackInfoReturnable<Iterable<MapDecoration>> cir
    ) {
        Iterable<MapDecoration> originalIcons = cir.getReturnValue();
        Minecraft client = Minecraft.getInstance();

        if (client == null || client.player == null || originalIcons == null) {
            return;
        }

        List<MapDecoration> modifiedIcons = null;
        byte playerRotation = 0;
        boolean rotationCalculated = false;

        MapDecorationUtils.setPlayerContext(true);
        try {
            for (MapDecoration icon : originalIcons) {
                MapDecorationUtils.cachePlayerTypeFromIcon(icon);

                if (MapDecorationUtils.isPlayerOffMapAny(icon)) {
                    if (modifiedIcons == null) {
                        modifiedIcons = new ArrayList<>();
                        // Backfill previous icons
                        for (MapDecoration prev : originalIcons) {
                            if (prev == icon) break;
                            modifiedIcons.add(prev);
                        }
                    }

                    if (!rotationCalculated) {
                        playerRotation =
                            MapDecorationUtils.calculateMapRotation(
                                client.player.getYRot()
                            );
                        rotationCalculated = true;
                    }

                    MapDecorationUtils.convertOffMapIcon(
                        icon,
                        playerRotation
                    ).ifPresent(modifiedIcons::add);
                } else if (modifiedIcons != null) {
                    modifiedIcons.add(icon);
                }
            }

            if (modifiedIcons != null) {
                cir.setReturnValue(modifiedIcons);
            }
        } finally {
            MapDecorationUtils.clearPlayerContext();
        }
    }
}
