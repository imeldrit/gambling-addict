package com.eldrit.gamblingaddict.chat;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.eldrit.gamblingaddict.anim.AnimationQueue;
import com.eldrit.gamblingaddict.anim.GambleSession;
import com.eldrit.gamblingaddict.anim.GambleTrigger;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.fishing.SeaCreatureTracker;
import com.eldrit.gamblingaddict.loot.Boss;
import com.eldrit.gamblingaddict.loot.Drop;
import com.eldrit.gamblingaddict.slayer.SlayerTracker;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class ChatInterceptor {
    private static final long LOOT_SHARE_WINDOW_TICKS = 40;

    private static long lootShareNoticeTick = Long.MIN_VALUE;

    private ChatInterceptor() {
    }

    public static void register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register(ChatInterceptor::allow);
    }

    private static boolean allow(Component message, boolean overlay) {
        if (overlay) {
            return true;
        }
        ModConfig cfg = ModConfig.get();
        String raw = message.getString();
        String plain = SlayerTracker.strip(raw);

        if (cfg.debugLogging) {
            GamblingAddictClient.LOGGER.info("[chat] raw=\"{}\" plain=\"{}\"", raw, plain);
        }
        if (!cfg.enabled) {
            return true;
        }

        if (DropPatterns.isPlayerChat(plain)) {
            return true;
        }

        DropPatterns.LootShare share = DropPatterns.lootShare(plain);
        if (share != null) {
            lootShareNoticeTick = SlayerTracker.ticks();
            if (!cfg.lootShareDetection) {
                return true;
            }
            return !SeaCreatureTracker.onLootShare(message, share, context());
        }

        DropPatterns.Banner banner = DropPatterns.banner(plain);
        if (banner == null) {
            return true;
        }

        boolean lootShared = banner.lootShare() || recentlyLootShared();
        Boss context = context();
        Drop drop = Drop.findIn(plain.toLowerCase(Locale.ROOT), context);

        if (cfg.debugLogging) {
            GamblingAddictClient.LOGGER.info("[GamblingAddict] banner={} lootShare={} context={} drop={} line=\"{}\"",
                    banner.rarity(), lootShared, context == null ? "none" : context.displayName(),
                    drop == null ? "none" : drop.name(), plain);
        }

        if (drop == null) {
            GamblingAddictClient.LOGGER.info(
                    "[GamblingAddict] rare-drop line matched no configured item: \"{}\"", plain);
            return true;
        }
        if (lootShared && !cfg.lootShareDetection) {
            GamblingAddictClient.LOGGER.info("[GamblingAddict] {} was loot-shared and loot share detection is off",
                    drop.displayName());
            return true;
        }
        if (!GambleTrigger.isEnabled(drop.bossFor(context))) {
            return true;
        }
        if (GambleTrigger.isRepeat(plain, SlayerTracker.ticks())) {
            return true;
        }

        GamblingAddictClient.LOGGER.info("[GamblingAddict] {} drop{}: {}", drop.displayName(),
                lootShared ? " (loot share)" : "", plain);
        return !GambleTrigger.onRareDrop(message, drop, context, lootShared);
    }

    private static boolean recentlyLootShared() {
        return lootShareNoticeTick != Long.MIN_VALUE
                && SlayerTracker.ticks() - lootShareNoticeTick <= LOOT_SHARE_WINDOW_TICKS;
    }

    private static @Nullable Boss context() {
        GambleSession active = AnimationQueue.currentSession();
        if (active != null && active.boss() != null) {
            return active.boss();
        }
        Boss sea = SeaCreatureTracker.recentBoss();
        if (sea != null) {
            return sea;
        }
        return SlayerTracker.currentBoss();
    }

    public static String describeMatch(String raw) {
        String plain = SlayerTracker.strip(raw);
        if (DropPatterns.isPlayerChat(plain)) {
            return "NO MATCH: looks like player chat, which is never intercepted";
        }
        DropPatterns.LootShare share = DropPatterns.lootShare(plain);
        if (share != null) {
            Drop shared = share.item() == null ? null : Drop.findIn(share.item().toLowerCase(Locale.ROOT), context());
            return "LOOT SHARE from " + share.player() + (share.item() == null ? " (no item named)" : " -> " + share.item())
                    + (shared == null ? " - counts as a kill of the boss last seen" : " - MATCH " + shared.displayName())
                    + "; boss context: " + (context() == null ? "none" : context().displayName());
        }
        DropPatterns.Banner banner = DropPatterns.banner(plain);
        Boss context = context();
        Drop drop = Drop.findIn(plain.toLowerCase(Locale.ROOT), context);
        Drop anyDrop = drop != null ? drop : Drop.findIn(plain.toLowerCase(Locale.ROOT), null);

        if (banner != null && drop != null) {
            return "MATCH -> " + drop.displayName() + " (" + drop.bossFor(context).displayName() + ")"
                    + (banner.lootShare() ? " [loot share]" : "");
        }
        if (banner != null && anyDrop == null) {
            for (Drop d : Drop.values()) {
                if (d.matches(plain.toLowerCase(Locale.ROOT))) {
                    return "NO MATCH: " + d.displayName() + " needs " + d.boss().displayName()
                            + " as the current boss (context is " + (context == null ? "none" : context.displayName()) + ")";
                }
            }
        }
        if (banner == null && anyDrop != null) {
            return "NO MATCH: item found, but no rarity banner at the start of the line";
        }
        if (banner != null) {
            return "NO MATCH: rarity banner found, but no known item name in the line";
        }
        return "NO MATCH: neither a rarity banner nor a known item name";
    }
}
