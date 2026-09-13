# Gambling Addict

A client-side Fabric mod for Hypixel SkyBlock.

Every boss kill is a pull of the lever. The animation plays on the **kill**,
not on the drop - so it can lose, and it almost always does. If a real rare-drop
line arrives while the machine is still running, the result flips to a win and
the reveal shows the item that actually dropped.

## Machines

### Slayers

| Boss | Machine | Headline win | Loss |
|---|---|---|---|
| Voidgloom Seraph | Anvil test | The Core survives three blows → **Judgement Core** | The Core shatters |
| Tarantula Broodfather | 3-reel slot machine | Three Eyes → **Primordial Eye** | Two Eyes and a miss |
| Revenant Horror | Pulsing heart | The heart holds → **Warden Heart** | The heart shatters (revealing any lesser drop) |
| Sven Packmaster | Single-reel high roller | **Overflux Capacitor** / **Celeste Dye** | The reel stops on nothing special |

### Sea creatures

| Boss | Machine | Headline win | Loss |
|---|---|---|---|
| Wiki Tiki | Tiki mask slots | Three masks → **Tiki Mask** / **Aquamarine Dye** | Two out of three |
| Lord Jawbus | Exploding chests | **Radioactive Vial** / **Carmine Dye** float out of the crater | Just fragments |
| Thunder | Lightning-struck book | Three red strikes shatter it → **Carmine Dye** (two charge it into **Flash I**) | One strike burns it to ash |
| Ragnarok | Nether-red slots | **Burnt Texts** (or any other Ragnarok drop) | Two out of three |

Sea creatures have no kill message, so their machines watch the boss nametag
instead: the gamble runs when the health reads zero or the tag vanishes while you
are standing next to it.

A win shows the item's current lowest BIN under the reveal, e.g.
`+ 114.7M Coins`. When the price API is unreachable a rough built-in estimate is
shown with a `~`. Won items are drawn with the enchantment glint.

Loot Share is understood: a drop that arrives beside Hypixel's
`LOOT SHARE You received loot for assisting …` notice (or prefixed with
`[Loot Share]`) still animates, and the reveal says so.

## Requirements

- Minecraft **26.1+**
- Fabric Loader 0.19+
- Fabric API
- Java 25

## Commands

| Command | |
|---|---|
| `/gamblingaddict` | Open the options screen |
| `/gambling test <drop>` | Force a machine with that drop, e.g. `/gambling test carmine_dye` |
| `/gambling test loss <boss>` | Force a losing gamble, e.g. `/gambling test loss thunder` |
| `/gamblingaddict testkill [boss]` | Simulate a boss kill through the real trigger path |
| `/gamblingaddict status` | What the slayer and sea creature trackers currently see |
| `/gamblingaddict match <text>` | Test whether a drop line would trigger, and on which machine |
| `/gamblingaddict debug` | Toggle the chat regex logger + dump the sidebar |
| `/gamblingaddict reload` | Re-read the config file |

`/ga` works as an alias. Several other SkyBlock mods bind `/ga` to Garden features,
so if it is taken on your setup, use the full name.

## Options

`/gamblingaddict` opens an in-game options screen with search, in four tabs:

- **Fishing** — Wiki Tiki / Lord Jawbus / Thunder / Ragnarok toggles and volumes,
  sea creature kill detection, previews, the Mob Cap Counter (with its limit) and
  the Ability Cooldown HUD
- **Combat** — Voidgloom / Tarantula / Revenant / Sven toggles and their audio,
  top-tier only, previews, the Blaze Attunement HUD and the Ragnarock Axe
  notification (chat / title / action bar)
- **Misc** — master enable, gamble every kill, Loot Share detection, click to
  skip, mute game sounds, background opacity, banner times, Profit Tracking,
  master volume, win sound choice + custom clip volume/pitch, HUD X/Y
- **Dev** — a drop picker with a Trigger button (same as `/gambling test`),
  simulate slayer kill, the Chat Regex Logger, tracker status, detection
  fallbacks, per-machine animation speed sliders, reset

Settings live in `config/gamblingaddict.json`.

## Trackers

The HUD helpers draw a small panel at the position set in **Misc → HUD Position**:

- **Mob Cap Counter** - sea creatures currently loaded around you against the limit
- **Ability Cooldown** - a countdown whenever Hypixel reports an ability on
  cooldown, plus the Ragnarock Axe buff timer
- **Blaze Attunement** - the Inferno Demonlord's current ASHEN / SPIRIT / AURIC /
  CRYSTAL attunement, read from the boss nametags

The Ragnarock Axe line is matched loosely (`gain … +X Strength`), since Hypixel
does not document the exact wording. If it never fires for you, turn on the Chat
Regex Logger, channel the axe once, and check `latest.log` for the real line.

## Custom win sound

Minecraft only decodes OGG Vorbis. To use your own jackpot clip, drop an
`slotmachine.ogg` into `assets/gamblingaddict/sounds/` before building:

```bash
ffmpeg -i yourclip.mp3 -c:a libvorbis -q:a 5 -ar 44100 src/main/resources/assets/gamblingaddict/sounds/slotmachine.ogg
```

If the file is absent the mod falls back to the vanilla level-up sting.
**Misc → Test Sound** tells you which one you just heard.

## Building

```bash
gradle build
```

Build output goes to `%TEMP%/gamblingaddict-build` rather than `build/`. See
[DEVELOPMENT.md](DEVELOPMENT.md) for the 26.1 toolchain setup, which differs
substantially from the 1.21 line.

## Notes

- Client-side only. No packets are sent and no server state is touched: it reads
  chat, reads the sidebar scoreboard and nearby nametags, and draws a GUI. Check
  Hypixel's current rules before using it there.
- A vanilla `Screen` releases the mouse while open. Every animation is short and
  hands control straight back; ESC or a click skips at any point.
- If a kill lands while you have an inventory or chat open, the animation waits for
  the screen to close rather than interrupting you.

## License

MIT - see [LICENSE](LICENSE).
