package org.m9mx.cactus.glowberry.mixin.shield;
/**
 * Credits: https://github.com/Walksy/ShieldStatus
 */
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemDisplayContext;
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

	@Inject(method = "submit", at = @At("HEAD"), cancellable = true)
	public void onRenderShield(
		DataComponentMap dataComponentMap,
		ItemDisplayContext itemDisplayContext,
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
			return; // Module not enabled, use vanilla rendering
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			return;
		}
		
		Player player = null;
		
		// Determine which player's shield we're rendering
		if (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND 
			|| itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
			// First-person: always the client player
			player = mc.player;
		} else if (itemDisplayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND 
			|| itemDisplayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
			// Try to get from focused entity first (being rendered in PlayerItemInHandLayer)
			if (FocusedEntityHolder.getFocused() != null) {
				player = FocusedEntityHolder.getFocused();
			} else if (entityId >= 0) {
				// Fallback: use entityId to get the entity
				var entity = mc.level.getEntity(entityId);
				if (entity instanceof Player p) {
					player = p;
				}
			}
			
			// If still no player, try to find client player (F5 mode)
			if (player == null && (mc.player.getMainHandItem().is(Items.SHIELD) || mc.player.getOffhandItem().is(Items.SHIELD))) {
				player = mc.player;
			}
		}
		
		if (player == null) {
			return;
		}

		// Check selfStateOnly setting
		if (module.selfStateOnly.get() && player != mc.player) {
			return;
		}

		// Render the colored shield instead of vanilla
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		ShieldItemModelRenderer renderer = new ShieldItemModelRenderer();
		renderer.render(model, poseStack, bufferSource, light, overlay, player);
		bufferSource.endBatch();
		
		// Cancel vanilla rendering
		ci.cancel();
	}
}
