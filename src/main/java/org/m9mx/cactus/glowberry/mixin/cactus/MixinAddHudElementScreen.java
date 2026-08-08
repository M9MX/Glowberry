package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.gui.hud.AddHudElementScreen;
import com.dwarslooper.cactus.client.gui.screen.CScreen;
import com.dwarslooper.cactus.client.gui.widget.CButtonWidget;
import net.minecraft.network.chat.Component;
import org.m9mx.cactus.glowberry.util.cactus.placeholders.PlaceholderBrowserScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AddHudElementScreen.class)
public abstract class MixinAddHudElementScreen extends CScreen {

    public MixinAddHudElementScreen(String key) {
        super(key);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void glowberry$addPlaceholderButton(CallbackInfo ci) {
        // Back button is at (6, 6).
        // We put this next to it at (28, 6).
        this.addRenderableWidget(new CButtonWidget(
                28, 6, 110, 20,
                Component.literal("Placeholder List"),
                (button) -> {
                    this.minecraft.setScreen(new PlaceholderBrowserScreen(this));
                }
        ));
    }
}
