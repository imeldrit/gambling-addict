package com.eldrit.gamblingaddict.config;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.eldrit.gamblingaddict.anim.AnimationQueue;
import com.eldrit.gamblingaddict.anim.GambleSession;
import com.eldrit.gamblingaddict.anim.GambleTrigger;
import com.eldrit.gamblingaddict.config.gui.ConfigCategory;
import com.eldrit.gamblingaddict.config.gui.ConfigControls.ActionOption;
import com.eldrit.gamblingaddict.config.gui.ConfigControls.BooleanOption;
import com.eldrit.gamblingaddict.config.gui.ConfigControls.ChoiceOption;
import com.eldrit.gamblingaddict.config.gui.ConfigControls.FloatOption;
import com.eldrit.gamblingaddict.config.gui.ConfigControls.IntOption;
import com.eldrit.gamblingaddict.config.gui.ConfigControls.SectionHeader;
import com.eldrit.gamblingaddict.config.gui.ConfigScreen;
import com.eldrit.gamblingaddict.fishing.SeaCreatureTracker;
import com.eldrit.gamblingaddict.loot.Boss;
import com.eldrit.gamblingaddict.loot.Drop;
import com.eldrit.gamblingaddict.slayer.SlayerTracker;
import com.eldrit.gamblingaddict.sound.ModSounds;
import com.eldrit.gamblingaddict.util.SoundUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public final class ConfigManager {
    public static final List<String> OUTPUT_STYLES = List.of("Chat", "Title", "Action Bar");

    private static final List<ConfigExtension> EXTENSIONS = new CopyOnWriteArrayList<>();
    private static final List<String> DROP_LABELS = new ArrayList<>();
    private static int testDropIndex = 0;

    static {
        for (Drop drop : Drop.values()) {
            DROP_LABELS.add(drop.displayName() + " (" + drop.boss().displayName() + ")");
        }
    }

    private ConfigManager() {
    }

    public static ModConfig config() {
        return ModConfig.get();
    }

    public static void registerExtension(ConfigExtension extension) {
        if (extension != null && !EXTENSIONS.contains(extension)) {
            EXTENSIONS.add(extension);
        }
    }

    public static List<ConfigExtension> extensions() {
        return List.copyOf(EXTENSIONS);
    }

    public static void saveAll() {
        ModConfig.save();
        for (ConfigExtension extension : EXTENSIONS) {
            try {
                extension.save();
            } catch (RuntimeException e) {
                GamblingAddictClient.LOGGER.warn("[GamblingAddict] config extension failed to save", e);
            }
        }
    }

    public static List<ConfigCategory> build() {
        List<ConfigCategory> categories = new ArrayList<>();
        categories.add(fishing());
        categories.add(combat());
        categories.add(misc());
        for (ConfigExtension extension : EXTENSIONS) {
            try {
                categories.addAll(extension.categories());
            } catch (RuntimeException e) {
                GamblingAddictClient.LOGGER.warn("[GamblingAddict] config extension failed to build its categories", e);
            }
        }
        categories.add(dev());
        return categories;
    }

    private static ConfigCategory fishing() {
        ModConfig cfg = ModConfig.get();
        return new ConfigCategory("Fishing",
                "Sea creature animations and fishing HUD helpers.")
                .add(new SectionHeader("Sea Creature Animations"))
                .add(new BooleanOption("Wiki Tiki",
                        "Tiki mask slot machine on every Wiki Tiki kill. Three matching masks means the drop is real.",
                        () -> cfg.wikiTikiEnabled, v -> cfg.wikiTikiEnabled = v))
                .add(new FloatOption("Wiki Tiki Volume",
                        "Reel and payout volume for the Wiki Tiki machine, before the master volume.",
                        0.0f, 1.0f, 0.05f, "",
                        () -> cfg.wikiTikiVolume, v -> cfg.wikiTikiVolume = v))
                .add(new BooleanOption("Lord Jawbus",
                        "Three chests blow open on every Lord Jawbus kill. A Radioactive Vial or Carmine Dye gets the "
                                + "gold treatment; anything else floats up quietly.",
                        () -> cfg.jawbusEnabled, v -> cfg.jawbusEnabled = v))
                .add(new FloatOption("Jawbus Volume",
                        "Fuse, explosion and reveal volume for the Jawbus chests, before the master volume.",
                        0.0f, 1.0f, 0.05f, "",
                        () -> cfg.jawbusVolume, v -> cfg.jawbusVolume = v))
                .add(new BooleanOption("Thunder",
                        "Lightning judges a book on every Thunder kill. One strike burns it, two charge it into a "
                                + "Flash I book, three shatter it into Carmine Dye.",
                        () -> cfg.thunderEnabled, v -> cfg.thunderEnabled = v))
                .add(new FloatOption("Thunder Volume",
                        "Thunder, strike and reveal volume, before the master volume.",
                        0.0f, 1.0f, 0.05f, "",
                        () -> cfg.thunderVolume, v -> cfg.thunderVolume = v))
                .add(new BooleanOption("Ragnarok",
                        "Nether-red slot machine on every Ragnarok kill.",
                        () -> cfg.ragnarokEnabled, v -> cfg.ragnarokEnabled = v))
                .add(new FloatOption("Ragnarok Volume",
                        "Reel and payout volume for the Ragnarok machine, before the master volume.",
                        0.0f, 1.0f, 0.05f, "",
                        () -> cfg.ragnarokVolume, v -> cfg.ragnarokVolume = v))
                .add(new BooleanOption("Kill Detection",
                        "Sea creatures have no kill message, so the boss nametag is watched instead: the machine "
                                + "runs when its health hits zero or the tag vanishes while you are next to it. Turn this "
                                + "off to only animate when a rare drop line actually arrives.",
                        () -> cfg.seaCreatureKillDetection, v -> cfg.seaCreatureKillDetection = v))

                .add(new SectionHeader("Previews"))
                .add(new ActionOption("Wiki Tiki: Tiki Mask", "Three masks line up.", "Play",
                        () -> preview(Drop.TIKI_MASK)))
                .add(new ActionOption("Wiki Tiki: Miss", "Two out of three.", "Play",
                        () -> previewLoss(Boss.WIKI_TIKI)))
                .add(new ActionOption("Jawbus: Radioactive Vial", "The RNGesus reveal.", "Play",
                        () -> preview(Drop.RADIOACTIVE_VIAL)))
                .add(new ActionOption("Jawbus: Scriptures", "A normal reveal.", "Play",
                        () -> preview(Drop.BOBBIN_SCRIPTURES)))
                .add(new ActionOption("Jawbus: Nothing", "Just fragments.", "Play",
                        () -> previewLoss(Boss.LORD_JAWBUS)))
                .add(new ActionOption("Thunder: Carmine Dye", "Three strikes, red lightning.", "Play",
                        () -> preview(Drop.CARMINE_DYE)))
                .add(new ActionOption("Thunder: Flash I", "Two strikes, the book charges.", "Play",
                        () -> preview(Drop.FLASH_BOOK)))
                .add(new ActionOption("Thunder: Ash", "One strike, the book burns.", "Play",
                        () -> previewLoss(Boss.THUNDER)))
                .add(new ActionOption("Ragnarok: Burnt Texts", "The reels pay out.", "Play",
                        () -> preview(Drop.BURNT_TEXTS)))
                .add(new ActionOption("Ragnarok: Miss", "Two out of three.", "Play",
                        () -> previewLoss(Boss.RAGNAROK)))

                .add(new SectionHeader("HUD Helpers"))
                .add(new BooleanOption("Mob Cap Counter",
                        "Show how many sea creatures are loaded around you against the cap below.",
                        () -> cfg.mobCapCounter, v -> cfg.mobCapCounter = v))
                .add(new IntOption("Mob Cap Limit",
                        "The cap to count against. Hypixel allows 60 per server, but only 5 per player on the "
                                + "Crimson Isle and 20 in the Crystal Hollows.",
                        1, 60, "",
                        () -> cfg.mobCapLimit, v -> cfg.mobCapLimit = v))
                .add(new BooleanOption("Ability Cooldown",
                        "Show a countdown whenever Hypixel says an item ability is on cooldown, and the Ragnarock "
                                + "Axe buff timer once its channel completes.",
                        () -> cfg.abilityCooldownHud, v -> cfg.abilityCooldownHud = v));
    }

    private static ConfigCategory combat() {
        ModConfig cfg = ModConfig.get();
        return new ConfigCategory("Combat",
                "Slayer animations and combat trackers.")
                .add(new SectionHeader("Slayer Animations"))
                .add(new BooleanOption("Voidgloom (Hammer)",
                        "The anvil test on every Voidgloom Seraph kill: the Judgement Core takes three blows.",
                        () -> cfg.judgmentCoreEnabled, v -> cfg.judgmentCoreEnabled = v))
                .add(new FloatOption("Strike Volume",
                        "Volume of each hammer blow, before the master volume.",
                        0.0f, 1.0f, 0.05f, "",
                        () -> cfg.anvilStrikeVolume, v -> cfg.anvilStrikeVolume = v))
                .add(new BooleanOption("Screen Shake",
                        "Shake the view on each hammer impact. Turn this off if motion bothers you.",
                        () -> cfg.anvilScreenShake, v -> cfg.anvilScreenShake = v))
                .add(new BooleanOption("Sparks",
                        "Throw debris off the striking face on contact.",
                        () -> cfg.anvilSparks, v -> cfg.anvilSparks = v))
                .add(new BooleanOption("Tarantula (Slots)",
                        "The three-reel slot machine on every Tarantula Broodfather kill.",
                        () -> cfg.primordialEyeEnabled, v -> cfg.primordialEyeEnabled = v))
                .add(new FloatOption("Reel Volume",
                        "Volume of the pling each reel plays as it locks, for every slot machine in the mod.",
                        0.0f, 1.0f, 0.05f, "",
                        () -> cfg.slotReelVolume, v -> cfg.slotReelVolume = v))
                .add(new BooleanOption("Spin Ticks",
                        "Play a mechanical ticking sound while reels are turning.",
                        () -> cfg.slotSpinTicks, v -> cfg.slotSpinTicks = v))
                .add(new FloatOption("Tick Volume",
                        "Volume of that ticking. Keep it low; it plays several times a second.",
                        0.0f, 1.0f, 0.05f, "",
                        () -> cfg.slotTickVolume, v -> cfg.slotTickVolume = v))
                .add(new BooleanOption("Revenant (Heartbeat)",
                        "The pulsing heart on every Revenant Horror kill.",
                        () -> cfg.wardenHeartEnabled, v -> cfg.wardenHeartEnabled = v))
                .add(new FloatOption("Heartbeat Volume",
                        "Volume of the three heartbeats, before the master volume.",
                        0.0f, 1.0f, 0.05f, "",
                        () -> cfg.heartVolume, v -> cfg.heartVolume = v))
                .add(new BooleanOption("Sven (High Roller)",
                        "The single-reel high roller on every Sven Packmaster kill.",
                        () -> cfg.svenDropsEnabled, v -> cfg.svenDropsEnabled = v))
                .add(new FloatOption("High Roller Volume",
                        "Volume of the reel ticking, before the master volume.",
                        0.0f, 1.0f, 0.05f, "",
                        () -> cfg.highRollerVolume, v -> cfg.highRollerVolume = v))
                .add(new BooleanOption("Only Top Tier",
                        "Only gamble on the tier that can actually drop the headline item (Voidgloom IV, Tarantula V, "
                                + "Revenant V, Sven IV). Off by default: tier detection needs the boss nametag or sidebar, "
                                + "which mods like EntityCulling can hide. An unknown tier always gambles.",
                        () -> cfg.onlyGambleTopTier, v -> cfg.onlyGambleTopTier = v))

                .add(new SectionHeader("Previews"))
                .add(new ActionOption("Voidgloom: Judgement Core", "The Core survives all three blows.", "Play",
                        () -> preview(Drop.JUDGEMENT_CORE)))
                .add(new ActionOption("Voidgloom: Shatter", "The Core breaks on the third blow.", "Play",
                        () -> previewLoss(Boss.VOIDGLOOM_SERAPH)))
                .add(new ActionOption("Tarantula: Primordial Eye", "All three reels land on the Eye.", "Play",
                        () -> preview(Drop.PRIMORDIAL_EYE)))
                .add(new ActionOption("Tarantula: Miss", "Two Eyes and a third symbol.", "Play",
                        () -> previewLoss(Boss.TARANTULA_BROODFATHER)))
                .add(new ActionOption("Revenant: Warden Heart", "The heart holds and pulses gold.", "Play",
                        () -> preview(Drop.WARDEN_HEART)))
                .add(new ActionOption("Revenant: Scythe Blade", "The heart shatters and reveals a blade.", "Play",
                        () -> preview(Drop.SCYTHE_BLADE)))
                .add(new ActionOption("Revenant: Nothing", "The heart shatters with no drop.", "Play",
                        () -> previewLoss(Boss.REVENANT_HORROR)))
                .add(new ActionOption("Sven: Overflux", "Neon borders, thunder and a raid horn.", "Play",
                        () -> preview(Drop.OVERFLUX_CAPACITOR)))
                .add(new ActionOption("Sven: Grizzly Bait", "A modest drop.", "Play",
                        () -> preview(Drop.GRIZZLY_BAIT)))
                .add(new ActionOption("Sven: Nothing", "The reel stops on nothing special.", "Play",
                        () -> previewLoss(Boss.SVEN_PACKMASTER)))

                .add(new SectionHeader("Trackers"))
                .add(new BooleanOption("Blaze Attunement HUD",
                        "Show which attunement the Inferno Demonlord (or its split demons) currently has, read "
                                + "from the boss nametags, so you know which dagger to swing.",
                        () -> cfg.blazeAttunementHud, v -> cfg.blazeAttunementHud = v))
                .add(new BooleanOption("Ragnarock Axe Notification",
                        "Announce the Strength gained when a Ragnarock Axe channel completes.",
                        () -> cfg.ragnarockAxeNotify, v -> cfg.ragnarockAxeNotify = v))
                .add(new ChoiceOption("Ragnarock Output",
                        "Where the Ragnarock Axe notification goes: a chat line, a title in the middle of the "
                                + "screen, or the action bar above the hotbar.",
                        OUTPUT_STYLES,
                        () -> cfg.ragnarockAxeOutput, v -> cfg.ragnarockAxeOutput = v));
    }

    private static ConfigCategory misc() {
        ModConfig cfg = ModConfig.get();
        return new ConfigCategory("Misc",
                "System switches, economy, audio and HUD placement.")
                .add(new SectionHeader("System"))
                .add(new BooleanOption("Enabled",
                        "Master switch. When off, no drop is intercepted and no animation plays.",
                        () -> cfg.enabled, v -> cfg.enabled = v))
                .add(new BooleanOption("Gamble Every Kill",
                        "Pull the lever on every boss kill, not just when a rare drop lands. This is the point of "
                                + "the mod - you will lose almost every time. Turn it off to only animate on an actual drop.",
                        () -> cfg.gambleOnEveryKill, v -> cfg.gambleOnEveryKill = v))
                .add(new BooleanOption("Loot Share Detection",
                        "Also animate drops that reached you through Loot Share, whether Hypixel prints its LOOT "
                                + "SHARE notice beside the drop or prefixes the drop line itself. Off means shared "
                                + "drops are left alone.",
                        () -> cfg.lootShareDetection, v -> cfg.lootShareDetection = v))
                .add(new BooleanOption("Click To Skip",
                        "Let a mouse click end the animation early, the same way Escape already does.",
                        () -> cfg.clickToSkip, v -> cfg.clickToSkip = v))
                .add(new BooleanOption("Mute Game Sounds",
                        "Silence every sound except this mod until the result lands. Hypixel plays its own "
                                + "rare-drop sting, which would otherwise give the outcome away mid-spin.",
                        () -> cfg.muteOtherSounds, v -> cfg.muteOtherSounds = v))
                .add(new FloatOption("Background Opacity",
                        "How solid the animation backdrop is. 1.00 hides chat and the world completely; lower "
                                + "it to let them show through.",
                        0.2f, 1.0f, 0.05f, "",
                        () -> cfg.backgroundOpacity, v -> cfg.backgroundOpacity = v))
                .add(new FloatOption("Loss Banner Time",
                        "Seconds a losing result stays on screen before the GUI closes. Kept short by default - "
                                + "you will see this one a great many times.",
                        0.5f, 5.0f, 0.1f, "s",
                        () -> cfg.loseBannerSeconds, v -> cfg.loseBannerSeconds = v))
                .add(new FloatOption("Win Banner Time",
                        "Seconds a modest win stays on screen before the GUI closes.",
                        1.0f, 10.0f, 0.5f, "s",
                        () -> cfg.anvilBannerSeconds, v -> cfg.anvilBannerSeconds = v))
                .add(new FloatOption("Jackpot Hold",
                        "Seconds a headline win celebrates before the GUI closes. About 6.50 covers the full "
                                + "custom win sound.",
                        1.0f, 12.0f, 0.5f, "s",
                        () -> cfg.jackpotHoldSeconds, v -> cfg.jackpotHoldSeconds = v))

                .add(new SectionHeader("Economy"))
                .add(new BooleanOption("Profit Tracking",
                        "Show \"+ X Coins\" under every win, using the live lowest BIN from the auction house. "
                                + "When the price API is unreachable a rough built-in estimate is shown with a ~.",
                        () -> cfg.profitTracking, v -> cfg.profitTracking = v))

                .add(new SectionHeader("Audio"))
                .add(new FloatOption("Master Volume",
                        "Scales every sound this mod plays. Set to 0.00 to mute the mod without disabling it.",
                        0.0f, 1.0f, 0.05f, "",
                        () -> cfg.masterVolume, v -> cfg.masterVolume = v))
                .add(new ChoiceOption("Win Sound",
                        "The sound a jackpot plays: the bundled slot machine clip, or the vanilla level-up sting. "
                                + "The clip falls back to vanilla automatically if the OGG is missing from the jar.",
                        List.of("Custom Clip", "Vanilla Level-Up"),
                        () -> cfg.customWinSound ? 0 : 1, v -> cfg.customWinSound = v == 0))
                .add(new FloatOption("Custom Volume",
                        "Volume of the custom win sound, before the master volume.",
                        0.0f, 1.0f, 0.05f, "",
                        () -> cfg.customWinSoundVolume, v -> cfg.customWinSoundVolume = v))
                .add(new FloatOption("Custom Pitch",
                        "Pitch of the custom win sound. Minecraft clamps this to 0.50 - 2.00; each 0.06 is about a semitone.",
                        0.5f, 2.0f, 0.05f, "",
                        () -> cfg.customWinSoundPitch, v -> cfg.customWinSoundPitch = v))
                .add(new BooleanOption("Custom Sound On Anvil",
                        "Also play the custom clip for the Judgement Core reveal, not just the slot jackpots.",
                        () -> cfg.customWinSoundForAnvil, v -> cfg.customWinSoundForAnvil = v))
                .add(new ActionOption("Test Sound",
                        "Play the custom win sound at your current volume and pitch. Says so in chat if the OGG "
                                + "is missing and the vanilla fallback was used instead.",
                        "Play", ConfigManager::testWinSound))

                .add(new SectionHeader("HUD Position"))
                .add(new FloatOption("HUD X",
                        "Horizontal position of the tracker HUD, as a fraction of the screen width.",
                        0.0f, 1.0f, 0.01f, "",
                        () -> cfg.hudX, v -> cfg.hudX = v))
                .add(new FloatOption("HUD Y",
                        "Vertical position of the tracker HUD, as a fraction of the screen height.",
                        0.0f, 1.0f, 0.01f, "",
                        () -> cfg.hudY, v -> cfg.hudY = v));
    }

    private static ConfigCategory dev() {
        ModConfig cfg = ModConfig.get();
        return new ConfigCategory("Dev",
                "Testing, debugging and timing overrides.")
                .add(new SectionHeader("Testing"))
                .add(new ChoiceOption("Test Drop",
                        "Pick a drop, then press Trigger below to run its machine as if it had just dropped. "
                                + "The same thing is available as /gambling test <drop>.",
                        DROP_LABELS,
                        () -> testDropIndex, v -> testDropIndex = v))
                .add(new ActionOption("Trigger",
                        "Force the animation for the drop selected above.",
                        "Run", () -> preview(Drop.values()[Math.floorMod(testDropIndex, Drop.values().length)])))
                .add(new ActionOption("Simulate Slayer Kill",
                        "Run whatever the slayer tracker currently believes you are fighting, as a losing gamble.",
                        "Run", () -> {
                            chat(ChatFormatting.YELLOW, "simulating a slayer kill - tracker says: " + SlayerTracker.describe());
                            SlayerTracker.simulateKill();
                        }))

                .add(new SectionHeader("Debugging"))
                .add(new BooleanOption("Chat Regex Logger",
                        "Write every incoming chat line to latest.log, raw and stripped, plus how the drop matcher "
                                + "classified it. Use this to find out why a trigger was missed. Very noisy.",
                        () -> cfg.debugLogging, v -> cfg.debugLogging = v))
                .add(new ActionOption("Tracker Status",
                        "Print what the slayer and sea creature trackers currently see.",
                        "Show", () -> {
                            chat(ChatFormatting.GRAY, "slayer: " + SlayerTracker.describe());
                            chat(ChatFormatting.GRAY, "sea creatures: " + SeaCreatureTracker.describe());
                        }))
                .add(new BooleanOption("Require Tier Match",
                        "Only animate when the boss and tier are positively confirmed. Turn this OFF if Hypixel "
                                + "changed their wording and detection has gone blind.",
                        () -> cfg.strictDropTierCheck, v -> cfg.strictDropTierCheck = v))
                .add(new BooleanOption("Nametag Fallback",
                        "Read nearby boss nametags when the sidebar scoreboard yields nothing.",
                        () -> cfg.nametagFallback, v -> cfg.nametagFallback = v))
                .add(new FloatOption("Sighting Grace",
                        "Seconds a confirmed slayer sighting stays valid after the boss dies.",
                        5.0f, 120.0f, 5.0f, "s",
                        () -> cfg.sightingGraceSeconds, v -> cfg.sightingGraceSeconds = v))

                .add(new SectionHeader("Animation Speed"))
                .add(speed("Anvil Speed", () -> cfg.anvilSpeed, v -> cfg.anvilSpeed = v))
                .add(speed("Slots Speed", () -> cfg.slotSpeed, v -> cfg.slotSpeed = v))
                .add(speed("Heartbeat Speed", () -> cfg.heartSpeed, v -> cfg.heartSpeed = v))
                .add(speed("High Roller Speed", () -> cfg.highRollerSpeed, v -> cfg.highRollerSpeed = v))
                .add(speed("Wiki Tiki Speed", () -> cfg.wikiTikiSpeed, v -> cfg.wikiTikiSpeed = v))
                .add(speed("Jawbus Speed", () -> cfg.jawbusSpeed, v -> cfg.jawbusSpeed = v))
                .add(speed("Thunder Speed", () -> cfg.thunderSpeed, v -> cfg.thunderSpeed = v))
                .add(speed("Ragnarok Speed", () -> cfg.ragnarokSpeed, v -> cfg.ragnarokSpeed = v))

                .add(new SectionHeader("Reset"))
                .add(new ActionOption("Reset All Settings",
                        "Restore every setting in every category to its default value. This takes effect "
                                + "immediately and cannot be undone.",
                        "Reset", () -> {
                            ModConfig.resetToDefaults();
                            chat(ChatFormatting.YELLOW, "All settings restored to defaults.");
                            Minecraft client = Minecraft.getInstance();
                            client.execute(() -> client.setScreen(new ConfigScreen()));
                        }));
    }

    private static FloatOption speed(String name, java.util.function.Supplier<Float> getter,
                                     java.util.function.Consumer<Float> setter) {
        return new FloatOption(name,
                "Timeline multiplier. 2.00 runs at double speed, 0.50 at half; every phase scales together.",
                0.25f, 3.0f, 0.05f, "x", getter, setter);
    }

    public static void preview(Drop drop) {
        open(drop.boss(), GambleSession.win(drop));
    }

    public static void previewLoss(Boss boss) {
        open(boss, GambleSession.loss(boss));
    }

    public static boolean open(Boss boss, GambleSession session) {
        Function<GambleSession, Screen> factory = GambleTrigger.screenFor(boss);
        if (factory == null) {
            chat(ChatFormatting.RED, boss.displayName() + " has no animation.");
            return false;
        }
        if (AnimationQueue.isBusy()) {
            chat(ChatFormatting.RED, "An animation is already running.");
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            client.setScreen(null);
            AnimationQueue.play(factory, session);
        });
        return true;
    }

    private static void testWinSound() {
        boolean available = SoundUtil.isAvailable(ModSounds.SLOT_MACHINE_WIN);
        SoundUtil.playWinSound(true, 1.0f);
        if (available) {
            chat(ChatFormatting.GREEN, "Playing custom win sound.");
        } else {
            chat(ChatFormatting.RED, "slotmachine.ogg is missing from the jar - played the vanilla fallback instead. "
                    + "See sounds-src/HOW-TO-CONVERT.txt.");
        }
    }

    public static void chat(ChatFormatting colour, String message) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(
                        Component.literal("[GA] ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(Component.literal(message).withStyle(colour)));
            }
        });
    }

    public static @Nullable Drop dropByName(String name) {
        return Drop.byName(name);
    }
}
