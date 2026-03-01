/*
 * Adapted from Scribble mod by chrrs
 * Original mod: https://github.com/chrrs/scribble
 */

package org.m9mx.cactus.glowberry.mixin.scribble;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.m9mx.cactus.glowberry.feature.modules.ScribbleModule;
import org.m9mx.cactus.glowberry.util.scribble.screen.ScribbleBookEditScreen;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@NullMarked
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    // Disable the narrator hotkey when editing a book while not holding SHIFT,
    // as it conflicts with the bold hotkey.
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/GameNarrator;isActive()Z"))
    public boolean isNarratorActive(GameNarrator instance, Operation<Boolean> original, @Local(argsOnly = true) KeyEvent event) {
        ScribbleModule module = ScribbleModule.INSTANCE;
        if (module != null && module.active() && minecraft.screen instanceof ScribbleBookEditScreen && !event.hasShiftDown()) {
            return false;
        } else {
            return original.call(instance);
        }
    }
}
