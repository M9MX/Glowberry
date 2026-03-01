package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.ColorSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import java.awt.Color;

public class TrajectoryPreviewModule extends Module {
	public static volatile TrajectoryPreviewModule INSTANCE;

	// Trajectory Display Settings Group
	private final SettingGroup trajectoryGroup;
	public final Setting<TrajectoryState> showTrajectory;
	public final Setting<ColorSetting.ColorValue> trajectoryColor;
	public final Setting<OpacityMode> trajectoryOpacity;
	public final Setting<RenderStyle> trajectoryStyle;

	// Target Outline Settings Group
	private final SettingGroup outlineGroup;
	public final Setting<TrajectoryState> outlineTargets;
	public final Setting<ColorSetting.ColorValue> outlineColor;
	public final Setting<OpacityMode> outlineOpacity;

	// Target Highlight Settings Group
	private final SettingGroup highlightGroup;
	public final Setting<TrajectoryState> highlightTargets;
	public final Setting<ColorSetting.ColorValue> highlightColor;
	public final Setting<OpacityMode> highlightOpacity;

	// Feature Toggle Settings Group
	private final SettingGroup featuresGroup;
	public final Setting<Boolean> enableOffhand;

	public enum TrajectoryState {
		ENABLED,
		TARGET_IS_ENTITY,
		DISABLED
	}

	public enum OpacityMode {
		OPAQUE,
		TRANSPARENT,
		PULSING
	}

	public enum RenderStyle {
		SOLID,
		DASHED,
		DOTTED
	}

	public TrajectoryPreviewModule(Category category) {
		super("trajectoryPreview", category, new Module.Options().set(Flag.SERVER_UNSAFE, true));
		if (INSTANCE == null) {
			synchronized (TrajectoryPreviewModule.class) {
				if (INSTANCE == null) {
					INSTANCE = this;
				}
			}
		}

		// Trajectory Display Settings Group
		this.trajectoryGroup = this.settings.buildGroup("trajectory");
		this.showTrajectory = this.trajectoryGroup.add(new EnumSetting<>("showTrajectory", TrajectoryState.ENABLED));
		this.trajectoryColor = this.trajectoryGroup.add(new ColorSetting("trajectoryColor", new ColorSetting.ColorValue(new Color(68, 255, 68), false)));
		this.trajectoryOpacity = this.trajectoryGroup.add(new EnumSetting<>("trajectoryOpacity", OpacityMode.OPAQUE));
		this.trajectoryStyle = this.trajectoryGroup.add(new EnumSetting<>("trajectoryStyle", RenderStyle.SOLID));

		// Target Outline Settings Group
		this.outlineGroup = this.settings.buildGroup("outline");
		this.outlineTargets = this.outlineGroup.add(new EnumSetting<>("outlineTargets", TrajectoryState.ENABLED));
		this.outlineColor = this.outlineGroup.add(new ColorSetting("outlineColor", new ColorSetting.ColorValue(new Color(255, 68, 68), false)));
		this.outlineOpacity = this.outlineGroup.add(new EnumSetting<>("outlineOpacity", OpacityMode.OPAQUE));

		// Target Highlight Settings Group
		this.highlightGroup = this.settings.buildGroup("highlight");
		this.highlightTargets = this.highlightGroup.add(new EnumSetting<>("highlightTargets", TrajectoryState.ENABLED));
		this.highlightColor = this.highlightGroup.add(new ColorSetting("highlightColor", new ColorSetting.ColorValue(new Color(255, 68, 68), false)));
		this.highlightOpacity = this.highlightGroup.add(new EnumSetting<>("highlightOpacity", OpacityMode.TRANSPARENT));

		// Feature Toggle Settings Group
		this.featuresGroup = this.settings.buildGroup("features");
		this.enableOffhand = this.featuresGroup.add(new BooleanSetting("enableOffhand", false));
	}

	@Override
	public void onEnable() {
		// Module is enabled, trajectory preview rendering is active
	}

	@Override
	public void onDisable() {
		// Module is disabled, trajectory preview rendering is inactive
	}

	/**
	 * Gets the alpha value based on the opacity mode setting
	 */
	public int getAlphaFromOpacity(OpacityMode opacitySetting) {
		return switch (opacitySetting) {
			case OPAQUE -> 255;
			case TRANSPARENT -> 100;
			case PULSING -> (int) Math.floor(Math.sin((System.currentTimeMillis() % 2000 / 2000.0 * Math.PI)) * 206) + 50;
		};
	}

	/**
	 * Gets the RGB color based on entity type for TARGET_IS_ENTITY mode
	 */
	public int getColorDependsOnTarget(net.minecraft.world.entity.Entity entity) {
		if (entity == null) {
			return 0xFFFFFF; // White
		}

		if (entity instanceof net.minecraft.world.entity.player.Player) {
			return 0x0000FF; // Blue
		} else if (entity instanceof net.minecraft.world.entity.NeutralMob) {
			return 0xFFFF00; // Yellow
		} else if (entity instanceof net.minecraft.world.entity.AgeableMob) {
			return 0x00FF00; // Green
		} else if (entity instanceof net.minecraft.world.entity.monster.Monster) {
			return 0xFF0000; // Red
		} else if (entity instanceof net.minecraft.world.entity.Mob) {
			return 0x800080; // Purple
		} else if (entity instanceof net.minecraft.world.entity.LivingEntity) {
			return 0x00FFFF; // Cyan
		} else {
			return 0xFF00FF; // Magenta
		}
	}
}
