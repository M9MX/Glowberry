package org.m9mx.cactus.glowberry.mixin.Modules.waila;

import net.minecraft.world.entity.LivingEntity;
import org.m9mx.cactus.glowberry.util.waila.MobEffectTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Snapshot every entity's real effects on the integrated server (singleplayer)
 * so the Waila effects line can show them. 26.2 vanilla never sends other
 * entities' effects to clients - only the effects of an entity's passengers -
 * so without this the client-side {@code getActiveEffects()} is always empty
 * for mobs. {@code tickEffects} runs every tick from {@code baseTick}, which
 * keeps the snapshot in sync with adds, updates and expirations. On multiplayer
 * servers it never fires and only client-synced effects can be shown.
 */
@Mixin(LivingEntity.class)
public class MixinLivingEffectSync {

    @Inject(method = "tickEffects", at = @At("RETURN"))
    private void glowberry_syncServerEffects(CallbackInfo ci) {
        LivingEntity living = (LivingEntity) (Object) this;
        if (!living.level().isClientSide()) {
            MobEffectTracker.syncFromServer(living);
        }
    }
}
