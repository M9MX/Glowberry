package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.element.DynamicHudElement;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector2i;
import org.m9mx.cactus.glowberry.feature.modules.TimerModule;
import org.m9mx.cactus.glowberry.feature.modules.StopwatchModule;

import java.util.ArrayList;
import java.util.List;

public class TimerStopwatchHudElement extends DynamicHudElement<TimerStopwatchHudElement> {
    enum Direction {
        // Initial size is minimal; real size is set dynamically in renderContent
        Vertical(new Vector2i(1, 1)),
        Horizontal(new Vector2i(1, 1));
        final Vector2i size;
        Direction(Vector2i size) { this.size = size; }
    }

    private final Setting<Boolean> alwaysShow;

    public TimerStopwatchHudElement() {
        super("timer_stopwatch", Direction.Horizontal.size);
        this.style.set(com.dwarslooper.cactus.client.gui.hud.element.HudElement.Style.Default);
        var sgGeneral = this.settings.buildGroup("general");
        this.alwaysShow = sgGeneral.add(new BooleanSetting("alwaysShow", false));

    }

    @Override
    public void renderContent(GuiGraphics context, int x, int y, int width, int height, int screenWidth, int screenHeight, float delta, boolean inEditor) {
        TimerModule timer = TimerModule.INSTANCE;
        StopwatchModule stopwatch = StopwatchModule.INSTANCE;
        // Helper class for colored lines
        class ColoredLine {
            String text;
            int color;
            ColoredLine(String text, int color) { this.text = text; this.color = color; }
        }
        boolean timerEnabled = timer != null && timer.active();
        boolean stopwatchEnabled = stopwatch != null && stopwatch.active();
        boolean showHud = false;
        List<ColoredLine> lines = new ArrayList<>();
        int labelColor = 0xFFFFD700; // gold/yellow
        int timeColor = 0xFF55FF55; // green
        int finishedColor = 0xFFFF5555; // red
        int lapLabelColor = 0xFF00BFFF; // blue
        int lapTimeColor = 0xFF55FFFF; // cyan
        int white = 0xFFFFFFFF;

        if (inEditor) {
            lines.add(new ColoredLine("Timer: Stopped", labelColor));
            lines.add(new ColoredLine("00h 00m 00s", timeColor));
            lines.add(new ColoredLine("Stopwatch: Stopped", lapLabelColor));
            lines.add(new ColoredLine("00h 00m 00s", lapTimeColor));
            showHud = true;
        } else if (alwaysShow.get()) {
            // TIMER
            if (timerEnabled) {
                String status;
                String timeStr;
                if (timer.isRunning()) {
                    status = "Running";
                    timeStr = timer.formatTime(timer.getRemainingMillis());
                } else if (timer.getState() == TimerModule.TimerState.PAUSED) {
                    status = "Paused";
                    timeStr = timer.formatTime(timer.getRemainingMillis());
                } else if (timer.isFinished()) {
                    status = "Stopped";
                    timeStr = timer.formatTime(0);
                } else {
                    status = "Stopped";
                    timeStr = timer.formatTime(timer.getTargetMillis());
                }
                lines.add(new ColoredLine("Timer: " + status, labelColor));
                lines.add(new ColoredLine(timeStr, timeColor));
                if (timer.isFinished()) {
                    lines.add(new ColoredLine("Finished", finishedColor));
                }
            }
            // STOPWATCH
            if (stopwatchEnabled) {
                String status;
                String timeStr;
                if (stopwatch.isRunning()) {
                    status = "Running";
                    timeStr = stopwatch.formatTime(stopwatch.getElapsedMillis());
                } else if (stopwatch.getState().toString().equals("PAUSED")) {
                    status = "Paused";
                    timeStr = stopwatch.formatTime(stopwatch.getElapsedMillis());
                } else {
                    status = "Stopped";
                    timeStr = stopwatch.formatTime(0);
                }
                lines.add(new ColoredLine("Stopwatch: " + status, lapLabelColor));
                lines.add(new ColoredLine(timeStr, lapTimeColor));
                if (stopwatch.getLaps() != null && !stopwatch.getLaps().isEmpty()) {
                    lines.add(new ColoredLine("Last Lap: " + stopwatch.formatTime(stopwatch.getLastLapMillis()), white));
                }
            }
            showHud = timerEnabled || stopwatchEnabled;
        } else {
            // TIMER
            if (timerEnabled && (timer.isRunning() || timer.isFinished() || timer.getState() == TimerModule.TimerState.PAUSED)) {
                String status;
                if (timer.isFinished()) {
                    status = "Stopped";
                } else if (timer.getState() == TimerModule.TimerState.PAUSED) {
                    status = "Paused";
                } else if (timer.isRunning()) {
                    status = "Running";
                } else {
                    status = "Stopped";
                }
                lines.add(new ColoredLine("Timer: " + status, labelColor));
                lines.add(new ColoredLine(timer.formatTime(timer.getRemainingMillis()), timeColor));
                if (timer.isFinished()) {
                    lines.add(new ColoredLine("Finished", finishedColor));
                }
                showHud = true;
            }
            // STOPWATCH
            if (stopwatchEnabled && (stopwatch.isRunning() || stopwatch.getState().toString().equals("PAUSED"))) {
                String status;
                if (stopwatch.getState().toString().equals("PAUSED")) {
                    status = "Paused";
                } else if (stopwatch.isRunning()) {
                    status = "Running";
                } else {
                    status = "Stopped";
                }
                lines.add(new ColoredLine("Stopwatch: " + status, lapLabelColor));
                lines.add(new ColoredLine(stopwatch.formatTime(stopwatch.getElapsedMillis()), lapTimeColor));
                if (stopwatch.getLaps() != null && !stopwatch.getLaps().isEmpty()) {
                    lines.add(new ColoredLine("Last Lap: " + stopwatch.formatTime(stopwatch.getLastLapMillis()), white));
                }
                showHud = true;
            }
        }
        if (timer != null) timer.elementHUD = showHud;
        if (stopwatch != null) stopwatch.elementHUD = showHud;
        if (!showHud || lines.isEmpty()) return;

        int bgPadX = 4;
        int bgPadY = 3;
        int lineHeight = 11;
        int maxTextWidth = 0;
        for (ColoredLine line : lines) {
            int w = Minecraft.getInstance().font.width(line.text);
            if (w > maxTextWidth) maxTextWidth = w;
        }
        int bgWidth = maxTextWidth + bgPadX * 2;
        int bgHeight = lines.size() * lineHeight + bgPadY * 2;
        // Dynamically set the HUD element size to match the background size
        this.resize(bgWidth, bgHeight);
        int bgX = x + width - bgWidth - 3;
        int bgY = y + height - bgHeight - 2;
        int textX = bgX + bgPadX + 2;
        int textY = bgY + bgPadY + 3; // was +2, now +3 for 1px further down

        // Background rendering disabled as requested
        // context.blitSprite(
        //     net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
        //     com.dwarslooper.cactus.client.gui.hud.element.HudElement.BACKGROUND,
        //     bgX, bgY, bgWidth, bgHeight,
        //     net.minecraft.util.ARGB.color(224, 0, 0, 0)
        // );
        for (ColoredLine line : lines) {
            context.drawString(Minecraft.getInstance().font, line.text, textX, textY, line.color, false);
            textY += lineHeight;
        }
    }

    @Override
    public TimerStopwatchHudElement duplicate() {
        return new TimerStopwatchHudElement();
    }

    public int getDefaultWidth() {
        return 180;
    }

    public int getDefaultHeight() {
        // Use 11px per line, matching renderContent logic
        TimerModule timer = TimerModule.INSTANCE;
        StopwatchModule stopwatch = StopwatchModule.INSTANCE;
        boolean timerActive = timer != null && timer.active();
        boolean stopwatchActive = stopwatch != null && stopwatch.active();
        int n = 0;
        if (timerActive && (timer.isRunning() || timer.isFinished() || timer.getState() == TimerModule.TimerState.PAUSED)) {
            n += 2; // Timer label + time
            if (timer.isFinished() || timer.getState() == TimerModule.TimerState.PAUSED) n++;
        }
        if (stopwatchActive && (stopwatch.isRunning() || stopwatch.getState().toString().equals("PAUSED"))) {
            n += 2; // Stopwatch label + time
            if (stopwatch.getLaps() != null && !stopwatch.getLaps().isEmpty()) n++;
            if (stopwatch.getState().toString().equals("PAUSED")) n++;
        }
        if (n == 0) n = 6; // Editor mode: 3 timer + 3 stopwatch lines
        return n * 11 + 6; // bgPadY * 2 = 6
    }

    @Override
    public String getName() {
        return "Timer & Stopwatch";
    }
}
