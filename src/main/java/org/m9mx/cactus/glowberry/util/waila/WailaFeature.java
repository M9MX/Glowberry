package org.m9mx.cactus.glowberry.util.waila;

/**
 * The grouped Waila settings shown in the HUD "Show" picker.
 */
public enum WailaFeature {
    HEALTH(true),
    EXTRA_INFO(true),
    POTION_EFFECTS(true),
    EQUIPMENT(false),
    BREAK_PROGRESS(true),
    REQUIRED_TOOL(true),
    COORDINATES(true),
    BLOCK_EXTRA_INFO(true),
    BLOCK_PROPERTIES(false),
    STORAGE_INFO(false),
    MOD_NAME(true);

    public final boolean defaultEnabled;

    WailaFeature(boolean defaultEnabled) {
        this.defaultEnabled = defaultEnabled;
    }
}
