/*
 * Adapted from Moss Addon by Datenflieger
 * Original mod: https://github.com/Datenflieger/MossAddon
 */
package org.m9mx.cactus.glowberry.mixin.Modules.moss;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;

import org.m9mx.cactus.glowberry.feature.modules.ArrowTrails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Suppresses the vanilla crit-particle trail when Arrow Trails is spawning its
 * own custom particles, so the trail is purely our colored one.
 *
 * <p>The vanilla crit trail is spawned inside {@link AbstractArrow#tick()} as a
 * 4-iteration loop gated by a single {@code isCritArrow()} call. Redirecting
 * that one call to {@code false} removes the white vanilla particles without
 * touching any other crit behavior — the other {@code isCritArrow()} uses
 * (pierce handling, save data) live in different methods and are unaffected.
 */
@Mixin(AbstractArrow.class)
public class AbstractArrowTickMixin {

    /**
     * Matches the single {@code isCritArrow()} invocation inside
     * {@code AbstractArrow.tick()} (the vanilla crit-particle loop).
     */
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;isCritArrow()Z"
            )
    )
    private boolean glowberry$suppressVanillaCritTrail(AbstractArrow arrow) {
        ArrowTrails module = ArrowTrails.INSTANCE;
        if (module == null || !module.active() || (Boolean) module.vanillaTrail.get()) {
            return arrow.isCritArrow();
        }

        // Our trail spawner only covers Arrow (regular + tipped arrows).
        // For anything else (e.g. spectral arrows) leave vanilla untouched.
        if (!(arrow instanceof Arrow)) {
            return arrow.isCritArrow();
        }

        // Mirror the same gates our custom trail uses: if we are not spawning
        // particles for this arrow this tick, leave vanilla behavior alone.
        Minecraft minecraft = Minecraft.getInstance();
        if ((Boolean) module.ownOnly.get() && arrow.getOwner() != null && minecraft.player != null) {
            if (arrow.getOwner().getId() != minecraft.player.getId()) {
                return arrow.isCritArrow();
            }
        }

        double speed = arrow.getDeltaMovement().length();
        double minSpeed = ((Integer) module.minSpeed100.get()) / 100.0;
        if (speed < minSpeed) {
            return arrow.isCritArrow();
        }

        // Our trail is spawning — hide the vanilla crit particles.
        return false;
    }
}
