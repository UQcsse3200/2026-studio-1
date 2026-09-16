# Map System Rework: Phases

Making the map system something other teams can build on without editing map team code.

Each phase is one commit, lands with the test suite green, and is listed here with what
changed and what it looks like before and after.

| Phase | Change | Status |
| ----- | ------ | ------ |
| 1 | Tile properties | Done |
| 2 | Markers | Done |
| 3 | Spawn registry | Done |
| 4 | Validator, all-maps test, template map | Done |
| 5 | `LevelView` read API | Done |
| 6 | Cleanup: dead code and API traps | Done |
| 7 | Sub-levels and level order from data | Done |

---

## Phase 1: Tile properties

**Problem.** A tile could only carry a type and a texture. Anything else, such as how much
damage a hazard does or whether a wall is secret, meant changing the `TileDefinition` record,
the loader, and every place a `TileDefinition` is built. Two features needing per-tile data in
the same sprint would edit the same three files and conflict.

**Change.** A legend entry may now carry any extra keys. They are read as text into a
`properties` map on `TileDefinition` and never interpreted by the loader. Features read them
back with typed accessors that fall back instead of throwing.

### Before

A legend entry could say two things only, and extra keys were discarded by the loader:

```json
"~": { "type": "HAZARD", "texture": "images/level1/river-styx-512px.png" }
```

```java
public record TileDefinition(TileType type, String texture) {}
```

### After

A legend entry carries whatever the author needs, and the loader keeps it:

```json
"~": { "type": "HAZARD", "texture": "images/level1/river-styx-512px.png", "damage": "15" },
"W": { "type": "WALL",   "texture": "images/level1/wall-limestone-512px.png", "hidden": "true" },
"o": { "type": "DECORATIVE", "texture": "images/level1/decor-oil-lamp-512px.png", "light": "4.5" }
```

```java
public record TileDefinition(TileType type, String texture, Map<String, String> properties) {
  boolean flag(String key);
  int     getInt(String key, int fallback);
  float   getFloat(String key, float fallback);
}
```

### First consumer: per-tile hazard damage

The phase also wires the first consumer, so the mechanism is proven end to end rather than
sitting unused.

Before, every hazard in the game did the same damage, set by one constant:

```java
private static final int HAZARD_DAMAGE = 10;
...
stats.addHealth(-HAZARD_DAMAGE);
```

After, each hazard tile carries its own value, and the constant is only the fallback:

```java
// spawnHazardCollisions, once per hazard tile
Entity collider = ObstacleFactory.createHazardTile(tileSize, tileSize)
    .addComponent(new HazardDamageComponent(def.getInt("damage", HAZARD_DAMAGE)));

// on contact
HazardDamageComponent hazard = other.getComponent(HazardDamageComponent.class);
int damage = hazard == null ? HAZARD_DAMAGE : hazard.getDamage();
stats.addHealth(-damage);
```

A map author can now make the Styx and a firepit hurt differently by editing the legend.

No shipped map sets a `damage` value yet, so all hazards still do 10 and gameplay is
unchanged. Balancing them is a map content decision, not part of this phase.

### Still waiting on a consumer

| Property | Consumer | Status |
| -------- | -------- | ------ |
| `damage` | Hazard collision | Wired in this phase |
| `hidden` | Secret Loot Rooms (Task 4) | Feature not built yet |
| `light`  | Dungeon Lighting (Task 5) | Feature not built yet |

### Notes

- `type` and `texture` are not copied into the property bag.
- `properties` is never null and never modifiable, so callers do not null-check it.
- `flag()` returns false for an absent property, so it is safe to call on any tile.
- `getInt()` and `getFloat()` return the fallback when the value is absent or not a number,
  so a typo in a map file cannot crash a level.
- The two-argument `TileDefinition(type, texture)` constructor is kept. No existing call
  site changed.

### Files

| File | Change |
| ---- | ------ |
| `terrain/map/TileDefinition.java` | Third record component plus typed accessors |
| `terrain/map/JsonMapLoader.java` | `parseTileProperties`, collects extra legend keys |
| `components/HazardDamageComponent.java` | New. Carries one hazard tile's damage |
| `areas/LevelGameArea.java` | Attaches the component per tile, reads it on contact |
| `terrain/map/TileDefinitionTest.java` | 5 tests: defaults, values, fallbacks, immutability, nulls |
| `terrain/map/JsonMapLoaderTest.java` | 1 test: extra keys land in the bag, `type`/`texture` do not |
| `components/HazardDamageComponentTest.java` | New. 4 tests: value, clamping, legend-driven, fallback |

---

## Phase 2: Markers

**Problem.** The entities layer understood three kinds: `PLAYER`, `ENEMY`, `LOOT`. Anything else
an NPC, a pet, a checkpoint, a light source, a secret room trigger meant adding a case to
`placeEntity` and a list to `MapSpawns`. Every team that wanted to place something had to edit
map team code, and each addition made the next one no easier.

**Change.** A fourth kind, `MARKER`, stores anything by name with its own properties. The loader
parses it, checks it is inside the map, and serves it on request. It never interprets what a
marker means.

### Before

Placing an NPC was not supported. The one NPC in the game is positioned from a constant in
`LevelGameArea`:

```java
private static final GridPoint2 TRAVELER_NPC_SPAWN = new GridPoint2(4, 13);
```

Adding support would have meant another case here:

```java
switch (type) {
  case "PLAYER" -> placePlayerSpawn(x, y, spawns);
  case "ENEMY"  -> spawns.addEnemy(...);
  case "LOOT"   -> spawns.addLoot(...);
  // and a new case, and a new list on MapSpawns, per team
}
```

### After

```json
"entityLegend": {
  "N": { "type": "MARKER", "kind": "npc",        "id": "traveler" },
  "T": { "type": "MARKER", "kind": "light",      "radius": "6" },
  "C": { "type": "MARKER", "kind": "checkpoint", "id": "quarry" }
},
"layers": {
  "entities": [
    "   T      ",
    " N      C "
  ]
}
```

```java
for (Marker marker : mapData.getSpawns().getMarkers("npc")) {
    spawnNpc(marker.id(), marker.position());
}

float radius = light.getFloat("radius", 4f);
```

### Notes

- `kind` groups markers and is required; a marker without one is skipped with a warning.
- `id` says which one it is within that kind, and is optional.
- Kinds are matched ignoring case, so `"CheckPoint"` and `"checkpoint"` are the same group.
- `type`, `kind` and `id` stay out of the property bag; everything else on the entry goes in.
- Markers outside the map bounds warn at load, like the other spawn kinds.
- `PLAYER`, `ENEMY` and `LOOT` are unchanged and keep their own accessors, because the level
  area spawns those itself.

### Files

| File | Change |
| ---- | ------ |
| `terrain/map/Marker.java` | New. Record of kind, id, position, properties |
| `terrain/map/MapSpawns.java` | `addMarker`, `getMarkers(kind)`, `getAllMarkers`, `getMarkerKinds` |
| `terrain/map/JsonMapLoader.java` | `MARKER` case, `placeMarker`, bounds validation |
| `terrain/map/MarkerTest.java` | New. 5 tests: fields, kind normalising, properties, immutability |
| `terrain/map/MapSpawnsTest.java` | 3 tests: grouping, case-insensitive lookup, empty cases |
| `terrain/map/JsonMapLoaderTest.java` | 3 tests: parsing, missing kind, no interference with other kinds |

---

## Phase 3: Spawn registry

**Problem.** Opening the map format was not enough on its own. After a map said "skeleton at
(7, 33)", something still had to build a skeleton, and that something was a switch statement in
`LevelGameArea`. Every new enemy type was a pull request against a map team file, and there was
no path at all for a pet or an NPC.

**Change.** A registry maps a spawn name to a factory. Teams register their own factories; the
level area only ever looks names up. It now holds no list of what the game can spawn.

### Before

```java
// LevelGameArea, owned by the map team
private Entity createEnemy(String type) {
  return switch (type.toLowerCase()) {
    case "ghost"     -> NPCFactory.createGhost(player);
    case "skeleton"  -> NPCFactory.createSkeleton(player);
    case "cyclops"   -> NPCFactory.createCyclops(player);
    case "minotaur"  -> NPCFactory.createMinotaur(player);
    // every new type is another line in this file
    default -> null;
  };
}
```

### After

The level area no longer knows what the game can spawn. It asks the registry:

```java
// LevelGameArea, owned by the map team
private Entity createEnemy(String type) {
  return EntitySpawnRegistry.create(type, player);
}
```

### Where the names live now: DefaultEntitySpawns

Removing the switch does not by itself move the list off the map team. The names have to be
registered somewhere, and on day one that somewhere is `DefaultEntitySpawns`, which holds
exactly the roster the switch used to hold:

```java
// entities/spawn/DefaultEntitySpawns.java, called once from LevelGameArea.create()
EntitySpawnRegistry.registerIfAbsent("skeleton", NPCFactory::createSkeleton);
EntitySpawnRegistry.registerIfAbsent("centaur",  NPCFactory::createCentaur);
EntitySpawnRegistry.registerIfAbsent("npc:traveler", NPCFactory::createTravelerNPC);
// ...11 more
```

So today the list is still in a map team file. What changed is that it is no longer the
**only** place a name can be registered, and it no longer sits inside the spawning code.

`DefaultEntitySpawns` exists to keep every shipped map working on the day the switch is
removed, and it is meant to shrink to nothing. Each team moves its own lines into its own
start-up code:

```java
// enemy team, in their own file
EntitySpawnRegistry.register("skeleton", NPCFactory::createSkeleton);
EntitySpawnRegistry.register("centaur",  NPCFactory::createCentaur);

// pet team, in theirs
EntitySpawnRegistry.register("pet:companion", PetFactory::createCompanion);
```

and deletes the matching line from `DefaultEntitySpawns`. The order does not matter and
nothing breaks halfway through, because the defaults use `registerIfAbsent`: a name a team
has already claimed is left alone. When the last line is gone, so is the class.

**Status: no team has migrated yet.** All 12 names still live in `DefaultEntitySpawns`.

### Notes

- Names match ignoring case and surrounding space.
- An unregistered name is logged with the list of known names, then skipped. A map may place an
  entity before the code to build it exists, and the level still loads.
- Teams use `register`. Only `DefaultEntitySpawns` uses `registerIfAbsent`, so it can never
  override a team that has claimed a name for itself.
- The registry is static, matching how `NPCFactory` and `ServiceLocator` already work in this
  codebase. `clear()` and `DefaultEntitySpawns.reset()` give tests a known starting state.

### Files

| File | Change |
| ---- | ------ |
| `entities/spawn/EntitySpawnRegistry.java` | New. Name to factory, with lookup and diagnostics |
| `entities/spawn/DefaultEntitySpawns.java` | New. The shipped roster, transitional |
| `areas/LevelGameArea.java` | Switch replaced by one registry lookup |
| `entities/spawn/EntitySpawnRegistryTest.java` | New. 10 tests: lookup, casing, unknown names, precedence |

---

## Phase 4: Validator, all-maps test, template map

**Problem.** A wrong map still loaded. Misspell a layer name and nothing is solid, so the player
falls out of the world with no error anywhere. Point a doorway at a file that does not exist and
the level loads fine, then crashes when somebody walks into it. Every one of these was found by
a person during a playtest.

**Change.** `MapValidator` reports what is wrong with a loaded map, and one test runs it over
every map in `assets/maps`. CI already runs the unit suite on every push, so a broken map now
fails a pull request.

### Before

Nothing checked a map beyond the loader's own parse errors, and no test opened the shipped maps
except to assert facts about one particular level.

```java
// A map with "terrian" instead of "terrain" loads without complaint.
// getCollisionLayer() returns null, spawnCollisions() returns early,
// and the player falls through the floor.
```

### After

```java
List<Problem> problems = MapValidator.validate(map, path -> Gdx.files.internal(path).exists());
List<Problem> errors = MapValidator.errorsIn(problems);
```

```
ERROR:   Map 'Typo' has no 'collision' or 'terrain' layer, so nothing in it is solid.
         Check the layer names for a typo.
ERROR:   Map 'Level 2' transition 'summit-exit' leads to 'maps/level4.json', which does not exist.
WARNING: Map 'Level 1' player spawn (3, 9) has no ground beneath it.
```

### What counts as an error

Errors fail the build. Warnings are logged for a human to judge. The split is deliberate: a
validator that blocks on judgement calls gets switched off.

| Severity | Checks |
| -------- | ------ |
| ERROR | No collision layer. Player, enemy, loot or marker spawn outside the map. Doorway outside the map, with no destination, or pointing at a file that does not exist. A legend texture that is missing. |
| WARNING | No player spawn at all. Player spawn inside a solid tile, or with no ground beneath it. A doorway that names no arrival tile. |

All four shipped maps pass with no errors and no warnings.

### Also in this phase

- `template.json`, a small valid map with all four layers, both legends, a marker, a hazard with
  a `damage` property and a transition. Its `docs` block explains every key and is ignored by the
  loader. Copy it to start a map rather than starting from a 1,200 line level.
- `demo.json` and `room2.json` deleted. Neither was referenced by the game, only by tests.

### Notes

- The all-maps test is a plain loop, not a parameterised test, because `junit-jupiter-params` is
  not on the test classpath and adding it means editing the shared `build.gradle`. If that
  dependency is ever added, this test is worth converting so each map is named in the report.
- Checks needing the filesystem only run when `validate` is given a way to test for a file, so
  the validator stays usable in a pure unit test.

### Files

| File | Change |
| ---- | ------ |
| `terrain/map/MapValidator.java` | New. Checks, severities, `errorsIn` helper |
| `assets/maps/template.json` | New. Copyable starting point, with the schema documented inline |
| `assets/maps/demo.json`, `room2.json` | Deleted |
| `terrain/map/ShippedMapsTest.java` | New. Loads and validates every map in `assets/maps` |
| `terrain/map/MapValidatorTest.java` | New. 11 tests, one per check |
| `terrain/map/JsonMapLoaderTest.java` | Removed the test for the deleted maps |
| `components/room/RoomTransitionComponentTest.java` | Repointed two strings off `room2.json` |

---

## Phase 5: LevelView read API

**Problem.** The only way into a level was `getMapData()`, which hands back the whole
`LevelMapData`. Other teams then read layers by name and index tiles themselves, so whatever they
happened to write became the map system's public API by accident, and any change to how layers are
arranged broke somebody. `LootSpawnFinder` already did exactly this.

**Change.** A `LevelView` interface answers the questions other systems actually have, without
exposing how the map is structured. `getMapData()` still works and is deprecated.

### Before

```java
// LootSpawnFinder, owned by the loot team, reaching into map internals
for (int y = 1; y < map.getHeight(); y++) {
  for (int x = 0; x < map.getWidth(); x++) {
    TileType tile = map.getTileType(x, y);
    TileType below = map.getTileType(x, y - 1);
    if (isOpen(tile) && isGround(below)) {
      spots.add(new SpawnPoint("loot", x, y));
    }
  }
}

// with its own private copy of the rules
private static boolean isGround(TileType tile) {
  return tile == TileType.FLOOR || tile == TileType.WALL || tile == TileType.PLATFORM;
}
```

### After

```java
for (GridPoint2 tile : level.groundTiles()) {
  spots.add(new SpawnPoint("loot", tile.x, tile.y));
}
```

The rule for what counts as ground now lives in one place, so a new tile type that can be stood
on is correct everywhere at once rather than in each team's private copy.

### The interface

```java
public interface LevelView {
  String name();  int width();  int height();  float tileSize();

  boolean        inBounds(int x, int y);
  TileDefinition tileAt(int x, int y);        // includes tile properties
  TileType       tileTypeAt(int x, int y);
  boolean        isSolid(int x, int y);
  boolean        isSupporting(int x, int y);  // floors, walls, one-way platforms
  boolean        isWalkable(int x, int y);
  boolean        isHazard(int x, int y);

  GridPoint2       playerSpawn();
  List<GridPoint2> groundTiles();
  List<Marker>     markers(String kind);
  List<RoomTransition> transitions();
}
```

### Notes

- Out of bounds reads return null or false rather than throwing, so a system scanning the tiles
  around a position does not have to bounds check first.
- Methods are named for behaviour, not tile names. `isSupporting` is true for a one-way platform
  and false for a ladder, which is the distinction a caller actually cares about.
- `LevelGameArea.getLevel()` is the way in. `getMapData()` is kept and marked deprecated so
  nothing breaks while callers migrate.
- `LootSpawnFinder.findGroundSpots` keeps its `LevelMapData` overload, so the loot team's own
  tests were unchanged apart from one cast where `null` became ambiguous between the two.

### Files

| File | Change |
| ---- | ------ |
| `terrain/map/LevelView.java` | New. The supported read API |
| `terrain/map/MapDataLevelView.java` | New. The implementation over `LevelMapData` |
| `areas/LevelGameArea.java` | `getLevel()` added, `getMapData()` deprecated |
| `components/loot/LootSpawnFinder.java` | Reads through `LevelView`, private tile rules deleted |
| `terrain/map/MapDataLevelViewTest.java` | New. 8 tests: measurements, classification, ground, markers, bounds |
| `components/loot/LootSpawnFinderTest.java` | One cast for the new overload |

---

## Phase 6: Cleanup

**Problem.** Three things in the terrain package were not wrong so much as waiting to catch
someone out, and one large piece of it was dead.

**Change.** Delete the dead path, and make the two misleading methods say what they mean.

### The dead forest demo

`ForestGameArea` was referenced by nothing. That made roughly 150 of `TerrainFactory`'s 268 lines
unreachable: `createTerrain`, the `TerrainType` enum, `createForestDemoTerrain`,
`createForestDemoTiles`, `fillTiles`, `fillTilesAtRandom`, `fillFloor` and four constants. It was
also the first thing anyone opening that file read, so it taught the wrong lesson: that terrain
comes from a hardcoded factory method rather than a map file.

`TerrainFactory` is now 127 lines and does one thing.

### getTile read the wrong layer

```java
// before: layer 0, which became the decorative background layer when maps gained one
public TerrainTile getTile(int x, int y) {
  TiledMapTileLayer layer = (TiledMapTileLayer) tiledMap.getLayers().get(0);
```

```java
// after: name the layer you mean, and use LevelView for gameplay questions
public TerrainTile getTile(String layerName, int x, int y)
```

It had no callers, so this was a trap rather than a live bug. `getMapBounds(int layer)` took a raw
layer index for the same reason and is now `getMapBounds()`, reading the first tile layer, since
every layer of a map shares its dimensions.

### Mixed tile sizes are now visible

`resolveTilePixelSize` picks the smallest legend texture and draws the whole map at that size, so
one 16 pixel tile in a 512 pixel tileset silently shrinks everything. All current art is 512, so
nobody has hit it. A map mixing sizes now logs what it found and what it did.

### Also

Level 2's `authoring` blueprint moved to `docs/design/level2/level2-blueprint.json`, beside level
1's, leaving map files as runtime data only. The note at the top of it records that its
coordinates use the original top-left origin and need converting.

### Files

| File | Change |
| ---- | ------ |
| `areas/ForestGameArea.java` | Deleted |
| `terrain/TerrainFactory.java` | Demo path removed, 268 lines to 127, warns on mixed tile sizes |
| `terrain/TerrainComponent.java` | `getTile(layerName, x, y)`, `getMapBounds()` |
| `assets/maps/level2.json` | `authoring` block moved out |
| `docs/design/level2/level2-blueprint.json` | New. Level 2's design blueprint |

---

## Phase 7: Sub-levels and level order from data

**Problem.** Level 1 is one map holding a dungeon below and the Nether above. The map file already
described that split in a `subLevels` block, with bounds, doors and titles. No code read it. The
same facts were spelled out again in Java: a camera boundary constant, and a chain of if
statements matching map paths to title strings.

**Change.** Parse `subLevels`, and read the camera boundary and the crossing titles from it.

### The data was also wrong

Worth recording, because it is what happens when data is never read: `subLevels` said each half of
level 1 was 16 tiles tall. The real split is at row 32 of a 64 tile map, because the Java constant
was 16 **world** units and a tile is 0.5. The block had been drifting unnoticed since it was
written. Corrected as part of this phase.

Level 2 had no `subLevels` block at all, while the code still gave it BASE and SKIES titles from
the same constant. It now declares the split it always had.

### Before

```java
private static final float SUB_LEVEL_BOUNDARY = 16f;
...
boolean inNether = player.getCenterPosition().y >= SUB_LEVEL_BOUNDARY;
float subLevelBottom = inNether ? SUB_LEVEL_BOUNDARY : 0f;

static String subLevelTitle(String mapPath, boolean upperSection) {
  if (FIRST_ROOM_MAP.equals(mapPath))  return upperSection ? "NETHER" : "DUNGEON";
  if (SECOND_ROOM_MAP.equals(mapPath)) return upperSection ? "SKIES"  : "BASE";
  return null;
}
```

### After

```java
int playerRow = (int) Math.floor(player.getCenterPosition().y / level.tileSize());
SubLevel section = level.subLevelAt(playerRow);

float subLevelBottom = section.bounds().bottom() * tileSize;
float subLevelHeight = section.bounds().height() * tileSize;

if (section.title() != null) {
  player.getEvents().trigger("subLevelEntered", section.title());
}
```

```json
"subLevels": [
  { "id": "dungeon", "title": "DUNGEON", "bounds": { "x": 0, "y": 0,  "width": 56, "height": 32 } },
  { "id": "nether",  "title": "NETHER",  "bounds": { "x": 0, "y": 32, "width": 56, "height": 32 } }
]
```

A map with no `subLevels` is simply one place, and the camera uses the whole map. Adding a third
section to a level is now a map file edit.

### Still hardcoded, deliberately

- `FIRST_ROOM_MAP`. The game has to start at a named map, and a constant is the honest way to say
  which.
- The lift endpoints in `SubLevelTravelComponent`. The `door` tiles in the JSON do not agree with
  the world coordinates the component uses: the dungeon door is tile (26, 22), which is world
  y = 11.0, while the component uses 11.9. The component's values are the ones that have been
  playtested, and `isNear` triggers within one world unit, so switching to the map's values would
  move the lift by most of its own trigger radius. Reconciling them needs somebody to play it,
  which is map content work, not a refactor.

`SECOND_ROOM_MAP` is gone, and the lift prompt now appears on any map that declares sub-levels
rather than on one named file.

### Files

| File | Change |
| ---- | ------ |
| `terrain/map/SubLevel.java` | New. Record of id, title, bounds, door, destination |
| `terrain/map/JsonMapLoader.java` | `parseSubLevels` |
| `terrain/map/LevelMapData.java` | `getSubLevels()`, `getSubLevelAt(tileY)` |
| `terrain/map/LevelView.java`, `MapDataLevelView.java` | `subLevels()`, `subLevelAt(tileY)` |
| `screens/MainGameScreen.java` | Camera bounds and titles from data; `SUB_LEVEL_BOUNDARY` and `SECOND_ROOM_MAP` removed |
| `assets/maps/level1-greek.json` | Corrected bounds from 16 to 32 tiles |
| `assets/maps/level2.json` | Declares its BASE and SKIES split |
| `screens/MainGameScreenTest.java` | Rewritten against real maps rather than path strings |

