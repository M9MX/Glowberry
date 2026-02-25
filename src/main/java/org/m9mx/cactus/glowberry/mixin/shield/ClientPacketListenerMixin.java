package org.m9mx.cactus.glowberry.mixin.shield;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.m9mx.cactus.glowberry.feature.modules.ShieldStatusModule;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

	@Inject(method = "handleSoundEvent", at = @At("HEAD"))
	public void onSound(ClientboundSoundPacket packet, CallbackInfo ci) {
		ShieldStatusModule module = ShieldStatusModule.INSTANCE;
		if (module == null || !module.active()) {
			return;
		}

		String soundId = packet.getSound().value().location().toString().toLowerCase();
		if (soundId.contains("shield.break")) {
			System.out.println("[Shield] Shield break sound detected at: " + packet.getX() + ", " + packet.getY() + ", " + packet.getZ());
			module.getShieldStateManager().handleBreakPacket(packet.getX(), packet.getY(), packet.getZ());
		}
	}
}
