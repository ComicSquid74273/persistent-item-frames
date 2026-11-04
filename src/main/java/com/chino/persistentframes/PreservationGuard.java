package com.chino.persistentframes;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared state for the frame-preservation pipeline: marks removals this mod decided itself
 * (so the guard mixin lets them through) and shows a one-shot startup banner.
 */
public final class PreservationGuard {
    private static final Logger LOGGER = LoggerFactory.getLogger("PersistentItemFrames");

    /** Main-thread marker: removals the mod itself decided on (break sound / crosshair / phantom cleanup). */
    private static boolean ownRemovalInProgress;

    private static boolean bannerShown;

    private PreservationGuard() {
    }

    public static void beginOwnRemoval() {
        ownRemovalInProgress = true;
    }

    public static void endOwnRemoval() {
        ownRemovalInProgress = false;
    }

    public static boolean isOwnRemoval() {
        return ownRemovalInProgress;
    }

    /** Called by the guard mixin when an outside code path tried to discard a frame. */
    public static void logBlockedRemoval(ItemFrame frame, Object reason) {
        LOGGER.info("blocked foreign frame removal (reason={}, at {}, {}, {}) — frame stays persistent",
                reason, frame.getX(), frame.getY(), frame.getZ());
    }

    public static void showBannerOnce(Minecraft client) {
        if (bannerShown || client.player == null || client.level == null) {
            return;
        }
        bannerShown = true;
        if (client.gui != null && client.gui.hud != null) {
            client.gui.hud.getChat().addClientSystemMessage(
                    Component.literal("[Persistent Item Frames] active — frames will persist beyond server view distance")
                            .withStyle(ChatFormatting.GOLD));
        }
    }
}
