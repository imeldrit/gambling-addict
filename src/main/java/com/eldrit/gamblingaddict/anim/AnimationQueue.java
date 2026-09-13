package com.eldrit.gamblingaddict.anim;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.eldrit.gamblingaddict.screen.AnimationScreen;
import com.eldrit.gamblingaddict.util.SoundSuppressor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class AnimationQueue {
    private static final int OPEN_TIMEOUT_TICKS = 100;

    private static final int ORPHAN_GRACE_TICKS = 20;

    private static volatile boolean busy = false;
    private static volatile @Nullable GambleSession current = null;
    private static int orphanTicks = 0;

    private static @Nullable Function<GambleSession, Screen> pendingFactory = null;
    private static @Nullable GambleSession pendingSession = null;
    private static int waitingTicks = 0;
    private static boolean loggedWait = false;

    private AnimationQueue() {
    }

    public static boolean isBusy() {
        return busy;
    }

    public static @Nullable GambleSession currentSession() {
        return current;
    }

    public static void play(Function<GambleSession, Screen> factory, GambleSession session) {
        busy = true;
        current = session;
        pendingFactory = factory;
        pendingSession = session;
        waitingTicks = 0;
        loggedWait = false;

        Minecraft.getInstance().execute(AnimationQueue::tryOpen);
    }

    private static void tryOpen() {
        Function<GambleSession, Screen> factory = pendingFactory;
        GambleSession session = pendingSession;
        if (factory == null || session == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            abandon("no player/world");
            return;
        }
        if (client.screen != null) {
            if (!loggedWait) {
                loggedWait = true;
                GamblingAddictClient.LOGGER.info(
                        "[GamblingAddict] a screen is open ({}), holding the gamble until it closes",
                        client.screen.getClass().getSimpleName());
            }
            return;
        }

        pendingFactory = null;
        pendingSession = null;
        try {
            client.setScreen(factory.apply(session));
        } catch (Throwable t) {
            GamblingAddictClient.LOGGER.error("[GamblingAddict] failed to open animation screen", t);
            finish(session);
        }
    }

    private static void abandon(String why) {
        GambleSession session = pendingSession;
        pendingFactory = null;
        pendingSession = null;
        GamblingAddictClient.LOGGER.info("[GamblingAddict] gamble abandoned: {}", why);
        finish(session);
    }

    public static void tick(Minecraft client) {
        if (pendingFactory != null) {
            waitingTicks++;
            if (waitingTicks > OPEN_TIMEOUT_TICKS) {
                abandon("a screen stayed open for " + (OPEN_TIMEOUT_TICKS / 20) + "s");
            } else {
                tryOpen();
            }
            orphanTicks = 0;
            return;
        }

        if (!busy || client.screen instanceof AnimationScreen) {
            orphanTicks = 0;
            if (!busy && SoundSuppressor.isActive()) {
                SoundSuppressor.end();
            }
            return;
        }
        if (++orphanTicks > ORPHAN_GRACE_TICKS) {
            GamblingAddictClient.LOGGER.warn(
                    "[GamblingAddict] animation queue looked stuck (busy with no animation on screen) - clearing");
            finish(current);
            orphanTicks = 0;
        }
    }

    public static void finish(@Nullable GambleSession session) {
        busy = false;
        current = null;
        pendingFactory = null;
        pendingSession = null;
        if (session == null) {
            return;
        }
        Component message = session.dropMessage();
        if (message == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(message);
            }
        });
    }
}
