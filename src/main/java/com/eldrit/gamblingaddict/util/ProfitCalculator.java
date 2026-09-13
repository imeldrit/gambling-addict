package com.eldrit.gamblingaddict.util;

import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.loot.Drop;
import com.eldrit.gamblingaddict.render.RenderCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalLong;

public final class ProfitCalculator {
    public static final int GREEN = 0xFF55FF55;
    public static final int GREEN_DIM = 0xFF3FBF3F;
    public static final int GOLD = 0xFFFFAA00;

    private static final Map<Drop, Long> ESTIMATES = new EnumMap<>(Drop.class);

    static {
        ESTIMATES.put(Drop.JUDGEMENT_CORE, 45_000_000L);
        ESTIMATES.put(Drop.PRIMORDIAL_EYE, 110_000_000L);
        ESTIMATES.put(Drop.WARDEN_HEART, 55_000_000L);
        ESTIMATES.put(Drop.SCYTHE_BLADE, 4_000_000L);
        ESTIMATES.put(Drop.BEHEADED_HORROR, 5_000_000L);
        ESTIMATES.put(Drop.REVENANT_CATALYST, 300_000L);
        ESTIMATES.put(Drop.REVENANT_VISCERA, 100_000L);
        ESTIMATES.put(Drop.OVERFLUX_CAPACITOR, 60_000_000L);
        ESTIMATES.put(Drop.CELESTE_DYE, 400_000_000L);
        ESTIMATES.put(Drop.GRIZZLY_BAIT, 50_000L);
        ESTIMATES.put(Drop.RED_CLAW_EGG, 1_000_000L);
        ESTIMATES.put(Drop.TIKI_MASK, 100_000_000L);
        ESTIMATES.put(Drop.TROUBLED_BUBBLE, 30_000_000L);
        ESTIMATES.put(Drop.AQUAMARINE_DYE, 1_000_000_000L);
        ESTIMATES.put(Drop.BOBBIN_SCRIPTURES, 2_000_000L);
        ESTIMATES.put(Drop.CARMINE_DYE, 1_500_000_000L);
        ESTIMATES.put(Drop.MAGMA_LORD_FRAGMENT, 300_000L);
        ESTIMATES.put(Drop.RADIOACTIVE_VIAL, 100_000_000L);
        ESTIMATES.put(Drop.ATTRIBUTE_SHARD, 500_000L);
        ESTIMATES.put(Drop.FLASH_BOOK, 10_000_000L);
        ESTIMATES.put(Drop.BRIMSTONE_HANDLE, 5_000_000L);
        ESTIMATES.put(Drop.CHAIN_OF_THE_END_TIMES, 10_000_000L);
        ESTIMATES.put(Drop.BURNT_TEXTS, 100_000_000L);
    }

    public record Value(long coins, boolean estimate) {
    }

    private ProfitCalculator() {
    }

    public static void prefetch(@Nullable Drop drop) {
        if (drop != null && drop.hasPrice()) {
            PriceLookup.prefetch(drop.skyblockId());
        }
    }

    public static @Nullable Value valueOf(@Nullable Drop drop) {
        if (drop == null) {
            return null;
        }
        if (drop.hasPrice()) {
            OptionalLong live = PriceLookup.cached(drop.skyblockId());
            if (live.isPresent()) {
                return new Value(live.getAsLong(), false);
            }
        }
        Long estimate = ESTIMATES.get(drop);
        return estimate == null ? null : new Value(estimate, true);
    }

    public static String coinsText(@Nullable Drop drop) {
        Value v = valueOf(drop);
        if (v == null) {
            return "";
        }
        return "+ " + (v.estimate() ? "~" : "") + PriceLookup.formatCoins(v.coins()) + " Coins";
    }

    public static String suffix(@Nullable Drop drop) {
        if (!ModConfig.get().profitTracking) {
            return "";
        }
        Value v = valueOf(drop);
        if (v == null) {
            return "";
        }
        return " (+" + (v.estimate() ? "~" : "") + PriceLookup.formatCoins(v.coins()) + " Coins)";
    }

    public static boolean enabled() {
        return ModConfig.get().profitTracking;
    }

    public static void render(GuiGraphicsExtractor ctx, Font font, @Nullable Drop drop,
                              int centerX, int y, float t, boolean aggressive) {
        if (!enabled()) {
            return;
        }
        String text = coinsText(drop);
        if (text.isEmpty()) {
            return;
        }
        if (!aggressive) {
            RenderCompat.centeredText(ctx, font,
                    Component.literal(text).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    centerX, y, GREEN);
            return;
        }
        boolean gold = ((int) (t / 4.0f)) % 2 == 0;
        float scale = 1.15f + 0.15f * (float) Math.sin(t * 0.5f);
        RenderCompat.drawScaledCenteredText(ctx, font,
                Component.literal(text).withStyle(gold ? ChatFormatting.GOLD : ChatFormatting.GREEN, ChatFormatting.BOLD),
                centerX, y + font.lineHeight / 2.0f, scale, gold ? GOLD : GREEN);
    }
}
