package com.mrerenk.mapdistancefix.mixin.client;

import com.mrerenk.mapdistancefix.util.MapDecorationUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapState.class)
public class MapStateMixin {

    // This will only be applied if the method exists in the target version
    @Inject(
        method = "isInBounds",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void alwaysShowPlayer(
        float dx,
        float dz,
        CallbackInfoReturnable<Boolean> cir
    ) {
        // Check if we're in a player context
        if (MapDecorationUtils.isPlayerContext()) {
            cir.setReturnValue(true);
        }
    }

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

        List<MapDecoration> modifiedDecorations = null;
        byte playerRotation = 0;
        boolean rotationCalculated = false;

        MapDecorationUtils.setPlayerContext(true);
        try {
            for (MapDecoration decoration : originalDecorations) {
                MapDecorationUtils.cachePlayerTypeFromDecoration(decoration);

                if (MapDecorationUtils.isPlayerOffMapAny(decoration)) {
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

                    MapDecorationUtils.convertOffMapDecoration(
                        decoration,
                        playerRotation
                    ).ifPresent(modifiedDecorations::add);
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
}
