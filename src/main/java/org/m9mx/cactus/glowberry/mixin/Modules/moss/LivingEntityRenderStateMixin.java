/*
 * Adapted from Moss Addon by Datenflieger
 * Original mod: https://github.com/Datenflieger/MossAddon
 */
package org.m9mx.cactus.glowberry.mixin.Modules.moss;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.m9mx.cactus.glowberry.util.HealthRenderStateExtension;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements HealthRenderStateExtension {
    private float glowberry$health;
    private float glowberry$maxHealth;
    private float glowberry$absorption;

    @Override
    public void glowberry$setHealth(float health) {
        this.glowberry$health = health;
    }

    @Override
    public void glowberry$setMaxHealth(float maxHealth) {
        this.glowberry$maxHealth = maxHealth;
    }

    @Override
    public void glowberry$setAbsorption(float absorption) {
        this.glowberry$absorption = absorption;
    }

    @Override
    public float glowberry$getHealth() {
        return glowberry$health;
    }

    @Override
    public float glowberry$getMaxHealth() {
        return glowberry$maxHealth;
    }

    @Override
    public float glowberry$getAbsorption() {
        return glowberry$absorption;
    }
}
