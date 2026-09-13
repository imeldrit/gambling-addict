package com.eldrit.gamblingaddict.slayer;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.eldrit.gamblingaddict.anim.AnimationQueue;
import com.eldrit.gamblingaddict.anim.GambleTrigger;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.loot.Boss;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SlayerTracker {
    private static final Pattern BOSS_PATTERN = Pattern.compile(
            "(Revenant Horror|Tarantula Broodfather|Sven Packmaster|Voidgloom Seraph|Inferno Demonlord|Riftstalker Bloodfiend)"
                    + "\\s+(I{1,3}|IV|V)(?![IVX])");

    private static final Pattern BOSS_SLAIN_PATTERN = Pattern.compile(
            "(SLAYER QUEST COMPLETE|NICE! SLAYER BOSS SLAIN|SLAYER BOSS SLAIN)");

    private static final Pattern SLAYER_TYPE_PATTERN = Pattern.compile(
            "(Revenant|Tarantula|Sven|Enderman|Blaze|Vampire|Voidgloom|Inferno|Riftstalker)\\s+Slayer\\s+LVL"
                    + "|worth of\\s+(Zombies|Spiders|Wolves|Endermen|Blazes|Vampires)");

    private static final long KILL_ARM_TIMEOUT = 60;

    private static long killArmedTick = Long.MIN_VALUE;

    private static long memoryTicks() {
        return (long) (ModConfig.get().sightingGraceSeconds * 20.0f);
    }

    private static long clientTicks = 0L;

    private static @Nullable Boss boss = null;
    private static int tier = 0;
    private static final long NEVER = Long.MIN_VALUE;

    private static long lastSeenTick = NEVER;
    private static String lastSource = "none";

    private SlayerTracker() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            clientTicks++;
            AnimationQueue.tick(client);
            firePendingKill();
            if (client.level == null) {
                return;
            }
            if (clientTicks % 10 == 0 && scanScoreboard(client)) {
                return;
            }
            if (clientTicks % 10 == 5 && ModConfig.get().nametagFallback) {
                scanNametags(client);
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) {
                return;
            }
            feedChatLine(strip(message.getString()));
        });
    }

    private static void firePendingKill() {
        if (killArmedTick == Long.MIN_VALUE) {
            return;
        }
        boolean known = boss != null;
        boolean expired = clientTicks - killArmedTick > KILL_ARM_TIMEOUT;
        if (!known && !expired) {
            return;
        }

        killArmedTick = Long.MIN_VALUE;
        if (known) {
            GambleTrigger.onBossSlain(boss, tier, clientTicks);
        } else {
            GamblingAddictClient.LOGGER.info(
                    "[GamblingAddict] saw a slayer kill but could not tell which slayer - no gamble. "
                            + "Run /gamblingaddict debug and check the quest lines.");
        }
    }

    public static boolean matches(Boss expected, int expectedTier) {
        return boss == expected && tier == expectedTier && sightingFresh();
    }

    private static boolean sightingFresh() {
        return lastSeenTick != NEVER && (clientTicks - lastSeenTick) <= memoryTicks();
    }

    public static @Nullable Boss currentBoss() {
        return sightingFresh() ? boss : null;
    }

    public static int currentTier() {
        return sightingFresh() ? tier : 0;
    }

    public static long ticks() {
        return clientTicks;
    }

    public static void simulateKill() {
        killArmedTick = clientTicks;
    }

    public static String describe() {
        if (currentBoss() == null) {
            return "no active slayer boss detected";
        }
        long age = (clientTicks - lastSeenTick) / 20;
        return boss.displayName() + " " + roman(tier) + "  (source: " + lastSource + ", seen " + age + "s ago)";
    }

    private static boolean scanScoreboard(Minecraft client) {
        boolean found = false;
        for (String line : readSidebarLines(client)) {
            if (record(line, "sidebar")) {
                found = true;
            }
        }
        return found;
    }

    private static void scanNametags(Minecraft client) {
        if (client.level == null || client.player == null) {
            return;
        }
        final double maxDistanceSq = 30.0 * 30.0;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!entity.hasCustomName()) {
                continue;
            }
            if (entity.distanceToSqr(client.player) > maxDistanceSq) {
                continue;
            }
            Component name = entity.getCustomName();
            if (name == null) {
                continue;
            }
            record(strip(name.getString()), "nametag");
        }
    }

    private static void feedChatLine(String plain) {
        record(plain, "chat");
        recordQuestType(plain);

        if (BOSS_SLAIN_PATTERN.matcher(plain).find()) {
            if (boss != null) {
                lastSeenTick = clientTicks;
            }
            killArmedTick = clientTicks;
        }
    }

    private static void recordQuestType(String plain) {
        Matcher m = SLAYER_TYPE_PATTERN.matcher(plain);
        if (!m.find()) {
            return;
        }
        String word = m.group(1) != null ? m.group(1) : m.group(2);
        Boss found = switch (word) {
            case "Revenant", "Zombies" -> Boss.REVENANT_HORROR;
            case "Tarantula", "Spiders" -> Boss.TARANTULA_BROODFATHER;
            case "Sven", "Wolves" -> Boss.SVEN_PACKMASTER;
            case "Enderman", "Endermen", "Voidgloom" -> Boss.VOIDGLOOM_SERAPH;
            case "Blaze", "Blazes", "Inferno" -> Boss.INFERNO_DEMONLORD;
            case "Vampire", "Vampires", "Riftstalker" -> Boss.RIFTSTALKER_BLOODFIEND;
            default -> null;
        };
        if (found == null) {
            return;
        }
        if (found != boss && ModConfig.get().debugLogging) {
            GamblingAddictClient.LOGGER.info("[GamblingAddict] slayer type from quest line -> {}", found.displayName());
        }
        boss = found;
        lastSeenTick = clientTicks;
        lastSource = "quest line";
    }

    private static boolean record(String plain, String source) {
        Matcher m = BOSS_PATTERN.matcher(plain);
        if (!m.find()) {
            return false;
        }
        Boss found = Boss.byDisplayName(m.group(1));
        int foundTier = fromRoman(m.group(2));
        if (found == null || foundTier == 0) {
            return false;
        }

        boolean changed = found != boss || foundTier != tier;
        boss = found;
        tier = foundTier;
        lastSeenTick = clientTicks;
        lastSource = source;

        if (changed && ModConfig.get().debugLogging) {
            GamblingAddictClient.LOGGER.info("[GamblingAddict] slayer target -> {} {} (via {})",
                    found.displayName(), roman(foundTier), source);
        }
        return true;
    }

    public static List<String> readSidebarLines(Minecraft client) {
        List<String> lines = new ArrayList<>();
        if (client.level == null) {
            return lines;
        }
        try {
            Scoreboard scoreboard = client.level.getScoreboard();
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (objective == null) {
                return lines;
            }
            lines.add(strip(objective.getDisplayName().getString()));

            for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
                PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
                Component decorated = PlayerTeam.formatNameForTeam(team, entry.ownerName());
                lines.add(strip(decorated.getString()));
            }
        } catch (Throwable t) {
            GamblingAddictClient.LOGGER.debug("[GamblingAddict] sidebar read failed", t);
        }
        return lines;
    }

    public static String strip(String raw) {
        String s = ChatFormatting.stripFormatting(raw);
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\s+", " ").trim();
    }

    private static int fromRoman(String r) {
        return switch (r) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            default -> 0;
        };
    }

    private static String roman(int t) {
        return switch (t) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "?";
        };
    }
}
