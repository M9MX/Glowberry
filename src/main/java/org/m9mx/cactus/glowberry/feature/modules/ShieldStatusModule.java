package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.event.EventHandler;
import com.dwarslooper.cactus.client.event.impl.ClientTickEvent;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.ColorSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import org.m9mx.cactus.glowberry.util.shield.ShieldStateManager;
import java.awt.Color;

public class ShieldStatusModule extends Module {
	public static volatile ShieldStatusModule INSTANCE;

	private final SettingGroup generalGroup;
	public final Setting<Boolean> selfStateOnly;
	public final Setting<Boolean> interpolateColor;
	public final Setting<Boolean> grayscaleTexture;
	public final Setting<Integer> opacity;

	private final SettingGroup colorGroup;
	public final Setting<ColorSetting.ColorValue> enabledColor;
	public final Setting<ColorSetting.ColorValue> usingColor;
	public final Setting<ColorSetting.ColorValue> disabledColor;

	private ShieldStateManager shieldStateManager;

	public ShieldStatusModule(Category category) {
		super("shieldStatus", category, new Module.Options());
		if (INSTANCE == null) {
			synchronized (ShieldStatusModule.class) {
				if (INSTANCE == null) {
					INSTANCE = this;
				}
			}
		}

		this.generalGroup = this.settings.buildGroup("general");
		this.selfStateOnly = this.generalGroup.add(new BooleanSetting("selfStateOnly", false));
		this.interpolateColor = this.generalGroup.add(new BooleanSetting("interpolateColor", false));
		this.grayscaleTexture = this.generalGroup.add(new BooleanSetting("grayscaleTexture", false));
		this.opacity = this.generalGroup.add(new IntegerSetting("opacity", 100).min(20).max(100));

		this.colorGroup = this.settings.buildGroup("colors");
		this.enabledColor = this.colorGroup.add(new ColorSetting("enabledColor", new ColorSetting.ColorValue(new Color(68, 255, 68), false)));
		this.usingColor = this.colorGroup.add(new ColorSetting("usingColor", new ColorSetting.ColorValue(new Color(68, 255, 68), false)));
		this.disabledColor = this.colorGroup.add(new ColorSetting("disabledColor", new ColorSetting.ColorValue(new Color(255, 68, 68), false)));
	}

	@Override
	public void onEnable() {
		if (shieldStateManager == null) {
			shieldStateManager = new ShieldStateManager();
		}
	}

	@Override
	public void onDisable() {
		if (shieldStateManager != null) {
			shieldStateManager = null;
		}
	}

	@EventHandler
	public void onClientTick(ClientTickEvent event) {
		if (shieldStateManager != null) {
			shieldStateManager.update();
		}
	}

	public ShieldStateManager getShieldStateManager() {
		return shieldStateManager;
	}
}
