package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;

public class TabListModule extends Module {
    public static volatile TabListModule INSTANCE;

    private final SettingGroup sgGeneral;
    public final Setting<Boolean> showPing;

    public TabListModule(Category category) {
        super("tabList", category, new Module.Options());
        if (INSTANCE == null) {
            synchronized(TabListModule.class) {
                if (INSTANCE == null) {
                    INSTANCE = this;
                }
            }
        }

        this.sgGeneral = this.settings.buildGroup("general");
        this.showPing = this.sgGeneral.add(new BooleanSetting("showPing", true));
    }

    @Override
    public void onEnable() {
        // Module is enabled, ping display is active in tablist
    }

    @Override
    public void onDisable() {
        // Module is disabled
    }
}
