package org.m9mx.cactus.glowberry.mixin.util;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public interface MinecraftRightClickAccess {
    @Accessor("rightClickDelay")
    void setRightClickDelay(int delay);
    
    @Accessor("rightClickDelay")
    int getRightClickDelay();
}