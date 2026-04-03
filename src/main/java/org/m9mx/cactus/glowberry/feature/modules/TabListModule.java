package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.fabricmc.loader.api.FabricLoader; // <-- new import

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

        // Detect external ping mod. Accept either common mod id variants.
        boolean externalPingMod = FabricLoader.getInstance().isModLoaded("better-ping-display")
                || FabricLoader.getInstance().isModLoaded("betterpingdisplay");

        this.sgGeneral = this.settings.buildGroup("general");

        if (externalPingMod) {
            // If an external ping display mod is present, do not add this setting to the UI.
            // Create a local BooleanSetting with default false to avoid NPEs for callers.
            this.showPing = new BooleanSetting("showPing", false);
        } else {
            // Normal behavior: add setting to UI (default true).
            this.showPing = this.sgGeneral.add(new BooleanSetting("showPing", true));
        }
    }

    @Override
    public void onEnable() {
        // Module is enabled, ping display is active in tablist (if showPing is true)
    }

    @Override
    public void onDisable() {
        // Module is disabled
    }
}