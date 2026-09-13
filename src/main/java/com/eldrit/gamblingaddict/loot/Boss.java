package com.eldrit.gamblingaddict.loot;

import org.jetbrains.annotations.Nullable;

public enum Boss {
    REVENANT_HORROR("Revenant Horror", Kind.SLAYER),
    TARANTULA_BROODFATHER("Tarantula Broodfather", Kind.SLAYER),
    SVEN_PACKMASTER("Sven Packmaster", Kind.SLAYER),
    VOIDGLOOM_SERAPH("Voidgloom Seraph", Kind.SLAYER),
    INFERNO_DEMONLORD("Inferno Demonlord", Kind.SLAYER),
    RIFTSTALKER_BLOODFIEND("Riftstalker Bloodfiend", Kind.SLAYER),

    WIKI_TIKI("Wiki Tiki", Kind.SEA_CREATURE),
    LORD_JAWBUS("Lord Jawbus", Kind.SEA_CREATURE),
    THUNDER("Thunder", Kind.SEA_CREATURE),
    RAGNAROK("Ragnarok", Kind.SEA_CREATURE);

    public enum Kind {
        SLAYER,
        SEA_CREATURE
    }

    private final String displayName;
    private final Kind kind;

    Boss(String displayName, Kind kind) {
        this.displayName = displayName;
        this.kind = kind;
    }

    public String displayName() {
        return displayName;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isSeaCreature() {
        return kind == Kind.SEA_CREATURE;
    }

    @Nullable
    public static Boss byDisplayName(String name) {
        for (Boss boss : values()) {
            if (boss.displayName.equals(name)) {
                return boss;
            }
        }
        return null;
    }
}
