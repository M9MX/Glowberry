package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.gui.widget.list.MacroListWidget;
import org.m9mx.cactus.glowberry.util.cactus.macro.GlowberryMacroManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MacroListWidget.MacroEntry.class, remap = false)
public abstract class MixinMacroEntry {
    @Shadow @Final com.dwarslooper.cactus.client.feature.macro.Macro macro;

    @Inject(method = "delete", at = @At("HEAD"))
    private void onDelete(CallbackInfo ci) {
        // Check if this macro name exists in our Glowberry system
        // If it does, remove it from our JSON and save
        boolean removed = GlowberryMacroManager.MACROS.removeIf(m -> m.name.equals(this.macro.name));
        if (removed) {
            GlowberryMacroManager.saveToFile();
        }
    }
}