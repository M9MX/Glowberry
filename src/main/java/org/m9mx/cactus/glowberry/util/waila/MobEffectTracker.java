package org.m9mx.cactus.glowberry.util.waila;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges mob potion effects from the integrated server to the Waila display.
 *
 * In 26.2 vanilla only ever sends {@code ClientboundUpdateMobEffectPacket}s to
 * an entity's <em>passengers</em> - never to players tracking the entity, and
 * pre-existing effects are not re-sent on pairing either. As a result the
 * client-side {@code getActiveEffects()} of every mob is always empty.
 *
 * A mixin on {@link LivingEntity#tickEffects()} therefore snapshots the real
 * server-side effects of every entity into this map every tick (singleplayer
 * only - the integrated server runs in the same JVM). The display merges these
 * with whatever effects the client did receive (e.g. the player's own).
 */
public final class MobEffectTracker {
    private static final int MAX_TRACKED = 2048;
    private static final Map<UUID, Map<Holder<MobEffect>, MobEffectInstance>> SERVER_EFFECTS = new ConcurrentHashMap<>();

    private MobEffectTracker() {}

    /** Snapshot the current effects of a server-side entity. Called every tick. */
    public static void syncFromServer(LivingEntity living) {
        UUID uuid = living.getUUID();
        var effects = living.getActiveEffects();
        if (effects.isEmpty() && !SERVER_EFFECTS.containsKey(uuid)) return;
        if (SERVER_EFFECTS.size() > MAX_TRACKED) SERVER_EFFECTS.clear();
        if (effects.isEmpty()) {
            SERVER_EFFECTS.remove(uuid);
            return;
        }
        Map<Holder<MobEffect>, MobEffectInstance> copy = new ConcurrentHashMap<>();
        for (MobEffectInstance effect : effects) {
            // Copy so ticking the server-side instance can't tear values on the
            // render thread (the server ticks on its own thread).
            copy.put(effect.getEffect(), new MobEffectInstance(effect));
        }
        SERVER_EFFECTS.put(uuid, copy);
    }

    /** Snapshot of effects only known on the integrated server, for display. */
    public static List<MobEffectInstance> effectsFor(UUID uuid) {
        Map<Holder<MobEffect>, MobEffectInstance> map = SERVER_EFFECTS.get(uuid);
        return map == null ? List.of() : new ArrayList<>(map.values());
    }
}
