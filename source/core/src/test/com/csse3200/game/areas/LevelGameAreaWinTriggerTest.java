package com.csse3200.game.areas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.win.BossDefeatedWinComponent;
import com.csse3200.game.win.BossRoster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests how a {@link LevelGameArea} learns that the game has been won: it arms the final boss, and
 * only the final boss, with the component that reports his death, and it flips its own flag when he
 * dies or when a loaded save says he is already dead.
 *
 * <p>The area is built but never created, so no map, physics or assets are needed: the two methods
 * under test take the enemy and its kill id directly.
 */
@ExtendWith(GameExtension.class)
class LevelGameAreaWinTriggerTest {
  private LevelGameArea area;

  @BeforeEach
  void beforeEach() {
    area = new LevelGameArea(new TerrainFactory(new CameraComponent()), "");
  }

  private static Entity newEnemy() {
    return new Entity().addComponent(new CombatStatsComponent(100, 10));
  }

  @Test
  void shouldNotBeWonWhenTheAreaIsNew() {
    assertFalse(area.isFinalBossDefeated());
  }

  // ---------- arming ----------

  @Test
  void shouldGiveTheFinalBossTheWinComponent() {
    Entity zeus = newEnemy();

    area.armWinTrigger(zeus, BossRoster.FINAL_BOSS_ID);

    assertNotNull(zeus.getComponent(BossDefeatedWinComponent.class));
  }

  @ParameterizedTest(name = "\"{0}\" is not the final boss")
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "Zeus's Thunder Hall:26,12",
        "Hound's Den:48,2",
        "Level 1 - Out of the Underworld:3,3",
        "Level 3 - Zeus's Palace:26,5"
      })
  void shouldLeaveEveryOtherEnemyWithoutTheWinComponent(String enemyId) {
    Entity enemy = newEnemy();

    area.armWinTrigger(enemy, enemyId);

    assertNull(enemy.getComponent(BossDefeatedWinComponent.class));
  }

  // ---------- winning ----------

  @Test
  void shouldBeWonWhenTheArmedFinalBossDies() {
    Entity zeus = newEnemy();
    area.armWinTrigger(zeus, BossRoster.FINAL_BOSS_ID);
    zeus.create();
    assertFalse(area.isFinalBossDefeated(), "alive, so not won yet");

    zeus.getComponent(CombatStatsComponent.class).setHealth(0);

    assertTrue(area.isFinalBossDefeated());
  }

  @Test
  void shouldNotBeWonWhenTheFinalBossIsOnlyHurt() {
    Entity zeus = newEnemy();
    area.armWinTrigger(zeus, BossRoster.FINAL_BOSS_ID);
    zeus.create();

    zeus.getComponent(CombatStatsComponent.class).setHealth(1);

    assertFalse(area.isFinalBossDefeated());
  }

  @Test
  void shouldNotBeWonWhenAnotherZeusDies() {
    // The Thunder Hall Zeus is a mini boss: his death must never end the game.
    Entity thunderHallZeus = newEnemy();
    area.armWinTrigger(thunderHallZeus, "Zeus's Thunder Hall:26,12");
    thunderHallZeus.create();

    thunderHallZeus.getComponent(CombatStatsComponent.class).setHealth(0);

    assertFalse(area.isFinalBossDefeated());
  }

  @Test
  void shouldStayWonOnceWon() {
    Entity zeus = newEnemy();
    area.armWinTrigger(zeus, BossRoster.FINAL_BOSS_ID);
    zeus.create();
    zeus.getEvents().trigger("death");

    zeus.getEvents().trigger("death");

    assertTrue(area.isFinalBossDefeated());
  }

  @Test
  void shouldKeepTwoAreasIndependent() {
    LevelGameArea other = new LevelGameArea(new TerrainFactory(new CameraComponent()), "");
    Entity zeus = newEnemy();
    area.armWinTrigger(zeus, BossRoster.FINAL_BOSS_ID);
    zeus.create();

    zeus.getEvents().trigger("death");

    assertTrue(area.isFinalBossDefeated());
    assertFalse(other.isFinalBossDefeated(), "only the area that armed him hears him fall");
  }

  // ---------- a save made after the win ----------

  @Test
  void shouldBeWonStraightAwayWhenASaveSaysTheFinalBossIsAlreadyDead() {
    area.noteAlreadyKilled(BossRoster.FINAL_BOSS_ID);

    assertTrue(area.isFinalBossDefeated());
  }

  @ParameterizedTest(name = "already-dead \"{0}\" does not win the game")
  @NullAndEmptySource
  @ValueSource(strings = {"Zeus's Thunder Hall:26,12", "Hound's Den:48,2"})
  void shouldNotBeWonBecauseSomeOtherEnemyIsAlreadyDead(String enemyId) {
    area.noteAlreadyKilled(enemyId);

    assertFalse(area.isFinalBossDefeated());
  }

  @Test
  void shouldStayWonWhenOtherDeadEnemiesAreNotedAfterwards() {
    area.noteAlreadyKilled(BossRoster.FINAL_BOSS_ID);

    area.noteAlreadyKilled("Hound's Den:48,2");

    assertTrue(area.isFinalBossDefeated());
  }

  // ---------- the killzeus debug command ----------

  @Test
  void shouldNotKillAnythingWhenTheFinalBossIsNotInTheArea() {
    Entity skeleton = newEnemy();
    area.areaEntities.add(skeleton);

    assertFalse(area.killFinalBoss());
    assertFalse(skeleton.getComponent(CombatStatsComponent.class).isDead());
  }

  @Test
  void shouldKillTheArmedFinalBossAndWin() {
    Entity skeleton = newEnemy();
    Entity zeus = newEnemy();
    area.armWinTrigger(zeus, BossRoster.FINAL_BOSS_ID);
    zeus.create();
    area.areaEntities.add(skeleton);
    area.areaEntities.add(zeus);

    assertTrue(area.killFinalBoss());

    assertTrue(zeus.getComponent(CombatStatsComponent.class).isDead());
    assertFalse(skeleton.getComponent(CombatStatsComponent.class).isDead());
    assertTrue(area.isFinalBossDefeated());
  }

  @Test
  void shouldNotKillTheFinalBossTwice() {
    Entity zeus = newEnemy();
    area.armWinTrigger(zeus, BossRoster.FINAL_BOSS_ID);
    zeus.create();
    area.areaEntities.add(zeus);
    area.killFinalBoss();

    assertFalse(area.killFinalBoss(), "he is already dead");
  }

  @Test
  void shouldSkipAFinalBossWithNoCombatStats() {
    Entity statless = new Entity().addComponent(new BossDefeatedWinComponent(new Entity()));
    area.areaEntities.add(statless);

    assertFalse(area.killFinalBoss());
  }
}
