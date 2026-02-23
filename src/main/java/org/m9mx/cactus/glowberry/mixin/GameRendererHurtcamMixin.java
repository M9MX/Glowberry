package org.m9mx.cactus.glowberry.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.m9mx.cactus.glowberry.feature.modules.NoHurtcamModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererHurtcamMixin {
    
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    public void disableHurtCam(PoseStack matrices, float tickDelta, CallbackInfo ci) {
        if (NoHurtcamModule.INSTANCE != null && NoHurtcamModule.INSTANCE.active()) {
            ci.cancel();
        }
    }
}