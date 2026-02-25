package org.m9mx.cactus.glowberry.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.m9mx.cactus.glowberry.feature.modules.TabListModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.ArrayList;
import java.util.List;

/**
 * Credits: https://github.com/Alex-265/mc-ping-in-tablist
 */
@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {

    @ModifyConstant(method = "render", constant = @Constant(intValue = 13))
    private int modifySpace(int original) {
        if (TabListModule.INSTANCE != null && TabListModule.INSTANCE.active() && TabListModule.INSTANCE.showPing.get()) {
            return getMaxFontSize();
        }
        return original;
    }

    private int getMaxFontSize() {
        int maxPing = getPlayerInfos()
                .stream()
                .mapToInt(PlayerInfo::getLatency)
                .map(latency -> latency <= 0 ? 999 : latency)
                .max()
                .orElse(0);
        
        String displayText = (maxPing == 0 ? "???" : maxPing) + "ms";
        return Minecraft.getInstance().font.width(" " + displayText) + 3;
    }

    /**
     * Overwrite the renderPingIcon method to display ping as text instead of icon
     */
    @Overwrite
    public void renderPingIcon(GuiGraphics guiGraphics, int width, int posX, int posY, PlayerInfo playerInfo) {
        if (TabListModule.INSTANCE == null || !TabListModule.INSTANCE.active() || !TabListModule.INSTANCE.showPing.get()) {
            // If module is disabled, render default ping icon (do nothing here, let vanilla handle it)
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
        
        int textWidth = Minecraft.getInstance().font.width(text);
        int renderX = posX + width - textWidth - 3;
        
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            text,
            renderX,
            posY,
            color,
            false
        );
    }

    @Shadow
    private List<PlayerInfo> getPlayerInfos() {
        return new ArrayList<>();
    }
}
