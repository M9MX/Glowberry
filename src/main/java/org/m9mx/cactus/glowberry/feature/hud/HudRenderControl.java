package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import org.joml.Vector2i;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Render-time-only controls for Cactus HUD elements (see
 * {@code HudElementRenderMixin}).
 *
 * <p>This is the "modify the renderer" layer: any HUD element - Glowberry's own
 * or Cactus' built-ins - can be hidden or shifted at render time without ever
 * touching its stored model (position / size / anchor). Cactus itself always
 * sees the element exactly where the user placed it, so the HUD editor and the
 * saved config are never affected. Hiding and animation offsets are visual
 * only.</p>
 *
 * <p>Usage: {@code HudRenderControl.setHidden(element, true)} skips the element
 * in-game (the editor always shows it), and
 * {@code HudRenderControl.setOffset(element, x, y)} shifts the whole drawn box
 * by a render-time offset - e.g. an animation sliding it in from a screen
 * edge.</p>
 */
public final class HudRenderControl {
    private HudRenderControl() {}

    private static final Map<HudElement, Boolean> HIDDEN = new IdentityHashMap<>();
    private static final Map<HudElement, Vector2i> OFFSETS = new IdentityHashMap<>();

    /** Whether the element should be skipped in-game (never in the HUD editor). */
    public static boolean isHidden(HudElement element) {
        return HIDDEN.getOrDefault(element, Boolean.FALSE);
    }

    /** Hides (or unhides) the element in-game only. The editor always shows it. */
    public static void setHidden(HudElement element, boolean hidden) {
        if (hidden) {
            HIDDEN.put(element, Boolean.TRUE);
        } else {
            HIDDEN.remove(element);
        }
    }

    /** Current render-time offset for the element, or null when none is set. */
    public static Vector2i getOffset(HudElement element) {
        return OFFSETS.get(element);
    }

    /** Sets a render-time shift (pixels) for the element; (0, 0) clears it. */
    public static void setOffset(HudElement element, int x, int y) {
        if (x == 0 && y == 0) {
            OFFSETS.remove(element);
        } else {
            OFFSETS.put(element, new Vector2i(x, y));
        }
    }

    /** Drops all render control state for an element (e.g. it was removed). */
    public static void clear(HudElement element) {
        HIDDEN.remove(element);
        OFFSETS.remove(element);
    }
}
