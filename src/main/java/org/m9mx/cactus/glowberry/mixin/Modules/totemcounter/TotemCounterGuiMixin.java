/*
 * Adapted from TotemCounter mod by uku3lig
 * Original mod: https://github.com/uku3lig/totemcounter
 */
package org.m9mx.cactus.glowberry.mixin.Modules.totemcounter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.m9mx.cactus.glowberry.feature.modules.TotemCounterModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 26.2: health HUD rendering moved from Gui to the new net.minecraft.client.gui.Hud class
@Mixin(Hud.class)
public class TotemCounterGuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    private static ItemStack getTotemStack() {
        return new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
    }
    @Inject(method = "extractPlayerHealth", at = @At("RETURN"))
    private void renderCounter(GuiGraphicsExtractor graphics, CallbackInfo ci) {
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
        graphics.item(getTotemStack(), x, y);

        // Draw count text
        int textX = x + 20;
        graphics.text(textRenderer, text, textX, y + 4, TotemCounterModule.getColor(count));
        graphics.pose().popMatrix();
    }
}
