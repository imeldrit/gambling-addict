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

public class WikiTikiSlotScreen extends ReelSlotScreen {
    private static final Drop[] STRIP = {
            Drop.TIKI_MASK,
            Drop.BOBBIN_SCRIPTURES,
            Drop.AQUAMARINE_DYE,
            Drop.TROUBLED_BUBBLE,
    };

    private static final Theme THEME = new Theme(
            "WIKI TIKI",
            0xFF1A2612, 0xFF2E4220, 0xFF4B6A32, 0xFF0E160A, 0xFF0A120A,
            0xFF4FD1A0, 0xFFC8FFE8, 0xFF7FE8C0,
            "TIKI JACKPOT!", ChatFormatting.AQUA, 0xFF9FF0D8,
            "THE TOTEM FROWNS", "Two masks out of three. No drop.");

    private static final int TOTEM_WOOD = 0xFF6B4A2B;
    private static final int TOTEM_WOOD_DARK = 0xFF4A3220;
    private static final int[] TOTEM_FACES = {0xFFE9573F, 0xFF8E44AD, 0xFF27AE60, 0xFF2980B9};

    public WikiTikiSlotScreen(GambleSession session) {
        super(Component.literal("Wiki Tiki"), session, THEME, iconsFor(STRIP),
                indexOf(STRIP, session.wonDrop(), 0));
    }

    @Override
    protected float speedMultiplier() {
        return Math.max(0.1f, ModConfig.get().wikiTikiSpeed);
    }

    @Override
    protected float reelVolume() {
        return ModConfig.get().wikiTikiVolume;
    }

    @Override
    protected void onSettled(boolean won) {
        if (won) {
            SoundUtil.play(SoundEvents.PLAYER_LEVELUP, 1.0f, reelVolume());
            SoundUtil.play(SoundEvents.NOTE_BLOCK_PLING, 1.5f, reelVolume() * 0.8f);
            SoundUtil.playWinSound(true, 1.0f);
        } else {
            SoundUtil.play(SoundEvents.NOTE_BLOCK_BASS, 0.6f, reelVolume());
            SoundUtil.play(SoundEvents.VILLAGER_NO, 0.8f, 0.6f);
        }
    }

    @Override
    protected void drawExtras(GuiGraphicsExtractor ctx, int cx, int baseY, int totalW, float t, boolean settled, float jt) {
        int left = cx - totalW / 2 - 60;
        int right = cx + totalW / 2 + 62;
        float sway = (float) Math.sin(t * 0.08f) * 2.0f;
        drawTotem(ctx, left, baseY + 30 + (int) sway, t, settled && won());
        drawTotem(ctx, right - 14, baseY + 30 - (int) sway, t + 40.0f, settled && won());
    }

    private void drawTotem(GuiGraphicsExtractor ctx, int x, int bottom, float t, boolean celebrate) {
        int w = 14;
        int h = 16;
        for (int i = 0; i < 4; i++) {
            int y = bottom - (i + 1) * h;
            int face = TOTEM_FACES[(i + (celebrate ? (int) (t / 3.0f) : 0)) % TOTEM_FACES.length];
            ctx.fill(x, y, x + w, y + h, TOTEM_WOOD);
            ctx.fill(x, y, x + w, y + 2, TOTEM_WOOD_DARK);
            ctx.fill(x + 2, y + 4, x + w - 2, y + h - 4, face);
            ctx.fill(x + 3, y + 6, x + 5, y + 8, 0xFFFFFFFF);
            ctx.fill(x + w - 5, y + 6, x + w - 3, y + 8, 0xFFFFFFFF);
            ctx.fill(x + 4, y + h - 6, x + w - 4, y + h - 5, celebrate ? 0xFFFFFFFF : 0xFF1A1A1A);
        }
        ctx.fill(x - 2, bottom, x + w + 2, bottom + 3, TOTEM_WOOD_DARK);
        if (celebrate) {
            for (int i = 0; i < 6; i++) {
                float phase = (t * 0.2f + hash(i * 17)) % 1.0f;
                int px = x + (int) (hash(i * 5) * w);
                int py = bottom - 4 * h - (int) (phase * 24.0f);
                ctx.fill(px, py, px + 2, py + 2, RenderCompat.withAlpha(0x7FE8C0, 1.0f - phase));
            }
        }
    }
}
