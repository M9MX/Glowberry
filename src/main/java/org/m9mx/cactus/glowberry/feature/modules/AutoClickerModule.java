package org.m9mx.cactus.glowberry.feature.modules;

import org.lwjgl.glfw.GLFW;
import org.m9mx.cactus.glowberry.accessor.IMinecraftClickAccessor;
import org.m9mx.cactus.glowberry.cactus.FloatSetting;
import org.m9mx.cactus.glowberry.util.ModuleMessageUtil;

import com.dwarslooper.cactus.client.event.EventHandler;
import com.dwarslooper.cactus.client.event.impl.ClientTickEvent;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.KeybindSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.StringSetting;
import com.dwarslooper.cactus.client.systems.key.KeyBind;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoClickerModule extends Module {
    public static AutoClickerModule INSTANCE;

    /** Default time delay = the same as the default 1.6 attack speed (1s / 1.6 = 625ms). */
    private static final long DEFAULT_DELAY_MS = 625L;
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)(ms|s|m)");

    private final SettingGroup generalGroup;
    public final Setting<ButtonType> buttonType;
    public final Setting<DelayType> delayType;
    public final Setting<Float> attackSpeed;
    public final Setting<String> delayTime;
    public final Setting<KeyBind> toggleKeybind;

    private boolean autoClickerActive = false;
    private long lastClickTime = 0;
    private boolean lastKeyState = false;
    /** Last entity the auto clicker hit, so freecam can keep attacking it. */
    private int lastTargetEntityId = -1;

    public enum ButtonType {
        LEFT,
        RIGHT
    }

    public enum DelayType {
        ATTACK_SPEED,
        TIME
    }

    public AutoClickerModule(Category category) {
        super("autoClicker", category, new Module.Options());
        INSTANCE = this;

        this.generalGroup = this.settings.buildGroup("general");
        this.buttonType = this.generalGroup.add(new EnumSetting<>("buttonType", ButtonType.LEFT));
        this.delayType = this.generalGroup.add(new EnumSetting<>("delayType", DelayType.ATTACK_SPEED));
        this.attackSpeed = this.generalGroup.add(new FloatSetting("attackSpeed", 1.6f).min(0.0f).max(5.0f).decimals(1));
        this.delayTime = this.generalGroup.add(new StringSetting("delayTime", "625ms").setMinLength(1).setMaxLength(16));
        this.toggleKeybind = this.generalGroup.add(new KeybindSetting("toggleKeybind", KeyBind.of(GLFW.GLFW_KEY_X)));

        // Only show the delay setting that matches the selected delay type
        this.attackSpeed.visibleIf(() -> this.delayType.get() == DelayType.ATTACK_SPEED);
        this.delayTime.visibleIf(() -> this.delayType.get() == DelayType.TIME);
    }

    @Override
    public void onEnable() {
        // Module is enabled, ready to auto click when key is pressed
    }

    @Override
    public void onDisable() {
        this.autoClickerActive = false;
    }

    @EventHandler
    public void onTick(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Handle toggle key press
        boolean currentKeyState = isToggleKeyPressed();
        if (currentKeyState && !lastKeyState) {
            this.autoClickerActive = !this.autoClickerActive;
            int color = this.autoClickerActive ? 0xFF55FF55 : 0xFFFF5555;
            ModuleMessageUtil.show(Component.literal("AutoClicker " + (this.autoClickerActive ? "§aEnabled" : "§cDisabled")), color);
        }
        lastKeyState = currentKeyState;

        // Perform auto clicking if active and enough time has passed
        long delayMs = currentDelayMs();
        if (this.autoClickerActive && System.currentTimeMillis() - lastClickTime >= delayMs) {
            performClick(mc);
            lastClickTime = System.currentTimeMillis();
        }
    }

    /** The delay between clicks: 1s / attack speed, or the parsed time string. */
    private long currentDelayMs() {
        if (this.delayType.get() == DelayType.TIME) {
            long parsed = parseDelayMs(this.delayTime.get());
            return parsed > 0 ? parsed : DEFAULT_DELAY_MS;
        }
        float speed = this.attackSpeed.get();
        return (long) ((1.0 / Math.max(0.01f, speed)) * 1000);
    }

    /**
     * Parses a formatted time string like "1s10ms", "625ms", "2.5s" or "1m30s"
     * into milliseconds. Falls back to the default delay when unparseable.
     */
    private static long parseDelayMs(String input) {
        if (input == null) return DEFAULT_DELAY_MS;
        String s = input.trim();
        if (s.isEmpty()) return DEFAULT_DELAY_MS;

        Matcher matcher = TIME_PATTERN.matcher(s);
        double totalMs = 0;
        boolean matched = false;
        while (matcher.find()) {
            double value = Double.parseDouble(matcher.group(1));
            switch (matcher.group(2)) {
                case "ms" -> totalMs += value;
                case "s" -> totalMs += value * 1000;
                case "m" -> totalMs += value * 60_000;
            }
            matched = true;
        }
        if (matched) {
            return Math.max(1L, (long) totalMs);
        }

        // No units found - treat a plain number as milliseconds
        try {
            return Math.max(1L, (long) Double.parseDouble(s));
        } catch (NumberFormatException e) {
            return DEFAULT_DELAY_MS;
        }
    }

    private void performClick(Minecraft mc) {
        ButtonType type = this.buttonType.get();

        if (type == ButtonType.LEFT) {
            // Don't go through Minecraft.startAttack(): freecam mods (e.g.
            // MinecraftFreecam/Freecam) cancel that method at HEAD while freecam
            // is active, which silently killed the auto clicker. The underlying
            // calls startAttack makes - gameMode.attack / startDestroyBlock +
            // player.swing - are not blocked, so we replicate them here.
            startAttackDirect(mc);
        } else {
            // Use vanilla's own click handler instead of faking a held key. The
            // old approach set keyUse down every click, which left the input
            // system in a stuck "mouse held" state that dragged the framerate
            // down to ~30fps until the mouse was physically moved.
            IMinecraftClickAccessor accessor = (IMinecraftClickAccessor) (Object) mc;
            accessor.glowberry_StartUseItem();
        }
    }

    /**
     * Replicates Minecraft.startAttack() without calling the (freecam-cancelled)
     * method itself: swing + attack the entity / start breaking the block the
     * crosshair points at. The hit result already comes from the camera entity,
     * so with freecam active it targets whatever the freecam looks at.
     */
    private void startAttackDirect(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.hitResult == null) return;
        // Keep vanilla's guards: no attack while hands are busy (eating, using)
        // and no attack while spectating.
        if (player.isHandsBusy() || mc.gameMode.isSpectator()) return;

        HitResult hitResult = mc.hitResult;
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) hitResult).getEntity();
            attackEntity(mc, player, target);
            return;
        }

        // Freecam moves the camera entity away from the player, so the crosshair
        // hit result follows the camera - when the camera stops looking at the
        // target, keep hitting the last entity the auto clicker attacked.
        if (mc.getCameraEntity() != player) {
            Entity remembered = rememberedTarget(mc);
            if (remembered != null) {
                attackEntity(mc, player, remembered);
                return;
            }
        }

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            if (!mc.level.getBlockState(blockHit.getBlockPos()).isAir()) {
                mc.gameMode.startDestroyBlock(blockHit.getBlockPos(), blockHit.getDirection());
            }
        } else {
            player.resetAttackStrengthTicker();
        }
        player.swing(InteractionHand.MAIN_HAND);
    }

    /** Attacks an entity and remembers it as the auto clicker's current target. */
    private void attackEntity(Minecraft mc, LocalPlayer player, Entity target) {
        mc.gameMode.attack(player, target);
        lastTargetEntityId = target.getId();
        player.swing(InteractionHand.MAIN_HAND);
    }

    /** The last attacked entity if it still exists and is alive, else null. */
    private Entity rememberedTarget(Minecraft mc) {
        if (lastTargetEntityId == -1 || mc.level == null) return null;
        Entity entity = mc.level.getEntity(lastTargetEntityId);
        if (entity == null || !entity.isAlive()) {
            lastTargetEntityId = -1;
            return null;
        }
        return entity;
    }

    private boolean isToggleKeyPressed() {
        Minecraft mc = Minecraft.getInstance();
        if (isScreenOpen(mc)) {
			return false;
		}
        try {
            Object keybind = this.toggleKeybind.get();
            if (keybind instanceof com.dwarslooper.cactus.client.systems.key.KeyBind) {
                return ((com.dwarslooper.cactus.client.systems.key.KeyBind) keybind).isPressed();
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }
    
    private boolean isScreenOpen(Minecraft mc) {
		// 26.2: the current screen moved from Minecraft.screen to Minecraft.gui.screen()
		return mc.gui.screen() != null;
	}
}