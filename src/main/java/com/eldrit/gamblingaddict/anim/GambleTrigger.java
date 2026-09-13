package com.eldrit.gamblingaddict.anim;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.loot.Boss;
import com.eldrit.gamblingaddict.loot.Drop;
import com.eldrit.gamblingaddict.screen.AnvilTestScreen;
import com.eldrit.gamblingaddict.screen.HighRollerScreen;
import com.eldrit.gamblingaddict.screen.JawbusExplosionScreen;
import com.eldrit.gamblingaddict.screen.PulsingHeartScreen;
import com.eldrit.gamblingaddict.screen.RagnarokSlotScreen;
import com.eldrit.gamblingaddict.screen.SlotMachineScreen;
import com.eldrit.gamblingaddict.screen.ThunderLightningScreen;
import com.eldrit.gamblingaddict.screen.WikiTikiSlotScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class GambleTrigger {
    private static final long KILL_DEBOUNCE_TICKS = 100;
    private static final long DROP_REPEAT_TICKS = 200;
    private static final long NEVER = Long.MIN_VALUE;

    private static long lastKillTick = NEVER;
    private static String lastDropText = "";
    private static long lastDropTick = NEVER;

    private GambleTrigger() {
    }

    public enum Machine {
        ANVIL, SLOTS, HEART, HIGH_ROLLER, WIKI_TIKI, JAWBUS, THUNDER, RAGNAROK
    }

    public static void onBossSlain(@Nullable Boss boss, int tier, long nowTicks) {
        ModConfig cfg = ModConfig.get();

        if (!cfg.enabled) {
            reject("mod disabled");
            return;
        }
        if (!cfg.gambleOnEveryKill) {
            reject("gambleOnEveryKill is off");
            return;
        }
        if (lastKillTick != NEVER && nowTicks - lastKillTick < KILL_DEBOUNCE_TICKS) {
            reject("debounced (second announcement line for the same kill)");
            return;
        }
        if (AnimationQueue.isBusy()) {
            reject("an animation is already on screen");
            return;
        }
        if (boss == null) {
            reject("could not identify which boss was being fought");
            return;
        }

        Machine machine = machineFor(boss);
        if (machine == null) {
            return;
        }
        if (!boss.isSeaCreature() && !tierAllows(boss, tier)) {
            return;
        }

        lastKillTick = nowTicks;
        GamblingAddictClient.LOGGER.info("[GamblingAddict] boss slain ({} {}) - gambling on {}",
                boss.displayName(), tier == 0 ? "" : tier, machine);

        AnimationQueue.play(screenFor(machine), GambleSession.pending(boss));
    }

    public static boolean isRepeat(String plain, long nowTicks) {
        boolean repeat = plain.equals(lastDropText)
                && lastDropTick != NEVER
                && nowTicks - lastDropTick < DROP_REPEAT_TICKS;
        lastDropText = plain;
        lastDropTick = nowTicks;
        if (repeat) {
            GamblingAddictClient.LOGGER.info(
                    "[GamblingAddict] ignoring repeated drop line within {}s: \"{}\"",
                    DROP_REPEAT_TICKS / 20, plain);
        }
        return repeat;
    }

    public static boolean onRareDrop(Component message, Drop drop, @Nullable Boss context, boolean lootShared) {
        GambleSession active = AnimationQueue.currentSession();
        if (active != null && active.peek() == GambleSession.Outcome.PENDING) {
            active.markWin(message, drop, lootShared);
            GamblingAddictClient.LOGGER.info("[GamblingAddict] {} landed mid-gamble - flipping to WIN{}",
                    drop.displayName(), lootShared ? " (loot share)" : "");
            return true;
        }
        if (AnimationQueue.isBusy()) {
            return false;
        }

        Boss boss = drop.bossFor(context);
        Machine machine = machineFor(boss);
        if (machine == null) {
            return false;
        }
        GamblingAddictClient.LOGGER.info("[GamblingAddict] {} drop arrived before kill detection - instant WIN on {}",
                drop.displayName(), machine);
        GambleSession session = GambleSession.decided(boss, GambleSession.Outcome.WIN, message, drop)
                .lootShared(lootShared);
        AnimationQueue.play(screenFor(machine), session);
        return true;
    }

    public static boolean isEnabled(Boss boss) {
        ModConfig cfg = ModConfig.get();
        return switch (boss) {
            case VOIDGLOOM_SERAPH -> cfg.judgmentCoreEnabled;
            case TARANTULA_BROODFATHER -> cfg.primordialEyeEnabled;
            case REVENANT_HORROR -> cfg.wardenHeartEnabled;
            case SVEN_PACKMASTER -> cfg.svenDropsEnabled;
            case WIKI_TIKI -> cfg.wikiTikiEnabled;
            case LORD_JAWBUS -> cfg.jawbusEnabled;
            case THUNDER -> cfg.thunderEnabled;
            case RAGNAROK -> cfg.ragnarokEnabled;
            default -> false;
        };
    }

    public static @Nullable Machine machineFor(Boss boss) {
        Machine machine = switch (boss) {
            case VOIDGLOOM_SERAPH -> Machine.ANVIL;
            case TARANTULA_BROODFATHER -> Machine.SLOTS;
            case REVENANT_HORROR -> Machine.HEART;
            case SVEN_PACKMASTER -> Machine.HIGH_ROLLER;
            case WIKI_TIKI -> Machine.WIKI_TIKI;
            case LORD_JAWBUS -> Machine.JAWBUS;
            case THUNDER -> Machine.THUNDER;
            case RAGNAROK -> Machine.RAGNAROK;
            default -> null;
        };
        if (machine == null) {
            reject(boss.displayName() + " has no machine");
            return null;
        }
        if (!isEnabled(boss)) {
            reject(boss.displayName() + " animation is disabled in the config");
            return null;
        }
        return machine;
    }

    private static boolean tierAllows(Boss boss, int tier) {
        if (!ModConfig.get().onlyGambleTopTier) {
            return true;
        }
        if (tier == 0) {
            return true;
        }
        int required = switch (boss) {
            case VOIDGLOOM_SERAPH, SVEN_PACKMASTER -> 4;
            case TARANTULA_BROODFATHER, REVENANT_HORROR -> 5;
            default -> 0;
        };
        if (required == 0 || tier == required) {
            return true;
        }
        reject("tier " + tier + " cannot drop this item and onlyGambleTopTier is on");
        return false;
    }

    public static Function<GambleSession, Screen> screenFor(Machine machine) {
        return switch (machine) {
            case ANVIL -> AnvilTestScreen::new;
            case SLOTS -> SlotMachineScreen::new;
            case HEART -> PulsingHeartScreen::new;
            case HIGH_ROLLER -> HighRollerScreen::new;
            case WIKI_TIKI -> WikiTikiSlotScreen::new;
            case JAWBUS -> JawbusExplosionScreen::new;
            case THUNDER -> ThunderLightningScreen::new;
            case RAGNAROK -> RagnarokSlotScreen::new;
        };
    }

    public static @Nullable Function<GambleSession, Screen> screenFor(Boss boss) {
        return switch (boss) {
            case VOIDGLOOM_SERAPH -> AnvilTestScreen::new;
            case TARANTULA_BROODFATHER -> SlotMachineScreen::new;
            case REVENANT_HORROR -> PulsingHeartScreen::new;
            case SVEN_PACKMASTER -> HighRollerScreen::new;
            case WIKI_TIKI -> WikiTikiSlotScreen::new;
            case LORD_JAWBUS -> JawbusExplosionScreen::new;
            case THUNDER -> ThunderLightningScreen::new;
            case RAGNAROK -> RagnarokSlotScreen::new;
            default -> null;
        };
    }

    private static void reject(String why) {
        GamblingAddictClient.LOGGER.info("[GamblingAddict] kill seen but no gamble: {}", why);
    }
}
