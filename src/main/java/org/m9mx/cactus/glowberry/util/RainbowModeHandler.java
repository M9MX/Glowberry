package org.m9mx.cactus.glowberry.util;

import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;

/**
 * Holds the {@code rainbowMode} setting that Glowberry injects into Cactus' own
 * settings container (see {@code GlowberryCactus#registerGlowberryConfig}), so
 * the text-renderer mixin can read the selected animation mode without needing
 * a field that does not exist on {@code CactusSettings}.
 *
 * <p>The rainbow itself lives in the element's TextColor {@code ColorSetting}
 * ({@code usesRgb} → {@code ColorValue#color()}) and in {@code textChroma} - this
 * holder only carries the mode that decides HOW it animates.</p>
 */
public final class RainbowModeHandler {

    private static Setting<RainbowMode> rainbowMode;

    private RainbowModeHandler() {}

    /** Called by the config registration with the injected setting. */
    public static void setSetting(Setting<RainbowMode> setting) {
        rainbowMode = setting;
    }

    /** The selected animation mode; SMOOTH (Cactus' original) if unavailable. */
    public static RainbowMode mode() {
        Setting<RainbowMode> setting = rainbowMode;
        if (setting == null) return RainbowMode.SMOOTH;
        try {
            RainbowMode value = setting.get();
            return value == null ? RainbowMode.SMOOTH : value;
        } catch (Exception e) {
            return RainbowMode.SMOOTH;
        }
    }
}
