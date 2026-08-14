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

    // Small debounce after toggling so one quick press can never register twice,
    // which is what made sprint randomly turn on and off.
    private int toggleCooldownTicks = 0;

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
        toggleCooldownTicks = 0;
    }

    @Override
    public void onDisable() {
        sprintToggled = false;
        lastKeyState = false;
        toggleCooldownTicks = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
    }

    @EventHandler
    public void onTick(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (toggleCooldownTicks > 0) toggleCooldownTicks--;

        // Toggle key handling (edge detection + a short debounce so one press
        // can never flip the toggle twice in a row)
        boolean keyDown = isToggleKeyPressed();
        if (keyDown && !lastKeyState) {
            lastKeyState = true;
            if (toggleCooldownTicks <= 0) {
                toggleCooldownTicks = 4;
                sprintToggled = !sprintToggled;
                if (sprintToggled) {
                    // Engage immediately so sprint doesn't lag a tick behind the press
                    if (canSprint(mc)) mc.player.setSprinting(true);
                } else {
                    mc.player.setSprinting(false);
                }
            }
        } else if (!keyDown && lastKeyState) {
            lastKeyState = false;
        }

        // Keep sprinting while toggled on or always-sprint is enabled. We only
        // ever set it to true; stopping is left to vanilla when the sprint
        // conditions are no longer met (stops moving, sneaks, runs out of food...).
        if ((sprintToggled || alwaysSprint.get()) && canSprint(mc) && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
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
