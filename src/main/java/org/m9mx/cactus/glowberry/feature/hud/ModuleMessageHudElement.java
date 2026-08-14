package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import org.m9mx.cactus.glowberry.util.ModuleMessageUtil;

/**
 * Module Message - a small overlay that shows module changes and module info
 * messages. When this element is placed, module toggles ("X enabled"/"X
 * disabled") and messages sent by modules (e.g. Auto Clicker's on/off status)
 * are displayed here instead of the action bar. Without the element everything
 * falls back to the action bar, so nothing is lost.
 *
 * Only ONE message is ever shown: a new message replaces the previous one, so
 * the overlay never stacks. Messages can carry a leading sprite (e.g. a potion
 * effect icon) or item icon, drawn before the text. New messages slide in and
 * fade in; expiring messages fade out.
 *
 * The message text always renders in the element's Text Color setting, so the
 * user can freely override the color. The message's own color (picked by the
 * sender, e.g. green for enabled / red for disabled) is shown by the "State
 * Indicator" setting instead: a rounded colored border around the element, a
 * colored bar at the bottom, or nothing.
 */
@SuppressWarnings("unused")
public class ModuleMessageHudElement extends HideableHudElement<ModuleMessageHudElement> {
    public enum Alignment { LEFT, CENTER, RIGHT }
    public enum Indicator { BORDER, BAR, NONE }

    private final Setting<Integer>   duration;    // seconds the message stays
    private final Setting<Boolean>   showIcons;   // draw the sprite/item icon before the text
    private final Setting<Alignment> alignment;
    private final Setting<Indicator> indicator;   // how the message color is shown
    private final Setting<Integer>   scale;

    private static final int FADE_MS     = 400;   // fade out over the last 0.4s
    private static final int ENTER_MS    = 250;   // slide-in duration
    private static final int SLIDE_PIXELS = 10;   // entrance slide distance (whole element)
    private static final int PAD_X       = 6;
    private static final int PAD_Y       = 4;
    private static final int LINE_HEIGHT = 16;    // matches the 16px icon height
    private static final int ICON_SIZE   = 16;
    private static final int ICON_GAP    = 3;
    private static final int BAR_HEIGHT  = 2;     // bottom bar thickness
    private static final int CORNER_R    = 4;     // rounded border corner radius

    private int lastWidth = -1;
    private boolean editorSampleAdded = false;

    @Override
    protected boolean shouldHide() {
        // Only visible while there is a message to show (always visible in the editor)
        return ModuleMessageUtil.currentMessage == null;
    }

    public ModuleMessageHudElement() {
        super("module_message", new Vector2i(1, 1));
        this.style.set(HudElement.Style.Default);
        var sgGeneral = this.settings.buildGroup("general");
        this.duration  = sgGeneral.add(new IntegerSetting("duration", 4).min(1).max(15));
        this.showIcons = sgGeneral.add(new BooleanSetting("showIcons", true));
        this.alignment = sgGeneral.add(new EnumSetting<>("alignment", Alignment.LEFT));
        this.indicator = sgGeneral.add(new EnumSetting<>("indicator", Indicator.BORDER));
        this.scale     = sgGeneral.add(new IntegerSetting("scale", 100).min(25).max(400));
    }

    @Override
    public void created() {
        // The element was placed in the HUD - from now on module messages and
        // toggle feedback are shown here instead of the action bar.
        ModuleMessageUtil.moduleMessageElementActive = true;
    }

    @Override
    public void removed() {
        // Element removed - everything falls back to the action bar again.
        ModuleMessageUtil.moduleMessageElementActive = false;
        ModuleMessageUtil.currentMessage = null;
    }

    private void anchoredResize(int newWidth, int newHeight) {
        int oldWidth = lastWidth == -1 ? newWidth : lastWidth;
        lastWidth = newWidth;
        this.resize(newWidth, newHeight);
        if (lastWidth != -1 && newWidth != oldWidth) {
            int dx = newWidth - oldWidth;
            Alignment align = alignment.get();
            if (align == Alignment.CENTER) {
                this.move(this.getRelativePosition().x() - dx / 2, this.getRelativePosition().y());
            } else if (align == Alignment.RIGHT) {
                this.move(this.getRelativePosition().x() - dx, this.getRelativePosition().y());
            }
        }
    }

    /**
     * Slides the WHOLE element (background box included) in from the alignment
     * side while a new message animates in, instead of only moving the text.
     */
    @Override
    public void render(GuiGraphicsExtractor context, int x, int y, int width, int height, float delta, boolean inEditor) {
        recoverOffscreenPosition();
        if (!inEditor && shouldHide()) {
            return;
        }

        int slideX = x;
        int slideY = y;
        if (!inEditor) {
            ModuleMessageUtil.Message message = ModuleMessageUtil.currentMessage;
            if (message != null) {
                long now = System.currentTimeMillis();
                long keepMs = duration.get() * 1000L;

                // Entrance: slide the whole element in from the alignment side.
                // The offset is positive for the first ENTER_MS, then eases to 0.
                float enter = Mth.clamp((now - message.time) / (float) ENTER_MS, 0f, 1f);
                int offset = (int) ((1f - enter) * SLIDE_PIXELS);

                // Exit: during the fade-out window, keep drifting the whole
                // element away in the same direction so the background doesn't
                // sit still while the text fades.
                long age = now - message.time;
                if (age > keepMs) {
                    float exit = Mth.clamp((age - keepMs) / (float) FADE_MS, 0f, 1f);
                    offset -= (int) (exit * SLIDE_PIXELS * 0.5f);
                }

                switch (alignment.get()) {
                    case CENTER -> { /* slides straight in place - just fades */ }
                    case RIGHT -> slideX += offset;
                    default -> slideX -= offset;
                }
            }
        }
        super.render(context, slideX, slideY, width, height, delta, inEditor);
    }

    @Override
    public void renderContent(GuiGraphicsExtractor context, int x, int y, int width, int height, int screenWidth, int screenHeight, float delta, boolean inEditor) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;

        // Editor preview: a sample message with a potion icon so the element is
        // never blank and the icon feature is visible.
        if (inEditor && !editorSampleAdded) {
            Identifier sprite = null;
            try {
                sprite = Hud.getMobEffectSprite(MobEffects.SPEED);
            } catch (Exception ignored) {
            }
            ModuleMessageUtil.currentMessage =
                    new ModuleMessageUtil.Message(Component.literal("Module §aenabled"), 0xFF55FF55, sprite, ItemStack.EMPTY);
            editorSampleAdded = true;
        } else if (!inEditor && editorSampleAdded) {
            // Leaving the editor: drop the preview sample
            ModuleMessageUtil.currentMessage = null;
            editorSampleAdded = false;
        }

        ModuleMessageUtil.Message message = ModuleMessageUtil.currentMessage;
        if (message == null && !inEditor) return;

        float scaleF = scale.get() / 100f;
        Alignment align = alignment.get();
        boolean icons = showIcons.get();
        long now = System.currentTimeMillis();
        long keepMs = duration.get() * 1000L;

        // Expire the message after its duration (+ fade out time)
        if (!inEditor && message != null && now - message.time > keepMs + FADE_MS) {
            ModuleMessageUtil.currentMessage = null;
            message = null;
        }
        if (message == null && !inEditor) return;

        // The message text always renders in the element's Text Color setting, so
        // users can override the color freely; embedded formatting codes are
        // stripped so they can't fight the setting. The message's own color is
        // used for the state indicator (border/bar) below.
        String text = stripFormatting(message.text.getString());

        boolean hasSprite = icons && message.sprite != null;
        boolean hasIcon = icons && message.icon != null && !message.icon.isEmpty();
        int leadW = (hasSprite || hasIcon) ? ICON_SIZE + ICON_GAP : 0;

        int textW = mc.font.width(text);
        int maxW = leadW + textW;
        int unscaledW = PAD_X * 2 + maxW;
        int unscaledH = PAD_Y * 2 + LINE_HEIGHT;
        anchoredResize(Math.round(unscaledW * scaleF), Math.round(unscaledH * scaleF));

        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scaleF, scaleF);

        // Entrance: the whole element already slides in (see render()); here the
        // text just fades in over the first ENTER_MS.
        float progress = Mth.clamp((now - message.time) / (float) ENTER_MS, 0f, 1f);
        int alpha = (int) (255 * progress);

        // Expiry fade-out over the last FADE_MS
        if (!inEditor) {
            long age = now - message.time;
            if (age > keepMs) {
                alpha = Math.min(alpha, Math.max(0, 255 - (int) ((age - keepMs) * 255 / FADE_MS)));
            }
        }
        int a = Mth.clamp(alpha, 0, 255);
        int textColor = (this.textColor.get().color() & 0x00FFFFFF) | (a << 24);
        int indicatorColor = (message.color & 0x00FFFFFF) | (a << 24);

        int lineX = switch (align) {
            case CENTER -> PAD_X + (maxW - leadW - textW) / 2;
            case RIGHT -> PAD_X + (maxW - leadW - textW);
            default -> PAD_X;
        };

        int textX = lineX + leadW;
        if (hasSprite) {
            context.blitSprite(RenderPipelines.GUI_TEXTURED, message.sprite, lineX, PAD_Y + (LINE_HEIGHT - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE);
        } else if (hasIcon) {
            context.fakeItem(message.icon, lineX, PAD_Y + (LINE_HEIGHT - ICON_SIZE) / 2);
        }
        context.text(mc.font, text, textX, PAD_Y + (LINE_HEIGHT - 9) / 2, textColor);

        // State indicator: the message's color (senders pick it, e.g. green for
        // enabled, red for disabled) shown as a rounded border or a bottom bar,
        // while the text itself follows the Text Color setting.
        switch (indicator.get()) {
            case BORDER -> fillRoundedOutline(context, 0, 0, unscaledW, unscaledH, CORNER_R, indicatorColor);
            case BAR -> context.fill(0, unscaledH - BAR_HEIGHT, unscaledW, unscaledH, indicatorColor);
            case NONE -> { /* no indicator */ }
        }

        pose.popMatrix();
    }

    /** Removes Minecraft formatting codes (\u00a7x) so only the Text Color setting colors the text. */
    private static String stripFormatting(String s) {
        if (s == null || s.indexOf('\u00a7') < 0) return s == null ? "" : s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00a7' && i + 1 < s.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Draws a 1px rounded rectangle outline with filled rects: four straight
     * edges plus quarter-circle arc corners (radius {@code r}), so the corners
     * are genuinely rounded instead of square chunks - the same corner radius
     * Cactus' rounded background sprite uses. Pure fills, fully colored, works
     * over any background style.
     */
    private static void fillRoundedOutline(GuiGraphicsExtractor context, int x, int y, int w, int h, int r, int color) {
        // Straight edges (1px thick)
        context.fill(x + r, y, x + w - r, y + 1, color);
        context.fill(x + r, y + h - 1, x + w - r, y + h, color);
        context.fill(x, y + r, x + 1, y + h - r, color);
        context.fill(x + w - 1, y + r, x + w, y + h - r, color);
        // Rounded corners: every pixel in the corner block that lies on the
        // quarter-circle arc (distance in [r-1, r] from the corner center),
        // mirrored to all four corners.
        int inner = (r - 1) * (r - 1);
        int outer = r * r;
        for (int dx = 0; dx < r; dx++) {
            for (int dy = 0; dy < r; dy++) {
                int d2 = (r - dx) * (r - dx) + (r - dy) * (r - dy);
                if (d2 >= inner && d2 <= outer) {
                    context.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, color);
                    context.fill(x + w - 1 - dx, y + dy, x + w - dx, y + dy + 1, color);
                    context.fill(x + dx, y + h - 1 - dy, x + dx + 1, y + h - dy, color);
                    context.fill(x + w - 1 - dx, y + h - 1 - dy, x + w - dx, y + h - dy, color);
                }
            }
        }
    }

    @Override
    public ModuleMessageHudElement duplicate() {
        return new ModuleMessageHudElement();
    }

    @Override
    public String getName() {
        return "Module Message";
    }
}
