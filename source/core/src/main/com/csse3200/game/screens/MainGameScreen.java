package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.areas.LevelGameArea;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.areas.terrain.map.RoomTransition;
import com.csse3200.game.components.gamearea.PerformanceDisplay;
import com.csse3200.game.components.gamearea.SubLevelTitleDisplay;
import com.csse3200.game.components.gamearea.SubLevelTravelPromptDisplay;
import com.csse3200.game.components.maingame.DeathScreenDisplay;
import com.csse3200.game.components.maingame.DeathScreenInputComponent;
import com.csse3200.game.components.maingame.MainGameActions;
import com.csse3200.game.components.maingame.WinScreenDisplay;
import com.csse3200.game.components.maingame.WinScreenInputComponent;
import com.csse3200.game.components.player.ShopDisplay;
import com.csse3200.game.components.player.SubLevelTravelComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.files.LoadService;
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
import com.csse3200.game.ui.terminal.commands.UpgradesCommand;
import com.csse3200.game.ui.terminal.commands.WinCommand;
import com.csse3200.game.upgrades.ActiveUpgradesHud;
import com.csse3200.game.upgrades.UpgradesDisplay;
import com.csse3200.game.upgrades.UpgradesMenuComponent;
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
    "images/ui/heart.png",
    "images/ui/heart-empty.png",
    "images/ui/heart-green-half.png",
    "images/ui/heart-yellow-half.png",
    "images/ui/heart-red-half.png",
    "images/ui/heart-green.png",
    "images/ui/heart-yellow.png"
  };
  private static final Vector2 CAMERA_POSITION = new Vector2(7.5f, 7.5f);
  private static final String FIRST_ROOM_MAP = "maps/level1-greek.json";
  private static final String SECOND_ROOM_MAP = "maps/level2.json";
  private static final float GAMEPLAY_ZOOM = 0.95f;

  /** The crust seam in the 56x64 Greek map (32 tiles at 0.5 world units). */
  private static final float SUB_LEVEL_BOUNDARY = 16f;

  private final GdxGame game;
  private final Renderer renderer;
  private final PhysicsEngine physicsEngine;
  private LevelGameArea levelGameArea;
  private String currentRoomMapPath = FIRST_ROOM_MAP;
  private DeathScreenDisplay deathScreenDisplay;
  private WinScreenDisplay winScreenDisplay;
  private UpgradesDisplay upgradesDisplay;
  private boolean deathScreenShown = false;
  private Boolean playerInNether;
  private PauseMenuComponent pauseMenu;
  private final TerrainFactory terrainFactory;
  private Entity subLevelTravelPromptEntity;

  public MainGameScreen(GdxGame game, boolean loadsave) {
    this.game = game;

    Gdx.gl.glClearColor(0f, 0f, 0f, 1f);

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
    Entity player = levelGameArea.getPlayer();
    upgradesDisplay.setPlayer(player);
    ServiceLocator.getEntityService()
        .register(new Entity().addComponent(new SubLevelTitleDisplay(player)));
    createSubLevelTravelPrompt(player);

    ShopDisplay shopDisplay = player.getComponent(ShopDisplay.class);
    if (shopDisplay != null) {
      shopDisplay.setUpgradesDisplay(upgradesDisplay);
    }

    if (loadsave) {
      LoadService.load(
          levelGameArea.getPlayer(),
          levelGameArea.getMapWorldWidth(),
          levelGameArea.getMapWorldHeight());
    }
    fitCameraToMap(levelGameArea);
  }

  public Entity getPlayerEntity() {
    return levelGameArea != null ? levelGameArea.getPlayer() : null;
  }

  /**
   * Set an approachable gameplay view that is close enough to read platforms and hazards without
   * hiding the neighbouring routes that guide exploration. Small maps still use the smaller
   * whole-map zoom when necessary.
   *
   * @param area the level area whose map the camera should frame
   */
  private void fitCameraToMap(LevelGameArea area) {
    OrthographicCamera cam = (OrthographicCamera) renderer.getCamera().getCamera();
    float zoomForWholeMap =
        Math.min(
            area.getMapWorldWidth() / cam.viewportWidth,
            area.getMapWorldHeight() / cam.viewportHeight);
    cam.zoom = Math.min(GAMEPLAY_ZOOM, zoomForWholeMap);
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
    Vector2 playerPosition = player.getPosition();
    boolean inNether = player.getCenterPosition().y >= SUB_LEVEL_BOUNDARY;
    SubLevelTravelComponent travel = player.getComponent(SubLevelTravelComponent.class);
    if (playerInNether != null
        && playerInNether != inNether
        && (travel == null || !travel.isControlLocked())) {
      String title = subLevelTitle(currentRoomMapPath, inNether);
      if (title != null) {
        player.getEvents().trigger("subLevelEntered", title);
      }
    }
    playerInNether = inNether;
    float subLevelBottom = inNether ? SUB_LEVEL_BOUNDARY : 0f;
    float subLevelHeight =
        inNether ? levelGameArea.getMapWorldHeight() - SUB_LEVEL_BOUNDARY : SUB_LEVEL_BOUNDARY;

    float x = clampToMap(playerPosition.x, halfViewWidth, mapWidth);
    float y = clampToRange(playerPosition.y, halfViewHeight, subLevelBottom, subLevelHeight);

    renderer.getCamera().getEntity().setPosition(x, y);
  }

  /** Selects a crossing title for the current room without relabelling other rooms. */
  static String subLevelTitle(String mapPath, boolean upperSection) {
    if (FIRST_ROOM_MAP.equals(mapPath)) {
      return upperSection ? "NETHER" : "DUNGEON";
    }
    if (SECOND_ROOM_MAP.equals(mapPath)) {
      return upperSection ? "SKIES" : "BASE";
    }
    return null;
  }

  /**
   * Clamps a camera coordinate so the visible view stays within [0, mapSize] along one axis. When
   * the view is wider than the map itself, the map is centred instead.
   */
  private static float clampToMap(float value, float halfViewSize, float mapSize) {
    if (halfViewSize * 2f >= mapSize) {
      return mapSize / 2f;
    }
    return Math.clamp(value, halfViewSize, mapSize - halfViewSize);
  }

  private static float clampToRange(float value, float halfViewSize, float bottom, float height) {
    if (halfViewSize * 2f >= height) {
      return bottom + height / 2f;
    }
    return Math.clamp(value, bottom + halfViewSize, bottom + height - halfViewSize);
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
    removeSubLevelTravelPrompt();
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
    currentRoomMapPath = transition.getDestinationMap();
    playerInNether = null;
    player.getEvents().trigger("subLevelEntered", nextArea.getMapData().getName());
    if (FIRST_ROOM_MAP.equals(transition.getDestinationMap())) {
      createSubLevelTravelPrompt(player);
    }
    fitCameraToMap(nextArea);
  }

  private void createSubLevelTravelPrompt(Entity player) {
    subLevelTravelPromptEntity =
        new Entity()
            .addComponent(
                new SubLevelTravelPromptDisplay(player, renderer.getCamera().getCamera()));
    ServiceLocator.getEntityService().register(subLevelTravelPromptEntity);
  }

  private void removeSubLevelTravelPrompt() {
    if (subLevelTravelPromptEntity != null) {
      subLevelTravelPromptEntity.dispose();
      subLevelTravelPromptEntity = null;
    }
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
    winScreenDisplay = new WinScreenDisplay(this.game);

    Terminal terminal = new Terminal();
    terminal.addCommand("win", new WinCommand(winScreenDisplay));
    PauseMenuComponent pauseMenuComponent = new PauseMenuComponent();
    UpgradesMenuComponent upgradesMenuComponent = new UpgradesMenuComponent();
    upgradesDisplay = new UpgradesDisplay();
    ui.addComponent(new InputDecorator(stage, 10))
        .addComponent(new PerformanceDisplay())
        .addComponent(terminal)
        .addComponent(inputComponent)
        .addComponent(new TerminalDisplay())
        .addComponent(pauseMenuComponent)
        .addComponent(new KeyboardPauseInput())
        .addComponent(new PauseMenuDisplay())
        .addComponent(new PauseMenuActions(this::getPlayerEntity))
        .addComponent(new PauseMenuInputComponent())
        .addComponent(deathScreenDisplay)
        .addComponent(new DeathScreenInputComponent())
        .addComponent(winScreenDisplay)
        .addComponent(new WinScreenInputComponent())
        .addComponent(new MainGameActions(this.game))
        .addComponent(upgradesMenuComponent)
        .addComponent(upgradesDisplay)
        .addComponent(new ActiveUpgradesHud());
    this.pauseMenu = pauseMenuComponent;
    terminal.addCommand("upgrades", new UpgradesCommand(upgradesMenuComponent));

    ServiceLocator.getEntityService().register(ui);
  }
}
