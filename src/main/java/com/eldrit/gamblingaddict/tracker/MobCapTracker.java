package com.eldrit.gamblingaddict.tracker;

import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.slayer.SlayerTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MobCapTracker {
    private static final Pattern TAG = Pattern.compile("^\\[Lv\\d+\\]\\s+(?<name>.+?)\\s+[\\d.,]+[kKmMbB]?(?:/[\\d.,]+[kKmMbB]?)?\\s*❤");

    private static final Set<String> SEA_CREATURES = Set.of(
            "Squid", "Night Squid", "Sea Walker", "Sea Guardian", "Sea Witch", "Sea Archer", "Rider of the Deep",
            "Rider of The Deep", "Catfish", "Carrot King", "Sea Leech", "Guardian Defender", "Deep Sea Protector",
            "Water Hydra", "The Sea Emperor", "Sea Emperor", "Frozen Steve", "Frosty", "Grinch", "Yeti", "Nutcracker",
            "Reindrake", "Nurse Shark", "Blue Shark", "Tiger Shark", "Great White Shark", "Scarecrow", "Nightmare",
            "Werewolf", "Phantom Fisher", "Grim Reaper", "Oasis Rabbit", "Oasis Sheep", "Water Worm",
            "Poisoned Water Worm", "Flaming Worm", "Lava Blaze", "Lava Pigman", "Zombie Miner", "Abyssal Miner",
            "Agarimoo", "Trash Gobbler", "Dumpster Diver", "Bayou Sludge", "Titanoboa", "Banshee", "Alligator",
            "Snapping Turtle", "Wetwing", "Fried Chicken", "Blue Ringed Octopus", "Mithril Grubber",
            "Bloated Mithril Grubber", "Large Mithril Grubber", "Ent", "Frog Man", "Tadgang", "Nessie",
            "Loch Emperor", "The Loch Emperor", "Fireproof Witch", "Stridersurfer", "Bogged", "Vanessa",
            "Magma Slug", "Moogma", "Lava Leech", "Pyroclastic Worm", "Lava Flame", "Fire Eel", "Taurus",
            "Thunder", "Lord Jawbus", "Plhlegblast", "Fiery Scuttler", "Wiki Tiki", "Ragnarok");

    private static final int SCAN_INTERVAL = 10;

    private static int count = 0;
    private static long lastScan = Long.MIN_VALUE;

    private MobCapTracker() {
    }

    public static int count() {
        return count;
    }

    public static void tick(Minecraft client) {
        if (!ModConfig.get().mobCapCounter || client.level == null || client.player == null) {
            count = 0;
            return;
        }
        long now = SlayerTracker.ticks();
        if (lastScan != Long.MIN_VALUE && now - lastScan < SCAN_INTERVAL) {
            return;
        }
        lastScan = now;

        int found = 0;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!entity.hasCustomName()) {
                continue;
            }
            Component name = entity.getCustomName();
            if (name == null) {
                continue;
            }
            Matcher m = TAG.matcher(SlayerTracker.strip(name.getString()));
            if (m.find() && SEA_CREATURES.contains(m.group("name"))) {
                found++;
            }
        }
        count = found;
    }

    public static String line() {
        int limit = Math.max(1, ModConfig.get().mobCapLimit);
        String colour = count >= limit ? "§c" : (count >= limit * 0.8 ? "§e" : "§a");
        return "§bSea Creatures: " + colour + count + "§7/" + limit;
    }
}
