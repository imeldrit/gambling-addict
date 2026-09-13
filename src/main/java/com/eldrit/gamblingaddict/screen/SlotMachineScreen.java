package com.eldrit.gamblingaddict.screen;

import com.eldrit.gamblingaddict.anim.GambleSession;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.loot.Drop;
import com.eldrit.gamblingaddict.util.DropIcons;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class SlotMachineScreen extends ReelSlotScreen {
    private static final Theme THEME = new Theme(
            "SLAYER RNG",
            0xFF23131B, 0xFF3A1E2B, 0xFF562F3F, 0xFF120A0F, 0xFF0B0710,
            0xFFD4AF37, 0xFFFFE9A0, 0xFFD4AF37,
            "JACKPOT!", ChatFormatting.LIGHT_PURPLE, 0xFFE9A0FF,
            "SO CLOSE", "Two out of three. No drop.");

    public SlotMachineScreen(GambleSession session) {
        super(Component.literal("Slayer RNG"), session, THEME, strip(), 0);
    }

    private static ItemStack[] strip() {
        return new ItemStack[] {
                DropIcons.of(Drop.PRIMORDIAL_EYE),
                DropIcons.shriveledWasp(),
                DropIcons.brickRedDye(),
        };
    }

    @Override
    protected float speedMultiplier() {
        return Math.max(0.1f, ModConfig.get().slotSpeed);
    }

    @Override
    protected float reelVolume() {
        return ModConfig.get().slotReelVolume;
    }

    @Override
    protected String winLine() {
        return "YOU WON! 1x Primordial Eye" + (session.isLootShared() ? " (Loot Share)" : "");
    }
}
