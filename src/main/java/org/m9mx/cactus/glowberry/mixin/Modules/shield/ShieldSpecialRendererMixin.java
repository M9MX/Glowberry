package org.m9mx.cactus.glowberry.mixin.Modules.shield;

/**
 * Credits: https://github.com/Walksy/ShieldStatus
 */
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.m9mx.cactus.glowberry.feature.modules.ShieldStatusModule;
import org.m9mx.cactus.glowberry.util.shield.ShieldItemModelRenderer;
import org.m9mx.cactus.glowberry.util.shield.FocusedEntityHolder;

@Mixin(ShieldSpecialRenderer.class)
public class ShieldSpecialRendererMixin {

	@Shadow
	@Final
	private ShieldModel model;

	@Shadow
	@Final
	private SpriteGetter sprites;

	// Fixed: Removed ItemDisplayContext parameter to match modern signature
	@Inject(method = "submit", at = @At("HEAD"), cancellable = true)
	public void onRenderShield(
			DataComponentMap dataComponentMap,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int light,
			int overlay,
			boolean bl,
			int entityId,
			CallbackInfo ci
	) {
		ShieldStatusModule module = ShieldStatusModule.INSTANCE;
		if (module == null || !module.active()) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			return;
		}

		Player player = null;

		// Try to resolve holding player from context stack or entity id mapping
		if (FocusedEntityHolder.getFocused() != null) {
			player = FocusedEntityHolder.getFocused();
		} else if (entityId >= 0) {
			var entity = mc.level.getEntity(entityId);
			if (entity instanceof Player p) {
				player = p;
			}
		}

		// Fallback: If no explicit entity ID is associated, check if the client player is using a shield (F5/First-Person fallback)
		if (player == null && (mc.player.getMainHandItem().is(Items.SHIELD) || mc.player.getOffhandItem().is(Items.SHIELD))) {
			player = mc.player;
		}

		if (player == null) {
			return;
		}

		// Check selfStateOnly setting
		if (module.selfStateOnly.get() && player != mc.player) {
			return;
		}

		// Render the colored shield instead of vanilla.
		// 26.2 removed RenderBuffers.bufferSource()/Minecraft.renderBuffers() in favor of
		// the staged SubmitNodeCollector renderer, so submit the model through it
		// (same path vanilla ShieldSpecialRenderer uses, with our state color tint).
		// Note: we pass an explicit entityTranslucent render type here - the sprite-resolved
		// render type used by the SpriteId overload is opaque (drops the alpha channel),
		// which is why the opacity setting had no effect.
		ShieldItemModelRenderer renderer = new ShieldItemModelRenderer();
		int color = renderer.getColorForShield(player);
		submitNodeCollector.submitModel(
			model, Unit.INSTANCE, poseStack,
			RenderTypes.entityTranslucent(Sheets.SHIELD_BASE_NO_PATTERN.texture(), true),
			light, overlay, color,
			sprites.get(Sheets.SHIELD_BASE_NO_PATTERN), entityId, null
		);

		// Cancel vanilla rendering
		ci.cancel();
	}
}