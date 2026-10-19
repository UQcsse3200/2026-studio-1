package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.areas.LevelGameArea;
import com.csse3200.game.areas.terrain.map.JsonMapLoader;
import com.csse3200.game.areas.terrain.map.LevelView;
import com.csse3200.game.areas.terrain.map.MapDataLevelView;
import com.csse3200.game.areas.terrain.map.SubLevel;
import com.csse3200.game.components.player.StaminaComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.input.InputFactory;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.Terminal;
import com.csse3200.game.ui.terminal.commands.WhereCommand;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class MainGameScreenTest {
  private final JsonMapLoader loader = new JsonMapLoader();

  private LevelView level(String mapPath) {
    return new MapDataLevelView(loader.load(mapPath));
  }

  @Test
  void usesMountainTitlesForLevelTwo() {
    LevelView levelTwo = level("maps/level2.json");

    assertEquals("BASE", MainGameScreen.subLevelTitle(levelTwo, false));
    assertEquals("SKIES", MainGameScreen.subLevelTitle(levelTwo, true));
  }

  @Test
  void preservesUnderworldTitlesForLevelOne() {
    LevelView levelOne = level("maps/level1-greek.json");

    assertEquals("DUNGEON", MainGameScreen.subLevelTitle(levelOne, false));
    assertEquals("NETHER", MainGameScreen.subLevelTitle(levelOne, true));
  }

  @Test
  void hasNoTitlesForALevelThatIsOnePlace() {
    LevelView levelThree = level("maps/level3.json");

    assertNull(MainGameScreen.subLevelTitle(levelThree, false));
    assertNull(MainGameScreen.subLevelTitle(levelThree, true));
  }

  @Test
  void hasNoTitlesWithoutALevel() {
    assertNull(MainGameScreen.subLevelTitle(null, false));
  }

  @Test
  void whereLocationIsUnavailableWithoutAnAreaPlayerOrLevel() {
    LevelGameArea area = mock(LevelGameArea.class);
    String mapPath = "maps/level1-greek.json";

    assertNull(MainGameScreen.whereLocation(null, mapPath));
    assertNull(MainGameScreen.whereLocation(area, mapPath));

    Entity player = mock(Entity.class);
    when(area.getPlayer()).thenReturn(player);
    assertNull(MainGameScreen.whereLocation(area, mapPath));
  }

  @Test
  void whereLocationUsesTheCurrentTileAndSublevel() {
    LevelGameArea area = mock(LevelGameArea.class);
    Entity player = mock(Entity.class);
    LevelView level = mock(LevelView.class);
    SubLevel nether =
        new SubLevel("nether", "NETHER", new SubLevel.Bounds(0, 0, 56, 32), null, null, null);
    when(area.getPlayer()).thenReturn(player);
    when(area.getLevel()).thenReturn(level);
    when(player.getCenterPosition()).thenReturn(new Vector2(3.9f, 9.2f));
    when(level.tileSize()).thenReturn(0.5f);
    when(level.subLevelAt(18)).thenReturn(nether);
    when(level.name()).thenReturn("Level 1 - Out of the Underworld");

    assertEquals(
        new WhereCommand.Location(
            "Level 1 - Out of the Underworld", "maps/level1-greek.json", "nether", 7, 18),
        MainGameScreen.whereLocation(area, "maps/level1-greek.json"));
  }

  @Test
  void whereLocationReportsNoneOutsideSublevelsAndFloorsNegativeCoordinates() {
    LevelGameArea area = mock(LevelGameArea.class);
    Entity player = mock(Entity.class);
    LevelView level = mock(LevelView.class);
    when(area.getPlayer()).thenReturn(player);
    when(area.getLevel()).thenReturn(level);
    when(player.getCenterPosition()).thenReturn(new Vector2(-0.1f, 2.99f));
    when(level.tileSize()).thenReturn(1f);
    when(level.name()).thenReturn("Level 2 - Mount Olympus");

    assertEquals(
        new WhereCommand.Location("Level 2 - Mount Olympus", "maps/level2.json", "none", -1, 2),
        MainGameScreen.whereLocation(area, "maps/level2.json"));
  }

  @Test
  void staminaToggleIsSafeWithoutAPlayerOrStaminaComponent() {
    assertDoesNotThrow(() -> MainGameScreen.applyUnlimitedStamina(null, true));

    Entity player = mock(Entity.class);
    assertDoesNotThrow(() -> MainGameScreen.applyUnlimitedStamina(player, true));
    verify(player).getComponent(StaminaComponent.class);
  }

  @Test
  void staminaToggleForwardsBothModesToTheCurrentPlayer() {
    Entity player = mock(Entity.class);
    StaminaComponent stamina = mock(StaminaComponent.class);
    when(player.getComponent(StaminaComponent.class)).thenReturn(stamina);

    MainGameScreen.applyUnlimitedStamina(player, true);
    MainGameScreen.applyUnlimitedStamina(player, false);

    verify(stamina).setUnlimitedStamina(true);
    verify(stamina).setUnlimitedStamina(false);
  }

  @Test
  void registeredStaminaCommandUsesTheCurrentPlayer() {
    Terminal terminal = new Terminal();
    AtomicReference<Entity> currentPlayer = new AtomicReference<>();
    MainGameScreen.registerDeveloperCommands(
        terminal, currentPlayer::get, () -> null, () -> "maps/level1-greek.json");

    Entity firstPlayer = mock(Entity.class);
    StaminaComponent firstStamina = mock(StaminaComponent.class);
    when(firstPlayer.getComponent(StaminaComponent.class)).thenReturn(firstStamina);
    currentPlayer.set(firstPlayer);
    terminal.setEnteredMessage("stamina on");
    assertTrue(terminal.processMessage());
    verify(firstStamina).setUnlimitedStamina(true);

    Entity nextPlayer = mock(Entity.class);
    StaminaComponent nextStamina = mock(StaminaComponent.class);
    when(nextPlayer.getComponent(StaminaComponent.class)).thenReturn(nextStamina);
    currentPlayer.set(nextPlayer);
    terminal.setEnteredMessage("stamina off");
    assertTrue(terminal.processMessage());
    verify(nextStamina).setUnlimitedStamina(false);
  }

  @Test
  void registeredWhereCommandUsesTheCurrentRoomAndMapPath() {
    Terminal terminal = new Terminal();
    AtomicReference<LevelGameArea> currentArea = new AtomicReference<>();
    AtomicReference<String> currentMap = new AtomicReference<>("maps/level1-greek.json");
    MainGameScreen.registerDeveloperCommands(
        terminal, () -> null, currentArea::get, currentMap::get);

    terminal.setEnteredMessage("where");
    assertFalse(terminal.processMessage());

    LevelGameArea nextArea = mock(LevelGameArea.class);
    Entity player = mock(Entity.class);
    LevelView level = mock(LevelView.class);
    when(nextArea.getPlayer()).thenReturn(player);
    when(nextArea.getLevel()).thenReturn(level);
    when(player.getCenterPosition()).thenReturn(new Vector2(4.2f, 3.8f));
    when(level.tileSize()).thenReturn(1f);
    when(level.name()).thenReturn("Level 2 - Mount Olympus");
    currentArea.set(nextArea);
    currentMap.set("maps/level2.json");

    terminal.setEnteredMessage("where");
    assertTrue(terminal.processMessage());
    assertEquals(
        "Room: Level 2 - Mount Olympus\nMap: maps/level2.json\nSublevel: none\nTile: (4, 3)",
        terminal.getOutputMessage().replace(System.lineSeparator(), "\n"));
  }

  @Test
  void registeredHordeCommandUsesTheCurrentRoom() {
    Terminal terminal = new Terminal();
    AtomicReference<LevelGameArea> currentArea = new AtomicReference<>();
    MainGameScreen.registerDeveloperCommands(
        terminal, () -> null, currentArea::get, () -> "maps/level1-greek.json");

    terminal.setEnteredMessage("horde 1");
    assertFalse(terminal.processMessage());

    LevelGameArea area = mock(LevelGameArea.class);
    Entity player = mock(Entity.class);
    LevelView level = mock(LevelView.class);
    when(area.getPlayer()).thenReturn(player);
    when(area.getLevel()).thenReturn(level);
    when(player.getCenterPosition()).thenReturn(new Vector2(10.5f, 5.5f));
    when(level.tileSize()).thenReturn(1f);
    when(level.groundTiles()).thenReturn(List.of(new GridPoint2(8, 5)));
    when(level.isWalkable(8, 5)).thenReturn(true);
    when(level.isWalkable(8, 6)).thenReturn(true);
    when(level.inBounds(8, 6)).thenReturn(true);
    when(area.spawnEnemy(eq("skeleton"), any(Vector2.class))).thenReturn(true);
    currentArea.set(area);

    terminal.setEnteredMessage("horde 1");
    assertTrue(terminal.processMessage());
    verify(area).spawnEnemy("skeleton", new Vector2(8f, 5f));
    assertEquals("Spawned 1 of 1 skeletons.", terminal.getOutputMessage());
  }

  @Test
  void gameUiRegistersTheDeveloperCommands() {
    RenderService renderService = mock(RenderService.class);
    Stage stage = mock(Stage.class);
    when(renderService.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderService);

    InputService inputService = mock(InputService.class);
    InputFactory inputFactory = mock(InputFactory.class);
    when(inputService.getInputFactory()).thenReturn(inputFactory);
    InputComponent terminalInput = mock(InputComponent.class);
    when(inputFactory.createForTerminal()).thenReturn(terminalInput);
    ServiceLocator.registerInputService(inputService);

    EntityService entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    MainGameScreen screen = mock(MainGameScreen.class, CALLS_REAL_METHODS);
    screen.createUI();

    ArgumentCaptor<Entity> ui = ArgumentCaptor.forClass(Entity.class);
    verify(entityService).register(ui.capture());
    Terminal terminal = ui.getValue().getComponent(Terminal.class);
    assertNotNull(terminal);

    terminal.setEnteredMessage("stamina on");
    assertTrue(terminal.processMessage());
    terminal.setEnteredMessage("where");
    assertFalse(terminal.processMessage());
    assertEquals("", terminal.getEnteredMessage());
    terminal.setEnteredMessage("horde 1");
    assertFalse(terminal.processMessage());
    assertEquals("", terminal.getEnteredMessage());
  }
}
