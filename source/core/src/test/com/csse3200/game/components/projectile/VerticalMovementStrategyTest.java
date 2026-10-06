package com.csse3200.game.components.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class VerticalMovementStrategyTest {
  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  private Entity createProjectile() {
    Entity e =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyDef.BodyType.KinematicBody));
    e.create();
    return e;
  }

  @Test
  void shouldRejectNonPositiveSpeed() {
    assertThrows(IllegalArgumentException.class, () -> new VerticalMovementStrategy(0f));
    assertThrows(IllegalArgumentException.class, () -> new VerticalMovementStrategy(-1f));
  }

  @Test
  void startShouldFallStraightDown() {
    Entity p = createProjectile();
    new VerticalMovementStrategy(12f).start(p);

    Vector2 v = p.getComponent(PhysicsComponent.class).getBody().getLinearVelocity();
    assertEquals(0f, v.x, 0.001f);
    assertEquals(-12f, v.y, 0.001f);
  }

  @Test
  void updateShouldNotChangeVelocity() {
    Entity p = createProjectile();
    VerticalMovementStrategy strategy = new VerticalMovementStrategy(12f);
    strategy.start(p);

    strategy.update(p, 0.016f);

    assertEquals(
        -12f, p.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().y, 0.001f);
  }
}
