package com.csse3200.game.areas;

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
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.events.listeners.EventListener2;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
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

  /** Entity textures needed by the player and demo enemies. */
  private static final String[] entityTextures = {
    "images/box_boy_leaf.png", "images/ghost_king.png", "images/ghost_1.png", "images/sword.png"
  };

  private static final String[] entitySounds = {"sounds/Impact4.ogg"};

  private static final String[] entityAtlases = {"images/ghost.atlas", "images/ghostKing.atlas"};

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

    // tileToWorldPosition gives the bottom-left corner of the tile.
    // Entity positions represent the centre of the entity.
    Vector2 position = terrain.tileToWorldPosition(x, y).add(tileSize / 2f, tileSize / 2f);

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
    switch (type.toLowerCase()) {
      case "ghost":
        return NPCFactory.createGhost(player);
      case "ghostking":
      case "ghost_king":
        return NPCFactory.createGhostKing(player);
      default:
        logger.warn("Unknown enemy spawn type '{}' - skipped", type);
        return null;
    }
  }

  private void loadAssets() {
    logger.debug("Loading level assets");
    ResourceService resourceService = ServiceLocator.getResourceService();

    Set<String> tileTextures = mapData.getTexturePaths();
    resourceService.loadTextures(tileTextures.toArray(new String[0]));
    resourceService.loadTextures(entityTextures);
    resourceService.loadTextureAtlases(entityAtlases);
    resourceService.loadSounds(entitySounds);

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
  }

  @Override
  public void dispose() {
    super.dispose();
    unloadAssets();
  }
}
