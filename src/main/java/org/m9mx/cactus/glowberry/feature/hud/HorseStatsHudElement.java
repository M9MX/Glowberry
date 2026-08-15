package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.element.DynamicHudElement;
import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class HorseStatsHudElement extends HideableHudElement<HorseStatsHudElement> {
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

    // Horse stat formulas taken from Jade (https://github.com/snownee/Jade), with
    // values from https://minecraft.wiki/w/Horse#Movement_speed and
    // https://github.com/sakura-ryoko/minihud/pull/179.
    private static final double JUMP_A = 4.53680079;
    private static final double JUMP_B = 1.61431730;
    private static final double JUMP_C = -0.22656224;

    private int lastWidth = -1;

    @Override
    protected boolean shouldHide() {
        if (!showSpeed.get() && !showJump.get() && !showHealth.get()) return true;
        if (alwaysShow.get()) return false;
        Minecraft mc = Minecraft.getInstance();
        return riddenMount(mc) == null;
    }

    // Last known stats of the ridden animal; -1 means "never seen one yet"
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

    /** Jump height in blocks for an internal jump strength value. Credit: Jade. */
    private static double getJumpHeight(double jumpStrength) {
        return JUMP_A * jumpStrength * jumpStrength + JUMP_B * jumpStrength + JUMP_C;
    }

    /**
     * Movement speed in blocks/second from the internal attribute value.
     * Credit: Jade, values from the Minecraft wiki and minihud.
     */
    private static double getSpeed(double speed) {
        // https://minecraft.wiki/w/Horse#Movement_speed
        // https://github.com/sakura-ryoko/minihud/pull/179
        return speed * 43.171815466666658 - 0.000000339999999;
    }

    /**
     * The ridable animal we are currently sitting on, or null. Works for every
     * {@link AbstractHorse} (horse, donkey, mule, skeleton/zombie horse, llama)
     * as well as camels, which expose the same speed/jump attributes.
     */
    private static LivingEntity riddenMount(Minecraft mc) {
        if (mc.player == null) return null;
        net.minecraft.world.entity.Entity vehicle = mc.player.getVehicle();
        if (vehicle instanceof AbstractHorse || vehicle instanceof Camel) {
            return (LivingEntity) vehicle;
        }
        return null;
    }

    private List<Stat> buildStats() {
        List<Stat> stats = new ArrayList<>();

        // Speed: attribute is in internal units, displayed as blocks/second
        if (showSpeed.get()) {
            stats.add(lastSpeed >= 0
                    ? new Stat("Speed: ", String.format("%.2f b/s", lastSpeed), COL_SPEED)
                    : new Stat("Speed: ", "--", COL_UNKNOWN));
        }
        if (showJump.get()) {
            stats.add(lastJump > 0.001
                    ? new Stat("Jump: ", String.format("%.2f blocks", lastJump), COL_JUMP)
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

        LivingEntity mount = riddenMount(mc);

        if (mount != null) {
            // Live stats of the animal we are currently riding
            double jumpStrength = mount.getAttributeValue(Attributes.JUMP_STRENGTH);
            double movementSpeed = mount.getAttributeValue(Attributes.MOVEMENT_SPEED);
            lastSpeed  = getSpeed(movementSpeed);
            lastJump   = getJumpHeight(jumpStrength);
            lastHealth = mount.getAttributeValue(Attributes.MAX_HEALTH);
        } else if (inEditor) {
            // Sample values for the HUD editor preview
            lastSpeed  = 8.43;
            lastJump   = 2.89;
            lastHealth = 30;
        }

        boolean onMount = mount != null;
        boolean show = inEditor || onMount || alwaysShow.get();
        if (!show) return;

        float scaleF    = scale.get() / 100f;
        Alignment align = alignment.get();

        List<Stat> stats = buildStats();
        if (stats.isEmpty()) {
            // All stat toggles are off - nothing to render
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
