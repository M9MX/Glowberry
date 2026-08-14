package org.m9mx.cactus.glowberry.util;

import com.dwarslooper.cactus.client.feature.module.ModuleManager;
import com.dwarslooper.cactus.client.systems.config.ConfigHandler;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;

/**
 * Saves all of Cactus' configs (modules, HUD, macros, settings, ...) exactly like
 * Cactus does when the client closes - it runs the very same {@code ConfigHandler#save()}
 * - but on every screen change or close instead of only on shutdown.
 *
 * The "Auto Save on Update" toggle is a single setting that Glowberry adds to
 * Cactus' own settings container through the addon API (see
 * {@code GlowberryCactus#registerGlowberryConfig}); this handler holds a reference
 * to it and performs the saves. It is triggered from {@code MixinGuiSetScreen} on
 * every {@code Gui#setScreen} call (opening or closing any screen, vanilla or Cactus).
 */
public final class AutoSaveHandler {
    private static Setting<Boolean> autoSaveOnUpdate;

    // True while an auto-save (triggered by a screen change) is running, so the
    // "Cactus config saved!" log Cactus prints in ConfigHandler#save is suppressed
    // for our saves but still shown when Cactus saves on close.
    private static boolean suppressSaveLog = false;

    private AutoSaveHandler() {}

    /** Called by the config registration with the injected setting. */
    public static void setAutoSaveSetting(Setting<Boolean> setting) {
        autoSaveOnUpdate = setting;
    }

    private static boolean isAutoSaveEnabled() {
        return autoSaveOnUpdate != null && autoSaveOnUpdate.get();
    }

    /** Whether the current save was triggered by us (screen change) and should not log. */
    public static boolean isSuppressingLog() {
        return suppressSaveLog;
    }

    /** Called whenever the screen changes or is closed. */
    public static void onScreenChanged() {
        if (!isAutoSaveEnabled()) return;

        ConfigHandler handler = getHandler();
        if (handler == null || !handler.isDoneLoading()) return;

        // The exact same save Cactus runs when the client closes
        suppressSaveLog = true;
        try {
            handler.save();
        } catch (Exception ignored) {
            // A failed save must never break a screen change
        } finally {
            suppressSaveLog = false;
        }
    }

    private static ConfigHandler getHandler() {
        try {
            ModuleManager manager = ModuleManager.get();
            return manager == null ? null : manager.getHandler();
        } catch (Exception e) {
            return null;
        }
    }
}
