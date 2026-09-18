/*
 * Adapted from Moss Addon by Datenflieger
 * Original mod: https://github.com/Datenflieger/MossAddon
 */
package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.ColorSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;

import java.awt.Color;

/**
 * Shows floating damage numbers near entities that just lost health.
 */
public class DamageIndicator extends Module {
    public static volatile DamageIndicator INSTANCE;

    public enum DamageFormat {
        HP,
        Hearts
    }

    private final SettingGroup sgGeneral;

    public final Setting<DamageFormat> format;
    public final Setting<Integer> scale;
    public final Setting<Integer> duration;
    public final Setting<Boolean> showOnPlayers;
    public final Setting<Integer> spawnHeightPercent;
    public final Setting<ColorSetting.ColorValue> popupColor;
    public final Setting<Boolean> textShadow;
    public final Setting<Boolean> backplate;
    public final Setting<Integer> backplateOpacityPercent;
    public final Setting<Boolean> overrideHurtFlash;
    public final Setting<ColorSetting.ColorValue> hurtFlashColor;

    public DamageIndicator(Category category) {
        super("damage_indicator", category, new Module.Options()
                .set(Flag.HUD_LISTED, false)
                .set(Flag.SERVER_UNSAFE, true));
        if (INSTANCE == null) {
            synchronized (DamageIndicator.class) {
                if (INSTANCE == null) {
                    INSTANCE = this;
                }
            }
        }

        this.sgGeneral = this.settings.buildGroup("general");
        this.format = this.sgGeneral.add(new EnumSetting<>("format", DamageFormat.HP));
        this.scale = this.sgGeneral.add(new IntegerSetting("scale", 100).min(50).max(200));
        this.duration = this.sgGeneral.add(new IntegerSetting("duration", 12).min(3).max(60));
        this.showOnPlayers = this.sgGeneral.add(new BooleanSetting("showOnPlayers", true));
        this.spawnHeightPercent = this.sgGeneral.add(new IntegerSetting("spawnHeightPercent", 75).min(0).max(150));
        this.popupColor = this.sgGeneral.add(new ColorSetting("popupColor", new ColorSetting.ColorValue(new Color(255, 85, 85), false)));

        // Readability: drop shadow behind the glyphs and/or a translucent plate
        // behind the whole line, so numbers stay visible on red mobs (blaze,
        // rosy mooshroom, ...) and red flash frames.
        this.textShadow = this.sgGeneral.add(new BooleanSetting("textShadow", true));
        this.backplate = this.sgGeneral.add(new BooleanSetting("backplate", false));
        this.backplateOpacityPercent = this.sgGeneral.add(new IntegerSetting("backplateOpacityPercent", 60).min(10).max(100))
                .visibleIf(this.backplate::get);

        // Replaces the vanilla red hurt flash on damaged entities with a custom
        // color (rainbow works: enable RGB mode on the color picker).
        this.overrideHurtFlash = this.sgGeneral.add(new BooleanSetting("overrideHurtFlash", false));
        this.hurtFlashColor = this.sgGeneral.add(new ColorSetting("hurtFlashColor", new ColorSetting.ColorValue(Color.WHITE, false)))
                .visibleIf(this.overrideHurtFlash::get);
    }
}
