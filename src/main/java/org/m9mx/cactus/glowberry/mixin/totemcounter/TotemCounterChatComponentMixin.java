/*
 * Adapted from TotemCounter mod by uku3lig
 * Original mod: https://github.com/uku3lig/totemcounter
 */
package org.m9mx.cactus.glowberry.mixin.totemcounter;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.m9mx.cactus.glowberry.feature.modules.TotemCounterModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

@Mixin(ChatComponent.class)
public class TotemCounterChatComponentMixin {
    @Unique
    private static final List<String> roundEndMessages = Arrays.asList(
        "Winners:", "has won the round.", "has won the game!", "Winner: NONE!", "Match Complete"
    );

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"))
    public void resetCounterOnRoundEnd(Component message, MessageSignature signature, GuiMessageTag indicator, CallbackInfo ci) {
        if (TotemCounterModule.INSTANCE == null || !TotemCounterModule.INSTANCE.active()) return;
        
        if (roundEndMessages.stream().anyMatch(m -> message.getString().contains(m))) {
            TotemCounterModule.getPops().clear();
        }
    }
}
