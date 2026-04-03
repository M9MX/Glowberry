package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.feature.macro.Macro;
import com.dwarslooper.cactus.client.feature.macro.Action;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(value = Macro.class, remap = false)
public abstract class MixinCactusMacro {
    @Shadow public List<Action<?>> actions;
    @Shadow public boolean enabled;

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void onRun(CallbackInfo ci) {
        if (this.enabled) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.connection != null) {
                for (Action<?> action : this.actions) {
                    String cmd = action.data().toString();
                    if (cmd.startsWith("/")) {
                        mc.player.connection.sendCommand(cmd.substring(1));
                    } else {
                        mc.player.connection.sendChat(cmd);
                    }
                }
                ci.cancel();
            }
        }
    }
}