package org.m9mx.cactus.glowberry.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin to disable conflicting mixins when certain external mods are present.
 */
public class MixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        // no-op
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            if (mixinClassName != null) {
                // Full qualified name of our mixin class
                String ours = "org.m9mx.cactus.glowberry.mixin.PlayerTabOverlayMixin";
                if (mixinClassName.equals(ours) || mixinClassName.endsWith(".PlayerTabOverlayMixin")) {
                    boolean externalPingMod = FabricLoader.getInstance().isModLoaded("better-ping-display")
                            || FabricLoader.getInstance().isModLoaded("betterpingdisplay");
                    // If the external ping mod is present, skip applying our mixin.
                    return !externalPingMod;
                }
            }
        } catch (Throwable ignored) {
            // If FabricLoader not present or check fails, allow the mixin to apply (fail open).
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // no-op
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // no-op
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // no-op
    }
}
