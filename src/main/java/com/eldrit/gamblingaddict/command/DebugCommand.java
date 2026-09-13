package com.eldrit.gamblingaddict.command;

import com.eldrit.gamblingaddict.chat.ChatInterceptor;
import com.eldrit.gamblingaddict.config.ConfigManager;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.config.gui.ConfigScreen;
import com.eldrit.gamblingaddict.fishing.SeaCreatureTracker;
import com.eldrit.gamblingaddict.loot.Boss;
import com.eldrit.gamblingaddict.loot.Drop;
import com.eldrit.gamblingaddict.slayer.SlayerTracker;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public final class DebugCommand {
    private static final SuggestionProvider<FabricClientCommandSource> DROP_NAMES = (ctx, builder) -> {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (Drop drop : Drop.values()) {
            String name = drop.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(typed)) {
                builder.suggest(name);
            }
        }
        builder.suggest("loss");
        return builder.buildFuture();
    };

    private static final SuggestionProvider<FabricClientCommandSource> BOSS_NAMES = (ctx, builder) -> {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (Boss boss : Boss.values()) {
            String name = boss.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(typed)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    private DebugCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            var root = dispatcher.register(ClientCommands.literal("gamblingaddict")

                    .then(ClientCommands.literal("config")
                            .executes(ctx -> {
                                openConfig();
                                return 1;
                            }))

                    .then(test())

                    .then(ClientCommands.literal("status")
                            .executes(ctx -> {
                                feedback(ctx.getSource(), ChatFormatting.GRAY, "slayer: " + SlayerTracker.describe());
                                feedback(ctx.getSource(), ChatFormatting.GRAY, "sea creatures: " + SeaCreatureTracker.describe());
                                feedback(ctx.getSource(), ChatFormatting.GRAY,
                                        "enabled=" + ModConfig.get().enabled
                                                + "  lootShare=" + ModConfig.get().lootShareDetection
                                                + "  profit=" + ModConfig.get().profitTracking
                                                + "  killDetection=" + ModConfig.get().seaCreatureKillDetection);
                                return 1;
                            }))

                    .then(ClientCommands.literal("debug")
                            .executes(ctx -> {
                                ModConfig.get().debugLogging = !ModConfig.get().debugLogging;
                                ModConfig.save();
                                feedback(ctx.getSource(), ChatFormatting.YELLOW,
                                        "chat regex logger " + (ModConfig.get().debugLogging ? "ON" : "OFF")
                                                + " (written to latest.log)");
                                dumpSidebar(ctx.getSource());
                                dumpTags(ctx.getSource());
                                return 1;
                            }))

                    .then(ClientCommands.literal("tags")
                            .executes(ctx -> {
                                dumpTags(ctx.getSource());
                                return 1;
                            }))

                    .then(ClientCommands.literal("testkill")
                            .executes(ctx -> {
                                feedback(ctx.getSource(), ChatFormatting.YELLOW,
                                        "simulating a slayer kill - tracker says: " + SlayerTracker.describe());
                                SlayerTracker.simulateKill();
                                return 1;
                            })
                            .then(ClientCommands.argument("boss", StringArgumentType.word())
                                    .suggests(BOSS_NAMES)
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "boss");
                                        Boss boss = bossByName(name);
                                        if (boss == null) {
                                            feedback(ctx.getSource(), ChatFormatting.RED, "unknown boss: " + name);
                                            return 0;
                                        }
                                        feedback(ctx.getSource(), ChatFormatting.YELLOW,
                                                "simulating a " + boss.displayName() + " kill");
                                        if (boss.isSeaCreature()) {
                                            SeaCreatureTracker.simulateKill(boss);
                                        } else {
                                            ConfigManager.previewLoss(boss);
                                        }
                                        return 1;
                                    })))

                    .then(ClientCommands.literal("match")
                            .then(ClientCommands.argument("text", StringArgumentType.greedyString())
                                    .executes(ctx -> {
                                        String text = StringArgumentType.getString(ctx, "text");
                                        feedback(ctx.getSource(), ChatFormatting.YELLOW, ChatInterceptor.describeMatch(text));
                                        return 1;
                                    })))

                    .then(ClientCommands.literal("reload")
                            .executes(ctx -> {
                                ModConfig.load();
                                feedback(ctx.getSource(), ChatFormatting.GREEN, "config reloaded");
                                return 1;
                            }))

                    .executes(ctx -> {
                        openConfig();
                        help(ctx.getSource());
                        return 1;
                    }));
            dispatcher.register(ClientCommands.literal("ga").redirect(root));

            dispatcher.register(ClientCommands.literal("gambling")
                    .then(test())
                    .then(ClientCommands.literal("config").executes(ctx -> {
                        openConfig();
                        return 1;
                    }))
                    .executes(ctx -> {
                        help(ctx.getSource());
                        return 1;
                    }));
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> test() {
        return ClientCommands.literal("test")
                .then(ClientCommands.argument("drop", StringArgumentType.word())
                        .suggests(DROP_NAMES)
                        .executes(ctx -> runTest(ctx.getSource(), StringArgumentType.getString(ctx, "drop"), null))
                        .then(ClientCommands.argument("boss", StringArgumentType.word())
                                .suggests(BOSS_NAMES)
                                .executes(ctx -> runTest(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "drop"),
                                        StringArgumentType.getString(ctx, "boss")))))
                .executes(ctx -> {
                    feedback(ctx.getSource(), ChatFormatting.YELLOW, "usage: /gambling test <drop> | /gambling test loss <boss>");
                    feedback(ctx.getSource(), ChatFormatting.GRAY, "drops: " + String.join(", ", dropNames()));
                    return 1;
                });
    }

    private static int runTest(FabricClientCommandSource source, String dropName, String bossName) {
        if (dropName.equalsIgnoreCase("loss") || dropName.equalsIgnoreCase("lose")) {
            Boss boss = bossName == null ? null : bossByName(bossName);
            if (boss == null) {
                feedback(source, ChatFormatting.RED, "usage: /gambling test loss <boss>   bosses: "
                        + String.join(", ", bossNames()));
                return 0;
            }
            feedback(source, ChatFormatting.YELLOW, "forcing a losing " + boss.displayName() + " gamble");
            ConfigManager.previewLoss(boss);
            return 1;
        }
        Drop drop = Drop.byName(dropName);
        if (drop == null) {
            feedback(source, ChatFormatting.RED, "unknown drop: " + dropName + "   try: " + String.join(", ", dropNames()));
            return 0;
        }
        Boss boss = bossName == null ? drop.boss() : bossByName(bossName);
        if (boss == null || !drop.droppedBy(boss)) {
            boss = drop.boss();
        }
        feedback(source, ChatFormatting.YELLOW, "forcing " + drop.displayName() + " on the " + boss.displayName() + " machine");
        ConfigManager.open(boss, com.eldrit.gamblingaddict.anim.GambleSession.decided(
                boss, com.eldrit.gamblingaddict.anim.GambleSession.Outcome.WIN, null, drop));
        return 1;
    }

    private static Boss bossByName(String name) {
        String wanted = name.trim().replace(' ', '_').toUpperCase(Locale.ROOT);
        for (Boss boss : Boss.values()) {
            if (boss.name().equals(wanted)) {
                return boss;
            }
        }
        for (Boss boss : Boss.values()) {
            if (boss.displayName().toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT))) {
                return boss;
            }
        }
        return null;
    }

    private static List<String> dropNames() {
        List<String> names = new java.util.ArrayList<>();
        for (Drop drop : Drop.values()) {
            names.add(drop.name().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private static List<String> bossNames() {
        List<String> names = new java.util.ArrayList<>();
        for (Boss boss : Boss.values()) {
            names.add(boss.name().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private static void help(FabricClientCommandSource source) {
        feedback(source, ChatFormatting.GOLD, "Gambling Addict");
        feedback(source, ChatFormatting.GRAY, "/gamblingaddict config       - options screen   (alias: /ga)");
        feedback(source, ChatFormatting.GRAY, "/gambling test <drop>        - force a machine with that drop");
        feedback(source, ChatFormatting.GRAY, "/gambling test loss <boss>   - force a losing gamble");
        feedback(source, ChatFormatting.GRAY, "/gamblingaddict status       - what the trackers currently see");
        feedback(source, ChatFormatting.GRAY, "/gamblingaddict debug        - toggle the chat regex logger");
        feedback(source, ChatFormatting.GRAY, "/gamblingaddict tags         - list nearby boss nametags the tracker can see");
        feedback(source, ChatFormatting.GRAY, "/gamblingaddict testkill [boss] - simulate a boss kill");
        feedback(source, ChatFormatting.GRAY, "/gamblingaddict match <text> - test a drop line");
        feedback(source, ChatFormatting.GRAY, "/gamblingaddict reload       - re-read the config file");
    }

    private static void openConfig() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(new ConfigScreen()));
    }

    private static void dumpSidebar(FabricClientCommandSource source) {
        List<String> lines = SlayerTracker.readSidebarLines(Minecraft.getInstance());
        if (lines.isEmpty()) {
            feedback(source, ChatFormatting.RED, "sidebar is empty (not on SkyBlock?)");
            return;
        }
        feedback(source, ChatFormatting.YELLOW, "── sidebar (" + lines.size() + " lines) ──");
        for (String line : lines) {
            feedback(source, ChatFormatting.WHITE, "  \"" + line + "\"");
        }
    }

    private static void dumpTags(FabricClientCommandSource source) {
        List<String> tags = SeaCreatureTracker.nearbyTags(Minecraft.getInstance());
        if (tags.isEmpty()) {
            feedback(source, ChatFormatting.RED, "no mob nametags with a health readout nearby");
            return;
        }
        feedback(source, ChatFormatting.YELLOW, "── nametags (" + tags.size() + ") ──");
        for (String tag : tags) {
            feedback(source, ChatFormatting.WHITE, "  \"" + tag + "\"");
        }
        feedback(source, ChatFormatting.GRAY, "tracker: " + SeaCreatureTracker.describe());
    }

    private static void feedback(FabricClientCommandSource source, ChatFormatting colour, String msg) {
        source.sendFeedback(Component.literal(msg).withStyle(colour));
    }
}
