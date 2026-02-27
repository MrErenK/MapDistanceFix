package com.mrerenk.mapdistancefix.mixin;

import java.util.Map;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
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

    @Shadow
    @Final
    public int centerX;

    @Shadow
    @Final
    public int centerZ;

    @Shadow
    @Final
    public byte scale;

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

    /**
     * Suppress FRAME decorations that fall outside the map's visible area.
     *
     * When unlimitedTracking is forced on, vanilla's getMarker() still creates a
     * Marker for non-PLAYER types (e.g. FRAME) even when their world coordinates
     * are outside the map bounds — it just clamps them to the edge and keeps the
     * original type. This means every item frame that holds a copy of this map,
     * but sits outside the map's coverage area, gets a green teardrop icon pinned
     * to the nearest edge, which is the unwanted behaviour visible in the
     * "More complex map display" screenshot.
     *
     * The fix: inject at the HEAD of the private addDecoration() method. If the
     * decoration type is FRAME and the supplied world coordinates are outside the
     * map bounds, cancel the call so no decoration is stored. This preserves
     * FRAME markers for item frames that are actually within the map area.
     *
     * addDecoration signature:
     *   (RegistryEntry type, WorldAccess world, String key,
     *    double x, double z, double yaw, Text name)
     */
    @Inject(
        method = "addDecoration(Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/world/WorldAccess;Ljava/lang/String;DDDLnet/minecraft/text/Text;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void suppressOutOfBoundsFrameDecorations(
        RegistryEntry<?> type,
        WorldAccess world,
        String key,
        double x,
        double z,
        double yaw,
        Text name,
        CallbackInfo ci
    ) {
        // Only filter FRAME decorations — player markers are handled elsewhere.
        if (!type.equals(MapDecorationTypes.FRAME)) {
            return;
        }

        // Compute the per-pixel block size for this map's scale.
        int blocksPerPixel = 1 << this.scale;

        // Mirror the offset calculation used by addDecoration itself:
        //   offsetX = (x - centerX) / blocksPerPixel
        // isInBounds checks [-63, 63] for both axes.
        float offsetX = (float) ((x - this.centerX) / blocksPerPixel);
        float offsetZ = (float) ((z - this.centerZ) / blocksPerPixel);

        boolean inBounds =
            offsetX >= -63f &&
            offsetX <= 63f &&
            offsetZ >= -63f &&
            offsetZ <= 63f;

        if (!inBounds) {
            // Remove any stale decoration stored under this key so it doesn't
            // linger from a previous tick when the frame was in range.
            decorations.remove(key);
            ci.cancel();
        }
    }
}
