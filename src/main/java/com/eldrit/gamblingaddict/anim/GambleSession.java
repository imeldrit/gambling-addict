package com.eldrit.gamblingaddict.anim;

import com.eldrit.gamblingaddict.loot.Boss;
import com.eldrit.gamblingaddict.loot.Drop;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class GambleSession {
    public enum Outcome {
        PENDING,
        WIN,
        LOSE
    }

    private final @Nullable Boss boss;
    private volatile Outcome outcome;
    private volatile @Nullable Component dropMessage;
    private volatile @Nullable Drop wonDrop;
    private volatile boolean lootShared;

    private GambleSession(@Nullable Boss boss, Outcome outcome, @Nullable Component dropMessage, @Nullable Drop wonDrop) {
        this.boss = boss;
        this.outcome = outcome;
        this.dropMessage = dropMessage;
        this.wonDrop = wonDrop;
    }

    public static GambleSession pending(@Nullable Boss boss) {
        return new GambleSession(boss, Outcome.PENDING, null, null);
    }

    public static GambleSession decided(@Nullable Boss boss, Outcome outcome,
                                        @Nullable Component dropMessage, @Nullable Drop drop) {
        return new GambleSession(boss, outcome, dropMessage, drop);
    }

    public static GambleSession win(Drop drop) {
        return decided(drop.boss(), Outcome.WIN, null, drop);
    }

    public static GambleSession loss(@Nullable Boss boss) {
        return decided(boss, Outcome.LOSE, null, null);
    }

    public void markWin(Component message, @Nullable Drop drop, boolean lootShared) {
        if (outcome == Outcome.PENDING) {
            dropMessage = message;
            wonDrop = drop;
            this.lootShared = lootShared;
            outcome = Outcome.WIN;
        }
    }

    public Outcome resolve() {
        if (outcome == Outcome.PENDING) {
            outcome = Outcome.LOSE;
        }
        return outcome;
    }

    public Outcome peek() {
        return outcome;
    }

    public boolean isWin() {
        return outcome == Outcome.WIN;
    }

    public @Nullable Boss boss() {
        return boss;
    }

    public @Nullable Drop wonDrop() {
        return wonDrop;
    }

    public @Nullable Component dropMessage() {
        return dropMessage;
    }

    public boolean isLootShared() {
        return lootShared;
    }

    public GambleSession lootShared(boolean value) {
        this.lootShared = value;
        return this;
    }
}
