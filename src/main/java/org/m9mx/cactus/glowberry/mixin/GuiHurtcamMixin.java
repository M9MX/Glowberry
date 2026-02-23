package org.m9mx.cactus.glowberry.mixin;

import net.minecraft.client.gui.Gui;
import org.m9mx.cactus.glowberry.feature.modules.NoHurtcamModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Gui.class)
public class GuiHurtcamMixin {
    @ModifyArg(
        method = "renderPlayerHealth", 
        index = 10, 
        at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/client/gui/Gui;renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V"
        )
    )
    public boolean modifyHeartBlink(boolean blinking) {
        // Module only affects hurtcam, not heart blinking
        return blinking;
    }
}