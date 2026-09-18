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

public class HealthIndicators extends Module {
    public static volatile HealthIndicators INSTANCE;

    public enum DisplayType {
        Hearts,
        Amount,
        Percent
    }

    private final SettingGroup sgGeneral;

    public final Setting<Boolean> includeAbsorption;
    public final Setting<Boolean> includePlayers;
    public final Setting<Boolean> includeOtherEntities;
    public final Setting<DisplayType> displayType;
    public final Setting<Integer> heartsYOffsetPx;
    public final Setting<Boolean> compactRows;
    public final Setting<ColorSetting.ColorValue> compactTextColor;

    public HealthIndicators(Category category) {
        super("health_indicators", category, new Module.Options()
                .set(Flag.HUD_LISTED, false)
                .set(Flag.SERVER_UNSAFE, true));
        if (INSTANCE == null) {
            synchronized (HealthIndicators.class) {
                if (INSTANCE == null) {
                    INSTANCE = this;
                }
            }
        }

        this.sgGeneral = this.settings.buildGroup("general");
        this.includeAbsorption = this.sgGeneral.add(new BooleanSetting("includeAbsorption", true));
        this.includePlayers = this.sgGeneral.add(new BooleanSetting("includePlayers", true));
        this.includeOtherEntities = this.sgGeneral.add(new BooleanSetting("includeOtherEntities", true));
        this.displayType = this.sgGeneral.add(new EnumSetting<>("displayType", DisplayType.Hearts));
        this.heartsYOffsetPx = this.sgGeneral.add(new IntegerSetting("heartsYOffsetPx", 0).min(-40).max(80))
                .visibleIf(() -> this.displayType.get() == DisplayType.Hearts);
        this.compactRows = this.sgGeneral.add(new BooleanSetting("compactRows", false))
                .visibleIf(() -> this.displayType.get() == DisplayType.Hearts);
        this.compactTextColor = this.sgGeneral.add(new ColorSetting("compactTextColor", new ColorSetting.ColorValue(Color.WHITE, false)))
                .visibleIf(() -> this.displayType.get() == DisplayType.Hearts && this.compactRows.get());
    }
}
