package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
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
 * Whether a fired arrow actually deals damage/knockback on contact is covered separately by {@link
 * com.csse3200.game.components.projectile.ProjectileHitComponentTest}, since {@link
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

    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);

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
    RangedAttackComponent ranged = new RangedAttackComponent(6f, 2.5f, 1f, 8f);
    assertEquals(6f, ranged.getRange());
    assertEquals(2.5f, ranged.getCooldown());
    assertEquals(1f, ranged.getKnockback());
    assertEquals(8f, ranged.getProjectileSpeed());
  }

  @Test
  void shouldRejectNonPositiveProjectileSpeed() {
    assertThrows(IllegalArgumentException.class, () -> new RangedAttackComponent(6f, 2.5f, 0f, 0f));
    assertThrows(
        IllegalArgumentException.class, () -> new RangedAttackComponent(6f, 2.5f, 0f, -1f));
  }

  @Test
  void shouldFireOnFirstAttackWithinRange() {
    Entity attacker = createAttacker(6f, 2f, 0f, 8f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target);

    assertEquals(1, fired.size());
  }

  @Test
  void shouldNotFireWhenTargetOutsideRange() {
    Entity attacker = createAttacker(2f, 1f, 0f, 8f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(10, 0);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target);

    assertEquals(0, fired.size());
  }

  @Test
  void shouldHandleTargetExactlyAtRangeBoundary() {
    Entity attacker = createAttacker(2f, 1f, 0f, 8f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target);

    assertEquals(1, fired.size());
  }

  @Test
  void shouldNotFireAtTargetWithoutCombatStatsComponent() {
    Entity attacker = createAttacker(6f, 1f, 0f, 8f);
    Entity targetWithoutStats = new Entity().addComponent(new PhysicsComponent());
    targetWithoutStats.create();
    attacker.setPosition(0, 0);
    targetWithoutStats.setPosition(2, 0);
    List<Entity> fired = listenForFired(attacker);

    assertDoesNotThrow(() -> attacker.getEvents().trigger("rangedAttack", targetWithoutStats));

    assertEquals(0, fired.size());
  }

  @Test
  void shouldNotFireDuringCooldown() {
    Entity attacker = createAttacker(6f, 2f, 0f, 8f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target);
    attacker.getEvents().trigger("rangedAttack", target);

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
    Entity attacker = createAttacker(6f, 2f, 0f, 8f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target);
    for (int i = 0; i < 101; i++) {
      attacker.update();
    }
    attacker.getEvents().trigger("rangedAttack", target);

    assertEquals(2, fired.size());
  }

  @Test
  void shouldNotThrowWhenTargetIsNull() {
    Entity attacker = createAttacker(6f, 2f, 0f, 8f);
    assertDoesNotThrow(() -> attacker.getEvents().trigger("rangedAttack", (Entity) null));
  }

  @Test
  void firingShouldNotThrowAndShouldSuccessfullyRegisterAnArrow() {
    // Smoke test for the full spawn pipeline: ArrowFactory.createRangedArrow(...) building a
    // physics-backed entity and EntityService actually being able to create() it without error.
    Entity attacker = createAttacker(6f, 2f, 1f, 8f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);

    assertDoesNotThrow(() -> attacker.getEvents().trigger("rangedAttack", target));
  }

  @Test
  void shouldAimLeftWhenTargetIsToTheLeft() {
    // Indirect check: firing at a target to the left must not throw despite the direction/spawn
    // offset math flipping sign compared to firing to the right.
    Entity attacker = createAttacker(6f, 2f, 0f, 8f);
    Entity target = createTarget();
    attacker.setPosition(5, 0);
    target.setPosition(0, 0);
    List<Entity> fired = listenForFired(attacker);

    assertDoesNotThrow(() -> attacker.getEvents().trigger("rangedAttack", target));

    assertEquals(1, fired.size());
  }

  private List<Entity> listenForFired(Entity attacker) {
    List<Entity> fired = new ArrayList<>();
    attacker.getEvents().addListener("rangedAttackFired", (Entity t) -> fired.add(t));
    return fired;
  }

  private Entity createAttacker(float range, float cooldown, float knockback, float speed) {
    Entity attacker =
        new Entity()
            .addComponent(new RangedAttackComponent(range, cooldown, knockback, speed))
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
