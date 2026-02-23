package org.m9mx.cactus.glowberry.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.m9mx.cactus.glowberry.feature.modules.FastBreakModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(MultiPlayerGameMode.class)
public class BreakMixin {

   @ModifyConstant(
        method = "continueDestroyBlock",
        constant = @Constant(intValue = 5)
   )
   private int removeBreakDelay(int original) {
      // Only modify the break delay if the FastBreakModule is active
      if (FastBreakModule.INSTANCE != null && FastBreakModule.INSTANCE.active()) {
         return 0;
      } else {
         return original;
      }
   }
}