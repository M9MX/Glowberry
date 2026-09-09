package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.StringSetting;
public class BrandNameChanger extends Module {
    public static volatile BrandNameChanger INSTANCE;

    private final SettingGroup options;

    public final Setting<String> brandName;

    public BrandNameChanger(Category category) {
        super("brand_name", category, new com.dwarslooper.cactus.client.feature.module.Module.Options());
        if (INSTANCE == null) {
            synchronized(BrandNameChanger.class) {
                if (INSTANCE == null) {
                    INSTANCE = this;
                }
            }
        }

        this.options = this.settings.buildGroup("options");
        this.brandName = this.options.add(new StringSetting("brand", "Cactus Mod"));
    }
    public String getCustomBrandOrNull() {
        if (this.active()) {
            String value = this.brandName.get();
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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
