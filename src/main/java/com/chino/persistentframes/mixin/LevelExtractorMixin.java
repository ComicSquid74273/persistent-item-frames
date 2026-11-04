package com.chino.persistentframes.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {

    @Shadow
    private ClientLevel level;

    @Shadow
    @Final
    private LevelRenderer levelRenderer;

    // Vanilla only extracts entities whose chunk section has a compiled render mesh. When the
    // server unloads a chunk (low view distance) and no chunk-cache mod (Bobby etc.) serves data
    // for it, the section stays uncompiled and preserved item frames silently stop rendering --
    // even while LOD mods (Distant Horizons) keep drawing approximate terrain.
    // Item frames carry their own geometry and never depend on the chunk mesh, so re-admit any
    // culled frame that still passes the regular frustum + render-distance checks.
    @Inject(method = "isEntityVisible", at = @At("TAIL"), cancellable = true)
    private void persistentItemFrames$renderWithoutCompiledSection(Entity entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || this.level == null || !(entity instanceof ItemFrame)) {
            return;
        }

        if (this.levelRenderer.entityRenderDispatcher().shouldRender(entity, frustum, camX, camY, camZ)) {
            com.chino.persistentframes.DebugLog.RENDER_BYPASS_RESCUES.incrementAndGet();
            cir.setReturnValue(true);
        }
    }
}
