package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.feature.module.ModuleManager;
import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.joml.Vector2i;
import org.m9mx.cactus.glowberry.util.ModuleMessageUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Modules List - shows every toggled-on module as a vertical list of lines.
 * Each line is "Module Name", with the module's keybind in parentheses when it
 * has one (module bind first, then a custom keybind setting like the toggle key
 * of Auto Clicker / Shuffle / Anti AFK - only one is ever shown). Entries
 * animate in when a module gets toggled on and animate out when it gets
 * toggled off.
 */
@SuppressWarnings("unused")
public class ModulesListHudElement extends HideableHudElement<ModulesListHudElement> {
    public enum Alignment { LEFT, CENTER, RIGHT }

    private final Setting<Boolean>   showKeybind;
    private final Setting<Alignment> alignment;
    private final Setting<Integer>   scale;

    // Colors (all customizable through the element's textColor setting; the
    // keybind suffix and the animation tint are derived from it).
    private static final int PAD_X        = 6;
    private static final int PAD_Y        = 4;
    private static final int LINE_HEIGHT  = 11;
    private static final int ANIM_MS      = 260; // slide/fade duration on toggle
    private static final int SLIDE_PIXELS = 8;

    private int lastWidth = -1;

    @Override
    protected boolean shouldHide() {
        // Nothing to show while no module is toggled on
        return activeModules().isEmpty();
    }

    public ModulesListHudElement() {
        super("modules_list", new Vector2i(1, 1));
        this.style.set(HudElement.Style.Default);
        var sgGeneral = this.settings.buildGroup("general");
        this.showKeybind = sgGeneral.add(new BooleanSetting("showKeybind", true));
        this.alignment   = sgGeneral.add(new EnumSetting<>("alignment", Alignment.LEFT));
        this.scale       = sgGeneral.add(new IntegerSetting("scale", 100).min(25).max(400));
    }

    /** All currently toggled modules, sorted by full line length (name + keybind), longest first. */
    private static List<Module> activeModules() {
        List<Module> modules = new ArrayList<>();
        try {
            ModuleManager manager = ModuleManager.get();
            if (manager == null) return modules;
            Map<Class<? extends Module>, Module> map = manager.getModules();
            if (map == null) return modules;
            for (Module module : map.values()) {
                if (module != null && module.active()) {
                    modules.add(module);
                }
            }
        } catch (Exception ignored) {
            // A broken module manager must never break the whole element
        }
        modules.sort(Comparator.comparingInt(ModulesListHudElement::displayLength).reversed()
                .thenComparing(Module::getDisplayName));
        return modules;
    }

    /** The rendered width of a module's line (name + " (keybind)" when shown). */
    private static int displayLength(Module module) {
        int len = module.getDisplayName().length();
        String kb = ModuleMessageUtil.moduleKeybind(module);
        if (kb != null) {
            len += kb.length() + 3; // " (" + kb + ")"
        }
        return len;
    }

    private void anchoredResize(int newWidth, int newHeight) {
        int oldWidth = lastWidth == -1 ? newWidth : lastWidth;
        lastWidth = newWidth;
        this.resize(newWidth, newHeight);
        if (lastWidth != -1 && newWidth != oldWidth) {
            int dx = newWidth - oldWidth;
            Alignment align = alignment.get();
            if (align == Alignment.CENTER) {
                this.move(this.getRelativePosition().x() - dx / 2, this.getRelativePosition().y());
            } else if (align == Alignment.RIGHT) {
                this.move(this.getRelativePosition().x() - dx, this.getRelativePosition().y());
            }
        }
    }

    @Override
    public void renderContent(GuiGraphicsExtractor context, int x, int y, int width, int height, int screenWidth, int screenHeight, float delta, boolean inEditor) {
        List<Module> modules = inEditor ? editorSample() : activeModules();
        if (modules.isEmpty()) {
            // Editor-only placeholder so the element preview is never blank
            if (!inEditor) return;
            modules = EDITOR_PLACEHOLDERS;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;

        int textColor = this.textColor.get().color();
        float scaleF = scale.get() / 100f;
        Alignment align = alignment.get();
        boolean keybind = showKeybind.get();
        long now = System.currentTimeMillis();

        // Measure every line so the box fits the widest one
        int maxW = 0;
        for (Module module : modules) {
            int w = mc.font.width(module.getDisplayName());
            String kb = keybind ? moduleKeybind(module) : null;
            if (kb != null) {
                w += mc.font.width(" (" + kb + ")");
            }
            maxW = Math.max(maxW, w);
        }
        if (maxW == 0) maxW = 10;

        int unscaledW = PAD_X * 2 + maxW;
        int unscaledH = PAD_Y * 2 + modules.size() * LINE_HEIGHT;
        anchoredResize(Math.round(unscaledW * scaleF), Math.round(unscaledH * scaleF));

        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scaleF, scaleF);

        int lineY = PAD_Y;
        for (Module module : modules) {
            long toggleTime = ModuleMessageUtil.lastToggleTime(module);
            long age = now - toggleTime;

            // Animation: fade + slide from the alignment side for ANIM_MS after a toggle
            float progress = toggleTime > 0 ? Mth.clamp(age / (float) ANIM_MS, 0f, 1f) : 1f;
            int alpha = (int) (255 * progress);
            int color = (textColor & 0x00FFFFFF) | (Mth.clamp(alpha, 0, 255) << 24);
            int offset = (int) ((1f - progress) * SLIDE_PIXELS);

            String name = module.getDisplayName();
            String kb = keybind ? moduleKeybind(module) : null;
            String full = kb != null ? name + " (" + kb + ")" : name;
            int textW = mc.font.width(full);

            int lineX = switch (align) {
                case CENTER -> PAD_X + (maxW - textW) / 2;
                case RIGHT -> PAD_X + (maxW - textW);
                default -> PAD_X;
            };
            // Slide in from the alignment side
            lineX += switch (align) {
                case CENTER -> 0;
                case RIGHT -> offset;
                default -> -offset;
            };

            context.text(mc.font, name, lineX, lineY, color);
            if (kb != null) {
                // Dimmer version of the same color for the keybind suffix
                int dim = (color & 0x00FFFFFF) | (((color >>> 24) * 3 / 5) << 24);
                context.text(mc.font, " (" + kb + ")", lineX + mc.font.width(name), lineY, dim);
            }
            lineY += LINE_HEIGHT;
        }

        pose.popMatrix();
    }

    private static List<Module> editorSample() {
        List<Module> sample = new ArrayList<>();
        ModuleManager manager = ModuleMessageUtil.moduleManager();
        if (manager == null) return sample;
        try {
            Map<Class<? extends Module>, Module> map = manager.getModules();
            if (map == null) return sample;
            // Prefer a few actually toggled modules; otherwise take the first few
            for (Module module : map.values()) {
                if (module != null && module.active() && sample.size() < 4) {
                    sample.add(module);
                }
            }
            if (sample.isEmpty()) {
                for (Module module : map.values()) {
                    if (module != null && sample.size() < 4) {
                        sample.add(module);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        sample.sort(Comparator.comparingInt(ModulesListHudElement::displayLength).reversed()
                .thenComparing(Module::getDisplayName));
        return sample;
    }

    private static String moduleKeybind(Module module) {
        return module == null ? null : ModuleMessageUtil.moduleKeybind(module);
    }

    /** Placeholder modules used only for the HUD editor preview when none exist. */
    private static final class PlaceholderModule extends Module {
        private final String name;

        PlaceholderModule(String id, String name) {
            super(id);
            this.name = name;
        }

        @Override
        public String getDisplayName() {
            return name;
        }
    }

    private static final List<Module> EDITOR_PLACEHOLDERS = List.of(
            new PlaceholderModule("toggleSprint", "Toggle Sprint"),
            new PlaceholderModule("autoClicker", "Auto Clicker"),
            new PlaceholderModule("lightLevel", "Light Level")
    );

    @Override
    public ModulesListHudElement duplicate() {
        return new ModulesListHudElement();
    }

    @Override
    public String getName() {
        return "Modules List";
    }
}
