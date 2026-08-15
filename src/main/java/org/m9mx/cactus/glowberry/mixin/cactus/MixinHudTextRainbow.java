package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.m9mx.cactus.glowberry.util.rainbow.RainbowMode;
import org.m9mx.cactus.glowberry.util.rainbow.RainbowModeHandler;
import org.m9mx.cactus.glowberry.util.rainbow.RainbowRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies Glowberry's {@code rainbowMode} setting to the text Cactus HUD
 * elements draw. Hooks {@code HudElement#drawHudText} - the single method every
 * Cactus element uses to draw its text - where the element is directly
 * {@code this}, so the rainbow mode is read without any thread-local "current
 * element" tracking.
 *
 * <p>SMOOTH mode leaves the original call untouched (Cactus' own behavior,
 * including its per-character textChroma rendering). DIAGONAL mode re-draws the
 * text character by character with the moving diagonal gradient (see
 * {@link RainbowRenderer}).
 */
@Mixin(value = HudElement.class, remap = false)
public abstract class MixinHudTextRainbow {

    @Inject(method = "drawHudText(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/lang/String;IIZ)I", at = @At("HEAD"), cancellable = true)
    private void glowberry_applyRainbow(GuiGraphicsExtractor context, String text, int x, int y, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (text == null || text.isEmpty()) return;
        if (RainbowModeHandler.mode() != RainbowMode.DIAGONAL) return;

        HudElement<?> element = (HudElement<?>) (Object) this;
        if (!RainbowRenderer.rainbowActive(element)) return;

        cir.setReturnValue(RainbowRenderer.drawDiagonal(context, net.minecraft.client.Minecraft.getInstance().font, text, x, y, element, shadow));
    }
}
