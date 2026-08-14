package org.m9mx.cactus.glowberry.util;

import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.feature.module.ModuleManager;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.KeybindSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.systems.key.KeyBind;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared plumbing for the two Glowberry HUD elements:
 *
 * <ul>
 *   <li><b>Module Message routing</b> - modules (and the toggle feedback mixin)
 *       push messages here. When the Module Message HUD element is placed they
 *       appear there; otherwise they fall back to the action bar, so nothing is
 *       lost either way. Only one message is ever shown - a new message replaces
 *       the previous one.</li>
 *   <li><b>Toggle tracking</b> - the last toggle time of every module, used by the
 *       Modules List element to animate entries when modules get enabled/disabled.</li>
 * </ul>
 */
public final class ModuleMessageUtil {
    private ModuleMessageUtil() {}

    /** Set by {@code ModuleMessageHudElement} when it is placed in the HUD. */
    public static volatile boolean moduleMessageElementActive = false;

    /** The single message shown by the Module Message HUD element (null = nothing). */
    public static volatile Message currentMessage = null;

    /** Last time each module was toggled (ms), for the Modules List animation. */
    private static final Map<String, Long> TOGGLE_TIMES = new HashMap<>();

    /**
     * One queued message. A message can carry an optional leading item icon or
     * GUI sprite (e.g. a potion effect icon) drawn before the text.
     */
    public static final class Message {
        public final Component text;
        public final int color;
        public final long time;
        public final Identifier sprite;   // GUI atlas sprite, e.g. potion icons (may be null)
        public final ItemStack icon;      // item icon, e.g. an item the message is about (may be empty)

        public Message(Component text, int color) {
            this(text, color, null, ItemStack.EMPTY);
        }

        public Message(Component text, int color, Identifier sprite, ItemStack icon) {
            this.text = text;
            this.color = color;
            this.sprite = sprite;
            this.icon = icon == null ? ItemStack.EMPTY : icon;
            this.time = System.currentTimeMillis();
        }
    }

    /**
     * Routes a message: to the Module Message element when it is placed,
     * otherwise to the action bar. A new message replaces the previous one.
     */
    public static void show(Component text, int color) {
        show(new Message(text, color));
    }

    /** Routes a message with a leading sprite (e.g. a potion effect icon). */
    public static void show(Component text, int color, Identifier sprite) {
        show(new Message(text, color, sprite, ItemStack.EMPTY));
    }

    /** Routes a message with a leading item icon. */
    public static void show(Component text, int color, ItemStack icon) {
        show(new Message(text, color, null, icon));
    }

    public static void show(Message message) {
        if (moduleMessageElementActive) {
            currentMessage = message;
            return;
        }
        // Action bar fallback: drop the sprite/icon, only the text fits there
        ActionBarUtil.sendActionBarMessage(message.text.getString());
    }

    /** Toggle feedback message for a module ("Name enabled"/"Name disabled"). */
    public static void showToggleFeedback(Module module) {
        boolean on = module.active();
        int color = on ? 0xFF55FF55 : 0xFFFF5555;
        show(Component.literal(module.getDisplayName() + " " + (on ? "§aenabled" : "§cdisabled")), color);
    }

    /** Records a toggle so the Modules List can animate the change. */
    public static void recordToggle(Module module) {
        TOGGLE_TIMES.put(module.getID(), System.currentTimeMillis());
    }

    /** Time the module was last toggled (ms), or -1 if never toggled this session. */
    public static long lastToggleTime(Module module) {
        return TOGGLE_TIMES.getOrDefault(module.getID(), -1L);
    }

    /**
     * The keybind shown next to a module name in the Modules List, or null.
     * Priority: the module's own bind, then a custom keybind setting (e.g. the
     * toggle key of Auto Clicker / Shuffle / Anti AFK). Only one is ever shown.
     */
    public static String moduleKeybind(Module module) {
        try {
            KeyBind bind = module.getBind();
            if (bind != null && bind.isBound()) {
                return bind.getDisplay();
            }
        } catch (Exception ignored) {
            // Not every module exposes a bind - fall through to the settings scan
        }

        try {
            for (SettingGroup group : module.settings) {
                for (Setting<?> setting : group.getSettings()) {
                    if (setting instanceof KeybindSetting keybindSetting) {
                        KeyBind value = keybindSetting.get();
                        if (value != null && value.isBound()) {
                            return value.getDisplay();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // A broken setting must never break the whole list
        }
        return null;
    }

    /** Used by the mixins to reach the module manager safely. */
    public static ModuleManager moduleManager() {
        try {
            return ModuleManager.get();
        } catch (Exception e) {
            return null;
        }
    }
}
