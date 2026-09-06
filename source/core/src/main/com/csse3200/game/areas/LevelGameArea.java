package com.csse3200.game.areas;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.areas.terrain.CollisionType;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.areas.terrain.map.JsonMapLoader;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.MapLayerData;
import com.csse3200.game.areas.terrain.map.MapLoader;
import com.csse3200.game.areas.terrain.map.RoomTransition;
import com.csse3200.game.areas.terrain.map.SpawnPoint;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.loot.ConsumableGenerator;
import com.csse3200.game.components.loot.ConsumableType;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.room.RoomTransitionComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.LootFactory;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.events.listeners.EventListener2;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A game area built from a tile-based level map file. Loads the map via a {@link MapLoader},
 * renders it through the terrain system, and spawns the player and enemies from the map's spawn
 * data.
 *
 * <p>Textures for map tiles are loaded from the map's legend; entity textures are loaded from the
 * static lists below. Physical collision from the tile data is handled by the Collision Layer task
 * (#16), which reads {@link LevelMapData#getCollisionLayer()}.
 */
public class LevelGameArea extends GameArea {
  private static final Logger logger = LoggerFactory.getLogger(LevelGameArea.class);
  private static final float COLLIDER_HEIGHT = 0.2f;
  private static final long HAZARD_DAMAGE_COOLDOWN_MS = 500;
  private static final int HAZARD_DAMAGE = 10;

  /** Entity textures needed by the player, enemies, and loot items. */
  private static final String[] entityTextures = {
    "images/box_boy_leaf.png",
    "images/box_boy_crouch.png",
    "images/box_boy_slide.png",
    "images/ghost_king.png",
    "images/ghost_1.png",
    "images/sword.png",
    "images/bow.png",
    "images/arrow.png",
    "images/Health.png",
    "images/Poison.png",
    "images/Strength.png"
  };

  private static final String[] entitySounds = {
    "sounds/Impact4.ogg",
    "sounds/player-hit.ogg",
    "sounds/player-hit-crown.ogg",
    "sounds/walking1.mp3",
    "sounds/jump.mp3",
    "sounds/dash.mp3",
    "sounds/sneaking1.mp3",
    "sounds/slide.mp3"
  };

  private static final String[] entityAtlases = {
    "images/ghost.atlas",
    "images/ghostKing.atlas",
    "images/gold_coin/gold_coin.atlas",
    "images/skeleton.atlas"
  };

  private static final String BACKGROUND_MUSIC = "sounds/BGM_03_mp3.mp3";
  private static final String[] ENTITY_MUSIC = {BACKGROUND_MUSIC};

  private final TerrainFactory terrainFactory;
  private final MapLoader mapLoader;
  private final String mapPath;
  private final Entity existingPlayer;
  private final GridPoint2 entrySpawn;

  private LevelMapData mapData;
  private Entity player;
  private RoomTransition pendingTransition;

  /**
   * Create a level area using the default {@link JsonMapLoader}.
   *
   * @param terrainFactory factory used to build the terrain
   * @param mapPath asset path of the map file to load
   */
  public LevelGameArea(TerrainFactory terrainFactory, String mapPath) {
    this(terrainFactory, mapPath, new JsonMapLoader());
  }

  /**
   * Create a level area with an explicit map loader (useful for tests or alternative formats).
   *
   * @param terrainFactory factory used to build the terrain
   * @param mapPath asset path of the map file to load
   * @param mapLoader loader used to parse the map file
   */
  public LevelGameArea(TerrainFactory terrainFactory, String mapPath, MapLoader mapLoader) {
    this(terrainFactory, mapPath, mapLoader, null, null);
  }

  /**
   * Create a level while retaining an existing player and placing it at a specified entrance.
   * Reusing the entity preserves all player component state, including health and inventory.
   *
   * @param terrainFactory factory used to build the terrain
   * @param mapPath asset path of the map file to load
   * @param existingPlayer already registered player entity to retain
   * @param entrySpawn destination entrance tile, or {@code null} for the map's player spawn
   */
  public LevelGameArea(
      TerrainFactory terrainFactory, String mapPath, Entity existingPlayer, GridPoint2 entrySpawn) {
    this(terrainFactory, mapPath, new JsonMapLoader(), existingPlayer, entrySpawn);
  }

  private LevelGameArea(
      TerrainFactory terrainFactory,
      String mapPath,
      MapLoader mapLoader,
      Entity existingPlayer,
      GridPoint2 entrySpawn) {
    super();
    this.terrainFactory = terrainFactory;
    this.mapPath = mapPath;
    this.mapLoader = mapLoader;
    this.existingPlayer = existingPlayer;
    this.entrySpawn = entrySpawn == null ? null : new GridPoint2(entrySpawn);
  }

  @Override
  public void create() {
    mapData = mapLoader.load(mapPath);
    loadAssets();

    displayUI();
    spawnTerrain();
    spawnCollisions();
    player = existingPlayer == null ? spawnPlayer() : adoptPlayer(existingPlayer);
    spawnTransitions();
    spawnEnemies();
    spawnLoot();
    playMusic();
  }

  /**
   * @return the loaded map data (dimensions, layers, tile types, spawns) for other systems to use
   */
  public LevelMapData getMapData() {
    return mapData;
  }

  /**
   * @return the spawned player entity, or null before {@link #create()} runs
   */
  public Entity getPlayer() {
    return player;
  }

  /**
   * Remove ownership of the persistent player before this room is disposed.
   *
   * @return the player entity to adopt into the next room
   */
  public Entity releasePlayer() {
    areaEntities.remove(player);
    return player;
  }

  /**
   * Return and clear a doorway request. The screen consumes this after the physics update so the
   * Box2D world is not modified from inside a contact callback.
   *
   * @return pending transition, or {@code null}
   */
  public RoomTransition consumePendingTransition() {
    RoomTransition transition = pendingTransition;
    pendingTransition = null;
    return transition;
  }

  /**
   * @return true if the player has been spawned and its combat stats report it as dead
   */
  public boolean isPlayerDead() {
    if (player == null) {
      return false;
    }
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    return stats != null && stats.isDead();
  }

  /**
   * @return the map's width in world units (tiles * tileSize)
   */
  public float getMapWorldWidth() {
    return mapData.getWidth() * mapData.getTileSize();
  }

  /**
   * @return the map's height in world units (tiles * tileSize)
   */
  public float getMapWorldHeight() {
    return mapData.getHeight() * mapData.getTileSize();
  }

  /**
   * @return the world-space centre of the map, useful for positioning the camera
   */
  public Vector2 getMapCenter() {
    return new Vector2(getMapWorldWidth() / 2f, getMapWorldHeight() / 2f);
  }

  private void displayUI() {
    Entity ui = new Entity();
    ui.addComponent(new GameAreaDisplay(mapData.getName()));
    spawnEntity(ui);
  }

  private void spawnTerrain() {
    terrain = terrainFactory.createTerrainFromMap(mapData);
    spawnEntity(new Entity().addComponent(terrain));
  }

  /**
   * Spawns the collisions and collision types based on the map json file. Updated to spawn
   * platforms and rows as a singular layer, rather than individual tiles.
   */
  private void spawnCollisions() {
    MapLayerData collisionLayer = mapData.getCollisionLayer();

    if (collisionLayer == null) {
      return;
    }

    float tileSize = terrain.getTileSize();

    for (int y = 0; y < collisionLayer.getHeight(); y++) {
      int x = 0;

      // iterate x and accumulate collision tiles
      while (x < collisionLayer.getWidth()) {
        TileDefinition def = collisionLayer.get(x, y);

        if (def == null) {
          x++;
          continue;
        }

        CollisionType collisionType = def.type().getCollisionType();

        // Only merge solid/platform tiles.
        if (collisionType != CollisionType.SOLID && collisionType != CollisionType.PLATFORM) {
          x++;
          continue;
        }

        // Find consecutive tiles of the same collision type.
        int startX = x;

        while (x + 1 < collisionLayer.getWidth()) {
          TileDefinition next = collisionLayer.get(x + 1, y);

          if (next == null || next.type().getCollisionType() != collisionType) {
            break;
          }

          x++;
        }

        int tileCount = x - startX + 1;

        spawnCollisionRow(collisionType, startX, y, tileCount, tileSize);

        x++;
      }
    }

    // Hazards remain individual
    spawnHazardCollisions(collisionLayer, tileSize);
  }

  /**
   * Spawns a hazard tile collision layer which will deal damage to the player.
   *
   * @param collisionLayer the collisionLayer to add the hazard to
   * @param tileSize the size of each tile
   */
  private void spawnHazardCollisions(MapLayerData collisionLayer, float tileSize) {

    for (int x = 0; x < collisionLayer.getWidth(); x++) {
      for (int y = 0; y < collisionLayer.getHeight(); y++) {

        TileDefinition def = collisionLayer.get(x, y);

        if (def == null || def.type().getCollisionType() != CollisionType.HAZARD) {
          continue;
        }

        Entity collider = ObstacleFactory.createHazardTile(tileSize, tileSize);

        Vector2 position = terrain.tileToWorldPosition(x, y);

        if (position == null) {
          continue;
        }

        position.add(tileSize / 2f, tileSize / 2f);

        collider.setPosition(position);
        spawnEntity(collider);
      }
    }
  }

  /**
   * Spawns a wide row of a collision layer. Used for spawning in platforms and ground collision
   * layers.
   *
   * @param collisionType: the type of collision (hazard, solit etc).
   * @param startX: that starting x position of the row.
   * @param y: what y the row should be on
   * @param tileCount: how wide/how many tiles it should be
   * @param tileSize: how big each tile is
   */
  private void spawnCollisionRow(
      CollisionType collisionType, int startX, int y, int tileCount, float tileSize) {

    float width = tileCount * tileSize;
    Entity collider;
    float colliderHeight;

    switch (collisionType) {
      case SOLID:
        // Solid wall/floor tiles occupy the full tile. A thin surface can be crossed during the
        // longer frame in which a room is loaded, causing every dynamic entity to fall away.
        colliderHeight = tileSize;
        collider = ObstacleFactory.createSolidTile(width, colliderHeight);
        break;
      case PLATFORM:
        colliderHeight = COLLIDER_HEIGHT;
        collider = ObstacleFactory.createFloorTile(width, colliderHeight);
        break;

      default:
        return;
    }

    Vector2 position = terrain.tileToWorldPosition(startX, y);

    if (position == null) {
      return;
    }

    // tileToWorldPosition is the tile's bottom-left corner. Platforms only need a thin collider at
    // the tile's top; solid rows keep the unmodified bottom-left position and full tile height.
    if (collisionType == CollisionType.PLATFORM) {
      position.y += tileSize - colliderHeight;
    }

    collider.setPosition(position);
    spawnEntity(collider);
  }

  private Entity spawnPlayer() {
    Entity newPlayer = PlayerFactory.createPlayer();

    addHazardCollisionListener(newPlayer);
    positionAndSpawnPlayer(newPlayer, mapData.getSpawns().getPlayer());
    return newPlayer;
  }

  private Entity adoptPlayer(Entity retainedPlayer) {
    GridPoint2 spawn = entrySpawn != null ? entrySpawn : mapData.getSpawns().getPlayer();
    if (spawn == null) {
      spawn = new GridPoint2(0, 0);
    }
    positionEntityAt(retainedPlayer, spawn, true, true);
    PhysicsComponent physics = retainedPlayer.getComponent(PhysicsComponent.class);
    if (physics != null) {
      // A room entrance is a teleport, so momentum from the source doorway must not carry over.
      // Resetting it also prevents a dash/fall from crossing the destination floor on the load
      // frame.
      physics.getBody().setLinearVelocity(0f, 0f);
      physics.getBody().setAngularVelocity(0f);
      physics.getBody().setAwake(true);
    }
    areaEntities.add(retainedPlayer);
    return retainedPlayer;
  }

  private void positionAndSpawnPlayer(Entity newPlayer, GridPoint2 spawn) {
    if (spawn == null) {
      logger.warn("Map '{}' has no player spawn; defaulting to (0, 0)", mapData.getName());
      spawn = new GridPoint2(0, 0);
    }
    spawnEntityAt(newPlayer, spawn, true, true);
  }

  private static void addHazardCollisionListener(Entity newPlayer) {
    final long[] lastHazardDamageTime = {0L};

    newPlayer
        .getEvents()
        .addListener(
            "collisionStart",
            (EventListener2<Fixture, Fixture>)
                (fixtureA, fixtureB) -> {
                  Entity entityA = ((BodyUserData) fixtureA.getBody().getUserData()).entity;

                  Entity entityB = ((BodyUserData) fixtureB.getBody().getUserData()).entity;

                  Entity other;

                  if (entityA == newPlayer) {
                    other = entityB;
                  } else {
                    other = entityA;
                  }

                  ColliderComponent collider = other.getComponent(ColliderComponent.class);

                  if (collider != null && collider.getLayer() == PhysicsLayer.HAZARD) {

                    long currentTime = System.currentTimeMillis();

                    if (currentTime - lastHazardDamageTime[0] >= HAZARD_DAMAGE_COOLDOWN_MS) {

                      CombatStatsComponent stats =
                          newPlayer.getComponent(CombatStatsComponent.class);

                      stats.addHealth(-(int) HAZARD_DAMAGE);

                      lastHazardDamageTime[0] = currentTime;

                      logger.info("Player hit hazard! Health: {}", stats.getHealth());
                    }
                  }
                });
  }

  private void spawnTransitions() {
    float tileSize = terrain.getTileSize();
    for (RoomTransition transition : mapData.getTransitions()) {
      Entity doorway =
          new Entity()
              .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
              .addComponent(new ColliderComponent().setSensor(true))
              .addComponent(
                  new RoomTransitionComponent(
                      transition, player, requested -> pendingTransition = requested));

      if (transition.getTexture() != null) {
        doorway.addComponent(new TextureRenderComponent(transition.getTexture()));
      }

      doorway.setScale(transition.getWidth() * tileSize, transition.getHeight() * tileSize);
      doorway.setPosition(terrain.tileToWorldPosition(transition.getPosition()));
      spawnEntity(doorway);
    }
  }

  private void spawnEnemies() {
    for (SpawnPoint spawn : mapData.getSpawns().getEnemies()) {
      Entity enemy = createEnemy(spawn.getType());
      if (enemy != null) {
        spawnEntityAt(enemy, spawn.getPosition(), true, true);
      }
    }
  }

  private Entity createEnemy(String type) {
    if (type == null) {
      return null;
    }
    return switch (type.toLowerCase()) {
      case "ghost" -> NPCFactory.createGhost(player);
      case "ghostking", "ghost_king" -> NPCFactory.createGhostKing(player);
      case "skeleton" -> NPCFactory.createSkeleton(player);
      case "rangedskeleton", "ranged-skeleton" -> NPCFactory.createRangedSkeleton(player);
      default -> {
        logger.warn("Unknown enemy spawn type '{}' - skipped", type);
        yield null;
      }
    };
  }

  /**
   * Spawns pickup loot (weapons, consumables, and a gold coin) so the loot/inventory features work
   * in this level, mirroring what {@code ForestGameArea} spawns. Items are laid out in a row
   * anchored to the map's first loot spawn point (falling back to just right of the player), so
   * they land on the loaded map regardless of its size.
   */
  private void spawnLoot() {
    List<Entity> items = new ArrayList<>();

    WeaponGenerator weaponGenerator = new WeaponGenerator();
    items.add(LootFactory.createLoot(weaponGenerator.generateWeapon(WeaponType.BOW, 1)));
    items.add(LootFactory.createLoot(weaponGenerator.generateWeapon(WeaponType.SWORD, 1)));

    ConsumableGenerator consumableGenerator = new ConsumableGenerator();
    for (ConsumableType type : ConsumableType.values()) {
      items.add(LootFactory.createLoot(consumableGenerator.generateConsumable(type, 1)));
    }

    items.add(LootFactory.createLoot(new Item("Gold Coin", ItemType.CURRENCY, 1, 99)));

    GridPoint2 start = lootRowStart();
    int maxX = Math.max(0, mapData.getWidth() - 2);
    int x = start.x;
    for (Entity item : items) {
      spawnEntityAt(item, new GridPoint2(Math.min(x, maxX), start.y), true, true);
      x++;
    }
  }

  /**
   * @return the tile position to begin laying out loot: the first map loot spawn, else near the
   *     player.
   */
  private GridPoint2 lootRowStart() {
    if (!mapData.getSpawns().getLoot().isEmpty()) {
      return mapData.getSpawns().getLoot().getFirst().getPosition();
    }
    GridPoint2 playerSpawn = mapData.getSpawns().getPlayer();
    if (playerSpawn != null) {
      return new GridPoint2(playerSpawn.x + 2, playerSpawn.y);
    }
    return new GridPoint2(1, 1);
  }

  private void playMusic() {
    Music music = ServiceLocator.getResourceService().getAsset(BACKGROUND_MUSIC, Music.class);
    music.setLooping(true);
    music.setVolume(0.3f);
    music.play();
  }

  /** Restart this room's music after the previous room releases its shared music asset. */
  public void resumeMusic() {
    playMusic();
  }

  private void loadAssets() {
    logger.debug("Loading level assets");
    ResourceService resourceService = ServiceLocator.getResourceService();

    Set<String> tileTextures = mapData.getTexturePaths();
    resourceService.loadTextures(tileTextures.toArray(new String[0]));
    resourceService.loadTextures(entityTextures);
    resourceService.loadTextureAtlases(entityAtlases);
    resourceService.loadSounds(entitySounds);
    resourceService.loadMusic(ENTITY_MUSIC);

    while (!resourceService.loadForMillis(10)) {
      logger.info("Loading... {}%", resourceService.getProgress());
    }
  }

  private void unloadAssets() {
    logger.debug("Unloading level assets");
    ResourceService resourceService = ServiceLocator.getResourceService();
    if (mapData != null) {
      resourceService.unloadAssets(mapData.getTexturePaths().toArray(new String[0]));
    }
    resourceService.unloadAssets(entityTextures);
    resourceService.unloadAssets(entityAtlases);
    resourceService.unloadAssets(entitySounds);
    resourceService.unloadAssets(ENTITY_MUSIC);
  }

  @Override
  public void dispose() {
    super.dispose();
    ServiceLocator.getResourceService().getAsset(BACKGROUND_MUSIC, Music.class).stop();
    unloadAssets();
  }
}
