package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Vector2i;
import org.m9mx.cactus.glowberry.feature.hud.HudRenderControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Modifies Cactus' HUD element renderer so elements can be hidden or shifted at
 * render time without touching their stored model.
 *
 * <p>Every Cactus element (and every Glowberry element via its
 * {@code super.render} call) funnels through
 * {@code HudElement#render(GuiGraphicsExtractor, int, int, int, int, float,
 * boolean)} - in-game as well as in the HUD editor. This mixin hooks that
 * single method: in-game it applies the hide flag registered in
 * {@link HudRenderControl} (cancelling the whole render), and it shifts the
 * draw coordinates passed to {@code renderBackground} / {@code renderContent}
 * by the registered render-time offset (a fake move). The stored position /
 * size / anchor are never changed, so Cactus (and its HUD editor) always sees
 * the element where the user placed it - hiding and animations are purely
 * visual.</p>
 */
@Mixin(value = HudElement.class, remap = false)
public abstract class HudElementRenderMixin {

    /**
     * Hide: cancel the whole render in-game when the element is registered as
     * hidden. The HUD editor always shows the element.
     */
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIFZ)V", at = @At("HEAD"), cancellable = true)
    private void glowberry_hideElement(GuiGraphicsExtractor context, int x, int y, int width, int height, float delta, boolean inEditor, CallbackInfo ci) {
        if (inEditor) {
            return; // HUD editor: always show the element
        }
        if (HudRenderControl.isHidden((HudElement) (Object) this)) {
            ci.cancel();
        }
    }

    /**
     * Fake move (background): shift the draw coordinates of the style background
     * box by the registered offset. Skipped in the HUD editor, where the box
     * always shows at its true model position.
     */
    @ModifyArgs(method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIFZ)V",
            at = @At(value = "INVOKE", target = "Lcom/dwarslooper/cactus/client/gui/hud/element/HudElement;renderBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIFZ)V"))
    private void glowberry_applyOffsetToBackground(Args args) {
        if ((boolean) args.get(6)) {
            return; // HUD editor: always show the true model
        }
        Vector2i offset = HudRenderControl.getOffset((HudElement) (Object) this);
        if (offset != null) {
            args.set(1, (Integer) args.get(1) + offset.x());
            args.set(2, (Integer) args.get(2) + offset.y());
        }
    }

    /**
     * Fake move (content): shift the draw coordinates of the content by the
     * registered offset. The content call inside {@code render} only happens
     * in-game (the editor path renders a placeholder instead), so no editor
     * check is needed here.
     */
    @ModifyArgs(method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIFZ)V",
            at = @At(value = "INVOKE", target = "Lcom/dwarslooper/cactus/client/gui/hud/element/HudElement;renderContent(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIIFZ)V"))
    private void glowberry_applyOffsetToContent(Args args) {
        Vector2i offset = HudRenderControl.getOffset((HudElement) (Object) this);
        if (offset != null) {
            args.set(1, (Integer) args.get(1) + offset.x());
            args.set(2, (Integer) args.get(2) + offset.y());
        }
    }
}
