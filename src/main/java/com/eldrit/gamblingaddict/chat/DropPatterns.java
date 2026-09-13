package com.eldrit.gamblingaddict.chat;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DropPatterns {
    public static final Pattern RARE_DROP_BANNER = Pattern.compile(
            "^(?<share>\\[?LOOT ?SHARE\\]?\\s*[:\\-]?\\s*)?"
                    + "(?<banner>PRAY RNGESUS DROP|INSANE DROP|CRAZY RARE DROP|VERY RARE DROP|RARE DROP|PET DROP)!",
            Pattern.CASE_INSENSITIVE);

    public static final Pattern LOOT_SHARE_NOTICE = Pattern.compile(
            "^LOOT SHARE\\s+You received loot for assisting\\s+(?<player>[A-Za-z0-9_]{2,16})!?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PLAYER_CHAT = Pattern.compile(
            "^(\\[\\d+\\]\\s*)?[^:]{0,48}?[A-Za-z0-9_]{2,16}:\\s");

    public record Banner(String rarity, boolean lootShare) {
    }

    private DropPatterns() {
    }

    public static boolean isPlayerChat(String plain) {
        return PLAYER_CHAT.matcher(plain).find();
    }

    public static boolean hasRarityBanner(String plain) {
        return RARE_DROP_BANNER.matcher(plain).find();
    }

    public static @Nullable Banner banner(String plain) {
        Matcher m = RARE_DROP_BANNER.matcher(plain);
        if (!m.find()) {
            return null;
        }
        return new Banner(m.group("banner").toUpperCase(Locale.ROOT), m.group("share") != null);
    }

    public static @Nullable String lootShareFrom(String plain) {
        Matcher m = LOOT_SHARE_NOTICE.matcher(plain);
        return m.find() ? m.group("player") : null;
    }

    public static boolean isRareDropOf(String plain, String... itemNames) {
        if (isPlayerChat(plain) || !RARE_DROP_BANNER.matcher(plain).find()) {
            return false;
        }
        String haystack = plain.toLowerCase(Locale.ROOT);
        for (String item : itemNames) {
            if (haystack.contains(item.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
