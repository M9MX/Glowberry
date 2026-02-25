/*
 * Adapted from TotemCounter mod by uku3lig
 * Original mod: https://github.com/uku3lig/totemcounter
 */
package org.m9mx.cactus.glowberry.mixin.totemcounter;

import net.minecraft.world.entity.player.Player;
import org.m9mx.cactus.glowberry.feature.modules.TotemCounterModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class TotemCounterPlayerDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void onPlayerDeath(CallbackInfo ci) {
        if (TotemCounterModule.INSTANCE == null || !TotemCounterModule.INSTANCE.active()) return;
        
        Player self = (Player) (Object) this;
        // Remove pop count when player dies
        TotemCounterModule.getPops().remove(self.getUUID());
    }
}
