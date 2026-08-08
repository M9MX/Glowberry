package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.event.EventHandler;
import com.dwarslooper.cactus.client.event.impl.ClientTickEvent;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.KeybindSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.systems.key.KeyBind;

import net.minecraft.client.Minecraft;

import org.lwjgl.glfw.GLFW;

/**
 * Toggle Sprint module.
 * Press the configured key once to keep sprinting without holding it (vanilla
 * requires holding the sprint key). With "always sprint" enabled the player
 * automatically sprints whenever moving forward, as long as the vanilla sprint
 * conditions are met (food above 6, not sneaking, not riding, ...).
 */
public class ToggleSprintModule extends Module {
    public static volatile ToggleSprintModule INSTANCE;

    private final SettingGroup generalGroup;
    public final Setting<KeyBind> toggleKeybind;
    public final Setting<Boolean> alwaysSprint;

    private boolean sprintToggled = false;
    private boolean lastKeyState = false;

    public ToggleSprintModule(Category category) {
        super("toggleSprint", category, new Module.Options());
        INSTANCE = this;

        this.generalGroup = this.settings.buildGroup("general");
        this.toggleKeybind = this.generalGroup.add(new KeybindSetting("toggleKeybind", KeyBind.of(GLFW.GLFW_KEY_LEFT_CONTROL)));
        this.alwaysSprint = this.generalGroup.add(new BooleanSetting("alwaysSprint", true));
    }

    @Override
    public void onEnable() {
        sprintToggled = false;
        lastKeyState = false;
    }

    @Override
    public void onDisable() {
        sprintToggled = false;
        lastKeyState = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
    }

    @EventHandler
    public void onTick(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Toggle key handling (edge detection so one press = one toggle)
        boolean keyDown = isToggleKeyPressed();
        if (keyDown && !lastKeyState) {
            lastKeyState = true;
            sprintToggled = !sprintToggled;
            if (!sprintToggled) {
                mc.player.setSprinting(false);
            }
        } else if (!keyDown && lastKeyState) {
            lastKeyState = false;
        }

        // Keep sprinting while toggled on or always-sprint is enabled
        if (sprintToggled || alwaysSprint.get()) {
            if (canSprint(mc)) {
                mc.player.setSprinting(true);
            }
        }
    }

    private boolean isToggleKeyPressed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen != null) return false;
        try {
            Object keybind = this.toggleKeybind.get();
            if (keybind instanceof KeyBind) {
                return ((KeyBind) keybind).isPressed();
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    /**
     * Mirrors the vanilla sprint conditions: needs forward movement input,
     * food level above 6, and no state that blocks sprinting.
     */
    private boolean canSprint(Minecraft mc) {
        if (mc.player == null) return false;
        if (mc.player.isSpectator()) return false;
        if (mc.player.isCrouching()) return false;
        if (mc.player.isFallFlying()) return false;
        if (mc.player.isPassenger()) return false;
        if (mc.player.getFoodData().getFoodLevel() <= 6) return false;
        return mc.player.input.hasForwardImpulse();
    }

    /** Whether the manual sprint toggle is currently engaged. */
    public boolean isSprintToggled() {
        return sprintToggled;
    }

    /** Whether the module is actively driving sprinting (manual toggle or always sprint). */
    public boolean isSprintRequested() {
        return sprintToggled || alwaysSprint.get();
    }
}
