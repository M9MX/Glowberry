    package org.m9mx.cactus.glowberry.feature.modules;

    import org.lwjgl.glfw.GLFW;
    import org.m9mx.cactus.glowberry.util.ActionBarUtil;

    import com.dwarslooper.cactus.client.event.EventHandler;
    import com.dwarslooper.cactus.client.event.impl.ClientTickEvent;
    import com.dwarslooper.cactus.client.feature.module.Category;
    import com.dwarslooper.cactus.client.feature.module.Module;
    import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
    import com.dwarslooper.cactus.client.systems.config.settings.impl.KeybindSetting;
    import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
    import com.dwarslooper.cactus.client.systems.key.KeyBind;

    import net.minecraft.client.Minecraft;

    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.List;

    @SuppressWarnings("unused")
    public class StopwatchModule extends Module {
        public static volatile StopwatchModule INSTANCE;

        public final Setting<Boolean> showMilliseconds;
        public final Setting<KeyBind> startKeybind;
        public final Setting<KeyBind> pauseKeybind;
        public final Setting<KeyBind> resetKeybind;
        public final Setting<KeyBind> lapKeybind;

        public boolean elementHUD = false;

        private long startTimeMillis = 0;
        private long accumulatedMillis = 0;
        private boolean running = false;

        private final List<Long> laps = new ArrayList<>();

        private boolean lastStartState = false;
        private boolean lastPauseState = false;
        private boolean lastResetState = false;
        private boolean lastLapState = false;

        // tje stopwatchstate enum which is IDLE or RUNNING or PAUSED
        // obviously you peepeepoopoo head
        public enum StopwatchState {
            IDLE, RUNNING, PAUSED
        }

        private StopwatchState state = StopwatchState.IDLE;

        public StopwatchModule(Category category) {
            super("stopwatch", category, new Module.Options());
            INSTANCE = this;

            var sgGeneral = this.settings.buildGroup("general");

            this.showMilliseconds = sgGeneral.add(new BooleanSetting("showMilliseconds", true));
            this.startKeybind = sgGeneral.add(new KeybindSetting("startStopwatch", KeyBind.of(GLFW.GLFW_KEY_UNKNOWN)));
            this.pauseKeybind = sgGeneral.add(new KeybindSetting("pauseStopwatch", KeyBind.of(GLFW.GLFW_KEY_UNKNOWN)));
            this.resetKeybind = sgGeneral.add(new KeybindSetting("resetStopwatch", KeyBind.of(GLFW.GLFW_KEY_UNKNOWN)));
            this.lapKeybind = sgGeneral.add(new KeybindSetting("recordLap", KeyBind.of(GLFW.GLFW_KEY_UNKNOWN)));
        }

        @Override
        public void onEnable() {
            resetStopwatch();
        }

        @Override
        public void onDisable() {
            resetStopwatch();
        }

        @EventHandler
        public void onTick(ClientTickEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;

            boolean currentStart = isKeyPressed(startKeybind);
            boolean currentPause = isKeyPressed(pauseKeybind);
            boolean currentReset = isKeyPressed(resetKeybind);
            boolean currentLap = isKeyPressed(lapKeybind);

            if (currentStart && !lastStartState) handleStart();
            if (currentPause && !lastPauseState) handlePause(); // very neat sht
            if (currentReset && !lastResetState) resetStopwatch();
            if (currentLap && !lastLapState) recordLap();

            lastStartState = currentStart;
            lastPauseState = currentPause; lastResetState = currentReset; // fuck you, take rnadom smae line
            lastLapState = currentLap;

            if (!elementHUD) {
                ActionBarUtil.sendActionBarMessage(buildActionBarText());
            }
        }

        private String buildActionBarText() {
            long elapsed = getElapsedMillis();

            StringBuilder sb = new StringBuilder();
            sb.append("§bStopwatch§7: ");
            sb.append(formatTime(elapsed));

            if (!laps.isEmpty()) {
                sb.append(" §7| §fLast Lap§7: ");
                sb.append(formatTime(getLastLapMillis()));
            }

            if (state == StopwatchState.PAUSED) {
                sb.append(" §7| §cPaused");
            }

            return sb.toString();
        }

        private String formatTime(long millis) {
            long totalSecs = millis / 1000;
            long h = totalSecs / 3600;
            long m = (totalSecs % 3600) / 60;
            long s = totalSecs % 60;
            long ms = millis % 1000;

            StringBuilder sb = new StringBuilder();

            if (h >= 1) {
                sb.append("§e").append(h).append("§7h");
            }

            sb.append("§e").append(String.format("%02d", m)).append("§7m");
            sb.append("§e").append(String.format("%02d", s)).append("§7s");

            if (showMilliseconds.get()) {
                sb.append("§7.§e").append(String.format("%03d", ms)).append("§7ms");
            }

            return sb.toString();
        }

        private void handleStart() {
            if (state == StopwatchState.IDLE || state == StopwatchState.PAUSED) {
                startTimeMillis = System.currentTimeMillis();
                state = StopwatchState.RUNNING;
                running = true;
            }
        }

        private void handlePause() {
            if (state != StopwatchState.RUNNING) return;
            accumulatedMillis += System.currentTimeMillis() - startTimeMillis;
            state = StopwatchState.PAUSED;
            running = false;
        }

        private void recordLap() {
            if (state == StopwatchState.RUNNING || state == StopwatchState.PAUSED) {
                laps.add(getElapsedMillis());
            }
        }

        private void resetStopwatch() {
            state = StopwatchState.IDLE;
            running = false;
            startTimeMillis = 0;
            accumulatedMillis = 0;
            laps.clear();
            lastStartState = false;
            lastPauseState = false;
            lastResetState = false;
            lastLapState = false;
        }

        private boolean isKeyPressed(Setting<KeyBind> setting) {
            KeyBind kb = setting.get();
            return kb != null && kb.isPressed();
        }

        public long getElapsedMillis() {
            if (state == StopwatchState.RUNNING) {
                return accumulatedMillis + (System.currentTimeMillis() - startTimeMillis);
            }
            return accumulatedMillis;
        }

        // UN. MODIFIABLE. list. of all recorded lap timestamps in ms
        public List<Long> getLaps() {
            return Collections.unmodifiableList(laps);
        }

        public long getLastLapMillis() {
            if (laps.isEmpty()) return 0;
            return laps.getLast();
        }

        public StopwatchState getState() {
            return state;
        }

        public boolean isRunning() {
            return running;
        }
    }