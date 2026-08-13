package org.m9mx.cactus.glowberry.mixin.util;

import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class)
public interface GuiAccessor {
    // This creates a "setter" for the private field overlayMessageTime
    // (26.2: field moved from Gui to Hud; interface name kept as GuiAccessor)
    @Accessor("overlayMessageTime")
    void setOverlayMessageTime(int time);
}
