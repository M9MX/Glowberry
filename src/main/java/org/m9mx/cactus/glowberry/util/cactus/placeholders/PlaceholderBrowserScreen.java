package org.m9mx.cactus.glowberry.util.cactus.placeholders;

import com.dwarslooper.cactus.client.gui.screen.CScreen;
import com.dwarslooper.cactus.client.gui.widget.CButtonWidget;
import com.dwarslooper.cactus.client.util.CactusConstants;
import com.dwarslooper.cactus.client.util.game.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PlaceholderBrowserScreen extends CScreen {
    private PlaceholderListWidget listWidget;
    private String currentCategory = "All";
    private long copyTimestamp = -1;
    private int lastWidth, lastHeight;

    public PlaceholderBrowserScreen(CScreen parent) {
        super("placeholder_browser");
        this.parent = parent;
    }

    @Override
    public void init() {
        super.init();
        this.lastWidth = this.width;
        this.lastHeight = this.height;

        int boxWidth = (int)((this.width / 3.0) * 1.2);
        int boxX = (this.width - boxWidth) / 2;
        int listY = 75;

        List<String> categories = PlaceholderRegistryList.PLACEHOLDERS.stream()
                .map(PlaceholderInfo::category).distinct().collect(Collectors.toList());
        categories.add(0, "All");

        int spacing = 4;
        int btnWidth = (boxWidth - (spacing * (categories.size() - 1))) / categories.size();

        for (int i = 0; i < categories.size(); i++) {
            String cat = categories.get(i);
            this.addRenderableWidget(new CButtonWidget(boxX + (i * (btnWidth + spacing)), 40, btnWidth, 20, Component.literal(cat), (btn) -> {
                this.currentCategory = cat;
                this.listWidget.refresh();
            }));
        }

        this.listWidget = new PlaceholderListWidget(CactusConstants.mc, boxWidth, this.height - listY - 30, listY, 28);
        this.listWidget.setX(boxX);
        this.addRenderableWidget(this.listWidget);
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (this.width != lastWidth || this.height != lastHeight) {
            this.init();
        }

        super.render(context, mouseX, mouseY, delta);

        if (this.listWidget != null) {
            int boxWidth = (int)((this.width / 3.0) * 1.2);
            int x = (this.width - boxWidth) / 2;
            int colW = (boxWidth - 10) / 3;

            RenderUtils.drawText(context, "§6§nPlaceholder Key", x + 5, 62, -1);
            RenderUtils.drawText(context, "§6§nExample Value", x + colW + 5, 62, -1);
            RenderUtils.drawText(context, "§6§nDescription", x + (colW * 2) + 5, 62, -1);

            this.listWidget.render(context, mouseX, mouseY, delta);
        }

        // Draw this LAST so it stays on top without needing complex 3D math
        renderCopyNotification(context);
    }

    private void renderCopyNotification(GuiGraphics context) {
        if (copyTimestamp == -1) return;
        long elapsed = System.currentTimeMillis() - copyTimestamp;
        if (elapsed > 2000) { copyTimestamp = -1; return; }

        float alpha = elapsed < 200 ? elapsed / 200f : (elapsed > 1800 ? (2000 - elapsed) / 200f : 1f);
        int alphaInt = (int)(alpha * 255);

        int toastW = 120;
        int toastX = (this.width - toastW) / 2;
        int toastY = this.height - 45;

        // Simplified rendering: No translate, just straight draw calls
        context.fill(toastX, toastY, toastX + toastW, toastY + 16, (alphaInt << 24));
        int textColor = (alphaInt << 24) | 0x55FF55;
        context.drawCenteredString(CactusConstants.mc.font, "§aCopied to Clipboard!", this.width / 2, toastY + 4, textColor);
    }

    private class PlaceholderListWidget extends ContainerObjectSelectionList<PlaceholderEntry> {
        public PlaceholderListWidget(Minecraft client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
            refresh();
        }

        public void refresh() {
            this.clearEntries();
            for (PlaceholderInfo info : PlaceholderRegistryList.PLACEHOLDERS) {
                if (currentCategory.equals("All") || info.category().equals(currentCategory)) {
                    this.addEntry(new PlaceholderEntry(info));
                }
            }
        }
        @Override public int getRowWidth() { return this.width - 10; }
    }

    private class PlaceholderEntry extends ContainerObjectSelectionList.Entry<PlaceholderEntry> {
        private final PlaceholderInfo info;
        public PlaceholderEntry(PlaceholderInfo info) { this.info = info; }

        @Override
        public void renderContent(@NotNull GuiGraphics context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            // Using Screen math directly so it works perfectly even on the very first frame
            int boxWidth = (int)((PlaceholderBrowserScreen.this.width / 3.0) * 1.2);
            int x = (PlaceholderBrowserScreen.this.width - boxWidth) / 2;
            int y = this.getContentY();
            int w = boxWidth - 10;
            int colW = w / 3;

            if (hovered) context.fill(x, y, x + w, y + 28, 0x1AFFFFFF);

            drawPingPongText(context, "§b{" + info.key() + "}", x + 2, y + 8, colW - 10);
            drawPingPongText(context, "§a" + info.example(), x + colW + 2, y + 8, colW - 10);
            drawPingPongText(context, "§7" + info.description(), x + (colW * 2) + 2, y + 8, colW - 10);
        }

        private void drawPingPongText(GuiGraphics context, String text, int x, int y, int maxWidth) {
            int textWidth = CactusConstants.mc.font.width(text);
            if (textWidth <= maxWidth) {
                RenderUtils.drawText(context, text, x, y, -1);
                return;
            }

            float maxScroll = textWidth - maxWidth;
            double time = (System.currentTimeMillis() % 5000) / 5000.0;
            float offset = (float) ((Math.sin(time * Math.PI * 2 - Math.PI / 2) + 1) / 2.0f) * maxScroll;

            context.enableScissor(x, y, x + maxWidth, y + 20);
            RenderUtils.drawText(context, text, x - (int)offset, y, -1);
            context.disableScissor();
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
            if (event.button() == 0) {
                Minecraft.getInstance().keyboardHandler.setClipboard("{" + info.key() + "}");
                PlaceholderBrowserScreen.this.copyTimestamp = System.currentTimeMillis();
                return true;
            }
            return false;
        }
        @Override public @NotNull List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        @Override public @NotNull List<? extends GuiEventListener> children() { return Collections.emptyList(); }
    }
}