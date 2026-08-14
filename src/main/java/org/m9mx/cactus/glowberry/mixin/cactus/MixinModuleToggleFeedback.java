package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.feature.module.ModuleManager;
import org.m9mx.cactus.glowberry.util.ModuleMessageUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When the Module Message HUD element is placed, Cactus' toggle feedback
 * ("X enabled"/"X disabled") is shown there instead of the action bar. When the
 * element is not placed, Cactus' original behavior (action bar / chat / toast,
 * per its own setting) is kept untouched.
 */
@Mixin(value = ModuleManager.class, remap = false)
public abstract class MixinModuleToggleFeedback {

    @Inject(method = "sendToggleFeedback", at = @At("HEAD"), cancellable = true)
    private static void glowberry_routeToggleFeedback(Module module, CallbackInfo ci) {
        if (module == null) return;
        if (!ModuleMessageUtil.moduleMessageElementActive) return;

        ModuleMessageUtil.showToggleFeedback(module);
        // Cancel the original feedback so it doesn't also hit the action bar/chat
        ci.cancel();
    }
}
