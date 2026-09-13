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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class ReelSlotScreen extends AnimationScreen {
    public record Theme(String title, int chassisDark, int chassisMid, int chassisLight, int bezel, int windowBg,
                        int trim, int trimBright, int titleColour, String winTitle, ChatFormatting winStyle,
                        int winColour, String lossTitle, String lossLine) {
    }

    protected static final int REELS = 3;
    protected static final int[] STOP_TICK = {42, 68, 96};
    protected static final int EASE_TICKS = 24;
    protected static final int SETTLED_TICK = STOP_TICK[REELS - 1] + EASE_TICKS;

    private static final float SPIN_SPEED = 0.62f;
    private static final int MIN_SPIN_DOWN = 4;
    private static final float[] REEL_STOP_PITCH = {1.0f, 1.26f, 1.5f};
    private static final float TICK_PITCH = 1.9f;
    private static final int TICK_INTERVAL = 3;

    protected static final int CELL = 30;
    protected static final int WINDOW_W = 44;
    protected static final int WINDOW_H = 54;
    protected static final int WINDOW_GAP = 8;
    private static final float ICON_SCALE = 1.6f;

    protected final Theme theme;
    private final ItemStack[] icons;
    private final ItemStack[] foilIcons;
    private final int winningIndex;

    private final float[] startPos = new float[REELS];
    private final float[] stopTarget = new float[REELS];
    private final boolean[] reelSounded = new boolean[REELS];
    private Float finalTarget = null;

    protected final int seed = (int) (System.nanoTime() & 0x7FFFFFFF);
    private int lastTickSoundAt = -99;

    protected ReelSlotScreen(Component title, GambleSession session, Theme theme, ItemStack[] strip, int winningIndex) {
        super(title, session);
        this.theme = theme;
        this.icons = strip;
        this.foilIcons = new ItemStack[strip.length];
        for (int i = 0; i < strip.length; i++) {
            foilIcons[i] = DropIcons.foil(strip[i].copy());
        }
        this.winningIndex = Math.max(0, Math.min(strip.length - 1, winningIndex));
        ProfitCalculator.prefetch(session.wonDrop());
    }

    protected static int indexOf(Drop[] strip, @Nullable Drop wanted, int fallback) {
        if (wanted == null) {
            return fallback;
        }
        for (int i = 0; i < strip.length; i++) {
            if (strip[i] == wanted) {
                return i;
            }
        }
        return fallback;
    }

    protected static ItemStack[] iconsFor(Drop[] strip) {
        ItemStack[] out = new ItemStack[strip.length];
        for (int i = 0; i < strip.length; i++) {
            out[i] = DropIcons.of(strip[i]);
        }
        return out;
    }

    protected abstract float reelVolume();

    protected boolean spinTicks() {
        return ModConfig.get().slotSpinTicks;
    }

    protected float tickVolume() {
        return ModConfig.get().slotTickVolume;
    }

    protected int holdTicks() {
        ModConfig cfg = ModConfig.get();
        return (int) ((won() ? cfg.jackpotHoldSeconds : cfg.loseBannerSeconds) * 20.0f);
    }

    protected void onSettled(boolean won) {
        if (won) {
            SoundUtil.play(SoundEvents.NOTE_BLOCK_PLING, REEL_STOP_PITCH[REELS - 1], reelVolume());
            SoundUtil.playWinSound(true, 1.0f);
        } else {
            SoundUtil.play(SoundEvents.NOTE_BLOCK_BASS, 0.7f, reelVolume());
            SoundUtil.play(SoundEvents.VILLAGER_NO, 0.9f, 0.6f);
        }
    }

    protected void drawBackdrop(GuiGraphicsExtractor ctx, float t, boolean settled, float jt) {
    }

    protected void drawExtras(GuiGraphicsExtractor ctx, int cx, int baseY, int totalW, float t, boolean settled, float jt) {
    }

    protected String winLine() {
        Drop drop = session.wonDrop();
        String name = drop == null ? "the jackpot" : "1x " + drop.displayName();
        return "YOU WON! " + name + (session.isLootShared() ? " (Loot Share)" : "");
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < REELS; i++) {
            startPos[i] = i * 1.37f;
        }
        stopTarget[0] = stopTargetFor(startPos[0] + STOP_TICK[0] * SPIN_SPEED, winningIndex);
        stopTarget[1] = stopTargetFor(startPos[1] + STOP_TICK[1] * SPIN_SPEED, winningIndex);
    }

    private int lossSymbol() {
        int n = icons.length;
        if (n <= 1) {
            return 0;
        }
        int pick = (int) (hash(seed) * (n - 1));
        return pick >= winningIndex ? pick + 1 : pick;
    }

    private float finalReelTarget() {
        if (finalTarget == null) {
            int symbol = outcome() == GambleSession.Outcome.WIN ? winningIndex : lossSymbol();
            finalTarget = stopTargetFor(startPos[2] + STOP_TICK[2] * SPIN_SPEED, symbol);
        }
        return finalTarget;
    }

    private float stopTargetFor(float from, int symbolIndex) {
        int candidate = (int) Math.ceil(from) + MIN_SPIN_DOWN;
        while (Math.floorMod(candidate, icons.length) != symbolIndex) {
            candidate++;
        }
        return candidate;
    }

    @Override
    protected void onAnimationTick(float t) {
        if (spinTicks() && t < SETTLED_TICK && ticks - lastTickSoundAt >= TICK_INTERVAL) {
            lastTickSoundAt = ticks;
            SoundUtil.play(SoundEvents.UI_BUTTON_CLICK, TICK_PITCH, tickVolume());
        }

        for (int i = 0; i < REELS; i++) {
            if (!reelSounded[i] && t >= STOP_TICK[i] + EASE_TICKS) {
                reelSounded[i] = true;
                if (i == REELS - 1) {
                    revealNow();
                    onSettled(won());
                } else {
                    SoundUtil.play(SoundEvents.NOTE_BLOCK_PLING, REEL_STOP_PITCH[i], reelVolume());
                }
            }
        }

        if (t >= SETTLED_TICK + holdTicks()) {
            finishAndClose();
        }
    }

    private float reelPosition(int i, float t) {
        if (t <= STOP_TICK[i]) {
            return startPos[i] + t * SPIN_SPEED;
        }
        float atStop = startPos[i] + STOP_TICK[i] * SPIN_SPEED;
        float p = clamp01((t - STOP_TICK[i]) / EASE_TICKS);
        float target = (i == REELS - 1) ? finalReelTarget() : stopTarget[i];
        return lerp(atStop, target, easeOutCubic(p));
    }

    @Override
    protected void renderAnimation(GuiGraphicsExtractor ctx, float t, float delta) {
        int cx = this.width / 2;
        int cy = this.height / 2;

        boolean settled = t >= SETTLED_TICK;
        float jt = settled ? t - SETTLED_TICK : 0.0f;

        float intro = easeOutCubic(clamp01(t / 10.0f));
        int baseY = cy + (int) ((1.0f - intro) * -50.0f);

        int totalW = REELS * WINDOW_W + (REELS - 1) * WINDOW_GAP;
        int reelsLeft = cx - totalW / 2;
        int reelsTop = baseY - WINDOW_H / 2;

        drawBackdrop(ctx, t, settled, jt);
        drawChassis(ctx, cx, baseY, totalW, settled, jt);

        boolean glint = settled && won();
        for (int i = 0; i < REELS; i++) {
            int wx = reelsLeft + i * (WINDOW_W + WINDOW_GAP);
            drawReel(ctx, wx, reelsTop, reelPosition(i, t), glint);
        }

        drawPayline(ctx, reelsLeft, baseY, totalW, settled, jt);
        drawLever(ctx, cx + totalW / 2 + 34, baseY, t, settled);
        drawExtras(ctx, cx, baseY, totalW, t, settled, jt);

        if (settled && won()) {
            drawJackpot(ctx, cx, baseY, jt);
        } else if (settled) {
            drawNearMiss(ctx, cx, baseY, jt);
        } else {
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal(theme.title()).withStyle(ChatFormatting.BOLD),
                    cx, baseY - WINDOW_H / 2 - 34, 1.5f, theme.titleColour());
        }
    }

    private void drawChassis(GuiGraphicsExtractor ctx, int cx, int cy, int totalW, boolean settled, float jt) {
        int halfW = totalW / 2 + 22;
        int top = cy - WINDOW_H / 2 - 46;
        int bottom = cy + WINDOW_H / 2 + 26;

        ctx.fill(cx - halfW, top, cx + halfW, bottom, theme.chassisMid());
        ctx.fill(cx - halfW, top, cx + halfW, top + 4, theme.chassisLight());
        ctx.fill(cx - halfW, bottom - 5, cx + halfW, bottom, theme.chassisDark());
        ctx.fill(cx - halfW + 6, top + 8, cx + halfW - 6, top + 30, theme.chassisDark());

        int trim = settled && won() ? flashColour(jt) : (settled ? theme.chassisLight() : theme.trim());
        RenderCompat.outline(ctx, cx - halfW, top, halfW * 2, bottom - top, trim);
        RenderCompat.outline(ctx, cx - halfW + 1, top + 1, halfW * 2 - 2, bottom - top - 2, theme.bezel());

        ctx.fill(cx - totalW / 2 - 6, cy - WINDOW_H / 2 - 6,
                cx + totalW / 2 + 6, cy + WINDOW_H / 2 + 6, theme.bezel());
        ctx.fill(cx - 10, bottom - 20, cx + 10, bottom - 16, theme.chassisDark());

        if (settled && won()) {
            drawJackpotSparkle(ctx, cx, halfW, top, bottom, jt);
        }
    }

    private void drawReel(GuiGraphicsExtractor ctx, int x, int y, float position, boolean glint) {
        ctx.fill(x, y, x + WINDOW_W, y + WINDOW_H, theme.windowBg());

        int centerY = y + WINDOW_H / 2;
        int centerX = x + WINDOW_W / 2;

        ctx.enableScissor(x, y, x + WINDOW_W, y + WINDOW_H);
        int base = (int) Math.floor(position);
        float frac = position - base;
        for (int row = -2; row <= 2; row++) {
            int symbol = Math.floorMod(base - row, icons.length);
            float iconY = centerY + (row + frac) * CELL;
            if (Math.abs(iconY - centerY) > WINDOW_H) {
                continue;
            }
            ItemStack stack = glint && symbol == winningIndex ? foilIcons[symbol] : icons[symbol];
            RenderCompat.drawScaledItem(ctx, stack, centerX, iconY, ICON_SCALE);
        }
        for (int i = 0; i < 10; i++) {
            int a = (int) (170 * (1.0f - i / 10.0f));
            ctx.fill(x, y + i, x + WINDOW_W, y + i + 1, (a << 24));
            ctx.fill(x, y + WINDOW_H - i - 1, x + WINDOW_W, y + WINDOW_H - i, (a << 24));
        }
        ctx.disableScissor();
        RenderCompat.outline(ctx, x, y, WINDOW_W, WINDOW_H, theme.chassisDark());
    }

    private void drawPayline(GuiGraphicsExtractor ctx, int left, int cy, int totalW, boolean settled, float jt) {
        int colour = settled && won()
                ? flashColour(jt)
                : RenderCompat.withAlpha(settled ? theme.chassisLight() & 0xFFFFFF : theme.trim() & 0xFFFFFF,
                settled ? 0.8f : 0.35f);
        ctx.fill(left - 4, cy - 1, left + totalW + 4, cy + 1, colour);
        for (int i = 0; i < 4; i++) {
            ctx.fill(left - 10 - i, cy - 4 + i, left - 9 - i, cy + 4 - i, colour);
            ctx.fill(left + totalW + 9 + i, cy - 4 + i, left + totalW + 10 + i, cy + 4 - i, colour);
        }
    }

    private void drawLever(GuiGraphicsExtractor ctx, int x, int cy, float t, boolean settled) {
        float pull;
        if (t < 8.0f) {
            pull = easeOutCubic(clamp01(t / 8.0f));
        } else if (settled) {
            pull = 1.0f - easeOutCubic(clamp01((t - SETTLED_TICK) / 14.0f));
        } else {
            pull = 1.0f;
        }
        int shaftTop = cy - 34 + (int) (pull * 30.0f);
        ctx.fill(x - 2, shaftTop, x + 2, cy + 14, 0xFF5A5A66);
        ctx.fill(x - 6, shaftTop - 8, x + 6, shaftTop + 4, 0xFFC0392B);
        ctx.fill(x - 4, shaftTop - 6, x + 2, shaftTop, 0xFFE74C3C);
        ctx.fill(x - 5, cy + 12, x + 5, cy + 18, theme.chassisDark());
    }

    private void drawJackpot(GuiGraphicsExtractor ctx, int cx, int cy, float jt) {
        int top = cy - (WINDOW_H / 2 + 34);
        float in = easeOutCubic(clamp01(jt / 10.0f));
        float pulse = 0.7f + 0.3f * (float) Math.sin(jt * 0.45f);
        int bright = (int) (255 * pulse);
        int flashing = 0xFF000000 | (bright << 16) | ((int) (bright * 0.85f) << 8) | (int) (bright * 0.3f);

        RenderCompat.drawScaledCenteredText(ctx, this.font,
                Component.literal(theme.winTitle()).withStyle(ChatFormatting.BOLD),
                cx, top, 1.2f + in * 1.1f, flashing);

        if (in >= 0.95f) {
            RenderCompat.centeredText(ctx, this.font,
                    Component.literal(winLine()).withStyle(theme.winStyle(), ChatFormatting.BOLD),
                    cx, cy + WINDOW_H / 2 + 14, theme.winColour());
            Drop drop = session.wonDrop();
            boolean aggressive = drop != null && drop.isHeadline();
            ProfitCalculator.render(ctx, this.font, drop, cx, cy + WINDOW_H / 2 + 28, jt, aggressive);
        }
    }

    private void drawNearMiss(GuiGraphicsExtractor ctx, int cx, int cy, float jt) {
        float in = easeOutCubic(clamp01(jt / 8.0f));
        int top = cy - (WINDOW_H / 2 + 34);
        RenderCompat.drawScaledCenteredText(ctx, this.font,
                Component.literal(theme.lossTitle()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                cx, top, 1.0f + in * 0.4f, 0xFFB05868);
        if (in >= 0.95f) {
            RenderCompat.centeredText(ctx, this.font,
                    Component.literal(theme.lossLine()).withStyle(ChatFormatting.DARK_GRAY),
                    cx, cy + WINDOW_H / 2 + 14, 0xFF6E6E7A);
        }
    }

    private void drawJackpotSparkle(GuiGraphicsExtractor ctx, int cx, int halfW, int top, int bottom, float jt) {
        final int count = 22;
        for (int i = 0; i < count; i++) {
            float phase = (jt * 0.25f + hash(i) * 6.0f) % 3.0f;
            if (phase > 1.0f) {
                continue;
            }
            float along = hash(i * 31 + 7);
            boolean vertical = i % 2 == 0;
            int x;
            int y;
            if (vertical) {
                x = (i % 4 < 2) ? cx - halfW - 4 : cx + halfW + 2;
                y = (int) lerp(top, bottom, along);
            } else {
                x = (int) lerp(cx - halfW, cx + halfW, along);
                y = (i % 4 < 2) ? top - 4 : bottom + 2;
            }
            float a = 1.0f - phase;
            ctx.fill(x, y, x + 3, y + 3, RenderCompat.withAlpha(theme.trimBright() & 0xFFFFFF, a));
        }
    }

    protected int flashColour(float jt) {
        return ((int) (jt / 3.0f)) % 2 == 0 ? theme.trimBright() : theme.trim();
    }

    protected static float hash(int seed) {
        int h = seed * 374761393 + 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0f;
    }
}
