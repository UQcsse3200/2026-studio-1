package com.csse3200.game.areas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.areas.terrain.map.BackdropLayer;
import com.csse3200.game.areas.terrain.map.JsonMapLoader;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.MapLayerData;
import com.csse3200.game.areas.terrain.map.MapLoader;
import com.csse3200.game.areas.terrain.map.MapSpawns;
import com.csse3200.game.areas.terrain.map.Marker;
import com.csse3200.game.areas.terrain.map.RoomTransition;
import com.csse3200.game.areas.terrain.map.SpawnPoint;
import com.csse3200.game.areas.terrain.map.SubLevel;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.HazardDamageComponent;
import com.csse3200.game.components.enemies.EnemyType;
import com.csse3200.game.components.enemies.EnemyTypeComponent;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.lighting.LightColour;
import com.csse3200.game.components.lighting.LightComponent;
import com.csse3200.game.components.lighting.LightSpec;
import com.csse3200.game.components.lighting.LightingConfig;
import com.csse3200.game.components.loot.ConsumableType;
import com.csse3200.game.components.loot.LootId;
import com.csse3200.game.components.loot.LootRegistry;
import com.csse3200.game.components.npc.NameComponent;
import com.csse3200.game.components.npc.NameDisplay;
import com.csse3200.game.components.player.LadderComponent;
import com.csse3200.game.components.player.SubLevelTravelComponent;
import com.csse3200.game.components.room.RoomTransitionComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.EnemyFactory;
import com.csse3200.game.entities.factories.LootFactory;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.entities.spawn.EntitySpawnRegistry;
import com.csse3200.game.entities.spawn.NpcQuestRegistry;
import com.csse3200.game.entities.spawn.enemies.EnemyId;
import com.csse3200.game.entities.spawn.enemies.EnemyRegistry;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.perks.tortoises.TortoiseFactory;
import com.csse3200.game.perks.tortoises.TortoisePlan;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.GlowRenderComponent;
import com.csse3200.game.rendering.MapBackgroundRenderComponent;
import com.csse3200.game.rendering.ParallaxBackdropRenderComponent;
import com.csse3200.game.rendering.SheetAnimationRenderComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.LightService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Runs {@link LevelGameArea#create()} end to end on small hand-built maps.
 *
 * <p>Everything that needs a GPU or a physics world is swapped out: the factories that build
 * players, obstacles, loot and tortoises are mocked statically, and the render, physics and light
 * components the area builds itself are mocked at construction while {@code create()} runs. The
 * map, spawn, loot and NPC registries are the real ones, so these tests check what the area decides
 * to spawn, where, and what it skips.
 */
@ExtendWith(GameExtension.class)
class LevelGameAreaCreateTest {
  private static final float TS = 0.5f;
  private static final float EPS = 1e-4f;
  private static final String MAP_NAME = "Test Map";
  private static final String MAP_PATH = "maps/test.json";

  private static final TileDefinition WALL = new TileDefinition(TileType.WALL, "wall.png");
  private static final TileDefinition PLATFORM =
      new TileDefinition(TileType.PLATFORM, "platform.png");
  private static final TileDefinition ONE_WAY =
      new TileDefinition(TileType.ONE_WAY_PLATFORM, "one_way.png");
  private static final TileDefinition HAZARD =
      new TileDefinition(TileType.HAZARD, "lava.png", Map.of("damage", "15"));
  private static final TileDefinition GLOW =
      new TileDefinition(TileType.DECORATIVE, "lamp.png", Map.of("glow", "images/glow.png"));
  private static final TileDefinition ANIMATED =
      new TileDefinition(
          TileType.DECORATIVE,
          "brazier.png",
          Map.of("animation", "images/flame.png", "frames", "4", "fps", "6"));
  private static final TileDefinition LAMP =
      new TileDefinition(
          TileType.DECORATIVE,
          "oil_lamp.png",
          Map.of("light", "point", "lightColour", "oil_lamp", "lightRadius", "2"));

  private static final LightingConfig LIGHTING =
      new LightingConfig(
          new LightingConfig.Ambient(Color.GRAY, 0.5f),
          LightSpec.of(Color.WHITE, 3f, 0f),
          Map.of("dungeon", new LightingConfig.Ambient(Color.RED, 0.25f)));

  private ResourceService resources;
  private Music music;
  private LightService lightService;
  private TerrainComponent terrain;
  private TerrainFactory terrainFactory;
  private MapLoader mapLoader;

  /** Tile row for which the terrain reports no world position, or -1 for none. */
  private int hiddenRow = -1;

  private final List<AutoCloseable> staticMocks = new ArrayList<>();
  private MockedStatic<ObstacleFactory> obstacleFactory;
  private MockedStatic<LootFactory> lootFactory;
  private MockedStatic<TortoiseFactory> tortoiseFactory;
  private final List<Entity> obstacles = new ArrayList<>();
  private final List<Entity> createdPlayers = new ArrayList<>();
  private final Map<Class<?>, List<List<?>>> built = new HashMap<>();

  @BeforeEach
  void setUp() {
    resetRegistries();

    resources = mock(ResourceService.class);
    music = mock(Music.class);
    when(resources.loadForMillis(anyInt())).thenReturn(false, true);
    when(resources.getAsset(anyString(), eq(Music.class))).thenReturn(music);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerEntityService(new EntityService());
    lightService = mock(LightService.class);
    ServiceLocator.registerLightService(lightService);

    terrain = mock(TerrainComponent.class);
    when(terrain.getTileSize()).thenReturn(TS);
    when(terrain.tileToWorldPosition(any(GridPoint2.class)))
        .thenAnswer(
            inv -> {
              GridPoint2 tile = inv.getArgument(0);
              return worldOf(tile.x, tile.y);
            });
    when(terrain.tileToWorldPosition(anyInt(), anyInt()))
        .thenAnswer(inv -> worldOf(inv.getArgument(0), inv.getArgument(1)));
    terrainFactory = mock(TerrainFactory.class);
    when(terrainFactory.createTerrainFromMap(any())).thenReturn(terrain);
    mapLoader = mock(MapLoader.class);

    staticMocks.add(mockStatic(EnemyFactory.class));
    staticMocks.add(mockStatic(NPCFactory.class));
    MockedStatic<PlayerFactory> players = mockStatic(PlayerFactory.class);
    players
        .when(() -> PlayerFactory.createPlayer(any(LevelMapData.class)))
        .thenAnswer(inv -> newPlayer());
    staticMocks.add(players);
    obstacleFactory =
        mockStatic(
            ObstacleFactory.class,
            inv -> {
              Entity obstacle = new Entity();
              obstacles.add(obstacle);
              return obstacle;
            });
    staticMocks.add(obstacleFactory);
    lootFactory = mockStatic(LootFactory.class, inv -> new Entity());
    staticMocks.add(lootFactory);
    tortoiseFactory =
        mockStatic(
            TortoiseFactory.class,
            inv ->
                inv.getMethod().getReturnType() == Entity.class
                    ? new Entity()
                    : RETURNS_DEFAULTS.answer(inv));
    staticMocks.add(tortoiseFactory);
  }

  @AfterEach
  void tearDown() throws Exception {
    for (AutoCloseable staticMock : staticMocks) {
      staticMock.close();
    }
    resetRegistries();
  }

  private static void resetRegistries() {
    EntitySpawnRegistry.clear();
    EnemyRegistry.loadFrom(List.of());
    LootRegistry.loadFrom(List.of());
    NpcQuestRegistry.reset();
    TortoisePlan.reset();
  }

  // ---- helpers ----

  private Vector2 worldOf(int x, int y) {
    return y == hiddenRow ? null : new Vector2(x * TS, y * TS);
  }

  private Entity newPlayer() {
    Entity player = new Entity().addComponent(new CombatStatsComponent(100, 10));
    createdPlayers.add(player);
    return player;
  }

  /** A 12x8 map using every feature the area builds from map data. */
  private static LevelMapData fullMap() {
    MapLayerData terrain = new MapLayerData(LevelMapData.TERRAIN_LAYER, 12, 8);
    fill(terrain, 0, 0, 12, 1, WALL); // floor
    fill(terrain, 0, 1, 1, 7, WALL); // left wall
    fill(terrain, 3, 3, 3, 1, PLATFORM);
    fill(terrain, 7, 4, 2, 1, ONE_WAY);
    terrain.set(10, 1, HAZARD);

    MapLayerData hazards = new MapLayerData("hazards", 12, 8);
    hazards.set(11, 1, HAZARD);

    MapLayerData decor = new MapLayerData("decor", 12, 8);
    decor.set(2, 5, GLOW);
    decor.set(4, 6, ANIMATED);
    decor.set(6, 6, LAMP);

    MapSpawns spawns = new MapSpawns();
    spawns.setPlayer(new GridPoint2(2, 1));
    spawns.addEnemy(new SpawnPoint("grunt", 4, 1));
    spawns.addEnemy(new SpawnPoint("named", 5, 1));
    spawns.addEnemy(new SpawnPoint("ghost", 6, 1));
    spawns.addEnemy(new SpawnPoint("grunt", 7, 1));
    spawns.addLoot(new SpawnPoint(null, 8, 1));
    spawns.addLoot(new SpawnPoint(null, 9, 1));
    spawns.addLoot(new SpawnPoint(null, 10, 1));
    spawns.addMarker(new Marker("npc", "villager", new GridPoint2(3, 1), Map.of()));
    spawns.addMarker(new Marker("npc", "villager", new GridPoint2(4, 2), Map.of()));
    spawns.addMarker(new Marker("npc", "stranger", new GridPoint2(5, 2), Map.of()));

    return LevelMapData.builder(MAP_NAME)
        .tileSize(TS)
        .size(12, 8)
        .layers(List.of(terrain, hazards, decor))
        .spawns(spawns)
        .transitions(
            List.of(
                new RoomTransition("east", new GridPoint2(11, 1), 1, 2, "door.png", "b.json", null),
                new RoomTransition("west", new GridPoint2(1, 1), 1, 2, null, "c.json", null)))
        .backgroundTexture("images/background.png")
        .subLevels(
            List.of(
                new SubLevel(
                    "dungeon", "DUNGEON", new SubLevel.Bounds(0, 0, 12, 4), null, null, null)))
        .lighting(LIGHTING)
        .backdrops(Map.of("*", List.of(new BackdropLayer("sky.png", 0.5f, 0f, 0f))))
        .overlays(Map.of("*", List.of(new BackdropLayer("fog.png", 1f, 0f, 0f))))
        .build();
  }

  /** A map with no tiles, spawns, lighting or art at all. */
  private static LevelMapData bareMap(String name) {
    return LevelMapData.builder(name)
        .tileSize(TS)
        .size(12, 8)
        .layers(List.of(new MapLayerData(LevelMapData.TERRAIN_LAYER, 12, 8)))
        .build();
  }

  /** A map whose only collision is a solid floor, plus whatever spawns are given. */
  private static LevelMapData floorMap(MapSpawns spawns) {
    MapLayerData terrain = new MapLayerData(LevelMapData.TERRAIN_LAYER, 12, 8);
    fill(terrain, 0, 0, 12, 1, WALL);
    return LevelMapData.builder(MAP_NAME)
        .tileSize(TS)
        .size(12, 8)
        .layers(List.of(terrain))
        .spawns(spawns)
        .build();
  }

  private static void fill(
      MapLayerData layer, int startX, int startY, int width, int height, TileDefinition def) {
    for (int x = startX; x < startX + width; x++) {
      for (int y = startY; y < startY + height; y++) {
        layer.set(x, y, def);
      }
    }
  }

  private static void registerSpawns() {
    EntitySpawnRegistry.register(
        "grunt", player -> new Entity().addComponent(new EnemyTypeComponent(EnemyType.MEDUSA)));
    EntitySpawnRegistry.register(
        "named",
        player ->
            new Entity()
                .addComponent(new EnemyTypeComponent(EnemyType.MEDUSA))
                .addComponent(new NameComponent("Bob")));
    EntitySpawnRegistry.register("villager", player -> new Entity());
  }

  private LevelGameArea areaFor(LevelMapData map) {
    when(mapLoader.load(MAP_PATH)).thenReturn(map);
    return new LevelGameArea(terrainFactory, MAP_PATH, mapLoader);
  }

  /** Runs {@code create()} with the GPU- and physics-bound components mocked at construction. */
  private void create(LevelGameArea area) {
    List<MockedConstruction<?>> constructions = new ArrayList<>();
    try {
      for (Class<?> type :
          List.of(
              GameAreaDisplay.class,
              ParallaxBackdropRenderComponent.class,
              MapBackgroundRenderComponent.class,
              GlowRenderComponent.class,
              SheetAnimationRenderComponent.class,
              PhysicsComponent.class,
              ColliderComponent.class,
              RoomTransitionComponent.class,
              TextureRenderComponent.class,
              LightComponent.class,
              NameDisplay.class)) {
        constructions.add(recordConstructions(type));
      }
      area.create();
    } finally {
      constructions.forEach(MockedConstruction::close);
    }
  }

  private <T> MockedConstruction<T> recordConstructions(Class<T> type) {
    return mockConstruction(
        type,
        withSettings().defaultAnswer(RETURNS_SELF),
        (mocked, context) ->
            built.computeIfAbsent(type, k -> new ArrayList<>()).add(context.arguments()));
  }

  private List<List<?>> built(Class<?> type) {
    return built.getOrDefault(type, List.of());
  }

  private static Fixture fixtureOf(Entity entity) {
    BodyUserData userData = new BodyUserData();
    userData.entity = entity;
    Body body = mock(Body.class);
    when(body.getUserData()).thenReturn(userData);
    Fixture fixture = mock(Fixture.class);
    when(fixture.getBody()).thenReturn(body);
    return fixture;
  }

  private static Entity hazard(Integer damage) {
    Entity hazard =
        new Entity().addComponent(new ColliderComponent().setLayer(PhysicsLayer.HAZARD));
    if (damage != null) {
      hazard.addComponent(new HazardDamageComponent(damage));
    }
    return hazard;
  }

  private static int health(Entity player) {
    return player.getComponent(CombatStatsComponent.class).getHealth();
  }

  // ---- before create ----

  @Test
  void beforeCreate_hasNoLevelOrPlayer() {
    LevelGameArea area = areaFor(fullMap());

    assertNull(area.getLevel());
    assertNull(area.getMapData());
    assertNull(area.getPlayer());
    assertFalse(area.isPlayerDead());
    assertFalse(area.recoverPlayerIfOutOfBounds());
    assertNull(area.consumePendingTransition());
    assertEquals(MAP_PATH, area.getMapPath());
  }

  @Test
  void disposeBeforeCreate_stopsMusicAndUnloadsOnlyEntityAssets() {
    LevelGameArea area = areaFor(fullMap());

    area.dispose();

    verify(music).stop();
    verify(resources, times(4)).unloadAssets(any(String[].class));
  }

  // ---- map loading and assets ----

  @Test
  void create_loadsTheMapAndItsAssets() {
    LevelMapData map = fullMap();
    LevelGameArea area = areaFor(map);

    create(area);

    verify(mapLoader).load(MAP_PATH);
    assertSame(map, area.getMapData());
    assertEquals(MAP_NAME, area.getLevel().name());
    verify(resources, atLeastOnce()).loadTextures(any(String[].class));
    verify(resources).loadTextureAtlases(any(String[].class));
    verify(resources).loadSounds(any(String[].class));
    verify(resources).loadMusic(any(String[].class));
    verify(resources, times(2)).loadForMillis(10);
    assertEquals(MAP_NAME, built(GameAreaDisplay.class).getFirst().getFirst());
  }

  @Test
  void create_reportsMapSize() {
    LevelGameArea area = areaFor(fullMap());

    create(area);

    assertEquals(6f, area.getMapWorldWidth(), EPS);
    assertEquals(4f, area.getMapWorldHeight(), EPS);
    assertEquals(new Vector2(3f, 2f), area.getMapCenter());
  }

  @Test
  void create_buildsTheTerrainFromTheMap() {
    LevelMapData map = fullMap();
    LevelGameArea area = areaFor(map);

    create(area);

    verify(terrainFactory).createTerrainFromMap(map);
    assertSame(terrain, area.terrain);
  }

  @Test
  void create_playsLoopingMusic() {
    LevelGameArea area = areaFor(fullMap());

    create(area);
    area.resumeMusic();

    verify(music, times(2)).setLooping(true);
    verify(music, times(2)).play();
  }

  @Test
  void dispose_stopsMusicAndUnloadsMapTextures() {
    LevelGameArea area = areaFor(fullMap());
    create(area);

    area.dispose();

    verify(music).stop();
    verify(resources, times(5)).unloadAssets(any(String[].class));
  }

  // ---- backdrops, glows and animated tiles ----

  @Test
  void create_spawnsBackdropOverlayAndBackground() {
    LevelGameArea area = areaFor(fullMap());

    create(area);

    List<List<?>> parallax = built(ParallaxBackdropRenderComponent.class);
    assertEquals(2, parallax.size());
    assertEquals(1, parallax.get(0).size(), "backdrops use the one-argument constructor");
    assertEquals(true, parallax.get(1).get(1), "overlays pass the overlay flag");
    assertEquals(
        List.of("images/background.png"), built(MapBackgroundRenderComponent.class).getFirst());
  }

  @Test
  void bareMap_spawnsNoBackdrops() {
    LevelGameArea area = areaFor(bareMap(MAP_NAME));

    create(area);

    assertTrue(built(ParallaxBackdropRenderComponent.class).isEmpty());
    assertTrue(built(MapBackgroundRenderComponent.class).isEmpty());
  }

  @Test
  void create_spawnsAGlowCentredOnEachGlowTile() {
    LevelGameArea area = areaFor(fullMap());

    create(area);

    List<List<?>> glows = built(GlowRenderComponent.class);
    assertEquals(1, glows.size());
    assertEquals("images/glow.png", glows.getFirst().get(0));
    assertEquals(5f * TS, (float) glows.getFirst().get(1), EPS);
  }

  @Test
  void create_spawnsAnAnimationOverEachAnimatedTile() {
    LevelGameArea area = areaFor(fullMap());

    create(area);

    List<List<?>> sheets = built(SheetAnimationRenderComponent.class);
    assertEquals(1, sheets.size());
    assertEquals("images/flame.png", sheets.getFirst().get(0));
    assertEquals(4, sheets.getFirst().get(1));
    assertEquals(6f, (float) sheets.getFirst().get(2), EPS);
  }

  // ---- colliders ----

  @Test
  void create_mergesSolidTilesIntoRectangles() {
    LevelGameArea area = areaFor(fullMap());

    create(area);

    obstacleFactory.verify(() -> ObstacleFactory.createSolidTile(12 * TS, TS), times(1));
    obstacleFactory.verify(() -> ObstacleFactory.createSolidTile(TS, 7 * TS), times(1));
  }

  @Test
  void create_spawnsPlatformRowsOfTheRightKind() {
    LevelGameArea area = areaFor(fullMap());

    create(area);

    obstacleFactory.verify(() -> ObstacleFactory.createFloorTile(3 * TS, 0.2f), times(1));
    obstacleFactory.verify(() -> ObstacleFactory.createOneWayPlatform(2 * TS, 0.2f), times(1));
  }

  @Test
  void create_spawnsHazardsFromTheCollisionAndHazardLayers() {
    LevelGameArea area = areaFor(fullMap());

    create(area);

    obstacleFactory.verify(() -> ObstacleFactory.createHazardTile(TS, TS), times(2));
    List<Entity> hazards =
        area.areaEntities.stream()
            .filter(e -> e.getComponent(HazardDamageComponent.class) != null)
            .toList();
    assertEquals(2, hazards.size());
    assertEquals(15, hazards.getFirst().getComponent(HazardDamageComponent.class).getDamage());
    assertEquals(new Vector2(10 * TS, TS + TS / 2f), hazards.getFirst().getPosition());
  }

  @Test
  void create_spawnsEveryCollider() {
    LevelGameArea area = areaFor(fullMap());

    create(area);

    assertEquals(6, obstacles.size());
    assertTrue(area.areaEntities.containsAll(obstacles));
  }

  @Test
  void collidersOnTilesWithNoWorldPosition_areSkipped() {
    MapLayerData terrainLayer = new MapLayerData(LevelMapData.TERRAIN_LAYER, 12, 8);
    fill(terrainLayer, 0, 0, 2, 1, WALL);
    fill(terrainLayer, 3, 0, 2, 1, PLATFORM);
    fill(terrainLayer, 6, 0, 2, 1, ONE_WAY);
    terrainLayer.set(9, 0, HAZARD);
    MapSpawns spawns = new MapSpawns();
    spawns.setPlayer(new GridPoint2(2, 2));
    LevelMapData map =
        LevelMapData.builder(MAP_NAME)
            .tileSize(TS)
            .size(12, 8)
            .layers(List.of(terrainLayer))
            .spawns(spawns)
            .lighting(LIGHTING)
            .build();
    hiddenRow = 0;
    LevelGameArea area = areaFor(map);

    create(area);

    assertEquals(4, obstacles.size());
    assertTrue(obstacles.stream().noneMatch(area.areaEntities::contains));
    // with no terrain origin, the only light is the player's
    assertEquals(1, built(LightComponent.class).size());
  }

  @Test
  void platformRows_stopAtOtherTilesAndAtTheMapEdge() {
    MapLayerData terrainLayer = new MapLayerData(LevelMapData.TERRAIN_LAYER, 4, 4);
    fill(terrainLayer, 0, 1, 2, 1, PLATFORM);
    terrainLayer.set(2, 1, WALL);
    terrainLayer.set(3, 1, ONE_WAY);
    LevelMapData map =
        LevelMapData.builder(MAP_NAME)
            .tileSize(TS)
            .size(4, 4)
            .layers(List.of(terrainLayer))
            .build();
    LevelGameArea area = areaFor(map);

    create(area);

    obstacleFactory.verify(() -> ObstacleFactory.createFloorTile(2 * TS, 0.2f), times(1));
    obstacleFactory.verify(() -> ObstacleFactory.createOneWayPlatform(TS, 0.2f), times(1));
    obstacleFactory.verify(() -> ObstacleFactory.createSolidTile(TS, TS), times(1));
  }

  @Test
  void mapWithNoCollisionLayer_spawnsNoColliders() {
    LevelMapData map =
        LevelMapData.builder(MAP_NAME)
            .tileSize(TS)
            .size(4, 4)
            .layers(List.of(new MapLayerData("background", 4, 4)))
            .build();
    LevelGameArea area = areaFor(map);

    create(area);

    assertTrue(obstacles.isEmpty());
  }

  // ---- player ----

  @Test
  void create_spawnsANewPlayerOnTheMapSpawn() {
    LevelGameArea area = areaFor(fullMap());

    create(area);

    Entity player = area.getPlayer();
    assertSame(createdPlayers.getFirst(), player);
    assertTrue(area.areaEntities.contains(player));
    assertEquals(2 * TS + (TS - 1f) / 2f, player.getPosition().x, EPS);
    assertFalse(area.isPlayerDead());
  }

  @Test
  void mapWithoutAPlayerSpawn_putsThePlayerAtTheOrigin() {
    LevelGameArea area = areaFor(bareMap(MAP_NAME));

    create(area);

    assertEquals((TS - 1f) / 2f, area.getPlayer().getPosition().x, EPS);
  }

  @Test
  void isPlayerDead_followsThePlayersHealth() {
    LevelGameArea area = areaFor(fullMap());
    create(area);

    area.getPlayer().getComponent(CombatStatsComponent.class).setHealth(0);

    assertTrue(area.isPlayerDead());
  }

  @Test
  void respawnPlayer_replacesTheOldPlayer() {
    LevelGameArea area = areaFor(fullMap());
    create(area);
    Entity old = area.getPlayer();

    Entity fresh = area.respawnPlayer();

    assertNotSame(old, fresh);
    assertSame(fresh, area.getPlayer());
    assertFalse(area.areaEntities.contains(old));
    assertTrue(area.areaEntities.contains(fresh));
  }

  @Test
  void releasePlayer_handsThePlayerOverWithoutDisposingIt() {
    LevelGameArea area = areaFor(fullMap());
    create(area);
    Entity player = area.getPlayer();

    assertSame(player, area.releasePlayer());
    assertFalse(area.areaEntities.contains(player));
  }

  // ---- hazard damage ----

  @Test
  void touchingAHazard_hurtsThePlayerByItsDamage() {
    LevelGameArea area = areaFor(fullMap());
    create(area);
    Entity player = area.getPlayer();

    player.getEvents().trigger("collisionStart", fixtureOf(player), fixtureOf(hazard(15)));

    assertEquals(85, health(player));
  }

  @Test
  void touchingAHazardAgainStraightAway_doesNoMoreDamage() {
    LevelGameArea area = areaFor(fullMap());
    create(area);
    Entity player = area.getPlayer();

    player.getEvents().trigger("collisionStart", fixtureOf(player), fixtureOf(hazard(15)));
    player.getEvents().trigger("collisionStart", fixtureOf(player), fixtureOf(hazard(15)));

    assertEquals(85, health(player));
  }

  @Test
  void aHazardWithNoDamageSet_usesTheDefault_whicheverFixtureIsThePlayer() {
    LevelGameArea area = areaFor(fullMap());
    create(area);
    Entity player = area.getPlayer();

    player.getEvents().trigger("collisionStart", fixtureOf(hazard(null)), fixtureOf(player));

    assertEquals(90, health(player));
  }

  @Test
  void touchingAnythingElse_doesNoDamage() {
    LevelGameArea area = areaFor(fullMap());
    create(area);
    Entity player = area.getPlayer();
    Entity wall =
        new Entity().addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));

    player.getEvents().trigger("collisionStart", fixtureOf(player), fixtureOf(new Entity()));
    player.getEvents().trigger("collisionStart", fixtureOf(player), fixtureOf(wall));

    assertEquals(100, health(player));
  }

  // ---- adopting a retained player ----

  private LevelGameArea adoptingArea(
      LevelMapData map, Entity existing, GridPoint2 entry, Long seed) {
    try (MockedConstruction<JsonMapLoader> loaders =
        mockConstruction(
            JsonMapLoader.class,
            (loader, context) -> when(loader.load(MAP_PATH)).thenReturn(map))) {
      return seed == null
          ? new LevelGameArea(terrainFactory, MAP_PATH, existing, entry)
          : new LevelGameArea(terrainFactory, MAP_PATH, existing, entry, seed, (Camera) null);
    }
  }

  @Test
  void adoptedPlayer_isMovedToTheEntranceAndGivenTheNewMap() {
    LevelMapData map = fullMap();
    Body body = mock(Body.class);
    PhysicsComponent physics = mock(PhysicsComponent.class);
    when(physics.getBody()).thenReturn(body);
    LadderComponent ladder = mock(LadderComponent.class);
    SubLevelTravelComponent travel = mock(SubLevelTravelComponent.class);
    Entity existing = spy(new Entity());
    doReturn(physics).when(existing).getComponent(PhysicsComponent.class);
    doReturn(ladder).when(existing).getComponent(LadderComponent.class);
    doReturn(travel).when(existing).getComponent(SubLevelTravelComponent.class);
    LevelGameArea area = adoptingArea(map, existing, new GridPoint2(5, 2), 42L);

    create(area);

    assertSame(existing, area.getPlayer());
    assertTrue(area.areaEntities.contains(existing));
    assertTrue(createdPlayers.isEmpty(), "no new player is built");
    assertEquals(42L, area.getLootSeed());
    verify(ladder).setMapData(map);
    verify(travel).setMapData(map);
    verify(body).setLinearVelocity(0f, 0f);
    verify(body).setAngularVelocity(0f);
    verify(body).setAwake(true);
    assertEquals(5 * TS + (TS - 1f) / 2f, existing.getPosition().x, EPS);
    assertEquals(2 * TS + (TS - 1f) / 2f, existing.getPosition().y, EPS);
  }

  @Test
  void adoptedPlayerWithNoEntrance_goesToTheMapSpawn() {
    Entity existing = new Entity();
    LevelGameArea area = adoptingArea(fullMap(), existing, null, null);

    create(area);

    assertEquals(2 * TS + (TS - 1f) / 2f, existing.getPosition().x, EPS);
    assertFalse(area.isPlayerDead(), "a player with no combat stats is never dead");
  }

  @Test
  void adoptedPlayerWithNoEntranceOrMapSpawn_goesToTheOrigin() {
    Entity existing = new Entity();
    LevelGameArea area = adoptingArea(bareMap(MAP_NAME), existing, null, null);

    create(area);

    assertEquals((TS - 1f) / 2f, existing.getPosition().x, EPS);
  }

  @Test
  void everyPublicConstructor_loadsTheGivenMap() {
    LevelMapData map = fullMap();
    Entity existing = new Entity();
    Camera camera = mock(Camera.class);
    List<LevelGameArea> areas;
    try (MockedConstruction<JsonMapLoader> loaders =
        mockConstruction(
            JsonMapLoader.class,
            (loader, context) -> when(loader.load(MAP_PATH)).thenReturn(map))) {
      areas =
          List.of(
              new LevelGameArea(terrainFactory, MAP_PATH),
              new LevelGameArea(terrainFactory, MAP_PATH, camera),
              new LevelGameArea(terrainFactory, MAP_PATH, existing, null, 7L),
              new LevelGameArea(terrainFactory, MAP_PATH, existing, null, camera));
    }

    for (LevelGameArea area : areas) {
      assertEquals(MAP_PATH, area.getMapPath());
    }
    assertEquals(7L, areas.get(2).getLootSeed());
    create(areas.get(1));
    assertSame(map, areas.get(1).getMapData());
  }

  // ---- out-of-bounds recovery ----

  @Test
  void playerInsideTheMap_isNotRecovered() {
    LevelGameArea area = areaFor(fullMap());
    create(area);

    assertFalse(area.recoverPlayerIfOutOfBounds());
  }

  @Test
  void playerOutsideTheMap_isReturnedToTheMapSpawnAndStopped() {
    LevelMapData map = fullMap();
    Body body = mock(Body.class);
    PhysicsComponent physics = mock(PhysicsComponent.class);
    when(physics.getBody()).thenReturn(body);
    LadderComponent ladder = mock(LadderComponent.class);
    Entity existing = spy(new Entity());
    doReturn(physics).when(existing).getComponent(PhysicsComponent.class);
    doReturn(ladder).when(existing).getComponent(LadderComponent.class);
    LevelGameArea area = adoptingArea(map, existing, new GridPoint2(5, 2), 1L);
    create(area);
    existing.setPosition(-10f, 1f);

    assertTrue(area.recoverPlayerIfOutOfBounds());

    assertEquals(2 * TS + (TS - 1f) / 2f, existing.getPosition().x, EPS);
    verify(body).setGravityScale(1f);
    verify(body, times(2)).setLinearVelocity(0f, 0f);
    verify(ladder, times(2)).setMapData(map);
  }

  @Test
  void playerOutsideAMapWithNoSpawn_isReturnedToTheEntrance() {
    Entity existing = new Entity();
    LevelGameArea area = adoptingArea(bareMap(MAP_NAME), existing, new GridPoint2(6, 3), null);
    create(area);
    existing.setPosition(20f, 1f);

    assertTrue(area.recoverPlayerIfOutOfBounds());

    assertEquals(6 * TS + (TS - 1f) / 2f, existing.getPosition().x, EPS);
  }

  @Test
  void playerOutsideAMapWithNoSafeSpawn_isLeftAlone() {
    LevelGameArea area = areaFor(bareMap(MAP_NAME));
    create(area);
    area.getPlayer().setPosition(2f, -10f);

    assertFalse(area.recoverPlayerIfOutOfBounds());
    assertEquals(-10f, area.getPlayer().getPosition().y, EPS);
  }

  // ---- room transitions ----

  @Test
  void create_spawnsADoorwayForEachTransition() {
    LevelGameArea area = areaFor(fullMap());

    create(area);

    List<List<?>> doors = built(RoomTransitionComponent.class);
    assertEquals(2, doors.size());
    assertEquals("east", ((RoomTransition) doors.get(0).get(0)).getId());
    assertSame(area.getPlayer(), doors.get(0).get(1));
    assertEquals(List.of("door.png"), built(TextureRenderComponent.class).getFirst());
    assertEquals(1, built(TextureRenderComponent.class).size(), "only one doorway has a texture");
  }

  @Test
  @SuppressWarnings("unchecked")
  void aDoorwayRequest_isHeldUntilConsumed() {
    LevelGameArea area = areaFor(fullMap());
    create(area);
    List<?> door = built(RoomTransitionComponent.class).getFirst();
    RoomTransition transition = (RoomTransition) door.get(0);

    ((Consumer<RoomTransition>) door.get(2)).accept(transition);

    assertSame(transition, area.consumePendingTransition());
    assertNull(area.consumePendingTransition());
  }

  // ---- enemies ----

  @Test
  void create_spawnsKnownLiveEnemiesOnly() {
    registerSpawns();
    EnemyRegistry.markKilled(EnemyId.of(MAP_NAME, new GridPoint2(7, 1)));
    LevelGameArea area = areaFor(fullMap());

    create(area);

    List<Entity> enemies =
        area.areaEntities.stream()
            .filter(e -> e.getComponent(EnemyTypeComponent.class) != null)
            .toList();
    assertEquals(2, enemies.size(), "the unknown and the killed enemy are skipped");
    assertEquals(4 * TS + (TS - 1f) / 2f, enemies.getFirst().getPosition().x, EPS);
    assertEquals(1, built(NameDisplay.class).size(), "only the named enemy gets a name tag");
  }

  @Test
  void anAlreadyKilledFinalBoss_meansTheGameIsWon() {
    MapSpawns spawns = new MapSpawns();
    spawns.addEnemy(new SpawnPoint("zeus", 26, 4));
    LevelMapData map =
        LevelMapData.builder("Level 3 - Zeus's Palace")
            .tileSize(TS)
            .size(30, 8)
            .layers(List.of(new MapLayerData(LevelMapData.TERRAIN_LAYER, 30, 8)))
            .spawns(spawns)
            .build();
    EnemyRegistry.markKilled(EnemyId.of("Level 3 - Zeus's Palace", new GridPoint2(26, 4)));
    LevelGameArea area = areaFor(map);

    create(area);

    assertTrue(area.isFinalBossDefeated());
  }

  @Test
  void spawnEnemy_placesAKnownEnemyAtTheGivenPosition() {
    registerSpawns();
    LevelGameArea area = areaFor(fullMap());
    create(area);

    assertTrue(area.spawnEnemy("grunt", new Vector2(1f, 2f)));
    assertFalse(area.spawnEnemy("grunt", null));
    assertFalse(area.spawnEnemy("ghost", new Vector2(1f, 2f)));

    Entity spawned = area.areaEntities.getLast();
    assertNotNull(spawned.getComponent(EnemyTypeComponent.class));
    assertEquals(new Vector2(1f, 2f), spawned.getPosition());
  }

  // ---- tortoises ----

  private void verifyTortoise(String id, int count) {
    tortoiseFactory.verify(() -> TortoiseFactory.createTortoise(id), times(count));
  }

  @Test
  void level1_spawnsItsHandPlacedTortoise() {
    LevelGameArea area = areaFor(bareMap("Level 1 - Out of the Underworld"));

    create(area);

    verifyTortoise("level1_a", 1);
  }

  @Test
  void level1_skipsAFoundTortoise() {
    tortoiseFactory.when(() -> TortoiseFactory.isFound("level1_a")).thenReturn(true);
    LevelGameArea area = areaFor(bareMap("Level 1 - Out of the Underworld"));

    create(area);

    verifyTortoise("level1_a", 0);
  }

  @Test
  void otherLevels_haveNoHandPlacedTortoise() {
    LevelGameArea area = areaFor(bareMap(MAP_NAME));

    create(area);

    verifyTortoise("level1_a", 0);
  }

  @Test
  void plannedTortoisesWithNoCarrier_goOnTheGround() {
    registerSpawns();
    TortoisePlan.set(Map.of(MAP_NAME, 2));
    LevelGameArea area = areaFor(fullMap());

    create(area);

    tortoiseFactory.verify(() -> TortoiseFactory.createTortoise(anyString()), times(2));
  }

  @Test
  void groundTortoisesOnAMapWithNoPlayerSpawn_stillGetASpot() {
    TortoisePlan.set(Map.of(MAP_NAME, 1));
    LevelGameArea area = areaFor(bareMap(MAP_NAME));

    create(area);

    tortoiseFactory.verify(() -> TortoiseFactory.createTortoise(anyString()), times(1));
  }

  // ---- loot ----

  @Test
  void declaredLoot_putsTheShieldFirstAndSkipsCollectedSpots() {
    LootRegistry.markCollected(LootId.of(MAP_NAME, new GridPoint2(9, 1)));
    LevelGameArea area = areaFor(fullMap());

    create(area);

    lootFactory.verify(() -> LootFactory.createLoot(any()), times(2));
  }

  @Test
  void aCollectedShield_isNotSpawnedAgain() {
    LootRegistry.markCollected(LootId.of(MAP_NAME, new GridPoint2(8, 1)));
    LevelGameArea area = areaFor(fullMap());

    create(area);

    lootFactory.verify(() -> LootFactory.createLoot(any()), times(2));
  }

  @Test
  void undeclaredLoot_isScatteredOverTheGround() {
    MapSpawns spawns = new MapSpawns();
    spawns.setPlayer(new GridPoint2(1, 1));
    LevelGameArea area = areaFor(floorMap(spawns));

    create(area);

    lootFactory.verify(() -> LootFactory.createLoot(any()), times(8));
  }

  @Test
  void aMapWithNoGround_getsTheStarterLootRow() {
    LevelGameArea area = areaFor(bareMap(MAP_NAME));

    create(area);

    lootFactory.verify(
        () -> LootFactory.createLoot(any()), times(4 + ConsumableType.values().length));
  }

  @Test
  void theStarterLootRow_startsBesideThePlayer() {
    MapSpawns spawns = new MapSpawns();
    spawns.setPlayer(new GridPoint2(3, 2));
    LevelMapData map =
        LevelMapData.builder(MAP_NAME)
            .tileSize(TS)
            .size(12, 8)
            .layers(List.of(new MapLayerData(LevelMapData.TERRAIN_LAYER, 12, 8)))
            .spawns(spawns)
            .build();
    LevelGameArea area = areaFor(map);

    create(area);

    lootFactory.verify(
        () -> LootFactory.createLoot(any()), times(4 + ConsumableType.values().length));
  }

  // ---- NPCs ----

  @Test
  void create_spawnsLiveKnownNpcsAtTheirMarkers() {
    registerSpawns();
    NpcQuestRegistry.markKilled("villager@" + EnemyId.of(MAP_NAME, new GridPoint2(4, 2)));
    LevelGameArea area = areaFor(fullMap());

    create(area);

    List<Entity> npcs =
        area.areaEntities.stream()
            .filter(
                e ->
                    e.getComponent(com.csse3200.game.entities.spawn.PersistentNpcComponent.class)
                        != null)
            .toList();
    assertEquals(1, npcs.size(), "the killed and the unknown NPC are skipped");
    assertEquals(3 * TS + (TS - 1f) / 2f, npcs.getFirst().getPosition().x, EPS);
  }

  // ---- lighting ----

  @Test
  void create_usesTheStartingSubLevelsAmbientAndLightsTheMap() {
    LevelGameArea area = areaFor(fullMap());

    create(area);

    verify(lightService).setAmbient(Color.RED, 0.25f);
    assertEquals(2, built(LightComponent.class).size(), "the lamp and the player");
    area.fadeAmbientForSection("dungeon", 1f);
    verify(lightService).fadeAmbientTo(Color.RED, 0.25f, 1f);
  }

  @Test
  void aMapWithNoLighting_resetsTheAmbient() {
    LevelGameArea area = areaFor(bareMap(MAP_NAME));

    create(area);

    verify(lightService).setAmbient(LightColour.AMBIENT_LIGHT.getColour(), 1f);
    assertTrue(built(LightComponent.class).isEmpty());
    verify(lightService, never()).fadeAmbientTo(any(Color.class), anyFloat(), anyFloat());
  }
}
