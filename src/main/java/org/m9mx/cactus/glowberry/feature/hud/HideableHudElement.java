package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.Anchor;
import com.dwarslooper.cactus.client.gui.hud.element.DynamicHudElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Vector2i;

/**
 * Base class for Glowberry HUD elements that can hide themselves.
 *
 * Hiding is done by skipping the whole render pass - including Cactus' style
 * background box - instead of moving the element far off the screen. Because the
 * element's position (and size) are never changed to hide it, the position Cactus
 * saves is always the real one, so after a restart the element reappears exactly
 * where the user placed it. The element is always rendered in the HUD editor.
 *
 * <p>Also provides visual-only resize anchoring: when a resizing element grows
 * or shrinks, the correction is applied as a render-time offset instead of
 * calling {@code move()}, so resizing can never drift the stored (saved)
 * position - the element always reloads where the user placed it.</p>
 */
public abstract class HideableHudElement<T extends HideableHudElement<T>> extends DynamicHudElement<T> {

    // Visual-only horizontal shift that keeps the box anchored when it resizes,
    // following the Cactus anchor's horizontal factor (LEFT: 0, MIDDLE: 0.5,
    // RIGHT: 1). Never written to the stored position, so it is never saved -
    // the element always reloads where the user placed it.
    protected int anchorOffsetX = 0;

    // Visual-only vertical shift for the same anchoring, following the Cactus
    // anchor's vertical factor (UP: 0, MIDDLE: 0.5, LOW: 1). Same rules as
    // anchorOffsetX: render-time only, never saved.
    protected int anchorOffsetY = 0;

    // Resize baselines: the box size the current anchor offset was computed
    // against. Initialised lazily from the stored size, so the very first
    // content render corrects relative to the box as placed/saved.
    protected int lastWidth  = -1;
    protected int lastHeight = -1;

    protected HideableHudElement(String id, Vector2i defaultSize) {
        super(id, defaultSize);
    }

    /** Whether the element has nothing to show right now. */
    protected abstract boolean shouldHide();

    /**
     * Recovers configs saved by old versions that hid elements by moving them far
     * off-screen (that off-screen position got persisted). Runs every frame so a
     * broken config self-heals as soon as the element is rendered.
     */
    protected void recoverOffscreenPosition() {
        if (this.getRelativePosition().x() <= -90000 || this.getRelativePosition().y() <= -90000) {
            this.move(10, 10);
        }
    }

    /**
     * Resizes the element and keeps the box visually anchored to the side(s) its
     * Cactus anchor points at - e.g. a RIGHT_LOW element grows left/up from its
     * bottom-right corner instead of extending off-screen from the top-left. The
     * correction is a render-time offset: the stored position never changes, so
     * resizing can never drift the saved position.
     */
    protected void anchoredResize(int newWidth, int newHeight) {
        int oldWidth  = lastWidth  == -1 ? this.getSize().x() : lastWidth;
        int oldHeight = lastHeight == -1 ? this.getSize().y() : lastHeight;
        lastWidth  = newWidth;
        lastHeight = newHeight;
        this.resize(newWidth, newHeight);
        int dx = newWidth - oldWidth;
        int dy = newHeight - oldHeight;
        if (dx != 0 || dy != 0) {
            Anchor anchor = this.getAnchor();
            anchorOffsetX -= Math.round(dx * anchor.getHorizontalFactor());
            anchorOffsetY -= Math.round(dy * anchor.getVerticalFactor());
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int x, int y, int width, int height, float delta, boolean inEditor) {
        recoverOffscreenPosition();
        // Never hide in the HUD editor; otherwise render nothing at all when there
        // is nothing to show. Position is untouched, so it saves correctly.
        if (!inEditor && shouldHide()) {
            return;
        }

        if (inEditor) {
            // In the HUD editor the box shows exactly at its stored position (no
            // resize anchoring) so dragging saves what you see. Baselines reset
            // every editor frame so in-editor resizes never accumulate offsets.
            anchorOffsetX = 0;
            anchorOffsetY = 0;
            lastWidth = -1;
            lastHeight = -1;
        }

        super.render(context, x + (inEditor ? 0 : anchorOffsetX), y + (inEditor ? 0 : anchorOffsetY), width, height, delta, inEditor);
    }
}
