package com.eldrit.gamblingaddict.fishing;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.eldrit.gamblingaddict.anim.GambleTrigger;
import com.eldrit.gamblingaddict.chat.DropPatterns;
import com.eldrit.gamblingaddict.loot.Drop;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.loot.Boss;
import com.eldrit.gamblingaddict.slayer.SlayerTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SeaCreatureTracker {
    private static final Pattern NAMETAG = Pattern.compile(
            "(?:\\[Lv\\.?\\s*\\d+\\]\\s*)?(?<name>Wiki Tiki|Lord Jawbus|Thunder|Ragnarok)[^\\dA-Za-z]*"
                    + "(?<hp>[\\d.,]+[kKmMbB]?)(?:/(?<max>[\\d.,]+[kKmMbB]?))?\\s*[❤♥]");

    private static final Map<Boss, Pattern> SPAWN_LINES = Map.of(
            Boss.WIKI_TIKI, Pattern.compile("you have disturbed the Wiki Tiki", Pattern.CASE_INSENSITIVE),
            Boss.LORD_JAWBUS, Pattern.compile("Lord Jawbus has arrived", Pattern.CASE_INSENSITIVE),
            Boss.THUNDER, Pattern.compile("rumble as Thunder emerges", Pattern.CASE_INSENSITIVE),
            Boss.RAGNAROK, Pattern.compile("Ragnarok is here", Pattern.CASE_INSENSITIVE));

    private static final int SCAN_INTERVAL = 1;
    private static final int VANISH_TICKS = 4;
    private static final double KILL_RANGE_SQ = 48.0 * 48.0;
    private static final double RETAG_RANGE_SQ = 8.0 * 8.0;
    private static final long CONTEXT_TICKS = 60 * 20;
    private static final long LOOT_SHARE_CONTEXT_TICKS = 12 * 20;
    private static final int RESPAWN_SETTLE_TICKS = 60;

    private static final class Tracked {
        final Boss boss;
        double hp;
        double max;
        long lastSeen;
        double distSq;
        double x;
        double y;
        double z;
        boolean sawZero;

        Tracked(Boss boss) {
            this.boss = boss;
        }
    }

    private static final Map<Integer, Tracked> TRACKED = new HashMap<>();
    private static final Set<Integer> DEAD = new HashSet<>();
    private static @Nullable Level lastLevel = null;
    private static long ticks = 0L;

    private static @Nullable Boss recentBoss = null;
    private static long recentTick = Long.MIN_VALUE;

    private SeaCreatureTracker() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(SeaCreatureTracker::tick);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                onChat(SlayerTracker.strip(message.getString()));
            }
        });
    }

    private static void onChat(String plain) {
        for (Map.Entry<Boss, Pattern> entry : SPAWN_LINES.entrySet()) {
            if (entry.getValue().matcher(plain).find()) {
                remember(entry.getKey());
                if (ModConfig.get().debugLogging) {
                    GamblingAddictClient.LOGGER.info("[GamblingAddict] {} spawned (chat)", entry.getKey().displayName());
                }
                return;
            }
        }
    }

    private static void remember(Boss boss) {
        recentBoss = boss;
        recentTick = ticks;
    }

    public static @Nullable Boss recentBoss() {
        return recentBoss(CONTEXT_TICKS);
    }

    public static @Nullable Boss recentBoss(long withinTicks) {
        if (recentTick == Long.MIN_VALUE || ticks - recentTick > withinTicks) {
            return null;
        }
        return recentBoss;
    }

    private static void tick(Minecraft client) {
        ticks++;
        if (client.level == null || client.player == null) {
            TRACKED.clear();
            DEAD.clear();
            lastLevel = null;
            return;
        }
        if (client.level != lastLevel) {
            TRACKED.clear();
            DEAD.clear();
            lastLevel = client.level;
        }
        if (ticks % SCAN_INTERVAL != 0) {
            return;
        }
        scan(client);
        settle(client);
    }

    private static void scan(Minecraft client) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!entity.hasCustomName()) {
                continue;
            }
            Component custom = entity.getCustomName();
            if (custom == null) {
                continue;
            }
            Matcher m = NAMETAG.matcher(SlayerTracker.strip(custom.getString()));
            if (!m.find()) {
                continue;
            }
            Boss boss = Boss.byDisplayName(m.group("name"));
            if (boss == null || DEAD.contains(entity.getId())) {
                continue;
            }
            Tracked t = TRACKED.get(entity.getId());
            if (t == null) {
                t = new Tracked(boss);
                TRACKED.put(entity.getId(), t);
                if (ModConfig.get().debugLogging) {
                    GamblingAddictClient.LOGGER.info("[GamblingAddict] tracking {} (entity {})",
                            boss.displayName(), entity.getId());
                }
            }
            t.hp = parseNumber(m.group("hp"));
            String max = m.group("max");
            if (max != null) {
                t.max = parseNumber(max);
            }
            if (t.hp <= 0.0) {
                t.sawZero = true;
            }
            t.lastSeen = ticks;
            t.distSq = entity.distanceToSqr(client.player);
            t.x = entity.getX();
            t.y = entity.getY();
            t.z = entity.getZ();
            remember(boss);
        }
    }

    private static void settle(Minecraft client) {
        if (TRACKED.isEmpty()) {
            return;
        }
        boolean playerStable = client.player.isAlive() && client.player.tickCount > RESPAWN_SETTLE_TICKS;

        Iterator<Map.Entry<Integer, Tracked>> it = TRACKED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Tracked> entry = it.next();
            Tracked t = entry.getValue();

            if (t.sawZero) {
                it.remove();
                DEAD.add(entry.getKey());
                onKill(t, "health reached zero");
                continue;
            }
            if (ticks - t.lastSeen < VANISH_TICKS) {
                continue;
            }
            it.remove();

            if (retagged(t)) {
                continue;
            }
            if (!playerStable) {
                lost(t, "player respawning");
                continue;
            }
            if (t.distSq > KILL_RANGE_SQ) {
                lost(t, "too far away to tell");
                continue;
            }
            onKill(t, "nametag vanished nearby");
        }
    }

    private static boolean retagged(Tracked gone) {
        for (Tracked other : TRACKED.values()) {
            if (other == gone || other.boss != gone.boss) {
                continue;
            }
            double dx = other.x - gone.x;
            double dy = other.y - gone.y;
            double dz = other.z - gone.z;
            if (dx * dx + dy * dy + dz * dz <= RETAG_RANGE_SQ) {
                return true;
            }
        }
        return false;
    }

    private static void onKill(Tracked t, String how) {
        remember(t.boss);
        GamblingAddictClient.LOGGER.info("[GamblingAddict] {} died ({}, last hp {})",
                t.boss.displayName(), how, format(t.hp));
        if (!ModConfig.get().seaCreatureKillDetection) {
            GamblingAddictClient.LOGGER.info("[GamblingAddict] sea creature kill detection is off - waiting for a drop line instead");
            return;
        }
        GambleTrigger.onBossSlain(t.boss, 0, SlayerTracker.ticks());
    }

    private static void lost(Tracked t, String why) {
        if (ModConfig.get().debugLogging) {
            GamblingAddictClient.LOGGER.info("[GamblingAddict] lost sight of {} ({})", t.boss.displayName(), why);
        }
    }

    public static boolean onLootShare(Component message, DropPatterns.LootShare share, @Nullable Boss context) {
        String item = share.item();
        Boss boss = context != null && context.isSeaCreature() ? recentBoss(LOOT_SHARE_CONTEXT_TICKS) : context;
        if (boss == null && item != null) {
            Drop guess = Drop.findIn(item.toLowerCase(Locale.ROOT), null);
            if (guess != null) {
                boss = guess.boss();
            }
        }
        GamblingAddictClient.LOGGER.info("[GamblingAddict] loot share from {}{} - {}", share.player(),
                item == null ? "" : " (" + item + ")",
                boss == null ? "no boss seen recently, cannot gamble" : "counting as a " + boss.displayName() + " kill");
        if (boss == null) {
            return false;
        }
        remember(boss);
        GambleTrigger.onBossSlain(boss, 0, SlayerTracker.ticks());
        if (item == null) {
            return false;
        }
        Drop drop = Drop.findIn(item.toLowerCase(Locale.ROOT), boss);
        if (drop == null || !GambleTrigger.isEnabled(drop.bossFor(boss))) {
            return false;
        }
        return GambleTrigger.onRareDrop(message, drop, boss, true);
    }

    public static List<String> nearbyTags(Minecraft client) {
        List<String> tags = new ArrayList<>();
        if (client.level == null || client.player == null) {
            return tags;
        }
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!entity.hasCustomName()) {
                continue;
            }
            Component custom = entity.getCustomName();
            if (custom == null) {
                continue;
            }
            String plain = SlayerTracker.strip(custom.getString());
            if (plain.contains("❤") || plain.contains("♥") || plain.contains("Lv")) {
                tags.add(plain + "  [" + (int) Math.sqrt(entity.distanceToSqr(client.player)) + "m]");
            }
        }
        return tags;
    }

    public static void simulateKill(Boss boss) {        Tracked t = new Tracked(boss);
        t.sawZero = true;
        onKill(t, "simulated");
    }

    public static String describe() {
        if (TRACKED.isEmpty()) {
            Boss recent = recentBoss();
            return recent == null
                    ? "no sea creature boss in sight"
                    : "none in sight; last seen " + recent.displayName() + " " + ((ticks - recentTick) / 20) + "s ago";
        }
        List<String> parts = new ArrayList<>();
        for (Tracked t : TRACKED.values()) {
            parts.add(t.boss.displayName() + " " + format(t.hp) + (t.max > 0 ? "/" + format(t.max) : "")
                    + " (" + (int) Math.sqrt(t.distSq) + "m)");
        }
        return String.join(", ", parts);
    }

    static double parseNumber(String text) {
        String s = text.replace(",", "").trim().toLowerCase(Locale.ROOT);
        double mult = 1.0;
        if (s.endsWith("k")) {
            mult = 1_000.0;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("m")) {
            mult = 1_000_000.0;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("b")) {
            mult = 1_000_000_000.0;
            s = s.substring(0, s.length() - 1);
        }
        try {
            return Double.parseDouble(s) * mult;
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private static String format(double v) {
        if (v >= 1_000_000.0) {
            return String.format(Locale.ROOT, "%.1fM", v / 1_000_000.0);
        }
        if (v >= 1_000.0) {
            return String.format(Locale.ROOT, "%.1fk", v / 1_000.0);
        }
        return String.format(Locale.ROOT, "%.0f", v);
    }
}
