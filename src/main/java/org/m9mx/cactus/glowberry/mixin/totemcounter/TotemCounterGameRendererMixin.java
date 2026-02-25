/*
 * Adapted from TotemCounter mod by uku3lig
 * Original mod: https://github.com/uku3lig/totemcounter
 */
package org.m9mx.cactus.glowberry.mixin.totemcounter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.m9mx.cactus.glowberry.feature.modules.TotemCounterModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(GameRenderer.class)
public class TotemCounterGameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "displayItemActivation", at = @At("HEAD"))
    public void updateClientCounter(ItemStack floatingItem, CallbackInfo ci) {
        if (minecraft.player == null) return;
        if (!TotemCounterModule.INSTANCE.active()) return;
        if (floatingItem.is(Items.TOTEM_OF_UNDYING)) {
            UUID uuid = minecraft.player.getUUID();
            TotemCounterModule.getPops().putIfAbsent(uuid, 0);
            TotemCounterModule.getPops().computeIfPresent(uuid, (u, i) -> i + 1);
        }
    }
}
