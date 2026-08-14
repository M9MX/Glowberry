package org.m9mx.cactus.glowberry.mixin.Modules.AntiAfk;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import org.m9mx.cactus.glowberry.feature.modules.AntiAfkModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overrides the player's movement input while the Anti AFK module is running
 * and the real player isn't pressing anything. Runs at the end of the input
 * tick, so the simulated keys are what the player AI step sees this tick.
 */
@Mixin(ClientInput.class)
public abstract class ClientInputMixin {

    @Shadow
    public Input keyPresses;

    @Inject(method = "tick", at = @At("RETURN"))
    private void glowberry_antiAfkInput(CallbackInfo ci) {
        AntiAfkModule module = AntiAfkModule.INSTANCE;
        // Only take over input while the anti-AFK behavior is switched on (the
        // module itself may stay enabled without driving any input).
        if (module == null || !module.shouldOverrideInput(this.keyPresses)) return;

        module.updateSimulatedInput();
        Input simulated = module.simulatedInput();
        if (simulated != null) {
            this.keyPresses = simulated;
        }
    }
}
