package com.csse3200.game.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.DeathStateComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.pausemenu.PauseMenuComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Confirms UpgradesDisplay.draw() skips ticking active upgrades' countdowns entirely - never
 * touching remainingSeconds/remainingKills or currentTier - whenever the game is paused or the
 * player is dead, reading the same PauseMenuComponent/DeathStateComponent state the rest of the
 * game already uses (a sibling component on this entity, and a component on the player entity
 * respectively) rather than any separate tracking, and resumes exactly where it froze once both
 * conditions clear.
 *
 * <p>Gdx.graphics is mocked with a fixed 1s-per-frame delta (the same technique GameExtension
 * already uses for Gdx.gl) so every tick amount here is exact and deterministic, rather than
 * depending on whatever real wall-clock delta the headless test harness happens to report.
 */
@ExtendWith(GameExtension.class)
class UpgradesDisplayPauseDeathGatingTest {
  private UpgradesDisplay display;
  private PauseMenuComponent pauseMenu;
  private Entity player;

  @BeforeEach
  void beforeEach() {
    RenderService renderService = new RenderService();
    renderService.setStage(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));

    Gdx.graphics = mock(Graphics.class);
    when(Gdx.graphics.getDeltaTime()).thenReturn(1f); // deterministic 1s-per-draw() delta

    display = new UpgradesDisplay();
    pauseMenu = new PauseMenuComponent();
    new Entity()
        .addComponent(new UpgradesMenuComponent())
        .addComponent(pauseMenu) // sibling component, exactly as in MainGameScreen's "ui" entity
        .addComponent(display)
        .create();

    player = new Entity()
        .addComponent(new CombatStatsComponent(100, 10))
        .addComponent(new PlayerActions())
        .addComponent(new DeathStateComponent());
    display.setPlayer(player);
  }

  @SuppressWarnings("unchecked")
  private List<UpgradeNode> getMovementUpgrades() throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("movementUpgrades");
    field.setAccessible(true);
    return (List<UpgradeNode>) field.get(display);
  }

  private float getRemainingSeconds(UpgradeNode node) throws Exception {
    Field field = UpgradeNode.class.getDeclaredField("remainingSeconds");
    field.setAccessible(true);
    return (float) field.get(node);
  }

  private void markPlayerDead() throws Exception {
    DeathStateComponent deathState = player.getComponent(DeathStateComponent.class);
    Field field = DeathStateComponent.class.getDeclaredField("dead");
    field.setAccessible(true);
    field.set(deathState, true);
  }

  @Test
  void tickingProceedsNormallyWhenNeitherPausedNorDead() throws Exception {
    UpgradeNode playerSpeed = getMovementUpgrades().get(0); // "player_speed"
    playerSpeed.purchaseNextTier(); // 0 + 10 = 10s remaining

    display.draw(null); // 1s stubbed delta -> 9s remaining

    assertEquals(9f, getRemainingSeconds(playerSpeed), 0.0001f);
  }

  @Test
  void tickingIsSkippedWhilePaused() throws Exception {
    UpgradeNode playerSpeed = getMovementUpgrades().get(0);
    playerSpeed.purchaseNextTier(); // 10s remaining

    pauseMenu.toggleIsPaused();
    display.draw(null);
    display.draw(null); // several frozen frames - should never move at all

    assertEquals(10f, getRemainingSeconds(playerSpeed), 0.0001f);
    assertEquals(1, playerSpeed.getCurrentTier()); // tier untouched by pause
  }

  @Test
  void tickingIsSkippedWhilePlayerIsDead() throws Exception {
    UpgradeNode playerSpeed = getMovementUpgrades().get(0);
    playerSpeed.purchaseNextTier(); // 10s remaining
    markPlayerDead();

    display.draw(null);
    display.draw(null);

    assertEquals(10f, getRemainingSeconds(playerSpeed), 0.0001f);
    assertEquals(1, playerSpeed.getCurrentTier()); // tier untouched by death
  }

  @Test
  void tickingIsSkippedWhenBothPausedAndDead() throws Exception {
    UpgradeNode playerSpeed = getMovementUpgrades().get(0);
    playerSpeed.purchaseNextTier(); // 10s remaining
    markPlayerDead();
    pauseMenu.toggleIsPaused();

    display.draw(null);

    assertEquals(10f, getRemainingSeconds(playerSpeed), 0.0001f);
  }

  @Test
  void tickingResumesFromTheExactFrozenValueOncePauseClears() throws Exception {
    UpgradeNode playerSpeed = getMovementUpgrades().get(0);
    playerSpeed.purchaseNextTier(); // 10s remaining

    display.draw(null); // ticks normally: 10 - 1 = 9s remaining
    assertEquals(9f, getRemainingSeconds(playerSpeed), 0.0001f);

    pauseMenu.toggleIsPaused();
    display.draw(null);
    display.draw(null);
    display.draw(null); // several frozen frames while paused
    assertEquals(9f, getRemainingSeconds(playerSpeed), 0.0001f); // still exactly 9s - untouched

    pauseMenu.toggleIsPaused(); // unpause
    display.draw(null); // resumes from 9s, ticks the stubbed 1s -> 8s (not reset to 10s/anything else)

    assertEquals(8f, getRemainingSeconds(playerSpeed), 0.0001f);
    assertEquals(1, playerSpeed.getCurrentTier());
  }

  @Test
  void tickingResumesFromTheExactFrozenValueOnceNoLongerDead() throws Exception {
    UpgradeNode playerSpeed = getMovementUpgrades().get(0);
    playerSpeed.purchaseNextTier(); // 10s remaining

    display.draw(null); // 9s remaining
    markPlayerDead();
    display.draw(null);
    display.draw(null); // frozen while dead
    assertEquals(9f, getRemainingSeconds(playerSpeed), 0.0001f);

    // Player "revives" - directly clear the same dead flag DeathStateComponent itself exposes,
    // rather than a separate/duplicate death flag of our own.
    Field deadField = DeathStateComponent.class.getDeclaredField("dead");
    deadField.setAccessible(true);
    deadField.set(player.getComponent(DeathStateComponent.class), false);

    display.draw(null); // resumes from 9s -> 8s

    assertEquals(8f, getRemainingSeconds(playerSpeed), 0.0001f);
  }
}
