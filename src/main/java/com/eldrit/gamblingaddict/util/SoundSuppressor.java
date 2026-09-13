package com.eldrit.gamblingaddict.util;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.eldrit.gamblingaddict.config.ModConfig;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

import java.util.List;
import java.util.Locale;

public final class SoundSuppressor {

    public static final List<String> DEFAULT_DROP_SOUNDS = List.of(
            "minecraft:block.note_block.pling",
            "minecraft:block.note_block.harp",
            "minecraft:block.note_block.bell",
            "minecraft:block.note_block.chime",
            "minecraft:block.note_block.xylophone",
            "minecraft:entity.player.levelup",
            "minecraft:ui.toast.challenge_complete",
            "minecraft:entity.wither.spawn",
            "minecraft:entity.ender_dragon.growl",
            "minecraft:entity.firework_rocket.twinkle",
            "minecraft:entity.firework_rocket.blast");

    private static volatile boolean active = false;

    private SoundSuppressor() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void begin() {
        active = true;
    }

    public static void end() {
        active = false;
    }

    public static boolean isOwn(SoundInstance sound) {
        return sound.getSource() == SoundSource.UI && sound.isRelative();
    }

    public static boolean shouldCancel(SoundInstance sound) {
        if (!active || sound == null) {
            return false;
        }
        ModConfig cfg = ModConfig.get();
        if (isOwn(sound)) {
            return false;
        }
        String id = sound.getIdentifier() == null ? "" : sound.getIdentifier().toString();
        if (cfg.logGameSounds) {
            GamblingAddictClient.LOGGER.info("[GamblingAddict] sound during gamble: {} ({})", id, sound.getSource());
        }
        if (cfg.muteAllGameSounds) {
            return true;
        }
        if (!cfg.muteDropSounds) {
            return false;
        }
        String lower = id.toLowerCase(Locale.ROOT);
        for (String muted : cfg.mutedDropSounds) {
            if (muted == null || muted.isBlank()) {
                continue;
            }
            String m = muted.toLowerCase(Locale.ROOT);
            if (m.indexOf(':') < 0) {
                m = "minecraft:" + m;
            }
            if (lower.equals(m)) {
                return true;
            }
        }
        return false;
    }
}
