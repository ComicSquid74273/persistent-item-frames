package com.chino.persistentframes.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    // Item frames must never be frustum/occlusion culled: culling mods (e.g. EntityCulling)
    // consult this exact method and otherwise replace the extracted render state with an
    // invisible dummy, which would swallow preserved frames behind vanilla's distance checks.
    // Vanilla's render-distance cap still applies (checked before this method).
    @Inject(method = "affectedByCulling", at = @At("HEAD"), cancellable = true)
    private void persistentItemFrames$neverCullFrames(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ItemFrame) {
            cir.setReturnValue(false);
        }
    }

    // In chunks the server unloaded (and no chunk-cache mod serves data for), the light engine
    // reports full darkness, so preserved frames would render pitch black. If a frame sits in a
    // section without a compiled mesh AND both light channels are 0 -- a combination that cannot
    // happen in a normally loaded chunk that is merely dark (its section is compiled) -- fall
    // back to daylight. Sky light 15 is still dimmed by the vanilla lightmap at night.
    @Inject(method = "getPackedLightCoords", at = @At("RETURN"), cancellable = true)
    private void persistentItemFrames$daylightInUnloadedChunks(T entity, float partialTickTime, CallbackInfoReturnable<Integer> cir) {
        if (!(entity instanceof ItemFrame) || cir.getReturnValueI() != 0) {
            return;
        }

        LevelRenderer levelRenderer = Minecraft.getInstance().levelRenderer;
        if (levelRenderer != null && !levelRenderer.isSectionCompiledAndVisible(entity.blockPosition())) {
            cir.setReturnValue(LightCoordsUtil.pack(0, 15));
        }
    }
}
