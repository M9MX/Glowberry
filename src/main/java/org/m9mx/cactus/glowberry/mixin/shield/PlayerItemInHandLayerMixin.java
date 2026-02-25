package org.m9mx.cactus.glowberry.mixin.shield;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.m9mx.cactus.glowberry.feature.modules.ShieldStatusModule;
import org.m9mx.cactus.glowberry.util.shield.ShieldStateManager;

@Mixin(net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer.class)
public class PlayerItemInHandLayerMixin {

	@Inject(method = "submitArmWithItem", at = @At("HEAD"))
	public void onSubmitArmWithItem(
		AvatarRenderState renderState,
		ItemStackRenderState itemStackRenderState,
		ItemStack itemStack,
		HumanoidArm humanoidArm,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int light,
		CallbackInfo ci
	) {
		// Check if it's a shield being rendered
		if (!itemStack.is(Items.SHIELD)) {
			return;
		}

		ShieldStatusModule module = ShieldStatusModule.INSTANCE;
		if (module == null || !module.active()) {
			return;
		}

		// Get the player entity being rendered (if available)
		if (!(renderState instanceof AvatarRenderState)) {
			return;
		}

		// Try to extract player from render state (implementation-dependent)
		// For now, we can only reliably color shields in first-person
		// Third-person requires access to the entity being rendered
	}
}
