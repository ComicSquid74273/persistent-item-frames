package com.chino.persistentframes.mixin;

import com.chino.persistentframes.PreservationGuard;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    // ClientLevel.addEntity first discards any existing entity with the same id before
    // inserting the new one. When the server re-tracks a preserved frame (player returns
    // within view distance), that discard must be allowed through the removal guard, or
    // the duplicate would corrupt the id/uuid lookup maps. The marker covers the whole
    // addEntity body, including its internal removeEntity call.
    @Inject(method = "addEntity", at = @At("HEAD"))
    private void persistentItemFrames$beginReAdd(Entity entity, CallbackInfo ci) {
        PreservationGuard.beginReAdd();
    }

    @Inject(method = "addEntity", at = @At("TAIL"))
    private void persistentItemFrames$endReAdd(Entity entity, CallbackInfo ci) {
        PreservationGuard.endReAdd();
    }
}
