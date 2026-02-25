package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class TotemCounterModule extends Module {
    public static volatile TotemCounterModule INSTANCE;

    // Static data for pop counting
    private static final Map<UUID, Integer> pops = new HashMap<>();
    private static final java.util.List<Identifier> CUSTOM_TOTEMS = java.util.List.of(
            Identifier.fromNamespaceAndPath("voidtotem", "totem_of_void_undying")
    );

    // Settings groups
    private final SettingGroup sgDisplay;
    private final SettingGroup sgCounter;

    // Display settings
    public final Setting<Boolean> displayEnabled;
    public final Setting<Boolean> displayColors;

    // Pop counter settings (tab list)
    public final Setting<Boolean> counterEnabled;
    public final Setting<Boolean> separator;
    public final Setting<Boolean> counterColors;

    public TotemCounterModule(Category category) {
        super("totemcounter", category, new Module.Options());
        if (INSTANCE == null) {
            synchronized (TotemCounterModule.class) {
                if (INSTANCE == null) {
                    INSTANCE = this;
                }
            }
        }

        // Display Settings Group
        this.sgDisplay = this.settings.buildGroup("display");
        this.displayEnabled = this.sgDisplay.add(new BooleanSetting("displayEnabled", true));
        this.displayColors = this.sgDisplay.add(new BooleanSetting("displayColors", true));

        // Counter Settings Group (Tab List)
        this.sgCounter = this.settings.buildGroup("counter");
        this.counterEnabled = this.sgCounter.add(new BooleanSetting("counterEnabled", true));
        this.separator = this.sgCounter.add(new BooleanSetting("separator", true));
        this.counterColors = this.sgCounter.add(new BooleanSetting("counterColors", true));
    }

    @Override
    public void onEnable() {
        // Module is enabled
    }

    @Override
    public void onDisable() {
        // Module is disabled
    }

    // ===== Static utility methods =====

    public static Map<UUID, Integer> getPops() {
        return pops;
    }

    public static boolean isTotem(ItemStack stack) {
        if (stack.is(Items.TOTEM_OF_UNDYING)) return true;

        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return CUSTOM_TOTEMS.contains(id);
    }

    public static int getCount(Player player) {
        if (player == null) return 0;

        Inventory inv = player.getInventory();
        ItemStack offhand = inv.getItem(Inventory.SLOT_OFFHAND);

        return (int) Stream.concat(inv.getNonEquipmentItems().stream(), Stream.of(offhand))
                .filter(TotemCounterModule::isTotem)
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    public static int getColor(int count) {
        if (INSTANCE == null || !INSTANCE.displayColors.get()) {
            return 0xFFFFFFFF;
        }
        return getTotemColor(count);
    }

    public static int getPopColor(int pops) {
        return switch (pops) {
            case 1, 2 -> 0xFF55FF55; // light green
            case 3, 4 -> 0xFF00AA00; // dark green
            case 5, 6 -> 0xFFFFFF55; // yellow
            case 7, 8 -> 0xFFFFAA00; // gold
            default -> 0xFFFF5555; // red
        };
    }

    public static int getTotemColor(int amount) {
        return switch (amount) {
            case 1, 2 -> 0xFFFF5555; // red
            case 3, 4 -> 0xFFFFAA00; // gold
            case 5, 6 -> 0xFFFFFF55; // yellow
            case 7, 8 -> 0xFF00AA00; // dark green
            default -> 0xFF55FF55; // light green
        };
    }

    public static void resetPopCounter() {
        pops.clear();
    }
}
