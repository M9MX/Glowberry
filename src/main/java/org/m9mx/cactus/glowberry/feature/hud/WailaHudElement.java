package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.systems.config.settings.impl.ColorSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector2i;
import org.m9mx.cactus.glowberry.util.waila.WailaFeature;
import org.m9mx.cactus.glowberry.util.waila.WailaInfoContext;
import org.m9mx.cactus.glowberry.util.waila.WailaLine;
import org.m9mx.cactus.glowberry.util.waila.WailaProviders;
import org.m9mx.cactus.glowberry.util.waila.WailaRegistry;

import java.awt.Color;
import java.util.List;
import java.util.Set;

/**
 * Waila renderer. All information logic lives in {@code util/waila/} - this
 * element just builds the "Show" setting from the feature registry and draws
 * whatever lines the enabled providers produce.
 */
@SuppressWarnings("unused")
public class WailaHudElement extends HideableHudElement<WailaHudElement> {
    public enum Alignment { LEFT, CENTER, RIGHT }

    private final Setting<Set<WailaFeature>> show;
    private final Setting<Alignment> alignment;
    private final Setting<Integer>   scale;

    // The element has a FIXED width - it can never expand sideways, it only grows downwards.
    private static final int FIXED_WIDTH = 150;
    private static final int PAD_X       = 6;
    private static final int PAD_Y       = 4;
    private static final int LINE_HEIGHT      = 16; // matches the icon size (same as the Pickup Log)
    private static final int ICON_SIZE        = 16;
    private static final int ICON_GAP         = 3;
    private static final int TEXT_OFFSET      = (LINE_HEIGHT - 9) / 2;
    private static final int BAR_HEIGHT       = 4;
    private static final int BREAK_BAR_HEIGHT = 2;  // thin breaking bar at the bottom edge
    private static final int BREAK_BAR_GAP    = 1;
    private static final int COL_BAR_BG       = 0xFF2A2A2A;

    // Vanilla heart sprites (drawn at vanilla size, like the regular HUD)
    private static final int    HEART_SIZE    = 9;
    private static final int    HEART_GAP     = 1;
    private static final int    MAX_HEARTS    = 10;
    private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_FULL      = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_HALF      = Identifier.withDefaultNamespace("hud/heart/half");
    // Clean friend-list icons (no background baked in), drawn over the required-tool
    // icon to show whether the held tool can harvest the block
    private static final Identifier BADGE_ERROR     = Identifier.withDefaultNamespace("friends/reject");
    private static final Identifier BADGE_OK        = Identifier.withDefaultNamespace("friends/accept");

    public WailaHudElement() {
        super("waila", new Vector2i(1, 1));
        this.style.set(HudElement.Style.Default);
        // Cactus auto-adds a textColor setting to every element. Preset it to a
        // light gray (RGB mode, so the color selector offers the RGB picker) -
        // every piece of Waila text and the breaking progress bar render with it.
        this.textColor.set(new ColorSetting.ColorValue(new Color(211, 211, 211), false));
        // Make sure every provider is registered before the setting list is built
        WailaRegistry.registerProviders();
        var sgGeneral = this.settings.buildGroup("general");
        // The toggle set is built automatically from every feature registered in WailaRegistry
        this.show = WailaRegistry.buildSetting(sgGeneral);
        this.alignment = sgGeneral.add(new EnumSetting<>("alignment", Alignment.LEFT));
        this.scale     = sgGeneral.add(new IntegerSetting("scale", 100).min(25).max(400));
    }

    @Override
    protected boolean shouldHide() {
        // Nothing to look at - the whole element (including its background box) hides
        Minecraft mc = Minecraft.getInstance();
        return mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS;
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

        // The single customizable color: labels, values and the break bar all
        // use the element's Cactus textColor setting (RGB capable).
        int textColor = this.textColor.get().color();

        WailaInfoContext infoContext = new WailaInfoContext(mc, mc.hitResult, inEditor, show.get()::contains);
        List<WailaLine> lines = WailaRegistry.buildLines(infoContext);
        if (lines.isEmpty()) {
            // Nothing to look at - render nothing but stay exactly where the user placed us
            return;
        }

        float scaleF   = scale.get() / 100f;
        Alignment align = alignment.get();

        float breakProg = show.get().contains(WailaFeature.BREAK_PROGRESS)
                ? WailaProviders.breakingProgress(infoContext)
                : -1f;
        boolean hasBreakBar = breakProg >= 0;

        // Fixed width, height grows with the number of lines (downwards only)
        int unscaledH = PAD_Y * 2 + lines.size() * LINE_HEIGHT
                + (hasBreakBar ? BREAK_BAR_GAP + BREAK_BAR_HEIGHT : 0);
        int scaledW   = Math.round(FIXED_WIDTH * scaleF);
        int scaledH   = Math.round(unscaledH * scaleF);
        this.resize(scaledW, scaledH);

        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scaleF, scaleF);

        int contentW = FIXED_WIDTH - PAD_X * 2;
        int lineY = PAD_Y;
        boolean blockSection = false;
        int blockTextX = PAD_X + ICON_SIZE + 3;

        for (WailaLine line : lines) {
            if (line.isHearts) {
                // Vanilla-style heart row: container + full/half fill, then the HP value
                int heartCount = Math.max(1, Math.min(MAX_HEARTS, (int) Math.ceil(line.max / 2f)));
                int heartsW = heartCount * HEART_SIZE + Math.max(0, heartCount - 1) * HEART_GAP;
                int valueW = line.value == null ? 0 : font.width(line.value);
                int textGap = line.value != null ? 4 : 0;
                int curX = alignStart(align, contentW, heartsW + textGap + valueW);
                int heartY = lineY + (LINE_HEIGHT - HEART_SIZE) / 2;
                int halfHearts = Math.max(0, Math.round(line.progress * 2));
                for (int i = 0; i < heartCount; i++) {
                    context.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, curX, heartY, HEART_SIZE, HEART_SIZE);
                    int remaining = halfHearts - i * 2;
                    if (remaining >= 2) {
                        context.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_FULL, curX, heartY, HEART_SIZE, HEART_SIZE);
                    } else if (remaining == 1) {
                        context.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_HALF, curX, heartY, HEART_SIZE, HEART_SIZE);
                    }
                    curX += HEART_SIZE + HEART_GAP;
                }
                if (line.value != null) {
                    context.text(font, line.value, curX, lineY + TEXT_OFFSET, textColor);
                }
                lineY += LINE_HEIGHT;
                continue;
            }

            if (line.isIconRow) {
                List<net.minecraft.world.item.ItemStack> icons = line.icons;
                int iconsW = icons.size() * ICON_SIZE + Math.max(0, icons.size() - 1) * ICON_GAP;
                int labelW = line.label == null ? 0 : font.width(line.label);
                int valueW = line.value == null ? 0 : font.width(line.value);
                int sepW = (line.label != null && line.value != null) ? font.width(": ") : 0;
                int lineW = iconsW + labelW + sepW + valueW;
                int startX = alignStart(align, contentW, lineW);

                int curX = startX;
                for (int i = 0; i < icons.size(); i++) {
                    context.fakeItem(icons.get(i), curX, lineY);
                    curX += ICON_SIZE + ICON_GAP;
                }
                if (line.label != null) {
                    context.text(font, line.label, curX, lineY + TEXT_OFFSET, textColor);
                    curX += labelW;
                    if (line.value != null) {
                        context.text(font, ": ", curX, lineY + TEXT_OFFSET, textColor);
                        curX += sepW;
                    }
                }
                if (line.value != null) {
                    context.text(font, line.value, curX, lineY + TEXT_OFFSET, textColor);
                }
                lineY += LINE_HEIGHT;
                continue;
            }

            // Block section: the header line itself is the first line (icon at the
            // left edge, text indented past it). Every following line in the section
            // aligns its text under the header text.
            if (line.isBlockHeader) {
                blockSection = true;
            }

            boolean hasIcon   = line.icon != null && !line.icon.isEmpty();
            boolean hasIcon2  = line.icon2 != null && !line.icon2.isEmpty();
            boolean hasSprite = line.sprite != null;
            int iconGap   = hasIcon ? ICON_SIZE + ICON_GAP : 0;
            int spriteGap = hasSprite ? ICON_SIZE + ICON_GAP : 0;
            int icon2Gap  = hasIcon2 ? ICON_SIZE + ICON_GAP : 0;
            int labelW = line.label == null ? 0 : font.width(line.label);
            int valueW = line.value == null ? 0 : font.width(line.value);

            if (line.isBar) {
                int barMaxW = Math.max(8, contentW - iconGap - labelW - valueW - 8);
                int lineW = Math.min(contentW, iconGap + labelW + 4 + barMaxW + 2 + valueW);
                int startX = blockSection ? blockTextX : alignStart(align, contentW, lineW);

                int textY = lineY + TEXT_OFFSET;
                int barY  = lineY + (LINE_HEIGHT - BAR_HEIGHT) / 2;
                int curX  = startX;

                if (hasIcon) {
                    context.fakeItem(line.icon, curX, lineY);
                    curX += ICON_SIZE + ICON_GAP;
                }
                if (line.label != null) {
                    context.text(font, line.label, curX, textY, textColor);
                    curX += labelW + 2;
                }
                context.fill(curX, barY, curX + barMaxW, barY + BAR_HEIGHT, COL_BAR_BG);
                int fillW = Math.round(barMaxW * line.progress);
                if (fillW > 0) {
                    context.fill(curX, barY, curX + fillW, barY + BAR_HEIGHT, textColor);
                }
                curX += barMaxW + 2;
                if (line.value != null) {
                    context.text(font, line.value, curX, textY, textColor);
                }
            } else {
                // "Type" + "Skeleton" renders as "Type: Skeleton" - a separator is shown
                // between the label and its value on every labeled line
                int sepW = (line.label != null && line.value != null) ? font.width(": ") : 0;
                Component valueText = truncate(font, line.value, contentW - iconGap - spriteGap - icon2Gap - labelW - sepW);
                int valueTextW = valueText == null ? 0 : font.width(valueText);
                boolean clipped = line.value != null && valueTextW < valueW;
                int lineW = iconGap + spriteGap + labelW + sepW + valueTextW + (clipped ? font.width("…") : 0) + icon2Gap;
                int startX = blockSection ? blockTextX : alignStart(align, contentW, lineW);

                int curX = startX;
                if (hasIcon) {
                    if (line.isBlockHeader) {
                        // Block header: icon pinned at the left edge, vertically
                        // centered on its own line, text indented past it
                        context.fakeItem(line.icon, PAD_X, lineY + (LINE_HEIGHT - ICON_SIZE) / 2);
                        curX = blockTextX;
                    } else {
                        context.fakeItem(line.icon, curX, lineY);
                        curX += ICON_SIZE + ICON_GAP;
                    }
                } else if (hasSprite) {
                    context.blitSprite(RenderPipelines.GUI_TEXTURED, line.sprite, curX, lineY, ICON_SIZE, ICON_SIZE);
                    curX += ICON_SIZE + ICON_GAP;
                }
                if (line.label != null) {
                    context.text(font, line.label, curX, lineY + TEXT_OFFSET, textColor);
                    curX += labelW;
                    if (line.value != null) {
                        context.text(font, ": ", curX, lineY + TEXT_OFFSET, textColor);
                        curX += sepW;
                    }
                }
                if (valueText != null) {
                    context.text(font, valueText, curX, lineY + TEXT_OFFSET, textColor);
                    curX += valueTextW;
                    if (clipped) {
                        context.text(font, "…", curX, lineY + TEXT_OFFSET, textColor);
                        curX += font.width("…");
                    }
                }
                if (hasIcon2) {
                    // Required-tool icon at default size, with the harvest badge drawn
                    // small (10px) in its bottom-right corner, pulled slightly toward
                    // the center so both the tool and the badge stay clearly visible
                    int toolX = curX + ICON_GAP + 1;
                    int toolY = lineY;
                    context.fakeItem(line.icon2, toolX, toolY);
                    if (line.icon2Badge != WailaLine.Badge.NONE) {
                        int badgeSize = 10;
                        Identifier badge = line.icon2Badge == WailaLine.Badge.OK ? BADGE_OK : BADGE_ERROR;
                        int badgeX = toolX + ICON_SIZE - badgeSize + 2;
                        int badgeY = toolY + ICON_SIZE - badgeSize + 2;
                        context.blitSprite(RenderPipelines.GUI_TEXTURED, badge, badgeX, badgeY, badgeSize, badgeSize, 0x80FFFFFF);
                    }
                }
            }

            lineY += LINE_HEIGHT;
        }

        if (hasBreakBar) {
            // Thin progress bar sitting at the bottom edge of the element,
            // filled with the same customizable text color
            int barY = lineY + BREAK_BAR_GAP;
            context.fill(PAD_X, barY, PAD_X + contentW, barY + BREAK_BAR_HEIGHT, COL_BAR_BG);
            int fillW = Math.round(contentW * breakProg);
            if (fillW > 0) {
                context.fill(PAD_X, barY, PAD_X + fillW, barY + BREAK_BAR_HEIGHT, textColor);
            }
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
