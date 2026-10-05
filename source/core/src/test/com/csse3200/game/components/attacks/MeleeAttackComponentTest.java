package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponTier;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MeleeAttackComponentTest {

  /**
   * Damage dealt by the default test weapon (see {@link #createInstantWeapon()}): the real,
   * currently-compiling {@code WeaponItem} computes damage as {@code baseDamage * tier}, so a
   * DAGGER (base damage 3) at tier 1 deals 3 damage.
   */
  private static final int DEFAULT_WEAPON_DAMAGE = 3;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(20f / 1000);
    ServiceLocator.registerTimeSource(gameTime);
  }

  // Constructor stores range, cooldown, knockback and exposes the weapon's damage via getDamage().
  @Test
  void shouldStoreConstructorValuesCorrectly() {
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    WeaponItem weapon = createInstantWeapon();
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue, weapon);
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
    assertEquals(
        weapon.getDamage(),
        meleeAttack.getDamage(),
        "Expected getDamage() to expose the equipped weapon's damage, but got a mismatch.");
  }

  // Constructor rejects a negative range value.
  @Test
  void shouldThrowWhenConstructedWithInvalidRange() {
    WeaponItem weapon = createInstantWeapon();
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(-1, 10, 5f, weapon),
        "Expected constructor to throw IllegalArgumentException for range = "
            + -1
            + ", but it did not.");
  }

  // Constructor rejects a negative cooldown value.
  @Test
  void shouldThrowWhenConstructedWithInvalidCooldown() {
    WeaponItem weapon = createInstantWeapon();
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(1, -10, 5f, weapon),
        "Expected constructor to throw IllegalArgumentException for cooldown = "
            + -10
            + ", but it did not.");
  }

  // Constructor rejects a negative knockback value.
  @Test
  void shouldThrowWhenConstructedWithInvalidKnockback() {
    WeaponItem weapon = createInstantWeapon();
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(1, 10, -5f, weapon),
        "Expected constructor to throw IllegalArgumentException for knockback = "
            + -5f
            + ", but it did not.");
  }

  // Constructor rejects a null weapon.
  @Test
  void shouldThrowWhenConstructedWithNullWeapon() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(1, 10, 5f, null),
        "Expected constructor to throw IllegalArgumentException for a null weapon, but it did not.");
  }

  // Constructor rejects a Bow weapon, since melee attacks cannot use ranged weapons.
  @Test
  void shouldThrowWhenConstructedWithBowWeapon() {
    WeaponItem bow = new WeaponItem("Test Bow", WeaponType.BOW, DEFAULT_WEAPON_DAMAGE, 1, 1);
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(1, 10, 5f, bow),
        "Expected constructor to throw IllegalArgumentException for a Bow weapon, but it did not.");
  }

  // Constructor rejects a weapon windup that is negative, equal to cooldown, or exceeds cooldown.
  @Test
  void shouldRejectInvalidWindupDurations() {
    WeaponItem negativeWindup =
        new WeaponItem("Test Dagger", WeaponType.DAGGER, 1, DEFAULT_WEAPON_DAMAGE, 1, -1);
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(1, 10, 5f, negativeWindup),
        "Expected a negative windupDuration to be rejected, but it was not.");

    WeaponItem windupEqualsCooldown =
        new WeaponItem("Test Dagger", WeaponType.DAGGER, DEFAULT_WEAPON_DAMAGE, 1, 10, 10);
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(1, 10, 5f, windupEqualsCooldown),
        "Expected windupDuration equal to cooldown to be rejected, but it was not.");

    WeaponItem windupExceedsCooldown =
        new WeaponItem("Test Dagger", WeaponType.DAGGER, DEFAULT_WEAPON_DAMAGE, 1, 11, 11);
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(1, 10, 5f, windupExceedsCooldown),
        "Expected windupDuration exceeding cooldown to be rejected, but it was not.");
  }

  // Constructor accepts a weapon windup that sits just below the configured cooldown.
  @Test
  void shouldAcceptWeaponWindupJustBelowCooldown() {
    WeaponItem weapon =
        new WeaponItem("Test Dagger", WeaponType.DAGGER, DEFAULT_WEAPON_DAMAGE, 1, 11, 9.9f);
    assertDoesNotThrow(
        () -> new MeleeAttackComponent(1, 10, 5f, weapon),
        "Expected windupDuration just below cooldown to be accepted as a valid boundary value.");
  }

  // getDamage() and a landed hit both scale as baseDamage * tier, not just baseDamage.
  @Test
  void shouldApplyTierScaledWeaponDamage() {
    WeaponItem tierTwoSword =
        new WeaponItem("Test Sword", WeaponType.SWORD, WeaponTier.TIER_2, 1, 1, 0f);
    int expectedDamage = WeaponTier.TIER_2.getStats(WeaponType.SWORD).getDamage(); // 10 * 2 = 20

    MeleeAttackComponent meleeAttack = new MeleeAttackComponent(2, 5, 0, tierTwoSword);
    assertEquals(
        expectedDamage,
        meleeAttack.getDamage(),
        "Expected getDamage() to scale as baseDamage * tier, but got "
            + meleeAttack.getDamage()
            + " for tier "
            + 2
            + " (baseDamage "
            + tierTwoSword.getDamage()
            + ").");

    Entity attacker =
        new Entity()
            .addComponent(meleeAttack)
            .addComponent(new CombatStatsComponent(10, 2))
            .addComponent(new PhysicsComponent());
    attacker.create();
    // health set well above expectedDamage (20) so a landed hit can't bottom out at a health
    // floor of 0 - keeps this test from depending on how hit()/setHealth() clamps negative values
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(50, 0))
            .addComponent(new PhysicsComponent());
    target.create();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    int healthBeforeAttack = targetStats.getHealth();

    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update(); // resolve the zero-length windup

    assertEquals(
        healthBeforeAttack - expectedDamage,
        targetStats.getHealth(),
        "Expected a landed hit to reduce health by the tier-scaled damage of "
            + expectedDamage
            + ", but health went from "
            + healthBeforeAttack
            + " to "
            + targetStats.getHealth());
  }

  // The difficulty damage multiplier (as set by DifficultyScaler at spawn) scales a landed hit's
  // damage: unchanged at the default 1f, scaled up at 1.5f, scaled down (but never below 1) at
  // 0.6f.
  @Test
  void shouldApplyDifficultyDamageMultiplierToLandedHitDamage() {
    // default multiplier (1f) -> weapon damage of 10 is unchanged
    Entity defaultAttacker = createWeaponDamageTenAttacker();
    Entity defaultTarget = createHighHealthTarget();
    defaultAttacker.setPosition(0, 0);
    defaultTarget.setPosition(1, 0);
    int defaultHealthBefore = defaultTarget.getComponent(CombatStatsComponent.class).getHealth();
    defaultAttacker.getEvents().trigger("meleeAttack", defaultTarget);
    defaultAttacker.update();
    assertEquals(
        defaultHealthBefore - 10,
        defaultTarget.getComponent(CombatStatsComponent.class).getHealth(),
        "Expected the default 1f multiplier to leave weapon damage of 10 unchanged.");

    // 1.5f multiplier -> weapon damage of 10 becomes round(10 * 1.5) = 15
    Entity hardAttacker = createWeaponDamageTenAttacker();
    hardAttacker.getComponent(MeleeAttackComponent.class).setDamageMultiplier(1.5f);
    Entity hardTarget = createHighHealthTarget();
    hardAttacker.setPosition(0, 0);
    hardTarget.setPosition(1, 0);
    int hardHealthBefore = hardTarget.getComponent(CombatStatsComponent.class).getHealth();
    hardAttacker.getEvents().trigger("meleeAttack", hardTarget);
    hardAttacker.update();
    assertEquals(
        hardHealthBefore - 15,
        hardTarget.getComponent(CombatStatsComponent.class).getHealth(),
        "Expected a 1.5f multiplier to scale weapon damage of 10 up to 15.");

    // 0.6f multiplier -> weapon damage of 10 becomes round(10 * 0.6) = 6
    Entity easyAttacker = createWeaponDamageTenAttacker();
    easyAttacker.getComponent(MeleeAttackComponent.class).setDamageMultiplier(0.6f);
    Entity easyTarget = createHighHealthTarget();
    easyAttacker.setPosition(0, 0);
    easyTarget.setPosition(1, 0);
    int easyHealthBefore = easyTarget.getComponent(CombatStatsComponent.class).getHealth();
    easyAttacker.getEvents().trigger("meleeAttack", easyTarget);
    easyAttacker.update();
    assertEquals(
        easyHealthBefore - 6,
        easyTarget.getComponent(CombatStatsComponent.class).getHealth(),
        "Expected a 0.6f multiplier to scale weapon damage of 10 down to 6.");
  }

  // The charge multiplier and the difficulty damage multiplier stack: charge is applied first
  // (truncated), then the difficulty multiplier is applied on top of that (rounded), matching
  // resolveAttack()'s actual order of operations.
  @Test
  void shouldStackChargeMultiplierThenDifficultyMultiplier() {
    WeaponItem weapon = new WeaponItem("Test Sword", WeaponType.SWORD, 10, 1, 1, 0f);
    MeleeAttackComponent meleeAttack = new MeleeAttackComponent(3, 2, 0, weapon);
    Entity attacker =
        new Entity()
            .addComponent(meleeAttack)
            .addComponent(new CombatStatsComponent(20, 2))
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(new ChargeComponent(1f, 1f, 2.0f, 1.5f));
    attacker.create();
    meleeAttack.setDamageMultiplier(1.5f);

    Entity target = createHighHealthTarget();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);
    int healthBefore = target.getComponent(CombatStatsComponent.class).getHealth();

    // start a real charge: canCharge() is true immediately after construction (timeSinceLastCharge
    // is seeded to the cooldown), so this doesn't need any preceding update() calls.
    attacker.getComponent(ChargeComponent.class).startCharge(target.getPosition());

    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update(); // resolve the zero-length windup so the hit actually lands

    // charge first: (int) (10 * 2.0) = 20, then difficulty: round(20 * 1.5) = 30
    assertEquals(
        healthBefore - 30,
        target.getComponent(CombatStatsComponent.class).getHealth(),
        "Expected charge (x2.0, truncated) then difficulty (x1.5, rounded) to combine to 30 "
            + "damage from a base weapon damage of 10.");
  }

  // Regression test for a compounding-multiplier bug: for a weaponless attacker, getDamage()
  // falls back to combatStats.getBaseAttack(), which resolveAttack() then writes back via
  // setBaseAttack(finalDamage). If the damage multiplier were applied in that branch too, the
  // multiplier would compound on every single swing (10 -> 15 -> 22.5 -> ...). It must not apply
  // at all when there's no weapon - only the weapon-damage path gets the difficulty multiplier.
  //
  // There is no public API to construct a weapon-less MeleeAttackComponent without throwing (the
  // 3-arg constructor reads an unset weapon field and NPEs), so this test builds a normal,
  // weapon-equipped component and clears the weapon field via reflection afterward.
  @Test
  void shouldNotCompoundDamageMultiplierForWeaponlessAttacker() throws Exception {
    MeleeAttackComponent meleeAttack = new MeleeAttackComponent(3, 2, 0, createInstantWeapon());
    Field weaponField = MeleeAttackComponent.class.getDeclaredField("weapon");
    weaponField.setAccessible(true);
    weaponField.set(meleeAttack, null);
    meleeAttack.setDamageMultiplier(1.5f);

    Entity attacker =
        new Entity()
            .addComponent(meleeAttack)
            .addComponent(new CombatStatsComponent(20, 10))
            .addComponent(new PhysicsComponent());
    attacker.create();

    Entity target = createHighHealthTarget();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

    int healthBeforeFirstHit = targetStats.getHealth();
    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update(); // resolve the zero-length windup so the first hit lands
    assertEquals(
        healthBeforeFirstHit - 10,
        targetStats.getHealth(),
        "Expected the first landed hit to deal the unscaled base attack of 10, since a weaponless "
            + "attacker's damage multiplier must not apply.");

    // let the cooldown elapse so a second attack can land
    for (int i = 0; i < 101; i++) {
      attacker.update();
    }

    int healthBeforeSecondHit = targetStats.getHealth();
    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update();
    assertEquals(
        healthBeforeSecondHit - 10,
        targetStats.getHealth(),
        "Expected a second consecutive landed hit to still deal exactly 10 damage, proving the "
            + "multiplier did not compound onto the attacker's own baseAttack between hits.");
  }

  // Setters update range, cooldown, and knockback to new valid values.
  @Test
  void ShouldUpdateStatsViaSetter() {
    float oldRange = 0.5f;
    float oldCooldown = 10;
    float oldKnockback = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(oldRange, oldCooldown, oldKnockback, createInstantWeapon());
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

  // setCooldown() rejects both a negative value and exactly zero, leaving the old value intact.
  @Test
  void shouldRejectZeroOrNegativeCooldown() {
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue, createInstantWeapon());
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setCooldown(-30));
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setCooldown(0));
    assertEquals(
        cooldownValue,
        meleeAttack.getCooldown(),
        "Cooldown stats from meleeAttack expected: "
            + cooldownValue
            + " but got"
            + meleeAttack.getCooldown());
  }

  // setRange() rejects a negative value, leaving the old value intact.
  @Test
  void shouldRejectNegativeRange() {
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue, createInstantWeapon());
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setRange(-1));
    assertEquals(
        rangeValue,
        meleeAttack.getRange(),
        "Range stats from meleeAttack expected: "
            + rangeValue
            + " but got: "
            + meleeAttack.getRange());
  }

  // setKnockback() rejects a negative value, leaving the old value intact.
  @Test
  void shouldRejectNegativeKnockback() {
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue, createInstantWeapon());
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setKnockback(-5f));
    assertEquals(
        knockbackValue,
        meleeAttack.getKnockback(),
        "Knockback stats from meleeAttack expected: "
            + knockbackValue
            + " but got"
            + meleeAttack.getKnockback());
  }

  // setKnockback() accepts exactly zero, which intentionally disables knockback.
  @Test
  void shouldAcceptZeroKnockback() {
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue, createInstantWeapon());
    meleeAttack.setKnockback(0f);
    assertEquals(
        0f,
        meleeAttack.getKnockback(),
        "meleeAttack Knockback value should accept "
            + 0f
            + " but got a value of "
            + meleeAttack.getKnockback());
  }

  // setDamageMultiplier() rejects both a negative value and exactly zero, leaving the old value
  // intact.
  @Test
  void shouldRejectZeroOrNegativeDamageMultiplier() {
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(0.5f, 10, 2.0f, createInstantWeapon());
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setDamageMultiplier(-1f));
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setDamageMultiplier(0f));
    assertEquals(
        1f,
        meleeAttack.getDamageMultiplier(),
        "Expected the damage multiplier to remain at its default of 1f after rejected updates.");
  }

  // create() wires up CombatStatsComponent and the meleeAttack listener so an attack can land.
  @Test
  void shouldResolveCombatStatsComponentOnCreate() {
    Entity attacker = createAttacker(3, 2, 5);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    int targetHealthBeforeAttack = target.getComponent(CombatStatsComponent.class).getHealth();
    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update(); // resolve the zero-length windup so the hit actually lands
    assertTrue(
        target.getComponent(CombatStatsComponent.class).getHealth() < targetHealthBeforeAttack,
        "Expected Target's health to decrease from "
            + targetHealthBeforeAttack
            + " to "
            + (targetHealthBeforeAttack - DEFAULT_WEAPON_DAMAGE)
            + " but got "
            + target.getComponent(CombatStatsComponent.class).getHealth());
  }

  // The meleeAttack listener registered in create() actually applies the weapon's damage.
  @Test
  void ShouldAttachToAttackEventOnCreate() {
    WeaponItem weapon = createInstantWeapon();
    Entity attacker =
        new Entity()
            .addComponent(new MeleeAttackComponent(3, 2, 5, weapon))
            .addComponent(new CombatStatsComponent(20, 2));
    attacker.create();

    Entity target = new Entity().addComponent(new CombatStatsComponent(10, 0));
    target.create();
    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update(); // resolve the zero-length windup so the hit actually lands

    int expectedHealth = 10 - DEFAULT_WEAPON_DAMAGE;
    assertEquals(
        expectedHealth,
        target.getComponent(CombatStatsComponent.class).getHealth(),
        "expected "
            + expectedHealth
            + " but got "
            + target.getComponent(CombatStatsComponent.class).getHealth());
  }

  // update() advances the cooldown timer, allowing a blocked attack to land once it elapses.
  @Test
  void ShouldIncrementCooldownTimerEachUpdate() {
    float cooldownValue = 2;
    Entity attacker = createAttacker(3, cooldownValue, 1.0f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    CombatStatsComponent targetCombat = target.getComponent(CombatStatsComponent.class);
    int targetHealthBeforeFirstAttack = targetCombat.getHealth();

    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update();
    assertTrue(
        targetHealthBeforeFirstAttack > targetCombat.getHealth(),
        "Expected the first attack to land, reducing health below "
            + targetHealthBeforeFirstAttack
            + ", but got "
            + targetCombat.getHealth());
    int expectedHealthAfterFirstAttack = targetHealthBeforeFirstAttack - DEFAULT_WEAPON_DAMAGE;
    assertEquals(
        expectedHealthAfterFirstAttack,
        targetCombat.getHealth(),
        "Expected health of "
            + expectedHealthAfterFirstAttack
            + " after first attack, but got "
            + targetCombat.getHealth());

    int targetHealthAfterFirstAttack = targetCombat.getHealth();
    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update();
    assertEquals(
        targetHealthAfterFirstAttack,
        targetCombat.getHealth(),
        "Expected no additional damage while still on cooldown, but health changed from "
            + targetHealthAfterFirstAttack
            + " to "
            + targetCombat.getHealth());

    for (int i = 0; i < 101; i++) {
      attacker.update();
    }

    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update();
    assertTrue(
        targetHealthAfterFirstAttack > targetCombat.getHealth(),
        "Expected a third attack to land once cooldown elapsed, reducing health below "
            + targetHealthAfterFirstAttack
            + ", but got "
            + targetCombat.getHealth());
    int expectedHealthAfterThirdAttack = targetHealthAfterFirstAttack - DEFAULT_WEAPON_DAMAGE;
    assertEquals(
        expectedHealthAfterThirdAttack,
        targetCombat.getHealth(),
        "Expected health of "
            + expectedHealthAfterThirdAttack
            + " after third attack, but got "
            + targetCombat.getHealth());
  }

  // Triggering meleeAttack with a null target does not throw.
  @Test
  void ShouldNotThrowWhenTargetIsNull() {
    Entity attacker = createAttacker(2, 1, 0);
    attacker.create();
    assertDoesNotThrow(() -> attacker.getEvents().trigger("meleeAttack", null));
  }

  // Attacks are blocked immediately after landing, still blocked partway through cooldown, then
  // land once elapsed.
  @Test
  void shouldRespectCooldownAcrossBlockedPartialAndElapsedStates() {
    Entity attacker = createAttacker(2, 2, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

    // first attack lands and restarts the cooldown
    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update();
    float healthAfterFirstAttack = targetStats.getHealth();

    // immediately blocked by cooldown
    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update();
    assertEquals(
        healthAfterFirstAttack,
        targetStats.getHealth(),
        "Expected no damage immediately after the first attack while on cooldown.");

    // still blocked just before the 2-second cooldown completes (99 * 20ms = 1.98s total)
    for (int i = 0; i < 98; i++) {
      attacker.update();
    }
    attacker.getEvents().trigger("meleeAttack", target);
    assertEquals(
        healthAfterFirstAttack,
        targetStats.getHealth(),
        "Expected no damage with cooldown only partially elapsed.");

    // cooldown elapses, next attempt lands
    for (int i = 0; i < 101; i++) {
      attacker.update();
    }
    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update();
    assertTrue(
        targetStats.getHealth() < healthAfterFirstAttack,
        "Expected a further attack to land once cooldown elapsed, reducing health below "
            + healthAfterFirstAttack
            + ", but got "
            + targetStats.getHealth());
  }

  // A target with no CombatStatsComponent does not consume cooldown of Attacker, only when the
  // next real target is given does it then consume cooldown.
  @Test
  void ShouldResetCooldownOnlyAfterSuccessfulHit() {
    Entity attacker = createAttacker(2, 5, 0);
    Entity targetWithoutCombatStats = new Entity().addComponent(new PhysicsComponent());
    targetWithoutCombatStats.create();
    attacker.setPosition(0, 0);
    targetWithoutCombatStats.setPosition(1, 0);

    attacker.getEvents().trigger("meleeAttack", targetWithoutCombatStats);

    Entity targetWithCombatStats = createTarget();
    attacker.setPosition(0, 0);
    targetWithCombatStats.setPosition(1, 0);
    float targetHealthBeforeAttack =
        targetWithCombatStats.getComponent(CombatStatsComponent.class).getHealth();
    attacker.getEvents().trigger("meleeAttack", targetWithCombatStats);
    attacker.update();
    float targetHealthAfterAttack =
        targetWithCombatStats.getComponent(CombatStatsComponent.class).getHealth();
    assertTrue(
        targetHealthAfterAttack < targetHealthBeforeAttack,
        "Expected the attack on targetWithCombatStats to land (cooldown was not "
            + "consumed by the earlier failed attempt), reducing health from "
            + targetHealthBeforeAttack
            + " to below that value, but got "
            + targetHealthAfterAttack);
  }

  // Attacks land within range and exactly at the boundary, but not when the target is out of range.
  @Test
  void shouldRespectRangeWithinAtBoundaryAndOutside() {
    // within range
    Entity attackerWithin = createAttacker(2, 1, 0);
    Entity targetWithin = createTarget();
    attackerWithin.setPosition(0, 0);
    targetWithin.setPosition(1, 0);
    float healthBeforeWithin = targetWithin.getComponent(CombatStatsComponent.class).getHealth();
    attackerWithin.getEvents().trigger("meleeAttack", targetWithin);
    attackerWithin.update();
    assertTrue(
        targetWithin.getComponent(CombatStatsComponent.class).getHealth() < healthBeforeWithin,
        "Expected an attack within range to land, reducing health below " + healthBeforeWithin);

    // exactly at the range boundary
    Entity attackerBoundary = createAttacker(2, 1, 0);
    Entity targetBoundary = createTarget();
    attackerBoundary.setPosition(0, 0);
    targetBoundary.setPosition(2, 0);
    float healthBeforeBoundary =
        targetBoundary.getComponent(CombatStatsComponent.class).getHealth();
    attackerBoundary.getEvents().trigger("meleeAttack", targetBoundary);
    attackerBoundary.update();
    assertTrue(
        targetBoundary.getComponent(CombatStatsComponent.class).getHealth() < healthBeforeBoundary,
        "Expected an attack exactly at the range boundary to land, reducing health below "
            + healthBeforeBoundary);

    // outside range
    Entity attackerOutside = createAttacker(2, 1, 0);
    Entity targetOutside = createTarget();
    attackerOutside.setPosition(0, 0);
    targetOutside.setPosition(5, 0);
    float healthBeforeOutside = targetOutside.getComponent(CombatStatsComponent.class).getHealth();
    attackerOutside.getEvents().trigger("meleeAttack", targetOutside);
    attackerOutside.update();
    assertEquals(
        healthBeforeOutside,
        targetOutside.getComponent(CombatStatsComponent.class).getHealth(),
        "Expected an attack outside range to not land; health should remain "
            + healthBeforeOutside);
  }

  // A target with no CombatStatsComponent never takes damage and never causes an exception.
  @Test
  void ShouldNotAttackWhenTargetHasNoCombatStatsComponent() {
    Entity attacker = createAttacker(2, 5, 0);
    Entity targetWithoutCombatStats = new Entity().addComponent(new PhysicsComponent());
    targetWithoutCombatStats.create();
    attacker.setPosition(0, 0);
    targetWithoutCombatStats.setPosition(1, 0);

    attacker.getEvents().trigger("meleeAttack", targetWithoutCombatStats);
    assertDoesNotThrow(() -> attacker.getEvents().trigger("meleeAttack", targetWithoutCombatStats));
  }

  // Knockback applies when positive and the target has physics, but not when zero or physics is
  // absent.
  @Test
  void shouldApplyOrSkipKnockbackAcrossConditions() {
    // positive knockback + target has PhysicsComponent -> velocity changes
    Entity attackerWithKnockback = createAttacker(3, 1, 3);
    Entity targetWithPhysics = createTarget();
    attackerWithKnockback.setPosition(0, 0);
    targetWithPhysics.setPosition(2, 0);
    PhysicsComponent targetPhysics = targetWithPhysics.getComponent(PhysicsComponent.class);
    float velocityBefore = targetPhysics.getBody().getLinearVelocity().len();
    assertEquals(
        0,
        velocityBefore,
        "Velocity of target is expected to be at rest - 0 initially, but was actually "
            + velocityBefore);
    attackerWithKnockback.getEvents().trigger("meleeAttack", targetWithPhysics);
    attackerWithKnockback.update();
    float velocityAfter = targetPhysics.getBody().getLinearVelocity().len();
    assertNotSame(
        velocityBefore,
        velocityAfter,
        "Expected a change in velocity from knockback of attack, thus expecting velocity before "
            + "attack: "
            + velocityBefore
            + " to not equal velocity after attack: "
            + velocityAfter);

    // zero knockback -> velocity unaffected
    Entity attackerNoKnockback = createAttacker(3, 1, 0);
    Entity targetForZeroKnockback = createTarget();
    attackerNoKnockback.setPosition(0, 0);
    targetForZeroKnockback.setPosition(2, 0);
    PhysicsComponent zeroKnockbackPhysics =
        targetForZeroKnockback.getComponent(PhysicsComponent.class);
    float velocityBeforeZero = zeroKnockbackPhysics.getBody().getLinearVelocity().len();
    attackerNoKnockback.getEvents().trigger("meleeAttack", targetForZeroKnockback);
    attackerNoKnockback.update();
    float velocityAfterZero = zeroKnockbackPhysics.getBody().getLinearVelocity().len();
    assertEquals(
        velocityBeforeZero,
        velocityAfterZero,
        "Expected no change in velocity from knockback as it is disabled, thus expecting "
            + "velocity before attack: "
            + velocityBeforeZero
            + " to equal velocity after attack: "
            + velocityAfterZero);

    // target has no PhysicsComponent -> damage still applies, knockback is skipped without throwing
    Entity attackerVsNoPhysicsTarget = createAttacker(2, 1, 3);
    Entity targetWithoutPhysics = new Entity().addComponent(new CombatStatsComponent(10, 0));
    targetWithoutPhysics.create();
    attackerVsNoPhysicsTarget.setPosition(0, 0);
    targetWithoutPhysics.setPosition(2, 0);
    float healthBeforeNoPhysics =
        targetWithoutPhysics.getComponent(CombatStatsComponent.class).getHealth();
    attackerVsNoPhysicsTarget.getEvents().trigger("meleeAttack", targetWithoutPhysics);
    assertDoesNotThrow(
        attackerVsNoPhysicsTarget::update,
        "Expected resolving an attack on a target with no PhysicsComponent not to throw.");
    float healthAfterNoPhysics =
        targetWithoutPhysics.getComponent(CombatStatsComponent.class).getHealth();
    assertTrue(
        healthAfterNoPhysics < healthBeforeNoPhysics,
        "Expected damage to still apply even though target has no PhysicsComponent "
            + "(knockback should be skipped but not damage), but health went from "
            + healthBeforeNoPhysics
            + " to "
            + healthAfterNoPhysics);
  }

  // canAttack() is false right after triggering, stays false until cooldown elapses, then true and
  // stays true.
  @Test
  void canAttack_reflectsCooldownProgressionOverTime() {
    Entity attacker = createAttacker(2, 2, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);
    MeleeAttackComponent meleeAttack = attacker.getComponent(MeleeAttackComponent.class);

    attacker.getEvents().trigger("meleeAttack", target);
    assertFalse(
        meleeAttack.canAttack(),
        "Expected canAttack() to be false immediately after the attack windup starts.");

    attacker.update(); // resolve the zero-length windup; cooldown timer restarts at 0 from here

    // just before the 2-second cooldown completes (99 * 20ms = 1.98s)
    for (int i = 0; i < 99; i++) {
      attacker.update();
    }
    assertFalse(
        meleeAttack.canAttack(),
        "Expected canAttack() to still be false with cooldown partially elapsed.");

    // just past the 2-second cooldown (2 more * 20ms => total 2.02s)
    for (int i = 0; i < 2; i++) {
      attacker.update();
    }
    assertTrue(
        meleeAttack.canAttack(),
        "Expected canAttack() to be true once the cooldown timer has reached or passed the "
            + "configured duration.");

    // well past the cooldown
    for (int i = 0; i < 50; i++) {
      attacker.update();
    }
    assertTrue(
        meleeAttack.canAttack(), "Expected canAttack() to remain true well past the cooldown.");
  }

  // canAttack() reporting true is consistent with a second attack actually landing.
  @Test
  void canAttack_consistentWithAttemptAttackBehaviour() {
    Entity attacker = createAttacker(2, 2, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update();
    for (int i = 0; i < 101; i++) {
      attacker.update();
    }
    assertTrue(
        attacker.getComponent(MeleeAttackComponent.class).canAttack(),
        "Expected canAttack() to report true before the second attack is attempted.");

    float healthBeforeSecondAttack = targetStats.getHealth();
    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update();
    float healthAfterSecondAttack = targetStats.getHealth();

    assertTrue(
        healthAfterSecondAttack < healthBeforeSecondAttack,
        "Expected the second attack to actually land (consistent with canAttack() reporting "
            + "true beforehand), reducing health from "
            + healthBeforeSecondAttack
            + " to below that value, but got "
            + healthAfterSecondAttack);
  }

  /* ---------- Helpers ---------- */

  /**
   * Builds the default weapon used by {@link #createAttacker(float, float, float)}: a non-BOW type
   * with zero windup, so an attack resolves on the very next {@code update()} call after being
   * triggered. Deals {@link #DEFAULT_WEAPON_DAMAGE} damage per hit.
   *
   * @return a fresh weapon item suitable for most tests
   */
  WeaponItem createInstantWeapon() {
    return new WeaponItem("Test Dagger", WeaponType.DAGGER, DEFAULT_WEAPON_DAMAGE, 1, 1);
  }

  /**
   * Builds a fully created Entity representing an attacker, with a {@link MeleeAttackComponent}
   * (equipped with the default {@link #createInstantWeapon()}) and the components it depends on.
   *
   * @param knockback knockback magnitude passed directly into {@link MeleeAttackComponent}
   * @param range melee reach passed directly into {@link MeleeAttackComponent}
   * @param cooldown minimum time, in seconds, between successive melee attacks
   * @return an entity carrying {@link MeleeAttackComponent}, {@link CombatStatsComponent}, and
   *     {@link PhysicsComponent}
   */
  Entity createAttacker(float range, float cooldown, float knockback) {
    Entity attacker =
        new Entity()
            .addComponent(
                new MeleeAttackComponent(range, cooldown, knockback, createInstantWeapon()))
            .addComponent(new CombatStatsComponent(20, 2))
            .addComponent(new PhysicsComponent());
    attacker.create();
    return attacker;
  }

  /**
   * Builds a fully created Entity representing an attacker equipped with a weapon dealing exactly
   * 10 damage (via the literal-damage {@link WeaponItem} constructor, which returns {@code
   * getDamage()} unmodified - no tier scaling), for tests asserting exact difficulty-multiplier
   * arithmetic against a known base damage.
   *
   * @return an entity carrying {@link MeleeAttackComponent} (weapon damage 10), {@link
   *     CombatStatsComponent}, and {@link PhysicsComponent}
   */
  Entity createWeaponDamageTenAttacker() {
    WeaponItem weapon = new WeaponItem("Test Sword", WeaponType.SWORD, 10, 1, 1, 0f);
    Entity attacker =
        new Entity()
            .addComponent(new MeleeAttackComponent(3, 2, 0, weapon))
            .addComponent(new CombatStatsComponent(20, 2))
            .addComponent(new PhysicsComponent());
    attacker.create();
    return attacker;
  }

  /**
   * Builds a fully created target Entity with health set well above any damage value used in these
   * tests, so a landed hit can't bottom out at a health floor of 0 - keeps assertions from
   * depending on how hit()/setHealth() clamps negative values.
   *
   * @return a target entity that has {@link CombatStatsComponent} (100 health) and {@link
   *     PhysicsComponent} attached
   */
  Entity createHighHealthTarget() {
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(new PhysicsComponent());
    target.create();
    return target;
  }

  /**
   * Builds a fully created Entity representing a target.
   *
   * @return a target entity that has {@link CombatStatsComponent} and {@link PhysicsComponent}
   *     attached
   */
  Entity createTarget() {
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 0))
            .addComponent(new PhysicsComponent());

    target.create();

    return target;
  }
}
