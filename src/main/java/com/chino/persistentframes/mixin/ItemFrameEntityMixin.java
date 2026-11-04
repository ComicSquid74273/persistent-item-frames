package com.chino.persistentframes.mixin;

import com.chino.persistentframes.PreservationGuard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrame.class)
public abstract class ItemFrameEntityMixin {

    // Every path that removes a frame client-side funnels through setRemoved (packet handlers,
    // discard(), other mods). Removal decisions belong exclusively to this mod's heuristics
    // (break sounds, player interaction); anything else trying to discard a preserved frame is
    // cancelled. Level teardown reasons still pass so dimension changes work.
    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void persistentItemFrames$guardRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        if (PreservationGuard.isOwnRemoval()) {
            return;
        }
        ItemFrame frame = (ItemFrame) (Object) this;
        if (frame.level() == null || !frame.level().isClientSide()) {
            return;
        }
        if (reason != Entity.RemovalReason.DISCARDED && reason != Entity.RemovalReason.KILLED) {
            return; // CHANGED_DIMENSION / UNLOADED_* during level teardown
        }
        PreservationGuard.logBlockedRemoval(frame, reason);
        ci.cancel();
    }
}
