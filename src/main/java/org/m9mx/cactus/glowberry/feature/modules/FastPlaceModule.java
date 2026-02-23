package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;

public class FastPlaceModule extends Module {
    public static volatile FastPlaceModule INSTANCE;

    // Fast place state management
    private boolean fastPlaceActive = false;

    public FastPlaceModule(Category category) {
        super("fastPlace", category, new Module.Options());
        if (INSTANCE == null) {
            synchronized (FastPlaceModule.class) {
                if (INSTANCE == null) {
                    INSTANCE = this;
                }
            }
        }
    }

    @Override
    public void onEnable() {
        // When module is enabled, fast place is active
        this.fastPlaceActive = true;
    }

    @Override
    public void onDisable() {
        // When module is disabled, fast place is disabled
        this.fastPlaceActive = false;
    }


}
