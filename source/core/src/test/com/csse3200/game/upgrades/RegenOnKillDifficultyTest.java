package com.csse3200.game.upgrades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.ConsumableUseComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.difficulty.Difficulty;
import com.csse3200.game.difficulty.DifficultyService;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Regression coverage for Difficulty.getRegenHealMultiplier(): UpgradesDisplay.onEnemyKilled()
 * scales REGEN_HEAL_PER_KILL_PER_TIER by the current difficulty's regen multiplier (EASY/NORMAL
 * 1.0, HARD 1.5), rounded to the nearest int. Kept separate from UpgradesDisplayTest so this file
 * doesn't conflict with that one gaining its own getDefenceUpgrades()/
 * newPlayerEntityWithMaxHealth() helpers elsewhere.
 */
@ExtendWith(GameExtension.class)
class RegenOnKillDifficultyTest {
  private UpgradesDisplay display;

  @BeforeEach
  void beforeEach() {
    DifficultyService.setCurrent(Difficulty.NORMAL);

    RenderService renderService = new RenderService();
    renderService.setStage(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));

    display = new UpgradesDisplay();
    Entity entity = new Entity().addComponent(new UpgradesMenuComponent()).addComponent(display);
    entity.create();
  }

  @AfterEach
  void afterEach() {
    DifficultyService.setCurrent(Difficulty.NORMAL);
  }

  @SuppressWarnings("unchecked")
  private List<UpgradeNode> getDefenceUpgrades() throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("defenceUpgrades");
    field.setAccessible(true);
    return (List<UpgradeNode>) field.get(display);
  }

  /** Adds ConsumableUseComponent, unlike a plain player entity, since onEnemyKilled() needs it. */
  private Entity newPlayerEntityWithMaxHealth(int currentHealth, int maxHealth) {
    return new Entity()
        .addComponent(new CombatStatsComponent(currentHealth, 10))
        .addComponent(new PlayerActions())
        .addComponent(new ConsumableUseComponent(maxHealth));
  }

  @Test
  void regenOnKillHealsMoreOnHardThanNormalAtEachTier() throws Exception {
    Entity player = newPlayerEntityWithMaxHealth(0, 10_000); // plenty of headroom, never caps
    display.setPlayer(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    UpgradeNode regenOnKill = getDefenceUpgrades().get(1); // "regen_on_kill"

    regenOnKill.purchaseNextTier(); // Tier 1: base 5 HP/kill

    int before = stats.getHealth();
    player.getEvents().trigger("enemyKilled");
    assertEquals(5, stats.getHealth() - before); // Tier 1, Normal: round(5 * 1.0) = 5

    DifficultyService.setCurrent(Difficulty.HARD);
    before = stats.getHealth();
    player.getEvents().trigger("enemyKilled");
    assertEquals(8, stats.getHealth() - before); // Tier 1, Hard: round(5 * 1.5) = 8

    regenOnKill.purchaseNextTier(); // Tier 2: base 10 HP/kill - still Hard

    before = stats.getHealth();
    player.getEvents().trigger("enemyKilled");
    assertEquals(15, stats.getHealth() - before); // Tier 2, Hard: round(10 * 1.5) = 15

    DifficultyService.setCurrent(Difficulty.NORMAL);
    before = stats.getHealth();
    player.getEvents().trigger("enemyKilled");
    assertEquals(10, stats.getHealth() - before); // Tier 2, Normal: round(10 * 1.0) = 10
  }
}
