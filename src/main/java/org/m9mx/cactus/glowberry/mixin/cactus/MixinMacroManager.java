package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.feature.macro.Macro;
import com.dwarslooper.cactus.client.feature.macro.MacroManager;
import com.dwarslooper.cactus.client.systems.config.TreeSerializerFilter;
import com.google.gson.JsonObject;
import org.m9mx.cactus.glowberry.util.cactus.macro.GlowberryMacroManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = MacroManager.class, remap = false)
public abstract class MixinMacroManager {

    @Shadow public abstract List<Macro> getMacros();

    @Inject(method = "toJson", at = @At("HEAD"))
    private void onToJson(TreeSerializerFilter filter, CallbackInfoReturnable<JsonObject> cir) {
        // Remove from memory right before serialization
        this.getMacros().removeIf(cactusMacro ->
                GlowberryMacroManager.MACROS.stream()
                        .anyMatch(glow -> glow.name.equalsIgnoreCase(cactusMacro.name))
        );
    }
}