package org.m9mx.cactus.glowberry.mixin.Modules.Gamma;

import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.m9mx.cactus.glowberry.feature.modules.GammaModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes every read and write of the vanilla brightness option
 * ({@code options.gamma()}) through the Gamma module while it is active.
 *
 * <p>{@code get()} returns the module's own (unclamped) gamma value, so the
 * lightmap, the brightness slider and anything else that reads the option all
 * see the boosted value - no per-tick poking needed. {@code set()} consumes the
 * write and routes it back into the module, so dragging the vanilla brightness
 * slider while the module is on adjusts the module's gamma instead of fighting
 * it.
 *
 * <p>The approach is inspired by Gamma Utils' MixinOptionInstance
 * (https://modrinth.com/mod/gamma-utils, LGPL-3.0); the code is original.
 */
@Mixin(OptionInstance.class)
public class GammaOptionInstanceMixin<T> {
	@Shadow
	@Final
	private Component caption;

	@Inject(method = "get", at = @At("HEAD"), cancellable = true)
	private void glowberry$gammaGet(CallbackInfoReturnable<Object> cir) {
		if (isGammaOption() && GammaModule.isDiverting()) {
			cir.setReturnValue(GammaModule.getCurrentGamma());
		}
	}

	@Inject(method = "set", at = @At("HEAD"), cancellable = true)
	private void glowberry$gammaSet(Object value, CallbackInfo ci) {
		if (isGammaOption() && GammaModule.isDiverting()) {
			if (value instanceof Number number) {
				GammaModule.onGammaOptionSet(number.doubleValue());
			}
			ci.cancel();
		}
	}

	@Unique
	private boolean isGammaOption() {
		if (caption.getContents() instanceof TranslatableContents translatable) {
			return translatable.getKey().equals("options.gamma");
		}
		return false;
	}
}
