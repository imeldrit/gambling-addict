package com.eldrit.gamblingaddict.util;

import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class SoundUtil {
    private SoundUtil() {
    }

    public static void play(SoundEvent sound, float pitch, float volume) {
        Minecraft client = Minecraft.getInstance();
        if (client.getSoundManager() == null) {
            return;
        }
        float v = volume * ModConfig.get().masterVolume;
        if (v <= 0.0f) {
            return;
        }
        client.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, v));
    }

    public static void play(SoundEvent sound, float pitch) {
        play(sound, pitch, 1.0f);
    }

    public static void play(Holder<SoundEvent> sound, float pitch, float volume) {
        if (sound != null) {
            play(sound.value(), pitch, volume);
        }
    }

    public static void play(Holder<SoundEvent> sound, float pitch) {
        play(sound, pitch, 1.0f);
    }

    public static void playCustom(SoundEvent sound, float pitch, float volume) {
        Minecraft client = Minecraft.getInstance();
        if (client.getSoundManager() == null || sound == null) {
            return;
        }
        float v = volume * ModConfig.get().masterVolume;
        if (v <= 0.0f) {
            return;
        }
        client.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, v));
    }

    public static boolean isAvailable(SoundEvent sound) {
        if (sound == null) {
            return false;
        }
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        return manager != null && manager.getSoundEvent(sound.location()) != null;
    }

    public static void playWinSound(boolean useCustom, float fallbackPitch) {
        ModConfig cfg = ModConfig.get();
        if (useCustom && cfg.customWinSound && isAvailable(ModSounds.SLOT_MACHINE_WIN)) {
            playCustom(ModSounds.SLOT_MACHINE_WIN, cfg.customWinSoundPitch, cfg.customWinSoundVolume);
            return;
        }
        play(SoundEvents.PLAYER_LEVELUP, fallbackPitch, 1.0f);
    }
}
