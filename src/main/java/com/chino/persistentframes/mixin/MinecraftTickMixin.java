package com.chino.persistentframes.mixin;

import com.chino.persistentframes.PreservationGuard;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftTickMixin {

    // One-shot chat banner once in a world: unmistakable confirmation the mod is loaded
    // (and that a missing banner means a stale/missing jar, not a rendering issue).
    @Inject(method = "tick", at = @At("TAIL"))
    private void persistentItemFrames$startupBanner(CallbackInfo ci) {
        PreservationGuard.showBannerOnce((Minecraft) (Object) this);
    }
}
