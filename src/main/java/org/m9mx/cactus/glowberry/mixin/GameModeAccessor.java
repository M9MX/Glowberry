package org.m9mx.cactus.glowberry.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.network.ClientPlayerInteractionManager")
public interface GameModeAccessor {
    @Accessor("gameMode")
    MultiPlayerGameMode getGameMode();
}