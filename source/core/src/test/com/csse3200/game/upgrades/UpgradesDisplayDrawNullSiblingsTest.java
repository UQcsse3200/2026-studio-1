package com.csse3200.game.upgrades;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Covers draw()'s null-sibling guards that UpgradesDisplayPauseDeathGatingTest doesn't exercise -
 * that file always wires a PauseMenuComponent sibling, an UpgradesMenuComponent sibling, and a
 * player with a DeathStateComponent. Kept in its own file (same reason EnduranceUpgradeTest/
 * RegenOnKillDifficultyTest are split out) since each test here needs different, deliberately
 * incomplete entity wiring.
 */
@ExtendWith(GameExtension.class)
class UpgradesDisplayDrawNullSiblingsTest {

  @BeforeEach
  void beforeEach() {
    RenderService renderService = new RenderService();
    renderService.setStage(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    ServiceLocator.registerTimeSource(new GameTime());

    Gdx.graphics = mock(Graphics.class);
    when(Gdx.graphics.getDeltaTime()).thenReturn(1f); // deterministic 1s-per-draw() delta
  }

  @SuppressWarnings("unchecked")
  private List<UpgradeNode> getMovementUpgrades(UpgradesDisplay display) throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("movementUpgrades");
    field.setAccessible(true);
    return (List<UpgradeNode>) field.get(display);
  }

  private float getRemainingSeconds(UpgradeNode node) throws Exception {
    Field field = UpgradeNode.class.getDeclaredField("remainingSeconds");
    field.setAccessible(true);
    return (float) field.get(node);
  }

  /** draw() still ticks normally when this entity has no sibling PauseMenuComponent at all. */
  @Test
  void tickingProceedsNormallyWithNoPauseMenuSibling() throws Exception {
    UpgradesDisplay display = new UpgradesDisplay();
    new Entity().addComponent(new UpgradesMenuComponent()).addComponent(display).create();

    Entity player = new Entity().addComponent(new PlayerActions());
    display.setPlayer(player);

    UpgradeNode playerSpeed = getMovementUpgrades(display).get(0);
    playerSpeed.purchaseNextTier(); // 10s remaining

    assertDoesNotThrow(() -> display.draw(null));

    assertEquals(9f, getRemainingSeconds(playerSpeed), 0.0001f); // ticked, not frozen
  }

  /** draw() does not throw and still ticks when no player has ever been set. */
  @Test
  void tickingProceedsNormallyWithNoPlayerSet() throws Exception {
    UpgradesDisplay display = new UpgradesDisplay();
    new Entity().addComponent(new UpgradesMenuComponent()).addComponent(display).create();

    UpgradeNode playerSpeed = getMovementUpgrades(display).get(0);
    playerSpeed.purchaseNextTier();

    assertDoesNotThrow(() -> display.draw(null));

    assertEquals(9f, getRemainingSeconds(playerSpeed), 0.0001f);
  }

  /** draw() treats a player with no DeathStateComponent as alive, so ticking still proceeds. */
  @Test
  void tickingProceedsNormallyWhenPlayerHasNoDeathStateComponent() throws Exception {
    UpgradesDisplay display = new UpgradesDisplay();
    new Entity().addComponent(new UpgradesMenuComponent()).addComponent(display).create();

    Entity player = new Entity().addComponent(new CombatStatsComponent(100, 10));
    display.setPlayer(player);

    UpgradeNode playerSpeed = getMovementUpgrades(display).get(0);
    playerSpeed.purchaseNextTier();

    assertDoesNotThrow(() -> display.draw(null));

    assertEquals(9f, getRemainingSeconds(playerSpeed), 0.0001f);
  }

  /** With no UpgradesMenuComponent sibling at all, draw() still runs and leaves root hidden. */
  @Test
  void rootStaysHiddenWithNoUpgradesMenuSibling() throws Exception {
    UpgradesDisplay display = new UpgradesDisplay();
    new Entity().addComponent(display).create(); // deliberately no UpgradesMenuComponent

    assertDoesNotThrow(() -> display.draw(null));

    Field rootField = UpgradesDisplay.class.getDeclaredField("root");
    rootField.setAccessible(true);
    Table root = (Table) rootField.get(display);
    assertFalse(root.isVisible());
  }

  /**
   * While the menu is open with a node selected, draw() keeps refreshing that node's detail text.
   */
  @Test
  void drawKeepsTheSelectedNodesDetailTextLiveWhileTheMenuIsOpen() throws Exception {
    UpgradesDisplay display = new UpgradesDisplay();
    UpgradesMenuComponent menu = new UpgradesMenuComponent();
    new Entity().addComponent(menu).addComponent(display).create();
    menu.toggleIsOpen();

    UpgradeNode playerSpeed = getMovementUpgrades(display).get(0);
    playerSpeed.purchaseNextTier(); // 10s remaining

    Field selectedField = UpgradesDisplay.class.getDeclaredField("selectedNode");
    selectedField.setAccessible(true);
    selectedField.set(display, playerSpeed);

    display.draw(null); // ticks to 9s and refreshes the status label to match

    Field statusField = UpgradesDisplay.class.getDeclaredField("detailStatusLabel");
    statusField.setAccessible(true);
    Label statusLabel = (Label) statusField.get(display);

    assertEquals("Active - " + playerSpeed.getRemainingText(), statusLabel.getText().toString());
  }

  /** While the menu is open with no node selected, draw() must not try to refresh any detail. */
  @Test
  void drawDoesNotShowDetailWhileTheMenuIsOpenButNothingIsSelected() throws Exception {
    UpgradesDisplay display = new UpgradesDisplay();
    UpgradesMenuComponent menu = new UpgradesMenuComponent();
    new Entity().addComponent(menu).addComponent(display).create();
    menu.toggleIsOpen(); // open, but nothing has ever been selected - selectedNode stays null

    assertDoesNotThrow(() -> display.draw(null));

    Field statusField = UpgradesDisplay.class.getDeclaredField("detailStatusLabel");
    statusField.setAccessible(true);
    Label statusLabel = (Label) statusField.get(display);
    assertEquals("", statusLabel.getText().toString());
  }
}
