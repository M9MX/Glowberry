package org.m9mx.cactus.glowberry.util;

import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import com.dwarslooper.cactus.client.systems.config.settings.impl.ColorSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;

import java.awt.Color;

/**
 * Draws HUD text with Glowberry's rainbow handling applied. This is the single
 * place that decides how the element's text rainbow animates:
 *
 * <ul>
 *   <li><b>SMOOTH</b> - Cactus' original behavior: the whole text shifts color
 *       together (the slow fade from {@code ColorValue#color()}).</li>
 *   <li><b>DIAGONAL</b> - every character is drawn with its own color from the
 *       same moving diagonal gradient Cactus uses in
 *       {@code HudElement#getTextColorAt}, so a line of color sweeps across the
 *       text character by character instead of all characters sharing one color.</li>
 * </ul>
 *
 * <p>Unlike the old implementation (a global {@code GuiGraphicsExtractor.text}
 * mixin guarded by a thread-local "current element"), this helper is invoked
 * with the element directly in scope - by a mixin on
 * {@code HudElement#drawHudText} for Cactus' own elements, and directly by
 * Glowberry's elements - so there is no shared mutable state that can silently
 * stop working.
 */
public final class RainbowRenderer {

    private RainbowRenderer() {}

    /**
     * Draws {@code text} at ({@code x}, {@code y}) using Glowberry's own
     * elements (which draw text directly with {@code context.text}). When the
     * DIAGONAL mode is selected and the element's rainbow is active, the text
     * is drawn character by character with the moving diagonal gradient;
     * otherwise it is drawn in one call with the given color.
     *
     * @return the x position just past the drawn text (like Cactus' drawHudText).
     */
    public static int draw(GuiGraphicsExtractor context, Font font, String text, int x, int y, int color, boolean shadow, HudElement<?> element) {
        if (text == null || text.isEmpty()) return x;

        if (RainbowModeHandler.mode() == RainbowMode.DIAGONAL && rainbowActive(element)) {
            return drawDiagonal(context, font, text, x, y, element, shadow, color);
        }

        context.text(font, text, x, y, color, shadow);
        return x + font.width(text);
    }

    /**
     * The element's rainbow is active when either the "Text Chroma" toggle is on
     * or the TextColor setting's RGB ({@code usesRgb}) is enabled - the same two
     * conditions Cactus checks in {@code getTextColorAt}/{@code ColorValue#color}.
     */
    public static boolean rainbowActive(HudElement<?> element) {
        if (element == null) return false;
        try {
            Setting<Boolean> chroma = element.textChroma;
            if (chroma != null && Boolean.TRUE.equals(chroma.get())) return true;

            Setting<ColorSetting.ColorValue> textColor = element.textColor;
            if (textColor != null) {
                ColorSetting.ColorValue value = textColor.get();
                if (value != null && value.usesRgb()) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Re-draws {@code text} character by character with Cactus' moving diagonal
     * gradient. The alpha comes from the element's TextColor setting.
     */
    public static int drawDiagonal(GuiGraphicsExtractor context, Font font, String text, int x, int y, HudElement<?> element, boolean shadow) {
        return drawDiagonal(context, font, text, x, y, element, shadow, textColorOf(element));
    }

    /** Re-draws character by character; {@code fallbackColor} supplies the alpha. */
    private static int drawDiagonal(GuiGraphicsExtractor context, Font font, String text, int x, int y, HudElement<?> element, boolean shadow, int fallbackColor) {
        int alpha = ARGB.alpha(fallbackColor);
        float timeSec = (System.currentTimeMillis() % 100_000L) / 1000f;
        float drift = 0.06f + chromaSpeed(element) * 0.03f;
        float charStep = 0.0035f;

        int cursor = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                i++; // formatting code - renders as nothing
                continue;
            }
            String ch = String.valueOf(c);
            int w = font.width(ch);
            if (c == ' ' || w <= 0) {
                cursor += w;
                continue;
            }

            float hue = (timeSec * drift + (cursor + y * 2) * charStep) % 1f;
            if (hue < 0) hue += 1f;
            int rgb = Color.HSBtoRGB(hue, 0.85f, 1f) & 0xFFFFFF;

            context.text(font, ch, cursor, y, ARGB.color(alpha, ARGB.red(rgb), ARGB.green(rgb), ARGB.blue(rgb)), shadow);
            cursor += w;
        }
        return x + font.width(text);
    }

    private static int textColorOf(HudElement<?> element) {
        try {
            Setting<ColorSetting.ColorValue> textColor = element.textColor;
            if (textColor != null) {
                ColorSetting.ColorValue value = textColor.get();
                if (value != null) return value.color();
            }
        } catch (Exception ignored) {
        }
        return 0xFFFFFFFF;
    }

    private static int chromaSpeed(HudElement<?> element) {
        try {
            Setting<Integer> setting = element.textChromaSpeed;
            if (setting == null) return 8;
            Integer value = setting.get();
            return value == null ? 8 : Math.max(1, Math.min(20, value));
        } catch (Exception e) {
            return 8;
        }
    }
}
