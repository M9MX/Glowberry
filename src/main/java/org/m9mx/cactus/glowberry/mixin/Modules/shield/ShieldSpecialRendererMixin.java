package org.m9mx.cactus.glowberry.mixin.Modules.shield;

/**
 * Credits: https://github.com/Walksy/ShieldStatus (26.2 branch)
 */
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import org.m9mx.cactus.glowberry.util.shield.ShieldSpecialSubmitter;
import org.m9mx.cactus.glowberry.util.shield.ShieldSpecialSubmitterHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShieldSpecialRenderer.class)
public class ShieldSpecialRendererMixin {

	// Capture the exact ShieldModel + SpriteGetter vanilla uses the first time
	// the shield special renderer is constructed, so our custom submit path is
	// always in sync with vanilla's.
	@Inject(method = "<init>", at = @At("RETURN"))
	private void glowberry$captureShieldModel(SpriteGetter sprites, ShieldModel model, CallbackInfo ci) {
		ShieldSpecialSubmitterHolder.set(new ShieldSpecialSubmitter(model, sprites));
	}
}
