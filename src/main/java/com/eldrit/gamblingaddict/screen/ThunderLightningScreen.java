package com.eldrit.gamblingaddict.screen;

import com.eldrit.gamblingaddict.anim.GambleSession;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.loot.Drop;
import com.eldrit.gamblingaddict.render.RenderCompat;
import com.eldrit.gamblingaddict.util.DropIcons;
import com.eldrit.gamblingaddict.util.ProfitCalculator;
import com.eldrit.gamblingaddict.util.SoundUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public class ThunderLightningScreen extends AnimationScreen {
    private static final Identifier ELDER = Identifier.withDefaultNamespace("textures/entity/guardian/guardian_elder.png");

    private static final int GHOST_TICKS = 30;
    private static final int BOOK_IN_TICKS = 18;
    private static final int STRIKE1 = GHOST_TICKS + BOOK_IN_TICKS + 12;
    private static final int STRIKE_GAP = 24;
    private static final int FLASH_TICKS = 7;
    private static final int BURN_TICKS = 34;
    private static final int SETTLE_TICKS = 18;

    private static final float BOOK_SCALE = 3.2f;
    private static final int BOLT_SEGMENTS = 14;

    private static final int BOLT_CORE = 0xFFFFFFFF;
    private static final int BOLT_BLUE = 0xFF7FD0FF;
    private static final int BOLT_RED = 0xFFFF3A3A;
    private static final int BOLT_RED_DEEP = 0xFFB00000;

    private final ItemStack book = DropIcons.plainBook();
    private ItemStack revealed = null;

    private boolean rumbled = false;
    private boolean bookPlaced = false;
    private int needed = 0;
    private int strikesDone = 0;
    private boolean outcomePlayed = false;
    private boolean ashPlayed = false;
    private final int seed = (int) (System.nanoTime() & 0x7FFFFFFF);

    public ThunderLightningScreen(GambleSession session) {
        super(Component.literal("Thunder"), session);
        ProfitCalculator.prefetch(session.wonDrop());
    }

    @Override
    protected float speedMultiplier() {
        return Math.max(0.1f, ModConfig.get().thunderSpeed);
    }

    private float volume() {
        return ModConfig.get().thunderVolume;
    }

    private Drop drop() {
        return won() ? session.wonDrop() : null;
    }

    private int strikesNeeded() {
        Drop d = drop();
        if (d == null) {
            return 1;
        }
        return d.isHeadline() ? 3 : 2;
    }

    private int lastStrikeTick() {
        return STRIKE1 + (needed - 1) * STRIKE_GAP;
    }

    private int endTick() {
        ModConfig cfg = ModConfig.get();
        float hold = needed == 1 ? cfg.loseBannerSeconds + 0.6f : (needed == 2 ? cfg.anvilBannerSeconds : cfg.jackpotHoldSeconds);
        int after = needed == 1 ? BURN_TICKS : SETTLE_TICKS;
        return lastStrikeTick() + after + (int) (hold * 20.0f);
    }

    private boolean finished(float t) {
        return needed > 0 && strikesDone == needed && t >= lastStrikeTick() + 4;
    }

    private float strikeTime(int index) {
        return STRIKE1 + index * STRIKE_GAP;
    }

    @Override
    protected void onAnimationTick(float t) {
        if (!rumbled && t >= 2.0f) {
            rumbled = true;
            SoundUtil.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, volume() * 0.7f);
            SoundUtil.play(SoundEvents.ELDER_GUARDIAN_CURSE, 1.0f, volume() * 0.5f);
        }
        if (!bookPlaced && t >= GHOST_TICKS) {
            bookPlaced = true;
            SoundUtil.play(SoundEvents.ITEM_PICKUP, 0.8f, volume() * 0.6f);
        }

        if (needed == 0 && t >= STRIKE1) {
            revealNow();
            needed = strikesNeeded();
        }
        if (needed > 0 && strikesDone < needed && t >= strikeTime(strikesDone)) {
            strikesDone++;
            boolean last = strikesDone == needed;
            boolean red = last && needed == 3;
            SoundUtil.play(SoundEvents.LIGHTNING_BOLT_IMPACT, red ? 0.6f : 1.0f, volume());
            SoundUtil.play(SoundEvents.LIGHTNING_BOLT_THUNDER, red ? 0.5f : 1.1f, volume() * (red ? 1.0f : 0.6f));
            if (red) {
                SoundUtil.play(SoundEvents.ANVIL_LAND, 0.5f, volume());
                SoundUtil.play(SoundEvents.NOTE_BLOCK_BASS, 0.5f, volume());
                SoundUtil.play(SoundEvents.GENERIC_EXPLODE, 0.6f, volume() * 0.7f);
            }
        }

        if (finished(t) && !outcomePlayed) {
            outcomePlayed = true;
            Drop d = drop();
            if (needed == 1) {
                SoundUtil.play(SoundEvents.FIRE_AMBIENT, 1.0f, volume() * 0.8f);
            } else if (needed == 2) {
                revealed = DropIcons.of(d, true);
                SoundUtil.play(SoundEvents.ENCHANTMENT_TABLE_USE, 1.2f, volume());
                SoundUtil.play(SoundEvents.PLAYER_LEVELUP, 1.0f, volume());
            } else {
                revealed = DropIcons.of(d, true);
                SoundUtil.play(SoundEvents.GLASS_BREAK, 0.7f, volume());
                SoundUtil.play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, volume());
                SoundUtil.playWinSound(true, 1.0f);
            }
        }
        if (needed == 1 && outcomePlayed && !ashPlayed && t >= lastStrikeTick() + BURN_TICKS - 6) {
            ashPlayed = true;
            SoundUtil.play(SoundEvents.FIRE_EXTINGUISH, 1.0f, volume());
        }

        if (needed > 0 && t >= endTick()) {
            finishAndClose();
        }
    }

    @Override
    protected void renderAnimation(GuiGraphicsExtractor ctx, float t, float delta) {
        int cx = this.width / 2;
        int cy = this.height / 2;

        drawStorm(ctx, t);
        drawGhost(ctx, cx, cy, t);

        int shakeX = 0;
        int shakeY = 0;
        float sinceStrike = strikesDone == 0 ? 99.0f : t - strikeTime(strikesDone - 1);
        boolean redStrike = strikesDone == 3;
        if (sinceStrike < 8.0f) {
            float s = (1.0f - sinceStrike / 8.0f) * (redStrike ? 8.0f : 4.0f);
            shakeX = (int) (Math.sin(t * 11.0f) * s);
            shakeY = (int) (Math.cos(t * 8.0f) * s);
        }

        int bookX = cx + shakeX;
        int bookY = cy + 10 + shakeY;
        drawPedestal(ctx, bookX, bookY);

        if (t >= GHOST_TICKS) {
            drawBookState(ctx, bookX, bookY, t);
        }

        if (sinceStrike < FLASH_TICKS) {
            drawBolt(ctx, bookX, bookY - (int) (8 * BOOK_SCALE), sinceStrike, redStrike);
        }

        drawTitle(ctx, cx, cy, t);
    }

    private void drawStorm(GuiGraphicsExtractor ctx, float t) {
        ctx.fill(0, 0, this.width, this.height / 3, RenderCompat.withAlpha(0x0A1424, 0.6f));
        float rumble = 0.05f + 0.05f * (float) Math.sin(t * 0.23f);
        ctx.fill(0, 0, this.width, this.height, RenderCompat.withAlpha(0x203050, rumble));
        for (int i = 0; i < 40; i++) {
            float phase = (t * 0.06f + hash(i * 7)) % 1.0f;
            int x = (int) (hash(i * 13) * this.width) - (int) (phase * 20.0f);
            int y = (int) (phase * this.height);
            ctx.fill(x, y, x + 1, y + 6, RenderCompat.withAlpha(0x8FB0D8, 0.35f));
        }
    }

    private void drawGhost(GuiGraphicsExtractor ctx, int cx, int cy, float t) {
        if (t >= GHOST_TICKS + 10) {
            return;
        }
        float a;
        if (t < 8.0f) {
            a = t / 8.0f;
        } else if (t < GHOST_TICKS - 6) {
            a = 1.0f;
        } else {
            a = clamp01(1.0f - (t - (GHOST_TICKS - 6)) / 16.0f);
        }
        float pulse = 0.55f + 0.15f * (float) Math.sin(t * 0.7f);
        int tint = RenderCompat.withAlpha(0xC8E8F8, a * pulse * 0.55f);
        int s = 9;
        int headX = cx - 6 * s;
        int headY = cy - 6 * s - 30;
        RenderCompat.blitRegion(ctx, ELDER, headX, headY, 12 * s, 12 * s, 16, 16, 12, 12, 64, 64, tint);
        int eyeTint = RenderCompat.withAlpha(0xFFFFFF, a * 0.9f);
        RenderCompat.blitRegion(ctx, ELDER, headX + 5 * s, headY + 5 * s, 2 * s, 2 * s, 9, 1, 2, 2, 64, 64, eyeTint);
        for (int i = 0; i < 12; i++) {
            float ang = (float) (i * Math.PI / 6.0 + t * 0.03f);
            int px = cx + (int) (Math.cos(ang) * 7.5f * s);
            int py = headY + 6 * s + (int) (Math.sin(ang) * 7.5f * s);
            ctx.fill(px - 2, py - 2, px + 2, py + 2, RenderCompat.withAlpha(0xC8E8F8, a * 0.4f));
        }
    }

    private void drawPedestal(GuiGraphicsExtractor ctx, int cx, int cy) {
        int top = cy + (int) (8 * BOOK_SCALE) + 2;
        ctx.fill(cx - 40, top, cx + 40, top + 8, 0xFF2A2A34);
        ctx.fill(cx - 34, top + 8, cx + 34, top + 22, 0xFF1C1C24);
        ctx.fill(cx - 40, top, cx + 40, top + 2, 0xFF4A4A58);
    }

    private void drawBookState(GuiGraphicsExtractor ctx, int cx, int cy, float t) {
        float in = easeOutCubic(clamp01((t - GHOST_TICKS) / BOOK_IN_TICKS));
        float y = cy + (1.0f - in) * -30.0f;

        if (!finished(t)) {
            float charge = strikesDone == 0 ? 0.0f : clamp01(strikesDone / 2.0f);
            if (charge > 0.0f) {
                float glow = charge * (0.3f + 0.15f * (float) Math.sin(t * 0.8f));
                ctx.fill(cx - 30, (int) y - 30, cx + 30, (int) y + 30, RenderCompat.withAlpha(0x7FD0FF, glow));
            }
            RenderCompat.drawScaledItem(ctx, book, cx, y, BOOK_SCALE);
            return;
        }

        float since = t - (lastStrikeTick() + 4);
        if (needed == 1) {
            drawBurning(ctx, cx, y, since);
            return;
        }
        if (needed == 2) {
            float sparkle = clamp01(1.0f - since / 24.0f);
            for (int i = 0; i < 16; i++) {
                float ang = (float) (i * Math.PI / 8.0);
                float dist = 14.0f + since * 1.6f + hash(i) * 8.0f;
                int px = cx + (int) (Math.cos(ang) * dist);
                int py = (int) y + (int) (Math.sin(ang) * dist);
                ctx.fill(px, py, px + 2, py + 2, RenderCompat.withAlpha(0x7FD0FF, sparkle));
            }
            RenderCompat.drawScaledItem(ctx, revealed != null ? revealed : book, cx, y, BOOK_SCALE);
            return;
        }
        drawShatter(ctx, cx, y, since);
    }

    private void drawBurning(GuiGraphicsExtractor ctx, int cx, float y, float since) {
        float burn = clamp01(since / BURN_TICKS);
        if (burn < 0.75f) {
            float dark = 1.0f - burn * 0.6f;
            RenderCompat.drawScaledItem(ctx, book, cx, y, BOOK_SCALE * (1.0f - burn * 0.25f));
            int scorch = RenderCompat.withAlpha(0x000000, burn * 0.7f);
            ctx.fill(cx - 22, (int) y - 22, cx + 22, (int) y + 22, scorch);
            for (int i = 0; i < 22; i++) {
                float phase = (since * 0.09f + hash(i * 3)) % 1.0f;
                int px = cx - 18 + (int) (hash(i * 17) * 36.0f);
                int py = (int) y + 18 - (int) (phase * 40.0f);
                int rgb = phase < 0.4f ? 0xFFE08A : (phase < 0.7f ? 0xFF8A2A : 0xFF3A1A);
                ctx.fill(px, py, px + 3, py + 3, RenderCompat.withAlpha(rgb, (1.0f - phase) * dark));
            }
        }
        if (burn > 0.5f) {
            float ash = clamp01((burn - 0.5f) / 0.5f);
            for (int i = 0; i < 28; i++) {
                float fall = (ash + hash(i * 5) * 0.4f) % 1.0f;
                int px = cx - 20 + (int) (hash(i * 11) * 40.0f) + (int) (Math.sin(since * 0.2f + i) * 3.0f);
                int py = (int) y - 10 + (int) (fall * 46.0f);
                ctx.fill(px, py, px + 2, py + 2, RenderCompat.withAlpha(0x8A8A8A, (1.0f - fall) * 0.9f));
            }
            int pile = (int) (ash * 6.0f);
            ctx.fill(cx - 24, (int) y + 24 - pile, cx + 24, (int) y + 26, 0xFF3A3A3A);
        }
    }

    private void drawShatter(GuiGraphicsExtractor ctx, int cx, float y, float since) {
        float p = clamp01(since / 18.0f);
        for (int i = 0; i < 20; i++) {
            float ang = (float) (i * Math.PI / 10.0 + hash(seed + i) * 0.3f);
            float dist = 6.0f + p * (30.0f + hash(i * 7) * 40.0f);
            int px = cx + (int) (Math.cos(ang) * dist);
            int py = (int) y + (int) (Math.sin(ang) * dist) + (int) (p * p * 20.0f);
            int size = hash(i * 3) > 0.5f ? 4 : 3;
            ctx.fill(px, py, px + size, py + size, RenderCompat.withAlpha(0xC8A060, 1.0f - p));
        }
        float in = easeOutCubic(clamp01((since - 4.0f) / 12.0f));
        if (in <= 0.0f) {
            return;
        }
        float aura = 0.35f + 0.2f * (float) Math.sin(since * 0.4f);
        ctx.fill(cx - 34, (int) y - 34, cx + 34, (int) y + 34, RenderCompat.withAlpha(0xFF3A3A, aura * 0.45f));
        for (int wave = 0; wave < 3; wave++) {
            float wp = ((since * 0.05f + wave * 0.33f) % 1.0f);
            RenderCompat.ring(ctx, cx, (int) y, (int) (28 + wp * 90), 2, RenderCompat.withAlpha(0xFF6A6A, (1.0f - wp) * 0.5f));
        }
        float bob = (float) Math.sin(since * 0.18f) * 3.0f;
        RenderCompat.drawScaledItem(ctx, revealed != null ? revealed : book, cx, y - 6 + bob, BOOK_SCALE * in);
    }

    private void drawBolt(GuiGraphicsExtractor ctx, int targetX, int targetY, float since, boolean red) {
        float fade = 1.0f - since / FLASH_TICKS;
        int flashRgb = red ? 0xFF2020 : 0xE0F0FF;
        ctx.fill(0, 0, this.width, this.height, RenderCompat.withAlpha(flashRgb, fade * (red ? 0.55f : 0.35f)));

        int boltSeed = seed + strikesDone * 1000;
        int width = red ? 4 : 2;
        int glow = red ? BOLT_RED_DEEP : BOLT_BLUE;
        int core = red ? BOLT_RED : BOLT_CORE;

        for (int branch = 0; branch < (red ? 3 : 2); branch++) {
            int x = targetX + (int) ((hash(boltSeed + branch * 77) - 0.5f) * 120.0f);
            int y = 0;
            int stepY = targetY / BOLT_SEGMENTS;
            for (int i = 0; i < BOLT_SEGMENTS; i++) {
                int nx = (i == BOLT_SEGMENTS - 1) ? targetX : x + (int) ((hash(boltSeed + branch * 31 + i * 7) - 0.5f) * 34.0f)
                        + (targetX - x) / (BOLT_SEGMENTS - i);
                int ny = y + stepY;
                drawSegment(ctx, x, y, nx, ny, width + 2, RenderCompat.withAlpha(glow & 0xFFFFFF, fade * 0.6f));
                drawSegment(ctx, x, y, nx, ny, width, RenderCompat.withAlpha(core & 0xFFFFFF, fade));
                x = nx;
                y = ny;
            }
        }
        int r = (int) (10 + (1.0f - fade) * 40.0f);
        RenderCompat.ring(ctx, targetX, targetY, r, 2, RenderCompat.withAlpha(core & 0xFFFFFF, fade * 0.8f));
    }

    private void drawSegment(GuiGraphicsExtractor ctx, int x0, int y0, int x1, int y1, int width, int argb) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps == 0) {
            return;
        }
        for (int i = 0; i <= steps; i += 2) {
            int x = x0 + (x1 - x0) * i / steps;
            int y = y0 + (y1 - y0) * i / steps;
            ctx.fill(x - width / 2, y - width / 2, x + width / 2 + 1, y + width / 2 + 1, argb);
        }
    }

    private void drawTitle(GuiGraphicsExtractor ctx, int cx, int cy, float t) {
        int top = cy - 104;

        if (!finished(t)) {
            float in = easeOutCubic(clamp01(t / 10.0f));
            String label = t < GHOST_TICKS ? "THUNDER" : "THE STORM JUDGES";
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal(label).withStyle(ChatFormatting.BOLD), cx, top, 1.5f * in, 0xFF8FD0FF);
            return;
        }

        float since = t - (lastStrikeTick() + 4);
        float in = easeOutCubic(clamp01(since / 10.0f));
        Drop drop = drop();
        int lineY = cy + 62;

        if (needed == 1) {
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal("BURNT TO ASH").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    cx, top, 1.0f + in * 0.4f, 0xFFB05868);
            if (in >= 0.95f) {
                RenderCompat.centeredText(ctx, this.font,
                        Component.literal("One strike. No drop this time.").withStyle(ChatFormatting.DARK_GRAY),
                        cx, lineY, 0xFF6E6E7A);
            }
            return;
        }
        if (needed == 2 && drop != null) {
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal("CHARGED!").withStyle(ChatFormatting.BOLD), cx, top, 1.2f + in * 0.5f, 0xFF8FD0FF);
            if (in >= 0.95f) {
                RenderCompat.centeredText(ctx, this.font,
                        Component.literal("YOU WON! 1x " + drop.displayName() + (session.isLootShared() ? " (Loot Share)" : ""))
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), cx, lineY, 0xFF9FE0FF);
                ProfitCalculator.render(ctx, this.font, drop, cx, lineY + 14, since, false);
            }
            return;
        }
        if (drop != null) {
            float pulse = 0.7f + 0.3f * (float) Math.sin(since * 0.45f);
            int b = (int) (255 * pulse);
            int col = 0xFF000000 | (b << 16) | ((int) (b * 0.25f) << 8) | (int) (b * 0.25f);
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal(drop.displayName().toUpperCase(Locale.ROOT) + "!").withStyle(ChatFormatting.BOLD),
                    cx, top, 1.2f + in * 0.9f, col);
            if (in >= 0.95f) {
                RenderCompat.centeredText(ctx, this.font,
                        Component.literal("YOU WON! 1x " + drop.displayName() + (session.isLootShared() ? " (Loot Share)" : ""))
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), cx, lineY, 0xFFFF8A8A);
                ProfitCalculator.render(ctx, this.font, drop, cx, lineY + 14, since, true);
            }
        }
    }

    private static float hash(int seed) {
        int h = seed * 374761393 + 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0f;
    }
}
