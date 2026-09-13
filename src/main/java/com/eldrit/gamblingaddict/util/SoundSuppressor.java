package com.eldrit.gamblingaddict.util;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.eldrit.gamblingaddict.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.sounds.SoundSource;

import java.util.EnumMap;
import java.util.Map;

public final class SoundSuppressor {
    private static final Map<SoundSource, Double> SAVED = new EnumMap<>(SoundSource.class);
    private static boolean active = false;

    private SoundSuppressor() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void begin() {
        if (active || !ModConfig.get().muteOtherSounds) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.options == null) {
            return;
        }
        try {
            SAVED.clear();
            for (SoundSource source : SoundSource.values()) {
                if (source == SoundSource.MASTER) {
                    continue;
                }
                OptionInstance<Double> option = client.options.getSoundSourceOptionInstance(source);
                SAVED.put(source, option.get());
                option.set(0.0);
            }
            active = true;

            if (client.getSoundManager() != null) {
                client.getSoundManager().stop();
            }
        } catch (Throwable t) {
            GamblingAddictClient.LOGGER.warn("[GamblingAddict] could not mute game sounds", t);
            end();
        }
    }

    public static void end() {
        if (!active) {
            return;
        }
        active = false;
        Minecraft client = Minecraft.getInstance();
        if (client.options == null) {
            SAVED.clear();
            return;
        }
        try {
            for (Map.Entry<SoundSource, Double> entry : SAVED.entrySet()) {
                client.options.getSoundSourceOptionInstance(entry.getKey()).set(entry.getValue());
            }
        } catch (Throwable t) {
            GamblingAddictClient.LOGGER.error("[GamblingAddict] could not restore game sounds", t);
        } finally {
            SAVED.clear();
        }
    }
}
