package com.eldrit.gamblingaddict.tracker;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.slayer.SlayerTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RagnarockAxeTracker {
    private static final Pattern STRENGTH_GAIN = Pattern.compile(
            "(?:gain(?:ed)?|grants?|granted)\\s+\\+?(?<amount>[\\d,.]+)\\s*❁?\\s*Strength", Pattern.CASE_INSENSITIVE);
    private static final Pattern DURATION = Pattern.compile("(?<secs>\\d+)\\s*s(?:econds?)?\\b", Pattern.CASE_INSENSITIVE);
    private static final int DEFAULT_SECONDS = 10;
    private static final long REPEAT_TICKS = 40;

    private static long lastFired = Long.MIN_VALUE;

    private RagnarockAxeTracker() {
    }

    public static void onChat(String plain) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.ragnarockAxeNotify) {
            return;
        }
        Matcher m = STRENGTH_GAIN.matcher(plain);
        if (!m.find()) {
            return;
        }
        long now = SlayerTracker.ticks();
        if (lastFired != Long.MIN_VALUE && now - lastFired < REPEAT_TICKS) {
            return;
        }
        lastFired = now;

        String amount = m.group("amount");
        int seconds = DEFAULT_SECONDS;
        Matcher d = DURATION.matcher(plain.substring(m.end()));
        if (d.find()) {
            try {
                seconds = Integer.parseInt(d.group("secs"));
            } catch (NumberFormatException ignored) {
            }
        }
        if (cfg.debugLogging) {
            GamblingAddictClient.LOGGER.info("[GamblingAddict] Ragnarock Axe: +{} Strength for {}s from \"{}\"", amount, seconds, plain);
        }
        AbilityCooldownTracker.startBuff("Rage", seconds);
        announce(amount, seconds, cfg.ragnarockAxeOutput);
    }

    private static void announce(String amount, int seconds, int style) {
        Minecraft client = Minecraft.getInstance();
        Component headline = Component.literal("+" + amount + " ❁ Strength").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        Component detail = Component.literal("Ragnarock Axe - " + seconds + "s").withStyle(ChatFormatting.GRAY);
        client.execute(() -> {
            if (client.player == null) {
                return;
            }
            switch (style) {
                case 1 -> {
                    client.gui.setTimes(5, 40, 10);
                    client.gui.setTitle(headline);
                    client.gui.setSubtitle(detail);
                }
                case 2 -> client.gui.setOverlayMessage(
                        Component.empty().append(headline).append(Component.literal("  ")).append(detail), false);
                default -> client.player.sendSystemMessage(
                        Component.literal("[GA] ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(headline).append(Component.literal(" ")).append(detail));
            }
        });
    }
}
