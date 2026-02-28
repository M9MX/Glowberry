package org.m9mx.cactus.glowberry.mixin.shield;
/**
 * Credits: https://github.com/Walksy/ShieldStatus
 */
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
	public void onAttackEntity(Player player, Entity target, CallbackInfo ci) {
		ShieldStatusModule module = ShieldStatusModule.INSTANCE;
		if (module == null || !module.active()) {
			return;
		}

		if (target instanceof Player targetPlayer) {
			module.getShieldStateManager().handlePlayerAttack(targetPlayer);
		}
	}
}
