package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ObstacleFactoryTest {
  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  @Test
  void shouldCreateNonBlockingHazardSensor() {
    Entity hazard = ObstacleFactory.createHazardTile(1f, 1f);

    hazard.create();

    ColliderComponent collider = hazard.getComponent(ColliderComponent.class);
    PhysicsComponent physics = hazard.getComponent(PhysicsComponent.class);
    assertEquals(PhysicsLayer.HAZARD, collider.getLayer());
    assertTrue(collider.getFixture().isSensor());
    assertEquals(BodyType.StaticBody, physics.getBody().getType());
  }
}
