package org.m9mx.cactus.glowberry.mixin.shield;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.m9mx.cactus.glowberry.feature.modules.ShieldStatusModule;

@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Inject(method = "tick", at = @At("HEAD"))
	public void onTick(CallbackInfo ci) {
		ShieldStatusModule module = ShieldStatusModule.INSTANCE;
		if (module == null || !module.active()) {
			return;
		}
		// Config tick can be done here if needed
	}
}
