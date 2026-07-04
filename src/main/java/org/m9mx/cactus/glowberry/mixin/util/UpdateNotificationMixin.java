package org.m9mx.cactus.glowberry.mixin.util;

import com.dwarslooper.cactus.client.gui.widget.CButtonWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.m9mx.cactus.glowberry.util.update.UpdateChecker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class UpdateNotificationMixin {
    @Unique
    private static boolean glowberry$checkStarted = false;
    @Unique
    private long glowberry$appearTime = 0;
    @Unique
    private long glowberry$dismissTime = 0;
    @Unique
    private boolean glowberry$dismissing = false;
    @Unique
    private boolean glowberry$done = false;
    @Unique
    private CButtonWidget glowberry$downloadButton;
    @Unique
    private CButtonWidget glowberry$dismissButton;

    private static final int CARD_MARGIN = 15;
    private static final int CARD_PAD = 10; // Slightly uniform padding
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 6;
    private static final int CARD_HEIGHT = 54;
    private static final int X_BTN_W = 18;
    private static final int BTN_GAP = 4;
    private static final int LEFT_OFFSET = -4;

    @Inject(method = "init", at = @At("TAIL"))
    private void glowberry$startUpdateCheck(CallbackInfo ci) {
        if (!glowberry$checkStarted) {
            glowberry$checkStarted = true;
            String currentVersion = FabricLoader.getInstance()
                    .getModContainer("glowberry-addon")
                    .orElseThrow()
                    .getMetadata()
                    .getVersion()
                    .getFriendlyString();

            // If this Glowberry version is the final one for the current Cactus version, skip checking
            if (UpdateChecker.shouldSkipCheck(currentVersion)) {
                glowberry$done = true;
                return;
            }

            glowberry$appearTime = Util.getMillis();
            new Thread(() -> {
                UpdateChecker.check(currentVersion);
            }, "Glowberry Update Check").start();
        }

        glowberry$downloadButton = new CButtonWidget(0, 0, 80, 20, Component.literal("Download"), (btn) -> {
            Util.getPlatform().openUri(UpdateChecker.getModrinthUrl());
            glowberry$dismissing = true;
            glowberry$dismissTime = Util.getMillis();
        });
        glowberry$dismissButton = new CButtonWidget(0, 0, X_BTN_W, 20, Component.literal("×"), (btn) -> {
            glowberry$dismissing = true;
            glowberry$dismissTime = Util.getMillis();
        });
        glowberry$downloadButton.visible = false;
        glowberry$dismissButton.visible = false;
        if ((Object) this instanceof Screen screen) {
            screen.addRenderableWidget(glowberry$downloadButton);
            screen.addRenderableWidget(glowberry$dismissButton);
        }
    }

    @Unique
    private float glowberry$getProgress(long now) {
        if (glowberry$dismissing) {
            long elapsed = now - glowberry$dismissTime;
            float raw = Math.min(1.0f, elapsed / 300.0f);
            if (raw >= 1.0f) {
                glowberry$done = true;
                return 0.0f;
            }
            return 1.0f - raw * raw * raw;
        } else {
            long elapsed = now - glowberry$appearTime;
            float raw = Math.min(1.0f, elapsed / 400.0f);
            return 1.0f - (1.0f - raw) * (1.0f - raw) * (1.0f - raw);
        }
    }

    @Unique
    private int glowberry$getContentAlpha() {
        long now = Util.getMillis();
        if (glowberry$dismissing) {
            long elapsed = now - glowberry$dismissTime;
            float raw = Math.min(1.0f, elapsed / 300.0f);
            return (int)(255 * (1.0f - raw * raw * raw));
        } else {
            long elapsed = now - glowberry$appearTime;
            float raw = Math.min(1.0f, elapsed / 400.0f);
            return (int)(255 * (1.0f - (1.0f - raw) * (1.0f - raw) * (1.0f - raw)));
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void glowberry$renderUpdateNotification(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (glowberry$done) {
            glowberry$downloadButton.visible = false;
            glowberry$dismissButton.visible = false;
            return;
        }
        if (!UpdateChecker.isCheckDone() || !UpdateChecker.isUpdateAvailable()) return;

        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (!(screen instanceof TitleScreen)) return;

        long now = Util.getMillis();
        float progress = glowberry$getProgress(now);
        if (glowberry$done) return;

        int contentAlpha = glowberry$getContentAlpha();
        int bgAlpha = (int)(contentAlpha * 0.45f);

        String versionText = "§aGlowberry §e" + UpdateChecker.getLatestVersion() + " §7available!";
        int textWidth = mc.font.width(versionText);

        // FIXED: Explicit card width accounting for full widget layouts & double padding edges
        int innerContentWidth = ICON_SIZE + ICON_GAP + textWidth;
        int buttonLayoutWidth = textWidth + BTN_GAP + X_BTN_W;
        int maxInnerWidth = Math.max(innerContentWidth, buttonLayoutWidth);
        int cardWidth = CARD_PAD + maxInnerWidth + CARD_PAD;

        // Position alignment
        int x = CARD_MARGIN;
        int restingY = screen.height - CARD_HEIGHT - CARD_MARGIN;
        int slideOffset = (int)(30.0f * (1.0f - progress));
        int y = restingY + slideOffset;

        // Subtle dark background
        int bg = (bgAlpha << 24) | 0x0A0A0A;
        context.fill(x + 1, y + 1, x + cardWidth - 1, y + CARD_HEIGHT - 1, bg);

        // Border frame
        int bAlpha = Math.min(255, bgAlpha * 2);
        int borderColor = (bAlpha << 24) | 0x555555;
        context.fill(x, y, x + cardWidth, y + 1, borderColor);
        context.fill(x, y + CARD_HEIGHT - 1, x + cardWidth, y + CARD_HEIGHT, borderColor);
        context.fill(x, y, x + 1, y + CARD_HEIGHT, borderColor);
        context.fill(x + cardWidth - 1, y, x + cardWidth, y + CARD_HEIGHT, borderColor);

        // Inner highlights
        context.fill(x + 1, y + 1, x + cardWidth - 1, y + 2, (bAlpha << 24) | 0x333333);
        context.fill(x + 1, y + 1, x + 2, y + CARD_HEIGHT - 1, (bAlpha << 24) | 0x333333);

        // FIXED: Properly align item icon vertically
        int iconX = x + CARD_PAD;
        int iconY = y + 8;
        context.fakeItem(new ItemStack(Items.GLOW_BERRIES), iconX, iconY);

        // Version text positioning aligned perfectly to icon
        int textX = iconX + ICON_SIZE + ICON_GAP;
        int textY = iconY + (ICON_SIZE - mc.font.lineHeight) / 2;
        context.text(mc.font, Component.literal(versionText), textX, textY, (contentAlpha << 24) | 0xFFFFFF);

        // Button alignment matching the text start bounds exactly
        int btnY = y + CARD_HEIGHT - 20 - 6;
        int dlBtnW = textWidth;

        glowberry$downloadButton.x = textX - X_BTN_W + LEFT_OFFSET;
        glowberry$downloadButton.y = btnY;
        glowberry$downloadButton.width = dlBtnW;
        glowberry$downloadButton.visible = !glowberry$dismissing;

        glowberry$dismissButton.x = glowberry$downloadButton.x + dlBtnW + BTN_GAP;
        glowberry$dismissButton.y = btnY;
        glowberry$dismissButton.visible = !glowberry$dismissing;
    }
}