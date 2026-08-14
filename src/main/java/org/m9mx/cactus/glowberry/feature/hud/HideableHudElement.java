package org.m9mx.cactus.glowberry.feature.hud;

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
 */
public abstract class HideableHudElement<T extends HideableHudElement<T>> extends DynamicHudElement<T> {

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

    @Override
    public void render(GuiGraphicsExtractor context, int x, int y, int width, int height, float delta, boolean inEditor) {
        recoverOffscreenPosition();
        // Never hide in the HUD editor; otherwise render nothing at all when there
        // is nothing to show. Position is untouched, so it saves correctly.
        if (!inEditor && shouldHide()) {
            return;
        }
        super.render(context, x, y, width, height, delta, inEditor);
    }
}
