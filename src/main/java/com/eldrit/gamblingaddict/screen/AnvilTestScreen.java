package com.eldrit.gamblingaddict.screen;

import com.eldrit.gamblingaddict.anim.GambleSession;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.render.RenderCompat;
import com.eldrit.gamblingaddict.util.DropIcons;
import com.eldrit.gamblingaddict.loot.Drop;
import com.eldrit.gamblingaddict.util.ProfitCalculator;
import com.eldrit.gamblingaddict.util.SoundUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AnvilTestScreen extends AnimationScreen {
    private static final int STRIKES = 3;
    private static final int INTRO_TICKS = 18;
    private static final int FALL_TICKS = 4;
    private static final int RECOIL_TICKS = 11;
    private static final int RAISE_TICKS = 13;
    private static final int CYCLE_TICKS = FALL_TICKS + RECOIL_TICKS + RAISE_TICKS;

    private static int victoryTicks() {
        return (int) (ModConfig.get().anvilBannerSeconds * 20.0f);
    }

    private static final int SHAKE_TICKS = 8;
    private static final float SHAKE_AMPLITUDE = 4.0f;

    private static final float STRIKE_BASE_PITCH = 0.85f;
    private static final float STRIKE_PITCH_STEP = 0.15f;
    private static final float VICTORY_PITCH = 1.0f;

    private static final float HAMMER_LIFT = 96.0f;
    private static final float CORE_SCALE = 2.5f;

    private static final int ANVIL_DARK = 0xFF232327;
    private static final int ANVIL_MID = 0xFF3E3E46;
    private static final int ANVIL_LIGHT = 0xFF5C5C68;
    private static final int HAMMER_DARK = 0xFF35373D;
    private static final int HAMMER_MID = 0xFF6C6E78;
    private static final int HAMMER_LIGHT = 0xFF9498A4;
    private static final int HANDLE_DARK = 0xFF4A331D;
    private static final int HANDLE_LIGHT = 0xFF6E4A2A;
    private static final int GOLD = 0xFFD4AF37;
    private static final int CRACK = 0xFF15060C;

    private final ItemStack core = DropIcons.of(Drop.JUDGEMENT_CORE);
    private final ItemStack coreFoil = DropIcons.of(Drop.JUDGEMENT_CORE, true);

    private int impactsDone = 0;
    private boolean resultPlayed = false;

    private int resultTicks() {
        ModConfig cfg = ModConfig.get();
        return (int) ((won() ? cfg.anvilBannerSeconds : cfg.loseBannerSeconds) * 20.0f);
    }

    public AnvilTestScreen(GambleSession session) {
        super(Component.literal("Judgement Test"), session);
        ProfitCalculator.prefetch(Drop.JUDGEMENT_CORE);
    }

    @Override
    protected float speedMultiplier() {
        return Math.max(0.1f, ModConfig.get().anvilSpeed);
    }

    @Override
    protected void onAnimationTick(float t) {
        float body = t - INTRO_TICKS;
        if (body < 0.0f) {
            return;
        }

        int strike = (int) (body / CYCLE_TICKS);
        float local = body - strike * CYCLE_TICKS;

        if (strike < STRIKES) {
            if (local >= FALL_TICKS && strike >= impactsDone) {
                onImpact(strike);
            }
            return;
        }

        float vt = body - STRIKES * CYCLE_TICKS;
        if (!resultPlayed) {
            resultPlayed = true;
            if (won()) {
                SoundUtil.play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, VICTORY_PITCH, 1.0f);
                if (ModConfig.get().customWinSoundForAnvil) {
                    SoundUtil.playWinSound(true, VICTORY_PITCH);
                }
            } else {
                SoundUtil.play(SoundEvents.VILLAGER_NO, 0.85f, 0.6f);
            }
        }
        if (vt >= resultTicks()) {
            finishAndClose();
        }
    }

    private void onImpact(int strikeIndex) {
        impactsDone = strikeIndex + 1;
        float volume = ModConfig.get().anvilStrikeVolume;

        if (strikeIndex == STRIKES - 1) {
            revealNow();
        }
        if (strikeIndex == STRIKES - 1 && !won()) {
            SoundUtil.play(SoundEvents.ANVIL_LAND, 0.55f, volume);
            SoundUtil.play(SoundEvents.GLASS_BREAK, 0.75f, volume);
            return;
        }

        SoundUtil.play(SoundEvents.ANVIL_LAND,
                STRIKE_BASE_PITCH + strikeIndex * STRIKE_PITCH_STEP, volume);
    }

    @Override
    protected void renderAnimation(GuiGraphicsExtractor ctx, float t, float delta) {
        int cx = this.width / 2;
        int anvilTopY = this.height / 2 + 34;

        float body = t - INTRO_TICKS;
        int strike = body < 0 ? -1 : (int) (body / CYCLE_TICKS);
        float local = body < 0 ? 0 : body - strike * CYCLE_TICKS;
        boolean victory = strike >= STRIKES;
        float vt = victory ? body - STRIKES * CYCLE_TICKS : 0.0f;

        float introEase = easeOutCubic(clamp01(t / INTRO_TICKS));
        int introOffset = (int) ((1.0f - introEase) * -40.0f);

        int shakeX = 0;
        int shakeY = 0;
        if (ModConfig.get().anvilScreenShake
                && !victory && strike >= 0 && local >= FALL_TICKS && local < FALL_TICKS + SHAKE_TICKS) {
            float decay = 1.0f - (local - FALL_TICKS) / SHAKE_TICKS;
            float amp = SHAKE_AMPLITUDE * decay * decay;
            shakeX = (int) (Math.sin(local * 4.1f) * amp);
            shakeY = (int) (Math.cos(local * 5.3f) * amp * 0.5f);
        }

        int baseY = anvilTopY + introOffset + shakeY;
        int baseX = cx + shakeX;

        drawTitle(ctx, cx, introEase, victory);
        drawAnvil(ctx, baseX, baseY);

        float coreCenterX = baseX;
        float coreCenterY = baseY - (16.0f * CORE_SCALE) / 2.0f - 2.0f;

        if (!victory && strike >= 0 && local >= FALL_TICKS && local < FALL_TICKS + 4) {
            coreCenterY += 2.0f;
        }

        boolean decided = impactsDone >= STRIKES;
        boolean broken = decided && !won();

        drawCoreHalo(ctx, (int) coreCenterX, (int) coreCenterY, t, victory && !broken, vt);

        if (broken) {
            float sinceBreak = body - ((STRIKES - 1) * CYCLE_TICKS + FALL_TICKS);
            drawShatteredCore(ctx, (int) coreCenterX, (int) coreCenterY, Math.max(0.0f, sinceBreak));
        } else {
            RenderCompat.drawScaledItem(ctx, victory ? coreFoil : core, coreCenterX, coreCenterY, CORE_SCALE);

            int crackStage = crackStage(strike, local, victory);
            if (crackStage > 0) {
                float sealProgress = victory ? clamp01(vt / 18.0f) : 0.0f;
                drawCracks(ctx, (int) coreCenterX, (int) coreCenterY, crackStage, sealProgress);
            }
        }

        float impactY = coreCenterY - (16.0f * CORE_SCALE) / 2.0f - 3.0f;
        if (!victory) {
            drawHammer(ctx, baseX, hammerBottomY(impactY, t, strike, local));
        } else {
            float away = easeOutCubic(clamp01(vt / 14.0f));
            drawHammer(ctx, baseX, impactY - HAMMER_LIFT - away * 140.0f);
        }

        if (ModConfig.get().anvilSparks
                && !victory && strike >= 0 && local >= FALL_TICKS && local < FALL_TICKS + 10) {
            drawSparks(ctx, (int) coreCenterX, (int) coreCenterY, (local - FALL_TICKS) / 10.0f, strike);
        }

        drawStrikePips(ctx, cx, baseY + 64, victory);

        if (victory) {
            if (won()) {
                drawVictoryBanner(ctx, cx, vt);
            } else {
                drawLossBanner(ctx, cx, vt);
            }
        }
    }

    private void drawShatteredCore(GuiGraphicsExtractor ctx, int cx, int cy, float sinceBreak) {
        final int shards = 11;
        float p = Math.min(1.0f, sinceBreak / 26.0f);

        for (int i = 0; i < shards; i++) {
            float angle = hash(i * 41 + 7) * (float) Math.PI * 2.0f;
            float speed = 10.0f + hash(i * 73 + 13) * 22.0f;
            float dist = speed * p;

            int x = cx + (int) (Math.cos(angle) * dist);
            int y = cy + (int) (Math.sin(angle) * dist * 0.55f + 34.0f * p * p);

            int size = 3 + (int) (hash(i * 19) * 3);
            int rgb = i % 3 == 0 ? 0x7A4A9C : (i % 3 == 1 ? 0x5B3676 : 0x39234A);
            ctx.fill(x, y, x + size, y + size, RenderCompat.withAlpha(rgb, 1.0f - p * 0.35f));
        }

        float dust = clamp01(1.0f - sinceBreak / 12.0f);
        if (dust > 0.0f) {
            ctx.fill(cx - 20, cy - 8, cx + 20, cy + 10, RenderCompat.withAlpha(0x2A2030, dust * 0.7f));
        }
    }

    private void drawLossBanner(GuiGraphicsExtractor ctx, int cx, float vt) {
        float in = easeOutCubic(clamp01(vt / 8.0f));
        int cy = this.height / 2 - 46;
        int halfW = (int) (150 * in);
        int bannerH = (int) (32 * in);
        if (bannerH < 2) {
            return;
        }

        ctx.fill(cx - halfW, cy - bannerH / 2, cx + halfW, cy + bannerH / 2, 0xD0100810);
        RenderCompat.outline(ctx, cx - halfW, cy - bannerH / 2, halfW * 2, bannerH, 0xFF5A3A48);

        if (in < 0.9f) {
            return;
        }
        RenderCompat.centeredText(ctx, this.font,
                Component.literal("THE CORE SHATTERED").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                cx, cy - 8, 0xFFB05868);
        RenderCompat.centeredText(ctx, this.font,
                Component.literal("No drop this time.").withStyle(ChatFormatting.DARK_GRAY),
                cx, cy + 3, 0xFF6E6E7A);
    }

    private int crackStage(int strike, float local, boolean victory) {
        if (victory) {
            return 2;
        }
        if (strike < 0) {
            return 0;
        }
        boolean landed = local >= FALL_TICKS;
        int landedCount = strike + (landed ? 1 : 0);
        return landedCount >= 2 ? 1 : 0;
    }

    private float hammerBottomY(float impactY, float t, int strike, float local) {
        float topY = impactY - HAMMER_LIFT;

        if (strike < 0) {
            return topY + (float) Math.sin(t * 0.18f) * 3.0f;
        }
        if (local < FALL_TICKS) {
            return lerp(topY, impactY, easeInQuad(local / FALL_TICKS));
        }
        if (local < FALL_TICKS + RECOIL_TICKS) {
            float since = local - FALL_TICKS;
            float bounce = (float) Math.exp(-since * 0.55f) * (float) Math.sin(since * 1.4f) * 7.0f;
            return impactY - Math.abs(bounce);
        }
        float p = (local - FALL_TICKS - RECOIL_TICKS) / RAISE_TICKS;
        return lerp(impactY, topY, easeOutCubic(p));
    }

    private void drawTitle(GuiGraphicsExtractor ctx, int cx, float introEase, boolean victory) {
        if (victory) {
            return;
        }
        int y = 44 + (int) ((1.0f - introEase) * -20.0f);
        RenderCompat.drawScaledCenteredText(ctx, this.font,
                Component.literal("THE JUDGMENT TEST").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD),
                cx, y, 1.6f, 0xFFBFBFC7);
        RenderCompat.centeredText(ctx, this.font,
                Component.literal("Three blows. If it holds, it's yours.").withStyle(ChatFormatting.DARK_GRAY),
                cx, y + 14, 0xFF7A7A85);
    }

    private void drawAnvil(GuiGraphicsExtractor ctx, int cx, int topY) {
        ctx.fill(cx - 52, topY, cx + 52, topY + 12, ANVIL_MID);
        ctx.fill(cx - 52, topY, cx + 52, topY + 3, ANVIL_LIGHT);
        ctx.fill(cx - 44, topY + 12, cx + 44, topY + 18, ANVIL_DARK);
        ctx.fill(cx - 22, topY + 18, cx + 22, topY + 34, ANVIL_MID);
        ctx.fill(cx - 22, topY + 18, cx - 16, topY + 34, ANVIL_DARK);
        ctx.fill(cx - 46, topY + 34, cx + 46, topY + 46, ANVIL_MID);
        ctx.fill(cx - 46, topY + 42, cx + 46, topY + 46, ANVIL_DARK);
        ctx.fill(cx - 58, topY + 46, cx + 58, topY + 50, 0x55000000);
    }

    private void drawHammer(GuiGraphicsExtractor ctx, int cx, float bottomY) {
        int by = (int) bottomY;
        int headTop = by - 26;

        ctx.fill(cx - 5, headTop - 62, cx + 5, headTop + 4, HANDLE_DARK);
        ctx.fill(cx - 5, headTop - 62, cx - 1, headTop + 4, HANDLE_LIGHT);
        ctx.fill(cx - 7, headTop - 66, cx + 7, headTop - 60, HANDLE_DARK);

        ctx.fill(cx - 30, headTop, cx + 30, by, HAMMER_MID);
        ctx.fill(cx - 30, headTop, cx + 30, headTop + 4, HAMMER_LIGHT);
        ctx.fill(cx - 30, by - 5, cx + 30, by, HAMMER_DARK);
        ctx.fill(cx - 34, by - 9, cx - 30, by, HAMMER_DARK);
        ctx.fill(cx + 30, by - 9, cx + 34, by, HAMMER_DARK);
        RenderCompat.outline(ctx, cx - 30, headTop, 60, 26, 0xFF1D1E22);
    }

    private void drawCoreHalo(GuiGraphicsExtractor ctx, int cx, int cy, float t, boolean victory, float vt) {
        float pulse = victory
                ? 0.6f + 0.4f * (float) Math.sin(vt * 0.35f)
                : 0.25f + 0.12f * (float) Math.sin(t * 0.12f);
        int rgb = victory ? 0xD4AF37 : 0x9B5FD0;

        for (int ring = 3; ring >= 0; ring--) {
            int r = 22 + ring * 5;
            float a = pulse * (0.30f - ring * 0.06f);
            ctx.fill(cx - r, cy - r, cx + r, cy + r, RenderCompat.withAlpha(rgb, a));
        }
    }

    private void drawCracks(GuiGraphicsExtractor ctx, int cx, int cy, int stage, float seal) {
        int colour = CRACK;
        if (stage >= 2) {
            float glow = 1.0f - seal;
            colour = RenderCompat.withAlpha(0xFFE28A, glow);
        }

        drawCrackChain(ctx, cx, cy, colour, new int[][]{{0, -14}, {-3, -6}, {2, 1}, {-2, 9}, {1, 15}});
        drawCrackChain(ctx, cx, cy, colour, new int[][]{{-14, -4}, {-7, -2}, {-2, 1}});
        drawCrackChain(ctx, cx, cy, colour, new int[][]{{2, 1}, {8, 5}, {13, 4}, {16, 9}});

        if (stage >= 2 && seal < 1.0f) {
            float flash = clamp01(1.0f - seal * 3.0f);
            if (flash > 0.0f) {
                ctx.fill(cx - 24, cy - 24, cx + 24, cy + 24, RenderCompat.withAlpha(0xFFFFFF, flash * 0.55f));
            }
        }
    }

    private void drawCrackChain(GuiGraphicsExtractor ctx, int cx, int cy, int colour, int[][] points) {
        for (int i = 0; i < points.length - 1; i++) {
            drawLine(ctx, cx + points[i][0], cy + points[i][1],
                    cx + points[i + 1][0], cy + points[i + 1][1], colour);
        }
    }

    private void drawLine(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int colour) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            ctx.fill(x1, y1, x1 + 2, y1 + 2, colour);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            ctx.fill(x, y, x + 2, y + 2, colour);
        }
    }

    private void drawSparks(GuiGraphicsExtractor ctx, int cx, int cy, float progress, int seed) {
        final int count = 14;
        float fade = 1.0f - progress;
        for (int i = 0; i < count; i++) {
            float angle = hash(seed * 97 + i * 3) * (float) Math.PI * 2.0f;
            float speed = 12.0f + hash(seed * 131 + i * 7) * 26.0f;
            float dist = speed * progress;

            int x = cx + (int) (Math.cos(angle) * dist);
            int y = cy + (int) (Math.sin(angle) * dist * 0.6f - 10.0f * progress + 22.0f * progress * progress);

            int size = hash(seed * 17 + i) > 0.65f ? 3 : 2;
            int rgb = i % 3 == 0 ? 0xFFF3B0 : 0xFFD24A;
            ctx.fill(x, y, x + size, y + size, RenderCompat.withAlpha(rgb, fade));
        }
    }

    private void drawStrikePips(GuiGraphicsExtractor ctx, int cx, int y, boolean victory) {
        int spacing = 18;
        int startX = cx - (STRIKES - 1) * spacing / 2;
        for (int i = 0; i < STRIKES; i++) {
            int x = startX + i * spacing;
            boolean lit = victory || i < impactsDone;
            ctx.fill(x - 5, y - 5, x + 5, y + 5, lit ? GOLD : 0xFF35353C);
            ctx.fill(x - 3, y - 3, x + 3, y + 3, lit ? 0xFFFFF0C0 : 0xFF1E1E23);
        }
    }

    private void drawVictoryBanner(GuiGraphicsExtractor ctx, int cx, float vt) {
        float in = easeOutCubic(clamp01(vt / 12.0f));
        int bannerH = (int) (46 * in);
        if (bannerH < 2) {
            return;
        }

        int cy = this.height / 2 - 46;
        int halfW = (int) (190 * in);

        ctx.fill(cx - halfW, cy - bannerH / 2, cx + halfW, cy + bannerH / 2, 0xE6120C04);
        RenderCompat.outline(ctx, cx - halfW, cy - bannerH / 2, halfW * 2, bannerH, GOLD);

        if (in < 0.9f) {
            return;
        }

        float pulse = 0.75f + 0.25f * (float) Math.sin(vt * 0.4f);
        int bright = (int) (255 * pulse);
        int gold = 0xFF000000 | (bright << 16) | ((int) (bright * 0.82f) << 8) | (int) (bright * 0.25f);

        RenderCompat.drawScaledCenteredText(ctx, this.font,
                Component.literal("JUDGEMENT CORE ACQUIRED!").withStyle(ChatFormatting.BOLD),
                cx, cy - 6, 1.5f, gold);
        RenderCompat.centeredText(ctx, this.font,
                Component.literal("YOU WON! 1x Judgement Core"
                                + ProfitCalculator.suffix(Drop.JUDGEMENT_CORE))
                        .withStyle(ChatFormatting.YELLOW),
                cx, cy + 10, 0xFFE8D48A);
    }

    private static float hash(int seed) {
        int h = seed * 374761393 + 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0f;
    }
}
