package com.mrerenk.mapdistancefix.mixin.client;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import com.mrerenk.mapdistancefix.util.MapDecorationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private void ensurePlayerDecorationExists(
        CallbackInfoReturnable<Iterable<MapDecoration>> cir
    ) {
        var decorations = cir.getReturnValue();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.player == null || decorations == null) {
            return;
        }

        try {
            PlayerEntity player = client.player;
            boolean hasPlayerDecoration = false;
            boolean needsModification = false;
            List<MapDecoration> modifiedDecorations = null;

            // First pass: extract player type from existing decorations and check what we have
            for (MapDecoration decoration : decorations) {
                // Cache player decoration type for future use
                MapDecorationUtils.cachePlayerTypeFromDecoration(decoration);

                if (
                    MapDecorationUtils.isPlayer(decoration) ||
                    MapDecorationUtils.isPlayerOffMap(decoration)
                ) {
                    hasPlayerDecoration = true;
                    if (MapDecorationUtils.isPlayerOffMap(decoration)) {
                        needsModification = true;
                        break;
                    }
                }
            }

            // If no player decoration exists and we have a cached player type, we need to add one
            if (
                !hasPlayerDecoration &&
                MapDecorationUtils.getPlayerDecorationType().isPresent()
            ) {
                needsModification = true;
            }

            // Only create new list if modifications are needed
            if (needsModification) {
                modifiedDecorations = new ArrayList<>();
                byte playerRotation = MapDecorationUtils.calculateMapRotation(
                    player.getYaw()
                );

                // Process existing decorations
                for (MapDecoration decoration : decorations) {
                    if (MapDecorationUtils.isPlayerOffMap(decoration)) {
                        // Convert off-map to regular player decoration
                        MapDecorationUtils.convertOffMapDecoration(
                            decoration,
                            playerRotation
                        ).ifPresent(modifiedDecorations::add);
                    } else {
                        modifiedDecorations.add(decoration);
                    }
                }

                // Add forced player decoration if none exists and we have a cached player type
                if (
                    !hasPlayerDecoration &&
                    MapDecorationUtils.getPlayerDecorationType().isPresent()
                ) {
                    createForcedPlayerDecoration(player).ifPresent(
                        modifiedDecorations::add
                    );
                }

                cir.setReturnValue(modifiedDecorations);
            }
        } catch (Exception e) {
            MapdistancefixClient.LOGGER.error(
                "Error ensuring player decoration exists",
                e
            );
        }
    }

    private Optional<MapDecoration> createForcedPlayerDecoration(
        PlayerEntity player
    ) {
        try {
            var edgePos = MapDecorationUtils.calculateEdgePosition(
                player.getX(),
                player.getZ()
            );
            byte rotation = MapDecorationUtils.calculateMapRotation(
                player.getYaw()
            );

            return MapDecorationUtils.createPlayerDecoration(
                edgePos.x(),
                edgePos.z(),
                rotation
            );
        } catch (Exception e) {
            MapdistancefixClient.LOGGER.error(
                "Error creating forced player decoration",
                e
            );
            return Optional.empty();
        }
    }
}
