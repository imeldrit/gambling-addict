package com.eldrit.gamblingaddict.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class RenderCompat {
    private RenderCompat() {
    }

    public static void push(GuiGraphicsExtractor ctx) {
        ctx.pose().pushMatrix();
    }

    public static void pop(GuiGraphicsExtractor ctx) {
        ctx.pose().popMatrix();
    }

    public static void translate(GuiGraphicsExtractor ctx, float x, float y) {
        ctx.pose().translate(x, y);
    }

    public static void scale(GuiGraphicsExtractor ctx, float sx, float sy) {
        ctx.pose().scale(sx, sy);
    }

    public static void outline(GuiGraphicsExtractor ctx, int x, int y, int width, int height, int argb) {
        ctx.fill(x, y, x + width, y + 1, argb);
        ctx.fill(x, y + height - 1, x + width, y + height, argb);
        ctx.fill(x, y + 1, x + 1, y + height - 1, argb);
        ctx.fill(x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    public static void centeredText(GuiGraphicsExtractor ctx, Font font, Component text, int centerX, int y, int argb) {
        ctx.text(font, text, centerX - font.width(text) / 2, y, argb, true);
    }

    public static void drawScaledItem(GuiGraphicsExtractor ctx, ItemStack stack, float centerX, float centerY, float scale) {
        push(ctx);
        translate(ctx, centerX, centerY);
        scale(ctx, scale, scale);
        translate(ctx, -8.0f, -8.0f);
        ctx.item(stack, 0, 0);
        pop(ctx);
    }

    public static void drawScaledCenteredText(GuiGraphicsExtractor ctx, Font font, Component text,
                                              float centerX, float centerY, float scale, int argb) {
        push(ctx);
        translate(ctx, centerX, centerY);
        scale(ctx, scale, scale);
        centeredText(ctx, font, text, 0, -(font.lineHeight / 2), argb);
        pop(ctx);
    }

    public static void blitRegion(GuiGraphicsExtractor ctx, Identifier texture, int x, int y, int width, int height,
                                  float u, float v, int regionW, int regionH, int texW, int texH, int argb) {
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionW, regionH, texW, texH, argb);
    }

    private static final Identifier GLINT = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png");

    public static void drawGlintItem(GuiGraphicsExtractor ctx, ItemStack stack, float centerX, float centerY,
                                     float scale, float t, int haloRgb) {
        int half = (int) (8.0f * scale);
        int x0 = (int) centerX - half;
        int y0 = (int) centerY - half;
        int size = half * 2;

        float pulse = 0.55f + 0.25f * (float) Math.sin(t * 0.35f);
        for (int i = 3; i >= 1; i--) {
            int grow = i * 5;
            ctx.fill(x0 - grow, y0 - grow, x0 + size + grow, y0 + size + grow,
                    withAlpha(haloRgb, pulse * (0.10f - i * 0.02f)));
        }

        drawScaledItem(ctx, stack, centerX, centerY, scale);

        ctx.enableScissor(x0, y0, x0 + size, y0 + size);
        float u = (t * 1.7f) % 128.0f;
        float v = (t * 0.9f) % 128.0f;
        blitRegion(ctx, GLINT, x0 - size, y0 - size, size * 3, size * 3, u, v, 128, 128, 128, 128,
                withAlpha(0xC8A8FF, 0.45f));
        blitRegion(ctx, GLINT, x0 - size, y0 - size, size * 3, size * 3, 128.0f - u, v * 0.5f, 128, 128, 128, 128,
                withAlpha(0xFFFFFF, 0.22f));
        ctx.disableScissor();
    }

    public static void ring(GuiGraphicsExtractor ctx, int cx, int cy, int r, int thickness, int argb) {
        ctx.fill(cx - r, cy - r, cx + r, cy - r + thickness, argb);
        ctx.fill(cx - r, cy + r - thickness, cx + r, cy + r, argb);
        ctx.fill(cx - r, cy - r, cx - r + thickness, cy + r, argb);
        ctx.fill(cx + r - thickness, cy - r, cx + r, cy + r, argb);
    }

    public static int withAlpha(int rgb, float alpha) {
        int a = (int) (Math.max(0.0f, Math.min(1.0f, alpha)) * 255.0f);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
