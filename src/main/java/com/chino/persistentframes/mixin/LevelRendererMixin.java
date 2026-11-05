package com.chino.persistentframes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// 1.21.11 has no net.minecraft.client.renderer.extract.LevelExtractor: the "only extract entities
// whose chunk section has a compiled mesh" logic is inlined in
// LevelRenderer#extractVisibleEntities(Camera, Frustum, DeltaTracker, LevelRenderState), where it
// gates each entity on this.isSectionCompiledAndVisible(entity.blockPosition()).
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    // Vanilla only extracts entities whose chunk section has a compiled render mesh. When the
    // server unloads a chunk (low view distance) and no chunk-cache mod (Bobby etc.) serves data
    // for it, the section stays uncompiled and preserved item frames silently stop rendering --
    // even while LOD mods (Distant Horizons) keep drawing approximate terrain.
    // Item frames carry their own geometry and never depend on the chunk mesh, so re-admit any
    // culled frame that still passes the regular frustum + render-distance checks. Those checks
    // (EntityRenderDispatcher.shouldRender) already ran earlier in the same loop iteration, so
    // making the section-mesh gate pass for frames is exactly the 26.2 LevelExtractor behavior.
    // isSectionCompiledAndVisible has exactly one caller (this loop), so the wrap is well-scoped.
    @WrapOperation(
            method = "extractVisibleEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean persistentItemFrames$renderFramesWithoutCompiledSection(
            LevelRenderer self, BlockPos pos, Operation<Boolean> original, @Local Entity entity) {
        if (entity instanceof ItemFrame) {
            return true;
        }
        return original.call(self, pos);
    }
}
