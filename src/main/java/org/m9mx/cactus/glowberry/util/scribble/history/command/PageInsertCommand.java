/*
 * Adapted from Scribble mod by chrrs
 * Original mod: https://github.com/chrrs/scribble
 */

package org.m9mx.cactus.glowberry.util.scribble.history.command;

import org.m9mx.cactus.glowberry.util.scribble.history.HistoryListener;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PageInsertCommand implements Command {
    private final int page;

    public PageInsertCommand(int page) {
        this.page = page;
    }

    @Override
    public void execute(HistoryListener listener) {
        listener.insertPageAt(page, null);
    }

    @Override
    public void rollback(HistoryListener listener) {
        listener.deletePage(page);
    }
}
