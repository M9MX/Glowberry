/*
 * Adapted from TotemCounter mod by uku3lig
 * Original mod: https://github.com/uku3lig/totemcounter
 */
package org.m9mx.cactus.glowberry.mixin.totemcounter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.m9mx.cactus.glowberry.feature.modules.TotemCounterModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class TotemCounterGuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    private static final ItemStack TOTEM = new ItemStack(Items.TOTEM_OF_UNDYING);

    @Inject(method = "renderPlayerHealth", at = @At("RETURN"))
    private void renderCounter(GuiGraphics graphics, CallbackInfo ci) {
        if (minecraft.player == null) return;
        if (TotemCounterModule.INSTANCE == null || !TotemCounterModule.INSTANCE.active()) return;
        if (!TotemCounterModule.INSTANCE.displayEnabled.get()) return;

        Font textRenderer = minecraft.font;

        int count = TotemCounterModule.getCount(minecraft.player);
        if (count == 0) return;

        MutableComponent text = Component.literal(String.valueOf(count));

        // Default position (below experience bar)
        int x = graphics.guiWidth() / 2 - 8;
        int y = graphics.guiHeight() - 38 - textRenderer.lineHeight;
        if (minecraft.player.experienceLevel > 0) y -= 6;

        // Draw totem icon
        graphics.pose().pushMatrix();
        graphics.renderItem(TOTEM, x, y);

        // Draw count text
        int textX = x + 20;
        graphics.drawString(textRenderer, text, textX, y + 4, TotemCounterModule.getColor(count));
        graphics.pose().popMatrix();
    }
}
