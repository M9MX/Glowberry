package org.m9mx.cactus.glowberry.mixin.Modules.moss;

import net.minecraft.client.ClientBrandRetriever;
import org.m9mx.cactus.glowberry.feature.modules.BrandNameChanger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public abstract class BrandClientBrandRetrieverMixin {
    @Inject(method = "getClientModName", at = @At("HEAD"), cancellable = true, remap = false)
    private static void moss$overrideBrand(CallbackInfoReturnable<String> cir) {
        if (BrandNameChanger.INSTANCE != null && BrandNameChanger.INSTANCE.active()) {
            String custom = BrandNameChanger.INSTANCE.getCustomBrandOrNull();
            if (custom != null) {
                cir.setReturnValue(custom);
                cir.cancel();
            }
        }
    }
}
