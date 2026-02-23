package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.event.EventHandler;
import com.dwarslooper.cactus.client.event.impl.ClientTickEvent;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.util.game.ChatUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameType;
public class FastBreakModule extends Module {
    public static volatile FastBreakModule INSTANCE;

    public FastBreakModule(Category category) {
        super("fastBreak", category, new Module.Options());
        if (INSTANCE == null) {
            synchronized(FastBreakModule.class) {
                if (INSTANCE == null) {
                    INSTANCE = this;
                }
            }
        }
    }

    @Override
    public void onEnable() {
        // Check if we're in creative mode before allowing the module to be enabled
        if (!isInCreativeMode()) {
            this.toggle(); // Disable the module since we're not in creative mode
            // Using the correct method based on the ExampleCommand
            ChatUtils.infoPrefix("Fast Break", "can only be enabled in Creative mode!");
            return;
        }
        // When module is enabled, fast break is active
    }

    @Override
    public void onDisable() {
        // When module is disabled, fast break is disabled
    }
    
    @EventHandler
    public void onClientTick(ClientTickEvent event) {
        // Check if we switched from creative to survival and disable the module if needed
        if (this.active() && !isInCreativeMode()) {
            this.toggle(); // Automatically disable the module
            ChatUtils.infoPrefix("Fast Break", "disabled because you left Creative mode!");
        }
    }

    private boolean isInCreativeMode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.gameMode != null) {
            return mc.gameMode.getPlayerMode() == GameType.CREATIVE;
        }
        return false;
    }
}