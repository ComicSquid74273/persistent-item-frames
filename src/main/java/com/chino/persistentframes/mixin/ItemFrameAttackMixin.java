package com.chino.persistentframes.mixin;

import com.chino.persistentframes.InteractionTracker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ItemFrameAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Player player, Entity entity, CallbackInfo ci) {
        if (entity instanceof ItemFrame itemFrame) {
            InteractionTracker.registerPendingInteraction(itemFrame.getId());
        }
    }

    // 1.21.11 splits the old 4-arg interact into interactAt (4-arg, primary path) and a 3-arg
    // interact fallback. Register the pending interaction on both; a double registration for the
    // same id is just a map re-put with a refreshed timestamp, so belt-and-braces is harmless.
    @Inject(method = "interactAt", at = @At("HEAD"))
    private void onInteractAt(Player player, Entity entity, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (entity instanceof ItemFrame itemFrame) {
            InteractionTracker.registerPendingInteraction(itemFrame.getId());
        }
    }

    @Inject(method = "interact", at = @At("HEAD"))
    private void onInteract(Player player, Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (entity instanceof ItemFrame itemFrame) {
            InteractionTracker.registerPendingInteraction(itemFrame.getId());
        }
    }
}
