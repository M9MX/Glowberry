package org.m9mx.cactus.glowberry.mixin.appleskin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.m9mx.cactus.glowberry.util.appleskin.client.HUDOverlayHandler;

@Mixin(Gui.class)
public class InGameHudMixin
{
	@Inject(at = @At("HEAD"), method = "renderFood")
	private void renderFoodPre(GuiGraphics guiGraphics, Player player, int left, int top, CallbackInfo info)
	{
		if (HUDOverlayHandler.INSTANCE != null)
			HUDOverlayHandler.INSTANCE.onPreRenderFood(guiGraphics, player, left, top);
	}

	@Inject(at = @At("RETURN"), method = "renderFood")
	private void renderFoodPost(GuiGraphics guiGraphics, Player player, int left, int top, CallbackInfo info)
	{
		if (HUDOverlayHandler.INSTANCE != null)
			HUDOverlayHandler.INSTANCE.onRenderFood(guiGraphics, player, left, top);
	}

	@Inject(at = @At("RETURN"), method = "renderHearts")
	private void renderHeartsPost(GuiGraphics guiGraphics, Player player, int x, int y, int height, int regeneratingHeartCount, float absorption, int food, int maxHearts, int halfHeartIndex, boolean blinking, CallbackInfo info)
	{
		if (HUDOverlayHandler.INSTANCE != null)
			HUDOverlayHandler.INSTANCE.onRenderHealth(guiGraphics, player, x, y, height, regeneratingHeartCount, 20.0F, 20, 20, (int) absorption, blinking);
	}
}