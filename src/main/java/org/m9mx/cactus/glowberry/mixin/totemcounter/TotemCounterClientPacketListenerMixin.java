/*
 * Adapted from TotemCounter mod by uku3lig
 * Original mod: https://github.com/uku3lig/totemcounter
 */
package org.m9mx.cactus.glowberry.mixin.totemcounter;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import org.m9mx.cactus.glowberry.feature.modules.TotemCounterModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ClientPacketListener.class)
public class TotemCounterClientPacketListenerMixin {
    @Shadow private ClientLevel level;

    @Inject(method = "handleEntityEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V"))
    public void updateCounter(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (TotemCounterModule.INSTANCE == null || !TotemCounterModule.INSTANCE.active()) return;

        Entity entity = packet.getEntity(level);
        if (entity instanceof RemotePlayer player) {
            UUID uuid = player.getUUID();
            TotemCounterModule.getPops().compute(uuid, (u, i) -> i == null ? 1 : i + 1);
        }
    }
}
