package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.event.EventHandler;
import com.dwarslooper.cactus.client.event.impl.ClientTickEvent;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.ColorSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import org.m9mx.cactus.glowberry.feature.overlay.LightLevelOverlayHandler;
import org.m9mx.cactus.glowberry.feature.overlay.LightLevelRenderer;
import java.awt.Color;

public class LightLevelModule extends Module {
	public static LightLevelModule INSTANCE;

	public enum OverlayType {
		NUMBER,
		BLOCK,
		BOTH
	}

	private final SettingGroup generalGroup;
	public final Setting<Boolean> showSafeAreas;
	public final Setting<Integer> chunkScanRange;
	public final Setting<Integer> threshold;
	public final Setting<OverlayType> overlayType;
	
	private final SettingGroup colorGroup;
	public final Setting<ColorSetting.ColorValue> unsafeColor;
	public final Setting<ColorSetting.ColorValue> safeColor;

	public LightLevelModule(Category category) {
		super("lightLevel", category, new Module.Options());
		INSTANCE = this;
		
		this.generalGroup = this.settings.buildGroup("general");
		this.showSafeAreas = this.generalGroup.add(new BooleanSetting("showSafeAreas", true));
		this.chunkScanRange = this.generalGroup.add(new IntegerSetting("chunkScanRange", 16).min(4).max(32));
		this.threshold = this.generalGroup.add(new IntegerSetting("threshold", 1).min(0).max(15));
		this.overlayType = this.generalGroup.add(new EnumSetting<>("overlayType", OverlayType.NUMBER));
		
		this.colorGroup = this.settings.buildGroup("colors");
		this.unsafeColor = this.colorGroup.add(new ColorSetting("unsafeColor", new ColorSetting.ColorValue(new Color(255, 68, 68), false)));
		this.safeColor = this.colorGroup.add(new ColorSetting("safeColor", new ColorSetting.ColorValue(new Color(0, 255, 100), false)));
	}

	@Override
	public void onEnable() {
		LightLevelRenderer.init();
		LightLevelOverlayHandler.init();
		LightLevelOverlayHandler.setActive(true);
	}

	@Override
	public void onDisable() {
		LightLevelOverlayHandler.setActive(false);
		LightLevelOverlayHandler.clearAll();
	}

	@EventHandler
	public void onTick(ClientTickEvent event) {
		// Update chunk scan radius in real-time
		LightLevelOverlayHandler.updateChunkScanRadius(chunkScanRange.get());
	}

	public int getColorForLightLevel(int lightLevel) {
		// Levels below threshold are unsafe (red), levels at/above threshold are safe (green)
		ColorSetting.ColorValue color = lightLevel < threshold.get() ? unsafeColor.get() : safeColor.get();
		return color.color();
	}

	public float[] getColorFloatForLightLevel(int lightLevel) {
		int color = getColorForLightLevel(lightLevel);
		float r = ((color >> 16) & 0xFF) / 255.0f;
		float g = ((color >> 8) & 0xFF) / 255.0f;
		float b = (color & 0xFF) / 255.0f;
		return new float[]{r, g, b};
	}

	public boolean shouldShowSafeAreas() {
		return showSafeAreas.get();
	}

	public int getChunkScanRange() {
		return chunkScanRange.get();
	}
	
	public int getThreshold() {
		return threshold.get();
	}

	public OverlayType getOverlayType() {
		return overlayType.get();
	}
}
