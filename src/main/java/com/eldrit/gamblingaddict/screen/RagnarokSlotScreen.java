package com.eldrit.gamblingaddict.screen;

import com.eldrit.gamblingaddict.anim.GambleSession;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.loot.Drop;
import com.eldrit.gamblingaddict.render.RenderCompat;
import com.eldrit.gamblingaddict.util.SoundUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class RagnarokSlotScreen extends ReelSlotScreen {
    private static final Drop[] STRIP = {
            Drop.BURNT_TEXTS,
            Drop.BRIMSTONE_HANDLE,
            Drop.CHAIN_OF_THE_END_TIMES,
            Drop.BOBBIN_SCRIPTURES,
            Drop.CARMINE_DYE,
    };

    private static final Theme THEME = new Theme(
            "RAGNAROK",
            0xFF2A0A0A, 0xFF4A1212, 0xFF7A2020, 0xFF160404, 0xFF120404,
            0xFFFF6A1A, 0xFFFFD27A, 0xFFFF7A2A,
            "THE END TIMES PAY OUT!", ChatFormatting.RED, 0xFFFFA070,
            "THE SKY STAYS DARK", "Two out of three. Ragnarok keeps its loot.");

    private static final int EMBERS = 36;

    public RagnarokSlotScreen(GambleSession session) {
        super(Component.literal("Ragnarok"), session, THEME, iconsFor(STRIP),
                indexOf(STRIP, session.wonDrop(), 0));
    }

    @Override
    protected float speedMultiplier() {
        return Math.max(0.1f, ModConfig.get().ragnarokSpeed);
    }

    @Override
    protected float reelVolume() {
        return ModConfig.get().ragnarokVolume;
    }

    @Override
    protected void onSettled(boolean won) {
        if (won) {
            SoundUtil.play(SoundEvents.NOTE_BLOCK_CHIME, 1.0f, reelVolume());
            SoundUtil.play(SoundEvents.NOTE_BLOCK_CHIME, 1.5f, reelVolume());
            SoundUtil.play(SoundEvents.PLAYER_LEVELUP, 0.9f, reelVolume());
            SoundUtil.play(SoundEvents.BLAZE_SHOOT, 0.7f, reelVolume() * 0.6f);
            SoundUtil.playWinSound(true, 1.0f);
        } else {
            SoundUtil.play(SoundEvents.NOTE_BLOCK_BASS, 0.5f, reelVolume());
            SoundUtil.play(SoundEvents.FIRE_EXTINGUISH, 0.8f, 0.6f);
        }
    }

    @Override
    protected void drawBackdrop(GuiGraphicsExtractor ctx, float t, boolean settled, float jt) {
        int glow = RenderCompat.withAlpha(0x3A0800, 0.55f);
        ctx.fill(0, this.height / 2, this.width, this.height, glow);
        for (int i = 0; i < EMBERS; i++) {
            float speed = 0.35f + hash(i * 7) * 0.5f;
            float phase = (t * speed * 0.02f + hash(i * 13)) % 1.0f;
            int x = (int) (hash(i * 31) * this.width) + (int) (Math.sin(t * 0.05f + i) * 6.0f);
            int y = (int) (this.height * (1.0f - phase));
            int size = hash(i * 3) > 0.7f ? 3 : 2;
            int rgb = hash(i * 11) > 0.5f ? 0xFF6A1A : 0xFFC46A;
            ctx.fill(x, y, x + size, y + size, RenderCompat.withAlpha(rgb, (1.0f - phase) * 0.9f));
        }
        if (settled && won()) {
            float flash = clamp01(1.0f - jt / 8.0f) * 0.35f;
            ctx.fill(0, 0, this.width, this.height, RenderCompat.withAlpha(0xFF3A00, flash));
        }
    }

    @Override
    protected void drawExtras(GuiGraphicsExtractor ctx, int cx, int baseY, int totalW, float t, boolean settled, float jt) {
        int halfW = totalW / 2 + 22;
        int top = baseY - WINDOW_H / 2 - 46;
        for (int i = 0; i < 6; i++) {
            int x = cx - halfW + 8 + i * (halfW * 2 - 16) / 5;
            float flicker = 0.6f + 0.4f * (float) Math.sin(t * 0.6f + i * 1.7f);
            int h = 4 + (int) (flicker * 6.0f);
            ctx.fill(x - 1, top - h, x + 2, top, RenderCompat.withAlpha(0xFF6A1A, flicker));
            ctx.fill(x, top - h + 2, x + 1, top, RenderCompat.withAlpha(0xFFE08A, flicker));
        }
    }
}
