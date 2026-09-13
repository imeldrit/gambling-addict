package com.eldrit.gamblingaddict.config.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public abstract class ConfigEntry {
    public static final int CONTROL_W = 122;
    public static final int DESC_X = CONTROL_W + 6;
    public static final int MIN_HEIGHT = 48;

    protected static final int COLOUR_NAME = 0xFFE6E6F0;
    protected static final int COLOUR_DESC = 0xFF9A9AAC;
    protected static final int COLOUR_ROW = 0xFF23232F;
    protected static final int COLOUR_ROW_HOVER = 0xFF2E2E3D;
    protected static final int COLOUR_VALUE = 0xFF8FB8E0;

    public final String name;
    public final String description;

    protected ConfigEntry(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public int height(Font tr, int width) {
        int lines = wrap(tr, width).size();
        return Math.max(MIN_HEIGHT, 16 + lines * 10);
    }

    protected List<FormattedCharSequence> wrap(Font tr, int width) {
        return tr.split(Component.literal(description), Math.max(20, width - DESC_X - 10));
    }

    public void render(GuiGraphicsExtractor ctx, Font tr, int x, int y, int width, int mouseX, int mouseY) {
        int h = height(tr, width);
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + h;
        ctx.fill(x, y, x + width, y + h, hovered ? COLOUR_ROW_HOVER : COLOUR_ROW);

        ctx.text(tr, Component.literal(name), x + 8, y + 7, COLOUR_NAME, false);

        int ly = y + 7;
        for (FormattedCharSequence line : wrap(tr, width)) {
            ctx.text(tr, line, x + DESC_X, ly, COLOUR_DESC, false);
            ly += 10;
        }

        renderWidget(ctx, tr, x + 8, y, CONTROL_W - 16, mouseX, mouseY);
    }

    protected abstract void renderWidget(GuiGraphicsExtractor ctx, Font tr, int wx, int rowY, int ww, int mouseX, int mouseY);

    public boolean mouseClicked(double mx, double my, int x, int y, int width) {
        return false;
    }

    public void mouseDragged(double mx, int x, int width) {
    }

    public boolean matchesSearch(String lowercaseQuery) {
        return name.toLowerCase(Locale.ROOT).contains(lowercaseQuery)
                || description.toLowerCase(Locale.ROOT).contains(lowercaseQuery);
    }

    public boolean isHeader() {
        return false;
    }
}
