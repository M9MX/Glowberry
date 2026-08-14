package org.m9mx.cactus.glowberry.mixin.Modules.waila;

import net.minecraft.world.entity.AgeableMob;
import org.m9mx.cactus.glowberry.util.waila.MobAgeTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records the real age of every mob on the integrated server (singleplayer).
 * {@code AgeableMob.aiStep} calls {@code setAge} every tick there, and the
 * client-side {@code getAge()} only returns -1/+1, so without this the Age and
 * Breed Waila lines cannot know baby growth or breeding cooldowns. On
 * multiplayer servers it never fires and the display falls back to estimates.
 */
@Mixin(AgeableMob.class)
public class MixinAgeableMob {

    @Inject(method = "setAge", at = @At("RETURN"))
    private void glowberry_recordRealAge(int age, CallbackInfo ci) {
        AgeableMob mob = (AgeableMob) (Object) this;
        if (!mob.level().isClientSide()) {
            MobAgeTracker.recordAge(mob.getUUID(), age);
        }
    }
}
