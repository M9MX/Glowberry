package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.systems.config.ConfigHandler;
import org.m9mx.cactus.glowberry.util.cactus.macro.GlowberryMacroManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ConfigHandler.class, remap = false)
public class MixinConfigHandler {

    @Inject(method = "save", at = @At("HEAD"))
    private void onSave(CallbackInfoReturnable<?> cir) {
        GlowberryMacroManager.saveToFile();
    }

    @Inject(method = "reload", at = @At("TAIL"))
    private void onLoad(CallbackInfo ci) {
        GlowberryMacroManager.load();
    }
}