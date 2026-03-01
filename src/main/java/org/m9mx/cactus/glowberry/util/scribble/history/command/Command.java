/*
 * Adapted from Scribble mod by chrrs
 * Original mod: https://github.com/chrrs/scribble
 */

package org.m9mx.cactus.glowberry.util.scribble.history.command;

import org.m9mx.cactus.glowberry.util.scribble.history.HistoryListener;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface Command {
    void execute(HistoryListener listener);

    void rollback(HistoryListener listener);
}
