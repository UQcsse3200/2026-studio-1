package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DaggerFactoryTest {
  @BeforeEach
  void setUp() {
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(mock(Texture.class));
    ServiceLocator.registerResourceService(resources);
    PhysicsEngine engine = mock(PhysicsEngine.class);
    Body body = mock(Body.class);
    when(engine.createBody(any())).thenReturn(body);
    when(body.getGravityScale()).thenReturn(0f);
    ServiceLocator.registerPhysicsService(new PhysicsService(engine));
  }

  @Test
  void combatDaggerDoesNotFallUnderGravity() {
    Entity dagger =
        DaggerFactory.createDagger(new Vector2(1f, 2f), new Vector2(1f, 0f), 3, new Entity());

    assertEquals(
        0f, dagger.getComponent(PhysicsComponent.class).getBody().getGravityScale(), 1e-6f);
    verify(dagger.getComponent(PhysicsComponent.class).getBody()).setGravityScale(0f);
  }
}
