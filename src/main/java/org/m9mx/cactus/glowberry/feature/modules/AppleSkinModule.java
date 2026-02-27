package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import org.m9mx.cactus.glowberry.cactus.FloatSetting;
import org.m9mx.cactus.glowberry.util.appleskin.client.HUDOverlayHandler;
import org.m9mx.cactus.glowberry.util.appleskin.client.TooltipOverlayHandler;

public class AppleSkinModule extends Module {
	public static volatile AppleSkinModule INSTANCE;

	// HUD Overlay Settings Group
	private final SettingGroup sgHudOverlay;
	public final Setting<Boolean> showSaturationHudOverlay;
	public final Setting<Boolean> showFoodValuesHudOverlay;
	public final Setting<Boolean> showFoodHealthHudOverlay;
	public final Setting<Boolean> showFoodExhaustionHudUnderlay;
	public final Setting<Boolean> showVanillaAnimationsOverlay;
	public final Setting<Float> maxHudOverlayFlashAlpha;

	// Tooltip Settings Group
	private final SettingGroup sgTooltip;
	public final Setting<Boolean> showFoodValuesInTooltip;
	public final Setting<Boolean> showFoodValuesInTooltipAlways;
	public final Setting<Boolean> showFoodValuesHudOverlayWhenOffhand;

	public AppleSkinModule(Category category) {
		super("appleSkin", category, new Module.Options());
		if (INSTANCE == null) {
			synchronized (AppleSkinModule.class) {
				if (INSTANCE == null) {
					INSTANCE = this;
				}
			}
		}

		// HUD Overlay Group
		this.sgHudOverlay = this.settings.buildGroup("hudOverlay");
		this.showSaturationHudOverlay = this.sgHudOverlay.add(new BooleanSetting("showSaturationHudOverlay", true));
		this.showFoodValuesHudOverlay = this.sgHudOverlay.add(new BooleanSetting("showFoodValuesHudOverlay", true));
		this.showFoodHealthHudOverlay = this.sgHudOverlay.add(new BooleanSetting("showFoodHealthHudOverlay", true));
		this.showFoodExhaustionHudUnderlay = this.sgHudOverlay.add(new BooleanSetting("showFoodExhaustionHudUnderlay", true));
		this.showVanillaAnimationsOverlay = this.sgHudOverlay.add(new BooleanSetting("showVanillaAnimationsOverlay", true));
		this.maxHudOverlayFlashAlpha = this.sgHudOverlay.add(new FloatSetting("maxHudOverlayFlashAlpha", 0.65f).min(0.0f).max(1.0f));

		// Tooltip Group
		this.sgTooltip = this.settings.buildGroup("tooltip");
		this.showFoodValuesInTooltip = this.sgTooltip.add(new BooleanSetting("showFoodValuesInTooltip", true));
		this.showFoodValuesInTooltipAlways = this.sgTooltip.add(new BooleanSetting("showFoodValuesInTooltipAlways", true));
		this.showFoodValuesHudOverlayWhenOffhand = this.sgTooltip.add(new BooleanSetting("showFoodValuesHudOverlayWhenOffhand", true));
	}

	@Override
	public void onEnable() {
		// Initialize AppleSkin functionality when module is enabled
		HUDOverlayHandler.init();
		TooltipOverlayHandler.init();
	}

	@Override
	public void onDisable() {
		// Module is disabled, AppleSkin HUD overlays are inactive
		// (Functionality will automatically be disabled due to module check in handlers)
	}
}