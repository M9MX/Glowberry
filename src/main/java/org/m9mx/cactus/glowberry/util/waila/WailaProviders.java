package org.m9mx.cactus.glowberry.util.waila;

import net.minecraft.client.gui.Hud;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.HitResult;
import org.m9mx.cactus.glowberry.accessor.IMultiPlayerGameModeAccessor;
import org.m9mx.cactus.glowberry.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Grouped Waila providers. The HUD only exposes a handful of settings now, and
 * each provider decides how to lay out the combined info for that group.
 */
public final class WailaProviders {
    private WailaProviders() {}

    public static final int COL_TEXT = 0xFFD8D8D8;
    public static final int COL_MOD = 0xFF66A6FF;

    private static final double SPEED_TO_BLOCKS_PER_SECOND = 43.17;
    private static final double JUMP_A = 3.35946;
    private static final double JUMP_B = 2.31115;
    private static final double JUMP_C = -0.37064;
    private static final long CURE_TOTAL_MS = 240_000L;
    private static final int STORAGE_ROW_WIDTH = 6;

    private static final Map<Integer, Long> CURE_START_TIMES = new HashMap<>();

    private static String formatTickDuration(long ticks) {
        long seconds = Math.max(0, (ticks + 19) / 20);
        if (seconds < 60) return seconds + "s";
        return (seconds / 60) + "m " + (seconds % 60) + "s";
    }

    private static String formatMillis(long millis) {
        long totalSec = Math.max(0, millis / 1000);
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return min > 0 ? min + "m " + sec + "s" : sec + "s";
    }

    private static String effectName(MobEffectInstance instance) {
        return Component.translatable(instance.getEffect().value().getDescriptionId()).getString();
    }

    private static String effectLabel(MobEffectInstance instance) {
        String suffix = switch (instance.getAmplifier()) {
            case 0 -> "";
            case 1 -> " II";
            case 2 -> " III";
            case 3 -> " IV";
            case 4 -> " V";
            default -> " " + (instance.getAmplifier() + 1);
        };
        return effectName(instance) + suffix;
    }

    private static String effectTime(MobEffectInstance instance) {
        if (instance.getDuration() < 0) return "∞";
        int secs = (instance.getDuration() + 19) / 20;
        return String.format("%d:%02d", secs / 60, secs % 60);
    }

    static double jumpHeight(double jumpStrength) {
        return JUMP_A * jumpStrength * jumpStrength + JUMP_B * jumpStrength + JUMP_C;
    }

    private static ItemStack requiredToolIcon(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return ItemStackUtil.safeStack(toolFor(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, state));
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return ItemStackUtil.safeStack(toolFor(Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, state));
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return ItemStackUtil.safeStack(toolFor(Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.DIAMOND_SHOVEL, state));
        }
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            return ItemStackUtil.safeStack(toolFor(Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.DIAMOND_HOE, state));
        }
        return ItemStack.EMPTY;
    }

    private static Item toolFor(Item wooden, Item stone, Item iron, Item diamond, BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return diamond;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return iron;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return stone;
        return wooden;
    }

    private static String namespaceOf(Entity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null ? id.getNamespace() : "minecraft";
    }

    private static String namespaceOf(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null ? id.getNamespace() : "minecraft";
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    public static void addNameLine(WailaInfoContext ctx, List<WailaLine> lines) {
        if (ctx.inEditor) {
            ItemStack tool = ctx.enabled(WailaFeature.REQUIRED_TOOL)
                    ? ItemStackUtil.safeStack(Items.IRON_PICKAXE) : ItemStack.EMPTY;
            WailaLine line = new WailaLine(false, ItemStackUtil.safeStack(Items.STONE), tool, null,
                    Component.literal("Stone"), COL_TEXT, 0);
            if (ctx.enabled(WailaFeature.REQUIRED_TOOL)) {
                line.icon2Badge = WailaLine.Badge.OK;
            }
            lines.add(line);
            return;
        }

        if (ctx.hit == null || ctx.hit.getType() == HitResult.Type.MISS) return;

        if (ctx.isEntity()) {
            Entity entity = ctx.entity();
            lines.add(WailaLine.text(null, Component.literal(entity.getDisplayName().getString()), COL_TEXT));
            return;
        }

        if (!ctx.isBlock()) return;

        BlockState state = ctx.blockState();
        ItemStack blockIcon = ItemStackUtil.safeStack(state.getBlock());
        ItemStack toolIcon = ctx.enabled(WailaFeature.REQUIRED_TOOL) ? requiredToolIcon(state) : ItemStack.EMPTY;
        WailaLine line = new WailaLine(false, blockIcon.isEmpty() ? ItemStack.EMPTY : blockIcon, toolIcon, null,
                state.getBlock().getName(), COL_TEXT, 0);
        WailaLine.Badge badge = WailaLine.Badge.NONE;

        if (ctx.enabled(WailaFeature.REQUIRED_TOOL) && ctx.mc.player != null) {
            boolean requiresTool = state.requiresCorrectToolForDrops();
            ItemStack held = ctx.mc.player.getMainHandItem();
            boolean canHarvest = !requiresTool || (!held.isEmpty() && held.isCorrectToolForDrops(state));
            badge = canHarvest ? WailaLine.Badge.OK : WailaLine.Badge.ERROR;
        }

        line = WailaLine.blockHeader(line.icon, line.icon2, line.value, line.color);
        line.icon2Badge = badge;
        lines.add(line);
    }

    public static void health(WailaInfoContext ctx, List<WailaLine> lines) {
        Entity entity = ctx.entity();
        if (!(entity instanceof LivingEntity living)) return;

        float current = living.getHealth();
        float max = living.getMaxHealth();
        // Up to 10 hearts can be drawn, so anything with 20 HP or less needs no
        // number next to the hearts. Bosses with more HP get the count.
        Component value = max > 20f ? Component.literal(String.format("%.0f/%.0f", current, max)) : null;
        lines.add(WailaLine.hearts(value, current / 2f, max));
    }

    public static void extraInfo(WailaInfoContext ctx, List<WailaLine> lines) {
        Entity entity = ctx.entity();
        if (entity == null) return;

        if (entity instanceof OwnableEntity ownable) {
            EntityReference<LivingEntity> ref = ownable.getOwnerReference();
            LivingEntity ownerEntity = null;
            if (ref != null) {
                try {
                    ownerEntity = ref.getEntity(ctx.mc.level, LivingEntity.class);
                } catch (Exception ignored) {
                }
            }
            if (ownerEntity == null) {
                try {
                    ownerEntity = ownable.getOwner();
                } catch (Exception ignored) {
                }
            }
            if (ownerEntity != null) {
                lines.add(WailaLine.text(Component.literal("Owner"), Component.literal(ownerEntity.getName().getString()), COL_TEXT));
            }
        }

        if (entity instanceof AgeableMob ageable) {
            long gameTime = ctx.mc.level != null ? ctx.mc.level.getGameTime() : 0;
            String text = ageable.isBaby() ? "Baby" : "Adult";
            if (ageable.isBaby()) {
                long remaining = MobAgeTracker.remainingBabyTicks(ageable, gameTime);
                if (remaining > 0 && !ageable.isAgeLocked()) {
                    text = "Baby (" + formatTickDuration(remaining) + ")";
                }
            }
            lines.add(WailaLine.text(Component.literal("Age"), Component.literal(text), COL_TEXT));

            // Breed: works for every breedable mob - animals and villagers alike.
            // The client cannot see love state or the real breeding cooldown, so
            // the tracker supplies them on the integrated server; elsewhere the
            // best available state is shown (Baby / Ready). Wandering traders are
            // the one AgeableMob that cannot breed, so they are skipped.
            String breed = null;
            if (!(ageable instanceof WanderingTrader)) {
                if (ageable.isBaby()) {
                    breed = "Baby";
                } else if (MobAgeTracker.isInLove(ageable.getUUID(), gameTime)) {
                    breed = "Love";
                } else {
                    long cooldown = MobAgeTracker.breedingCooldownTicks(ageable);
                    breed = cooldown > 0 ? "Cooldown (" + formatTickDuration(cooldown) + ")" : "Ready";
                }
            }
            if (breed != null) {
                lines.add(WailaLine.text(Component.literal("Breed"), Component.literal(breed), COL_TEXT));
            }
        }

        if (entity instanceof AbstractHorse horse) {
            lines.add(WailaLine.text(Component.literal("Speed"),
                    Component.literal(String.format("%.2f", horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * SPEED_TO_BLOCKS_PER_SECOND)),
                    COL_TEXT));
            lines.add(WailaLine.text(Component.literal("Jump"),
                    Component.literal(String.format("%.2f", jumpHeight(horse.getAttributeValue(Attributes.JUMP_STRENGTH)))),
                    COL_TEXT));
            lines.add(WailaLine.text(Component.literal("Health"),
                    Component.literal(String.format("%.0f", horse.getAttributeValue(Attributes.MAX_HEALTH))),
                    COL_TEXT));
        } else if (entity instanceof Camel camel) {
            lines.add(WailaLine.text(Component.literal("Speed"),
                    Component.literal(String.format("%.2f", camel.getAttributeValue(Attributes.MOVEMENT_SPEED) * SPEED_TO_BLOCKS_PER_SECOND)),
                    COL_TEXT));
            lines.add(WailaLine.text(Component.literal("Jump"),
                    Component.literal(String.format("%.2f", jumpHeight(camel.getAttributeValue(Attributes.JUMP_STRENGTH)))),
                    COL_TEXT));
            lines.add(WailaLine.text(Component.literal("Health"),
                    Component.literal(String.format("%.0f", camel.getAttributeValue(Attributes.MAX_HEALTH))),
                    COL_TEXT));
        }

        if (entity instanceof ZombieVillager zombieVillager) {
            if (!zombieVillager.isConverting()) {
                CURE_START_TIMES.remove(entity.getId());
            } else {
                long now = System.currentTimeMillis();
                long start = CURE_START_TIMES.computeIfAbsent(entity.getId(), id -> now);
                long remaining = Math.max(0, CURE_TOTAL_MS - (now - start));

                if (CURE_START_TIMES.size() > 32) {
                    CURE_START_TIMES.values().removeIf(t -> now - t > CURE_TOTAL_MS * 2);
                }

                lines.add(WailaLine.text(Component.literal("Cure"),
                        Component.literal(remaining <= 0 ? "Curing" : "≈" + formatMillis(remaining)), COL_TEXT));
            }
        }

        if (entity instanceof Painting painting) {
            String variant = painting.getVariant().unwrapKey()
                    .map(key -> key.identifier().getPath())
                    .orElse("painting");
            lines.add(WailaLine.text(Component.literal("Painting"), Component.literal(variant), COL_TEXT));
        }

        if (entity instanceof ItemFrame frame) {
            ItemStack stack = frame.getItem();
            if (!stack.isEmpty()) {
                lines.add(WailaLine.iconText(stack, Component.literal("Item"), Component.literal(stack.getHoverName().getString()), COL_TEXT));
            }
        }
    }

    public static void potionEffects(WailaInfoContext ctx, List<WailaLine> lines) {
        Entity entity = ctx.entity();
        if (!(entity instanceof LivingEntity living)) return;

        // 26.2 vanilla never sends other entities' effects to tracking clients,
        // so merge the live-synced effects (e.g. the player's own) with the ones
        // captured from the integrated server in singleplayer.
        Map<Holder<MobEffect>, MobEffectInstance> merged = new LinkedHashMap<>();
        for (MobEffectInstance effect : living.getActiveEffects()) {
            merged.put(effect.getEffect(), effect);
        }
        for (MobEffectInstance effect : MobEffectTracker.effectsFor(living.getUUID())) {
            merged.putIfAbsent(effect.getEffect(), effect);
        }

        for (MobEffectInstance effect : merged.values()) {
            // One row per effect, Jade-style: the potion icon on the left, then
            // the name (+ amplifier) and the remaining time.
            lines.add(WailaLine.spriteText(Hud.getMobEffectSprite(effect.getEffect()),
                    Component.literal(effectLabel(effect)), Component.literal(effectTime(effect)), COL_TEXT));
        }
    }

    public static void equipment(WailaInfoContext ctx, List<WailaLine> lines) {
        Entity entity = ctx.entity();
        if (!(entity instanceof LivingEntity living)) return;

        List<ItemStack> stacks = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = living.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }

        ItemStack mainHand = living.getMainHandItem();
        if (!mainHand.isEmpty()) {
            stacks.add(mainHand);
        }

        ItemStack offHand = living.getOffhandItem();
        if (!offHand.isEmpty()) {
            stacks.add(offHand);
        }

        if (!stacks.isEmpty()) {
            lines.add(WailaLine.icons(stacks, null, null, COL_TEXT));
        }
    }

    public static void modName(WailaInfoContext ctx, List<WailaLine> lines) {
        // Only show the mod name for non-vanilla content - "Minecraft" on every
        // vanilla block/entity is just noise (Jade does the same).
        if (ctx.isEntity()) {
            String ns = namespaceOf(ctx.entity());
            if (!"minecraft".equals(ns)) {
                lines.add(WailaLine.text(null, Component.literal(capitalize(ns)).withStyle(s -> s.withBold(true)), COL_MOD));
            }
            return;
        }

        if (ctx.isBlock()) {
            String ns = namespaceOf(ctx.blockState());
            if (!"minecraft".equals(ns)) {
                lines.add(WailaLine.text(null, Component.literal(capitalize(ns)).withStyle(s -> s.withBold(true)), COL_MOD));
            }
        }
    }

    public static float breakingProgress(WailaInfoContext ctx) {
        if (ctx.inEditor) return 0.5f;
        if (!ctx.isBlock()) return -1f;
        var gameMode = ctx.mc.gameMode;
        if (gameMode == null || !gameMode.isDestroying()) return -1f;
        float progress = ((IMultiPlayerGameModeAccessor) gameMode).glowberry_getDestroyProgress();
        return Math.max(0f, Math.min(1f, progress));
    }

    public static void coordinates(WailaInfoContext ctx, List<WailaLine> lines) {
        if (ctx.blockPos() == null) return;
        var pos = ctx.blockPos();
        lines.add(WailaLine.text(null, Component.literal("X: " + pos.getX() + ", Y: " + pos.getY() + ", Z: " + pos.getZ()), COL_TEXT));
    }

    public static void blockExtraInfo(WailaInfoContext ctx, List<WailaLine> lines) {
        BlockState state = ctx.blockState();
        if (state == null) return;

        if (state.getBlock() instanceof CropBlock crop) {
            int age = crop.getAge(state);
            int max = crop.getMaxAge();
            int percent = max <= 0 ? 100 : Math.round((age / (float) max) * 100);
            lines.add(WailaLine.text(Component.literal("Growth"), Component.literal(percent + "%"), COL_TEXT));
        }

        BlockEntity blockEntity = ctx.blockEntity();
        if (blockEntity == null) return;

        if (blockEntity instanceof BeehiveBlockEntity beehive) {
            lines.add(WailaLine.text(Component.literal("Honey"), Component.literal(beehive.getHoneyLevel(state) + "/5"), COL_TEXT));
            lines.add(WailaLine.text(Component.literal("Bees"), Component.literal(String.valueOf(beehive.getOccupantCount())), COL_TEXT));
        } else if (state.getBlock() instanceof NoteBlock) {
            int note = state.getValue(NoteBlock.NOTE);
            String[] notes = {"F", "F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F"};
            String noteText = notes[Math.max(0, Math.min(notes.length - 1, note))];
            lines.add(WailaLine.text(Component.literal("Note"), Component.literal(noteText), COL_TEXT));
        } else if (blockEntity instanceof JukeboxBlockEntity jukebox) {
            ItemStack record = jukebox.getTheItem();
            if (!record.isEmpty()) {
                lines.add(WailaLine.iconText(record, Component.literal("Record"), Component.literal(record.getHoverName().getString()), COL_TEXT));
            }
        } else if (blockEntity instanceof CommandBlockEntity commandBlock && ctx.mc.player != null && ctx.mc.player.getAbilities().instabuild) {
            String command = commandBlock.getCommandBlock().getCommand();
            if (!command.isBlank()) {
                lines.add(WailaLine.text(Component.literal("Cmd"), Component.literal(command), COL_TEXT));
            }
        } else if (blockEntity instanceof SpawnerBlockEntity spawner) {
            Entity display = spawner.getSpawner().getOrCreateDisplayEntity(ctx.mc.level, ctx.blockPos());
            if (display != null) {
                lines.add(WailaLine.text(Component.literal("Spawn"), Component.literal(display.getDisplayName().getString()), COL_TEXT));
            }
        }
    }

    public static void blockProperties(WailaInfoContext ctx, List<WailaLine> lines) {
        BlockState state = ctx.blockState();
        if (state == null) return;

        for (Property<?> property : state.getProperties()) {
            Comparable<?> value = state.getValue(property);
            lines.add(WailaLine.text(null, Component.literal(property.getName() + ": " + value), COL_TEXT));
        }

        // The strongest redstone signal feeding this block from any direction
        int signal = ctx.mc.level.getBestNeighborSignal(ctx.blockPos());
        for (Direction dir : Direction.values()) {
            signal = Math.max(signal, ctx.mc.level.getSignal(ctx.blockPos(), dir));
        }
        if (signal > 0) {
            lines.add(WailaLine.text(Component.literal("Redstone"), Component.literal(String.valueOf(signal)), COL_TEXT));
        }

        int light = ctx.mc.level.getMaxLocalRawBrightness(ctx.blockPos());
        lines.add(WailaLine.text(Component.literal("Light"), Component.literal(String.valueOf(light)), COL_TEXT));
    }

    public static void storageInfo(WailaInfoContext ctx, List<WailaLine> lines) {
        BlockEntity blockEntity = ctx.blockEntity();
        if (!(blockEntity instanceof Container container)) return;
        if (blockEntity instanceof BrewingStandBlockEntity || blockEntity instanceof JukeboxBlockEntity) return;

        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }

        if (stacks.isEmpty()) return;

        // A compact fill count above the icons, e.g. "Items: 14/27"
        lines.add(WailaLine.text(Component.literal("Items"),
                Component.literal(stacks.size() + "/" + container.getContainerSize()), COL_TEXT));

        for (int i = 0; i < stacks.size(); i += STORAGE_ROW_WIDTH) {
            int end = Math.min(stacks.size(), i + STORAGE_ROW_WIDTH);
            lines.add(WailaLine.icons(stacks.subList(i, end), null, null, COL_TEXT));
        }
    }

    public static void addEditorPreview(WailaInfoContext ctx, List<WailaLine> lines) {
        if (ctx.enabled(WailaFeature.HEALTH)) {
            lines.add(WailaLine.hearts(Component.literal("240/300"), 120f, 300f));
        }
        if (ctx.enabled(WailaFeature.EXTRA_INFO)) {
            lines.add(WailaLine.text(Component.literal("Owner"), Component.literal("Steve"), COL_TEXT));
            lines.add(WailaLine.text(Component.literal("Age"), Component.literal("Adult"), COL_TEXT));
            lines.add(WailaLine.text(Component.literal("Breed"), Component.literal("Ready"), COL_TEXT));
        }
        if (ctx.enabled(WailaFeature.POTION_EFFECTS)) {
            lines.add(WailaLine.spriteText(Hud.getMobEffectSprite(MobEffects.SPEED),
                    Component.literal("Speed II"), Component.literal("1:30"), COL_TEXT));
        }
        if (ctx.enabled(WailaFeature.EQUIPMENT)) {
            lines.add(WailaLine.icons(List.of(
                    ItemStackUtil.safeStack(Items.IRON_HELMET),
                    ItemStackUtil.safeStack(Items.IRON_CHESTPLATE),
                    ItemStackUtil.safeStack(Items.IRON_LEGGINGS),
                    ItemStackUtil.safeStack(Items.IRON_BOOTS),
                    ItemStackUtil.safeStack(Items.IRON_SWORD)
            ), null, null, COL_TEXT));
        }
        if (ctx.enabled(WailaFeature.COORDINATES)) {
            lines.add(WailaLine.text(null, Component.literal("X: 100, Y: 64, Z: -200"), COL_TEXT));
        }
        if (ctx.enabled(WailaFeature.BLOCK_EXTRA_INFO)) {
            lines.add(WailaLine.text(Component.literal("Growth"), Component.literal("50%"), COL_TEXT));
            lines.add(WailaLine.text(Component.literal("Bees"), Component.literal("2"), COL_TEXT));
            lines.add(WailaLine.text(Component.literal("Record"), Component.literal("music_disc_cat"), COL_TEXT));
        }
        if (ctx.enabled(WailaFeature.BLOCK_PROPERTIES)) {
            lines.add(WailaLine.text(null, Component.literal("lit: true"), COL_TEXT));
            lines.add(WailaLine.text(Component.literal("Redstone"), Component.literal("15"), COL_TEXT));
            lines.add(WailaLine.text(Component.literal("Light"), Component.literal("12"), COL_TEXT));
        }
        if (ctx.enabled(WailaFeature.STORAGE_INFO)) {
            lines.add(WailaLine.text(Component.literal("Items"), Component.literal("14/27"), COL_TEXT));
            lines.add(WailaLine.icons(List.of(
                    ItemStackUtil.safeStack(Items.DIAMOND),
                    ItemStackUtil.safeStack(Items.GOLD_INGOT),
                    ItemStackUtil.safeStack(Items.APPLE)
            ), null, null, COL_TEXT));
        }
        if (ctx.enabled(WailaFeature.MOD_NAME)) {
            // Mod name only shows for non-minecraft content, so preview a modded one
            lines.add(WailaLine.text(null, Component.literal("glowberry"), COL_MOD));
        }
    }
}
