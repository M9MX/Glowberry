package org.m9mx.cactus.glowberry.util.waila;

import net.minecraft.world.entity.AgeableMob;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side tracker for the Age / Breed Waila lines.
 *
 * In 26.2 the client only ever learns whether a mob is a baby or an adult
 * ({@link AgeableMob#getAge()} returns -1 / +1 on the client), while the real
 * age, the breeding cooldown and the love state stay on the server. This tracker
 * reconstructs them from two sources:
 *
 * <ul>
 *   <li>A mixin on {@link AgeableMob#setAge(int)} records the real age of every
 *       mob on the integrated server (singleplayer), giving exact baby growth
 *       counts and breeding cooldowns.</li>
 *   <li>A mixin on {@code Animal#setInLoveTime}/{@code resetLove} records the
 *       love state, again only on the integrated server.</li>
 * </ul>
 *
 * On multiplayer servers neither mixin fires, so baby growth falls back to an
 * estimate: babies grow one tick per tick starting from -24000 (20 minutes), so
 * the remaining time is derived from when we first saw the baby.
 */
public final class MobAgeTracker {
    /** How far below 0 a freshly born baby starts (-24000 ticks = 20 minutes). */
    public static final int BABY_START_AGE = -24000;
    private static final int MAX_TRACKED = 512;

    /** Real server-side age per entity, written by the integrated server thread. */
    private static final Map<UUID, Integer> REAL_AGES = new ConcurrentHashMap<>();
    /** Game time at which we first saw a baby (client thread only, multiplayer fallback). */
    private static final Map<UUID, Long> BABY_SINCE = new HashMap<>();
    /** Game time until which an animal is in love (written by the integrated server thread). */
    private static final Map<UUID, Long> LOVE_UNTIL = new ConcurrentHashMap<>();

    private MobAgeTracker() {}

    /** Called from a mixin on {@link AgeableMob#setAge(int)} (integrated server only). */
    public static void recordAge(UUID uuid, int age) {
        if (REAL_AGES.size() > MAX_TRACKED) REAL_AGES.clear();
        REAL_AGES.put(uuid, age);
    }

    /** Called from a mixin on {@code Animal#setInLoveTime(int)} (integrated server only). */
    public static void recordLoveUntil(UUID uuid, long gameTime) {
        if (LOVE_UNTIL.size() > MAX_TRACKED) LOVE_UNTIL.clear();
        LOVE_UNTIL.put(uuid, gameTime);
    }

    /** Called from a mixin on {@code Animal#resetLove()} (integrated server only). */
    public static void clearLove(UUID uuid) {
        LOVE_UNTIL.remove(uuid);
    }

    /**
     * Remaining ticks until a baby grows up, or -1 when the mob is not a baby.
     * Exact on the integrated server, estimated from first sight in multiplayer.
     */
    public static long remainingBabyTicks(AgeableMob mob, long gameTime) {
        UUID uuid = mob.getUUID();
        if (!mob.isBaby()) {
            BABY_SINCE.remove(uuid);
            return -1;
        }
        Integer realAge = REAL_AGES.get(uuid);
        if (realAge != null) {
            return Math.max(0, -(long) realAge);
        }
        long since = BABY_SINCE.computeIfAbsent(uuid, id -> gameTime);
        return Math.max(0, -BABY_START_AGE - (gameTime - since));
    }

    /** True while the animal is in love (only known on the integrated server). */
    public static boolean isInLove(UUID uuid, long gameTime) {
        Long until = LOVE_UNTIL.get(uuid);
        return until != null && until > gameTime;
    }

    /** Remaining ticks of the post-breeding cooldown, 0 when none (or unknown). */
    public static long breedingCooldownTicks(AgeableMob mob) {
        Integer realAge = REAL_AGES.get(mob.getUUID());
        return realAge != null && realAge > 0 ? realAge : 0;
    }
}
