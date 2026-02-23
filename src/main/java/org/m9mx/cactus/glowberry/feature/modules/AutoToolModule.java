package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;

public class AutoToolModule extends Module {
    public static volatile AutoToolModule INSTANCE;

    private final SettingGroup sgTools;
    public final Setting<Boolean> swords;
    public final Setting<Boolean> axes;
    public final Setting<Boolean> pickaxes;
    public final Setting<Boolean> shovels;
    public final Setting<Boolean> hoes;
    public final Setting<Boolean> changeForEntities;

    public AutoToolModule(Category category) {
        super("autoTool", category, new Module.Options());
        if (INSTANCE == null) {
            synchronized(AutoToolModule.class) {
                if (INSTANCE == null) {
                    INSTANCE = this;
                }
            }
        }

        this.sgTools = this.settings.buildGroup("tools");
        this.swords = this.sgTools.add(new BooleanSetting("swords", false));
        this.axes = this.sgTools.add(new BooleanSetting("axes", true));
        this.pickaxes = this.sgTools.add(new BooleanSetting("pickaxes", true));
        this.shovels = this.sgTools.add(new BooleanSetting("shovels", true));
        this.hoes = this.sgTools.add(new BooleanSetting("hoes", true));
        this.changeForEntities = this.sgTools.add(new BooleanSetting("changeForEntities", false));
    }

    @Override
    public void onEnable() {
        // Module is enabled, autotool functionality is active
    }

    @Override
    public void onDisable() {
        // Module is disabled, autotool functionality is inactive
    }
}