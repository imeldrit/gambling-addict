package com.eldrit.gamblingaddict.loot;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum Drop {
    JUDGEMENT_CORE("Judgement Core", "JUDGEMENT_CORE", 4, true, false, of(Boss.VOIDGLOOM_SERAPH), "Judgment Core"),
    PRIMORDIAL_EYE("Primordial Eye", "PRIMORDIAL_EYE", 5, true, false, of(Boss.TARANTULA_BROODFATHER)),

    WARDEN_HEART("Warden Heart", "WARDEN_HEART", 5, true, false, of(Boss.REVENANT_HORROR)),
    SCYTHE_BLADE("Scythe Blade", "SCYTHE_BLADE", 5, false, false, of(Boss.REVENANT_HORROR)),
    BEHEADED_HORROR("Beheaded Horror", "BEHEADED_HORROR", 5, false, false, of(Boss.REVENANT_HORROR)),
    REVENANT_CATALYST("Revenant Catalyst", "REVENANT_CATALYST", 5, false, false, of(Boss.REVENANT_HORROR)),
    REVENANT_VISCERA("Revenant Viscera", "REVENANT_VISCERA", 5, false, false, of(Boss.REVENANT_HORROR)),

    OVERFLUX_CAPACITOR("Overflux Capacitor", "OVERFLUX_CAPACITOR", 4, true, false, of(Boss.SVEN_PACKMASTER)),
    CELESTE_DYE("Celeste Dye", "DYE_CELESTE", 4, true, false, of(Boss.SVEN_PACKMASTER)),
    GRIZZLY_BAIT("Grizzly Bait", "GRIZZLY_BAIT", 4, false, false, of(Boss.SVEN_PACKMASTER), "Grizzly Salmon"),
    RED_CLAW_EGG("Red Claw Egg", "RED_CLAW_EGG", 4, false, false, of(Boss.SVEN_PACKMASTER)),

    TIKI_MASK("Tiki Mask", "TIKI_MASK", 0, true, false, of(Boss.WIKI_TIKI)),
    TROUBLED_BUBBLE("Troubled Bubble", "TROUBLED_BUBBLE", 0, false, false, of(Boss.WIKI_TIKI)),
    AQUAMARINE_DYE("Aquamarine Dye", "DYE_AQUAMARINE", 0, true, false, of(Boss.WIKI_TIKI)),

    BOBBIN_SCRIPTURES("Bobbin' Scriptures", "BOBBIN_SCRIPTURES", 0, false, false,
            of(Boss.WIKI_TIKI, Boss.LORD_JAWBUS, Boss.RAGNAROK), "Bobbing Scriptures", "Scriptures"),
    CARMINE_DYE("Carmine Dye", "DYE_CARMINE", 0, true, false,
            of(Boss.LORD_JAWBUS, Boss.THUNDER, Boss.RAGNAROK)),

    MAGMA_LORD_FRAGMENT("Magma Lord Fragment", "MAGMA_LORD_FRAGMENT", 0, false, false, of(Boss.LORD_JAWBUS)),
    RADIOACTIVE_VIAL("Radioactive Vial", "RADIOACTIVE_VIAL", 0, true, false, of(Boss.LORD_JAWBUS)),
    ATTRIBUTE_SHARD("Attribute Shard", "", 0, false, true, of(Boss.LORD_JAWBUS, Boss.THUNDER), " Shard"),

    FLASH_BOOK("Flash I", "ENCHANTMENT_FLASH_1", 0, false, true, of(Boss.THUNDER), "Enchanted Book"),

    BRIMSTONE_HANDLE("Brimstone Handle", "BRIMSTONE_HANDLE", 0, false, false, of(Boss.RAGNAROK)),
    CHAIN_OF_THE_END_TIMES("Chain of the End Times", "CHAIN_END_TIMES", 0, false, false, of(Boss.RAGNAROK)),
    BURNT_TEXTS("Burnt Texts", "BURNT_TEXTS", 0, true, false, of(Boss.RAGNAROK));

    private final String displayName;
    private final String skyblockId;
    private final int tier;
    private final boolean headline;
    private final boolean needsContext;
    private final Boss[] bosses;
    private final String[] altNames;

    Drop(String displayName, String skyblockId, int tier, boolean headline, boolean needsContext,
         Boss[] bosses, String... altNames) {
        this.displayName = displayName;
        this.skyblockId = skyblockId;
        this.tier = tier;
        this.headline = headline;
        this.needsContext = needsContext;
        this.bosses = bosses;
        this.altNames = altNames;
    }

    private static Boss[] of(Boss... bosses) {
        return bosses;
    }

    public String displayName() {
        return displayName;
    }

    public String skyblockId() {
        return skyblockId;
    }

    public boolean hasPrice() {
        return !skyblockId.isEmpty();
    }

    public int tier() {
        return tier;
    }

    public boolean isHeadline() {
        return headline;
    }

    public Boss boss() {
        return bosses[0];
    }

    public boolean droppedBy(@Nullable Boss boss) {
        if (boss == null) {
            return false;
        }
        for (Boss b : bosses) {
            if (b == boss) {
                return true;
            }
        }
        return false;
    }

    public Boss bossFor(@Nullable Boss context) {
        return droppedBy(context) ? context : bosses[0];
    }

    public boolean matches(String lowercaseLine) {
        if (lowercaseLine.contains(displayName.toLowerCase(Locale.ROOT))) {
            return true;
        }
        for (String alt : altNames) {
            if (lowercaseLine.contains(alt.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static @Nullable Drop findIn(String lowercaseLine, @Nullable Boss context) {
        for (Drop drop : values()) {
            if (!drop.matches(lowercaseLine)) {
                continue;
            }
            if (drop.needsContext && !drop.droppedBy(context)) {
                continue;
            }
            return drop;
        }
        return null;
    }

    public static @Nullable Drop byName(String name) {
        String wanted = name.trim().replace(' ', '_').replace('\'', '_').toUpperCase(Locale.ROOT);
        for (Drop drop : values()) {
            if (drop.name().equals(wanted)) {
                return drop;
            }
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (Drop drop : values()) {
            if (drop.displayName.toLowerCase(Locale.ROOT).equals(lower)) {
                return drop;
            }
        }
        return null;
    }
}
