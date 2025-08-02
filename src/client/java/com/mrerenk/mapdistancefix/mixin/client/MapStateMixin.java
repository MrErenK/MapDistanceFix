package com.mrerenk.mapdistancefix.mixin.client;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import com.mrerenk.mapdistancefix.util.MapDecorationUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapState.class)
public class MapStateMixin {

    @Inject(method = "getDecorations", at = @At("RETURN"), cancellable = true)
    private void convertOffMapPlayerDecorations(
        CallbackInfoReturnable<Iterable<MapDecoration>> cir
    ) {
        var decorations = cir.getReturnValue();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.player == null || decorations == null) {
            return;
        }

        try {
            PlayerEntity player = client.player;
            boolean needsModification = false;
            List<MapDecoration> modifiedDecorations = null;

            // First pass: check if we need to modify any decorations
            for (MapDecoration decoration : decorations) {
                // Cache player decoration type for future use
                MapDecorationUtils.cachePlayerTypeFromDecoration(decoration);

                if (MapDecorationUtils.isPlayerOffMapAny(decoration)) {
                    needsModification = true;
                    break;
                }
            }

            // Only create new list if modifications are needed
            if (needsModification) {
                modifiedDecorations = new ArrayList<>();
                byte playerRotation = MapDecorationUtils.calculateMapRotation(
                    player.getYaw()
                );

                // Process existing decorations
                for (MapDecoration decoration : decorations) {
                    if (MapDecorationUtils.isPlayerOffMapAny(decoration)) {
                        // Convert off-map to regular player decoration, keeping original position
                        MapDecorationUtils.convertOffMapDecoration(
                            decoration,
                            playerRotation
                        ).ifPresent(modifiedDecorations::add);
                    } else {
                        modifiedDecorations.add(decoration);
                    }
                }

                cir.setReturnValue(modifiedDecorations);
            }
        } catch (Exception e) {
            MapdistancefixClient.LOGGER.error(
                "Error converting off-map player decorations",
                e
            );
        }
    }
}
