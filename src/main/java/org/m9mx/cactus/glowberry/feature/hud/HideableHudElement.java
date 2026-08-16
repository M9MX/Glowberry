package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.Anchor;
import com.dwarslooper.cactus.client.gui.hud.element.DynamicHudElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Vector2i;

/**
 * Base class for Glowberry HUD elements.
 *
 * <p>Everything visual goes through Glowberry's renderer layer - the
 * {@code HudElementRenderMixin} on Cactus' {@code HudElement#render} plus
 * {@link HudRenderControl}: the base registers the element's hide state there
 * every frame, and the mixin skips the render in-game while the HUD editor
 * always shows the element. The stored model (position / size / anchor) is
 * never changed to hide, so Cactus always sees the element exactly where the
 * user placed it and the saved config stays valid.</p>
 *
 * <p>Also provides consistent resize anchoring: when a resizing element grows or
 * shrinks, the stored position is adjusted by the same delta the Cactus anchor
 * points at (e.g. a RIGHT-anchored box grows left/up from its right edge), so
 * the stored position and size always describe the same anchored box. The pair
 * is what Cactus saves, so a reload renders the box exactly where it was - the
 * anchor never drifts, and nothing is lost between sessions.</p>
 */
public abstract class HideableHudElement<T extends HideableHudElement<T>> extends DynamicHudElement<T> {

    protected HideableHudElement(String id, Vector2i defaultSize) {
        super(id, defaultSize);
    }

    /** Whether the element has nothing to show right now (in-game). */
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
     * Resizes the element to its content size and keeps the box anchored to the
     * side(s) its Cactus anchor points at - e.g. a RIGHT_LOW element grows
     * left/up from its bottom-right corner instead of extending off-screen from
     * the top-left. The stored position is adjusted together with the size, so
     * the saved (position, size) pair always describes the same anchored box:
     * the element reloads exactly where it was, and the anchored edge never
     * drifts while content grows or shrinks.
     */
    protected void anchoredResize(int newWidth, int newHeight) {
        int dx = newWidth - this.getSize().x();
        int dy = newHeight - this.getSize().y();
        if (dx != 0 || dy != 0) {
            Anchor anchor = this.getAnchor();
            double hFactor = anchor.getHorizontalFactor();
            double vFactor = anchor.getVerticalFactor();
            if (hFactor != 0 || vFactor != 0) {
                Vector2i position = this.getRelativePosition();
                this.move(position.x() - (int) Math.round(dx * hFactor),
                        position.y() - (int) Math.round(dy * vFactor));
            }
            this.resize(newWidth, newHeight);
        }
    }

    /**
     * Fake-moves the element at render time: the draw coordinates are shifted
     * by the given amount while the stored position stays untouched (see
     * {@link HudRenderControl}). Pass (0, 0) to clear. The HUD editor ignores
     * the offset, so the element always shows at its true model position there.
     */
    protected void setRenderOffset(int x, int y) {
        HudRenderControl.setOffset(this, x, y);
    }

    /**
     * Self-heals legacy configs that were saved with an inconsistent
     * (position, size) pair - the position was placed for a smaller box, then
     * the size was grown without adjusting the position, so the box ends up
     * half off-screen on world load. As soon as the element renders in-game the
     * box is clamped back into the screen; the corrected position is part of the
     * model, so it persists and the box stays where the user sees it.
     */
    private void correctOutOfBounds(int screenWidth, int screenHeight) {
        Vector2i absolute = this.getAbsolutePosition(screenWidth, screenHeight);
        Vector2i size = this.getSize();
        int maxX = screenWidth - size.x();
        int maxY = screenHeight - size.y();
        if (absolute.x() >= 0 && absolute.y() >= 0 && absolute.x() <= maxX && absolute.y() <= maxY) {
            return;
        }
        int newX = Math.max(0, Math.min(absolute.x(), maxX));
        int newY = Math.max(0, Math.min(absolute.y(), maxY));
        if (newX != absolute.x() || newY != absolute.y()) {
            this.fromAbsolute(new Vector2i(newX, newY), screenWidth, screenHeight);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int x, int y, int width, int height, float delta, boolean inEditor) {
        recoverOffscreenPosition();
        if (!inEditor) {
            // Fix legacy off-screen configs the moment the element renders in-game
            // (equivalent to what the HUD editor does when it opens).
            correctOutOfBounds(width, height);
        }
        // Route hiding through the renderer control: the mixin skips the element
        // in-game; the HUD editor always shows it. The stored model is untouched.
        HudRenderControl.setHidden(this, !inEditor && shouldHide());
        super.render(context, x, y, width, height, delta, inEditor);
    }

    @Override
    public void removed() {
        // Element removed from the HUD - drop any render control state for it.
        HudRenderControl.clear(this);
        super.removed();
    }
}
