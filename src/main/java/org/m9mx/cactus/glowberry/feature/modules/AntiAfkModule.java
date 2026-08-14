package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.event.EventHandler;
import com.dwarslooper.cactus.client.event.impl.ClientTickEvent;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.KeybindSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.systems.key.KeyBind;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;

import org.lwjgl.glfw.GLFW;
import org.m9mx.cactus.glowberry.util.ModuleMessageUtil;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Anti AFK - a cheat that keeps you from being kicked for idling by acting
 * like a real (bored) player:
 *
 * <ul>
 *   <li>randomly walks in short bursts in random directions, with occasional jumps,</li>
 *   <li>randomly pans the camera around in smooth sweeps (with a gentle sway
 *       while walking),</li>
 *   <li>finds interactable blocks (levers, buttons, repeaters/comparators, pressure
 *       plates) nearby and smoothly looks at one, then presses it.</li>
 * </ul>
 *
 * Best setup: stand inside a small 3x3x3 box with a few levers/buttons on the
 * walls. The module then has plenty of things to press; it also works without
 * any interactables by just moving and looking around.
 *
 * The module gives up control the moment the player actually touches any
 * movement key, so it never fights you.
 */
public class AntiAfkModule extends Module {
    public static volatile AntiAfkModule INSTANCE;

    public final Setting<Integer> interactionRange;
    public final Setting<KeyBind> toggleKeybind;

    private static final double MAX_CLICK_DISTANCE_SQ = 4.5 * 4.5;
    private static final float FACE_EPSILON = 2.5f;
    private static final long INTERACT_COOLDOWN_MS = 3000;
    // Camera smoothing. The rotation closes a fixed fraction of the remaining
    // angle every tick (no random jitter), so the view sweeps smoothly and
    // never overshoots. INTERACT snaps faster so pressing feels deliberate.
    private static final float LOOK_STEP = 0.14f;
    private static final float INTERACT_STEP = 0.18f;
    private static final float PRECISION_STEP = 0.30f;
    private static final float PRECISION_RANGE = 12f;
    private static final float LOOK_DONE_EPSILON = 1.0f;

    // Slow smooth camera drift while walking (sin-based, direction changes
    // every few seconds) so the view sways naturally instead of twitching.
    private static final float WALK_DRIFT_AMPLITUDE = 4f;
    private static final float WALK_DRIFT_SPEED = 0.01f;
    private float walkDriftPhase = 0f;
    private float walkDriftDirection = 1f;

    private enum State { IDLE, LOOK_AROUND, WALK, INTERACT }

    private State state = State.IDLE;
    private int stateTicksLeft = 0;

    // Camera target while interacting / looking around
    private BlockPos targetBlock = null;
    private float lookYaw = 0f;
    private float lookPitch = 0f;
    private boolean hasLookTarget = false;

    // Simulated movement for the current tick (consumed by ClientInputMixin)
    private Input simulatedInput = null;
    private boolean userInteracting = false;

    // Walk direction - picked once when the WALK state starts and held for the
    // whole burst so the player walks steadily instead of flickering.
    private boolean walkForward = false;
    private boolean walkBackward = false;
    private boolean walkLeft = false;
    private boolean walkRight = false;

    // Interaction cooldown so the lever isn't spammed
    private long lastInteractTime = 0;

    // Toggle key edge detection
    private boolean lastKeyState = false;

    // In-memory switch for the anti-AFK behavior itself. The module stays
    // enabled in the module list - the keybind toggles THIS flag instead, so
    // disabling anti-AFK never turns the module off. Starts off; press the
    // keybind to turn it on.
    private boolean antiAfkEnabled = false;

    private final Random random = new Random();

    public AntiAfkModule(Category category) {
        super("antiAfk", category, new Module.Options().set(Flag.SERVER_UNSAFE, true));
        INSTANCE = this;

        SettingGroup general = this.settings.buildGroup("general");
        this.interactionRange = general.add(new IntegerSetting("interactionRange", 6).min(2).max(16));
        this.toggleKeybind = general.add(new KeybindSetting("toggleKeybind", KeyBind.of(GLFW.GLFW_KEY_F8)));
    }

    @Override
    public void onEnable() {
        state = State.IDLE;
        stateTicksLeft = ticksBetween(20, 50);
        targetBlock = null;
        hasLookTarget = false;
        simulatedInput = null;
        userInteracting = false;
    }

    @Override
    public void onDisable() {
        // Module turned off (e.g. cheats pack toggled) - stop everything.
        antiAfkEnabled = false;
        simulatedInput = null;
        lastKeyState = false;
    }

    @EventHandler
    public void onTick(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.gui.screen() != null) {
            simulatedInput = null;
            return;
        }

        // Toggle key (edge detected) - press once to enable, again to disable.
        // This toggles the in-memory flag, NOT the module itself.
        boolean keyDown = isToggleKeyPressed();
        if (keyDown && !lastKeyState) {
            lastKeyState = true;
            antiAfkEnabled = !antiAfkEnabled;
            int color = antiAfkEnabled ? 0xFF55FF55 : 0xFFFF5555;
            ModuleMessageUtil.show(Component.literal("Anti AFK " + (antiAfkEnabled ? "§aEnabled" : "§cDisabled")), color);
            if (!antiAfkEnabled) {
                simulatedInput = null;
                targetBlock = null;
                hasLookTarget = false;
            }
            return;
        } else if (!keyDown && lastKeyState) {
            lastKeyState = false;
        }

        // Nothing to do while the anti-AFK behavior is switched off
        if (!antiAfkEnabled) return;

        // Give up control whenever the real player is doing something
        userInteracting = mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()
                || mc.options.keyJump.isDown() || mc.options.keyShift.isDown()
                || mc.options.keySprint.isDown();
        if (userInteracting) {
            simulatedInput = null;
            targetBlock = null;
            hasLookTarget = false;
            return;
        }

        tickStateMachine(mc);
        tickCamera(mc);
    }

    // ------------------------------------------------------------------
    // State machine
    // ------------------------------------------------------------------

    private void tickStateMachine(Minecraft mc) {
        if (stateTicksLeft > 0) {
            stateTicksLeft--;
            // Still aiming at a block? finish the interaction when lined up
            if (state == State.INTERACT && targetBlock != null) {
                tryInteract(mc);
            }
            return;
        }

        // LOOK_AROUND ran out of time mid-sweep - keep looking until the view
        // actually reaches the target so the camera never stops halfway.
        if (state == State.LOOK_AROUND && hasLookTarget && !isCameraNear(lookYaw, lookPitch, LOOK_DONE_EPSILON)) {
            stateTicksLeft = ticksBetween(30, 60);
            return;
        }

        // Time to pick a new random behavior
        int roll = random.nextInt(100);
        if (roll < 25) {
            // IDLE - a short pause where almost nothing happens
            enter(State.IDLE, ticksBetween(20, 60));
            hasLookTarget = false;
        } else if (roll < 60) {
            // LOOK_AROUND - wander the camera
            enter(State.LOOK_AROUND, ticksBetween(40, 120));
            pickRandomLookTarget(mc);
        } else if (roll < 85) {
            // WALK - move in a random direction
            enter(State.WALK, ticksBetween(20, 80));
            int dir = random.nextInt(4);
            walkForward = dir == 0;
            walkBackward = dir == 1;
            walkLeft = dir == 2;
            walkRight = dir == 3;
            hasLookTarget = false;
        } else {
            // INTERACT - find a lever/button and press it
            BlockPos target = findInteractable(mc);
            if (target == null) {
                // Nothing to press here - just look around instead
                enter(State.LOOK_AROUND, ticksBetween(40, 80));
                pickRandomLookTarget(mc);
                return;
            }
            enter(State.INTERACT, ticksBetween(60, 140));
            targetBlock = target;
            hasLookTarget = true;
            updateLookTarget(mc, target);
        }
    }

    private void enter(State newState, int ticks) {
        state = newState;
        stateTicksLeft = ticks;
        if (newState != State.INTERACT) {
            targetBlock = null;
        }
    }

    private void pickRandomLookTarget(Minecraft mc) {
        // Pick a random yaw around the horizon with a small pitch range
        float yaw = mc.player.getYRot() + (random.nextFloat() * 220f - 110f);
        float pitch = (random.nextFloat() * 30f - 15f);
        lookYaw = yaw;
        lookPitch = pitch;
        hasLookTarget = true;
    }

    // ------------------------------------------------------------------
    // Movement input (consumed by ClientInputMixin each tick)
    // ------------------------------------------------------------------

    private void tickCamera(Minecraft mc) {
        if (state == State.INTERACT && targetBlock != null && hasLookTarget) {
            // Smoothly aim at the block - faster once close so the press lands
            // quickly, with no random jitter around the target.
            float[] target = lookAnglesTo(mc, targetBlock);
            float yawDiff = Math.abs(shortestAngleDiff(mc.player.getYRot(), target[0]));
            float pitchDiff = Math.abs(shortestAngleDiff(mc.player.getXRot(), target[1]));
            float step = (yawDiff < PRECISION_RANGE && pitchDiff < PRECISION_RANGE) ? PRECISION_STEP : INTERACT_STEP;
            mc.player.setYRot(stepAngle(mc.player.getYRot(), target[0], step));
            mc.player.setXRot(stepAngle(mc.player.getXRot(), target[1], step));
        } else if (state == State.LOOK_AROUND && hasLookTarget) {
            // Sweep smoothly toward the chosen random look target. The step is
            // proportional to the remaining angle, so it visibly moves every
            // tick, slows down as it arrives, and never overshoots.
            mc.player.setYRot(stepAngle(mc.player.getYRot(), lookYaw, LOOK_STEP));
            mc.player.setXRot(stepAngle(mc.player.getXRot(), lookPitch, LOOK_STEP));
        } else if (state == State.WALK) {
            // Gentle slow sway while walking so the view moves naturally with
            // the motion instead of sitting frozen or twitching randomly.
            walkDriftPhase += WALK_DRIFT_SPEED;
            if (walkDriftPhase > (float) Math.PI * 2f) {
                walkDriftPhase = 0f;
                walkDriftDirection = random.nextBoolean() ? 1f : -1f;
            }
            float sway = (float) Math.sin(walkDriftPhase) * WALK_DRIFT_AMPLITUDE * walkDriftDirection;
            mc.player.setYRot(mc.player.getYRot() + sway);
        }
        // IDLE: the camera holds perfectly still - a real idle player's view
        // doesn't twitch, and a frozen view looks much more natural than jitter.
    }

    private void tryInteract(Minecraft mc) {
        float[] target = lookAnglesTo(mc, targetBlock);
        float yawDiff = shortestAngleDiff(mc.player.getYRot(), target[0]);
        float pitchDiff = shortestAngleDiff(mc.player.getXRot(), target[1]);
        double distSq = mc.player.distanceToSqr(targetBlock.getX() + 0.5, targetBlock.getY() + 0.5, targetBlock.getZ() + 0.5);

        if (Math.abs(yawDiff) < FACE_EPSILON && Math.abs(pitchDiff) < FACE_EPSILON && distSq < MAX_CLICK_DISTANCE_SQ) {
            long now = System.currentTimeMillis();
            if (now - lastInteractTime < INTERACT_COOLDOWN_MS) return;

            lastInteractTime = now;
            pressBlock(mc, targetBlock);
            // Long pause after pressing so the sequence looks deliberate
            enter(State.IDLE, ticksBetween(60, 160));
        }
    }

    private void pressBlock(Minecraft mc, BlockPos pos) {
        try {
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            BlockHitResult hit = new BlockHitResult(center, Direction.UP, pos, false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        } catch (Exception ignored) {
            // Ignore - pressing failed (e.g. world changed)
        }
    }

    // ------------------------------------------------------------------
    // Interactable block search
    // ------------------------------------------------------------------

    private BlockPos findInteractable(Minecraft mc) {
        BlockPos origin = mc.player.blockPosition();
        int range = interactionRange.get();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = mc.level.getBlockState(pos);
                    Block block = state.getBlock();
                    if (!(block instanceof LeverBlock || block instanceof ButtonBlock
                            || block instanceof DiodeBlock || block instanceof BasePressurePlateBlock)) {
                        continue;
                    }
                    double distSq = mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (distSq < bestDist) {
                        bestDist = distSq;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    private void updateLookTarget(Minecraft mc, BlockPos pos) {
        float[] target = lookAnglesTo(mc, pos);
        lookYaw = target[0];
        lookPitch = target[1];
    }

    // ------------------------------------------------------------------
    // Math helpers
    // ------------------------------------------------------------------

    private static float[] lookAnglesTo(Minecraft mc, BlockPos pos) {
        Vec3 eye = mc.player.getEyePosition();
        double dx = pos.getX() + 0.5 - eye.x;
        double dy = pos.getY() + 0.5 - eye.y;
        double dz = pos.getZ() + 0.5 - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new float[]{yaw, pitch};
    }

    private static float stepAngle(float current, float target, float step) {
        return current + shortestAngleDiff(current, target) * step;
    }

    private static float shortestAngleDiff(float from, float to) {
        return ((to - from + 540f) % 360f) - 180f;
    }

    /** True when the camera is within {@code epsilon} degrees of the target. */
    private boolean isCameraNear(float targetYaw, float targetPitch, float epsilon) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return true;
        return Math.abs(shortestAngleDiff(mc.player.getYRot(), targetYaw)) < epsilon
                && Math.abs(shortestAngleDiff(mc.player.getXRot(), targetPitch)) < epsilon;
    }

    private int ticksBetween(int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt(max - min);
    }

    // ------------------------------------------------------------------
    // Consumed by ClientInputMixin
    // ------------------------------------------------------------------

    /**
     * The movement input to simulate for this tick, or null to leave real
     * input alone.
     */
    public Input simulatedInput() {
        return simulatedInput;
    }

    /** True when the anti-AFK behavior is running (module enabled AND flag on). */
    public boolean isAntiAfkRunning() {
        return active() && antiAfkEnabled;
    }

    /** True when the module may take over the input (anti-AFK on and user idle). */
    public boolean shouldOverrideInput(Input realInput) {
        if (!isAntiAfkRunning()) return false;
        if (userInteracting) return false;
        return !realInput.forward() && !realInput.backward() && !realInput.left()
                && !realInput.right() && !realInput.jump() && !realInput.shift() && !realInput.sprint();
    }

    /**
     * Recomputes the simulated input for the current tick - called right before
     * the player AI step, so the input is always fresh.
     */
    public void updateSimulatedInput() {
        simulatedInput = null;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.gui.screen() != null) return;

        boolean forward = false, backward = false, left = false, right = false;
        boolean jump = false, sprint = false;

        if (state == State.WALK) {
            forward = walkForward;
            backward = walkBackward;
            left = walkLeft;
            right = walkRight;
            sprint = random.nextInt(100) < 30;
            // Occasional hop while walking so the movement looks alive
            if (random.nextInt(100) < 2) {
                jump = true;
            }
        }

        if (forward || backward || left || right || jump || sprint) {
            simulatedInput = new Input(forward, backward, left, right, jump, false, sprint);
        }
    }

    private boolean isToggleKeyPressed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null) return false;
        try {
            Object keybind = this.toggleKeybind.get();
            if (keybind instanceof KeyBind) {
                return ((KeyBind) keybind).isPressed();
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }
}
