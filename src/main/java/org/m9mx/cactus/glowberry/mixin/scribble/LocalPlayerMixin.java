/*
 * Adapted from Scribble mod by chrrs
 * Original mod: https://github.com/chrrs/scribble
 */

package org.m9mx.cactus.glowberry.mixin.scribble;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.m9mx.cactus.glowberry.util.scribble.screen.ScribbleBookEditScreen;
import org.m9mx.cactus.glowberry.feature.modules.ScribbleModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@NullMarked
@Mixin(value = LocalPlayer.class, priority = 500)
public abstract class LocalPlayerMixin {
    @WrapOperation(method = "openItemGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    public void overrideBookViewScreen(Minecraft instance, Screen screen, Operation<Void> original, @Local(argsOnly = true) ItemStack itemStack, @Local(argsOnly = true) InteractionHand hand, @Local WritableBookContent book) {
        ScribbleModule module = ScribbleModule.INSTANCE;
        if (module == null || !module.active()) {
            original.call(instance, screen);
            return;
        }
        if (instance.hasShiftDown() && module.openVanillaBookScreenOnShift.get()) {
            original.call(instance, screen);
        } else {
            // FIXME: ideally, I'd like to avoid even constructing the original BookEditScreen.
            original.call(instance, new ScribbleBookEditScreen((LocalPlayer) (Object) this, itemStack, hand, book));
        }
    }
}
