package org.m9mx.cactus.glowberry.util.shield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.m9mx.cactus.glowberry.feature.modules.ShieldStatusModule;

public class ShieldItemModelRenderer {
	private static final Identifier SHIELD_TEXTURE = Identifier.withDefaultNamespace("textures/entity/shield_base_nopattern.png");
	private final GrayscaleTextureCache grayscaleCache = new GrayscaleTextureCache();

	public void render(ShieldModel model, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay, Player player) {
		if (player == null) return;

		ShieldStatusModule module = ShieldStatusModule.INSTANCE;
		if (module == null) return;

		// Get color based on shield state
		int colorInt = getColorForShield(player);

		poseStack.pushPose();
		poseStack.scale(1.0F, -1.0F, -1.0F);

		// Use grayscale texture if enabled, otherwise use normal texture
		Identifier textureId = module.grayscaleTexture.get() ? grayscaleCache.get(SHIELD_TEXTURE) : SHIELD_TEXTURE;
		var layer = RenderTypes.entityTranslucent(textureId, true);
		VertexConsumer vertexConsumer = multiBufferSource.getBuffer(layer);

		// Render handle
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.0F, 0.0001F);
		model.handle().render(poseStack, vertexConsumer, light, overlay, colorInt);
		poseStack.popPose();

		// Render plate
		model.plate().render(poseStack, vertexConsumer, light, overlay, colorInt);

		poseStack.popPose();
	}

	private int getColorForShield(Player player) {
		ShieldStatusModule module = ShieldStatusModule.INSTANCE;
		if (module == null) return 0xFFFFFFFF;

		Minecraft mc = Minecraft.getInstance();

		// Check selfStateOnly setting - only apply colors to client player
		if (module.selfStateOnly.get() && player != mc.player) {
			return 0xFFFFFFFF; // Default white for other players when selfStateOnly is on
		}

		ShieldStateManager manager = module.getShieldStateManager();
		if (manager == null) return 0xFFFFFFFF;

		boolean isCoolingDown = manager.isCoolingDown(player);
		boolean isUsing = manager.isUsingShield(player);
		
		// Get opacity setting (0-100%) and convert to alpha (0-255)
		int opacity = module.opacity.get();
		int alpha = (int) (255 * (opacity / 100f)) & 0xFF;

		if (isUsing && module.usingColor.get() != null) {
			return applyAlpha(module.usingColor.get().color(), alpha);
		}

		if (isCoolingDown && module.disabledColor.get() != null) {
			if (module.interpolateColor.get()) {
				float progress = manager.getCooldownProgress(player);
				int interpolated = interpolateColor(
					module.enabledColor.get().color(),
					module.disabledColor.get().color(),
					progress
				);
				return applyAlpha(interpolated, alpha);
			}
			return applyAlpha(module.disabledColor.get().color(), alpha);
		}

		if (module.enabledColor.get() != null) {
			return applyAlpha(module.enabledColor.get().color(), alpha);
		}

		return 0xFFFFFFFF;
	}
	
	private int applyAlpha(int color, int alpha) {
		return (color & 0x00FFFFFF) | (alpha << 24);
	}

	private int interpolateColor(int color1, int color2, float progress) {
		int a1 = (color1 >> 24) & 0xFF;
		int r1 = (color1 >> 16) & 0xFF;
		int g1 = (color1 >> 8) & 0xFF;
		int b1 = color1 & 0xFF;

		int a2 = (color2 >> 24) & 0xFF;
		int r2 = (color2 >> 16) & 0xFF;
		int g2 = (color2 >> 8) & 0xFF;
		int b2 = color2 & 0xFF;

		int a = (int) (a1 + (a2 - a1) * progress);
		int r = (int) (r1 + (r2 - r1) * progress);
		int g = (int) (g1 + (g2 - g1) * progress);
		int b = (int) (b1 + (b2 - b1) * progress);

		return ARGB.color(a, r, g, b);
	}
}
