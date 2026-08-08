package org.m9mx.cactus.glowberry.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.m9mx.cactus.glowberry.mixin.util.GuiAccessor;

public class ActionBarUtil {
    
    private static final Minecraft mc = Minecraft.getInstance();
    
    public static void sendActionBarMessage(String message) {
        if (mc.player != null) {
            //mc.player.sendSystemMessage(Component.literal(message), true);
            mc.gui.setOverlayMessage(Component.literal(message), false);
        }
    }

    public static void sendActionBarMessageWithDuration(String message, int duration) {
        if (mc.gui != null) {
            mc.gui.setOverlayMessage(Component.literal(message), false);
            
            // This is the "Magic" part:
            // Cast the Gui to our Accessor to set the private field
            ((GuiAccessor) mc.gui).setOverlayMessageTime(duration);
        }
    }
}