package com.chino.persistentframes.mixin;

import com.chino.persistentframes.PreservationGuard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityRemovalGuardMixin {

    // NOTE: must target Entity, not ItemFrame — ItemFrame inherits setRemoved and does not
    // declare it, so a mixin on ItemFrame cannot resolve the target and crashes at launch.
    //
    // Every client-side removal path funnels through setRemoved (packet handlers, discard(),
    // other mods). Removal decisions belong exclusively to this mod's heuristics (break
    // sounds, player interaction); anything else discarding a frame is cancelled. Re-adding
    // an entity with the same id (server re-tracking, see ClientLevelMixin) and level
    // teardown reasons still pass.
    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void persistentItemFrames$guardFrameRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        if (!((Object) this instanceof ItemFrame frame)) {
            return;
        }
        if (PreservationGuard.isOwnRemoval() || PreservationGuard.isReAdd()) {
            return;
        }
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
