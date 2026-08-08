package org.m9mx.cactus.glowberry.mixin.cactus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.m9mx.cactus.glowberry.util.cactus.macro.GlowberryMacroManager;
import org.m9mx.cactus.glowberry.util.cactus.macro.GlowberryMacroManager.GlowberryMacro;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class MixinChatScreen {

    /**
     * TARGET: handleChatInput
     * MISSION: INTERCEPT MACROS AND LOG TO HISTORY USING 'addRecentChat'
     */
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void onHandleChatInput(String message, boolean addToHistory, CallbackInfo ci) {
        String trimmed = message.trim();

        GlowberryMacro match = GlowberryMacroManager.MACROS.stream()
                .filter(m -> m.isStringMode && m.trigger.equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(null);

        if (match != null) {
            var mc = Minecraft.getInstance();
            var player = mc.player;

            // 1. MANUALLY ADD TO HISTORY USING THE CORRECT MOJANG METHOD, SIR!
            if (addToHistory) {
                // In 1.21+ Mojang, 'gui.getChat()' returns the ChatComponent
                mc.gui.getChat().addRecentChat(message);
            }

            // 2. EXECUTE THE MACRO PAYLOAD VIA CONNECTION
            if (player != null && player.connection != null) {
                for (String action : match.commands) {
                    String cmd = action.trim();
                    if (cmd.isEmpty()) continue;

                    if (cmd.startsWith("/")) {
                        player.connection.sendCommand(cmd.substring(1));
                    } else {
                        player.connection.sendChat(cmd);
                    }
                }
            }

            // 3. TERMINATE ORIGINAL GUI PROCESS
            ci.cancel();
        }
    }
}