package org.m9mx.cactus.glowberry.mixin.Modules.AutoClicker;

import org.m9mx.cactus.glowberry.accessor.IMinecraftClickAccessor;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftClickAccessorMixin extends IMinecraftClickAccessor {
    @Invoker("startAttack")
    @Override
    boolean glowberry_StartAttack();

    @Invoker("startUseItem")
    @Override
    void glowberry_StartUseItem();
}
