package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;

public class NoHurtcamModule extends Module {
    public static volatile NoHurtcamModule INSTANCE;

    public NoHurtcamModule(Category category) {
        super("noHurtcam", category, new Module.Options());
        if (INSTANCE == null) {
            synchronized(NoHurtcamModule.class) {
                if (INSTANCE == null) {
                    INSTANCE = this;
                }
            }
        }
    }

    @Override
    public void onEnable() {
        // Module is enabled, hurtcam modifications are active
    }

    @Override
    public void onDisable() {
        // Module is disabled, hurtcam modifications are inactive
    }
}