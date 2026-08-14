package org.m9mx.cactus.glowberry.accessor;

/**
 * Exposes {@code MultiPlayerGameMode.destroyProgress} - the smooth 0..1 mining
 * progress vanilla itself updates every tick (as opposed to the discrete
 * destroy stage, which only advances in 10% jumps).
 */
public interface IMultiPlayerGameModeAccessor {
    float glowberry_getDestroyProgress();
}
