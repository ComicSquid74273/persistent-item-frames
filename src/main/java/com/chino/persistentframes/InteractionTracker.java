package com.chino.persistentframes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared state for pending player interactions with item frames.
 * Lives outside the mixin classes because mixin members must be private
 * or {@code @Unique}, and cross-mixin static calls into a mixin class
 * are not possible at runtime.
 */
public final class InteractionTracker {

    private InteractionTracker() {
    }

    public static final Map<Integer, Long> PENDING_INTERACTIONS = new ConcurrentHashMap<>();

    public static void registerPendingInteraction(int entityId) {
        PENDING_INTERACTIONS.put(entityId, System.currentTimeMillis());
    }
}
