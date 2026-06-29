package org.m9mx.cactus.glowberry.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import org.m9mx.cactus.glowberry.feature.modules.FastPlaceModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow private int rightClickDelay;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;

        if (FastPlaceModule.INSTANCE != null && FastPlaceModule.INSTANCE.active()) {
            if (minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof BlockItem) {
                this.rightClickDelay = 0;
            }
        }
    }
}