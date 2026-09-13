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

public class PulsingHeartScreen extends AnimationScreen {
    private static final int BEATS = 3;
    private static final int INTRO_TICKS = 16;
    private static final int SWELL_TICKS = 7;
    private static final int HOLD_TICKS = 3;
    private static final int RELAX_TICKS = 16;
    private static final int BEAT_TICKS = SWELL_TICKS + HOLD_TICKS + RELAX_TICKS;

    private static final float HEART_SCALE = 3.2f;
    private static final float SWELL_AMOUNT = 0.55f;

    private static final float BEAT_BASE_PITCH = 0.75f;
    private static final float BEAT_PITCH_STEP = 0.12f;

    private static final int HEART_DEEP = 0xFF6B0F1A;
    private static final int HEART_MID = 0xFFA51424;
    private static final int HEART_HOT = 0xFFE23A4C;
    private static final int GOLD = 0xFFD4AF37;

    private final ItemStack heartIcon = DropIcons.of(Drop.WARDEN_HEART);
    private final ItemStack heartFoil = DropIcons.of(Drop.WARDEN_HEART, true);
    private ItemStack revealIcon = null;

    private int beatsDone = 0;
    private boolean resultPlayed = false;
    private final int seed = (int) (System.nanoTime() & 0x7FFFFFFF);

    public PulsingHeartScreen(GambleSession session) {
        super(Component.literal("The Pulsing Heart"), session);
        ProfitCalculator.prefetch(Drop.WARDEN_HEART);
    }

    @Override
    protected float speedMultiplier() {
        return Math.max(0.1f, ModConfig.get().heartSpeed);
    }

    private int resultTicks() {
        ModConfig cfg = ModConfig.get();
        return (int) ((isWardenHeart() ? cfg.anvilBannerSeconds : cfg.loseBannerSeconds + 1.5f) * 20.0f);
    }

    private boolean isWardenHeart() {
        return won() && session.wonDrop() == Drop.WARDEN_HEART;
    }

    private Drop revealedDrop() {
        return won() ? session.wonDrop() : null;
    }

    @Override
    protected void onAnimationTick(float t) {
        float body = t - INTRO_TICKS;
        if (body < 0.0f) {
            return;
        }
        int beat = (int) (body / BEAT_TICKS);
        float local = body - beat * BEAT_TICKS;

        if (beat < BEATS) {
            if (local >= 1.0f && beat >= beatsDone) {
                onBeat(beat);
            }
            return;
        }

        float rt = body - BEATS * BEAT_TICKS;
        if (!resultPlayed) {
            resultPlayed = true;
            revealNow();
            if (isWardenHeart()) {
                SoundUtil.play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                SoundUtil.play(SoundEvents.BEACON_ACTIVATE, 1.2f, 0.8f);
            } else {
                SoundUtil.play(SoundEvents.GLASS_BREAK, 0.8f, 1.0f);
                SoundUtil.play(SoundEvents.ITEM_BREAK, 0.7f, 0.9f);
                Drop drop = revealedDrop();
                if (drop != null) {
                    revealIcon = DropIcons.of(drop, true);
                    ProfitCalculator.prefetch(drop);
                }
            }
        }
        if (rt >= resultTicks()) {
            finishAndClose();
        }
    }

    private void onBeat(int beatIndex) {
        beatsDone = beatIndex + 1;
        if (beatIndex == BEATS - 1) {
            won();
        }
        SoundUtil.play(SoundEvents.WARDEN_HEARTBEAT,
                BEAT_BASE_PITCH + beatIndex * BEAT_PITCH_STEP, ModConfig.get().heartVolume);
    }

    @Override
    protected void renderAnimation(GuiGraphicsExtractor ctx, float t, float delta) {
        int cx = this.width / 2;
        int cy = this.height / 2;

        float body = t - INTRO_TICKS;
        int beat = body < 0 ? -1 : (int) (body / BEAT_TICKS);
        float local = body < 0 ? 0 : body - beat * BEAT_TICKS;
        boolean result = beat >= BEATS;
        float rt = result ? body - BEATS * BEAT_TICKS : 0.0f;

        float intro = easeOutCubic(clamp01(t / INTRO_TICKS));

        int shakeX = 0;
        int shakeY = 0;
        if (result && isWardenHeart() && rt < 12.0f && ModConfig.get().anvilScreenShake) {
            float decay = 1.0f - rt / 12.0f;
            shakeX = (int) (Math.sin(rt * 3.9f) * 6.0f * decay);
            shakeY = (int) (Math.cos(rt * 4.7f) * 4.0f * decay);
        }
        int hx = cx + shakeX;
        int hy = cy + shakeY;

        drawTitle(ctx, cx, intro, result);

        float pulse = beatPulse(beat, local, result, rt);
        boolean shattered = result && !isWardenHeart();

        if (!shattered) {
            drawAura(ctx, hx, hy, pulse, result && isWardenHeart() ? GOLD : HEART_HOT, intro);
            RenderCompat.drawScaledItem(ctx, result ? heartFoil : heartIcon, hx, hy, HEART_SCALE * intro * (1.0f + pulse * SWELL_AMOUNT));
            if (beat >= 1 && !result) {
                drawBloodPulse(ctx, hx, hy, local, beat);
            }
        } else {
            drawShatter(ctx, hx, hy, rt);
            Drop drop = revealedDrop();
            if (drop != null && revealIcon != null && rt > 8.0f) {
                float in = easeOutCubic(clamp01((rt - 8.0f) / 12.0f));
                float hover = (float) Math.sin(rt * 0.12f) * 3.0f;
                drawFoilGlow(ctx, hx, (int) (hy + hover), rt, in);
                RenderCompat.drawScaledItem(ctx, revealIcon, hx, hy + hover, 2.6f * in);
            }
        }

        if (result && isWardenHeart()) {
            drawShockwave(ctx, hx, hy, rt);
        }

        drawBeatPips(ctx, cx, cy + 96, result);

        if (result) {
            drawResultBanner(ctx, cx, rt);
        }
    }

    private float beatPulse(int beat, float local, boolean result, float rt) {
        if (result) {
            return isWardenHeart() ? Math.max(0.0f, 1.6f - rt / 10.0f) : 0.0f;
        }
        if (beat < 0) {
            return 0.05f * (float) Math.sin(rt * 0.1f);
        }
        float intensity = 0.7f + beat * 0.25f;
        if (local < SWELL_TICKS) {
            return easeOutCubic(local / SWELL_TICKS) * intensity;
        }
        if (local < SWELL_TICKS + HOLD_TICKS) {
            return intensity;
        }
        float p = (local - SWELL_TICKS - HOLD_TICKS) / RELAX_TICKS;
        return (1.0f - easeOutCubic(p)) * intensity;
    }

    private void drawTitle(GuiGraphicsExtractor ctx, int cx, float intro, boolean result) {
        if (result) {
            return;
        }
        int y = 46 + (int) ((1.0f - intro) * -20.0f);
        RenderCompat.drawScaledCenteredText(ctx, this.font,
                Component.literal("THE PULSING HEART").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                cx, y, 1.6f, 0xFFB0303C);
        RenderCompat.centeredText(ctx, this.font,
                Component.literal("Three beats. Does it hold?").withStyle(ChatFormatting.DARK_GRAY),
                cx, y + 14, 0xFF7A5A5E);
    }

    private void drawAura(GuiGraphicsExtractor ctx, int cx, int cy, float pulse, int rgb, float intro) {
        for (int ring = 4; ring >= 0; ring--) {
            int r = (int) ((30 + ring * 8) * intro * (1.0f + pulse * 0.5f));
            float a = (0.26f - ring * 0.045f) * (0.5f + pulse * 0.7f);
            ctx.fill(cx - r, cy - r, cx + r, cy + r, RenderCompat.withAlpha(rgb & 0xFFFFFF, a));
        }
    }

    private void drawBloodPulse(GuiGraphicsExtractor ctx, int cx, int cy, float local, int beat) {
        float p = clamp01(local / (float) BEAT_TICKS);
        int count = 10 + beat * 6;
        for (int i = 0; i < count; i++) {
            float angle = hash(seed + beat * 71 + i * 13) * (float) Math.PI * 2.0f;
            float dist = (26.0f + hash(seed + i * 31) * 40.0f) * p;
            int x = cx + (int) (Math.cos(angle) * dist);
            int y = cy + (int) (Math.sin(angle) * dist);
            int size = hash(seed + i * 7) > 0.6f ? 3 : 2;
            ctx.fill(x, y, x + size, y + size, RenderCompat.withAlpha(0xC81E2A, 1.0f - p));
        }
    }

    private void drawShockwave(GuiGraphicsExtractor ctx, int cx, int cy, float rt) {
        for (int wave = 0; wave < 3; wave++) {
            float w = rt - wave * 5.0f;
            if (w <= 0) {
                continue;
            }
            float p = clamp01(w / 26.0f);
            int r = (int) (40 + p * 260);
            float a = (1.0f - p) * 0.5f;
            drawRing(ctx, cx, cy, r, RenderCompat.withAlpha(0xD4AF37, a));
        }
    }

    private void drawRing(GuiGraphicsExtractor ctx, int cx, int cy, int r, int argb) {
        int th = 3;
        ctx.fill(cx - r, cy - r, cx + r, cy - r + th, argb);
        ctx.fill(cx - r, cy + r - th, cx + r, cy + r, argb);
        ctx.fill(cx - r, cy - r, cx - r + th, cy + r, argb);
        ctx.fill(cx + r - th, cy - r, cx + r, cy + r, argb);
    }

    private void drawShatter(GuiGraphicsExtractor ctx, int cx, int cy, float rt) {
        float p = clamp01(rt / 22.0f);
        int shards = 14;
        for (int i = 0; i < shards; i++) {
            float angle = hash(seed * 3 + i * 17) * (float) Math.PI * 2.0f;
            float speed = 14.0f + hash(seed * 5 + i * 23) * 30.0f;
            float dist = speed * p;
            int x = cx + (int) (Math.cos(angle) * dist);
            int y = cy + (int) (Math.sin(angle) * dist * 0.6f + 40.0f * p * p);
            int size = 3 + (int) (hash(seed + i) * 4);
            int rgb = i % 2 == 0 ? (HEART_MID & 0xFFFFFF) : (HEART_DEEP & 0xFFFFFF);
            ctx.fill(x, y, x + size, y + size, RenderCompat.withAlpha(rgb, 1.0f - p));
        }
        float flash = clamp01(1.0f - rt / 6.0f);
        if (flash > 0.0f) {
            ctx.fill(cx - 60, cy - 60, cx + 60, cy + 60, RenderCompat.withAlpha(0xFFFFFF, flash * 0.4f));
        }
    }

    private void drawFoilGlow(GuiGraphicsExtractor ctx, int cx, int cy, float rt, float in) {
        for (int ring = 3; ring >= 0; ring--) {
            int r = (int) ((22 + ring * 6) * in);
            float a = (0.22f - ring * 0.045f) * (0.7f + 0.3f * (float) Math.sin(rt * 0.2f + ring));
            ctx.fill(cx - r, cy - r, cx + r, cy + r, RenderCompat.withAlpha(0xB44AE0, a));
        }
        for (int i = 0; i < 6; i++) {
            float angle = rt * 0.08f + i * ((float) Math.PI / 3.0f);
            int r = (int) (30 * in);
            int x = cx + (int) (Math.cos(angle) * r);
            int y = cy + (int) (Math.sin(angle) * r);
            ctx.fill(x, y, x + 3, y + 3, RenderCompat.withAlpha(0xE9A0FF, 0.8f * in));
        }
    }

    private void drawBeatPips(GuiGraphicsExtractor ctx, int cx, int y, boolean result) {
        int spacing = 18;
        int startX = cx - (BEATS - 1) * spacing / 2;
        for (int i = 0; i < BEATS; i++) {
            int x = startX + i * spacing;
            boolean lit = result || i < beatsDone;
            ctx.fill(x - 5, y - 5, x + 5, y + 5, lit ? HEART_MID : 0xFF2A2024);
            ctx.fill(x - 3, y - 3, x + 3, y + 3, lit ? HEART_HOT : 0xFF1A1418);
        }
    }

    private void drawResultBanner(GuiGraphicsExtractor ctx, int cx, float rt) {
        float in = easeOutCubic(clamp01(rt / 12.0f));
        int cy = this.height / 2 - 108;
        int halfW = (int) ((isWardenHeart() ? 210 : 175) * in);
        int h = (int) (44 * in);
        if (h < 2) {
            return;
        }

        ctx.fill(cx - halfW, cy - h / 2, cx + halfW, cy + h / 2, 0xE6100608);
        RenderCompat.outline(ctx, cx - halfW, cy - h / 2, halfW * 2, h, isWardenHeart() ? GOLD : 0xFF5A3A48);
        if (in < 0.9f) {
            return;
        }

        if (isWardenHeart()) {
            float pulse = 0.75f + 0.25f * (float) Math.sin(rt * 0.4f);
            int b = (int) (255 * pulse);
            int gold = 0xFF000000 | (b << 16) | ((int) (b * 0.82f) << 8) | (int) (b * 0.25f);
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal("WARDEN HEART ACQUIRED!").withStyle(ChatFormatting.BOLD), cx, cy - 6, 1.5f, gold);
            RenderCompat.centeredText(ctx, this.font,
                    Component.literal("YOU WON! 1x Warden Heart" + ProfitCalculator.suffix(Drop.WARDEN_HEART))
                            .withStyle(ChatFormatting.YELLOW), cx, cy + 10, 0xFFE8D48A);
            return;
        }

        Drop drop = revealedDrop();
        if (drop != null) {
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal("THE HEART SHATTERED").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    cx, cy - 6, 1.2f, 0xFFB05868);
            RenderCompat.centeredText(ctx, this.font,
                    Component.literal("1x " + drop.displayName() + ProfitCalculator.suffix(drop))
                            .withStyle(ChatFormatting.LIGHT_PURPLE), cx, cy + 10, 0xFFE9A0FF);
        } else {
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal("THE HEART SHATTERED").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    cx, cy - 6, 1.2f, 0xFFB05868);
            RenderCompat.centeredText(ctx, this.font,
                    Component.literal("No drop this time.").withStyle(ChatFormatting.DARK_GRAY),
                    cx, cy + 10, 0xFF6E6E7A);
        }
    }

    private static float hash(int seed) {
        int h = seed * 374761393 + 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0f;
    }
}
