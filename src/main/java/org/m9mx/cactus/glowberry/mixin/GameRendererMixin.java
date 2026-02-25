package org.m9mx.cactus.glowberry.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.BlockItem;
import org.m9mx.cactus.glowberry.feature.modules.FastPlaceModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "pick", at = @At("HEAD"))
    private void onPickStart(float f, CallbackInfo ci) {
        if (FastPlaceModule.INSTANCE != null && FastPlaceModule.INSTANCE.active()) {
            Minecraft minecraft = Minecraft.getInstance();
            
            // Only apply fast place if holding a block item
            if (minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof BlockItem) {
                    MinecraftAccessor accessor = (MinecraftAccessor) (Object) minecraft;
                    accessor.setRightClickDelay(0);
            }
        }
    }
}