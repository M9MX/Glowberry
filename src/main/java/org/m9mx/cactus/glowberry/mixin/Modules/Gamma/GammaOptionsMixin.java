package org.m9mx.cactus.glowberry.mixin.Modules.Gamma;

import net.minecraft.client.Options;
import net.minecraft.client.OptionInstance;
import org.m9mx.cactus.glowberry.feature.modules.GammaModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps the gamma option out of {@code options.txt} while the Gamma module is
 * active.
 *
 * <p>Because {@code GammaOptionInstanceMixin} makes {@code OptionInstance.get()}
 * return the boosted value, a vanilla options save while the module is on would
 * write e.g. {@code gamma:10.0} to disk - and the next launch would start with
 * that brightness even though the module is off. This mixin skips the gamma key
 * during {@code Options#processDumpedOptions} while the module is active, so
 * the player's own brightness setting in the options file is left untouched.
 * When the module is off, the option flows through vanilla exactly as before.
 *
 * <p>The approach is inspired by Gamma Utils' MixinOptions
 * (https://modrinth.com/mod/gamma-utils, LGPL-3.0); the code is original.
 */
@Mixin(Options.class)
public class GammaOptionsMixin {
	@Redirect(
			method = "processDumpedOptions",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/Options$OptionAccess;process(Ljava/lang/String;Lnet/minecraft/client/OptionInstance;)V"
			)
	)
	private void glowberry$skipGammaWhenActive(Options.OptionAccess access, String key, OptionInstance option) {
		if (!(key.equals("gamma") && GammaModule.isDiverting())) {
			access.process(key, option);
		}
	}
}
