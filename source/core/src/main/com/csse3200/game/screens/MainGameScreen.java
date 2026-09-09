package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.areas.LevelGameArea;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.areas.terrain.map.RoomTransition;
import com.csse3200.game.components.gamearea.PerformanceDisplay;
import com.csse3200.game.components.maingame.DeathScreenDisplay;
import com.csse3200.game.components.maingame.MainGameActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.pausemenu.*;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.Terminal;
import com.csse3200.game.ui.terminal.TerminalDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The game screen containing the main game.
 *
 * <p>Details on libGDX screens: https://happycoding.io/tutorials/libgdx/game-screens
 */
public class MainGameScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(MainGameScreen.class);
  private static final String[] mainGameTextures = {
    "images/heart.png",
    "images/heart-empty.png",
    "images/heart-green-half.png",
    "images/heart-yellow-half.png",
    "images/heart-red-half.png",
    "images/heart-green.png",
    "images/heart-yellow.png"
  };
  private static final Vector2 CAMERA_POSITION = new Vector2(7.5f, 7.5f);
  private static final String FIRST_ROOM_MAP = "maps/level1.json";

  private final GdxGame game;
  private final Renderer renderer;
  private final PhysicsEngine physicsEngine;
  private LevelGameArea levelGameArea;
  private DeathScreenDisplay deathScreenDisplay;
  private boolean deathScreenShown = false;
  private PauseMenuComponent pauseMenu;
  private final TerrainFactory terrainFactory;

  public MainGameScreen(GdxGame game) {
    this.game = game;

    logger.debug("Initialising main game screen services");
    ServiceLocator.registerTimeSource(new GameTime());

    PhysicsService physicsService = new PhysicsService();
    ServiceLocator.registerPhysicsService(physicsService);
    physicsEngine = physicsService.getPhysics();

    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());

    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    renderer = RenderFactory.createRenderer();
    renderer.getCamera().getEntity().setPosition(CAMERA_POSITION);
    renderer.getDebug().renderPhysicsWorld(physicsEngine.getWorld());

    loadAssets();
    createUI();

    logger.debug("Initialising main game screen entities");
    terrainFactory = new TerrainFactory(renderer.getCamera());
    this.levelGameArea = new LevelGameArea(terrainFactory, FIRST_ROOM_MAP);
    levelGameArea.create();

    fitCameraToMap(levelGameArea);
  }

  /**
   * Zoom the camera so the map fills the window. Uses the smaller of the two axis zoom factors so
   * the map always covers the whole viewport (axes where the map is bigger than the viewport are
   * left free for {@link #followPlayer()} to scroll along).
   *
   * @param area the level area whose map the camera should frame
   */
  private void fitCameraToMap(LevelGameArea area) {
    OrthographicCamera cam = (OrthographicCamera) renderer.getCamera().getCamera();
    float zoomForWidth = area.getMapWorldWidth() / cam.viewportWidth;
    float zoomForHeight = area.getMapWorldHeight() / cam.viewportHeight;
    cam.zoom = Math.min(zoomForWidth, zoomForHeight);
    cam.update();

    followPlayer();
  }

  /**
   * Moves the camera to track the player, clamping so the view never scrolls past the map's edges
   * (left, right, top or bottom). If the map is smaller than the current viewport along an axis,
   * the camera is centred on that axis instead of following.
   */
  private void followPlayer() {
    Entity player = levelGameArea.getPlayer();
    if (player == null) {
      return;
    }

    OrthographicCamera cam = (OrthographicCamera) renderer.getCamera().getCamera();
    float halfViewWidth = (cam.viewportWidth * cam.zoom) / 2f;
    float halfViewHeight = (cam.viewportHeight * cam.zoom) / 2f;

    float mapWidth = levelGameArea.getMapWorldWidth();
    float mapHeight = levelGameArea.getMapWorldHeight();

    Vector2 playerPosition = player.getPosition();
    float x = clampToMap(playerPosition.x, halfViewWidth, mapWidth);
    float y = clampToMap(playerPosition.y, halfViewHeight, mapHeight);

    renderer.getCamera().getEntity().setPosition(x, y);
  }

  /**
   * Clamps a camera coordinate so the visible view stays within [0, mapSize] along one axis. When
   * the view is wider than the map itself, the map is centred instead.
   */
  private static float clampToMap(float value, float halfViewSize, float mapSize) {
    if (halfViewSize * 2f >= mapSize) {
      return mapSize / 2f;
    }
    return Math.max(halfViewSize, Math.min(value, mapSize - halfViewSize));
  }

  @Override
  public void render(float delta) {

    /* If the player has died, stop updating the game world,
    but keep rendering the game and death popup.*/
    if (deathScreenShown) {
      renderer.render();
      return;
    }

    if (pauseMenu == null
        || !pauseMenu
            .isPaused()) { // Only updates the game physics (movement and all) when game is not
      // pauesd
      physicsEngine.update();
      ServiceLocator.getEntityService().update();
    }
    if (levelGameArea.isPlayerDead()) {
      deathScreenShown = true;
      deathScreenDisplay.showDeathScreen();
      renderer.render();
      return;
    }

    RoomTransition transition = levelGameArea.consumePendingTransition();
    if (transition != null) {
      transitionTo(transition);
    }

    followPlayer();
    renderer.render();
  }

  private void transitionTo(RoomTransition transition) {
    logger.info(
        "Entering '{}' through transition '{}'",
        transition.getDestinationMap(),
        transition.getId());

    LevelGameArea previousArea = levelGameArea;
    Entity player = previousArea.releasePlayer();
    LevelGameArea nextArea =
        new LevelGameArea(
            terrainFactory,
            transition.getDestinationMap(),
            player,
            transition.getDestinationSpawn());
    nextArea.create();

    previousArea.dispose();
    nextArea.resumeMusic();
    levelGameArea = nextArea;
    fitCameraToMap(nextArea);
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
    logger.trace("Resized renderer: ({} x {})", width, height);
  }

  @Override
  public void pause() {
    logger.info("Game paused");
  }

  @Override
  public void resume() {
    logger.info("Game resumed");
  }

  @Override
  public void dispose() {
    logger.debug("Disposing main game screen");

    // Dispose components while their services and physics world are still alive. The entity
    // service owns all active room and UI entities at screen shutdown; the resource service then
    // releases every loaded asset once. Calling LevelGameArea.dispose()/unloadAssets() here as
    // well would perform a second, overlapping cleanup pass.
    ServiceLocator.getEntityService().dispose();
    physicsEngine.dispose();
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getResourceService().dispose();

    ServiceLocator.clear();
  }

  private void loadAssets() {
    logger.debug("Loading assets");
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(mainGameTextures);
    ServiceLocator.getResourceService().loadAll();
  }

  /**
   * Creates the main game's ui including components for rendering ui elements to the screen and
   * capturing and handling ui input.
   */
  private void createUI() {
    logger.debug("Creating ui");
    Stage stage = ServiceLocator.getRenderService().getStage();
    InputComponent inputComponent =
        ServiceLocator.getInputService().getInputFactory().createForTerminal();

    Entity ui = new Entity();
    deathScreenDisplay = new DeathScreenDisplay(this.game);
    PauseMenuComponent pauseMenuComponent = new PauseMenuComponent();
    ui.addComponent(new InputDecorator(stage, 10))
        .addComponent(new PerformanceDisplay())
        .addComponent(new Terminal())
        .addComponent(inputComponent)
        .addComponent(new TerminalDisplay())
        .addComponent(pauseMenuComponent)
        .addComponent(new KeyboardPauseInput())
        .addComponent(new PauseMenuDisplay())
        .addComponent(new PauseMenuInputComponent())
        .addComponent(deathScreenDisplay)
        .addComponent(new MainGameActions(this.game))
        .addComponent(new PauseMenuActions());
    this.pauseMenu = pauseMenuComponent;

    ServiceLocator.getEntityService().register(ui);
  }
}
