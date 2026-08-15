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
 * automatically sprints whenever moving forward.
 *
 * <p>The module drives {@code options.keySprint.setDown()} - it fakes the
 * sprint key being held - instead of calling {@code setSprinting()} directly.
 * Vanilla's own sprint logic ({@code canStartSprinting}, {@code aiStep}, ...)
 * then evaluates every condition itself (food, headroom, water, sneaking,
 * fall-flying, ...), so the module can never fight vanilla into a flicker or
 * drift out of sync with the real sprint rules.
 *
 * <p>Inspired by BetterSprint by tfourj
 * (https://github.com/tfourj/BetterSprint), which fakes the sprint key the
 * same way instead of forcing the sprint state.
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
        // Release the faked sprint key so vanilla stops sprinting on its own.
        mc.options.keySprint.setDown(false);
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
            }
        } else if (!keyDown && lastKeyState) {
            lastKeyState = false;
        }

        // Fake the sprint key being held whenever sprint is wanted. Vanilla then
        // handles starting/stopping sprint and all its conditions itself - we
        // only supply the input, never the sprint state.
        boolean wantSprint = (sprintToggled || alwaysSprint.get()) && wantsForwardSprint(mc);
        mc.options.keySprint.setDown(wantSprint);
    }

    /**
     * Whether sprint should be requested right now. Only gates on the things
     * this module is about (the toggle being on and forward movement); every
     * other condition (food, headroom, water, sneaking, ...) is left to vanilla.
     */
    private boolean wantsForwardSprint(Minecraft mc) {
        if (mc.player == null) return false;
        if (mc.player.isSpectator()) return false;
        // Sneaking must win over our faked key so a toggled sprint can't
        // re-engage mid-sneak.
        if (mc.player.isCrouching()) return false;
        return mc.player.input.hasForwardImpulse();
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

    /** Whether the manual sprint toggle is currently engaged. */
    public boolean isSprintToggled() {
        return sprintToggled;
    }

    /** Whether the module is actively driving sprinting (manual toggle or always sprint). */
    public boolean isSprintRequested() {
        return sprintToggled || alwaysSprint.get();
    }
}
