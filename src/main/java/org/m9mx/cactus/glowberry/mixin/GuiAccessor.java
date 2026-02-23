package org.m9mx.cactus.glowberry.mixin;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiAccessor {
    // This creates a "setter" for the private field overlayMessageTime
    @Accessor("overlayMessageTime")
    void setOverlayMessageTime(int time);
}