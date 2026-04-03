package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.feature.macro.Macro;
import com.dwarslooper.cactus.client.gui.screen.impl.MacroListScreen;
import com.dwarslooper.cactus.client.gui.widget.list.MacroListWidget;
import com.dwarslooper.cactus.client.systems.key.KeyBind;
import org.m9mx.cactus.glowberry.util.cactus.macro.GlowberryMacroManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(value = MacroListWidget.class, remap = false)
public abstract class MixinMacroListWidget {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(MacroListScreen parent, CallbackInfo ci) {
        MacroListWidget self = (MacroListWidget) (Object) this;

        for (GlowberryMacroManager.GlowberryMacro glowMacro : GlowberryMacroManager.MACROS) {
            // DUPLICATION FIX: Check if a macro with this name is already in the UI list
            boolean alreadyExists = self.children().stream()
                    .anyMatch(entry -> entry.macro.name.equalsIgnoreCase(glowMacro.name));

            if (!alreadyExists) {
                Macro fake = new Macro(glowMacro.name, KeyBind.none(), new ArrayList<>(), true);
                self.addEntry(self.new MacroEntry(fake));
            }
        }
    }
}