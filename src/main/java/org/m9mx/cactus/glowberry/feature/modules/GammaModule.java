package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.event.EventHandler;
import com.dwarslooper.cactus.client.event.impl.ClientTickEvent;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.KeybindSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.systems.key.KeyBind;

import org.lwjgl.glfw.GLFW;
import org.m9mx.cactus.glowberry.cactus.FloatSetting;
import org.m9mx.cactus.glowberry.util.ModuleMessageUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Gamma (FullBright) module.
 *
 * <p>While enabled, the vanilla brightness option is driven to a configurable
 * gamma percentage (100% = vanilla maximum brightness, up to 1500%). Instead of
 * poking {@code options.gamma()} every tick, the module hijacks the gamma
 * {@link net.minecraft.client.OptionInstance} itself (see
 * {@code GammaOptionInstanceMixin}): every vanilla read of the brightness
 * option - the lightmap, the brightness slider, anything - gets the module's
 * own unclamped value, and every write (e.g. dragging the vanilla brightness
 * slider) is routed back into the module. The whole pipeline stays in sync and
 * values above 100% go beyond what the vanilla slider allows, making nights
 * and caves look fully lit.
 *
 * <p>While the module is active the gamma value is also kept out of
 * {@code options.txt} (see {@code GammaOptionsMixin}), so the boosted value can
 * never leak into the player's own brightness setting. When the module is
 * toggled off the brightness the player had before enabling is restored, and it
 * is also restored when the game closes while the module is still on (see
 * {@link #restoreGammaOnShutdown()}).
 *
 * <p>Extra features: in-game increase/decrease keys that adjust the gamma
 * percentage by a configurable step, an optional smooth transition between
 * gamma values, and an optional client-side night vision effect (a real
 * {@link MobEffects#NIGHT_VISION} instance on the local player, which works
 * with shaders unlike gamma). While night vision is in effect - the module's
 * own setting or a real night vision potion - the gamma boost is withheld and
 * the player's own brightness is used instead; the configured percentage is
 * kept, so gamma resumes where it was when the night vision ends.
 *
 * <p>The OptionInstance hijack approach is inspired by Gamma Utils
 * (https://modrinth.com/mod/gamma-utils, LGPL-3.0). All code here is written
 * from scratch for Glowberry; no code was copied.
 */
public class GammaModule extends Module {
	public static volatile GammaModule INSTANCE;

	private final SettingGroup generalGroup;
	/** Brightness while enabled, in percent of the vanilla maximum (100-1500). */
	public final Setting<Integer> gammaPercentage;
	/** How many percent each in-game increase/decrease key changes the gamma by. */
	public final Setting<Integer> stepPercentage;
	/** Apply a client-side night vision effect while enabled. */
	public final Setting<Boolean> nightVision;
	/** Smoothly animate the gamma value towards its target instead of snapping. */
	public final Setting<Boolean> smoothTransition;
	/** How fast the smooth transition moves, in gamma percentage per second. */
	public final Setting<Float> transitionSpeed;
	public final Setting<KeyBind> increaseKeybind;
	public final Setting<KeyBind> decreaseKeybind;

	/**
	 * The live gamma value handed out to the vanilla pipeline while active.
	 * Volatile: written by the transition timer thread, read every frame by the
	 * lightmap through {@code GammaOptionInstanceMixin}.
	 */
	private volatile double currentGamma = 1.0;
	/** The player's own brightness, captured on enable so it can be restored. */
	private double previousGamma = 1.0;
	/** Whether the gamma option reads/writes are currently routed through us. */
	private volatile boolean diverting = false;
	/** Whether we added the night vision effect ourselves (so we don't remove a real potion). */
	private boolean nightVisionAddedByModule = false;

	private Timer transitionTimer = null;

	private boolean lastIncrease = false;
	private boolean lastDecrease = false;
	private int increaseHoldTicks = 0;
	private int decreaseHoldTicks = 0;
	private int increaseStepDelay = 0;
	private int decreaseStepDelay = 0;

	public GammaModule(Category category) {
		super("gamma", category, new Module.Options());
		INSTANCE = this;

		this.generalGroup = this.settings.buildGroup("general");
		this.gammaPercentage = this.generalGroup.add(
				new IntegerSetting("gammaPercentage", 1000).min(100).max(1500));
		this.stepPercentage = this.generalGroup.add(
				new IntegerSetting("stepPercentage", 10).min(1).max(100));
		this.nightVision = this.generalGroup.add(new BooleanSetting("nightVision", false));
		this.smoothTransition = this.generalGroup.add(new BooleanSetting("smoothTransition", false));
		this.transitionSpeed = this.generalGroup.add(
				new FloatSetting("transitionSpeed", 4500).min(100).max(10000).decimals(0));
		this.increaseKeybind = this.generalGroup.add(new KeybindSetting("increaseKeybind", KeyBind.of(GLFW.GLFW_KEY_UP)));
		this.decreaseKeybind = this.generalGroup.add(new KeybindSetting("decreaseKeybind", KeyBind.of(GLFW.GLFW_KEY_DOWN)));
	}

	@Override
	public void onEnable() {
		Minecraft mc = Minecraft.getInstance();
		cancelTransition();
		// Capture the player's own brightness BEFORE diverting, so this reads the
		// real vanilla value and not our own.
		previousGamma = mc.options.gamma().get();
		// Start from the player's current brightness and move to the target, so
		// enabling (and disabling) fades smoothly when the transition is on.
		currentGamma = previousGamma;
		diverting = true;
		setGammaTarget(gammaPercentage.get() / 100.0, true);
		syncNightVision();
	}

	@Override
	public void onDisable() {
		Minecraft mc = Minecraft.getInstance();
		cancelTransition();
		// Stop diverting BEFORE touching the option, so this write goes to the
		// real OptionInstance instead of being routed back into us.
		diverting = false;
		currentGamma = previousGamma;
		mc.options.gamma().set(previousGamma);
		// Remove the client-side night vision effect, but only if we added it.
		if (mc.player != null && nightVisionAddedByModule && mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
			mc.player.removeEffect(MobEffects.NIGHT_VISION);
		}
		nightVisionAddedByModule = false;
	}

	@EventHandler
	public void onTick(ClientTickEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;

		// In-game increase/decrease keys: apply a step on press, then auto-repeat
		// while held - after a short initial delay, repeating faster the longer
		// the key is held (acceleration). IntegerSetting clamps to [100, 1500].
		boolean inc = isKeyPressed(mc, increaseKeybind.get());
		boolean dec = isKeyPressed(mc, decreaseKeybind.get());

		if (inc) {
			if (!lastIncrease) {
				lastIncrease = true;
				increaseHoldTicks = 0;
				increaseStepDelay = INITIAL_REPEAT_DELAY;
				applyGammaStep(1);
			} else {
				increaseHoldTicks++;
				increaseStepDelay--;
				if (increaseStepDelay <= 0) {
					applyGammaStep(1);
					increaseStepDelay = repeatDelay(increaseHoldTicks);
				}
			}
		} else if (lastIncrease) {
			lastIncrease = false;
			increaseHoldTicks = 0;
			increaseStepDelay = 0;
		}

		if (dec) {
			if (!lastDecrease) {
				lastDecrease = true;
				decreaseHoldTicks = 0;
				decreaseStepDelay = INITIAL_REPEAT_DELAY;
				applyGammaStep(-1);
			} else {
				decreaseHoldTicks++;
				decreaseStepDelay--;
				if (decreaseStepDelay <= 0) {
					applyGammaStep(-1);
					decreaseStepDelay = repeatDelay(decreaseHoldTicks);
				}
			}
		} else if (lastDecrease) {
			lastDecrease = false;
			decreaseHoldTicks = 0;
			decreaseStepDelay = 0;
		}

		// Pick up external setting changes (e.g. the Cactus settings slider)
		// while the module is on, unless a transition is already animating.
		double target = gammaPercentage.get() / 100.0;
		if (transitionTimer == null && Math.abs(currentGamma - target) > 0.0001) {
			currentGamma = target;
		}

		syncNightVision();
	}

	/**
	 * Applies one adjustment step (increase or decrease) and announces the new
	 * percentage through the Module Message element (action bar if not placed).
	 */
	private void applyGammaStep(int direction) {
		gammaPercentage.set(gammaPercentage.get() + direction * stepPercentage.get());
		setGammaTarget(gammaPercentage.get() / 100.0, true);
		ModuleMessageUtil.show(Component.literal("§eGamma §f" + gammaPercentage.get() + "%"), 0xFFFFFFFF);
	}

	/**
	 * Ticks between auto-repeat steps while a key is held: the longer the key is
	 * held, the faster it repeats (acceleration), down to one step per tick.
	 */
	private int repeatDelay(int holdTicks) {
		return Math.max(1, BASE_REPEAT_DELAY - holdTicks / ACCELERATION_TICKS);
	}

	/**
	 * Move the live gamma value towards {@code target}. Snaps instantly unless
	 * the smooth transition is enabled.
	 */
	private void setGammaTarget(double target, boolean smooth) {
		cancelTransition();
		if (smooth && smoothTransition.get()) {
			// transitionSpeed is in gamma percentage per second -> gamma per second.
			// Signed per-tick step must be final so the TimerTask can capture it.
			double speed = (transitionSpeed.get() / 100.0) * (TRANSITION_INTERVAL_MS / 1000.0);
			final double stepPerTick = target < currentGamma ? -speed : speed;
			transitionTimer = new Timer("Glowberry Gamma Transition");
			transitionTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					double next = currentGamma + stepPerTick;
					if ((stepPerTick > 0 && next >= target) || (stepPerTick < 0 && next <= target)) {
						currentGamma = target;
						transitionTimer.cancel();
						transitionTimer = null;
					} else {
						currentGamma = next;
					}
				}
			}, 0, TRANSITION_INTERVAL_MS);
		} else {
			currentGamma = target;
		}
	}

	private void cancelTransition() {
		if (transitionTimer != null) {
			transitionTimer.cancel();
			transitionTimer = null;
		}
	}

	/** Apply or remove the client-side night vision effect to match the setting. */
	private void syncNightVision() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;

		boolean want = nightVision.get();
		boolean has = mc.player.hasEffect(MobEffects.NIGHT_VISION);

		if (want && !has) {
			// No particles, no HUD icon - just the light boost. The client-side
			// instance is invisible to the server.
			mc.player.addEffect(new MobEffectInstance(
					MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, false), null);
			nightVisionAddedByModule = true;
		} else if (!want && has && nightVisionAddedByModule) {
			mc.player.removeEffect(MobEffects.NIGHT_VISION);
			nightVisionAddedByModule = false;
		}
	}

	private boolean isKeyPressed(Minecraft mc, Object keybind) {
		if (mc.gui.screen != null) return false;
		if (keybind instanceof KeyBind) {
			return ((KeyBind) keybind).isPressed();
		}
		return false;
	}

	/**
	 * Whether the gamma boost is currently withheld because night vision is in
	 * effect - either the module's own night vision setting or a real night
	 * vision potion. The configured gamma percentage is kept untouched, so the
	 * boost simply resumes when the night vision ends.
	 */
	private boolean nightVisionBlocking() {
		if (nightVision.get()) return true;
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.player.hasEffect(MobEffects.NIGHT_VISION);
	}

	// ---- Static API used by GammaOptionInstanceMixin ----

	/**
	 * Whether the vanilla gamma option should currently be routed through us.
	 * False while night vision is in effect, so the lightmap reads the player's
	 * own brightness instead of the boosted gamma value.
	 */
	public static boolean isDiverting() {
		return INSTANCE != null && INSTANCE.diverting && !INSTANCE.nightVisionBlocking();
	}

	/** The live gamma value to hand out instead of the vanilla option's value. */
	public static double getCurrentGamma() {
		return INSTANCE == null ? 1.0 : INSTANCE.currentGamma;
	}

	/**
	 * Called when something writes to the vanilla brightness option while the
	 * module is active (e.g. the brightness slider being dragged). The write is
	 * consumed here instead of reaching the vanilla OptionInstance.
	 */
	public static void onGammaOptionSet(double value) {
		GammaModule module = INSTANCE;
		if (module == null) return;
		module.cancelTransition();
		module.currentGamma = value;
		module.gammaPercentage.set((int) Math.round(value * 100));
	}

	/**
	 * Restores the player's own brightness, called when the game closes while
	 * the module is still on (the module cannot run its onDisable on shutdown).
	 * Registered on {@code ClientLifecycleEvents.CLIENT_STOPPING} so it runs
	 * before the options are saved to disk.
	 */
	public static void restoreGammaOnShutdown() {
		GammaModule module = INSTANCE;
		if (module == null) return;
		try {
			module.cancelTransition();
			module.diverting = false;
			module.currentGamma = module.previousGamma;
			Minecraft.getInstance().options.gamma().set(module.previousGamma);
		} catch (Exception ignored) {
			// The client is shutting down; nothing we can do about it.
		}
	}

	private static final long TRANSITION_INTERVAL_MS = 10;
	/** Ticks before the first auto-repeat step when an adjustment key is held. */
	private static final int INITIAL_REPEAT_DELAY = 6;
	/** Base ticks between auto-repeat steps while holding an adjustment key. */
	private static final int BASE_REPEAT_DELAY = 5;
	/** Held ticks it takes to shave one tick off the repeat delay (acceleration). */
	private static final int ACCELERATION_TICKS = 12;
}
