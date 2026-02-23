package org.m9mx.cactus.glowberry.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("rightClickDelay")
    void setRightClickDelay(int delay);
    
    @Accessor("rightClickDelay")
    int getRightClickDelay();
}