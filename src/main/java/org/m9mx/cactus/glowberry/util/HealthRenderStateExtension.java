/*
 * Adapted from Moss Addon by Datenflieger
 * Original mod: https://github.com/Datenflieger/MossAddon
 */
package org.m9mx.cactus.glowberry.util;

public interface HealthRenderStateExtension {
    void glowberry$setHealth(float health);
    void glowberry$setMaxHealth(float maxHealth);
    void glowberry$setAbsorption(float absorption);

    float glowberry$getHealth();
    float glowberry$getMaxHealth();
    float glowberry$getAbsorption();
}
