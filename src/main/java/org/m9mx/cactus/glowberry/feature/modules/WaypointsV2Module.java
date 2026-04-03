package org.m9mx.cactus.glowberry.feature.modules;

import org.lwjgl.glfw.GLFW;
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
import org.m9mx.cactus.glowberry.util.waypointsv2.ui.screen.WaypointConfigScreen;
import org.m9mx.cactus.glowberry.util.waypointsv2.ui.screen.BlockListManagerScreen;
import org.m9mx.cactus.glowberry.util.waypointsv2.storage.WaypointsV2FileManager;
import com.dwarslooper.cactus.client.util.game.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * WaypointsV2 Module - Glowberry's waypoint management system.
 * 
 * This module manages all waypoint functionality including:
 * - Creating and storing waypoints
 * - Rendering waypoints with distance-based filtering
 * - Grouping waypoints by category
 * - Configurable render distance and grouping behavior
 * - Per-category visibility controls
 */
public class WaypointsV2Module extends Module {
	public static volatile WaypointsV2Module INSTANCE;

	// ===== GENERAL SETTINGS =====
	private final SettingGroup sgGeneral;
	public final Setting<KeyBind> addWaypointKeybind;
	public final Setting<KeyBind> openWaypointsKeybind;
	public final Setting<KeyBind> AddBlockKeybind;

	// ===== RENDER DISTANCE SETTINGS =====
	private final SettingGroup sgRenderDistance;
	public final Setting<Integer> maxRenderDistance;

	// ===== GROUPING & VISIBILITY SETTINGS =====
	private final SettingGroup sgGrouping;
	public final Setting<Integer> groupingRadius;
	public final Setting<Integer> groupCombiningDistance;
	public final Setting<Boolean> showThroughWalls;
	public final Setting<Boolean> showInSpectatorMode;
	public final Setting<Boolean> renderOnlyWhenHudEnabled;

	// ===== KEYBIND STATE =====
	private boolean lastAddWaypointKeyPressed = false;
	private boolean lastOpenWaypointsKeyPressed = false;
	private boolean lastAddBlockKeyPressed = false;

	// ===== BLOCK ADDING MODE STATE =====
	private boolean blockAddingMode = false;
	private BlockListManagerScreen activeBlockListScreen = null;

	public WaypointsV2Module(Category category) {
		super("waypointsV2", category, new Module.Options());

		if (INSTANCE == null) {
			synchronized (WaypointsV2Module.class) {
				if (INSTANCE == null) {
					INSTANCE = this;
				}
			}
		}

		// ===== GENERAL SETTINGS =====
		this.sgGeneral = this.settings.buildGroup("general");
		this.addWaypointKeybind = this.sgGeneral.add(
				new KeybindSetting("addWaypointKeybind", KeyBind.of(GLFW.GLFW_KEY_B))
		);
		this.openWaypointsKeybind = this.sgGeneral.add(
				new KeybindSetting("openWaypointsKeybind", KeyBind.of(GLFW.GLFW_KEY_K))
		);
		this.AddBlockKeybind = this.sgGeneral.add(
				new KeybindSetting("addBlockKeybind", KeyBind.of(GLFW.GLFW_KEY_N))
		);

		// ===== RENDER DISTANCE SETTINGS =====
		this.sgRenderDistance = this.settings.buildGroup("renderDistance");
		this.maxRenderDistance = this.sgRenderDistance.add(
				new IntegerSetting("maxRenderDistance", 1024)
						.min(512)
						.max(2048)
		);
		this.groupCombiningDistance = this.sgRenderDistance.add(
				new IntegerSetting("groupCombiningDistance", 96)
						.min(0)
						.max(1024)
		);

		// ===== GROUPING & VISIBILITY SETTINGS =====
		this.sgGrouping = this.settings.buildGroup("groupingAndVisibility");
		this.groupingRadius = this.sgGrouping.add(
				new IntegerSetting("groupingRadius", 32)
						.min(0)
						.max(256)
		);
		this.showThroughWalls = this.sgGrouping.add(
				new BooleanSetting("showThroughWalls", false)
		);
		this.showInSpectatorMode = this.sgGrouping.add(
				new BooleanSetting("showInSpectatorMode", true)
		);
		this.renderOnlyWhenHudEnabled = this.sgGrouping.add(
				new BooleanSetting("renderOnlyWhenHudEnabled", false)
		);
	}

	public static WaypointsV2Module getInstance() {
		return INSTANCE;
	}

	public List<WaypointsV2FileManager.WaypointRecord> getWaypoints() {
		return WaypointsV2FileManager.loadWaypoints();
	}

	@Override
	public void onEnable() {
		// Module is enabled - waypoint rendering and keybinds are active
	}

	@Override
	public void onDisable() {
		// Module is disabled - waypoint rendering and keybinds are inactive
	}

	/**
	 * Handles keybind presses for waypoint operations
	 */
	@EventHandler
	public void onClientTick(ClientTickEvent event) {

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return;
		}

		// Handle add waypoint keybind
		boolean addWaypointPressed = isAddWaypointKeyPressed(mc);
		if (addWaypointPressed && !lastAddWaypointKeyPressed) {
			// Open Add Waypoint screen on key press
			mc.setScreen(WaypointConfigScreen.forCreate(mc.screen));
		}
		lastAddWaypointKeyPressed = addWaypointPressed;

		// Handle open waypoints keybind
		boolean openWaypointsPressed = isOpenWaypointsKeyPressed(mc);
		if (openWaypointsPressed && !lastOpenWaypointsKeyPressed) {
			mc.setScreen(new org.m9mx.cactus.glowberry.util.waypointsv2.ui.screen.WaypointsListScreen(mc.screen));
		}
		lastOpenWaypointsKeyPressed = openWaypointsPressed;

		// Track active block list screen
		if (mc.screen instanceof BlockListManagerScreen blockScreen) {
			this.activeBlockListScreen = blockScreen;
		} else if (!this.blockAddingMode) {
			this.activeBlockListScreen = null;
		}

		// Handle add block keybind (works when NOT in any screen and in block adding mode)
		if (this.blockAddingMode && mc.screen == null) {
			boolean addBlockPressed = isAddBlockKeyPressed(mc);
			if (addBlockPressed && !lastAddBlockKeyPressed) {
				// Capture the block the player is looking at
				HitResult hit = mc.hitResult;
				if (hit instanceof BlockHitResult blockHit) {
					BlockPos targetPos = blockHit.getBlockPos();
					if (mc.level.getBlockState(targetPos).isAir()) {
						ChatUtils.error("Cannot add air block");
					} else {
						int x = targetPos.getX();
						int y = targetPos.getY();
						int z = targetPos.getZ();
						if (this.activeBlockListScreen != null) {
							this.activeBlockListScreen.addBlock(x, y, z);
						}
					}
				} else {
					ChatUtils.error("You must look at a block to add it");
				}
			}
			lastAddBlockKeyPressed = addBlockPressed;
		} else {
			lastAddBlockKeyPressed = false;
		}
	}

	private boolean isAddWaypointKeyPressed(Minecraft mc) {
		return isKeybindPressedSafely(this.addWaypointKeybind, mc);
	}

	private boolean isOpenWaypointsKeyPressed(Minecraft mc) {
		return isKeybindPressedSafely(this.openWaypointsKeybind, mc);
	}

	private boolean isAddBlockKeyPressed(Minecraft mc) {
		try {
			Object keybind = this.AddBlockKeybind.get();
			if (keybind instanceof KeyBind bind) {
				return bind.isPressed();
			}
		} catch (Exception ignored) {
			// Ignore invalid keybind states.
		}
		return false;
	}

	private boolean isKeybindPressedSafely(Setting<KeyBind> keybindSetting, Minecraft mc) {
		if (isScreenOpen(mc)) {
			return false;
		}

		try {
			Object keybind = keybindSetting.get();
			if (keybind instanceof KeyBind bind) {
				return bind.isPressed();
			}
		} catch (Exception ignored) {
			// Ignore invalid keybind states.
		}

		return false;
	}

	private boolean isScreenOpen(Minecraft mc) {
		return mc.screen != null;
	}

	/**
	 * Enable block adding mode when user clicks "Add Block" button
	 */
	public void setBlockAddingMode(boolean enabled, BlockListManagerScreen screen) {
		this.blockAddingMode = enabled;
		if (enabled) {
			this.activeBlockListScreen = screen;
		} else {
			this.activeBlockListScreen = null;
		}
	}

	/**
	 * Check if currently in block adding mode
	 */
	public boolean isBlockAddingMode() {
		return this.blockAddingMode;
	}
}

