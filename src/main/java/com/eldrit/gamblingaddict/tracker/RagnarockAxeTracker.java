package com.eldrit.gamblingaddict.tracker;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.slayer.SlayerTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RagnarockAxeTracker {

    private static final Pattern STRENGTH_GAIN = Pattern.compile(
            "(?:gain(?:ed)?|grants?|granted)\\s+\\+?(?<amount>[\\d,.]+)\\s*❁?\\s*Strength", Pattern.CASE_INSENSITIVE);
    private static final Pattern DURATION = Pattern.compile("(?<secs>\\d+)\\s*s(?:econds?)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LORE_STRENGTH = Pattern.compile("Strength:\\s*\\+?(?<base>[\\d,]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COOLDOWN = Pattern.compile("This ability is on cooldown", Pattern.CASE_INSENSITIVE);
    private static final Pattern NO_MANA = Pattern.compile("not have enough mana", Pattern.CASE_INSENSITIVE);

    private static final int CHANNEL_TICKS = 60;
    private static final int BUFF_SECONDS = 10;
    private static final float MULTIPLIER = 1.5f;
    private static final long USE_DEBOUNCE = 10;
    private static final long REPEAT_TICKS = 40;

    private static long channelStart = Long.MIN_VALUE;
    private static int baseStrength = 0;
    private static long lastUse = Long.MIN_VALUE;
    private static long lastFired = Long.MIN_VALUE;
    private static long resultShownAt = Long.MIN_VALUE;

    private RagnarockAxeTracker() {
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            onUse(player, hand);
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            onUse(player, hand);
            return InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(RagnarockAxeTracker::tick);
    }

    private static boolean enabled() {
        ModConfig cfg = ModConfig.get();
        return cfg.ragnarockAxeNotify || cfg.ragnarockCastTimer;
    }

    private static void onUse(Player player, InteractionHand hand) {
        if (!enabled() || hand != InteractionHand.MAIN_HAND || player == null || !player.level().isClientSide()) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !held.getHoverName().getString().toLowerCase(Locale.ROOT).contains("ragnarock")) {
            return;
        }
        long now = SlayerTracker.ticks();
        if (lastUse != Long.MIN_VALUE && now - lastUse < USE_DEBOUNCE) {
            return;
        }
        lastUse = now;
        if (channelStart != Long.MIN_VALUE) {
            return;
        }
        baseStrength = readStrength(held);
        channelStart = now;
        if (ModConfig.get().debugLogging) {
            GamblingAddictClient.LOGGER.info("[GamblingAddict] Ragnarock channel started (base strength {})", baseStrength);
        }
    }

    private static int readStrength(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return 0;
        }
        for (Component line : lore.lines()) {
            Matcher m = LORE_STRENGTH.matcher(SlayerTracker.strip(line.getString()));
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group("base").replace(",", ""));
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static void tick(Minecraft client) {
        if (channelStart == Long.MIN_VALUE || client.player == null) {
            return;
        }
        long elapsed = SlayerTracker.ticks() - channelStart;
        if (elapsed > 2 && client.player.hurtTime > 0) {
            cancel("CANCELLED", "You took damage while channeling");
            return;
        }
        if (elapsed >= CHANNEL_TICKS) {
            complete(expectedGain(), BUFF_SECONDS, "channel finished");
            return;
        }
        if (ModConfig.get().ragnarockCastTimer && elapsed % 2 == 0) {
            float left = (CHANNEL_TICKS - elapsed) / 20.0f;
            int bars = (int) ((CHANNEL_TICKS - elapsed) * 10 / CHANNEL_TICKS);
            String meter = "§c" + "▮".repeat(bars) + "§8" + "▯".repeat(10 - bars);
            title(client,
                    Component.literal(String.format(Locale.ROOT, "Channeling %.1fs", left)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.literal(meter + "  §7don't take damage"),
                    0, 12, 4);
        }
    }

    private static int expectedGain() {
        return Math.round(baseStrength * MULTIPLIER);
    }

    public static void onChat(String plain) {
        if (!enabled()) {
            return;
        }
        if (channelStart != Long.MIN_VALUE) {
            if (COOLDOWN.matcher(plain).find()) {
                cancel("ON COOLDOWN", "Ragnarock Axe is not ready yet");
                return;
            }
            if (NO_MANA.matcher(plain).find()) {
                cancel("NOT ENOUGH MANA", "Ragnarock Axe needs 500 mana");
                return;
            }
        }
        Matcher m = STRENGTH_GAIN.matcher(plain);
        if (!m.find()) {
            return;
        }
        long now = SlayerTracker.ticks();
        if (lastFired != Long.MIN_VALUE && now - lastFired < REPEAT_TICKS) {
            return;
        }
        int amount;
        try {
            amount = Math.round(Float.parseFloat(m.group("amount").replace(",", "")));
        } catch (NumberFormatException e) {
            amount = expectedGain();
        }
        int seconds = BUFF_SECONDS;
        Matcher d = DURATION.matcher(plain.substring(m.end()));
        if (d.find()) {
            try {
                seconds = Integer.parseInt(d.group("secs"));
            } catch (NumberFormatException ignored) {
            }
        }
        if (ModConfig.get().debugLogging) {
            GamblingAddictClient.LOGGER.info("[GamblingAddict] Ragnarock Axe: +{} Strength for {}s from \"{}\"", amount, seconds, plain);
        }
        complete(amount, seconds, "chat line");
    }

    private static void cancel(String headline, String detail) {
        channelStart = Long.MIN_VALUE;
        Minecraft client = Minecraft.getInstance();
        if (ModConfig.get().ragnarockCastTimer) {
            title(client,
                    Component.literal(headline).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.literal(detail).withStyle(ChatFormatting.GRAY),
                    2, 30, 10);
        }
        if (ModConfig.get().ragnarockAxeNotify && ModConfig.get().ragnarockAxeOutput != 1) {
            announce(Component.literal(headline).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.literal(detail).withStyle(ChatFormatting.GRAY), ModConfig.get().ragnarockAxeOutput);
        }
    }

    private static void complete(int amount, int seconds, String how) {
        long now = SlayerTracker.ticks();
        channelStart = Long.MIN_VALUE;
        if (resultShownAt != Long.MIN_VALUE && now - resultShownAt < REPEAT_TICKS) {
            return;
        }
        resultShownAt = now;
        lastFired = now;
        AbilityCooldownTracker.startBuff("Rage", seconds);

        ModConfig cfg = ModConfig.get();
        Component headline = Component.literal("+" + amount + " ❁ Strength").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        Component detail = Component.literal("Ragnarock Axe - " + seconds + "s").withStyle(ChatFormatting.GRAY);
        if (cfg.debugLogging) {
            GamblingAddictClient.LOGGER.info("[GamblingAddict] Ragnarock complete (+{} for {}s, {})", amount, seconds, how);
        }
        if (cfg.ragnarockCastTimer || (cfg.ragnarockAxeNotify && cfg.ragnarockAxeOutput == 1)) {
            title(Minecraft.getInstance(), headline, detail, 2, 40, 10);
        }
        if (cfg.ragnarockAxeNotify && cfg.ragnarockAxeOutput != 1) {
            announce(headline, detail, cfg.ragnarockAxeOutput);
        }
    }

    private static void title(Minecraft client, Component headline, Component detail, int fadeIn, int stay, int fadeOut) {
        client.execute(() -> {
            client.gui.setTimes(fadeIn, stay, fadeOut);
            client.gui.setTitle(headline);
            client.gui.setSubtitle(detail);
        });
    }

    private static void announce(Component headline, Component detail, int style) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player == null) {
                return;
            }
            if (style == 2) {
                client.gui.setOverlayMessage(
                        Component.empty().append(headline).append(Component.literal("  ")).append(detail), false);
            } else {
                client.player.sendSystemMessage(
                        Component.literal("[GA] ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(headline).append(Component.literal(" ")).append(detail));
            }
        });
    }
}
