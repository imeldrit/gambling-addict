package com.eldrit.gamblingaddict;

import com.eldrit.gamblingaddict.chat.ChatInterceptor;
import com.eldrit.gamblingaddict.command.DebugCommand;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.fishing.SeaCreatureTracker;
import com.eldrit.gamblingaddict.slayer.SlayerTracker;
import com.eldrit.gamblingaddict.sound.ModSounds;
import com.eldrit.gamblingaddict.tracker.TrackerHud;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GamblingAddictClient implements ClientModInitializer {
    public static final String MOD_ID = "gamblingaddict";
    public static final Logger LOGGER = LoggerFactory.getLogger("GamblingAddict");

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        ModSounds.register();

        SlayerTracker.register();
        SeaCreatureTracker.register();
        ChatInterceptor.register();
        TrackerHud.register();
        DebugCommand.register();

        LOGGER.info("[GamblingAddict] initialised (client-only)");
    }
}
