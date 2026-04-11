package org.m9mx.cactus.glowberry.feature.modules;

import org.lwjgl.glfw.GLFW;
import org.m9mx.cactus.glowberry.util.ActionBarUtil;

import com.dwarslooper.cactus.client.event.EventHandler;
import com.dwarslooper.cactus.client.event.impl.ClientTickEvent;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.KeybindSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.systems.key.KeyBind;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

@SuppressWarnings("unused")
public class TimerModule extends Module {
    public static volatile TimerModule INSTANCE;

    public final Setting<Integer> hours;
    public final Setting<Integer> minutes;
    public final Setting<Integer> seconds;
    public final Setting<Boolean> showMilliseconds;
    public final Setting<KeyBind> startKeybind;
    public final Setting<KeyBind> pauseKeybind;
    public final Setting<KeyBind> resetKeybind;

    // set true if hud element is enabled
    public boolean elementHUD = false;

    private long remainingMillisAtPause = 0;
    private long startTimeMillis = 0;
    private long targetMillis = 0;

    private boolean running = false;
    private boolean finished = false;

    private boolean lastStartState = false;
    private boolean lastPauseState = false;
    private boolean lastResetState = false;

    public enum TimerState {
        IDLE, RUNNING, PAUSED, FINISHED
    }

    private TimerState state = TimerState.IDLE;

    public TimerModule(Category category) {
        super("timer", category, new Module.Options());
        INSTANCE = this;

        var sgTimer = this.settings.buildGroup("timer");
        var sgGeneral = this.settings.buildGroup("general");

        this.hours = sgTimer.add(new IntegerSetting("hours", 0).min(0).max(23));
        this.minutes = sgTimer.add(new IntegerSetting("minutes", 0).min(0).max(59));
        this.seconds = sgTimer.add(new IntegerSetting("seconds", 30).min(0).max(59));

        this.showMilliseconds = sgGeneral.add(new BooleanSetting("showMilliseconds", true));
        this.startKeybind = sgGeneral.add(new KeybindSetting("startTimer", KeyBind.of(GLFW.GLFW_KEY_UNKNOWN)));
        this.pauseKeybind = sgGeneral.add(new KeybindSetting("pauseTimer", KeyBind.of(GLFW.GLFW_KEY_UNKNOWN)));
        this.resetKeybind = sgGeneral.add(new KeybindSetting("resetTimer", KeyBind.of(GLFW.GLFW_KEY_UNKNOWN)));
    }

    @Override
    public void onEnable() {
        resetTimer();
    }

    @Override
    public void onDisable() {
        resetTimer();
    }

    @EventHandler
    public void onTick(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        boolean currentStart = isKeyPressed(startKeybind);
        boolean currentPause = isKeyPressed(pauseKeybind);
        boolean currentReset = isKeyPressed(resetKeybind);

        if (currentStart && !lastStartState) handleStart();
        if (currentPause && !lastPauseState) handlePause();
        if (currentReset && !lastResetState) resetTimer();

        lastStartState = currentStart;
        lastPauseState = currentPause;
        lastResetState = currentReset;

        if (state == TimerState.RUNNING) {
            long elapsed = System.currentTimeMillis() - startTimeMillis;
            if (remainingMillisAtPause - elapsed <= 0) {
                state = TimerState.FINISHED;
                running = false;
                finished = true;
                playFinishedSound(mc);
            }
        }

        if (!elementHUD) {
            ActionBarUtil.sendActionBarMessage(buildActionBarText());
        }
    }

    private String buildActionBarText() {
        long rem = getRemainingMillis();

        StringBuilder sb = new StringBuilder();
        sb.append("§eTimer§7: ");
        sb.append(formatTime(rem));

        if (finished) {
            sb.append(" §7| §cFinished");
        }

        return sb.toString();
    }

    private String formatTime(long millis) {
        long totalSecs = millis / 1000;
        long h = totalSecs / 3600;
        long m = (totalSecs % 3600) / 60;
        long s = totalSecs % 60;
        long ms = millis % 1000;

        boolean showHours = (targetMillis / 1000 / 3600) >= 1;

        StringBuilder sb = new StringBuilder();

        if (showHours) {
            sb.append("§a").append(h).append("§7h");
        }

        sb.append("§a").append(String.format("%02d", m)).append("§7m");
        sb.append("§a").append(String.format("%02d", s)).append("§7s");

        if (showMilliseconds.get()) {
            sb.append("§7.§a").append(String.format("%03d", ms)).append("§7ms");
        }

        return sb.toString();
    }

    private void handleStart() {
        if (state == TimerState.IDLE || state == TimerState.FINISHED) {
            long total = ((long) hours.get() * 3600 + (long) minutes.get() * 60 + seconds.get()) * 1000L;
            if (total <= 0) return;
            targetMillis = total;
            remainingMillisAtPause = total;
            startTimeMillis = System.currentTimeMillis();
            state = TimerState.RUNNING;
            running = true;
            finished = false;
        } else if (state == TimerState.PAUSED) {
            startTimeMillis = System.currentTimeMillis();
            state = TimerState.RUNNING;
            running = true;
        }
    }

    private void handlePause() {
        if (state != TimerState.RUNNING) return;
        long elapsed = System.currentTimeMillis() - startTimeMillis;
        remainingMillisAtPause = Math.max(0, remainingMillisAtPause - elapsed);
        state = TimerState.PAUSED;
        running = false;
    }

    private void resetTimer() {
        state = TimerState.IDLE;
        running = false;
        finished = false;
        targetMillis = 0;
        remainingMillisAtPause = 0;
        startTimeMillis = 0;
        lastStartState = false;
        lastPauseState = false;
        lastResetState = false;
    }

    private void playFinishedSound(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        mc.level.playLocalSound(x, y, z, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.MASTER, 1.0f, 1.0f, false);
        mc.level.playLocalSound(x, y, z, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.MASTER, 1.0f, 1.25f, false);
        mc.level.playLocalSound(x, y, z, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.MASTER, 1.0f, 1.5f, false);
    }

    private boolean isKeyPressed(Setting<KeyBind> setting) {
        KeyBind kb = setting.get();
        return kb != null && kb.isPressed();
    }

    public long getRemainingMillis() {
        if (state == TimerState.RUNNING) {
            return Math.max(0, remainingMillisAtPause - (System.currentTimeMillis() - startTimeMillis));
        }
        if (state == TimerState.PAUSED) return remainingMillisAtPause;
        if (state == TimerState.IDLE) return ((long) hours.get() * 3600 + (long) minutes.get() * 60 + seconds.get()) * 1000L;
        return 0;
    }

    public long getTargetMillis() {
        return targetMillis;
    }

    public TimerState getState() {
        return state;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return finished;
    }
}