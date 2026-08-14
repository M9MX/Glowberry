package org.m9mx.cactus.glowberry.accessor;

/**
 * Exposes Minecraft's private single-click handlers so the auto clicker can
 * trigger a real attack / item use without faking a held key (which left the
 * input system in a stuck state that tanked the framerate).
 */
public interface IMinecraftClickAccessor {
    boolean glowberry_StartAttack();
    void glowberry_StartUseItem();
}
