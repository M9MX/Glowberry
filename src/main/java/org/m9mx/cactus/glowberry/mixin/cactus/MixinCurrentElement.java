package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.m9mx.cactus.glowberry.util.CurrentHudElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pushes the {@link HudElement} currently rendering into
 * {@link CurrentHudElement}, so the text-renderer mixin knows which element's
 * TextColor / Text Chroma settings apply to the text being drawn. Cleared when
 * the render finishes so text drawn outside of Cactus HUD elements is never
 * touched.
 */
@Mixin(value = HudElement.class, remap = false)
public abstract class MixinCurrentElement {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIFZ)V", at = @At("HEAD"))
    private void glowberry_pushCurrent(GuiGraphicsExtractor context, int x, int y, int width, int height, float delta, boolean inEditor, CallbackInfo ci) {
        CurrentHudElement.push((HudElement<?>) (Object) this);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIFZ)V", at = @At("RETURN"))
    private void glowberry_popCurrent(GuiGraphicsExtractor context, int x, int y, int width, int height, float delta, boolean inEditor, CallbackInfo ci) {
        CurrentHudElement.pop();
    }
}
