package org.m9mx.cactus.glowberry.feature.modules;

import org.lwjgl.glfw.GLFW;
import org.m9mx.cactus.glowberry.accessor.IMinecraftClickAccessor;
import org.m9mx.cactus.glowberry.cactus.FloatSetting;
import org.m9mx.cactus.glowberry.util.ModuleMessageUtil;

import com.dwarslooper.cactus.client.event.EventHandler;
import com.dwarslooper.cactus.client.event.impl.ClientTickEvent;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.KeybindSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.systems.key.KeyBind;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class AutoClickerModule extends Module {
    public static AutoClickerModule INSTANCE;

    private final SettingGroup generalGroup;
    public final Setting<ButtonType> buttonType;
    public final Setting<Float> attackSpeed;
    public final Setting<KeyBind> toggleKeybind;

    private boolean autoClickerActive = false;
    private long lastClickTime = 0;
    private boolean lastKeyState = false;

    public enum ButtonType {
        LEFT,
        RIGHT
    }

    public AutoClickerModule(Category category) {
        super("autoClicker", category, new Module.Options());
        INSTANCE = this;

        this.generalGroup = this.settings.buildGroup("general");
        this.buttonType = this.generalGroup.add(new EnumSetting<>("buttonType", ButtonType.LEFT));
        this.attackSpeed = this.generalGroup.add(new FloatSetting("attackSpeed", 1.6f).min(0.0f).max(3.0f).decimals(1));
        this.toggleKeybind = this.generalGroup.add(new KeybindSetting("toggleKeybind", KeyBind.of(GLFW.GLFW_KEY_X)));
    }

    @Override
    public void onEnable() {
        // Module is enabled, ready to auto click when key is pressed
    }

    @Override
    public void onDisable() {
        this.autoClickerActive = false;
    }

    @EventHandler
    public void onTick(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Handle toggle key press
        boolean currentKeyState = isToggleKeyPressed();
        if (currentKeyState && !lastKeyState) {
            this.autoClickerActive = !this.autoClickerActive;
            int color = this.autoClickerActive ? 0xFF55FF55 : 0xFFFF5555;
            ModuleMessageUtil.show(Component.literal("AutoClicker " + (this.autoClickerActive ? "§aEnabled" : "§cDisabled")), color);
        }
        lastKeyState = currentKeyState;

        // Perform auto clicking if active and enough time has passed
        // Delay in milliseconds: 1 second / attack speed
        long delayMs = (long) ((1.0 / this.attackSpeed.get()) * 1000);
        if (this.autoClickerActive && System.currentTimeMillis() - lastClickTime >= delayMs) {
            performClick(mc);
            lastClickTime = System.currentTimeMillis();
        }
    }

    private void performClick(Minecraft mc) {
        ButtonType type = this.buttonType.get();

        // Use vanilla's own click handlers instead of faking a held key. The old
        // approach set keyAttack/keyUse down every click, which left the input
        // system in a stuck "mouse held" state that dragged the framerate down
        // to ~30fps until the mouse was physically moved.
        IMinecraftClickAccessor accessor = (IMinecraftClickAccessor) (Object) mc;
        if (type == ButtonType.LEFT) {
            accessor.glowberry_StartAttack();
        } else {
            accessor.glowberry_StartUseItem();
        }
    }

    private boolean isToggleKeyPressed() {
        Minecraft mc = Minecraft.getInstance();
        if (isScreenOpen(mc)) {
			return false;
		}
        try {
            Object keybind = this.toggleKeybind.get();
            if (keybind instanceof com.dwarslooper.cactus.client.systems.key.KeyBind) {
                return ((com.dwarslooper.cactus.client.systems.key.KeyBind) keybind).isPressed();
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }
    
    private boolean isScreenOpen(Minecraft mc) {
		// 26.2: the current screen moved from Minecraft.screen to Minecraft.gui.screen()
		return mc.gui.screen() != null;
	}
}