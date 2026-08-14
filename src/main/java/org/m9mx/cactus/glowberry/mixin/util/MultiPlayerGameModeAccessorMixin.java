package org.m9mx.cactus.glowberry.mixin.util;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.m9mx.cactus.glowberry.accessor.IMultiPlayerGameModeAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessorMixin extends IMultiPlayerGameModeAccessor {
    @Accessor("destroyProgress")
    @Override
    float glowberry_getDestroyProgress();
}
