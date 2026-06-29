package org.m9mx.cactus.glowberry.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.m9mx.cactus.glowberry.feature.modules.TabListModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Credits: https://github.com/Alex-265/mc-ping-in-tablist
 */
@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Final
    @Shadow
    private Minecraft minecraft;

    // Targets the proper modern state mapping
    @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 13))
    private int modifySpace(int original) {
        if (TabListModule.INSTANCE != null && TabListModule.INSTANCE.active() && TabListModule.INSTANCE.showPing.get()) {
            return getMaxFontSize();
        }
        return original;
    }

    @Unique
    private int getMaxFontSize() {
        int maxPing = getPlayerInfos()
                .stream()
                .mapToInt(PlayerInfo::getLatency)
                .map(latency -> latency <= 0 ? 999 : latency)
                .max()
                .orElse(0);

        String displayText = (maxPing == 0 ? "???" : maxPing) + "ms";
        return minecraft.font.width(" " + displayText) + 3;
    }

    // Swapped from @Overwrite to an clean @Inject with cancel()
    @Inject(method = "extractPingIcon", at = @At("HEAD"), cancellable = true)
    public void renderPingIcon(GuiGraphicsExtractor guiGraphics, int width, int posX, int posY, PlayerInfo playerInfo, CallbackInfo ci) {
        if (TabListModule.INSTANCE == null || !TabListModule.INSTANCE.active() || !TabListModule.INSTANCE.showPing.get()) {
            // Let vanilla execute if module is disabled
            return;
        }

        int latency = playerInfo.getLatency();
        String latencyText = latency <= 0 ? "???" : String.valueOf(latency);
        String text = latencyText + "ms";

        int color = 0xFF808080; // Gray default
        if (latency > 0) {
            if (latency < 150) {
                color = 0xFF00FF00; // Green
            } else if (latency < 300) {
                color = 0xFFFFAA00; // Orange
            } else if (latency < 600) {
                color = 0xFFFF0000; // Red
            } else if (latency < 1000) {
                color = 0xFF990000; // Dark red
            } else {
                color = 0xFF000000; // Black
            }
        }

        int textWidth = minecraft.font.width(text);
        int renderX = posX + width - textWidth - 3;

        guiGraphics.text(minecraft.font, text, renderX, posY, color, false);

        // Cancel the execution context so vanilla doesn't render its signal bars over our numbers
        ci.cancel();
    }

    @Shadow
    private List<PlayerInfo> getPlayerInfos() {
        return new ArrayList<>();
    }
}