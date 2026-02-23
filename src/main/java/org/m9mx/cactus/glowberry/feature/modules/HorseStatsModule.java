package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;

public class HorseStatsModule extends Module {
    public static volatile HorseStatsModule INSTANCE;

    private final SettingGroup sgGeneral;
    public final Setting<Integer> displayDuration; // Duration in seconds to show the stats

    public HorseStatsModule(Category category) {
        super("horseStats", category, new Module.Options());
        if (INSTANCE == null) {
            synchronized(HorseStatsModule.class) {
                if (INSTANCE == null) {
                    INSTANCE = this;
                }
            }
        }

        this.sgGeneral = this.settings.buildGroup("general");
        this.displayDuration = this.sgGeneral.add(new IntegerSetting("displayDuration", 3));
    }

    @Override
    public void onEnable() {
        // Module is enabled, ready to show horse stats when mounting
    }

    @Override
    public void onDisable() {
        // Module is disabled
    }
}