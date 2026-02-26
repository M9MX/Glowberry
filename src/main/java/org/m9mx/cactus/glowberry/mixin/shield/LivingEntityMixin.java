package org.m9mx.cactus.glowberry.mixin.shield;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.m9mx.cactus.glowberry.feature.modules.ShieldStatusModule;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

	@Inject(method = "handleEntityEvent", at = @At("HEAD"))
	public void onHandleEntityEvent(byte status, CallbackInfo ci) {
		ShieldStatusModule module = ShieldStatusModule.INSTANCE;
		if (module == null || !module.active()) {
			return;
		}

		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity instanceof Player player) {
			module.getShieldStateManager().handleEntityStatus(player, status);
		}
	}
}
