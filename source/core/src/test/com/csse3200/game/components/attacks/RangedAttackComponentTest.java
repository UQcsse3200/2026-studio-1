package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.ComponentPriority;
import com.csse3200.game.components.effects.SpeedEffectComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponTier;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Covers the behaviour unique to {@link RangedAttackComponent} now that it fires a real arrow
 * rather than resolving damage instantly: the cooldown/range gate on <i>firing</i>, and that
 * cooldown resets on firing rather than on a confirmed hit (see this class's own javadoc for why).
 * Whether a fired arrow actually deals damage/knockback on contact is covered separately by {@code
 * ProjectileHitComponentTest} (package-private, so it can't be linked from here), since {@link
 * com.csse3200.game.entities.EntityService} doesn't expose a way to inspect an entity it was just
 * asked to register - only that firing didn't throw and produced the expected "rangedAttackFired"
 * notification is verified here.
 */
@ExtendWith(GameExtension.class)
class RangedAttackComponentTest {

  @BeforeEach
  void beforeEach() {
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(16);
    when(texture.getHeight()).thenReturn(16);

    // LightningFreezeComponent.onHit plays "sounds/zap.mp3" via the resource service - stub it
    // the same way LightningFreezeComponentTest does, or firing LIGHTNING and triggering
    // "projectileHit" (see shouldApplyConfiguredLightningFreezeTicksNotTheDefault) NPEs on the
    // unstubbed (null) Sound.
    Sound sound = mock(Sound.class);

    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    when(resourceService.getAsset(anyString(), eq(Sound.class))).thenReturn(sound);

    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());

    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(20f / 1000);
    ServiceLocator.registerTimeSource(gameTime);
  }

  @Test
  void shouldStoreConstructorValuesCorrectly() {
    RangedAttackComponent ranged = new RangedAttackComponent(6f, 2.5f, 1f, createInstantWeapon());
    assertEquals(6f, ranged.getRange());
    assertEquals(2.5f, ranged.getCooldown());
    assertEquals(1f, ranged.getKnockback());
    assertEquals(
        8f,
        ranged.getProjectileSpeed(),
        "Should default to 8f until overridden via setProjectileSpeed(...)");
  }

  @Test
  void shouldRejectNonPositiveProjectileSpeed() {
    RangedAttackComponent ranged = new RangedAttackComponent(6f, 2.5f, 0f, createInstantWeapon());
    assertThrows(IllegalArgumentException.class, () -> ranged.setProjectileSpeed(0f));
    assertThrows(IllegalArgumentException.class, () -> ranged.setProjectileSpeed(-1f));
  }

  @Test
  void shouldFireOnFirstAttackWithinRange() {
    Entity attacker = createAttacker(6f, 2f, 0f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertEquals(1, fired.size());
  }

  @Test
  void shouldNotFireWhenTargetOutsideRange() {
    Entity attacker = createAttacker(2f, 1f, 0f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(10, 0);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertEquals(0, fired.size());
  }

  @Test
  void shouldHandleTargetExactlyAtRangeBoundary() {
    Entity attacker = createAttacker(2f, 1f, 0f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertEquals(1, fired.size());
  }

  @Test
  void shouldNotFireAtTargetWithoutCombatStatsComponent() {
    Entity attacker = createAttacker(6f, 1f, 0f);
    Entity targetWithoutStats = new Entity().addComponent(new PhysicsComponent());
    targetWithoutStats.create();
    attacker.setPosition(0, 0);
    targetWithoutStats.setPosition(2, 0);
    List<Entity> fired = listenForFired(attacker);

    assertDoesNotThrow(
        () ->
            attacker.getEvents().trigger("rangedAttack", targetWithoutStats, ProjectileType.ARROW));

    assertEquals(0, fired.size());
  }

  @Test
  void shouldNotFireDuringCooldown() {
    Entity attacker = createAttacker(6f, 2f, 0f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertEquals(
        1,
        fired.size(),
        "Expected the second shot attempted within the same cooldown window to be blocked.");
  }

  @Test
  void shouldResetCooldownOnFiringNotOnALandedHit() {
    // Unlike MeleeAttackComponent (which only resets its cooldown after a confirmed hit), this
    // component never finds out whether the arrow it fires actually lands - see this class's
    // javadoc. So cooldown must allow firing again once the cooldown duration elapses after the
    // FIRST shot alone, with no hit confirmation involved at all.
    Entity attacker = createAttacker(6f, 2f, 0f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    for (int i = 0; i < 101; i++) {
      attacker.update();
    }
    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertEquals(2, fired.size());
  }

  @Test
  void shouldNotThrowWhenTargetIsNull() {
    Entity attacker = createAttacker(6f, 2f, 0f);
    assertDoesNotThrow(
        () -> attacker.getEvents().trigger("rangedAttack", (Entity) null, ProjectileType.ARROW));
  }

  @Test
  void firingShouldNotThrowAndShouldSuccessfullyRegisterAnArrow() {
    // Smoke test for the full spawn pipeline: ArrowFactory.createRangedArrow(...) building a
    // physics-backed entity and EntityService actually being able to create() it without error.
    Entity attacker = createAttacker(6f, 2f, 1f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);

    assertDoesNotThrow(
        () -> attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW));
  }

  @Test
  void shouldAimLeftWhenTargetIsToTheLeft() {
    // Indirect check: firing at a target to the left must not throw despite the direction/spawn
    // offset math flipping sign compared to firing to the right.
    Entity attacker = createAttacker(6f, 2f, 0f);
    Entity target = createTarget();
    attacker.setPosition(5, 0);
    target.setPosition(0, 0);
    List<Entity> fired = listenForFired(attacker);

    assertDoesNotThrow(
        () -> attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW));

    assertEquals(1, fired.size());
  }

  // A natural weapon (no carried WeaponItem, e.g. a Cyclops's rock throw) flows through the
  // same weapon-based constructor an armed enemy uses, with damage exactly as given.
  @Test
  void shouldAcceptNaturalWeapon() {
    WeaponItem naturalRockThrow = WeaponItem.natural("Cyclops Rock Throw", 12, 2f);

    RangedAttackComponent ranged = new RangedAttackComponent(6f, 2.5f, 1f, naturalRockThrow);

    assertEquals(12, ranged.getDamage());
  }

  @Test
  void shouldSourceFiredArrowDamageFromWeaponNotShooterBaseAttack() {
    // Regression test: the fired arrow's damage must come from the equipped weapon
    // (WeaponItem#getDamage()), not the shooter's own CombatStatsComponent#getBaseAttack() - those
    // can differ, and sourcing the wrong one silently gives every ranged attacker identical damage
    // regardless of which weapon they're holding.
    EntityService entityService = mock(EntityService.class);
    List<Entity> registered = new ArrayList<>();
    doAnswer(
            invocation -> {
              registered.add(invocation.getArgument(0));
              return null;
            })
        .when(entityService)
        .register(any(Entity.class));
    ServiceLocator.registerEntityService(entityService);

    WeaponItem weapon = createInstantWeapon();
    Entity attacker =
        new Entity()
            .addComponent(new RangedAttackComponent(6f, 2f, 0f, weapon))
            // Deliberately different from the weapon's damage, so this test fails if the arrow's
            // damage is ever sourced from this instead.
            .addComponent(new CombatStatsComponent(20, 999));
    attacker.create();
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertEquals(1, registered.size());
    Entity arrow = registered.get(0);
    assertEquals(
        weapon.getDamage(),
        arrow.getComponent(CombatStatsComponent.class).getBaseAttack(),
        "Expected the fired arrow's damage to come from the weapon, not the shooter's own base "
            + "attack.");
  }

  @Test
  void shouldDefaultLightningFreezeAndPetrifyTicks() {
    // Matches this class's own previous hardcoded values (freezeTicks=120) and a comparable
    // petrify default (90), so an attacker that doesn't override either keeps today's behaviour.
    RangedAttackComponent ranged = new RangedAttackComponent(6f, 2.5f, 1f, createInstantWeapon());
    assertEquals(120, ranged.getLightningFreezeTicks());
    assertEquals(90, ranged.getPetrifyTicks());
  }

  @Test
  void shouldRejectNegativeLightningFreezeTicks() {
    RangedAttackComponent ranged = new RangedAttackComponent(6f, 2.5f, 1f, createInstantWeapon());
    assertThrows(IllegalArgumentException.class, () -> ranged.setLightningFreezeTicks(-1));
  }

  @Test
  void shouldRejectNegativePetrifyTicks() {
    RangedAttackComponent ranged = new RangedAttackComponent(6f, 2.5f, 1f, createInstantWeapon());
    assertThrows(IllegalArgumentException.class, () -> ranged.setPetrifyTicks(-1));
  }

  @Test
  void shouldSourceLightningDamageFromWeaponNotHardcodedValue() {
    // Regression test: previously this branch called ArrowFactory.createLightning(...) with a
    // literal damage of 3, ignoring whatever weapon/natural-weapon damage the shooter was actually
    // configured with. Zeus's lightning must scale with his own config like every other attack
    // does, not a magic number shared by every LIGHTNING shooter in the game.
    RangedAttackComponent rangedComponent = new RangedAttackComponent(6f, 2f, 0f, createWeapon(42));
    Entity bolt = fireAndCapture(ProjectileType.LIGHTNING, rangedComponent);
    assertEquals(42, bolt.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldApplyConfiguredLightningFreezeTicksNotTheDefault() {
    RangedAttackComponent rangedComponent = new RangedAttackComponent(6f, 2f, 0f, createWeapon(5));
    rangedComponent.setLightningFreezeTicks(50);
    Entity bolt = fireAndCapture(ProjectileType.LIGHTNING, rangedComponent);
    // The mocked EntityService used by fireAndCapture doesn't call create() the way the real one
    // would, so LightningFreezeComponent's own "projectileHit" listener needs registering here.
    bolt.create();

    PlayerActions playerActions = mockPlayerActionsTarget();
    Entity target =
        new Entity().addComponent(playerActions).addComponent(new SpeedEffectComponent());
    target.create();
    bolt.getEvents().trigger("projectileHit", target);
    SpeedEffectComponent speed = target.getComponent(SpeedEffectComponent.class);

    for (int i = 0; i < 49; i++) speed.update();
    verify(playerActions, never()).removeSpeedModifier(any());
    speed.update();
    verify(playerActions).removeSpeedModifier(any());
  }

  @Test
  void shouldFireGazeAndApplyConfiguredPetrifyTicksNotTheDefault() {
    RangedAttackComponent rangedComponent = new RangedAttackComponent(6f, 2f, 0f, createWeapon(5));
    rangedComponent.setPetrifyTicks(30);
    Entity gaze = fireAndCapture(ProjectileType.GAZE, rangedComponent);
    // The mocked EntityService used by fireAndCapture doesn't call create() the way the real one
    // would, so PetrifyEffectComponent's own "projectileHit" listener needs registering here.
    gaze.create();

    PlayerActions playerActions = mockPlayerActionsTarget();
    Entity target =
        new Entity().addComponent(playerActions).addComponent(new SpeedEffectComponent());
    target.create();
    gaze.getEvents().trigger("projectileHit", target);
    SpeedEffectComponent speed = target.getComponent(SpeedEffectComponent.class);

    for (int i = 0; i < 29; i++) speed.update();
    verify(playerActions, never()).removeSpeedModifier(any());
    speed.update();
    verify(playerActions).removeSpeedModifier(any());
  }

  /**
   * Fires {@code rangedComponent} at a fresh target and returns the single spawned projectile
   * entity, captured via a mocked {@link EntityService}.
   */
  private Entity fireAndCapture(ProjectileType projectile, RangedAttackComponent rangedComponent) {
    EntityService entityService = mock(EntityService.class);
    List<Entity> registered = new ArrayList<>();
    doAnswer(
            invocation -> {
              registered.add(invocation.getArgument(0));
              return null;
            })
        .when(entityService)
        .register(any(Entity.class));
    ServiceLocator.registerEntityService(entityService);

    Entity attacker =
        new Entity().addComponent(rangedComponent).addComponent(new CombatStatsComponent(20, 5));
    attacker.create();
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);

    attacker.getEvents().trigger("rangedAttack", target, projectile);

    assertEquals(1, registered.size());
    return registered.get(0);
  }

  private WeaponItem createWeapon(int damage) {
    return WeaponItem.natural("Test Natural Weapon", damage, 0f);
  }

  private PlayerActions mockPlayerActionsTarget() {
    PlayerActions playerActions = mock(PlayerActions.class);
    when(playerActions.getPrio()).thenReturn(ComponentPriority.LOW);
    return playerActions;
  }

  private List<Entity> listenForFired(Entity attacker) {
    List<Entity> fired = new ArrayList<>();
    attacker.getEvents().addListener("rangedAttackFired", (Entity t) -> fired.add(t));
    return fired;
  }

  /**
   * Builds the default weapon used by {@link #createAttacker(float, float, float)}: a non-BOW type
   * with zero windup, so an attack resolves on the very next {@code update()} call after being
   * triggered. Deals {@link WeaponTier#TIER_1} (7) damage per hit.
   *
   * @return a fresh weapon item suitable for most tests
   */
  WeaponItem createInstantWeapon() {
    return new WeaponItem("Test Bow", WeaponType.BOW, WeaponTier.TIER_1, 1, 1, 0f);
  }

  private Entity createAttacker(float range, float cooldown, float knockback) {
    Entity attacker =
        new Entity()
            .addComponent(
                new RangedAttackComponent(range, cooldown, knockback, createInstantWeapon()))
            .addComponent(new CombatStatsComponent(20, 5));
    attacker.create();
    return attacker;
  }

  private Entity createTarget() {
    Entity target = new Entity().addComponent(new CombatStatsComponent(10, 0));
    target.create();
    return target;
  }
}
