package com.csse3200.game.components.player;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ArrowMovementComponentTest {
  @Test
  void shouldMoveInStraightLineWithoutGravityOrSpeedLoss() {
    PhysicsEngine engine = mock(PhysicsEngine.class);
    Body body = mock(Body.class);
    when(engine.createBody(any())).thenReturn(body);
    ServiceLocator.registerPhysicsService(new PhysicsService(engine));
    ServiceLocator.registerEntityService(new EntityService());

    PhysicsComponent physics = new PhysicsComponent().setBodyType(BodyType.KinematicBody);
    Entity arrow =
        new Entity()
            .addComponent(physics)
            .addComponent(new ArrowMovementComponent(new Vector2(0f, 1f)));
    arrow.create();

    verify(body).setType(BodyType.KinematicBody);
    verify(body).setGravityScale(0f);
    verify(body).setLinearVelocity(eq(new Vector2(0f, 8f)));

    arrow.dispose();
  }
}
