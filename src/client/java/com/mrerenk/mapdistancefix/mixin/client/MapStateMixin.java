package com.mrerenk.mapdistancefix.mixin.client;

import com.mrerenk.mapdistancefix.util.MapIconUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.RegistryKey;
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

        List<MapIcon> modifiedIcons = null;
        byte playerRotation = 0;
        boolean rotationCalculated = false;

        MapIconUtils.setPlayerContext(true);
        try {
            for (MapIcon icon : originalIcons) {
                MapIconUtils.cachePlayerTypeFromIcon(icon);

                if (MapIconUtils.isPlayerOffMapAny(icon)) {
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

                    MapIconUtils.convertOffMapIcon(
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
            MapIconUtils.clearPlayerContext();
        }
    }
}
