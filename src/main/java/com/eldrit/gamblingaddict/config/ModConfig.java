package com.eldrit.gamblingaddict.config;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.eldrit.gamblingaddict.util.SoundSuppressor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE = new ModConfig();

    public boolean enabled = true;
    public boolean clickToSkip = true;
    public boolean muteDropSounds = true;
    public boolean muteAllGameSounds = false;
    public boolean logGameSounds = false;
    public List<String> mutedDropSounds = new ArrayList<>(SoundSuppressor.DEFAULT_DROP_SOUNDS);
    public float backgroundOpacity = 1.0f;
    public boolean gambleOnEveryKill = true;
    public boolean onlyGambleTopTier = false;
    public float loseBannerSeconds = 1.2f;

    public boolean wikiTikiEnabled = true;
    public float wikiTikiSpeed = 1.0f;
    public float wikiTikiVolume = 0.85f;

    public boolean jawbusEnabled = true;
    public float jawbusSpeed = 1.0f;
    public float jawbusVolume = 0.9f;

    public boolean thunderEnabled = true;
    public float thunderSpeed = 1.0f;
    public float thunderVolume = 0.9f;

    public boolean ragnarokEnabled = true;
    public float ragnarokSpeed = 1.0f;
    public float ragnarokVolume = 0.85f;

    public boolean seaCreatureKillDetection = true;

    public boolean mobCapCounter = false;
    public int mobCapLimit = 60;
    public boolean abilityCooldownHud = false;

    public boolean judgmentCoreEnabled = true;
    public float anvilSpeed = 1.0f;
    public float anvilStrikeVolume = 0.9f;
    public boolean anvilScreenShake = true;
    public boolean anvilSparks = true;
    public float anvilBannerSeconds = 3.5f;

    public boolean primordialEyeEnabled = true;
    public float slotSpeed = 1.0f;
    public float slotReelVolume = 0.85f;
    public boolean reelClicks = true;
    public float reelClickVolume = 0.6f;
    public float jackpotHoldSeconds = 4.0f;

    public boolean wardenHeartEnabled = true;
    public float heartSpeed = 1.0f;
    public float heartVolume = 0.9f;

    public boolean svenDropsEnabled = true;
    public float highRollerSpeed = 1.0f;
    public float highRollerVolume = 0.85f;

    public boolean blazeAttunementHud = false;
    public boolean ragnarockAxeNotify = false;
    public int ragnarockAxeOutput = 1;
    public boolean ragnarockCastTimer = true;

    public boolean lootShareDetection = true;
    public boolean profitTracking = true;

    public float masterVolume = 1.0f;
    public boolean customWinSound = true;
    public float customWinSoundVolume = 1.0f;
    public float customWinSoundPitch = 1.0f;
    public boolean customWinSoundForAnvil = false;

    public float hudX = 0.02f;
    public float hudY = 0.30f;

    public boolean strictDropTierCheck = false;
    public boolean nametagFallback = true;
    public float sightingGraceSeconds = 30.0f;
    public boolean debugLogging = false;

    public static ModConfig get() {
        return INSTANCE;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(GamblingAddictClient.MOD_ID + ".json");
    }

    public static void load() {
        Path file = path();
        try {
            if (Files.exists(file)) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                ModConfig parsed = GSON.fromJson(json, ModConfig.class);
                if (parsed != null) {
                    INSTANCE = parsed;
                }
            }
        } catch (IOException | RuntimeException e) {
            GamblingAddictClient.LOGGER.warn("[GamblingAddict] could not read config, using defaults", e);
            INSTANCE = new ModConfig();
        }
        save();
    }

    public static void save() {
        try {
            Path file = path();
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(INSTANCE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            GamblingAddictClient.LOGGER.warn("[GamblingAddict] could not write config", e);
        }
    }

    public static void resetToDefaults() {
        INSTANCE = new ModConfig();
        save();
    }
}
