package com.csse3200.game.components.player;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.utils.math.Vector2Utils;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class DashTest {
  PhysicsEngine engine = new PhysicsEngine();

  @Test
  public void bodyDashesRight() {
    PhysicsService service = new PhysicsService(engine);
    ServiceLocator.registerPhysicsService(service);
    Entity entity = new Entity();
    PhysicsComponent physics = new PhysicsComponent();
    entity.addComponent(physics);
    PlayerActions playerActions = new PlayerActions();
    entity.addComponent(playerActions);
    playerActions.create();
    playerActions.dash(Vector2Utils.RIGHT);
    Assertions.assertEquals(5f, physics.getBody().getLinearVelocity().x);
  }

  @Test
  public void bodyDashesLeft() {
    PhysicsService service = new PhysicsService(engine);
    ServiceLocator.registerPhysicsService(service);
    Entity entity = new Entity();
    PhysicsComponent physics = new PhysicsComponent();
    entity.addComponent(physics);
    PlayerActions playerActions = new PlayerActions();
    entity.addComponent(playerActions);
    playerActions.create();
    playerActions.dash(Vector2Utils.LEFT);
    Assertions.assertEquals(-5f, physics.getBody().getLinearVelocity().x);
  }
}
