package org.m9mx.cactus.glowberry.mixin.cactus;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.m9mx.cactus.glowberry.util.AutoSaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cactus only persists its settings when the client closes. This mixin triggers a
 * save on every screen change or close: {@code Gui#setScreen} is called both when
 * a new screen opens and when one is closed (with {@code null}), so hooking it
 * covers every screen change - vanilla and Cactus screens alike.
 */
@Mixin(Gui.class)
public class MixinGuiSetScreen {

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void glowberry_saveOnScreenChange(Screen screen, CallbackInfo ci) {
        AutoSaveHandler.onScreenChanged();
    }
}
