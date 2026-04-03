package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.feature.macro.Macro;
import com.dwarslooper.cactus.client.feature.macro.MacroManager;
import com.dwarslooper.cactus.client.gui.screen.impl.MacroEditScreen;
import com.dwarslooper.cactus.client.gui.widget.CButtonWidget;
import com.dwarslooper.cactus.client.gui.widget.KeyBindWidget;
import com.dwarslooper.cactus.client.gui.widget.list.ExpandableStringListWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.m9mx.cactus.glowberry.util.cactus.macro.GlowberryMacroManager;
import org.m9mx.cactus.glowberry.util.cactus.macro.GlowberryMacroManager.GlowberryMacro;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = MacroEditScreen.class, remap = false)
public abstract class MixinMacroEditScreen extends Screen {

    @Shadow private KeyBindWidget keyBindWidget;
    @Shadow @Final private ExpandableStringListWidget commandList;
    @Shadow private EditBox nameField;
    @Shadow @Final private Macro macro;

    private EditBox stringActivationField;
    private CButtonWidget typeToggleButton;
    private boolean isStringMode = false;

    protected MixinMacroEditScreen(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ((AbstractWidget)(Object)this.keyBindWidget).width = 133;

        this.stringActivationField = new EditBox(this.font, this.width / 2 + 2, 56, 133, 20, Component.empty());
        this.stringActivationField.setHint(Component.literal("Trigger (e.g. .hi)"));
        this.addRenderableWidget(this.stringActivationField);

        this.typeToggleButton = new CButtonWidget(this.width / 2 + 140, 56, 20, 20, Component.literal("K"), (button) -> {
            this.isStringMode = !this.isStringMode;
            updateWidgetVisibility();
        });
        this.addRenderableWidget(this.typeToggleButton);

        GlowberryMacro existing = GlowberryMacroManager.MACROS.stream()
                .filter(m -> m.name.equalsIgnoreCase(this.macro.name))
                .findFirst().orElse(null);

        if (existing != null) {
            this.isStringMode = true;
            this.stringActivationField.setValue(existing.trigger);

            // FIX: Don't clear the list directly (it's immutable).
            // Instead, remove all entries except the placeholder, then use .add()
            List<ExpandableStringListWidget.Entry> entries = new ArrayList<>(this.commandList.children());
            for (ExpandableStringListWidget.Entry entry : entries) {
                if (!(entry instanceof ExpandableStringListWidget.PlaceholderEntry)) {
                    this.commandList.removeEntry(entry);
                }
            }

            // Use the widget's internal method to add the strings as actual UI entries
            for (String cmd : existing.commands) {
                this.commandList.add(cmd);
            }
        }
        updateWidgetVisibility();
    }

    private void updateWidgetVisibility() {
        this.typeToggleButton.setMessage(Component.literal(isStringMode ? "S" : "K"));
        this.keyBindWidget.visible = !isStringMode;
        this.stringActivationField.visible = isStringMode;
    }

    @Inject(method = "updateActions", at = @At("HEAD"), cancellable = true)
    private void onUpdateActions(CallbackInfo ci) {
        String name = this.nameField.getValue().trim();
        if (name.isEmpty()) return;

        if (this.isStringMode) {
            // getList() returns an unmodifiable list; wrapping in ArrayList is safe
            List<String> commandsCopy = new ArrayList<>(this.commandList.getList());

            GlowberryMacroManager.MACROS.removeIf(m -> m.name.equalsIgnoreCase(name));
            GlowberryMacroManager.MACROS.add(new GlowberryMacro(
                    name,
                    this.stringActivationField.getValue().trim(),
                    true,
                    commandsCopy
            ));

            // Ensure Cactus doesn't save a ghost copy
            MacroManager.get().getMacros().removeIf(m -> m.name.equalsIgnoreCase(name));

            this.onClose();
            ci.cancel();
        } else {
            GlowberryMacroManager.MACROS.removeIf(m -> m.name.equalsIgnoreCase(name));
        }
    }
}