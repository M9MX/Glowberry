package org.m9mx.cactus.glowberry.mixin.shield;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.m9mx.cactus.glowberry.feature.modules.ShieldStatusModule;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

	@Inject(method = "attack", at = @At("HEAD"))
	public void onAttack(Player player, Entity target, CallbackInfo ci) {
		ShieldStatusModule module = ShieldStatusModule.INSTANCE;
		if (module == null || !module.active()) {
			return;
		}

		// Track attack on the target for shield state detection
		if (target instanceof Player targetPlayer) {
			module.getShieldStateManager().handlePlayerAttack(targetPlayer);
		}
	}
}
