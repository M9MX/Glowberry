/*
 * Adapted from AutoFish mod by quervylol
 * Original mod: https://github.com/quervyloll/AutoFish
 */
package org.m9mx.cactus.glowberry.feature.modules;

import org.lwjgl.glfw.GLFW;
import org.m9mx.cactus.glowberry.cactus.FloatSetting;
import org.m9mx.cactus.glowberry.util.ActionBarUtil;

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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;

public class AutoFishModule extends Module {
    public static AutoFishModule INSTANCE;

    private final SettingGroup generalGroup;
    public final Setting<KeyBind> toggleKeybind;
    public final Setting<Integer> reelDelay;
    public final Setting<Boolean> stopOnInventoryOpen;

    private boolean isAutoFishing = false;
    private boolean hasCastRod = false;
    private boolean lastCastAutomated = false;
    private int fishingTicks = 0;
    private int postReelDelayTicks = 0;
    private boolean lastKeyState = false;
    private static final int CAST_WAIT_TICKS = 100;

    public AutoFishModule(Category category) {
        super("autoFish", category, new Module.Options().set(Flag.SERVER_UNSAFE, true));
        INSTANCE = this;

        this.generalGroup = this.settings.buildGroup("general");
        this.toggleKeybind = this.generalGroup.add(new KeybindSetting("toggleKeybind", KeyBind.of(GLFW.GLFW_KEY_K)));
        this.reelDelay = this.generalGroup.add(new IntegerSetting("reelDelay", 20).min(0).max(100));
        this.stopOnInventoryOpen = this.generalGroup.add(new BooleanSetting("stopOnInventoryOpen", false));
    }

    @Override
    public void onEnable() {
        // Module is enabled
    }

    @Override
    public void onDisable() {
        stopAutoFishing();
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
            this.isAutoFishing = !this.isAutoFishing;
            String status = this.isAutoFishing ? "§aEnabled" : "§cDisabled";
            ActionBarUtil.sendActionBarMessage("AutoFish " + status);

            if (!this.isAutoFishing) {
                resetFishingState();
            } else {
                startAutoFishing();
            }
        }
        lastKeyState = currentKeyState;

        // Only process fishing logic if auto fishing is active
        if (this.isAutoFishing) {
            processFishing(mc);
        }
    }

    private void processFishing(Minecraft mc) {
        // Check if inventory is open and stop if setting enabled
        if (stopOnInventoryOpen.get() && mc.screen != null && !(mc.screen instanceof ChatScreen)) {
            stopAutoFishing();
            ActionBarUtil.sendActionBarMessage("§cAutoFish disabled! Opened GUI!");
            return;
        }

        // Check if still holding fishing rod
        if (mc.player.getMainHandItem().getItem() != Items.FISHING_ROD) {
            stopAutoFishing();
            ActionBarUtil.sendActionBarMessage("§cAutoFish disabled! Swapped item!");
            return;
        }

        // Handle post-reel delay
        if (postReelDelayTicks > 0) {
            postReelDelayTicks--;
            if (postReelDelayTicks == 0) {
                hasCastRod = false;
            }
        } else {
            fishingTicks++;

            // Cast rod if not already cast
            if (!hasCastRod) {
                castRod(mc);
                hasCastRod = true;
                lastCastAutomated = true;
            }

            // Check if we should reel in
            if (fishingTicks >= CAST_WAIT_TICKS) {
                if (isFishBiting(mc)) {
                    reelIn(mc);
                    fishingTicks = 0;
                    postReelDelayTicks = reelDelay.get();
                }
            }
        }

        // Check for manual reel (right click) and disable if detected
        if (mc.mouseHandler.isRightPressed() && !lastCastAutomated) {
            if (this.isAutoFishing) {
                stopAutoFishing();
                ActionBarUtil.sendActionBarMessage("§cAutoFish disabled! Manual reel!");
            }
        }
        lastCastAutomated = false;
    }

    private void startAutoFishing() {
        fishingTicks = 0;
        postReelDelayTicks = 0;
        hasCastRod = false;
        lastCastAutomated = false;
    }

    private void resetFishingState() {
        fishingTicks = 0;
        postReelDelayTicks = 0;
        hasCastRod = false;
        lastCastAutomated = false;
    }

    private void stopAutoFishing() {
        this.isAutoFishing = false;
        resetFishingState();
    }

    private void castRod(Minecraft mc) {
        if (mc.player != null && mc.player.getMainHandItem().getItem() == Items.FISHING_ROD) {
            mc.player.swing(InteractionHand.MAIN_HAND);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            lastCastAutomated = true;
        }
    }

    private boolean isFishBiting(Minecraft mc) {
        if (mc.player != null && mc.player.fishing != null) {
            FishingHook hook = mc.player.fishing;
            Vec3 velocity = hook.getDeltaMovement();
            if (velocity.y < -0.05 && fishingTicks > CAST_WAIT_TICKS / 2) {
                return true;
            }
        }
        return false;
    }

    private void reelIn(Minecraft mc) {
        if (mc.player != null) {
            mc.player.swing(InteractionHand.MAIN_HAND);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
    }

    private boolean isToggleKeyPressed() {
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

    public boolean isAutoFishing() {
        return this.isAutoFishing;
    }
}
