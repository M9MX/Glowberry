package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.element.DynamicHudElement;
import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Vector2i;
import org.m9mx.cactus.glowberry.feature.modules.ToggleSprintModule;

@SuppressWarnings("unused")
public class ToggleSprintHudElement extends DynamicHudElement<ToggleSprintHudElement> {
    public enum Alignment { LEFT, CENTER, RIGHT }

    private final Setting<Boolean>   alwaysShow;
    private final Setting<Alignment> alignment;
    private final Setting<Integer>   scale;

    private static final int COL_LABEL   = 0xFFFFFFFF;
    private static final int COL_ON      = 0xFF55FF55;
    private static final int COL_OFF     = 0xFFAAAAAA;
    private static final int COL_TOGGLED = 0xFFFFD966;

    private static final int PAD_X       = 6;
    private static final int PAD_Y       = 4;
    private static final int LINE_HEIGHT = 11;
    private static final int OFFSCREEN   = -99999;

    private int savedX   = Integer.MIN_VALUE;
    private int savedY   = Integer.MIN_VALUE;
    private boolean isHidden = false;
    private int lastWidth = -1;

    public ToggleSprintHudElement() {
        super("toggle_sprint", new Vector2i(1, 1));
        this.style.set(HudElement.Style.Default);
        var sgGeneral = this.settings.buildGroup("general");
        this.alwaysShow = sgGeneral.add(new BooleanSetting("alwaysShow", false));
        this.alignment  = sgGeneral.add(new EnumSetting<>("alignment", Alignment.LEFT));
        this.scale      = sgGeneral.add(new IntegerSetting("scale", 100).min(25).max(400));
    }

    private void hideOffscreen() {
        if (!isHidden) {
            savedX = this.getRelativePosition().x();
            savedY = this.getRelativePosition().y();
            this.move(OFFSCREEN, OFFSCREEN);
            isHidden = true;
        }
    }

    private void restorePosition() {
        if (isHidden && savedX != Integer.MIN_VALUE) {
            this.move(savedX, savedY);
            isHidden = false;
        }
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
        ToggleSprintModule module = ToggleSprintModule.INSTANCE;
        boolean moduleActive = module != null && module.active();

        // Show "(Toggled)" when the module is driving sprinting itself
        // (manual toggle engaged or always sprint enabled)
        boolean toggled = moduleActive && module.isSprintRequested();

        boolean sprinting;
        if (inEditor) {
            // Show a sample state while editing the element in the HUD editor
            sprinting = true;
            toggled = true;
        } else {
            Minecraft mc = Minecraft.getInstance();
            sprinting = moduleActive && mc.player != null && mc.player.isSprinting();
        }

        boolean show = inEditor || moduleActive || alwaysShow.get();
        if (!show) {
            hideOffscreen();
            return;
        }
        restorePosition();

        Minecraft mc   = Minecraft.getInstance();
        float scaleF   = scale.get() / 100f;
        Alignment align = alignment.get();

        String state  = sprinting ? "ON" : "OFF";
        int stateColor = sprinting ? COL_ON : COL_OFF;
        String suffix = toggled ? " (Toggled)" : "";

        int unscaledW = PAD_X * 2 + mc.font.width("Sprint: ") + mc.font.width(state) + mc.font.width(suffix);
        int unscaledH = PAD_Y * 2 + LINE_HEIGHT;
        int scaledW   = Math.round(unscaledW * scaleF);
        int scaledH   = Math.round(unscaledH * scaleF);

        anchoredResize(scaledW, scaledH);

        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scaleF, scaleF);

        int fullWidth = mc.font.width("Sprint: " + state + suffix);
        int lineX;
        if (align == Alignment.RIGHT) {
            lineX = PAD_X + (unscaledW - PAD_X * 2 - fullWidth);
        } else if (align == Alignment.CENTER) {
            lineX = PAD_X + (unscaledW - PAD_X * 2 - fullWidth) / 2;
        } else {
            lineX = PAD_X;
        }

        int stateX = lineX + mc.font.width("Sprint: ");
        context.text(mc.font, "Sprint: ", lineX, PAD_Y, COL_LABEL);
        context.text(mc.font, state, stateX, PAD_Y, stateColor);
        context.text(mc.font, suffix, stateX + mc.font.width(state), PAD_Y, COL_TOGGLED);

        pose.popMatrix();
    }

    @Override
    public ToggleSprintHudElement duplicate() {
        return new ToggleSprintHudElement();
    }

    @Override
    public String getName() {
        return "Toggle Sprint";
    }
}
