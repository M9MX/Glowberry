//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.m9mx.cactus.glowberry.cactus;

import com.dwarslooper.cactus.client.systems.config.CactusSettings;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.util.game.render.RenderUtils;
import com.dwarslooper.cactus.client.util.generic.TextUtils;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class FloatSetting extends Setting<Float> {
    private EditorStyle editorStyle;
    private float min;
    private float max;
    private float sliderMin;
    private float sliderMax;

    public FloatSetting(String name, float value) {
        this(name, value, FloatSetting.EditorStyle.Slider);
    }

    public FloatSetting(String name, float value, EditorStyle editorStyle) {
        super(name, value);
        this.min = 0.0f;
        this.max = 200.0f;
        this.sliderMin = this.min;
        this.sliderMax = this.max;
        this.editorStyle = editorStyle;
    }

    public FloatSetting(String name, float value, Function<Float, String> textGetter) {
        this(name, value, FloatSetting.EditorStyle.Slider, textGetter);
    }

    public FloatSetting(String name, float value, EditorStyle editorStyle, Function<Float, String> textGetter) {
        super(name, value, textGetter);
        this.min = 0.0f;
        this.max = 200.0f;
        this.sliderMin = this.min;
        this.sliderMax = this.max;
        this.editorStyle = editorStyle;
    }

    /** @deprecated */
    @Deprecated(
            since = "0.12.1",
            forRemoval = true
    )
    public FloatSetting setMin(float min) {
        return this.min(min);
    }

    /** @deprecated */
    @Deprecated(
            since = "0.12.1",
            forRemoval = true
    )
    public FloatSetting setMax(float max) {
        return this.max(max);
    }

    public FloatSetting min(float min) {
        this.min = min;
        return this;
    }

    public FloatSetting max(float max) {
        this.max = max;
        return this;
    }

    public FloatSetting sliderMin(float min) {
        this.sliderMin = min;
        return this;
    }

    public FloatSetting sliderMax(float max) {
        this.sliderMax = max;
        return this;
    }

    public void setEditorStyle(EditorStyle editorStyle) {
        this.editorStyle = editorStyle;
    }

    public float getMin() {
        return this.min;
    }

    public float getMax() {
        return this.max;
    }

    public float getSliderMin() {
        return Math.max(this.sliderMin, this.min);
    }

    public float getSliderMax() {
        return Math.min(this.sliderMax, this.max);
    }

    public EditorStyle getEditorStyle() {
        return this.editorStyle;
    }

    public void set(Float value) {
        super.set(Mth.clamp(value, this.min, this.max));
    }

    public Float get() {
        return (Float)super.get();
    }

    public void save(JsonObject object) {
        object.addProperty("editor", this.getEditorStyle().name());
        object.addProperty("value", this.get());
    }

    public Float load(JsonObject object) {
        if (object.has("editor")) {
            this.setEditorStyle(FloatSetting.EditorStyle.valueOf(object.get("editor").getAsString()));
        }

        this.set(object.get("value").getAsFloat());
        return this.get();
    }

    public AbstractWidget buildWidget() {
        return new Widget();
    }

    public static enum EditorStyle {
        Slider,
        Input;
    }

    public class Widget extends Setting<Float>.Widget {
        public float value = FloatSetting.this.get();
        public boolean isSlider;
        private final EditBox widget;
        private boolean sliderDown;

        public Widget() {
            super();
            this.isSlider = FloatSetting.this.getEditorStyle() == FloatSetting.EditorStyle.Slider;
            this.widget = new EditBox(this.textRenderer, this.widgetWidth, 20, Component.empty());
            this.widget.setFilter((s) -> s.isEmpty() || s.matches("^-?\\d*\\.?\\d*$"));
            this.widget.setValue(FloatSetting.this.get().toString());
            this.widget.setResponder((s) -> {
                boolean invalid = s.isEmpty() || s.equals("-");
                float f = Float.NaN;

                try {
                    f = Float.parseFloat(s);
                } catch (NumberFormatException var5) {
                }

                if (f >= FloatSetting.this.getMin() && f <= FloatSetting.this.getMax() && !invalid) {
                    this.widget.setTextColor(-2039584);
                    this.setValue(f);
                } else {
                    this.widget.setTextColor(-43691);
                }

            });
            this.widget.moveCursorToStart(false);
            this.widget.moveCursorToEnd(false);
        }

        public Identifier getHandleTexture(int mouseX) {
            return RenderUtils.SLIDER_HANDLE_TEXTURES.get(true, this.isHovered && mouseX > this.getX() + this.widgetPosX());
        }

        public void wrappedRender(GuiGraphics context, int mouseX, int mouseY, float delta) {
            Font var10001 = this.textRenderer;
            MutableComponent var10002 = Component.literal(this.isSlider ? "↔" : "T").withStyle(this.isToggleHovered((double)mouseX, (double)mouseY) ? ChatFormatting.WHITE : ChatFormatting.GRAY);
            int var10003 = this.widgetPosX() - 8;
            int var10004 = this.getHeight();
            Objects.requireNonNull(this.textRenderer);
            context.drawCenteredString(var10001, var10002, var10003, (var10004 - 9) / 2 + 1, -1);
            if (this.isSlider) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED, RenderUtils.SLIDER_TEXTURES.enabled(), this.widgetPosX(), 0, this.widgetWidth, this.getHeight());
                context.blitSprite(RenderPipelines.GUI_TEXTURED, this.getHandleTexture(mouseX), this.widgetPosX() + (int)((double)(Mth.clamp(this.value, FloatSetting.this.getSliderMin(), FloatSetting.this.getSliderMax()) - FloatSetting.this.getSliderMin()) / (double)this.sliderDifference() * (double)(this.widgetWidth - 8)), 0, 8, this.height);
                var10001 = this.textRenderer;
                String var6 = FloatSetting.this.getText();
                var10003 = this.width - this.widgetWidth / 2;
                var10004 = this.getHeight();
                Objects.requireNonNull(this.textRenderer);
                context.drawCenteredString(var10001, var6, var10003, (var10004 - 9) / 2 + 1, Color.WHITE.getRGB());
            } else {
                this.widget.setWidth(this.widgetWidth);
                context.pose().pushMatrix();
                context.pose().translate((float)(-this.getX()), (float)(-this.getY()));
                this.widget.setPosition(this.getX() + this.width - this.widget.getWidth(), this.getY());
                this.widget.render(context, mouseX, mouseY, delta);
                context.pose().popMatrix();
            }

        }

        protected void onDrag(@NotNull MouseButtonEvent click, double offsetX, double offsetY) {
            if (this.isSlider && this.sliderDown) {
                this.setValueFromMouse(click.x());
            }

            super.onDrag(click, offsetX, offsetY);
        }

        public boolean mouseClicked(@NotNull MouseButtonEvent click, boolean doubled) {
            if (!this.isSlider && this.widget.isFocused() && click.x() > (double)(this.getX() + this.widgetPosX())) {
                return this.widget.mouseClicked(click, doubled);
            } else {
                if (this.isToggleHovered(click.x(), click.y())) {
                    this.isSlider = !this.isSlider;
                    this.widget.setValue(FloatSetting.this.get().toString());
                    FloatSetting.this.setEditorStyle(this.isSlider ? FloatSetting.EditorStyle.Slider : FloatSetting.EditorStyle.Input);
                }

                if (super.mouseClicked(click, doubled)) {
                    if (!this.isSlider) {
                        this.widget.setFocused(true);
                    } else if (click.x() >= (double)(this.getX() + this.widgetPosX())) {
                        this.sliderDown = true;
                        this.setValueFromMouse(click.x());
                    }

                    return true;
                } else {
                    return false;
                }
            }
        }

        public boolean mouseReleased(@NotNull MouseButtonEvent click) {
            this.sliderDown = false;
            return super.mouseReleased(click);
        }

        public boolean keyPressed(@NotNull KeyEvent input) {
            if (!this.isSlider || input.key() != 262 && input.key() != 263) {
                return this.widget.keyPressed(input) || super.keyPressed(input);
            } else {
                this.setValue(this.value + (input.key() == 262 ? 0.1f : -0.1f) * (input.hasShiftDown() ? 10 : 1));
                return true;
            }
        }

        public boolean charTyped(@NotNull CharacterEvent input) {
            return !this.isSlider && this.widget.charTyped(input) || super.charTyped(input);
        }

        public void setFocused(boolean focused) {
            this.widget.setFocused(focused);
            super.setFocused(focused);
        }

        private boolean isToggleHovered(double mouseX, double mouseY) {
            return mouseX >= (double)(this.getX() + this.widgetPosX() - 16) && mouseX < (double)(this.getX() + this.widgetPosX()) && mouseY >= (double)this.getY() && mouseY < (double)(this.getY() + this.getHeight());
        }

        private void setValueFromMouse(double mouseX) {
            float value = (float)((double) FloatSetting.this.getSliderMin() + (mouseX - (double)(this.getX() + this.widgetPosX() - 4)) / (double)this.widgetWidth * (double)this.sliderDifference());
            this.setValue((Boolean)CactusSettings.get().allowSliderOverdrive.get() ? value : Mth.clamp(value, FloatSetting.this.getSliderMin(), FloatSetting.this.getSliderMax()));
        }

        public void setValue(float value) {
            float clampedValue = Mth.clamp(value, FloatSetting.this.getMin(), FloatSetting.this.getMax());
            if (clampedValue != this.value) {
                this.value = clampedValue;
                FloatSetting.this.set(this.value);
            }

        }

        public float sliderDifference() {
            return FloatSetting.this.getSliderMax() - FloatSetting.this.getSliderMin();
        }

        public float difference() {
            return FloatSetting.this.getMax() - FloatSetting.this.getMin();
        }
    }
}
