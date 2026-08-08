package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.element.DynamicHudElement;
import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class HorseStatsHudElement extends DynamicHudElement<HorseStatsHudElement> {
    public enum Alignment { LEFT, CENTER, RIGHT }
    public enum Orientation { HORIZONTAL, VERTICAL }

    private final Setting<Boolean> alwaysShow;
    private final Setting<Boolean> showSpeed;
    private final Setting<Boolean> showJump;
    private final Setting<Boolean> showHealth;
    private final Setting<Orientation> orientation;
    private final Setting<Alignment> alignment;
    private final Setting<Integer>   scale;

    private static final int COL_LABEL    = 0xFFFFFFFF;
    private static final int COL_SPEED    = 0xFF55FFFF;
    private static final int COL_JUMP     = 0xFF55FF55;
    private static final int COL_HEALTH   = 0xFFFF5555;
    private static final int COL_PUNCT    = 0xFF888888;
    private static final int COL_UNKNOWN  = 0xFFAAAAAA;

    private static final int PAD_X       = 6;
    private static final int PAD_Y       = 4;
    private static final int LINE_HEIGHT = 11;
    private static final int OFFSCREEN   = -99999;

    private int savedX   = Integer.MIN_VALUE;
    private int savedY   = Integer.MIN_VALUE;
    private boolean isHidden = false;
    private int lastWidth = -1;

    // Last known stats of the ridden horse; -1 means "never seen one yet"
    private double lastSpeed  = -1;
    private double lastJump   = -1;
    private double lastHealth = -1;

    public HorseStatsHudElement() {
        super("horse_stats", new Vector2i(1, 1));
        this.style.set(HudElement.Style.Default);
        var sgGeneral = this.settings.buildGroup("general");
        this.alwaysShow  = sgGeneral.add(new BooleanSetting("alwaysShow", false));
        this.showSpeed   = sgGeneral.add(new BooleanSetting("showSpeed", true));
        this.showJump    = sgGeneral.add(new BooleanSetting("showJump", true));
        this.showHealth  = sgGeneral.add(new BooleanSetting("showHealth", true));
        this.orientation = sgGeneral.add(new EnumSetting<>("orientation", Orientation.HORIZONTAL));
        this.alignment   = sgGeneral.add(new EnumSetting<>("alignment", Alignment.LEFT));
        this.scale       = sgGeneral.add(new IntegerSetting("scale", 100).min(25).max(400));
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

    private static class Segment {
        final String text;
        final int color;
        Segment(String text, int color) { this.text = text; this.color = color; }
    }

    private static class Stat {
        final String label;
        final String value;
        final int color;
        Stat(String label, String value, int color) { this.label = label; this.value = value; this.color = color; }
    }

    private List<Stat> buildStats() {
        List<Stat> stats = new ArrayList<>();

        // Speed: attribute is in blocks/second, vanilla displays it as ~4.3..14.5 b/s
        if (showSpeed.get()) {
            stats.add(lastSpeed >= 0
                    ? new Stat("Speed: ", String.format("%.2f b/s", lastSpeed), COL_SPEED)
                    : new Stat("Speed: ", "--", COL_UNKNOWN));
        }
        if (showJump.get()) {
            stats.add(lastJump >= 0
                    ? new Stat("Jump: ", String.format("%.1f blocks", lastJump), COL_JUMP)
                    : new Stat("Jump: ", "--", COL_UNKNOWN));
        }
        if (showHealth.get()) {
            stats.add(lastHealth >= 0
                    ? new Stat("Health: ", String.format("%.0f", lastHealth), COL_HEALTH)
                    : new Stat("Health: ", "--", COL_UNKNOWN));
        }
        return stats;
    }

    /** Builds the rows of segments for the current orientation. */
    private List<List<Segment>> buildRows(List<Stat> stats) {
        List<List<Segment>> rows = new ArrayList<>();

        if (orientation.get() == Orientation.VERTICAL) {
            // One line per stat
            for (Stat stat : stats) {
                List<Segment> row = new ArrayList<>();
                row.add(new Segment(stat.label, COL_LABEL));
                row.add(new Segment(stat.value, stat.color));
                rows.add(row);
            }
        } else {
            // Single line, stats joined by separators
            List<Segment> row = new ArrayList<>();
            boolean first = true;
            for (Stat stat : stats) {
                if (!first) row.add(new Segment(" | ", COL_PUNCT));
                row.add(new Segment(stat.label, COL_LABEL));
                row.add(new Segment(stat.value, stat.color));
                first = false;
            }
            rows.add(row);
        }
        return rows;
    }

    private static int rowWidth(Minecraft mc, List<Segment> row) {
        int w = 0;
        for (Segment seg : row) w += mc.font.width(seg.text);
        return w;
    }

    @Override
    public void renderContent(GuiGraphicsExtractor context, int x, int y, int width, int height, int screenWidth, int screenHeight, float delta, boolean inEditor) {
        Minecraft mc = Minecraft.getInstance();

        AbstractHorse horse = mc.player != null && mc.player.getVehicle() instanceof AbstractHorse h ? h : null;

        if (horse != null) {
            // Live stats of the horse we are currently riding
            double jumpStrength = horse.getAttributeValue(Attributes.JUMP_STRENGTH);
            double movementSpeed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
            lastSpeed  = movementSpeed * 42.15;
            lastJump   = -1.291 * jumpStrength * jumpStrength + 4.707 * jumpStrength - 0.016;
            lastHealth = horse.getAttributeValue(Attributes.MAX_HEALTH);
        } else if (inEditor) {
            // Sample values for the HUD editor preview
            lastSpeed  = 8.43;
            lastJump   = 2.0;
            lastHealth = 30;
        }

        boolean onHorse = horse != null;
        boolean show = inEditor || onHorse || alwaysShow.get();
        if (!show) {
            hideOffscreen();
            return;
        }
        restorePosition();

        float scaleF    = scale.get() / 100f;
        Alignment align = alignment.get();

        List<Stat> stats = buildStats();
        if (stats.isEmpty()) {
            // All stat toggles are off - nothing to render
            hideOffscreen();
            return;
        }

        List<List<Segment>> rows = buildRows(stats);

        int unscaledW = PAD_X * 2;
        for (List<Segment> row : rows) {
            unscaledW = Math.max(unscaledW, rowWidth(mc, row) + PAD_X * 2);
        }
        int unscaledH = PAD_Y * 2 + rows.size() * LINE_HEIGHT;
        int scaledW   = Math.round(unscaledW * scaleF);
        int scaledH   = Math.round(unscaledH * scaleF);

        anchoredResize(scaledW, scaledH);

        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scaleF, scaleF);

        int rowY = PAD_Y;
        for (List<Segment> row : rows) {
            int rowW = rowWidth(mc, row);
            int lineX;
            if (align == Alignment.RIGHT) {
                lineX = PAD_X + (unscaledW - PAD_X * 2 - rowW);
            } else if (align == Alignment.CENTER) {
                lineX = PAD_X + (unscaledW - PAD_X * 2 - rowW) / 2;
            } else {
                lineX = PAD_X;
            }

            int curX = lineX;
            for (Segment seg : row) {
                context.text(mc.font, seg.text, curX, rowY, seg.color);
                curX += mc.font.width(seg.text);
            }
            rowY += LINE_HEIGHT;
        }

        pose.popMatrix();
    }

    @Override
    public HorseStatsHudElement duplicate() {
        return new HorseStatsHudElement();
    }

    @Override
    public String getName() {
        return "Horse Stats";
    }
}
