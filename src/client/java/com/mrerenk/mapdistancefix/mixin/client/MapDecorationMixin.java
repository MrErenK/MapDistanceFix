package com.mrerenk.mapdistancefix.mixin.client;

import com.mrerenk.mapdistancefix.client.MapdistancefixClient;
import com.mrerenk.mapdistancefix.util.MapDecorationUtils;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapDecoration.class)
public class MapDecorationMixin {

    private static final Identifier PLAYER_OFF_MAP_ID = Identifier.of(
        "minecraft",
        "player_off_map"
    );

    @Shadow
    @Final
    @Mutable
    private byte rotation;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void replaceOffMapPlayerType(
        RegistryEntry<MapDecorationType> type,
        byte x,
        byte z,
        byte rotation,
        Optional<Text> text,
        CallbackInfo ci
    ) {
        // Only process player_off_map decorations
        if (!type.matchesId(PLAYER_OFF_MAP_ID)) {
            return;
        }

        try {
            // Update rotation based on player's facing direction
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                this.rotation = MapDecorationUtils.calculateMapRotation(
                    client.player.getYaw()
                );
            }
        } catch (Exception e) {
            MapdistancefixClient.LOGGER.error(
                "Error updating off-map decoration rotation: {}",
                e.getMessage()
            );
        }
    }
}
