package org.m9mx.cactus.glowberry.mixin.Modules.shield;

/**
 * Credits: https://github.com/Walksy/ShieldStatus (26.2 branch)
 *
 * Taken out of ShieldSpecialRenderer to acquire the displayContext: special
 * model renderers never see it in 26.2, but ItemStackRenderState$LayerRenderState
 * does. We intercept one level above vanilla's shield submit, run our own tinted
 * submit and cancel vanilla's.
 */
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import org.m9mx.cactus.glowberry.feature.modules.ShieldStatusModule;
import org.m9mx.cactus.glowberry.util.shield.ShieldSpecialSubmitter;
import org.m9mx.cactus.glowberry.util.shield.ShieldSpecialSubmitterHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public abstract class LayerRenderStateMixin {

	@Shadow
	@Final
	ItemStackRenderState this$0;

	@Shadow
	private @Nullable SpecialModelRenderer<Object> specialRenderer;

	@Shadow
	private @Nullable Object argumentForSpecialRendering;

	@Shadow
	private ItemStackRenderState.FoilType foilType;

	@Inject(
			method = "submit",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/special/SpecialModelRenderer;submit(Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V"
			),
			cancellable = true
	)
	private void glowberry$redirectShieldSubmit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, int outlineColor, CallbackInfo ci) {
		ShieldStatusModule module = ShieldStatusModule.INSTANCE;
		if (module == null || !module.active()) return;

		if ((Object) this.specialRenderer instanceof ShieldSpecialRenderer) {
			ShieldSpecialSubmitter submitter = ShieldSpecialSubmitterHolder.get();
			if (submitter == null) return;

			ItemDisplayContext context = this.this$0.displayContext;
			DataComponentMap components = (this.argumentForSpecialRendering instanceof DataComponentMap map)
					? map
					: null;
			boolean hasFoil = this.foilType != ItemStackRenderState.FoilType.NONE;

			submitter.submit(context, components, poseStack, submitNodeCollector, lightCoords, overlayCoords, hasFoil);

			// Vanilla pushes the pose before calling into the special renderer;
			// since we skip its body, pop it here exactly like vanilla would.
			poseStack.popPose();
			ci.cancel();
		}
	}
}
