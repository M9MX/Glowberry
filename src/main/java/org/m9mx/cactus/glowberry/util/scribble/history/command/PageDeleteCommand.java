/*
 * Adapted from Scribble mod by chrrs
 * Original mod: https://github.com/chrrs/scribble
 */

package org.m9mx.cactus.glowberry.util.scribble.history.command;

import org.m9mx.cactus.glowberry.util.scribble.book.RichText;
import org.m9mx.cactus.glowberry.util.scribble.history.HistoryListener;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PageDeleteCommand implements Command {
    private final int page;
    private final RichText content;

    public PageDeleteCommand(int page, RichText content) {
        this.page = page;
        this.content = content;
    }

    @Override
    public void execute(HistoryListener listener) {
        listener.deletePage(page);
    }

    @Override
    public void rollback(HistoryListener listener) {
        listener.insertPageAt(page, content);
    }
}
