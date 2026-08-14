package org.m9mx.cactus.glowberry.util;

import com.dwarslooper.cactus.client.gui.hud.element.HudElement;

/**
 * Tracks which {@link HudElement} is currently rendering in a thread-local, so
 * {@code MixinTextRendererRainbow} knows which element's TextColor/Text Chroma
 * settings apply to the text being drawn. Kept as a plain (non-mixin) class
 * because Mixin does not allow non-private static methods inside a mixin class.
 */
public final class CurrentHudElement {

    private static final ThreadLocal<HudElement<?>> CURRENT = new ThreadLocal<>();

    private CurrentHudElement() {}

    /** The element currently rendering, or null outside of a Cactus HUD element. */
    public static HudElement<?> current() {
        return CURRENT.get();
    }

    /** Called at the start of a HUD element render. */
    public static void push(HudElement<?> element) {
        CURRENT.set(element);
    }

    /** Called when the render finishes. */
    public static void pop() {
        CURRENT.remove();
    }
}
