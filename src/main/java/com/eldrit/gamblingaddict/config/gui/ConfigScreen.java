package com.eldrit.gamblingaddict.config.gui;

import com.eldrit.gamblingaddict.render.RenderCompat;
import com.eldrit.gamblingaddict.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConfigScreen extends Screen {
    private static final int PANEL_BG = 0xF01A1A24;
    private static final int PANEL_BORDER = 0xFF6E6E8A;
    private static final int SIDEBAR_BG = 0xFF14141C;
    private static final int TITLE_BG = 0xFF20202C;
    private static final int CAT_SELECTED = 0xFFFFD24A;
    private static final int CAT_NORMAL = 0xFFB9B9C9;
    private static final int CAT_HOVER = 0xFFE6E6F0;
    private static final int SEARCH_BG = 0xFF0E0E14;

    private static final int SIDEBAR_W = 132;
    private static final int TITLE_H = 26;
    private static final int HEADER_H = 20;
    private static final int SEARCH_W = 118;
    private static final int SEARCH_H = 14;
    private static final int PAD = 8;

    private List<ConfigCategory> categories = new ArrayList<>();
    private int selected = 0;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;

    private double scroll = 0.0;
    private String search = "";
    private boolean searchFocused = false;
    private int caretTimer = 0;

    private @Nullable ConfigEntry dragging = null;

    public ConfigScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("gamblingaddict.config.title"));
    }

    @Override
    protected void init() {
        super.init();
        categories = ConfigManager.build();
        selected = Math.min(selected, Math.max(0, categories.size() - 1));

        panelW = Math.min(this.width - 40, 640);
        panelH = Math.min(this.height - 40, 360);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        contentX = panelX + PAD + SIDEBAR_W + 6;
        contentY = panelY + TITLE_H + HEADER_H;
        contentW = panelX + panelW - PAD - contentX;
        contentH = panelY + panelH - PAD - contentY;
    }

    private List<ConfigEntry> visibleEntries() {
        if (search.isEmpty()) {
            return categories.isEmpty() ? List.of() : categories.get(selected).entries;
        }
        String q = search.toLowerCase(Locale.ROOT);
        List<ConfigEntry> out = new ArrayList<>();
        for (ConfigCategory category : categories) {
            for (ConfigEntry entry : category.entries) {
                if (entry.matchesSearch(q)) {
                    out.add(entry);
                }
            }
        }
        return out;
    }

    private int totalContentHeight() {
        int total = 0;
        for (ConfigEntry entry : visibleEntries()) {
            total += entry.height(this.font, contentW - 10) + 3;
        }
        return total;
    }

    private double maxScroll() {
        return Math.max(0.0, totalContentHeight() - contentH);
    }

    private void dimBackground(GuiGraphicsExtractor ctx) {
        ctx.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        dimBackground(ctx);
        caretTimer++;

        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        RenderCompat.outline(ctx, panelX, panelY, panelW, panelH, PANEL_BORDER);

        ctx.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + TITLE_H, TITLE_BG);
        ctx.text(this.font,
                Component.literal("Gambling Addict").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                        .append(Component.literal("  by eldrit").withStyle(ChatFormatting.DARK_GRAY)),
                panelX + PAD + 2, panelY + 9, 0xFFFFD24A, false);
        drawSearchBox(ctx);

        drawSidebar(ctx, mouseX, mouseY);
        drawContentHeader(ctx, mouseX, mouseY);
        drawEntries(ctx, mouseX, mouseY);
        drawScrollbar(ctx);
    }

    private void drawSidebar(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        int x = panelX + PAD;
        int y = panelY + TITLE_H + 4;
        int h = panelY + panelH - PAD - y;

        ctx.fill(x, y, x + SIDEBAR_W, y + h, SIDEBAR_BG);
        RenderCompat.outline(ctx, x, y, SIDEBAR_W, h, 0xFF32324A);

        ctx.text(this.font, Component.literal("Categories").withStyle(ChatFormatting.BOLD),
                x + 8, y + 7, 0xFFD8D8E4, false);
        ctx.fill(x + 6, y + 19, x + SIDEBAR_W - 6, y + 20, 0xFF32324A);

        int ry = y + 26;
        for (int i = 0; i < categories.size(); i++) {
            boolean isSelected = i == selected && search.isEmpty();
            boolean hovered = mouseX >= x && mouseX < x + SIDEBAR_W && mouseY >= ry && mouseY < ry + 15;

            if (isSelected) {
                ctx.fill(x + 2, ry - 2, x + SIDEBAR_W - 2, ry + 13, 0xFF2C2C40);
                ctx.fill(x + 2, ry - 2, x + 4, ry + 13, CAT_SELECTED);
            }
            int colour = isSelected ? CAT_SELECTED : (hovered ? CAT_HOVER : CAT_NORMAL);
            ctx.text(this.font, Component.literal(categories.get(i).name), x + 10, ry + 1, colour, false);
            ry += 16;
        }
    }

    private void drawContentHeader(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        int y = panelY + TITLE_H + 8;

        String blurb = search.isEmpty()
                ? (categories.isEmpty() ? "" : categories.get(selected).blurb)
                : visibleEntries().size() + " setting(s) matching \"" + search + "\"";
        ctx.text(this.font, Component.literal(blurb), contentX + 2, y - 2, 0xFFB0B0C0, false);
    }

    private int searchBoxX() {
        return panelX + panelW - PAD - SEARCH_W;
    }

    private int searchBoxY() {
        return panelY + (TITLE_H - SEARCH_H) / 2 + 1;
    }

    private void drawSearchBox(GuiGraphicsExtractor ctx) {
        int boxW = SEARCH_W;
        int boxX = searchBoxX();
        int boxY = searchBoxY();
        ctx.fill(boxX, boxY, boxX + boxW, boxY + SEARCH_H, SEARCH_BG);
        RenderCompat.outline(ctx, boxX, boxY, boxW, SEARCH_H, searchFocused ? CAT_SELECTED : 0xFF3A3A4C);

        int gx = boxX + 4;
        int gy = boxY + 4;
        RenderCompat.outline(ctx, gx, gy, 6, 6, 0xFF9A9AAC);
        ctx.fill(gx + 6, gy + 6, gx + 9, gy + 8, 0xFF9A9AAC);

        String shown = search.isEmpty() && !searchFocused ? "Search..." : search;
        int textColour = search.isEmpty() && !searchFocused ? 0xFF66667A : 0xFFE6E6F0;
        ctx.text(this.font, Component.literal(shown), gx + 12, boxY + 3, textColour, false);

        if (searchFocused && (caretTimer / 6) % 2 == 0) {
            int caretX = gx + 12 + this.font.width(search);
            ctx.fill(caretX, boxY + 2, caretX + 1, boxY + 12, 0xFFE6E6F0);
        }
    }

    private void drawEntries(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        ctx.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);

        int rowW = contentW - 10;
        int y = contentY - (int) scroll;

        for (ConfigEntry entry : visibleEntries()) {
            int h = entry.height(this.font, rowW);
            if (y + h >= contentY && y <= contentY + contentH) {
                entry.render(ctx, this.font, contentX, y, rowW, mouseX, mouseY);
            }
            y += h + 3;
        }

        ctx.disableScissor();
    }

    private void drawScrollbar(GuiGraphicsExtractor ctx) {
        double max = maxScroll();
        int trackX = contentX + contentW - 6;
        ctx.fill(trackX, contentY, trackX + 4, contentY + contentH, 0xFF14141C);
        if (max <= 0.0) {
            return;
        }
        int total = totalContentHeight();
        int thumbH = Math.max(16, (int) ((double) contentH / total * contentH));
        int thumbY = contentY + (int) ((scroll / max) * (contentH - thumbH));
        ctx.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xFF5C5C7C);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int sx = panelX + PAD;
        int ry = panelY + TITLE_H + 4 + 26;
        for (int i = 0; i < categories.size(); i++) {
            if (mouseX >= sx && mouseX < sx + SIDEBAR_W && mouseY >= ry - 2 && mouseY < ry + 13) {
                selected = i;
                search = "";
                searchFocused = false;
                scroll = 0.0;
                return true;
            }
            ry += 16;
        }

        int boxX = searchBoxX();
        int boxY = searchBoxY();
        searchFocused = mouseX >= boxX && mouseX < boxX + SEARCH_W && mouseY >= boxY && mouseY < boxY + SEARCH_H;
        if (searchFocused) {
            return true;
        }

        if (mouseX >= contentX && mouseX < contentX + contentW
                && mouseY >= contentY && mouseY < contentY + contentH) {
            int rowW = contentW - 10;
            int y = contentY - (int) scroll;
            for (ConfigEntry entry : visibleEntries()) {
                int h = entry.height(this.font, rowW);
                if (mouseY >= y && mouseY < y + h && entry.mouseClicked(mouseX, mouseY, contentX, y, rowW)) {
                    dragging = entry;
                    return true;
                }
                y += h + 3;
            }
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        double mouseX = event.x();
        if (dragging != null) {
            dragging.mouseDragged(mouseX, contentX, contentW - 10);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            ConfigManager.saveAll();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0.0, Math.min(maxScroll(), scroll - vertical * 16.0));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) {
                    search = search.substring(0, search.length() - 1);
                    scroll = 0.0;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        String chr = event.codepointAsString();
        if (searchFocused && !chr.isEmpty() && chr.charAt(0) >= ' ' && chr.charAt(0) != 127) {
            search += chr;
            scroll = 0.0;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void removed() {
        super.removed();
        ConfigManager.saveAll();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
