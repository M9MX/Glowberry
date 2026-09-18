/*
 * Adapted from Moss Addon by Datenflieger
 * Original mod: https://github.com/Datenflieger/MossAddon
 */
package org.m9mx.cactus.glowberry.mixin.Modules.moss;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.m9mx.cactus.glowberry.feature.modules.HealthIndicators;
import org.m9mx.cactus.glowberry.util.DamagePopupTracker;
import org.m9mx.cactus.glowberry.util.HealthRenderStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class DamageIndicatorEntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
    private void glowberry$storeHealth(T entity, S state, float tickDelta, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity living) || !(state instanceof HealthRenderStateExtension ext)) return;

        // Damage popups: track health drops for the Damage Indicator module.
        DamagePopupTracker.track(living);

        HealthIndicators module = HealthIndicators.INSTANCE;
        if (module == null || !module.active()) return;

        boolean isPlayer = living instanceof Player;
        if (isPlayer && !module.includePlayers.get()) return;
        if (!isPlayer && !module.includeOtherEntities.get()) return;

        ext.glowberry$setHealth(living.getHealth());
        ext.glowberry$setMaxHealth(living.getMaxHealth());
        ext.glowberry$setAbsorption(living.getAbsorptionAmount());
    }
}
