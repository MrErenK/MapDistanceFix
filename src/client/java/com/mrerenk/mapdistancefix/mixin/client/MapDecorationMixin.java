package com.mrerenk.mapdistancefix.mixin.client;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import com.mrerenk.mapdistancefix.util.MapDecorationUtils;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapDecoration.class)
public class MapDecorationMixin {

    @Shadow
    @Final
    @Mutable
    private RegistryEntry<MapDecorationType> type;

    @Shadow
    @Final
    @Mutable
    private byte rotation;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void convertOffMapPlayerDecoration(
        RegistryEntry<MapDecorationType> type,
        byte x,
        byte z,
        byte rotation,
        Optional<Text> text,
        CallbackInfo ci
    ) {
        // Only process player_off_map and player_off_limits decorations
        if (
            !type.equals(MapDecorationTypes.PLAYER_OFF_MAP) &&
            !type.equals(MapDecorationTypes.PLAYER_OFF_LIMITS)
        ) {
            return;
        }

        try {
            // Update both type and rotation
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                // Convert to regular player decoration type
                this.type = MapDecorationTypes.PLAYER;

                // Update rotation based on player's facing direction
                this.rotation = MapDecorationUtils.calculateMapRotation(
                    client.player.getYaw()
                );

                MapdistancefixClient.LOGGER.debug(
                    "Converted off-map decoration to player decoration with rotation: {}",
                    this.rotation
                );
            }
        } catch (Exception e) {
            MapdistancefixClient.LOGGER.error(
                "Error converting off-map decoration: {}",
                e.getMessage()
            );
        }
    }
}
