package org.m9mx.cactus.glowberry.mixin.Modules.shield;

/**
 * Credits: https://github.com/Walksy/ShieldStatus (26.2 branch)
 *
 * The GUI caches item renders keyed by a model identity; the shield's model
 * never changes when only its tint changes. Mark shield renders as animated so
 * the state color also updates live in inventories.
 */
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.m9mx.cactus.glowberry.feature.modules.ShieldStatusModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {

	@Inject(
			method = "lambda$prepareItemElements$0",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/render/GuiItemAtlas;getOrUpdate(Lnet/minecraft/client/renderer/item/TrackingItemStackRenderState;)Lnet/minecraft/client/gui/render/GuiItemAtlas$SlotView;"
			)
	)
	private void glowberry$guiShieldAnimated(MutableBoolean hasOversizedItems, GuiItemAtlas itemAtlas, GuiItemRenderState itemState, CallbackInfo ci) {
		ShieldStatusModule module = ShieldStatusModule.INSTANCE;
		if (module == null || !module.active()) return;

		if (itemState.itemStackRenderState().getModelIdentity().toString().contains("shield")) {
			itemState.itemStackRenderState().setAnimated();
		}
	}
}
