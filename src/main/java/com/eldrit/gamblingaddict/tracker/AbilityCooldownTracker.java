package com.eldrit.gamblingaddict.tracker;

import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.slayer.SlayerTracker;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AbilityCooldownTracker {
    private static final Pattern COOLDOWN = Pattern.compile(
            "This ability is on cooldown for (?<secs>\\d+)s", Pattern.CASE_INSENSITIVE);

    private static long cooldownUntil = Long.MIN_VALUE;
    private static long buffUntil = Long.MIN_VALUE;
    private static String buffLabel = "";

    private AbilityCooldownTracker() {
    }

    public static void onChat(String plain) {
        Matcher m = COOLDOWN.matcher(plain);
        if (m.find()) {
            cooldownUntil = SlayerTracker.ticks() + Long.parseLong(m.group("secs")) * 20L;
        }
    }

    public static void startBuff(String label, int seconds) {
        buffLabel = label;
        buffUntil = SlayerTracker.ticks() + seconds * 20L;
    }

    private static long remaining(long until) {
        if (until == Long.MIN_VALUE) {
            return 0L;
        }
        long left = until - SlayerTracker.ticks();
        return left <= 0 ? 0L : left;
    }

    public static @Nullable String cooldownLine() {
        if (!ModConfig.get().abilityCooldownHud) {
            return null;
        }
        long left = remaining(cooldownUntil);
        if (left <= 0) {
            return null;
        }
        return "§6Ability: §c" + ((left + 19) / 20) + "s";
    }

    public static @Nullable String buffLine() {
        if (!ModConfig.get().abilityCooldownHud) {
            return null;
        }
        long left = remaining(buffUntil);
        if (left <= 0) {
            return null;
        }
        return "§c" + buffLabel + ": §e" + String.format(java.util.Locale.ROOT, "%.1fs", left / 20.0);
    }
}
