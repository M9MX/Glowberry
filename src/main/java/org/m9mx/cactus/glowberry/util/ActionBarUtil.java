package org.m9mx.cactus.glowberry.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.m9mx.cactus.glowberry.mixin.util.GuiAccessor;

public class ActionBarUtil {
    
    private static final Minecraft mc = Minecraft.getInstance();
    
    public static void sendActionBarMessage(String message) {
        if (mc.player != null) {
            //mc.player.sendSystemMessage(Component.literal(message), true);
            mc.gui.hud.setOverlayMessage(Component.literal(message), false);
        }
    }

    public static void sendActionBarMessageWithDuration(String message, int duration) {
        if (mc.gui != null) {
            mc.gui.hud.setOverlayMessage(Component.literal(message), false);
            
            // This is the "Magic" part:
            // 26.2: overlayMessageTime moved from Gui to Hud, so cast hud to our Accessor
            ((GuiAccessor) mc.gui.hud).setOverlayMessageTime(duration);
        }
    }
}