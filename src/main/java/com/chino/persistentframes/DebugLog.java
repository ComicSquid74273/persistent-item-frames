package com.chino.persistentframes;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.MapItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Chat + log instrumentation for the 1.1.2-debug1 diagnostic build.
 * Answers, live and on the user's machine, the questions we cannot answer from source:
 * which frames are preserved, what removes the others, whether their chunk sections compile,
 * and whether map data ever arrived. Keep out of stable releases.
 */
public final class DebugLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("PersistentItemFrames");

    public static final AtomicLong CULL_PRESERVED = new AtomicLong();
    public static final AtomicLong CULL_REMOVED_BREAK = new AtomicLong();
    public static final AtomicLong CULL_REMOVED_AIM = new AtomicLong();
    public static final AtomicLong RENDER_BYPASS_RESCUES = new AtomicLong();
    public static final AtomicLong REMOVALS_OUTSIDE_MOD = new AtomicLong();
    public static final AtomicLong FRAMES_IN_FORGOTTEN_CHUNKS = new AtomicLong();

    /** Main-thread marker: removals the mod itself decided on (break sound / crosshair). */
    private static boolean ownRemovalInProgress;

    private static long lastAuditMs;
    private static long chatBudgetMs;
    private static int chatBudget = 4;
    private static int lastAlive = -1;
    private static boolean bannerShown;

    private DebugLog() {
    }

    public static void chat(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null || client.gui.hud == null) {
            return;
        }
        client.gui.hud.getChat().addClientSystemMessage(
                Component.literal("[PIF] ").withStyle(ChatFormatting.GOLD).append(Component.literal(message)));
    }

    /** Rate-limits chat lines for bursty events (map walls hold hundreds of frames). */
    public static boolean allowChat() {
        long now = System.currentTimeMillis();
        if (now - chatBudgetMs > 5000L) {
            chatBudgetMs = now;
            chatBudget = 4;
        }
        return chatBudget-- > 0;
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

    /** Logs every frame removal the mod did not decide itself, with the caller chain. */
    public static void logFrameRemoval(ItemFrame frame, Entity.RemovalReason reason) {
        if (isOwnRemoval()) {
            return; // our decision (break event / crosshair); counted at the call site
        }
        long n = REMOVALS_OUTSIDE_MOD.incrementAndGet();
        BlockPos pos = frame.blockPosition();
        String stack = StackWalker.getInstance().walk(stream -> stream
                .skip(3L)
                .limit(6L)
                .map(frameInfo -> frameInfo.getClassName() + "." + frameInfo.getMethodName())
                .reduce((a, b) -> a + " <- " + b)
                .orElse("?"));
        LOGGER.info("frame #{} removed, reason={} at ({}, {}, {}), caller: {}",
                n, reason, pos.getX(), pos.getY(), pos.getZ(), stack);
        if (allowChat()) {
            chat("frame REMOVED reason=" + reason + " (" + pos.getX() + " " + pos.getY() + " " + pos.getZ() + ") [" + stack + "]");
        }
    }

    public static void logForgottenChunk(int chunkX, int chunkZ, int frames) {
        FRAMES_IN_FORGOTTEN_CHUNKS.addAndGet(frames);
        LOGGER.info("server forgot chunk ({}, {}); {} frame(s) still alive client-side", chunkX, chunkZ, frames);
        if (frames > 0 && allowChat()) {
            chat("chunk " + chunkX + "," + chunkZ + " forgotten — " + frames + " frame(s) kept in memory");
        }
    }

    /** Runs every client tick; reports at most every 3 seconds once frames exist. */
    public static void audit(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            return;
        }
        if (!bannerShown) {
            bannerShown = true;
            chat("diagnostic build active — go near a map wall, then fly out past the server view edge and read these lines");
        }
        long now = System.currentTimeMillis();
        if (now - lastAuditMs < 3000L) {
            return;
        }
        lastAuditMs = now;

        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        int alive = 0;
        int far = 0;
        int sectionCompiled = 0;
        int withMap = 0;
        int mapDataLoaded = 0;
        double maxDist = 0.0;

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ItemFrame frame)) {
                continue;
            }
            alive++;
            double dist = Math.sqrt(frame.distanceToSqr(px, py, pz));
            if (dist > maxDist) {
                maxDist = dist;
            }
            if (dist <= 64.0) {
                continue;
            }
            far++;
            if (client.levelRenderer.isSectionCompiledAndVisible(frame.blockPosition())) {
                sectionCompiled++;
            }
            var mapId = frame.getFramedMapId(frame.getItem());
            if (mapId != null) {
                withMap++;
                if (MapItem.getSavedData(mapId, client.level) != null) {
                    mapDataLoaded++;
                }
            }
        }

        if (alive == 0 && lastAlive <= 0) {
            return; // stay quiet until the first frames appear
        }
        if (alive == 0 && lastAlive > 0) {
            chat("ALL frames gone from client memory (had " + lastAlive + ") — see removal lines above for the culprit");
        }
        lastAlive = alive;

        chat(String.format(
                "alive=%d | >64b: %d (max %.0fb) secCompiled=%d mapsLoaded=%d/%d | 3s: kept=%d removed(break=%d,aim=%d,other=%d) renderBypass=%d framesInForgottenChunks=%d",
                alive, far, maxDist, sectionCompiled, mapDataLoaded, withMap,
                CULL_PRESERVED.getAndSet(0L), CULL_REMOVED_BREAK.getAndSet(0L), CULL_REMOVED_AIM.getAndSet(0L),
                REMOVALS_OUTSIDE_MOD.getAndSet(0L), RENDER_BYPASS_RESCUES.getAndSet(0L),
                FRAMES_IN_FORGOTTEN_CHUNKS.getAndSet(0L)));
    }
}
