# Development notes

Technical findings that are not obvious from the source. The code itself is kept
lean; this file is where the reasoning lives.

## Minecraft 26.1 toolchain

Almost none of this matches a 1.21-era guide.

| | |
|---|---|
| **Yarn** | Does not exist for 26.x. Newest Yarn build is `1.21.11+build.6`. |
| **Intermediary** | Also stops at 1.21.11. fabric-meta reports `"intermediary": "0.0.0"` for 26.1 — the "none needed" sentinel. |
| **Obfuscation** | Gone. Mojang publishes no `client_mappings` for 26.1, and the client jar ships real names. |
| **Mappings config** | `loom.officialMojangMappings()` **fails**. The working recipe is `noIntermediateMappings()` plus the identity stub `net.fabricmc:intermediary:0.0.0:v2`. |
| **Mod dependencies** | `modImplementation` breaks (it chokes remapping fabric-api's sources jar). Plain `implementation` is correct — with identity mappings there is nothing to remap. |
| **Sources jar** | `withSourcesJar()` must be omitted; `remapSourcesJar` needs a "named" namespace the identity stub does not have. |
| **Java** | 26.1's manifest declares `javaVersion.majorVersion = 25`. |
| **Loom** | 1.17.20 fails on mappings; 1.18.0-alpha.x is required. |

### API renames from the 1.21 line

- `DrawContext` → `GuiGraphicsExtractor`; `render(...)` → `Renderable.extractRenderState(...)`
- `drawBorder` and `drawCenteredTextWithShadow` no longer exist — reimplemented in `RenderCompat`
- `Screen` constructor is `(Minecraft, Font, Component)`; `shouldPause()` → `isPauseScreen()`
- Input is records: `mouseClicked(MouseButtonEvent, boolean)`, `keyPressed(KeyEvent)`, `charTyped(CharacterEvent)`
- `ResourceLocation` → `Identifier` (Mojang adopted the Yarn name)
- `Text` → `Component`, `Formatting` → `ChatFormatting`, `ScoreboardEntry` → `PlayerScoreEntry`
- `ClientCommandManager` → `ClientCommands` (fabric-command-api-v2 3.x)

## Traps worth remembering

**`SoundEvents` constants are mixed types.** `ANVIL_LAND` and `PLAYER_LEVELUP` are
bare `SoundEvent`; `NOTE_BLOCK_PLING` and `UI_BUTTON_CLICK` are
`Holder.Reference<SoundEvent>`. `SoundUtil` carries an overload for each. The
`BLOCK_` / `ENTITY_` prefixes are also dropped.

**`PropertyMap` is immutable.** Its constructor runs `ImmutableMultimap.copyOf`, and
`GameProfile`'s two-arg constructor hands back the shared `PropertyMap.EMPTY`. So
`put()` throws `UnsupportedOperationException` on any instance. The multimap must be
populated *before* being wrapped, then passed to the three-arg `GameProfile`
constructor. `ResolvableProfile` is abstract — build via `createResolved(GameProfile)`.

**`ChatComponent.addMessage`** now demands signature/source/tag arguments. Use
`player.sendSystemMessage(Component)` for client-side output.

**Sentinel arithmetic overflows.** `nowTicks - Long.MIN_VALUE` wraps to a large
negative number. Any "never happened yet" sentinel must be compared explicitly
(`x != NEVER && ...`) rather than subtracted. This silently disabled the kill
debounce for several releases.

**Config defaults cannot be changed after release.** Gson keeps whatever is on disk,
so flipping a default has no effect for existing users. Rename the field instead —
that is why `gambleOnAnyTier` → `onlyGambleTopTier` and `requireTierMatch` →
`strictDropTierCheck`.

## Hypixel detection

Verified against real client logs.

Kill line, with leading spaces:

```
  SLAYER QUEST COMPLETE!
   Enderman Slayer LVL 8 - Next LVL in 575,078 XP!
   » Slay 22,000 Combat XP worth of Endermen.
```

Slayer **type** comes from those quest lines and is reliable. Slayer **tier** needs
the boss nametag (`☠ Voidgloom Seraph IV 12M❤`) or the sidebar, and is not — mods
like EntityCulling can keep the boss out of `entitiesForRendering()` entirely. Hence
type gates which machine opens, and an unknown tier never blocks a gamble.

The type line arrives one tick *after* the kill line, so a kill arms and fires once
the type is known rather than firing immediately.

Item name is **"Judgement Core"** (id `JUDGEMENT_CORE`) — with an "e".

Drop banners seen in the wild: `RARE DROP!`, `VERY RARE DROP!`, `CRAZY RARE DROP!`,
`INSANE DROP!`, `PRAY RNGESUS DROP!`, `PET DROP!`.

### Sea creatures

No kill line exists. Spawn lines do (all shown to the catcher):

```
The water bubbles and froths. A massive form emerges- you have disturbed the Wiki Tiki! You shall pay the price.
You have angered a legendary creature... Lord Jawbus has arrived.
You hear a massive rumble as Thunder emerges.
The sky darkens and the air thickens. The end times are upon us: Ragnarok is here.
```

Kills come from the nametag, `[Lv400] Thunder 35M/35M❤`, tracked per entity id in
`SeaCreatureTracker`: health reading zero, or the tag disappearing while the player is
within 48 blocks (and not mid-respawn), fires `GambleTrigger.onBossSlain`. A second
tag of the same boss standing within 8 blocks of the old one is treated as a
re-tag, not a death. Level changes clear everything.

### Loot Share

Hypixel's notice is its own line: `LOOT SHARE You received loot for assisting <player>!`
A banner within 40 ticks of it counts as shared. `[Loot Share] RARE DROP! …` as a
same-line prefix is accepted too.

### Shared drops

Bobbin' Scriptures comes from Wiki Tiki, Jawbus and Ragnarok; Carmine Dye from
Jawbus, Thunder and Ragnarok. `Drop.bossFor(context)` picks the machine, where the
context is the running gamble's boss, else the most recent sea creature sighting,
else the slayer tracker. `FLASH_BOOK` and `ATTRIBUTE_SHARD` match only with context,
because their chat names (`Enchanted Book`, `… Shard`) are generic.

### Enchantment glint in a GUI

There is no `RenderSystem` glint pass to write in 26.x. The item renderer chooses the
foil layer itself from `ItemStack#hasFoil()`, which consults
`DataComponents.ENCHANTMENT_GLINT_OVERRIDE` first. `DropIcons.foil(stack)` sets that
component to `true`; `GuiGraphicsExtractor#item` then draws the glint on its own,
including under `pose()` scaling.

## Data sources

- Item metadata and skull textures: `api.hypixel.net/v2/resources/skyblock/items`
- Dyes are **not** in that API — Brick Red, Celeste, Aquamarine and Carmine textures
  come from the NotEnoughUpdates item repo under `DYE_*`
- Sea creature drops from the wiki (Sept 2026): Wiki Tiki → Tiki Mask, Troubled
  Bubble, Bobbin' Scriptures, Aquamarine Dye; Lord Jawbus → Magma Lord Fragment,
  Radioactive Vial, Carmine Dye, attribute shards; Thunder → Flash I, Carmine Dye;
  Ragnarok → Brimstone Handle, Chain of the End Times, Burnt Texts, Carmine Dye
- `GRIZZLY_BAIT` is named "Grizzly Salmon" in the API; `THUNDER_SHARDS` is the id of
  "Thunder Fragment"
- Prices: `sky.coflnet.com/api/item/price/{id}/bin`. These are auction items, so
  Hypixel's bazaar endpoint does not carry them, and the full auctions endpoint is
  too heavy to page through client-side. `moulberry.codes` is returning 525.

## Build

Output goes to `%TEMP%/gamblingaddict-build`, not `build/`, because OneDrive holds
file locks that make Gradle's clean step fail intermittently. Override with
`-Pga.buildDir=...`.
