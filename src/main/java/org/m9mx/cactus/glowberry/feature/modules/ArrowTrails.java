/*
 * Adapted from Moss Addon by Datenflieger
 * Original mod: https://github.com/Datenflieger/MossAddon
 */
package org.m9mx.cactus.glowberry.feature.modules;

import java.awt.Color;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.ColorSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;

public class ArrowTrails extends Module {
    public static volatile ArrowTrails INSTANCE;

    private final SettingGroup sgVisual;

    public final Setting<Boolean> rainbow;
    public final Setting<Integer> rgbSpeed;
    public final Setting<Integer> particleDensity;
    public final Setting<Integer> particleSize10;
    public final Setting<ColorSetting.ColorValue> color;
    public final Setting<Boolean> ownOnly;
    public final Setting<Integer> offsetSpread1000;
    public final Setting<Integer> minSpeed100;
    public final Setting<Boolean> vanillaTrail;

    public ArrowTrails(Category category) {
        super("arrow_trails", category, new Module.Options().set(Flag.HUD_LISTED, false));
        if (INSTANCE == null) {
            synchronized (ArrowTrails.class) {
                if (INSTANCE == null) {
                    INSTANCE = this;
                }
            }
        }

        this.sgVisual = this.settings.buildGroup("visual");

        this.rainbow = this.sgVisual.add(new BooleanSetting("rainbow", true));
        this.rgbSpeed = this.sgVisual.add(new IntegerSetting("rgbSpeed", 100).min(1).max(2000))
                .visibleIf(() -> (Boolean) this.rainbow.get());
        this.particleDensity = this.sgVisual.add(new IntegerSetting("particleDensity", 6).min(1).max(20));
        this.particleSize10 = this.sgVisual.add(new IntegerSetting("particleSize10", 10).min(1).max(50));
        this.color = this.sgVisual.add(new ColorSetting("color", new ColorSetting.ColorValue(new Color(0, 255, 127), false)))
                .visibleIf(() -> !(Boolean) this.rainbow.get());
        this.ownOnly = this.sgVisual.add(new BooleanSetting("ownOnly", true));
        this.offsetSpread1000 = this.sgVisual.add(new IntegerSetting("offsetSpread1000", 100).min(0).max(500));
        this.minSpeed100 = this.sgVisual.add(new IntegerSetting("minSpeed100", 0).min(0).max(500));
        this.vanillaTrail = this.sgVisual.add(new BooleanSetting("vanillaTrail", false));
    }
}
