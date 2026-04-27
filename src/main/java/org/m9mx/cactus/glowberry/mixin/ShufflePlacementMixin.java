package org.m9mx.cactus.glowberry.mixin;

import org.m9mx.cactus.glowberry.feature.modules.ShuffleModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(MultiPlayerGameMode.class)
public class ShufflePlacementMixin {

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                             CallbackInfoReturnable<InteractionResult> cir) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        if (ShuffleModule.INSTANCE == null || !ShuffleModule.INSTANCE.active()) {
            return;
        }

        if (!(player.getMainHandItem().getItem() instanceof BlockItem)) {
            return;
        }

        InteractionResult result = cir.getReturnValue();
        if (result == null || !result.consumesAction()) {
            return;
        }

        ShuffleModule.INSTANCE.onBlockPlaced();
    }
}

