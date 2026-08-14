package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.UpdateReason;
import org.m9mx.cactus.glowberry.util.ModuleMessageUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Records every module toggle (enabled/disabled) with a timestamp so the
 * Modules List HUD element can animate entries when modules change. Runs for
 * every toggle path (module list clicks, keybinds, commands) - but only after
 * the state was actually applied, so config-restore toggles (which go through
 * a different UpdateReason) are also caught harmlessly.
 */
@Mixin(value = Module.class, remap = false)
public abstract class MixinModuleToggleTrack {

    @Inject(method = "active(ZLcom/dwarslooper/cactus/client/systems/UpdateReason;)Lcom/dwarslooper/cactus/client/feature/module/Module;", at = @At("RETURN"))
    private void glowberry_trackToggle(boolean active, UpdateReason reason, CallbackInfoReturnable<Module> cir) {
        Module self = (Module) (Object) this;
        ModuleMessageUtil.recordToggle(self);
    }
}
