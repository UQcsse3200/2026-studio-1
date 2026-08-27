package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class MeleeAttackComponentTest {

  @BeforeEach
  public void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    // should reject negative range

  }

  /* ---------- Helpers ---------- */

  Entity createAttacker(short targetLayer, float knockback, double range, double cooldown) {
    // TODO: build an Entity with MeleeAttackComponent, CombatStatsComponent,
    //  PhysicsComponent, HitboxComponent; call entity.create()
    return null;
  }

  Entity createTarget(short layer) {
    // TODO: mirror TouchAttackComponentTest.createTarget()
    return null;
  }

  @Test
  void shouldStoreConstructorValuesCorrectly() {
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue);
    assertEquals(
        rangeValue,
        meleeAttack.getRange(),
        "Range stats from meleeAttack expected: "
            + rangeValue
            + " but got: "
            + meleeAttack.getRange());
    assertEquals(
        cooldownValue,
        meleeAttack.getCooldown(),
        "Cooldown stats from meleeAttack expected: "
            + cooldownValue
            + " but got"
            + meleeAttack.getCooldown());
    assertEquals(
        knockbackValue,
        meleeAttack.getKnockback(),
        "Knockback stats from meleeAttack expected: "
            + knockbackValue
            + " but got"
            + meleeAttack.getKnockback());
  }

  @Test
  void shouldThrowWhenConstructedWithInvalidRange() {
    /* Confirm an invalid range passed directly to the constructor is rejected. */
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(-1, 10, 5f),
        "Expected constructor to throw IllegalArgumentException for range = "
            + -1
            + ", but it did not.");
  }

  @Test
  void shouldThrowWhenConstructedWithInvalidCooldown() {
    /* Confirm an invalid cooldown passed directly to the constructor is rejected. */
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(1, -10, 5f),
        "Expected constructor to throw IllegalArgumentException for cooldown = "
            + -10
            + ", but it did not.");
  }

  @Test
  void shouldThrowWhenConstructedWithInvalidKnockback() {
    /* Confirm an invalid knockback passed directly to the constructor is rejected. */
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(1, 10, -5f),
        "Expected constructor to throw IllegalArgumentException for knockback = "
            + -5f
            + ", but it did not.");
  }

  @Test
  void ShouldUpdateStatsViaSetter() {
    /*
     */
    float oldRange = 0.5f;
    float oldCooldown = 10;
    float oldKnockback = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(oldRange, oldCooldown, oldKnockback);
    float newRange = 0.1f;
    float newCooldown = 50;
    float newKnockback = 5.5f;
    meleeAttack.setRange(newRange);
    meleeAttack.setCooldown(newCooldown);
    meleeAttack.setKnockback(newKnockback);
    assertEquals(
        newRange,
        meleeAttack.getRange(),
        "meleeAttack Range stats from changed from "
            + oldRange
            + " to expected: "
            + newRange
            + " but got: "
            + meleeAttack.getRange());
    assertEquals(
        newCooldown,
        meleeAttack.getCooldown(),
        "meleeAttack Cooldown stats from changed from "
            + oldCooldown
            + " to expected: "
            + newCooldown
            + " but got: "
            + meleeAttack.getCooldown());
    assertEquals(
        newKnockback,
        meleeAttack.getKnockback(),
        "meleeAttack Range stats from changed from "
            + oldKnockback
            + " to expected: "
            + newKnockback
            + " but got: "
            + meleeAttack.getKnockback());
  }

  @Test
  void shouldRejectZeroOrNegativeCooldown() {
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue);
    // negative value
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setCooldown(-30));

    // base case also throws exception
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setCooldown(0));

    // original value has not been modified
    assertEquals(
        cooldownValue,
        meleeAttack.getCooldown(),
        "Cooldown stats from meleeAttack expected: "
            + cooldownValue
            + " but got"
            + meleeAttack.getCooldown());
  }

  @Test
  void shouldRejectNegativeRange() {
    /* Confirm negative */
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue);
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setRange(-1));
    assertEquals(
        rangeValue,
        meleeAttack.getRange(),
        "Range stats from meleeAttack expected: "
            + rangeValue
            + " but got: "
            + meleeAttack.getRange());
  }

  @Test
  void shouldRejectNegativeKnockback() {
    /* Confirm negative */
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue);
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setKnockback(-5f));
    assertEquals(
        knockbackValue,
        meleeAttack.getKnockback(),
        "Knockback stats from meleeAttack expected: "
            + knockbackValue
            + " but got"
            + meleeAttack.getKnockback());
  }

  @Test
  void shouldAcceptZeroKnockback() {
    /* Confirm 0 is treated as valid since it disables knockback rather than being invalid. */
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue);
    meleeAttack.setKnockback(0f);
    assertEquals(
        0f,
        meleeAttack.getKnockback(),
        "meleeAttack Knockback value should accept "
            + 0f
            + " but got a value of "
            + meleeAttack.getKnockback());
  }
}
