/*
 * Adapted from Moss Addon by Datenflieger
 * Original mod: https://github.com/Datenflieger/MossAddon
 */
package org.m9mx.cactus.glowberry.mixin.Modules.moss;

import java.awt.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.Level;

import org.m9mx.cactus.glowberry.feature.modules.ArrowTrails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Arrow.class)
public abstract class ArrowEntityTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void moss$arrowTrails_tick(CallbackInfo ci) {
        ArrowTrails module = ArrowTrails.INSTANCE;
        if (module == null || !module.active()) return;

        Arrow arrow = (Arrow) (Object) this;
        Level world = arrow.level();
        if (!world.isClientSide()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if ((Boolean) module.ownOnly.get() && arrow.getOwner() != null && minecraft.player != null) {
            if (arrow.getOwner().getId() != minecraft.player.getId()) {
                return;
            }
        }

        double speed = arrow.getDeltaMovement().length();
        double minSpeed = ((Integer) module.minSpeed100.get()) / 100.0;
        if (speed < minSpeed) return;

        float size = ((Integer) module.particleSize10.get()) / 10.0f;

        Color rgb;
        if ((Boolean) module.rainbow.get()) {
            float hueSpeed = ((Integer) module.rgbSpeed.get()) / 1000.0f;
            long time = System.currentTimeMillis();
            float hue = (time % (long) (360.0f / hueSpeed)) * hueSpeed / 360.0f;
            rgb = Color.getHSBColor(hue, 1.0f, 1.0f);
        } else {
            rgb = module.color.get().value();
        }

        int packedColor = ARGB.color(rgb.getRed(), rgb.getGreen(), rgb.getBlue());
        DustParticleOptions effect = new DustParticleOptions(packedColor, size);

        int density = (Integer) module.particleDensity.get();
        double spread = ((Integer) module.offsetSpread1000.get()) / 1000.0;

        // Distribute the particles along the path the arrow traveled last tick
        // (xo/yo/zo -> current pos), so fast arrows leave a continuous trail
        // instead of gaps between per-tick spawn points.
        double fromX = arrow.xo;
        double fromY = arrow.yo;
        double fromZ = arrow.zo;
        double toX = arrow.getX();
        double toY = arrow.getY();
        double toZ = arrow.getZ();

        for (int i = 0; i < density; i++) {
            double t = (i + 0.5) / density;
            double baseX = fromX + (toX - fromX) * t;
            double baseY = fromY + (toY - fromY) * t;
            double baseZ = fromZ + (toZ - fromZ) * t;
            double xOffset = (arrow.getRandom().nextDouble() - 0.5) * spread;
            double yOffset = (arrow.getRandom().nextDouble() - 0.5) * spread;
            double zOffset = (arrow.getRandom().nextDouble() - 0.5) * spread;
            world.addParticle(
                    effect,
                    baseX + xOffset,
                    baseY + yOffset,
                    baseZ + zOffset,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }
}
