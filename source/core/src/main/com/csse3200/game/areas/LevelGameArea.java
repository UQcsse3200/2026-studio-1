package com.csse3200.game.areas;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.areas.terrain.CollisionType;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.areas.terrain.map.JsonMapLoader;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.MapLayerData;
import com.csse3200.game.areas.terrain.map.MapLoader;
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
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.LootFactory;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.events.listeners.EventListener2;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
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

  private LevelMapData mapData;
  private Entity player;

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
    super();
    this.terrainFactory = terrainFactory;
    this.mapPath = mapPath;
    this.mapLoader = mapLoader;
  }

  @Override
  public void create() {
    mapData = mapLoader.load(mapPath);
    loadAssets();

    displayUI();
    spawnTerrain();
    spawnCollisions();
    player = spawnPlayer();
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
   * Build a static collider for every solid tile in the map's collision layer so the player and
   * other physics bodies rest on floors and platforms instead of falling through. Reads the
   * collision layer ({@link LevelMapData#getCollisionLayer()}), not layer 0, so the visual
   * background layer never produces colliders.
   */
  private void spawnCollisions() {
    MapLayerData collisionLayer = mapData.getCollisionLayer();

    if (collisionLayer == null) {
      return;
    }

    float tileSize = terrain.getTileSize();

    for (int x = 0; x < collisionLayer.getWidth(); x++) {
      for (int y = 0; y < collisionLayer.getHeight(); y++) {
        TileDefinition def = collisionLayer.get(x, y);

        if (def == null) {
          continue;
        }

        CollisionType collisionType = def.type().getCollisionType();

        spawnCollisionTile(collisionType, x, y, tileSize);
      }
    }
  }

  /**
   * Spawns the collision type at the x y coordinates from the relevant ObstacleFactory method
   *
   * @param collisionType the type of collision to spawn
   * @param x the x coordinate of the collision
   * @param y the y coordinate of the collision
   * @param tileSize the width/height of the tile
   */
  private void spawnCollisionTile(CollisionType collisionType, int x, int y, float tileSize) {

    Entity collider;

    switch (collisionType) {
      case SOLID, PLATFORM:
        collider = ObstacleFactory.createFloorTile(tileSize, COLLIDER_HEIGHT);
        break;

      case HAZARD:
        collider = ObstacleFactory.createHazardTile(tileSize, tileSize);
        break;

      case NONE:
      default:
        return;
    }

    // tileToWorldPosition gives the bottom-left corner of the tile (null for unsupported
    // orientations). Entity positions represent the centre of the entity.
    Vector2 position = terrain.tileToWorldPosition(x, y);
    if (position == null) {
      return;
    }
    position.add(tileSize / 2f, tileSize / 2f);

    collider.setPosition(position);
    spawnEntity(collider);
  }

  private Entity spawnPlayer() {
    Entity newPlayer = PlayerFactory.createPlayer();

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
                    CombatStatsComponent stats = newPlayer.getComponent(CombatStatsComponent.class);

                    stats.addHealth(-10);

                    logger.info("Player hit hazard! Health: {}", stats.getHealth());
                  }
                });

    GridPoint2 spawn = mapData.getSpawns().getPlayer();

    if (spawn == null) {
      logger.warn("Map '{}' has no player spawn; defaulting to (0, 0)", mapData.getName());
      spawn = new GridPoint2(0, 0);
    }

    spawnEntityAt(newPlayer, spawn, true, true);
    return newPlayer;
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
