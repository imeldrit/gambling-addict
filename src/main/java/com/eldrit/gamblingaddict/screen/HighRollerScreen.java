package com.eldrit.gamblingaddict.screen;

import com.eldrit.gamblingaddict.anim.GambleSession;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.render.RenderCompat;
import com.eldrit.gamblingaddict.loot.Drop;
import com.eldrit.gamblingaddict.util.DropIcons;
import com.eldrit.gamblingaddict.util.ProfitCalculator;
import com.eldrit.gamblingaddict.util.SoundUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public class HighRollerScreen extends AnimationScreen {
    private static final Drop[] STRIP = {
            Drop.GRIZZLY_BAIT,
            Drop.OVERFLUX_CAPACITOR,
            Drop.RED_CLAW_EGG,
            Drop.CELESTE_DYE,
    };

    private static final int INTRO_TICKS = 10;
    private static final int FAST_TICKS = 24;
    private static final int DECEL_TICKS = 60;
    private static final int SETTLED_TICK = INTRO_TICKS + FAST_TICKS + DECEL_TICKS;

    private static final float SPIN_SPEED = 0.9f;
    private static final int MIN_SPIN_DOWN = 6;
    private static final int TICK_INTERVAL = 2;

    private static final int CELL = 34;
    private static final int WINDOW_W = 74;
    private static final int WINDOW_H = 74;
    private static final float ICON_SCALE = 2.2f;

    private static final int CHASSIS = 0xFF161B2C;
    private static final int CHASSIS_LIGHT = 0xFF243354;
    private static final int BEZEL = 0xFF080B14;
    private static final int NEON = 0xFF34E0F0;
    private static final int NEON_HOT = 0xFFB44AE0;

    private final ItemStack[] icons = new ItemStack[STRIP.length];
    private final ItemStack[] foilIcons = new ItemStack[STRIP.length];

    private Float stopTarget = null;
    private boolean landedSounded = false;
    private int lastTickSoundAt = -99;
    private final int seed = (int) (System.nanoTime() & 0x7FFFFFFF);

    public HighRollerScreen(GambleSession session) {
        super(Component.literal("High Roller"), session);
        for (int i = 0; i < STRIP.length; i++) {
            icons[i] = DropIcons.of(STRIP[i]);
            foilIcons[i] = DropIcons.of(STRIP[i], true);
        }
    }

    @Override
    protected float speedMultiplier() {
        return Math.max(0.1f, ModConfig.get().highRollerSpeed);
    }

    private int resultTicks() {
        ModConfig cfg = ModConfig.get();
        return (int) ((isHeadline() ? cfg.jackpotHoldSeconds : cfg.loseBannerSeconds + 1.0f) * 20.0f);
    }

    private Drop landedDrop() {
        Drop won = won() ? session.wonDrop() : null;
        return won != null ? won : STRIP[stripIndexForLoss()];
    }

    private boolean isHeadline() {
        Drop d = won() ? session.wonDrop() : null;
        return d != null && d.isHeadline();
    }

    private int stripIndexForLoss() {
        int idx = (int) (hash(seed) * STRIP.length);
        for (int i = 0; i < STRIP.length; i++) {
            int c = (idx + i) % STRIP.length;
            if (!STRIP[c].isHeadline()) {
                return c;
            }
        }
        return 0;
    }

    private int targetIndex() {
        Drop drop = landedDrop();
        for (int i = 0; i < STRIP.length; i++) {
            if (STRIP[i] == drop) {
                return i;
            }
        }
        return stripIndexForLoss();
    }

    private float stopTarget() {
        if (stopTarget == null) {
            outcome();
            float atStop = (INTRO_TICKS + FAST_TICKS) * SPIN_SPEED;
            int candidate = (int) Math.ceil(atStop) + MIN_SPIN_DOWN;
            int wanted = targetIndex();
            while (Math.floorMod(candidate, STRIP.length) != wanted) {
                candidate++;
            }
            stopTarget = (float) candidate;
        }
        return stopTarget;
    }

    private float reelPosition(float t) {
        if (t <= INTRO_TICKS + FAST_TICKS) {
            return Math.max(0.0f, t) * SPIN_SPEED;
        }
        float atStop = (INTRO_TICKS + FAST_TICKS) * SPIN_SPEED;
        float p = clamp01((t - INTRO_TICKS - FAST_TICKS) / DECEL_TICKS);
        return lerp(atStop, stopTarget(), easeOutCubic(p));
    }

    @Override
    protected void onAnimationTick(float t) {
        ModConfig cfg = ModConfig.get();

        if (t < SETTLED_TICK && ticks - lastTickSoundAt >= TICK_INTERVAL) {
            lastTickSoundAt = ticks;
            float progress = clamp01((t - INTRO_TICKS) / (FAST_TICKS + DECEL_TICKS));
            if (progress < 0.55f || ticks % 3 == 0) {
                SoundUtil.play(SoundEvents.NOTE_BLOCK_HAT, 1.6f + progress * 0.3f, cfg.highRollerVolume * 0.5f);
            }
        }

        if (!landedSounded && t >= SETTLED_TICK) {
            landedSounded = true;
            revealNow();
            Drop drop = landedDrop();
            if (drop != null) {
                ProfitCalculator.prefetch(drop);
            }
            if (isHeadline()) {
                SoundUtil.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.4f, 0.7f);
                SoundUtil.play(SoundEvents.RAID_HORN, 0.7f, 0.9f);
            } else {
                SoundUtil.play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.1f, 0.8f);
            }
        }

        if (t >= SETTLED_TICK + resultTicks()) {
            finishAndClose();
        }
    }

    @Override
    protected void renderAnimation(GuiGraphicsExtractor ctx, float t, float delta) {
        int cx = this.width / 2;
        int cy = this.height / 2;

        boolean settled = t >= SETTLED_TICK;
        float rt = settled ? t - SETTLED_TICK : 0.0f;
        float intro = easeOutCubic(clamp01(t / INTRO_TICKS));
        int baseY = cy + (int) ((1.0f - intro) * -40.0f);

        if (settled && isHeadline()) {
            float dark = clamp01(1.0f - rt / 10.0f) * 0.35f;
            ctx.fill(0, 0, this.width, this.height, RenderCompat.withAlpha(0x000000, dark));
        }

        drawChassis(ctx, cx, baseY, settled, rt);
        drawReel(ctx, cx, baseY, reelPosition(t));

        if (settled && isHeadline()) {
            drawNeonBurst(ctx, cx, baseY, rt);
        }

        drawTitle(ctx, cx, baseY, intro, settled, rt);
    }

    private void drawChassis(GuiGraphicsExtractor ctx, int cx, int cy, boolean settled, float rt) {
        int halfW = WINDOW_W / 2 + 26;
        int top = cy - WINDOW_H / 2 - 20;
        int bottom = cy + WINDOW_H / 2 + 20;

        ctx.fill(cx - halfW, top, cx + halfW, bottom, CHASSIS);
        ctx.fill(cx - halfW, top, cx + halfW, top + 3, CHASSIS_LIGHT);
        ctx.fill(cx - halfW, bottom - 4, cx + halfW, bottom, BEZEL);

        int trim = NEON;
        int layers = 1;
        if (settled && isHeadline()) {
            trim = ((int) (rt / 3.0f)) % 2 == 0 ? NEON_HOT : NEON;
            layers = 4;
        }
        for (int i = 0; i < layers; i++) {
            RenderCompat.outline(ctx, cx - halfW - i * 3, top - i * 3,
                    (halfW + i * 3) * 2, (bottom - top) + i * 6,
                    i == 0 ? trim : RenderCompat.withAlpha(trim & 0xFFFFFF, 0.5f - i * 0.12f));
        }

        ctx.fill(cx - WINDOW_W / 2 - 4, cy - WINDOW_H / 2 - 4,
                cx + WINDOW_W / 2 + 4, cy + WINDOW_H / 2 + 4, BEZEL);
    }

    private void drawReel(GuiGraphicsExtractor ctx, int cx, int cy, float position) {
        int x = cx - WINDOW_W / 2;
        int y = cy - WINDOW_H / 2;
        ctx.fill(x, y, x + WINDOW_W, y + WINDOW_H, 0xFF0B1020);

        ctx.enableScissor(x, y, x + WINDOW_W, y + WINDOW_H);
        int base = (int) Math.floor(position);
        float frac = position - base;
        for (int row = -2; row <= 2; row++) {
            int symbol = Math.floorMod(base - row, STRIP.length);
            float iconY = cy + (row + frac) * CELL;
            if (Math.abs(iconY - cy) > WINDOW_H) {
                continue;
            }
            boolean glint = landedSounded && won() && symbol == targetIndex() && row == 0;
            if (glint) {
                RenderCompat.drawGlintItem(ctx, foilIcons[symbol], cx, iconY, ICON_SCALE, ticks, NEON & 0xFFFFFF);
            } else {
                RenderCompat.drawScaledItem(ctx, icons[symbol], cx, iconY, ICON_SCALE);
            }
        }
        for (int i = 0; i < 12; i++) {
            int a = (int) (200 * (1.0f - i / 12.0f));
            ctx.fill(x, y + i, x + WINDOW_W, y + i + 1, (a << 24));
            ctx.fill(x, y + WINDOW_H - i - 1, x + WINDOW_W, y + WINDOW_H - i, (a << 24));
        }
        ctx.disableScissor();

        RenderCompat.outline(ctx, x, y, WINDOW_W, WINDOW_H, 0xFF1E2A44);
        int mid = cy;
        ctx.fill(x - 6, mid - 1, x, mid + 1, NEON);
        ctx.fill(x + WINDOW_W, mid - 1, x + WINDOW_W + 6, mid + 1, NEON);
    }

    private void drawNeonBurst(GuiGraphicsExtractor ctx, int cx, int cy, float rt) {
        int count = 20;
        float p = clamp01(rt / 22.0f);
        for (int i = 0; i < count; i++) {
            float angle = hash(seed * 11 + i * 29) * (float) Math.PI * 2.0f;
            float dist = (40.0f + hash(seed * 7 + i * 19) * 90.0f) * p;
            int px = cx + (int) (Math.cos(angle) * dist);
            int py = cy + (int) (Math.sin(angle) * dist);
            int size = hash(seed + i * 3) > 0.6f ? 4 : 2;
            int rgb = i % 3 == 0 ? 0xFFFFFF : (i % 3 == 1 ? (NEON & 0xFFFFFF) : (NEON_HOT & 0xFFFFFF));
            ctx.fill(px, py, px + size, py + size, RenderCompat.withAlpha(rgb, 1.0f - p));
        }
        for (int wave = 0; wave < 2; wave++) {
            float w = rt - wave * 6.0f;
            if (w <= 0) {
                continue;
            }
            float wp = clamp01(w / 20.0f);
            int r = (int) (50 + wp * 200);
            drawRing(ctx, cx, cy, r, RenderCompat.withAlpha(NEON & 0xFFFFFF, (1.0f - wp) * 0.45f));
        }
    }

    private void drawRing(GuiGraphicsExtractor ctx, int cx, int cy, int r, int argb) {
        int th = 2;
        ctx.fill(cx - r, cy - r, cx + r, cy - r + th, argb);
        ctx.fill(cx - r, cy + r - th, cx + r, cy + r, argb);
        ctx.fill(cx - r, cy - r, cx - r + th, cy + r, argb);
        ctx.fill(cx + r - th, cy - r, cx + r, cy + r, argb);
    }

    private void drawTitle(GuiGraphicsExtractor ctx, int cx, int cy, float intro, boolean settled, float rt) {
        int top = cy - WINDOW_H / 2 - 42;
        if (!settled) {
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal("HIGH ROLLER").withStyle(ChatFormatting.BOLD),
                    cx, top, 1.5f * intro, NEON);
            return;
        }

        Drop drop = landedDrop();
        float in = easeOutCubic(clamp01(rt / 10.0f));

        if (isHeadline()) {
            float pulse = 0.7f + 0.3f * (float) Math.sin(rt * 0.45f);
            int b = (int) (255 * pulse);
            int col = 0xFF000000 | ((int) (b * 0.7f) << 16) | (b << 8) | b;
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal(drop.displayName().toUpperCase(java.util.Locale.ROOT) + "!")
                            .withStyle(ChatFormatting.BOLD),
                    cx, top, 1.2f + in * 0.9f, col);
            if (in >= 0.95f) {
                RenderCompat.centeredText(ctx, this.font,
                        Component.literal("YOU WON! 1x " + drop.displayName()
                                        + ProfitCalculator.suffix(drop))
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                        cx, cy + WINDOW_H / 2 + 28, 0xFF8FE8F0);
            }
            return;
        }

        RenderCompat.drawScaledCenteredText(ctx, this.font,
                Component.literal(won() ? drop.displayName() : "No drop").withStyle(ChatFormatting.GRAY),
                cx, top, 1.0f + in * 0.2f, 0xFFA0A8B8);
        if (in >= 0.95f && won()) {
            RenderCompat.centeredText(ctx, this.font,
                    Component.literal("1x " + drop.displayName() + ProfitCalculator.suffix(drop))
                            .withStyle(ChatFormatting.BLUE),
                    cx, cy + WINDOW_H / 2 + 28, 0xFF7A9AD0);
        }
    }

    private static float hash(int seed) {
        int h = seed * 374761393 + 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0f;
    }
}
