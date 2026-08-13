package org.m9mx.cactus.glowberry.mixin.Modules.LightLevel;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.m9mx.cactus.glowberry.feature.overlay.LightLevelOverlayHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Credits: https://github.com/lugosieben/lightoverlay
 * 26.2: LevelRenderer.blockChanged was removed; client block changes now flow
 * through ClientLevel.sendBlockUpdated -> LevelExtractor.blockChanged.
 */
@Mixin(ClientLevel.class)
public class LightLevelBlockChangeMixin {
    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void onBlockChanged(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        LightLevelOverlayHandler.clear(pos);
        // A block change can also cover/uncover the walkable surface below it
        // (e.g. a snow layer placed on top of grass), so invalidate that too.
        LightLevelOverlayHandler.clear(pos.below());
    }
}
