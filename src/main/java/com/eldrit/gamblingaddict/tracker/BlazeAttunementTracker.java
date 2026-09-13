package com.eldrit.gamblingaddict.tracker;

import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.slayer.SlayerTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class BlazeAttunementTracker {
    private static final Pattern BOSS_TAG = Pattern.compile("Inferno Demonlord|Quazii|Typhoeus");
    private static final Pattern ATTUNEMENT = Pattern.compile("\\b(ASHEN|SPIRIT|AURIC|CRYSTAL)\\b");
    private static final double PAIR_RANGE_SQ = 8.0 * 8.0;
    private static final int SCAN_INTERVAL = 4;

    private static @Nullable String attunement = null;
    private static long lastScan = Long.MIN_VALUE;

    private record Tag(String text, double x, double y, double z) {
    }

    private BlazeAttunementTracker() {
    }

    public static @Nullable String attunement() {
        return attunement;
    }

    public static void tick(Minecraft client) {
        if (!ModConfig.get().blazeAttunementHud || client.level == null || client.player == null) {
            attunement = null;
            return;
        }
        long now = SlayerTracker.ticks();
        if (lastScan != Long.MIN_VALUE && now - lastScan < SCAN_INTERVAL) {
            return;
        }
        lastScan = now;

        List<Tag> bosses = new ArrayList<>();
        List<Tag> shields = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!entity.hasCustomName()) {
                continue;
            }
            Component name = entity.getCustomName();
            if (name == null) {
                continue;
            }
            String plain = SlayerTracker.strip(name.getString());
            Tag tag = new Tag(plain, entity.getX(), entity.getY(), entity.getZ());
            if (BOSS_TAG.matcher(plain).find()) {
                bosses.add(tag);
            } else if (ATTUNEMENT.matcher(plain.toUpperCase(Locale.ROOT)).find() && plain.length() < 24) {
                shields.add(tag);
            }
        }

        String found = null;
        double best = Double.MAX_VALUE;
        for (Tag shield : shields) {
            for (Tag boss : bosses) {
                double dx = shield.x() - boss.x();
                double dy = shield.y() - boss.y();
                double dz = shield.z() - boss.z();
                double d = dx * dx + dy * dy + dz * dz;
                if (d <= PAIR_RANGE_SQ && d < best) {
                    best = d;
                    var m = ATTUNEMENT.matcher(shield.text().toUpperCase(Locale.ROOT));
                    if (m.find()) {
                        found = m.group(1);
                    }
                }
            }
        }
        attunement = found;
    }

    public static @Nullable String line() {
        if (attunement == null) {
            return null;
        }
        String colour = switch (attunement) {
            case "ASHEN" -> "§7";
            case "SPIRIT" -> "§f";
            case "AURIC" -> "§e";
            case "CRYSTAL" -> "§b";
            default -> "§f";
        };
        return "§6Attunement: " + colour + "§l" + attunement;
    }
}
