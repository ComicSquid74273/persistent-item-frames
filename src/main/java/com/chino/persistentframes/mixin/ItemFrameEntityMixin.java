package com.chino.persistentframes.mixin;

import com.chino.persistentframes.DebugLog;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrame.class)
public abstract class ItemFrameEntityMixin {

    // Diagnostic (1.1.2-debug1): every path that removes a frame client-side funnels through
    // setRemoved (ClientLevel.removeEntity, discard(), packet handlers). Log the caller chain
    // unless the mod itself decided the removal or the whole level is being torn down.
    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void persistentItemFrames$traceRemovals(Entity.RemovalReason reason, CallbackInfo ci) {
        ItemFrame frame = (ItemFrame) (Object) this;
        if (frame.level() == null || !frame.level().isClientSide()) {
            return;
        }
        if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.CHANGED_DIMENSION) {
            return; // expected during dimension change / level teardown
        }
        DebugLog.logFrameRemoval(frame, reason);
    }
}
