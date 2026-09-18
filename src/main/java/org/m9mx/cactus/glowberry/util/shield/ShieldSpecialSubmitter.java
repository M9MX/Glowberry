package org.m9mx.cactus.glowberry.util.shield;
/**
 * Credits: https://github.com/Walksy/ShieldStatus (26.2 branch)
 *
 * Re-submits the vanilla shield model with a state color tint. Captured from
 * the vanilla ShieldSpecialRenderer at construction (see ShieldSpecialRendererMixin)
 * so we always use the exact same model and sprite getter vanilla uses.
 */
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.m9mx.cactus.glowberry.feature.modules.ShieldStatusModule;

import java.util.Objects;

public class ShieldSpecialSubmitter {

	// The standalone entity texture file - NOT the atlas sprite. Binding the
	// atlas path here is what made the shield render purple (missing texture).
	private static final Identifier SHIELD_TEXTURE =
			Identifier.withDefaultNamespace("textures/entity/shield/shield_base_nopattern.png");

	private final ShieldModel model;
	private final SpriteGetter sprites;
	private final GrayscaleTextureCache grayscaleCache = new GrayscaleTextureCache();

	public ShieldSpecialSubmitter(ShieldModel model, SpriteGetter sprites) {
		this.model = model;
		this.sprites = sprites;
	}

	public void submit(ItemDisplayContext context, DataComponentMap components, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil) {
		ShieldStatusModule module = ShieldStatusModule.INSTANCE;
		if (module == null || !module.active()) return;

		// Resolve whose shield is being rendered. First-person and GUI renders
		// always belong to the client player; third-person renders use the
		// player tracked by PlayerItemInHandLayerMixin (mirrors upstream
		// checkDisplayContext).
		Minecraft mc = Minecraft.getInstance();
		Player tintPlayer;
		if (context != null && (context.firstPerson() || context == ItemDisplayContext.GUI)) {
			tintPlayer = mc.player;
		} else {
			tintPlayer = FocusedEntityHolder.getFocused();
			if (tintPlayer == null) tintPlayer = mc.player;
		}

		Identifier sheet = SHIELD_TEXTURE;
		if (module.grayscaleTexture.get()) {
			sheet = grayscaleCache.get(sheet);
		}

		BannerPatternLayers patterns = components != null
				? components.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
				: BannerPatternLayers.EMPTY;
		DyeColor baseColor = components != null ? components.get(DataComponents.BASE_COLOR) : null;
		boolean hasPatterns = !patterns.layers().isEmpty() || baseColor != null;

		int color = new ShieldItemModelRenderer().getColorForShield(tintPlayer);

		submitNodeCollector.submitModel(
				this.model, Unit.INSTANCE, poseStack,
				RenderTypes.entityTranslucent(sheet),
				lightCoords, overlayCoords, color,
				null, 0, null
		);

		if (hasPatterns) {
			BannerRenderer.submitPatterns(
					this.sprites, poseStack, submitNodeCollector,
					lightCoords, overlayCoords, this.model, Unit.INSTANCE,
					false, Objects.requireNonNullElse(baseColor, DyeColor.WHITE), patterns, null
			);
		}

		if (hasFoil) {
			submitNodeCollector.order(patterns.layers().size() + 1).submitModel(
					this.model, Unit.INSTANCE, poseStack,
					RenderTypes.entityGlint(),
					lightCoords, overlayCoords, -1,
					null, 0, null
			);
		}
	}
}
