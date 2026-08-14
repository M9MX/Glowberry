package org.m9mx.cactus.glowberry.mixin.Modules.waila;

import net.minecraft.world.entity.animal.Animal;
import org.m9mx.cactus.glowberry.util.waila.MobAgeTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records when an animal enters (and leaves) love mode on the integrated server,
 * so the Waila Breed line can show "Love" and switch to the breeding cooldown
 * once the pair has mated. The love state is not synced to the client, so this
 * only works in singleplayer; elsewhere the Breed line shows Baby/Cooldown/Ready
 * from whatever age data is available.
 */
@Mixin(Animal.class)
public class MixinAnimalBreeding {

    @Inject(method = "setInLoveTime", at = @At("RETURN"))
    private void glowberry_recordLove(int ticks, CallbackInfo ci) {
        Animal animal = (Animal) (Object) this;
        if (!animal.level().isClientSide()) {
            MobAgeTracker.recordLoveUntil(animal.getUUID(), animal.level().getGameTime() + ticks);
        }
    }

    @Inject(method = "resetLove", at = @At("RETURN"))
    private void glowberry_clearLove(CallbackInfo ci) {
        Animal animal = (Animal) (Object) this;
        if (!animal.level().isClientSide()) {
            MobAgeTracker.clearLove(animal.getUUID());
        }
    }
}
