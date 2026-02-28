package org.m9mx.cactus.glowberry.mixin.shield;
/**
 * Credits: https://github.com/Walksy/ShieldStatus
 */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.m9mx.cactus.glowberry.feature.modules.ShieldStatusModule;
import org.m9mx.cactus.glowberry.util.shield.FocusedEntityHolder;

@Mixin(PlayerItemInHandLayer.class)
public class PlayerItemInHandLayerMixin {

	@Inject(method = "submitArmWithItem", at = @At("HEAD"))
	public void onSubmitArmWithItem(
		AvatarRenderState avatarRenderState,
		ItemStackRenderState itemStackRenderState,
		ItemStack itemStack,
		HumanoidArm humanoidArm,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int light,
		CallbackInfo ci
	) {
		ShieldStatusModule module = ShieldStatusModule.INSTANCE;
		if (module == null || !module.active()) {
			return;
		}

		// Track the entity being rendered for shield coloring
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && avatarRenderState.id >= 0) {
			var entity = mc.level.getEntity(avatarRenderState.id);
			if (entity instanceof Player player) {
				FocusedEntityHolder.setFocused(player);
			}
		}
	}
}
