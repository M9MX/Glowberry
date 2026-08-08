package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.element.DynamicHudElement;
import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector2i;
import org.m9mx.cactus.glowberry.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public class WailaHudElement extends DynamicHudElement<WailaHudElement> {
    public enum Alignment { LEFT, CENTER, RIGHT }

    private final Setting<Boolean> showHealth;
    private final Setting<Boolean> showOwner;
    private final Setting<Boolean> showType;
    private final Setting<Boolean> showAge;
    private final Setting<Boolean> showPotionEffects;
    private final Setting<Boolean> showVillagerProfession;
    private final Setting<Boolean> showHorseStats;
    private final Setting<Boolean> showBlockProperties;
    private final Setting<Boolean> showBreakingBar;
    private final Setting<Boolean> showHardness;
    private final Setting<Boolean> showRequiredTool;
    private final Setting<Boolean> showRedstone;
    private final Setting<Boolean> showCoordinates;
    private final Setting<Boolean> showNicheInfo;
    private final Setting<Alignment> alignment;
    private final Setting<Integer>   scale;

    // The element has a FIXED width - it can never expand sideways, it only grows downwards.
    private static final int FIXED_WIDTH = 150;
    private static final int PAD_X       = 6;
    private static final int PAD_Y       = 4;
    private static final int LINE_HEIGHT = 16; // matches the icon size (same as the Pickup Log)
    private static final int ICON_SIZE   = 16;
    private static final int ICON_GAP    = 3;
    private static final int TEXT_OFFSET = (LINE_HEIGHT - 9) / 2;
    private static final int BAR_HEIGHT  = 4;
    private static final int OFFSCREEN   = -99999;

    private static final int COL_NAME   = 0xFFFFFFFF;
    private static final int COL_LABEL  = 0xFFAAAAAA;
    private static final int COL_OWNER  = 0xFFFFD966;
    private static final int COL_PROP   = 0xFF55FFFF;
    private static final int COL_BREAK  = 0xFFFFD966;
    private static final int COL_BABY   = 0xFF55FFFF;
    private static final int COL_BAR_BG = 0xFF2A2A2A;

    private int savedX   = Integer.MIN_VALUE;
    private int savedY   = Integer.MIN_VALUE;
    private boolean isHidden = false;

    public WailaHudElement() {
        super("waila", new Vector2i(1, 1));
        this.style.set(HudElement.Style.Default);
        var sgGeneral = this.settings.buildGroup("general");
        // Defaults: only info that's useful in everyday play. Everything niche
        // (type, effects, professions, horse stats, hardness, redstone, niche
        // block info) is off by default and one toggle away.
        this.showHealth        = sgGeneral.add(new BooleanSetting("showHealth", true));
        this.showOwner         = sgGeneral.add(new BooleanSetting("showOwner", true));
        this.showType          = sgGeneral.add(new BooleanSetting("showType", false));
        this.showAge           = sgGeneral.add(new BooleanSetting("showAge", true));
        this.showPotionEffects = sgGeneral.add(new BooleanSetting("showPotionEffects", false));
        this.showVillagerProfession = sgGeneral.add(new BooleanSetting("showVillagerProfession", false));
        this.showHorseStats    = sgGeneral.add(new BooleanSetting("showHorseStats", false));
        this.showBlockProperties = sgGeneral.add(new BooleanSetting("showBlockProperties", false));
        this.showBreakingBar   = sgGeneral.add(new BooleanSetting("showBreakingBar", true));
        this.showHardness      = sgGeneral.add(new BooleanSetting("showHardness", false));
        this.showRequiredTool  = sgGeneral.add(new BooleanSetting("showRequiredTool", true));
        this.showRedstone      = sgGeneral.add(new BooleanSetting("showRedstone", false));
        this.showCoordinates   = sgGeneral.add(new BooleanSetting("showCoordinates", false));
        this.showNicheInfo     = sgGeneral.add(new BooleanSetting("showNicheInfo", false));
        this.alignment         = sgGeneral.add(new EnumSetting<>("alignment", Alignment.LEFT));
        this.scale             = sgGeneral.add(new IntegerSetting("scale", 100).min(25).max(400));
    }

    private void hideOffscreen() {
        if (!isHidden) {
            savedX = this.getRelativePosition().x();
            savedY = this.getRelativePosition().y();
            this.move(OFFSCREEN, OFFSCREEN);
            isHidden = true;
        }
    }

    private void restorePosition() {
        if (isHidden && savedX != Integer.MIN_VALUE) {
            this.move(savedX, savedY);
            isHidden = false;
        }
    }

    private static final class Line {
        final boolean isBar;
        final ItemStack icon;    // 16x16 icon shown before the text (name lines only)
        final ItemStack icon2;   // second icon shown after the text (e.g. required tool)
        final Component label;   // left-aligned label (may be null)
        final Component value;   // value text, or right-side suffix on bars
        final int color;         // text color, or bar fill color
        final float progress;    // bar only (0..1)

        Line(boolean isBar, ItemStack icon, ItemStack icon2, Component label, Component value, int color, float progress) {
            this.isBar = isBar;
            this.icon = icon;
            this.icon2 = icon2;
            this.label = label;
            this.value = value;
            this.color = color;
            this.progress = progress;
        }
    }

    private static Line textLine(Component label, Component value, int color) {
        return new Line(false, ItemStack.EMPTY, ItemStack.EMPTY, label, value, color, 0);
    }

    private static Line barLine(Component label, Component value, int color, float progress) {
        return new Line(true, ItemStack.EMPTY, ItemStack.EMPTY, label, value, color, progress);
    }

    /** Spawn egg of the entity type, or empty if the type has none (players, modded mobs without eggs). */
    private static ItemStack entityIcon(Entity entity) {
        Optional<Holder<Item>> egg = SpawnEggItem.byId(entity.getType());
        return egg.map(holder -> ItemStackUtil.safeStack(holder.value())).orElse(ItemStack.EMPTY);
    }

    /**
     * The tool that breaks this block fastest, with the required tier shown by the item
     * (e.g. an iron pickaxe icon means it needs iron tier). Empty if no tool helps.
     */
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

    /** Roman numeral suffix for the effect amplifier: 0 -> "", 1 -> " II", 2 -> " III" ... */
    private static String amplifierSuffix(int amplifier) {
        if (amplifier < 0) return "";
        return switch (amplifier) {
            case 0 -> "";
            case 1 -> " II";
            case 2 -> " III";
            case 3 -> " IV";
            case 4 -> " V";
            default -> " " + (amplifier + 1);
        };
    }

    private static Component effectLine(MobEffectInstance instance) {
        MobEffect mobEffect = instance.getEffect().value();
        int color = 0xFF000000 | (mobEffect.getColor() & 0xFFFFFF);
        String suffix = amplifierSuffix(instance.getAmplifier());
        String time;
        if (instance.getDuration() < 0) {
            time = " (∞)";
        } else {
            int secs = (instance.getDuration() + 19) / 20;
            time = String.format(" (%d:%02d)", secs / 60, secs % 60);
        }
        return Component.translatable(mobEffect.getDescriptionId())
                .append(Component.literal(suffix + time)).withStyle(s -> s.withColor(color));
    }

    private static Component firstSignLine(SignText signText) {
        for (int i = 0; i < 4; i++) {
            Component message = signText.getMessage(i, false);
            if (!message.getString().isEmpty()) return message;
        }
        return null;
    }

    /** Special info for certain block entities (beehive, jukebox, sign, spawner). */
    private static void addNicheLines(Minecraft mc, BlockPos pos, List<Line> lines) {
        BlockEntity blockEntity = mc.level.getBlockEntity(pos);
        if (blockEntity == null) return;

        if (blockEntity instanceof BeehiveBlockEntity beehive && beehive.getOccupantCount() > 0) {
            lines.add(textLine(Component.literal("Bees: "), Component.literal(String.valueOf(beehive.getOccupantCount())), COL_OWNER));
        } else if (blockEntity instanceof JukeboxBlockEntity jukebox) {
            ItemStack record = jukebox.getTheItem();
            if (!record.isEmpty()) {
                lines.add(textLine(Component.literal("Playing: "), record.getHoverName(), COL_OWNER));
            }
        } else if (blockEntity instanceof SignBlockEntity sign) {
            Component message = firstSignLine(sign.getFrontText());
            if (message == null) message = firstSignLine(sign.getBackText());
            if (message != null) {
                lines.add(textLine(null, message, COL_OWNER));
            }
        } else if (blockEntity instanceof SpawnerBlockEntity spawner) {
            Entity display = spawner.getSpawner().getOrCreateDisplayEntity(mc.level, pos);
            if (display != null) {
                lines.add(textLine(Component.literal("Spawns: "), Component.translatable(display.getType().getDescriptionId()), COL_OWNER));
            }
        }
    }

    private List<Line> buildContent(Minecraft mc, boolean inEditor) {
        List<Line> lines = new ArrayList<>();

        if (inEditor) {
            // Sample preview so the editor shows what the element looks like
            lines.add(new Line(false, ItemStackUtil.safeStack(Items.STONE), ItemStackUtil.safeStack(Items.IRON_PICKAXE), null, Component.literal("Stone"), COL_NAME, 0));
            if (showCoordinates.get()) {
                lines.add(textLine(null, Component.literal("X: 100 Y: 64 Z: -200"), COL_PROP));
            }
            if (showRedstone.get()) {
                lines.add(textLine(Component.literal("Signal: "), Component.literal("15"), COL_OWNER));
            }
            if (showHardness.get()) {
                lines.add(textLine(Component.literal("Hardness: "), Component.literal("1.5"), COL_PROP));
            }
            if (showBlockProperties.get()) {
                lines.add(textLine(null, Component.literal("axis: y"), COL_PROP));
            }
            if (showBreakingBar.get()) {
                lines.add(barLine(Component.literal("Breaking"), Component.literal("50%"), COL_BREAK, 0.5f));
            }
            return lines;
        }

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return lines;
        }

        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity == null) return lines;

            // Name with spawn egg icon
            lines.add(new Line(false, entityIcon(entity), ItemStack.EMPTY, null, entity.getDisplayName(), COL_NAME, 0));

            // Entity type (toggleable)
            if (showType.get()) {
                lines.add(textLine(Component.literal("Type: "), Component.translatable(entity.getType().getDescriptionId()), COL_PROP));
            }

            // Baby / Adult (toggleable)
            if (showAge.get() && entity instanceof AgeableMob ageable) {
                boolean baby = ageable.isBaby();
                lines.add(textLine(Component.literal("Age: "), Component.literal(baby ? "Baby" : "Adult"), baby ? COL_BABY : COL_LABEL));
            }

            // Health (toggleable)
            if (showHealth.get() && entity instanceof LivingEntity living) {
                float ratio = living.getMaxHealth() <= 0 ? 0f : living.getHealth() / living.getMaxHealth();
                String text = String.format("%.0f/%.0f", living.getHealth(), living.getMaxHealth());
                lines.add(barLine(Component.literal("Health"), Component.literal(text), healthColor(ratio), ratio));
            }

            // Active potion effects (toggleable) - works for modded effects too
            if (showPotionEffects.get() && entity instanceof LivingEntity living) {
                for (MobEffectInstance effect : living.getActiveEffects()) {
                    lines.add(textLine(null, effectLine(effect), 0xFFFFFFFF));
                }
            }

            // Tamed / owner (toggleable) - works for every tameable mob including horses
            // (TamableAnimal like cats/wolves/parrots and AbstractHorse all implement OwnableEntity)
            if (showOwner.get() && entity instanceof OwnableEntity ownable) {
                LivingEntity owner = ownable.getOwner();
                boolean tamed;
                if (entity instanceof TamableAnimal tamable) {
                    tamed = tamable.isTame();
                } else if (entity instanceof AbstractHorse horse) {
                    tamed = horse.isTamed();
                } else {
                    tamed = owner != null;
                }

                if (owner != null) {
                    // Has an owner - show the owner instead of the tamed status
                    lines.add(textLine(Component.literal("Owner: "), owner.getDisplayName(), COL_OWNER));
                } else if (tamed) {
                    // Tamed but no owner is available (e.g. not loaded) - show tamed
                    lines.add(textLine(Component.literal("Tamed: "), Component.literal("Yes"), COL_OWNER));
                }
            }

            // Villager profession (toggleable)
            if (showVillagerProfession.get() && entity instanceof Villager villager) {
                VillagerData data = villager.getVillagerData();
                String profession = data.profession().unwrapKey().map(key -> key.identifier().getPath()).orElse("none");
                lines.add(textLine(Component.literal("Profession: "), Component.literal(profession), COL_PROP));
            }

            // Horse stats (toggleable) - same formulas as the Horse Stats HUD element
            if (showHorseStats.get() && entity instanceof AbstractHorse horse) {
                double jumpStrength = horse.getAttributeValue(Attributes.JUMP_STRENGTH);
                double moveSpeed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
                double speed = moveSpeed * 42.15;
                double jump = -1.291 * jumpStrength * jumpStrength + 4.707 * jumpStrength - 0.016;
                double maxHp = horse.getAttributeValue(Attributes.MAX_HEALTH);
                lines.add(textLine(null, Component.literal(String.format("Speed: %.2f b/s | Jump: %.1f | HP: %.0f", speed, jump, maxHp)), COL_PROP));
            }
        } else if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit && mc.level != null) {
            BlockState state = mc.level.getBlockState(blockHit.getBlockPos());

            // Name with block item icon, plus the required tool icon when one helps
            ItemStack blockIcon = ItemStackUtil.safeStack(state.getBlock());
            ItemStack toolIcon = showRequiredTool.get() ? requiredToolIcon(state) : ItemStack.EMPTY;
            lines.add(new Line(false,
                    blockIcon.isEmpty() ? ItemStack.EMPTY : blockIcon,
                    toolIcon,
                    null,
                    state.getBlock().getName(),
                    COL_NAME, 0));

            // Coordinates (toggleable)
            if (showCoordinates.get()) {
                BlockPos pos = blockHit.getBlockPos();
                lines.add(textLine(null, Component.literal(String.format("X: %d Y: %d Z: %d", pos.getX(), pos.getY(), pos.getZ())), COL_PROP));
            }

            // Redstone signal (toggleable) - strongest of what the block receives and what it emits,
            // so levers, buttons, comparators and repeaters show their own output too
            if (showRedstone.get()) {
                BlockPos pos = blockHit.getBlockPos();
                int signal = mc.level.getBestNeighborSignal(pos);
                for (Direction dir : Direction.values()) {
                    signal = Math.max(signal, mc.level.getSignal(pos, dir));
                }
                if (signal > 0) {
                    lines.add(textLine(Component.literal("Signal: "), Component.literal(String.valueOf(signal)), COL_OWNER));
                }
            }

            // Hardness (toggleable)
            if (showHardness.get()) {
                float hardness = state.getDestroySpeed(mc.level, blockHit.getBlockPos());
                lines.add(textLine(Component.literal("Hardness: "), Component.literal(String.format("%.1f", hardness)), COL_PROP));
            }

            // Block properties (toggleable)
            if (showBlockProperties.get()) {
                for (Property<?> property : state.getProperties()) {
                    Comparable<?> value = state.getValue(property);
                    lines.add(textLine(null, Component.literal(property.getName() + ": " + value), COL_PROP));
                }
            }

            // Special block entity info (toggleable): beehive bees, jukebox disc, sign text, spawner
            if (showNicheInfo.get()) {
                addNicheLines(mc, blockHit.getBlockPos(), lines);
            }

            // Block breaking bar (toggleable) - progress from the crack stage (0..9)
            if (showBreakingBar.get()) {
                MultiPlayerGameMode gameMode = mc.gameMode;
                if (gameMode != null && gameMode.isDestroying()) {
                    float progress = Math.max(0f, Math.min(1f, (gameMode.getDestroyStage() + 1) / 10f));
                    String text = Math.round(progress * 100) + "%";
                    lines.add(barLine(Component.literal("Breaking"), Component.literal(text), COL_BREAK, progress));
                }
            }
        }

        return lines;
    }

    private static int healthColor(float ratio) {
        int r = (int) (255 * (1 - ratio));
        int g = (int) (255 * ratio);
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    private static int alignStart(Alignment align, int contentW, int lineW) {
        return switch (align) {
            case CENTER -> PAD_X + (contentW - lineW) / 2;
            case RIGHT  -> PAD_X + (contentW - lineW);
            default     -> PAD_X;
        };
    }

    private static Component truncate(Font font, Component text, int maxW) {
        if (text == null || font.width(text) <= maxW) return text;
        FormattedText cut = font.substrByWidth(text, Math.max(0, maxW - font.width("…")));
        // Keep the original styling (e.g. effect colors) on the truncated text
        return Component.literal(cut.getString()).withStyle(text.getStyle());
    }

    @Override
    public void renderContent(GuiGraphicsExtractor context, int x, int y, int width, int height, int screenWidth, int screenHeight, float delta, boolean inEditor) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        List<Line> lines = buildContent(mc, inEditor);
        if (lines.isEmpty()) {
            hideOffscreen();
            return;
        }
        restorePosition();

        float scaleF   = scale.get() / 100f;
        Alignment align = alignment.get();

        // Fixed width, height grows with the number of lines (downwards only)
        int unscaledH = PAD_Y * 2 + lines.size() * LINE_HEIGHT;
        int scaledW   = Math.round(FIXED_WIDTH * scaleF);
        int scaledH   = Math.round(unscaledH * scaleF);
        this.resize(scaledW, scaledH);

        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scaleF, scaleF);

        int contentW = FIXED_WIDTH - PAD_X * 2;
        int lineY = PAD_Y;

        for (Line line : lines) {
            boolean hasIcon  = line.icon != null && !line.icon.isEmpty();
            boolean hasIcon2 = line.icon2 != null && !line.icon2.isEmpty();
            int iconGap  = hasIcon ? ICON_SIZE + ICON_GAP : 0;
            int icon2Gap = hasIcon2 ? ICON_SIZE + ICON_GAP : 0;
            int labelW = line.label == null ? 0 : font.width(line.label);
            int valueW = line.value == null ? 0 : font.width(line.value);

            if (line.isBar) {
                int barMaxW = Math.max(8, contentW - labelW - valueW - 8);
                int lineW = Math.min(contentW, labelW + 4 + barMaxW + 2 + valueW);
                int startX = alignStart(align, contentW, lineW);

                int textY = lineY + TEXT_OFFSET;
                int barY  = lineY + (LINE_HEIGHT - BAR_HEIGHT) / 2;
                int curX  = startX;

                if (line.label != null) {
                    context.text(font, line.label, curX, textY, COL_LABEL);
                    curX += labelW + 2;
                }
                context.fill(curX, barY, curX + barMaxW, barY + BAR_HEIGHT, COL_BAR_BG);
                int fillW = Math.round(barMaxW * line.progress);
                if (fillW > 0) {
                    context.fill(curX, barY, curX + fillW, barY + BAR_HEIGHT, line.color);
                }
                curX += barMaxW + 2;
                if (line.value != null) {
                    context.text(font, line.value, curX, textY, COL_LABEL);
                }
            } else {
                Component valueText = truncate(font, line.value, contentW - iconGap - icon2Gap - labelW);
                int valueTextW = valueText == null ? 0 : font.width(valueText);
                boolean clipped = line.value != null && valueTextW < valueW;
                int lineW = iconGap + labelW + valueTextW + (clipped ? font.width("…") : 0) + icon2Gap;
                int startX = alignStart(align, contentW, lineW);

                int curX = startX;
                if (hasIcon) {
                    context.fakeItem(line.icon, curX, lineY);
                    curX += ICON_SIZE + ICON_GAP;
                }
                if (line.label != null) {
                    context.text(font, line.label, curX, lineY + TEXT_OFFSET, COL_LABEL);
                    curX += labelW;
                }
                if (valueText != null) {
                    context.text(font, valueText, curX, lineY + TEXT_OFFSET, line.color);
                    curX += valueTextW;
                    if (clipped) {
                        context.text(font, "…", curX, lineY + TEXT_OFFSET, line.color);
                        curX += font.width("…");
                    }
                }
                if (hasIcon2) {
                    context.fakeItem(line.icon2, curX + ICON_GAP, lineY);
                }
            }

            lineY += LINE_HEIGHT;
        }

        pose.popMatrix();
    }

    @Override
    public WailaHudElement duplicate() {
        return new WailaHudElement();
    }

    @Override
    public String getName() {
        return "Waila (What Am I Looking At)";
    }
}
