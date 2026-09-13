package com.eldrit.gamblingaddict.config.gui;

import com.eldrit.gamblingaddict.render.RenderCompat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class ConfigControls {
    private ConfigControls() {
    }

    public static class BooleanOption extends ConfigEntry {
        private static final int TRACK_W = 40;
        private static final int TRACK_H = 8;
        private static final int KNOB_W = 12;

        private final BooleanSupplier getter;
        private final Consumer<Boolean> setter;

        public BooleanOption(String name, String description, BooleanSupplier getter, Consumer<Boolean> setter) {
            super(name, description);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        protected void renderWidget(GuiGraphicsExtractor ctx, Font tr, int wx, int rowY, int ww, int mouseX, int mouseY) {
            boolean on = getter.getAsBoolean();
            int y = rowY + 26;

            ctx.fill(wx, y, wx + TRACK_W, y + TRACK_H, on ? 0xFF2C6E31 : 0xFF7A2222);
            ctx.fill(wx, y, wx + TRACK_W, y + 1, on ? 0xFF3F9B47 : 0xFF9E2C2C);

            int knobX = on ? wx + TRACK_W - KNOB_W : wx;
            ctx.fill(knobX, y - 2, knobX + KNOB_W, y + TRACK_H + 2, 0xFFDCDCE8);
            ctx.fill(knobX + 1, y - 1, knobX + KNOB_W - 1, y + TRACK_H + 1, 0xFFF2F2FA);

            ctx.text(tr, Component.literal(on ? "ON" : "OFF"), wx + TRACK_W + 6, y,
                    on ? 0xFF6FCF77 : 0xFFCF6F6F, false);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int x, int y, int width) {
            if (mx >= x && mx < x + CONTROL_W && my >= y && my < y + MIN_HEIGHT) {
                setter.accept(!getter.getAsBoolean());
                return true;
            }
            return false;
        }
    }

    public static class FloatOption extends ConfigEntry {
        private static final int BAR_W = 92;
        private static final int BAR_H = 4;

        private final Supplier<Float> getter;
        private final Consumer<Float> setter;
        private final float min;
        private final float max;
        private final float step;
        private final String suffix;

        public FloatOption(String name, String description, float min, float max, float step,
                           String suffix, Supplier<Float> getter, Consumer<Float> setter) {
            super(name, description);
            this.min = min;
            this.max = max;
            this.step = step;
            this.suffix = suffix;
            this.getter = getter;
            this.setter = setter;
        }

        private float fraction() {
            return (clamp(getter.get()) - min) / (max - min);
        }

        private float clamp(float v) {
            return Math.max(min, Math.min(max, v));
        }

        private float quantise(float v) {
            float snapped = Math.round(clamp(v) / step) * step;
            return Math.round(snapped * 1000.0f) / 1000.0f;
        }

        @Override
        protected void renderWidget(GuiGraphicsExtractor ctx, Font tr, int wx, int rowY, int ww, int mouseX, int mouseY) {
            String readout = String.format(Locale.ROOT, step >= 1.0f ? "%.0f%s" : "%.2f%s", getter.get(), suffix);
            ctx.text(tr, Component.literal(readout), wx, rowY + 19, COLOUR_VALUE, false);

            int y = rowY + 32;
            ctx.fill(wx, y, wx + BAR_W, y + BAR_H, 0xFF15151E);

            int filled = (int) (BAR_W * fraction());
            ctx.fill(wx, y, wx + filled, y + BAR_H, 0xFF4A7FB5);

            int knobX = wx + Math.min(BAR_W - 6, Math.max(0, filled - 3));
            ctx.fill(knobX, y - 4, knobX + 6, y + BAR_H + 4, 0xFFDCDCE8);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int x, int y, int width) {
            if (mx >= x && mx < x + CONTROL_W && my >= y + 22 && my < y + MIN_HEIGHT) {
                mouseDragged(mx, x, width);
                return true;
            }
            return false;
        }

        @Override
        public void mouseDragged(double mx, int x, int width) {
            int barX = x + 8;
            float p = (float) ((mx - barX) / BAR_W);
            setter.accept(quantise(min + (max - min) * Math.max(0.0f, Math.min(1.0f, p))));
        }
    }

    public static class IntOption extends ConfigEntry {
        private static final int BAR_W = 92;
        private static final int BAR_H = 4;

        private final IntSupplier getter;
        private final IntConsumer setter;
        private final int min;
        private final int max;
        private final String suffix;

        public IntOption(String name, String description, int min, int max, String suffix,
                         IntSupplier getter, IntConsumer setter) {
            super(name, description);
            this.min = min;
            this.max = max;
            this.suffix = suffix;
            this.getter = getter;
            this.setter = setter;
        }

        private int clamp(int v) {
            return Math.max(min, Math.min(max, v));
        }

        @Override
        protected void renderWidget(GuiGraphicsExtractor ctx, Font tr, int wx, int rowY, int ww, int mouseX, int mouseY) {
            ctx.text(tr, Component.literal(clamp(getter.getAsInt()) + suffix), wx, rowY + 19, COLOUR_VALUE, false);

            int y = rowY + 32;
            ctx.fill(wx, y, wx + BAR_W, y + BAR_H, 0xFF15151E);

            float fraction = max == min ? 0.0f : (float) (clamp(getter.getAsInt()) - min) / (max - min);
            int filled = (int) (BAR_W * fraction);
            ctx.fill(wx, y, wx + filled, y + BAR_H, 0xFF4A7FB5);

            int knobX = wx + Math.min(BAR_W - 6, Math.max(0, filled - 3));
            ctx.fill(knobX, y - 4, knobX + 6, y + BAR_H + 4, 0xFFDCDCE8);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int x, int y, int width) {
            if (mx >= x && mx < x + CONTROL_W && my >= y + 22 && my < y + MIN_HEIGHT) {
                mouseDragged(mx, x, width);
                return true;
            }
            return false;
        }

        @Override
        public void mouseDragged(double mx, int x, int width) {
            int barX = x + 8;
            float p = (float) ((mx - barX) / BAR_W);
            p = Math.max(0.0f, Math.min(1.0f, p));
            setter.accept(clamp(Math.round(min + (max - min) * p)));
        }
    }

    public static class ChoiceOption extends ConfigEntry {
        private static final int BOX_H = 14;

        private final List<String> labels;
        private final IntSupplier getter;
        private final IntConsumer setter;

        public ChoiceOption(String name, String description, List<String> labels,
                            IntSupplier getter, IntConsumer setter) {
            super(name, description);
            this.labels = List.copyOf(labels);
            this.getter = getter;
            this.setter = setter;
        }

        private int index() {
            int i = getter.getAsInt();
            if (labels.isEmpty()) {
                return 0;
            }
            return ((i % labels.size()) + labels.size()) % labels.size();
        }

        @Override
        protected void renderWidget(GuiGraphicsExtractor ctx, Font tr, int wx, int rowY, int ww, int mouseX, int mouseY) {
            int y = rowY + 24;
            boolean hovered = mouseX >= wx && mouseX < wx + ww && mouseY >= y && mouseY < y + BOX_H;

            ctx.fill(wx, y, wx + ww, y + BOX_H, hovered ? 0xFF41415A : 0xFF32324A);
            RenderCompat.outline(ctx, wx, y, ww, BOX_H, 0xFF5C5C7C);

            ctx.text(tr, Component.literal("◀"), wx + 3, y + 3, 0xFF9A9AAC, false);
            ctx.text(tr, Component.literal("▶"), wx + ww - 9, y + 3, 0xFF9A9AAC, false);

            String label = labels.isEmpty() ? "-" : labels.get(index());
            label = tr.plainSubstrByWidth(label, ww - 26);
            RenderCompat.centeredText(ctx, tr, Component.literal(label), wx + ww / 2, y + 3, COLOUR_VALUE);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int x, int y, int width) {
            int wx = x + 8;
            int ww = CONTROL_W - 16;
            int by = y + 24;
            if (labels.isEmpty() || mx < wx || mx >= wx + ww || my < by || my >= by + BOX_H) {
                return false;
            }
            boolean backwards = mx < wx + ww / 3;
            int next = index() + (backwards ? -1 : 1);
            setter.accept(((next % labels.size()) + labels.size()) % labels.size());
            return true;
        }

        @Override
        public boolean matchesSearch(String lowercaseQuery) {
            if (super.matchesSearch(lowercaseQuery)) {
                return true;
            }
            for (String label : labels) {
                if (label.toLowerCase(Locale.ROOT).contains(lowercaseQuery)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class ActionOption extends ConfigEntry {
        private static final int BTN_H = 14;

        private final String label;
        private final Runnable action;

        public ActionOption(String name, String description, String label, Runnable action) {
            super(name, description);
            this.label = label;
            this.action = action;
        }

        @Override
        protected void renderWidget(GuiGraphicsExtractor ctx, Font tr, int wx, int rowY, int ww, int mouseX, int mouseY) {
            int y = rowY + 24;
            boolean hovered = mouseX >= wx && mouseX < wx + ww && mouseY >= y && mouseY < y + BTN_H;

            ctx.fill(wx, y, wx + ww, y + BTN_H, hovered ? 0xFF41415A : 0xFF32324A);
            RenderCompat.outline(ctx, wx, y, ww, BTN_H, 0xFF5C5C7C);
            RenderCompat.centeredText(ctx, tr, Component.literal(label), wx + ww / 2, y + 3, 0xFFE6E6F0);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int x, int y, int width) {
            int wx = x + 8;
            int ww = CONTROL_W - 16;
            int by = y + 24;
            if (mx >= wx && mx < wx + ww && my >= by && my < by + BTN_H) {
                action.run();
                return true;
            }
            return false;
        }
    }

    public static class SectionHeader extends ConfigEntry {
        public SectionHeader(String name) {
            super(name, "");
        }

        @Override
        public int height(Font tr, int width) {
            return 18;
        }

        @Override
        public void render(GuiGraphicsExtractor ctx, Font tr, int x, int y, int width, int mouseX, int mouseY) {
            ctx.text(tr, Component.literal("▼ " + name), x + 2, y + 6, 0xFFD8D8E4, false);
            ctx.fill(x + 2, y + 16, x + width - 2, y + 17, 0xFF3A3A4C);
        }

        @Override
        protected void renderWidget(GuiGraphicsExtractor ctx, Font tr, int wx, int rowY, int ww, int mouseX, int mouseY) {
        }

        @Override
        public boolean isHeader() {
            return true;
        }

        @Override
        public boolean matchesSearch(String lowercaseQuery) {
            return false;
        }
    }
}
