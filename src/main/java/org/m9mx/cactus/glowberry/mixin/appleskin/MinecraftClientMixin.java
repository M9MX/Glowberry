package org.m9mx.cactus.glowberry.mixin.appleskin;
/**
 * Credits: https://github.com/squeek502/AppleSkin/tree/1.21.11-fabric
 */
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.m9mx.cactus.glowberry.util.appleskin.client.HUDOverlayHandler;
import org.m9mx.cactus.glowberry.feature.modules.AppleSkinModule;

@Mixin(Minecraft.class)
public class MinecraftClientMixin
{
	@Inject(at = @At("HEAD"), method = "tick")
	void onTick(CallbackInfo info)
	{
		// Only tick HUD overlay if module is active
		if (HUDOverlayHandler.INSTANCE != null && AppleSkinModule.INSTANCE != null && AppleSkinModule.INSTANCE.active())
			HUDOverlayHandler.INSTANCE.onClientTick();
	}
}