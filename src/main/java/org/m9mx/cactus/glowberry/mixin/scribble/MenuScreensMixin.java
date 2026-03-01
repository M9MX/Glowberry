/*
 * Adapted from Scribble mod by chrrs
 * Original mod: https://github.com/chrrs/scribble
 */

package org.m9mx.cactus.glowberry.mixin.scribble;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.m9mx.cactus.glowberry.util.scribble.screen.ScribbleLecternScreen;
import org.m9mx.cactus.glowberry.feature.modules.ScribbleModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.MenuType;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;

@NullMarked
@Mixin(MenuScreens.class)
public abstract class MenuScreensMixin {
    @SuppressWarnings("unchecked")
    @WrapMethod(method = "register")
    private static <S extends Screen & MenuAccess<LecternMenu>> void overrideLecternScreen(MenuType<LecternMenu> type, MenuScreens.ScreenConstructor<LecternMenu, S> factory, Operation<Void> original) {
        if (type == MenuType.LECTERN) {
            original.call(type, (MenuScreens.ScreenConstructor<LecternMenu, S>) (menu, inventory, title) -> {
                Minecraft minecraft = Minecraft.getInstance();
                ScribbleModule module = ScribbleModule.INSTANCE;
                if (module == null || !module.active()) {
                    return factory.create(menu, inventory, title);
                }
                if (!minecraft.hasShiftDown() || !module.openVanillaBookScreenOnShift.get()) {
                    return (S) new ScribbleLecternScreen(menu);
                } else {
                    return factory.create(menu, inventory, title);
                }
            });
        } else {
            original.call(type, factory);
        }
    }
}
