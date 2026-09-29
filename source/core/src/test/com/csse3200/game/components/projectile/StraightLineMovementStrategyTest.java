package com.csse3200.game.components.projectile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class StraightLineMovementStrategyTest {

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  @Test
  void shouldRejectNonPositiveSpeed() {
    assertThrows(IllegalArgumentException.class, () -> new StraightLineMovementStrategy(0f, true));
    assertThrows(IllegalArgumentException.class, () -> new StraightLineMovementStrategy(-1f, true));
  }

  @Test
  void shouldSetPositiveHorizontalVelocityWhenMovingRight() {
    Entity projectile = createProjectile();
    new StraightLineMovementStrategy(5f, true).start(projectile);

    Body body = projectile.getComponent(PhysicsComponent.class).getBody();
    assertEquals(5f, body.getLinearVelocity().x, 0.001f);
    assertEquals(0f, body.getLinearVelocity().y, 0.001f, "Movement must be x-axis only");
  }

  @Test
  void shouldSetNegativeHorizontalVelocityWhenMovingLeft() {
    Entity projectile = createProjectile();
    new StraightLineMovementStrategy(5f, false).start(projectile);

    Body body = projectile.getComponent(PhysicsComponent.class).getBody();
    assertEquals(-5f, body.getLinearVelocity().x, 0.001f);
    assertEquals(0f, body.getLinearVelocity().y, 0.001f, "Movement must be x-axis only");
  }

  @Test
  void updateShouldBeANoOpAfterStart() {
    Entity projectile = createProjectile();
    StraightLineMovementStrategy strategy = new StraightLineMovementStrategy(5f, true);
    strategy.start(projectile);
    Body body = projectile.getComponent(PhysicsComponent.class).getBody();

    assertDoesNotThrow(() -> strategy.update(projectile, 0.02f));

    // Constant velocity persists on its own via the kinematic body - update() shouldn't touch it.
    assertEquals(5f, body.getLinearVelocity().x, 0.001f);
    assertEquals(0f, body.getLinearVelocity().y, 0.001f);
  }

  private Entity createProjectile() {
    Entity entity = new Entity().addComponent(new PhysicsComponent());
    entity.create();
    return entity;
  }
}
