package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for the S1 melee changes: the windup getters, the {@code "meleeAttackWhiff"} event, and
 * restoring the attacker's own base attack after a hit.
 *
 * <p>Assumed new API (see S1 spec): {@code getWindupDuration()}, {@code getWindupTimeRemaining()},
 * {@code isWindingUp()}, and the {@code "meleeAttackWhiff"} event carrying the target entity.
 *
 * <p>Timing: the mocked clock advances 0.02 s per {@code update()}. The test weapon has a 0.1 s
 * windup, so a windup resolves on the 5th update after the attack is triggered.
 */
@ExtendWith(GameExtension.class)
class MeleeAttackWindupWhiffTest {
  private static final float DELTA = 0.02f;
  private static final float WINDUP = 0.1f;
  private static final float COOLDOWN = 1f;
  private static final float RANGE = 2f;
  private static final int WEAPON_DAMAGE = 3; // DAGGER, tier 1 (see MeleeAttackComponentTest)
  private static final int ATTACKER_BASE_ATTACK = 2;
  private static final int UPDATES_TO_RESOLVE = 6; // 5 needed, one spare

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(DELTA);
    ServiceLocator.registerTimeSource(gameTime);
  }

  // ---------- windup getters ----------

  @Test
  void windupDurationMatchesTheWeaponsWindup() {
    MeleeAttackComponent melee = new MeleeAttackComponent(RANGE, COOLDOWN, 0f, windupWeapon());
    assertEquals(
        WINDUP, melee.getWindupDuration(), 1e-6f, "Armed attacker should use the weapon's windup.");
  }

  @Test
  void unarmedWindupDurationIsCooldownMinusOne() {
    MeleeAttackComponent melee = new MeleeAttackComponent(RANGE, 3f, 0f);
    assertEquals(2f, melee.getWindupDuration(), 1e-6f, "Unarmed windup is forced to cooldown - 1.");
  }

  @Test
  void notWindingUpBeforeAnyAttack() {
    Entity attacker = createAttacker();
    MeleeAttackComponent melee = attacker.getComponent(MeleeAttackComponent.class);
    assertFalse(melee.isWindingUp(), "Fresh component must not report a windup in progress.");
    assertEquals(0f, melee.getWindupTimeRemaining(), 1e-6f);
  }

  @Test
  void windingUpAfterTriggerAndCountsDown() {
    Entity attacker = createAttacker();
    Entity target = createTarget(1f, 50);
    MeleeAttackComponent melee = attacker.getComponent(MeleeAttackComponent.class);

    attacker.getEvents().trigger("meleeAttack", target);

    assertTrue(melee.isWindingUp(), "Windup should be in progress right after a valid trigger.");
    assertEquals(WINDUP, melee.getWindupTimeRemaining(), 1e-6f);

    attacker.update();
    assertEquals(
        WINDUP - DELTA,
        melee.getWindupTimeRemaining(),
        1e-5f,
        "Remaining windup should drop by exactly one frame's delta.");
    assertTrue(melee.isWindingUp());
  }

  @Test
  void windupTimeRemainingIsZeroAfterTheAttackResolves() {
    Entity attacker = createAttacker();
    Entity target = createTarget(1f, 50);
    MeleeAttackComponent melee = attacker.getComponent(MeleeAttackComponent.class);

    attacker.getEvents().trigger("meleeAttack", target);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertFalse(melee.isWindingUp(), "Windup must end once the attack resolves.");
    assertEquals(0f, melee.getWindupTimeRemaining(), 1e-6f);
  }

  @Test
  void windupMultiplierScalesTheRemainingWindup() {
    Entity attacker = createAttacker();
    MeleeAttackComponent melee = attacker.getComponent(MeleeAttackComponent.class);
    melee.setWindupMultiplier(2f);
    Entity target = createTarget(1f, 50);

    attacker.getEvents().trigger("meleeAttack", target);

    assertEquals(WINDUP * 2f, melee.getWindupTimeRemaining(), 1e-6f);
  }

  @Test
  void noWindupStartsWhenTheTargetIsOutOfRange() {
    Entity attacker = createAttacker();
    Entity target = createTarget(RANGE + 5f, 50);
    MeleeAttackComponent melee = attacker.getComponent(MeleeAttackComponent.class);

    attacker.getEvents().trigger("meleeAttack", target);

    assertFalse(melee.isWindingUp(), "An out-of-range trigger must not start a windup.");
  }

  // ---------- whiff event ----------

  @Test
  void whiffFiresWhenTheTargetDiesDuringWindup() {
    Entity attacker = createAttacker();
    Entity target = createTarget(1f, 50);
    List<Entity> whiffs = listen(attacker, "meleeAttackWhiff");
    List<Entity> hits = listen(attacker, "meleeAttackHit");

    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update();
    target.getComponent(CombatStatsComponent.class).setHealth(0);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(1, whiffs.size(), "Expected exactly one whiff when the target died mid-windup.");
    assertSame(target, whiffs.get(0), "Whiff must carry the intended target.");
    assertEquals(0, hits.size(), "No hit may be announced on a whiff.");
  }

  @Test
  void whiffFiresWhenTheTargetMovesOutOfRangeDuringWindup() {
    Entity attacker = createAttacker();
    Entity target = createTarget(1f, 50);
    List<Entity> whiffs = listen(attacker, "meleeAttackWhiff");
    List<Entity> hits = listen(attacker, "meleeAttackHit");

    attacker.getEvents().trigger("meleeAttack", target);
    target.setPosition(RANGE + 10f, 0f);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(1, whiffs.size());
    assertEquals(0, hits.size());
    assertEquals(50, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void whiffDoesNotFireOnASuccessfulHit() {
    Entity attacker = createAttacker();
    Entity target = createTarget(1f, 50);
    List<Entity> whiffs = listen(attacker, "meleeAttackWhiff");
    List<Entity> hits = listen(attacker, "meleeAttackHit");

    attacker.getEvents().trigger("meleeAttack", target);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(0, whiffs.size());
    assertEquals(1, hits.size());
    assertEquals(50 - WEAPON_DAMAGE, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void whiffDoesNotFireOnlyBecauseTheTriggerWasRejected() {
    Entity attacker = createAttacker();
    Entity farTarget = createTarget(RANGE + 5f, 50);
    List<Entity> whiffs = listen(attacker, "meleeAttackWhiff");

    attacker.getEvents().trigger("meleeAttack", farTarget);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(0, whiffs.size(), "A rejected trigger never starts a windup, so it cannot whiff.");
  }

  @Test
  void nullTargetNeitherThrowsNorWhiffs() {
    Entity attacker = createAttacker();
    List<Entity> whiffs = listen(attacker, "meleeAttackWhiff");

    assertDoesNotThrow(() -> attacker.getEvents().trigger("meleeAttack", (Entity) null));
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(0, whiffs.size());
  }

  @Test
  void aWhiffStillSpendsTheCooldown() {
    Entity attacker = createAttacker();
    Entity target = createTarget(1f, 50);
    MeleeAttackComponent melee = attacker.getComponent(MeleeAttackComponent.class);

    attacker.getEvents().trigger("meleeAttack", target);
    target.setPosition(RANGE + 10f, 0f);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertFalse(melee.canAttack(), "A whiffed swing must not refund the cooldown.");
  }

  @Test
  void aSecondTriggerDuringWindupIsIgnored() {
    Entity attacker = createAttacker();
    Entity target = createTarget(1f, 50);
    List<Entity> hits = listen(attacker, "meleeAttackHit");

    attacker.getEvents().trigger("meleeAttack", target);
    attacker.update();
    attacker.getEvents().trigger("meleeAttack", target); // cooldown is spent, so ignored
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(1, hits.size(), "Only one hit may land from a single windup.");
  }

  // ---------- base attack restore ----------

  @Test
  void baseAttackIsRestoredAfterAHit() {
    Entity attacker = createAttacker();
    Entity target = createTarget(1f, 50);

    attacker.getEvents().trigger("meleeAttack", target);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(
        ATTACKER_BASE_ATTACK,
        attacker.getComponent(CombatStatsComponent.class).getBaseAttack(),
        "The attacker's own base attack must not be left at the weapon's damage after a hit.");
  }

  @Test
  void baseAttackIsUnchangedAfterAWhiff() {
    Entity attacker = createAttacker();
    Entity target = createTarget(1f, 50);

    attacker.getEvents().trigger("meleeAttack", target);
    target.setPosition(RANGE + 10f, 0f);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(
        ATTACKER_BASE_ATTACK, attacker.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void repeatedHitsDealTheSameDamageEachTime() {
    Entity attacker = createAttacker();
    Entity target = createTarget(1f, 100);
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

    attacker.getEvents().trigger("meleeAttack", target);
    runUpdates(attacker, UPDATES_TO_RESOLVE);
    int afterFirst = targetStats.getHealth();
    runUpdates(attacker, (int) (COOLDOWN / DELTA) + 5); // let the cooldown elapse
    attacker.getEvents().trigger("meleeAttack", target);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(100 - WEAPON_DAMAGE, afterFirst);
    assertEquals(
        afterFirst - WEAPON_DAMAGE, targetStats.getHealth(), "Second hit drifted from the first.");
  }

  @Test
  void difficultyMultiplierDoesNotCompoundOrLeakIntoBaseAttack() {
    Entity attacker = createAttacker();
    attacker.getComponent(MeleeAttackComponent.class).setDamageMultiplier(1.5f);
    Entity target = createTarget(1f, 100);
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

    attacker.getEvents().trigger("meleeAttack", target);
    runUpdates(attacker, UPDATES_TO_RESOLVE);
    int firstHitDamage = 100 - targetStats.getHealth();
    runUpdates(attacker, (int) (COOLDOWN / DELTA) + 5);
    int before = targetStats.getHealth();
    attacker.getEvents().trigger("meleeAttack", target);
    runUpdates(attacker, UPDATES_TO_RESOLVE);
    int secondHitDamage = before - targetStats.getHealth();

    assertEquals(firstHitDamage, secondHitDamage, "Multiplier must not compound between hits.");
    assertEquals(
        ATTACKER_BASE_ATTACK, attacker.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void unarmedAttackerKeepsItsBaseAttackAndDealsIt() {
    Entity attacker =
        new Entity()
            .addComponent(new MeleeAttackComponent(RANGE, 3f, 0f))
            .addComponent(new CombatStatsComponent(10, 7))
            .addComponent(new PhysicsComponent());
    attacker.create();
    Entity target = createTarget(1f, 100);

    attacker.getEvents().trigger("meleeAttack", target);
    runUpdates(attacker, (int) (2f / DELTA) + 5);

    assertEquals(93, target.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(7, attacker.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  // ---------- helpers ----------

  private WeaponItem windupWeapon() {
    return new WeaponItem("Test Dagger", WeaponType.DAGGER, WeaponTier.TIER_1, 1, 1, WINDUP);
  }

  private Entity createAttacker() {
    Entity attacker =
        new Entity()
            .addComponent(new MeleeAttackComponent(RANGE, COOLDOWN, 0f, windupWeapon()))
            .addComponent(new CombatStatsComponent(10, ATTACKER_BASE_ATTACK))
            .addComponent(new PhysicsComponent());
    attacker.create();
    attacker.setPosition(0f, 0f);
    return attacker;
  }

  private Entity createTarget(float x, int health) {
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(health, 0))
            .addComponent(new PhysicsComponent());
    target.create();
    target.setPosition(x, 0f);
    return target;
  }

  private void runUpdates(Entity entity, int frames) {
    for (int i = 0; i < frames; i++) {
      entity.update();
    }
  }

  private List<Entity> listen(Entity attacker, String eventName) {
    List<Entity> received = new ArrayList<>();
    attacker.getEvents().addListener(eventName, (Entity e) -> received.add(e));
    return received;
  }
}
