package com.csse3200.game.components.npc;

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
class FlightComponentTest {

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  @Test
  void shouldZeroGravityScaleOnCreate() {
    PhysicsComponent physicsComponent = new PhysicsComponent();
    Entity entity = new Entity().addComponent(physicsComponent).addComponent(new FlightComponent());

    entity.create();

    Body body = physicsComponent.getBody();
    assertEquals(0f, body.getGravityScale());
  }

  @Test
  void shouldNotThrowWhenAddedWithoutAPhysicsComponent() {
    Entity entity = new Entity().addComponent(new FlightComponent());

    assertDoesNotThrow(entity::create);
  }

  @Test
  void shouldApplyLinearDampingWhenConfigured() {
    PhysicsComponent physicsComponent = new PhysicsComponent();
    Entity entity =
        new Entity().addComponent(physicsComponent).addComponent(new FlightComponent(2.5f));

    entity.create();

    Body body = physicsComponent.getBody();
    assertEquals(0f, body.getGravityScale());
    assertEquals(2.5f, body.getLinearDamping());
  }

  @Test
  void shouldDefaultToZeroDampingWhenUnspecified() {
    PhysicsComponent physicsComponent = new PhysicsComponent();
    Entity entity = new Entity().addComponent(physicsComponent).addComponent(new FlightComponent());

    entity.create();

    Body body = physicsComponent.getBody();
    assertEquals(0f, body.getLinearDamping());
  }

  @Test
  void shouldRejectNegativeDamping() {
    assertThrows(IllegalArgumentException.class, () -> new FlightComponent(-1f));
  }
}
