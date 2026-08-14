package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import com.dwarslooper.cactus.client.systems.config.settings.impl.ColorSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import org.m9mx.cactus.glowberry.util.CurrentHudElement;
import org.m9mx.cactus.glowberry.util.RainbowMode;
import org.m9mx.cactus.glowberry.util.RainbowModeHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

/**
 * Overrides the text renderer Cactus HUD elements draw through
 * ({@code GuiGraphicsExtractor.text}) so the element's text rainbow can animate
 * in two modes, selected by the {@code rainbowMode} setting Glowberry adds to
 * Cactus settings (see {@code RainbowModeHandler}):
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
 * The rainbow is only active when the element's TextColor {@code ColorSetting}
 * has RGB ({@code usesRgb}) enabled or its "Text Chroma" toggle is on - exactly
 * the conditions Cactus itself checks. Because this hooks the renderer itself,
 * every element gets the effect automatically (Cactus' own elements and
 * Glowberry's), with no element code changes, and text drawn outside of a Cactus
 * HUD element render (screens, tooltips, chat, ...) is never touched.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class MixinTextRendererRainbow {

    /** True while this mixin is drawing the per-character rainbow (re-entrancy guard). */
    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);

    @Inject(method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void glowberry_diagonalRainbow(Font font, String text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        if (text == null || text.isEmpty()) return;
        if (APPLYING.get()) return; // per-character draws below must not re-enter

        HudElement<?> element = CurrentHudElement.current();
        if (element == null) return;
        if (!rainbowActive(element)) return;
        if (RainbowModeHandler.mode() != RainbowMode.DIAGONAL) return;

        try {
            APPLYING.set(true);
            GuiGraphicsExtractor context = (GuiGraphicsExtractor) (Object) this;
            int alpha = ARGB.alpha(color);
            int speed = chromaSpeed(element);

            // Same moving diagonal gradient Cactus' getTextColorAt computes:
            // hue = (time * drift + (x + y*2) * charStep) % 1
            float timeSec = (System.currentTimeMillis() % 100_000L) / 1000f;
            float drift = 0.06f + speed * 0.03f;
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
                if (c == ' ') {
                    cursor += w;
                    continue;
                }
                if (w <= 0) continue;

                float hue = (timeSec * drift + (cursor + y * 2) * charStep) % 1f;
                if (hue < 0) hue += 1f;
                int rgb = Color.HSBtoRGB(hue, 0.85f, 1f) & 0xFFFFFF;

                context.text(font, ch, cursor, y, ARGB.color(alpha, ARGB.red(rgb), ARGB.green(rgb), ARGB.blue(rgb)), shadow);
                cursor += w;
            }
            ci.cancel();
        } catch (Exception ignored) {
            // Any failure falls back to the normal render
        } finally {
            APPLYING.set(false);
        }
    }

    /**
     * The element's rainbow is active when either the "Text Chroma" toggle is on
     * or the TextColor setting's RGB ({@code usesRgb}) is enabled - the same two
     * conditions Cactus checks in {@code getTextColorAt}/{@code ColorValue#color}.
     */
    private static boolean rainbowActive(HudElement<?> element) {
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
