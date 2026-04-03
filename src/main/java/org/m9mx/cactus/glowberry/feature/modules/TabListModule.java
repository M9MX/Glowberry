package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import org.m9mx.cactus.glowberry.util.compat.IncompatibilityRegistry;

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

        // Resolve compatibility through shared registry so future mod conflicts are data-only changes.
        boolean hideShowPingSetting = IncompatibilityRegistry.isSettingBlocked("tabList", "showPing");

        this.sgGeneral = this.settings.buildGroup("general");

        if (hideShowPingSetting) {
            // Setting is suppressed in UI; keep a local fallback instance to avoid null checks.
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