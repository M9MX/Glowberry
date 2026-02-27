package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.event.EventHandler;
import com.dwarslooper.cactus.client.event.impl.ClientTickEvent;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.KeybindSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.systems.key.KeyBind;

import org.m9mx.cactus.glowberry.cactus.FloatSetting;
import org.m9mx.cactus.glowberry.util.ActionBarUtil;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.m9mx.cactus.glowberry.accessor.IKeyBindingAccessor;
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
        this.attackSpeed = this.generalGroup.add(new FloatSetting("attackSpeed", 1.6f).min(0.0f).max(3.0f));
        this.toggleKeybind = this.generalGroup.add(new KeybindSetting("toggleKeybind", KeyBind.of(GLFW.GLFW_KEY_X)));
    }

    @Override
    public void onEnable() {
        // Module is enabled, ready to auto click when key is pressed
    }

    @Override
    public void onDisable() {
        this.autoClickerActive = false;
        releaseKeys();
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
            String status = this.autoClickerActive ? "§aEnabled" : "§cDisabled";
            ActionBarUtil.sendActionBarMessage("AutoClicker " + status);

            if (!this.autoClickerActive) {
                releaseKeys();
            }
        }
        lastKeyState = currentKeyState;

        // Perform auto clicking if active and enough time has passed
        // Delay in milliseconds: 1 second / attack speed
        long delayMs = (long) ((1.0 / this.attackSpeed.get()) * 1000);
        if (this.autoClickerActive && System.currentTimeMillis() - lastClickTime >= delayMs) {
            performClick(mc);
            releaseKeys();
            lastClickTime = System.currentTimeMillis();
        }
    }

    private void performClick(Minecraft mc) {
        ButtonType type = this.buttonType.get();

        if (type == ButtonType.LEFT) {
            // Simulate a left click by incrementing clickCount and setting key down
            // Minecraft's handleKeybinds() will consume this and handle attack/break properly
            ((IKeyBindingAccessor) (Object) mc.options.keyAttack).glowberry_SetTimesPressed(1);
            mc.options.keyAttack.setDown(true);
        } else if (type == ButtonType.RIGHT) {
            // Simulate a right click by incrementing clickCount and setting key down
            // Minecraft's handleKeybinds() will consume this and handle use/place properly
            ((IKeyBindingAccessor) (Object) mc.options.keyUse).glowberry_SetTimesPressed(1);
            mc.options.keyUse.setDown(true);
        }
    }

    private void releaseKeys() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            mc.options.keyAttack.setDown(false);
            mc.options.keyUse.setDown(false);
        }
    }

    private boolean isToggleKeyPressed() {
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
}