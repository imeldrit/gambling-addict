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

public class JawbusExplosionScreen extends AnimationScreen {
    private static final Identifier GOLEM = Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png");

    private static final int INTRO_TICKS = 12;
    private static final int FUSE_TICKS = 44;
    private static final int BOOM_TICK = INTRO_TICKS + FUSE_TICKS;
    private static final int RISE_DELAY = 8;
    private static final int RISE_TICKS = 22;

    private static final int CHESTS = 3;
    private static final int CHEST_GAP = 64;
    private static final float CHEST_SCALE = 3.0f;
    private static final float ITEM_SCALE = 2.4f;
    private static final int DEBRIS = 54;

    private static final int GOLD = 0xFFFFAA00;
    private static final int GREEN = 0xFF55FF55;

    private final ItemStack chest = DropIcons.chest();
    private final ItemStack fragment = DropIcons.of(Drop.MAGMA_LORD_FRAGMENT);
    private final ItemStack magmafish = DropIcons.silverMagmafish();
    private ItemStack prize = null;

    private boolean fuseLit = false;
    private boolean exploded = false;
    private boolean revealed = false;
    private int lastTickSoundAt = -99;
    private final int seed = (int) (System.nanoTime() & 0x7FFFFFFF);

    public JawbusExplosionScreen(GambleSession session) {
        super(Component.literal("Lord Jawbus"), session);
        ProfitCalculator.prefetch(session.wonDrop());
    }

    @Override
    protected float speedMultiplier() {
        return Math.max(0.1f, ModConfig.get().jawbusSpeed);
    }

    private float volume() {
        return ModConfig.get().jawbusVolume;
    }

    private boolean headline() {
        Drop d = won() ? session.wonDrop() : null;
        return d != null && d.isHeadline();
    }

    private int holdTicks() {
        ModConfig cfg = ModConfig.get();
        float seconds = headline() ? cfg.jackpotHoldSeconds : (won() ? cfg.anvilBannerSeconds : cfg.loseBannerSeconds + 1.2f);
        return (int) (seconds * 20.0f);
    }

    @Override
    protected void onAnimationTick(float t) {
        if (!fuseLit && t >= INTRO_TICKS) {
            fuseLit = true;
            SoundUtil.play(SoundEvents.TNT_PRIMED, 1.0f, volume() * 0.8f);
        }
        if (fuseLit && t < BOOM_TICK) {
            float p = clamp01((t - INTRO_TICKS) / FUSE_TICKS);
            int interval = p < 0.5f ? 6 : (p < 0.8f ? 3 : 2);
            if (ticks - lastTickSoundAt >= interval) {
                lastTickSoundAt = ticks;
                SoundUtil.play(SoundEvents.NOTE_BLOCK_HAT, 1.2f + p * 0.8f, volume() * 0.4f);
            }
        }
        if (!exploded && t >= BOOM_TICK) {
            exploded = true;
            revealNow();
            Drop drop = won() ? session.wonDrop() : null;
            if (drop != null) {
                prize = DropIcons.of(drop, true);
            }
            SoundUtil.play(SoundEvents.GENERIC_EXPLODE, 0.9f, volume());
            SoundUtil.play(SoundEvents.GENERIC_EXPLODE, 0.7f, volume() * 0.7f);
        }
        if (exploded && !revealed && t >= BOOM_TICK + RISE_DELAY + RISE_TICKS / 2) {
            revealed = true;
            if (headline()) {
                SoundUtil.play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, volume());
                SoundUtil.play(SoundEvents.TOTEM_USE, 1.2f, volume() * 0.5f);
                SoundUtil.playWinSound(true, 1.0f);
            } else if (won()) {
                SoundUtil.play(SoundEvents.PLAYER_LEVELUP, 1.0f, volume());
            } else {
                SoundUtil.play(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, volume() * 0.6f);
            }
        }
        if (t >= BOOM_TICK + RISE_DELAY + RISE_TICKS + holdTicks()) {
            finishAndClose();
        }
    }

    @Override
    protected void renderAnimation(GuiGraphicsExtractor ctx, float t, float delta) {
        int cx = this.width / 2;
        int cy = this.height / 2;

        drawGolem(ctx, cx, cy, t);
        drawWatermark(ctx, cx, cy, t);

        boolean boom = t >= BOOM_TICK;
        float bt = boom ? t - BOOM_TICK : 0.0f;

        int shakeX = 0;
        int shakeY = 0;
        if (boom && bt < 10.0f) {
            float s = (1.0f - bt / 10.0f) * 6.0f;
            shakeX = (int) (Math.sin(t * 9.1f) * s);
            shakeY = (int) (Math.cos(t * 7.3f) * s);
        }

        int floorY = cy + 34 + shakeY;
        drawFloor(ctx, cx + shakeX, floorY, boom, bt);

        if (!boom) {
            drawChests(ctx, cx, floorY, t);
        } else {
            drawCraters(ctx, cx + shakeX, floorY, bt);
            drawDebris(ctx, cx, floorY, bt);
            drawLoot(ctx, cx, floorY, bt);
        }

        if (boom && bt < 6.0f) {
            ctx.fill(0, 0, this.width, this.height, RenderCompat.withAlpha(0xFFF0D0, (1.0f - bt / 6.0f) * 0.85f));
        }

        drawTitle(ctx, cx, cy, t, boom, bt);
    }

    private void drawGolem(GuiGraphicsExtractor ctx, int cx, int cy, float t) {
        float in = easeOutCubic(clamp01(t / 16.0f));
        int tint = RenderCompat.withAlpha(0x9AA0A8, 0.30f * in);
        int s = 3;
        int top = cy - 78;
        int headX = cx - 4 * s;
        RenderCompat.blitRegion(ctx, GOLEM, headX, top, 8 * s, 10 * s, 8, 8, 8, 10, 128, 128, tint);
        int bodyY = top + 10 * s;
        RenderCompat.blitRegion(ctx, GOLEM, cx - 9 * s, bodyY, 18 * s, 12 * s, 11, 51, 18, 12, 128, 128, tint);
        RenderCompat.blitRegion(ctx, GOLEM, cx - 13 * s, bodyY, 4 * s, 30 * s, 66, 27, 4, 30, 128, 128, tint);
        RenderCompat.blitRegion(ctx, GOLEM, cx + 9 * s, bodyY, 4 * s, 30 * s, 66, 64, 4, 30, 128, 128, tint);
        int lowerY = bodyY + 12 * s;
        RenderCompat.blitRegion(ctx, GOLEM, cx - 4 * s - s / 2, lowerY, 9 * s, 5 * s, 6, 76, 9, 5, 128, 128, tint);
        int legY = lowerY + 5 * s;
        RenderCompat.blitRegion(ctx, GOLEM, cx - 7 * s, legY, 6 * s, 16 * s, 42, 5, 6, 16, 128, 128, tint);
        RenderCompat.blitRegion(ctx, GOLEM, cx + s, legY, 6 * s, 16 * s, 65, 5, 6, 16, 128, 128, tint);
    }

    private void drawWatermark(GuiGraphicsExtractor ctx, int cx, int cy, float t) {
        float pulse = 0.35f + 0.25f * (float) Math.sin(t * 0.15f);
        int glow = RenderCompat.withAlpha(0xFFD27A, pulse);
        RenderCompat.drawScaledCenteredText(ctx, this.font,
                Component.literal("?").withStyle(ChatFormatting.BOLD), cx + 1, cy - 30 + 1, 9.0f,
                RenderCompat.withAlpha(0x000000, pulse * 0.6f));
        RenderCompat.drawScaledCenteredText(ctx, this.font,
                Component.literal("?").withStyle(ChatFormatting.BOLD), cx, cy - 30, 9.0f, glow);
    }

    private void drawFloor(GuiGraphicsExtractor ctx, int cx, int floorY, boolean boom, float bt) {
        int halfW = 150;
        ctx.fill(cx - halfW, floorY, cx + halfW, floorY + 26, 0xFF3A1A12);
        ctx.fill(cx - halfW, floorY, cx + halfW, floorY + 3, 0xFF5A2A1A);
        for (int i = 0; i < 12; i++) {
            int x = cx - halfW + 6 + i * 25;
            float flicker = 0.5f + 0.5f * (float) Math.sin(bt * 0.4f + i * 2.1f);
            ctx.fill(x, floorY + 22, x + 8, floorY + 24, RenderCompat.withAlpha(0xFF6A1A, boom ? flicker : 0.5f));
        }
    }

    private int chestX(int cx, int i) {
        return cx + (i - 1) * CHEST_GAP;
    }

    private void drawChests(GuiGraphicsExtractor ctx, int cx, int floorY, float t) {
        float intro = easeOutCubic(clamp01(t / INTRO_TICKS));
        float fuse = clamp01((t - INTRO_TICKS) / FUSE_TICKS);
        for (int i = 0; i < CHESTS; i++) {
            float dropIn = (1.0f - intro) * -70.0f;
            float shake = fuse * fuse * 5.0f;
            float sx = (float) Math.sin(t * (6.0f + i) + i * 2.0f) * shake;
            float sy = (float) Math.cos(t * (7.5f + i)) * shake * 0.5f;
            float x = chestX(cx, i) + sx;
            float y = floorY - 8 * CHEST_SCALE + dropIn + sy;
            if (fuse > 0.0f) {
                float glow = fuse * (0.4f + 0.3f * (float) Math.sin(t * 0.9f + i));
                ctx.fill((int) x - 26, (int) y - 26, (int) x + 26, (int) y + 26, RenderCompat.withAlpha(0xFF4A00, glow * 0.35f));
            }
            RenderCompat.drawScaledItem(ctx, chest, x, y, CHEST_SCALE);
            if (fuse > 0.0f) {
                int fx = (int) x;
                int fy = (int) y - 26;
                float flame = 0.6f + 0.4f * (float) Math.sin(t * 1.3f + i);
                ctx.fill(fx - 1, fy - 6, fx + 1, fy, RenderCompat.withAlpha(0xFF8A2A, flame));
                ctx.fill(fx - 1, fy - 9, fx + 1, fy - 6, RenderCompat.withAlpha(0xFFE08A, flame));
            }
        }
    }

    private void drawCraters(GuiGraphicsExtractor ctx, int cx, int floorY, float bt) {
        float open = easeOutCubic(clamp01(bt / 6.0f));
        for (int i = 0; i < CHESTS; i++) {
            int x = chestX(cx, i);
            int w = (int) (28 * open);
            ctx.fill(x - w, floorY - 2, x + w, floorY + 10, 0xFF120806);
            ctx.fill(x - w + 3, floorY + 1, x + w - 3, floorY + 8, 0xFF080302);
            float ember = 0.4f + 0.4f * (float) Math.sin(bt * 0.5f + i);
            ctx.fill(x - 4, floorY + 4, x + 4, floorY + 6, RenderCompat.withAlpha(0xFF6A1A, ember));
        }
    }

    private void drawDebris(GuiGraphicsExtractor ctx, int cx, int floorY, float bt) {
        float life = 26.0f;
        if (bt > life) {
            return;
        }
        float p = bt / life;
        for (int i = 0; i < DEBRIS; i++) {
            int origin = chestX(cx, i % CHESTS);
            float angle = (float) (-Math.PI * (0.1 + 0.8 * hash(seed + i * 13)));
            float speed = 4.0f + hash(seed * 3 + i * 7) * 7.0f;
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed;
            float x = origin + vx * bt;
            float y = floorY - 20 + vy * bt + 0.35f * bt * bt;
            int size = hash(i * 5) > 0.6f ? 4 : 3;
            int rgb;
            switch (i % 4) {
                case 0 -> rgb = 0xFFB040;
                case 1 -> rgb = 0x8B5A2B;
                case 2 -> rgb = 0x6E6E6E;
                default -> rgb = 0xFF5A1A;
            }
            ctx.fill((int) x, (int) y, (int) x + size, (int) y + size, RenderCompat.withAlpha(rgb, 1.0f - p));
        }
        for (int i = 0; i < CHESTS; i++) {
            int x = chestX(cx, i);
            float r = 8.0f + bt * 4.0f;
            RenderCompat.ring(ctx, x, floorY - 8, (int) r, 2, RenderCompat.withAlpha(0xFFC070, (1.0f - p) * 0.6f));
        }
    }

    private void drawLoot(GuiGraphicsExtractor ctx, int cx, int floorY, float bt) {
        float rise = easeOutCubic(clamp01((bt - RISE_DELAY) / RISE_TICKS));
        if (rise <= 0.0f) {
            return;
        }
        float bob = (float) Math.sin(bt * 0.18f) * 3.0f;
        float restY = floorY - 70;
        float y = lerp(floorY - 6, restY, rise) + bob * rise;

        if (headline() && rise > 0.5f) {
            float aura = 0.35f + 0.2f * (float) Math.sin(bt * 0.4f);
            int ax = cx;
            int ay = (int) y;
            ctx.fill(ax - 34, ay - 34, ax + 34, ay + 34, RenderCompat.withAlpha(0xFFD27A, aura * 0.5f));
            for (int wave = 0; wave < 3; wave++) {
                float wp = ((bt * 0.05f + wave * 0.33f) % 1.0f);
                RenderCompat.ring(ctx, ax, ay, (int) (30 + wp * 90), 2, RenderCompat.withAlpha(0xFFD27A, (1.0f - wp) * 0.5f));
            }
        }

        float sideY = lerp(floorY - 6, floorY - 50, rise) - bob * 0.5f * rise;
        RenderCompat.drawScaledItem(ctx, fragment, chestX(cx, 0), sideY, 1.8f);
        RenderCompat.drawScaledItem(ctx, magmafish, chestX(cx, 2), sideY, 1.8f);

        if (prize != null) {
            RenderCompat.drawGlintItem(ctx, prize, cx, y, ITEM_SCALE, bt, headline() ? 0xFFD27A : 0x8FE08F);
        } else {
            RenderCompat.drawScaledItem(ctx, magmafish, cx, y, 1.8f);
        }
    }

    private void drawTitle(GuiGraphicsExtractor ctx, int cx, int cy, float t, boolean boom, float bt) {
        int top = cy - 110;
        if (!boom) {
            float in = easeOutCubic(clamp01(t / 10.0f));
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal("LORD JAWBUS").withStyle(ChatFormatting.BOLD), cx, top, 1.5f * in, 0xFFFF7A2A);
            return;
        }
        float in = easeOutCubic(clamp01((bt - RISE_DELAY - 6) / 10.0f));
        if (in <= 0.0f) {
            return;
        }
        Drop drop = won() ? session.wonDrop() : null;
        int lineY = cy + 74;

        if (headline() && drop != null) {
            float pulse = 0.7f + 0.3f * (float) Math.sin(bt * 0.45f);
            int b = (int) (255 * pulse);
            int col = 0xFF000000 | (b << 16) | ((int) (b * 0.75f) << 8) | (int) (b * 0.2f);
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal(drop.displayName().toUpperCase(Locale.ROOT) + "!").withStyle(ChatFormatting.BOLD),
                    cx, top, 1.2f + in * 0.9f, col);
            RenderCompat.centeredText(ctx, this.font,
                    Component.literal("YOU WON! 1x " + drop.displayName() + (session.isLootShared() ? " (Loot Share)" : ""))
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), cx, lineY, GOLD);
            ProfitCalculator.render(ctx, this.font, drop, cx, lineY + 14, bt, true);
            return;
        }
        if (drop != null) {
            RenderCompat.drawScaledCenteredText(ctx, this.font,
                    Component.literal(drop.displayName()).withStyle(ChatFormatting.BOLD), cx, top, 1.0f + in * 0.4f, 0xFFF0C070);
            RenderCompat.centeredText(ctx, this.font,
                    Component.literal("1x " + drop.displayName() + (session.isLootShared() ? " (Loot Share)" : ""))
                            .withStyle(ChatFormatting.YELLOW), cx, lineY, 0xFFE8D48A);
            ProfitCalculator.render(ctx, this.font, drop, cx, lineY + 14, bt, false);
            return;
        }
        RenderCompat.drawScaledCenteredText(ctx, this.font,
                Component.literal("JUST FRAGMENTS").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                cx, top, 1.0f + in * 0.3f, 0xFFB05868);
        RenderCompat.centeredText(ctx, this.font,
                Component.literal("The chests held nothing rare.").withStyle(ChatFormatting.DARK_GRAY),
                cx, lineY, 0xFF6E6E7A);
    }

    private static float hash(int seed) {
        int h = seed * 374761393 + 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0f;
    }
}
