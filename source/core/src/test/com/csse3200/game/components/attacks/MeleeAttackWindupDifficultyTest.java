package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.difficulty.Difficulty;
import com.csse3200.game.difficulty.DifficultyScaler;
import com.csse3200.game.difficulty.DifficultyService;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Drives a real melee attack cycle (trigger, then wind-up countdown, then damage) under a mocked
 * GameTime, and checks when the hit lands for each difficulty's wind-up multiplier. Time advances
 * in 0.1s steps, and the assertions leave a 0.2s margin either side of each boundary.
 */
@ExtendWith(GameExtension.class)
class MeleeAttackWindupDifficultyTest {
  private static final float STEP_SECONDS = 0.1f;
  private static final float WINDUP_SECONDS = 2f;
  private static final float COOLDOWN_SECONDS = 5f;
  private static final float DELTA = 0.0001f;

  @BeforeEach
  void beforeEach() {
    DifficultyService.setCurrent(Difficulty.NORMAL);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(STEP_SECONDS);
    ServiceLocator.registerTimeSource(gameTime);
  }

  @AfterEach
  void afterEach() {
    DifficultyService.setCurrent(Difficulty.NORMAL);
  }

  private WeaponItem windupWeapon() {
    return new WeaponItem("Test Sword", WeaponType.SWORD, 10, 1, 1, WINDUP_SECONDS);
  }

  private Entity newAttacker(WeaponItem weapon) {
    Entity attacker =
        new Entity()
            .addComponent(new MeleeAttackComponent(3f, COOLDOWN_SECONDS, 0f, weapon))
            .addComponent(new CombatStatsComponent(20, 10))
            .addComponent(new PhysicsComponent());
    attacker.create();
    attacker.setPosition(0, 0);
    return attacker;
  }

  private Entity newTarget() {
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(new PhysicsComponent());
    target.create();
    target.setPosition(1, 0);
    return target;
  }

  private void advance(Entity attacker, int steps) {
    for (int i = 0; i < steps; i++) {
      attacker.update();
    }
  }

  @Test
  void defaultWindupHasNotLandedAt1Point8sAndHasLandedBy2Point2s() {
    Entity attacker = newAttacker(windupWeapon());
    Entity target = newTarget();
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    int healthBefore = targetStats.getHealth();

    attacker.getEvents().trigger("meleeAttack", target);
    advance(attacker, 18); // 1.8s
    assertEquals(healthBefore, targetStats.getHealth(), "Hit should not have landed at 1.8s.");

    advance(attacker, 4); // 2.2s total
    assertTrue(targetStats.getHealth() < healthBefore, "Hit should have landed by 2.2s.");
  }

  @Test
  void hardWindupHasNotLandedAt1Point2sAndHasLandedBy1Point6s() {
    DifficultyService.setCurrent(Difficulty.HARD);
    Entity attacker = newAttacker(windupWeapon());
    DifficultyScaler.apply(attacker);
    Entity target = newTarget();
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    int healthBefore = targetStats.getHealth();

    attacker.getEvents().trigger("meleeAttack", target);
    advance(attacker, 12); // 1.2s - windup is 2.0s x 0.7 = 1.4s
    assertEquals(healthBefore, targetStats.getHealth(), "Hit should not have landed at 1.2s.");

    advance(attacker, 4); // 1.6s total
    assertTrue(targetStats.getHealth() < healthBefore, "Hit should have landed by 1.6s.");
  }

  @Test
  void setWindupMultiplierRejectsZeroAndNegativeValues() {
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(3f, COOLDOWN_SECONDS, 0f, windupWeapon());

    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setWindupMultiplier(0f));
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setWindupMultiplier(-1f));
    assertEquals(1f, meleeAttack.getWindupMultiplier(), DELTA);
  }

  @Test
  void hardWindupLeavesTheWeaponsOwnWindupDurationUnchanged() {
    DifficultyService.setCurrent(Difficulty.HARD);
    WeaponItem weapon = windupWeapon();
    Entity attacker = newAttacker(weapon);
    DifficultyScaler.apply(attacker);
    Entity target = newTarget();

    attacker.getEvents().trigger("meleeAttack", target);
    advance(attacker, 16);

    assertEquals(WINDUP_SECONDS, weapon.getWindupDuration(), DELTA);
  }

  @Test
  void weaponlessMeleeAttackStillResolvesImmediately() throws Exception {
    WeaponItem instant = new WeaponItem("Test Dagger", WeaponType.DAGGER, 3, 1, 1, 0f);
    Entity attacker = newAttacker(instant);
    MeleeAttackComponent meleeAttack = attacker.getComponent(MeleeAttackComponent.class);
    Field weaponField = MeleeAttackComponent.class.getDeclaredField("weapon");
    weaponField.setAccessible(true);
    weaponField.set(meleeAttack, null);
    meleeAttack.setWindupMultiplier(0.7f);
    Entity target = newTarget();
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    int healthBefore = targetStats.getHealth();

    attacker.getEvents().trigger("meleeAttack", target);
    advance(attacker, 1);

    assertTrue(
        targetStats.getHealth() < healthBefore,
        "A weaponless attacker with no wind-up should still hit on the next update.");
  }
}
