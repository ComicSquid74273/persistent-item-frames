package com.chino.persistentframes.mixin;

import com.chino.persistentframes.DebugLog;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftTickMixin {

    // Diagnostic (1.1.2-debug1): drives the periodic frame audit without pulling in
    // fabric-api event hooks — keeps the mod a pure-mixin, zero-entrypoint build.
    @Inject(method = "tick", at = @At("TAIL"))
    private void persistentItemFrames$audit(CallbackInfo ci) {
        DebugLog.audit((Minecraft) (Object) this);
    }
}
