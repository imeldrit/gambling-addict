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

    protected static final int CELL = 30;
    protected static final int WINDOW_W = 46;
    protected static final int WINDOW_H = 58;
    protected static final int WINDOW_GAP = 10;
    private static final float ICON_SCALE = 1.6f;
    private static final int LIGHTS = 14;
    private static final int COINS = 40;

    private static final int[] LIGHT_CYCLE = {0xFFFF5A5A, 0xFFFFD24A, 0xFF5AFF7A, 0xFF5AB4FF, 0xFFD45AFF};

    protected final Theme theme;
    private final ItemStack[] icons;
    private final ItemStack[] foilIcons;
    private final int winningIndex;

    private final float[] startPos = new float[REELS];
    private final float[] stopTarget = new float[REELS];
    private final boolean[] reelSounded = new boolean[REELS];
    private Float finalTarget = null;

    protected final int seed = (int) (System.nanoTime() & 0x7FFFFFFF);
    private int lastClickAt = -99;
    private boolean leverSounded = false;

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

    private int reelsSpinning(float t) {
        int n = 0;
        for (int i = 0; i < REELS; i++) {
            if (t < STOP_TICK[i] + EASE_TICKS) {
                n++;
            }
        }
        return n;
    }

    @Override
    protected void onAnimationTick(float t) {
        ModConfig cfg = ModConfig.get();

        if (!leverSounded && t >= 1.0f) {
            leverSounded = true;
            SoundUtil.play(SoundEvents.UI_BUTTON_CLICK, 0.6f, cfg.reelClickVolume);
        }

        int spinning = reelsSpinning(t);
        if (cfg.reelClicks && spinning > 0 && t >= 6.0f) {
            int interval = spinning == 3 ? 2 : (spinning == 2 ? 3 : 4);
            if (ticks - lastClickAt >= interval) {
                lastClickAt = ticks;
                float pitch = 0.9f + (ticks % 3) * 0.08f;
                SoundUtil.play(SoundEvents.UI_BUTTON_CLICK, pitch, cfg.reelClickVolume);
            }
        }

        for (int i = 0; i < REELS; i++) {
            if (!reelSounded[i] && t >= STOP_TICK[i] + EASE_TICKS) {
                reelSounded[i] = true;
                if (i == REELS - 1) {
                    revealNow();
                    onSettled(won());
                } else {
                    SoundUtil.play(SoundEvents.NOTE_BLOCK_PLING, REEL_STOP_PITCH[i], reelVolume());
                    SoundUtil.play(SoundEvents.UI_BUTTON_CLICK, 0.7f, cfg.reelClickVolume);
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

    private float reelSpeed(int i, float t) {
        if (t <= STOP_TICK[i]) {
            return SPIN_SPEED;
        }
        float p = clamp01((t - STOP_TICK[i]) / EASE_TICKS);
        return SPIN_SPEED * (1.0f - p) * (1.0f - p);
    }

    @Override
    protected void renderAnimation(GuiGraphicsExtractor ctx, float t, float delta) {
        int cx = this.width / 2;
        int cy = this.height / 2;

        boolean settled = t >= SETTLED_TICK;
        float jt = settled ? t - SETTLED_TICK : 0.0f;
        boolean jackpot = settled && won();

        float intro = easeOutCubic(clamp01(t / 10.0f));
        int baseY = cy + (int) ((1.0f - intro) * -50.0f);

        int totalW = REELS * WINDOW_W + (REELS - 1) * WINDOW_GAP;
        int reelsLeft = cx - totalW / 2;
        int reelsTop = baseY - WINDOW_H / 2;

        drawBackdrop(ctx, t, settled, jt);
        if (jackpot) {
            drawSpotlight(ctx, cx, baseY, jt);
        }
        drawChassis(ctx, cx, baseY, totalW, t, settled, jt);

        for (int i = 0; i < REELS; i++) {
            int wx = reelsLeft + i * (WINDOW_W + WINDOW_GAP);
            boolean stopped = t >= STOP_TICK[i] + EASE_TICKS;
            drawReel(ctx, wx, reelsTop, reelPosition(i, t), reelSpeed(i, t), jackpot, stopped, t);
        }

        drawPayline(ctx, reelsLeft, baseY, totalW, settled, jt);
        drawLever(ctx, cx + totalW / 2 + 40, baseY, t, settled);
        drawExtras(ctx, cx, baseY, totalW, t, settled, jt);

        if (jackpot) {
            drawCoinShower(ctx, cx, baseY, totalW, jt);
            drawJackpot(ctx, cx, baseY, jt);
        } else if (settled) {
            drawNearMiss(ctx, cx, baseY, jt);
        } else {
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal(theme.title()).withStyle(ChatFormatting.BOLD),
                    cx, baseY - WINDOW_H / 2 - 36, 1.5f, theme.titleColour());
        }
    }

    private void drawSpotlight(GuiGraphicsExtractor ctx, int cx, int cy, float jt) {
        float a = clamp01(jt / 12.0f) * 0.22f;
        int rgb = theme.trimBright() & 0xFFFFFF;
        for (int i = 0; i < 4; i++) {
            int r = 90 + i * 40;
            ctx.fill(cx - r, cy - r, cx + r, cy + r, RenderCompat.withAlpha(rgb, a * (0.5f - i * 0.1f)));
        }
    }

    private void drawChassis(GuiGraphicsExtractor ctx, int cx, int cy, int totalW, float t, boolean settled, float jt) {
        int halfW = totalW / 2 + 26;
        int top = cy - WINDOW_H / 2 - 50;
        int bottom = cy + WINDOW_H / 2 + 34;
        boolean jackpot = settled && won();

        ctx.fill(cx - halfW - 3, top + 4, cx + halfW + 3, bottom + 4, RenderCompat.withAlpha(0x000000, 0.45f));

        ctx.fill(cx - halfW, top, cx + halfW, bottom, theme.chassisMid());
        ctx.fill(cx - halfW, top, cx + halfW, top + 3, theme.chassisLight());
        ctx.fill(cx - halfW, top, cx - halfW + 3, bottom, theme.chassisLight());
        ctx.fill(cx - halfW, bottom - 4, cx + halfW, bottom, theme.chassisDark());
        ctx.fill(cx + halfW - 3, top, cx + halfW, bottom, theme.chassisDark());

        int plateTop = top + 8;
        ctx.fill(cx - halfW + 8, plateTop, cx + halfW - 8, plateTop + 26, theme.chassisDark());
        RenderCompat.outline(ctx, cx - halfW + 8, plateTop, halfW * 2 - 16, 26, theme.bezel());
        drawMarquee(ctx, cx - halfW + 12, plateTop + 4, halfW * 2 - 24, t, settled, jt);

        int trayTop = cy + WINDOW_H / 2 + 12;
        ctx.fill(cx - halfW + 10, trayTop, cx + halfW - 10, trayTop + 16, theme.chassisDark());
        ctx.fill(cx - halfW + 10, trayTop, cx + halfW - 10, trayTop + 2, theme.bezel());
        drawMarquee(ctx, cx - halfW + 14, trayTop + 5, halfW * 2 - 28, t + 7.0f, settled, jt);

        int trim = jackpot ? flashColour(jt) : (settled ? theme.chassisLight() : theme.trim());
        RenderCompat.outline(ctx, cx - halfW, top, halfW * 2, bottom - top, trim);
        RenderCompat.outline(ctx, cx - halfW + 1, top + 1, halfW * 2 - 2, bottom - top - 2, theme.bezel());

        ctx.fill(cx - totalW / 2 - 7, cy - WINDOW_H / 2 - 7, cx + totalW / 2 + 7, cy + WINDOW_H / 2 + 7, theme.bezel());
        ctx.fill(cx - totalW / 2 - 7, cy - WINDOW_H / 2 - 7, cx + totalW / 2 + 7, cy - WINDOW_H / 2 - 6,
                RenderCompat.withAlpha(0xFFFFFF, 0.12f));

        if (jackpot) {
            drawJackpotSparkle(ctx, cx, halfW, top, bottom, jt);
        }
    }

    private void drawMarquee(GuiGraphicsExtractor ctx, int x, int y, int width, float t, boolean settled, float jt) {
        boolean jackpot = settled && won();
        float speed = jackpot ? 0.6f : 0.18f;
        int phase = (int) (t * speed);
        for (int i = 0; i < LIGHTS; i++) {
            int lx = x + i * (width - 6) / (LIGHTS - 1);
            int colour;
            float bright;
            if (settled && !won()) {
                colour = 0xFF6A2A2A;
                bright = 0.5f + 0.2f * (float) Math.sin(jt * 0.2f + i);
            } else {
                int idx = Math.floorMod(i - phase, LIGHT_CYCLE.length);
                colour = jackpot ? LIGHT_CYCLE[idx] : ((i + phase) % 3 == 0 ? theme.trimBright() : theme.trim());
                bright = jackpot ? 1.0f : ((i + phase) % 3 == 0 ? 1.0f : 0.35f);
            }
            int rgb = colour & 0xFFFFFF;
            ctx.fill(lx - 1, y - 1, lx + 7, y + 7, RenderCompat.withAlpha(rgb, bright * 0.25f));
            ctx.fill(lx, y, lx + 6, y + 6, RenderCompat.withAlpha(rgb, bright));
            ctx.fill(lx + 1, y + 1, lx + 3, y + 3, RenderCompat.withAlpha(0xFFFFFF, bright * 0.6f));
        }
    }

    private void drawReel(GuiGraphicsExtractor ctx, int x, int y, float position, float speed,
                          boolean jackpot, boolean stopped, float t) {
        ctx.fill(x, y, x + WINDOW_W, y + WINDOW_H, theme.windowBg());
        int band = RenderCompat.withAlpha(0xFFFFFF, 0.05f);
        ctx.fill(x, y + WINDOW_H / 2 - CELL / 2, x + WINDOW_W, y + WINDOW_H / 2 + CELL / 2, band);

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
            boolean winner = jackpot && symbol == winningIndex && row == 0;
            if (winner) {
                RenderCompat.drawGlintItem(ctx, foilIcons[symbol], centerX, iconY, ICON_SCALE, t, theme.trimBright() & 0xFFFFFF);
            } else {
                RenderCompat.drawScaledItem(ctx, icons[symbol], centerX, iconY, ICON_SCALE);
            }
        }
        if (speed > 0.3f) {
            float blur = clamp01((speed - 0.3f) / 0.35f) * 0.35f;
            for (int i = 0; i < WINDOW_H; i += 3) {
                ctx.fill(x, y + i, x + WINDOW_W, y + i + 1, RenderCompat.withAlpha(theme.windowBg() & 0xFFFFFF, blur));
            }
        }
        for (int i = 0; i < 12; i++) {
            int a = (int) (180 * (1.0f - i / 12.0f));
            ctx.fill(x, y + i, x + WINDOW_W, y + i + 1, (a << 24));
            ctx.fill(x, y + WINDOW_H - i - 1, x + WINDOW_W, y + WINDOW_H - i, (a << 24));
        }
        ctx.fill(x + 2, y + 2, x + 4, y + WINDOW_H - 2, RenderCompat.withAlpha(0xFFFFFF, 0.06f));
        ctx.disableScissor();

        int frame = jackpot ? flashColour(t) : (stopped ? theme.trim() : theme.chassisDark());
        RenderCompat.outline(ctx, x - 1, y - 1, WINDOW_W + 2, WINDOW_H + 2, frame);
    }

    private void drawPayline(GuiGraphicsExtractor ctx, int left, int cy, int totalW, boolean settled, float jt) {
        int rgb = settled && won() ? (flashColour(jt) & 0xFFFFFF) : (theme.trim() & 0xFFFFFF);
        float a = settled ? (won() ? 1.0f : 0.25f) : 0.35f;
        ctx.fill(left - 5, cy - 1, left + totalW + 5, cy + 1, RenderCompat.withAlpha(rgb, a));
        ctx.fill(left - 5, cy - 3, left + totalW + 5, cy + 3, RenderCompat.withAlpha(rgb, a * 0.2f));
        for (int i = 0; i < 5; i++) {
            ctx.fill(left - 12 - i, cy - 5 + i, left - 11 - i, cy + 5 - i, RenderCompat.withAlpha(rgb, a));
            ctx.fill(left + totalW + 11 + i, cy - 5 + i, left + totalW + 12 + i, cy + 5 - i, RenderCompat.withAlpha(rgb, a));
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
        int shaftTop = cy - 36 + (int) (pull * 32.0f);
        ctx.fill(x - 3, shaftTop, x + 3, cy + 16, 0xFF3A3A44);
        ctx.fill(x - 2, shaftTop, x + 1, cy + 16, 0xFF8A8A98);
        ctx.fill(x - 7, shaftTop - 9, x + 7, shaftTop + 5, 0xFFA02A1E);
        ctx.fill(x - 5, shaftTop - 7, x + 5, shaftTop + 3, 0xFFC0392B);
        ctx.fill(x - 4, shaftTop - 6, x + 1, shaftTop - 2, 0xFFF08070);
        ctx.fill(x - 7, cy + 12, x + 7, cy + 20, theme.chassisDark());
        ctx.fill(x - 7, cy + 12, x + 7, cy + 14, theme.chassisLight());
    }

    private void drawCoinShower(GuiGraphicsExtractor ctx, int cx, int cy, int totalW, float jt) {
        int halfW = totalW / 2 + 40;
        int top = cy - WINDOW_H / 2 - 70;
        int bottom = cy + WINDOW_H / 2 + 44;
        for (int i = 0; i < COINS; i++) {
            float delay = hash(seed + i * 17) * 30.0f;
            float life = jt - delay;
            if (life < 0.0f) {
                continue;
            }
            float fall = (life * (1.4f + hash(i * 3) * 1.2f)) % (bottom - top);
            int x = cx - halfW + (int) (hash(seed * 5 + i * 11) * halfW * 2);
            int y = top + (int) fall;
            int size = hash(i * 7) > 0.5f ? 4 : 3;
            boolean face = ((int) (life / 4.0f) + i) % 2 == 0;
            ctx.fill(x, y, x + size, y + size, face ? 0xFFFFD24A : 0xFFC89A2A);
            ctx.fill(x, y, x + 1, y + 1, 0xFFFFF2B0);
        }
    }

    private void drawJackpot(GuiGraphicsExtractor ctx, int cx, int cy, float jt) {
        int top = cy - (WINDOW_H / 2 + 36);
        float in = easeOutCubic(clamp01(jt / 10.0f));
        float pulse = 0.7f + 0.3f * (float) Math.sin(jt * 0.45f);
        int bright = (int) (255 * pulse);
        int flashing = 0xFF000000 | (bright << 16) | ((int) (bright * 0.85f) << 8) | (int) (bright * 0.3f);

        RenderCompat.drawScaledCenteredText(ctx, this.font,
                Component.literal(theme.winTitle()).withStyle(ChatFormatting.BOLD),
                cx, top, 1.2f + in * 1.1f, flashing);

        if (in >= 0.95f) {
            int lineY = cy + WINDOW_H / 2 + 42;
            RenderCompat.centeredText(ctx, this.font,
                    Component.literal(winLine()).withStyle(theme.winStyle(), ChatFormatting.BOLD),
                    cx, lineY, theme.winColour());
            Drop drop = session.wonDrop();
            boolean aggressive = drop != null && drop.isHeadline();
            ProfitCalculator.render(ctx, this.font, drop, cx, lineY + 14, jt, aggressive);
        }
    }

    private void drawNearMiss(GuiGraphicsExtractor ctx, int cx, int cy, float jt) {
        float in = easeOutCubic(clamp01(jt / 8.0f));
        int top = cy - (WINDOW_H / 2 + 36);
        RenderCompat.drawScaledCenteredText(ctx, this.font,
                Component.literal(theme.lossTitle()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                cx, top, 1.0f + in * 0.4f, 0xFFB05868);
        if (in >= 0.95f) {
            RenderCompat.centeredText(ctx, this.font,
                    Component.literal(theme.lossLine()).withStyle(ChatFormatting.DARK_GRAY),
                    cx, cy + WINDOW_H / 2 + 42, 0xFF6E6E7A);
        }
    }

    private void drawJackpotSparkle(GuiGraphicsExtractor ctx, int cx, int halfW, int top, int bottom, float jt) {
        final int count = 26;
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
                x = (i % 4 < 2) ? cx - halfW - 5 : cx + halfW + 2;
                y = (int) lerp(top, bottom, along);
            } else {
                x = (int) lerp(cx - halfW, cx + halfW, along);
                y = (i % 4 < 2) ? top - 5 : bottom + 2;
            }
            float a = 1.0f - phase;
            ctx.fill(x - 1, y - 1, x + 4, y + 4, RenderCompat.withAlpha(theme.trimBright() & 0xFFFFFF, a * 0.3f));
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
