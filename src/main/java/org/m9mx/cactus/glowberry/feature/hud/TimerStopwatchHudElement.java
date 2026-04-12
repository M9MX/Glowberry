package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.element.DynamicHudElement;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector2i;
import org.m9mx.cactus.glowberry.feature.modules.TimerModule;
import org.m9mx.cactus.glowberry.feature.modules.StopwatchModule;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class TimerStopwatchHudElement extends DynamicHudElement<TimerStopwatchHudElement> {
    enum Direction {
        Vertical(new Vector2i(1, 1)),
        Horizontal(new Vector2i(1, 1));
        final Vector2i size;
        Direction(Vector2i size) { this.size = size; }
    }

    public enum Alignment { LEFT, CENTER, RIGHT }

    private final Setting<Boolean>   alwaysShow;
    private final Setting<Alignment> alignment;
    private final Setting<Integer>   scale;

    private static final int COL_LABEL    = 0xFFFFFFFF;
    private static final int COL_STATE    = 0xFFAAAAAA;
    private static final int COL_TIME     = 0xFFFFD966;
    private static final int COL_PUNCT    = 0xFF888888;
    private static final int COL_FINISHED = 0xFFFF6B6B;

    private static final int PAD_X       = 6;
    private static final int PAD_Y       = 4;
    private static final int LINE_HEIGHT = 11;
    private static final int SEP_HEIGHT  = 5;
    private static final int OFFSCREEN   = -99999;

    private int savedX   = Integer.MIN_VALUE;
    private int savedY   = Integer.MIN_VALUE;
    private boolean isHidden = false;

    private int lastWidth = -1;

    public TimerStopwatchHudElement() {
        super("timer_stopwatch", Direction.Horizontal.size);
        this.style.set(com.dwarslooper.cactus.client.gui.hud.element.HudElement.Style.Default);
        var sgGeneral = this.settings.buildGroup("general");
        this.alwaysShow = sgGeneral.add(new BooleanSetting("alwaysShow", false));
        this.alignment  = sgGeneral.add(new EnumSetting<>("alignment", Alignment.LEFT));
        this.scale      = sgGeneral.add(new IntegerSetting("scale", 100).min(25).max(400));
    }

    private static class Segment {
        String text;
        int color;
        Segment(String text, int color) { this.text = text; this.color = color; }
    }

    private static class Line {
        List<Segment> segments = new ArrayList<>();
        boolean isSeparator;

        Line() { this.isSeparator = false; }

        static Line separator() {
            Line l = new Line();
            l.isSeparator = true;
            return l;
        }

        void add(String text, int color) { segments.add(new Segment(text, color)); }

        String full() {
            if (isSeparator) return "";
            StringBuilder sb = new StringBuilder();
            for (Segment s : segments) sb.append(s.text);
            return sb.toString();
        }
    }

    private List<Segment> timeSegments(long millis, boolean showHours, boolean showMs) {
        long totalSecs = millis / 1000;
        long h  = totalSecs / 3600;
        long m  = (totalSecs % 3600) / 60;
        long s  = totalSecs % 60;
        long ms = millis % 1000;

        List<Segment> segs = new ArrayList<>();
        if (showHours) {
            segs.add(new Segment(String.format("%02dh", h), COL_TIME));
            segs.add(new Segment(" : ", COL_PUNCT));
        }
        segs.add(new Segment(String.format("%02dm", m), COL_TIME));
        segs.add(new Segment(" : ", COL_PUNCT));
        segs.add(new Segment(String.format("%02ds", s), COL_TIME));
        if (showMs) {
            segs.add(new Segment(" . ", COL_PUNCT));
            segs.add(new Segment(String.format("%03dms", ms), COL_TIME));
        }
        return segs;
    }

    private Line makeTimeLine(long millis, boolean showHours, boolean showMs) {
        Line line = new Line();
        line.segments.addAll(timeSegments(millis, showHours, showMs));
        return line;
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
    public void renderContent(GuiGraphics context, int x, int y, int width, int height, int screenWidth, int screenHeight, float delta, boolean inEditor) {
        TimerModule timer         = TimerModule.INSTANCE;
        StopwatchModule stopwatch = StopwatchModule.INSTANCE;

        boolean timerEnabled     = timer != null && timer.active();
        boolean stopwatchEnabled = stopwatch != null && stopwatch.active();
        boolean showHud;

        List<Line> lines     = new ArrayList<>();
        boolean hasTimer     = false;
        boolean hasStopwatch = false;

        if (inEditor) {
            restorePosition();
            Line l1 = new Line(); l1.add("Timer: ", COL_LABEL); l1.add("Stopped", COL_STATE); lines.add(l1);
            lines.add(makeTimeLine(0, false, true));
            lines.add(Line.separator());
            Line l3 = new Line(); l3.add("Stopwatch: ", COL_LABEL); l3.add("Stopped", COL_STATE); lines.add(l3);
            lines.add(makeTimeLine(0, false, true));
            showHud = true;
        } else {
            boolean always = alwaysShow.get();

            boolean timerActive     = timerEnabled && (always || timer.isRunning() || timer.isFinished() || timer.getState() == TimerModule.TimerState.PAUSED);
            boolean stopwatchActive = stopwatchEnabled && (always || stopwatch.isRunning() || stopwatch.getState() == StopwatchModule.StopwatchState.PAUSED);

            if (timerActive) {
                boolean finished = timer.isFinished();
                String stateStr  = finished ? "Finished"
                        : timer.getState() == TimerModule.TimerState.PAUSED ? "Paused"
                          : timer.isRunning() ? "Running" : "Stopped";
                int stateColor   = finished ? COL_FINISHED : COL_STATE;

                Line header = new Line();
                header.add("Timer: ", COL_LABEL);
                header.add(stateStr, stateColor);
                lines.add(header);

                boolean showHours = (timer.getTargetMillis() / 1000 / 3600) >= 1;
                boolean showMs    = timer.showMilliseconds.get();
                lines.add(makeTimeLine(finished ? 0 : timer.getRemainingMillis(), showHours, showMs));
                hasTimer = true;
            }

            if (stopwatchActive) {
                if (hasTimer) lines.add(Line.separator());

                boolean paused  = stopwatch.getState() == StopwatchModule.StopwatchState.PAUSED;
                String stateStr = paused ? "Paused" : stopwatch.isRunning() ? "Running" : "Stopped";

                Line header = new Line();
                header.add("Stopwatch: ", COL_LABEL);
                header.add(stateStr, COL_STATE);
                lines.add(header);

                boolean showMs    = stopwatch.showMilliseconds.get();
                boolean showHours = stopwatch.getElapsedMillis() >= 3_600_000;
                lines.add(makeTimeLine(stopwatch.getElapsedMillis(), showHours, showMs));

                if (!stopwatch.getLaps().isEmpty()) {
                    boolean lapShowHours = stopwatch.getLastLapMillis() >= 3_600_000;
                    Line lapLine = new Line();
                    lapLine.add("\nLast Lap: \n", COL_LABEL);
                    lapLine.segments.addAll(timeSegments(stopwatch.getLastLapMillis(), lapShowHours, showMs));
                    lines.add(lapLine);
                }

                hasStopwatch = true;
            }

            showHud = hasTimer || hasStopwatch;
        }

        if (timer != null)     timer.elementHUD     = showHud;
        if (stopwatch != null) stopwatch.elementHUD = showHud;

        if (!showHud) {
            hideOffscreen();
            return;
        }

        restorePosition();

        Minecraft mc        = Minecraft.getInstance();
        float scaleFactor   = scale.get() / 100f;
        Alignment align     = alignment.get();

        int maxTextWidth = 0;
        for (Line line : lines) {
            int w = mc.font.width(line.full());
            if (w > maxTextWidth) maxTextWidth = w;
        }

        int totalContentHeight = 0;
        for (Line line : lines) {
            totalContentHeight += line.isSeparator ? SEP_HEIGHT : LINE_HEIGHT;
        }

        int unscaledW = maxTextWidth + PAD_X * 2;
        int unscaledH = totalContentHeight + PAD_Y * 2;
        int scaledW   = Math.round(unscaledW * scaleFactor);
        int scaledH   = Math.round(unscaledH * scaleFactor);

        anchoredResize(scaledW, scaledH);

        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scaleFactor, scaleFactor);

        int textY = PAD_Y;
        for (Line line : lines) {
            if (line.isSeparator) {
                textY += SEP_HEIGHT;
                continue;
            }

            int lineWidth = mc.font.width(line.full());
            int lineX;
            if (align == Alignment.RIGHT) {
                lineX = PAD_X + (maxTextWidth - lineWidth);
            } else if (align == Alignment.CENTER) {
                lineX = PAD_X + (maxTextWidth - lineWidth) / 2;
            } else {
                lineX = PAD_X;
            }

            int curX = lineX;
            for (Segment seg : line.segments) {
                context.drawString(mc.font, seg.text, curX, textY, seg.color);
                curX += mc.font.width(seg.text);
            }
            textY += LINE_HEIGHT;
        }

        pose.popMatrix();
    }

    @Override
    public TimerStopwatchHudElement duplicate() {
        return new TimerStopwatchHudElement();
    }

    @Override
    public String getName() {
        return "Timer & Stopwatch";
    }
}