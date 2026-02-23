package org.m9mx.cactus.glowberry.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.m9mx.cactus.glowberry.feature.modules.FastPlaceModule;
import org.m9mx.cactus.glowberry.mixin.MinecraftAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "pick", at = @At("HEAD"))
    private void onPickStart(float f, CallbackInfo ci) {
        if (FastPlaceModule.INSTANCE != null && 
            FastPlaceModule.INSTANCE.active()) {  // Using the proper active() method instead of fastPlaceActive
            // When fast place is enabled, we'll set the right click delay to 0
            // This reduces the delay between block placements
            Minecraft minecraft = Minecraft.getInstance();
            MinecraftAccessor accessor = (MinecraftAccessor) (Object) minecraft;
            accessor.setRightClickDelay(0);
        }
    }
}