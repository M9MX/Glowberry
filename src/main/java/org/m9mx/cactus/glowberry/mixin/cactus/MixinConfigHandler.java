package org.m9mx.cactus.glowberry.mixin.cactus;

import com.dwarslooper.cactus.client.systems.config.ConfigHandler;
import com.dwarslooper.cactus.client.systems.config.FileConfiguration;
import org.m9mx.cactus.glowberry.util.AutoSaveHandler;
import org.m9mx.cactus.glowberry.util.cactus.macro.GlowberryMacroManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ConfigHandler.class, remap = false)
public class MixinConfigHandler {

    @Inject(method = "save", at = @At("HEAD"))
    private void onSave(CallbackInfoReturnable<?> cir) {
        GlowberryMacroManager.saveToFile();
    }

    /**
     * Modifies Cactus' save code: when the save was triggered by our screen-change
     * auto-save, run the exact same saving logic (isDoneLoading check + saving
     * every config) but WITHOUT the "Cactus config saved!" message, then cancel
     * the original method. Cactus' own close-save runs normally and still logs.
     */
    // "save()V" pins this to the no-arg void save() - the plain "save" selector also
    // matches save(FileConfiguration) / save(FileConfiguration, File, TreeSerializerFilter)
    // which return boolean and require a CallbackInfoReturnable instead.
    @Inject(method = "save()V", at = @At("HEAD"), cancellable = true)
    private void glowberry_suppressSaveLog(CallbackInfo ci) {
        if (!AutoSaveHandler.isSuppressingLog()) return;

        ConfigHandler self = (ConfigHandler) (Object) this;
        if (!self.isDoneLoading()) return;

        // Same work Cactus does inside save(), just without the log message
        for (FileConfiguration<?> config : self.getConfigurations().values()) {
            try {
                self.save(config);
            } catch (Exception ignored) {
                // A failed config must never break the whole save
            }
        }
        ci.cancel();
    }

    @Inject(method = "reload", at = @At("TAIL"))
    private void onLoad(CallbackInfo ci) {
        GlowberryMacroManager.load();
    }
}