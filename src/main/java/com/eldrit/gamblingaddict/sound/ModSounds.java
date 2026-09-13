package com.eldrit.gamblingaddict.sound;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;

public final class ModSounds {
    public static final Identifier SLOT_MACHINE_WIN_ID =
            Identifier.fromNamespaceAndPath(GamblingAddictClient.MOD_ID, "slotmachine");

    public static SoundEvent SLOT_MACHINE_WIN;

    private ModSounds() {
    }

    public static void register() {
        SLOT_MACHINE_WIN = Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                SLOT_MACHINE_WIN_ID,
                SoundEvent.createVariableRangeEvent(SLOT_MACHINE_WIN_ID));
    }
}
