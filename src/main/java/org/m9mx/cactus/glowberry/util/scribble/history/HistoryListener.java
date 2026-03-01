/*
 * Adapted from Scribble mod by chrrs
 * Original mod: https://github.com/chrrs/scribble
 */

package org.m9mx.cactus.glowberry.util.scribble.history;

import org.m9mx.cactus.glowberry.util.scribble.book.RichText;
import org.m9mx.cactus.glowberry.util.scribble.gui.edit.RichMultiLineTextField;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Set;

@NullMarked
public interface HistoryListener {
    RichMultiLineTextField switchAndFocusPage(int page);

    void setFormat(@Nullable ChatFormatting color, Set<ChatFormatting> modifiers);

    void insertPageAt(int page, @Nullable RichText content);

    void deletePage(int page);
}
