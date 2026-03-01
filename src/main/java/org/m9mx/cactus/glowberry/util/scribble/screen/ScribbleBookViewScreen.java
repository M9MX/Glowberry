/*
 * Adapted from Scribble mod by chrrs
 * Original mod: https://github.com/chrrs/scribble
 */

package org.m9mx.cactus.glowberry.util.scribble.screen;

import org.m9mx.cactus.glowberry.util.scribble.book.BookFile;
import org.m9mx.cactus.glowberry.util.scribble.book.FileChooser;
import org.m9mx.cactus.glowberry.util.scribble.book.RichText;
import org.m9mx.cactus.glowberry.util.scribble.gui.BookTextWidget;
import org.m9mx.cactus.glowberry.util.scribble.gui.TextArea;
import org.m9mx.cactus.glowberry.util.scribble.gui.button.IconButtonWidget;
import org.m9mx.cactus.glowberry.feature.modules.ScribbleModule;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Objects;

@NullMarked
public class ScribbleBookViewScreen extends ScribbleBookScreen<Component> {
    private static final Logger LOGGER = LogManager.getLogger();
    
    protected BookViewScreen.BookAccess book;

    public ScribbleBookViewScreen(BookViewScreen.BookAccess book) {
        super(Component.translatable("book.view.title"));
        this.book = book;
    }

    @Override
    protected boolean shouldShowActionButtons() {
        ScribbleModule module = ScribbleModule.INSTANCE;
        if (module == null || !module.active()) {
            return false;
        }
        return module.showActionButtons.get() == ScribbleModule.ShowActionButtons.ALWAYS;
    }

    @Override
    protected void initActionButtons(int x, int y) {
        addRenderableWidget(new IconButtonWidget(
                Component.translatable("text.scribble.action.save_book_to_file"),
                this::saveBookToFile,
                x, y, 48, 90, 12, 12));
    }

    @Override
    protected void initMenuControls(int y) {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> this.onClose())
                .pos((this.width - 200) / 2, y).width(200).build());
    }

    @Override
    protected TextArea<Component> createTextArea(int x, int y, int width, int height, int pageOffset) {
        return new BookTextWidget(x, y, width, height, this.font, this::handleClickEvent);
    }

    @Override
    protected Component getPage(int page) {
        return book.getPage(page);
    }

    @Override
    protected int getTotalPages() {
        // Always return at least 1 page, so we don't show an empty book (see #100).
        return Math.max(1, book.getPageCount());
    }

    private void saveBookToFile() {
        FileChooser.chooseFile(true, (path) -> {
            try {
                List<String> pages = this.book.pages().stream()
                        .map(RichText::fromFormattedTextLossy)
                        .map(RichText::getAsFormattedString)
                        .toList();

                BookFile bookFile = new BookFile("<written book>", pages);
                bookFile.writeJson(path);
            } catch (Exception e) {
                LOGGER.error("could not save book to file", e);
            }
        });
    }

    private void handleClickEvent(ClickEvent event) {
        switch (event) {
            case ClickEvent.ChangePage(int page) -> this.jumpToPage(page - 1);
            case ClickEvent.RunCommand(String command) -> {
                this.closeRemoteContainer();
                clickCommandAction(Objects.requireNonNull(this.minecraft.player), command, null);
            }
            default -> Screen.defaultHandleGameClickEvent(event, this.minecraft, this);
        }
    }
}
