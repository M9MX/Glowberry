package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import org.m9mx.cactus.glowberry.util.scribble.book.FileChooser;

public class ScribbleModule extends Module {
    public static volatile ScribbleModule INSTANCE;

    // Appearance Settings
    private final SettingGroup sgAppearance;
    public final Setting<Boolean> doublePageViewing;
    public final Setting<Boolean> centerBookGui;
    public final Setting<Boolean> showFormattingButtons;
    public final Setting<ShowActionButtons> showActionButtons;

    // Behaviour Settings
    private final SettingGroup sgBehaviour;
    public final Setting<Boolean> copyFormattingCodes;
    public final Setting<Integer> editHistorySize;

    // Miscellaneous Settings
    private final SettingGroup sgMiscellaneous;
    public final Setting<Boolean> openVanillaBookScreenOnShift;

    public ScribbleModule(Category category) {
        super("scribble", category, new Module.Options());
        
        if (INSTANCE == null) {
            synchronized(ScribbleModule.class) {
                if (INSTANCE == null) {
                    INSTANCE = this;
                }
            }
        }

        // Appearance
        this.sgAppearance = this.settings.buildGroup("appearance");
        this.doublePageViewing = sgAppearance.add(new BooleanSetting("doublePageViewing", false));
        this.centerBookGui = sgAppearance.add(new BooleanSetting("centerBookGui", true));
        this.showFormattingButtons = sgAppearance.add(new BooleanSetting("showFormattingButtons", true));
        this.showActionButtons = sgAppearance.add(new EnumSetting<>("showActionButtons", ShowActionButtons.WHEN_EDITING));

        // Behaviour
        this.sgBehaviour = this.settings.buildGroup("behaviour");
        this.copyFormattingCodes = sgBehaviour.add(new BooleanSetting("copyFormattingCodes", true));
        this.editHistorySize = sgBehaviour.add(new IntegerSetting("editHistorySize", 32).min(8).max(128));

        // Miscellaneous
        this.sgMiscellaneous = this.settings.buildGroup("miscellaneous");
        this.openVanillaBookScreenOnShift = sgMiscellaneous.add(new BooleanSetting("openVanillaBookScreenOnShift", false));
    }

    @Override
    public void onEnable() {
        // Scribble is enabled - book screens will show when opened
        FileChooser.convertLegacyBooks();
    }

    @Override
    public void onDisable() {
        // Scribble is disabled - but books can still be opened (just without enhanced UI)
    }

    public enum ShowActionButtons {
        ALWAYS,
        WHEN_EDITING,
        NEVER
    }
}
