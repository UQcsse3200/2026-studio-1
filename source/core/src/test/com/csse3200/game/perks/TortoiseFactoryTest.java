package com.csse3200.game.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/** Tests for {@link TortoiseFactory}. Requires Mockito's inline mock maker for mockStatic. */
class TortoiseFactoryTest {
  private ResourceService resourceService;

  @BeforeEach
  void setUp() {
    TextureAtlas atlas = mock(TextureAtlas.class);
    Array<AtlasRegion> idleFrames = new Array<>();
    idleFrames.add(mock(AtlasRegion.class));
    when(atlas.findRegions("idle")).thenReturn(idleFrames);

    resourceService = mock(ResourceService.class);
    when(resourceService.getAsset("images/tortoise.atlas", TextureAtlas.class)).thenReturn(atlas);
    ServiceLocator.registerResourceService(resourceService);

    // PhysicsComponent's default constructor pulls the engine from the PhysicsService and asks it
    // for a body, so the engine must hand back a non-null Body.
    PhysicsEngine engine = mock(PhysicsEngine.class);
    when(engine.createBody(any(BodyDef.class))).thenReturn(mock(Body.class));
    ServiceLocator.registerPhysicsService(new PhysicsService(engine));
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.clear();
  }

  @Test
  void createTortoiseLoadsTheTortoiseAtlas() {
    TortoiseFactory.createTortoise("level1_a");

    verify(resourceService).getAsset("images/tortoise.atlas", TextureAtlas.class);
  }

  @Test
  void createTortoiseHasAllRequiredComponents() {
    Entity tortoise = TortoiseFactory.createTortoise("level1_a");

    assertNotNull(tortoise.getComponent(AnimationRenderComponent.class));
    assertNotNull(tortoise.getComponent(PhysicsComponent.class));
    assertNotNull(tortoise.getComponent(ColliderComponent.class));
    assertNotNull(tortoise.getComponent(HitboxComponent.class));
    assertNotNull(tortoise.getComponent(TortoiseComponent.class));
  }

  @Test
  void createTortoisePlaysIdleAnimation() {
    Entity tortoise = TortoiseFactory.createTortoise("level1_a");

    AnimationRenderComponent animator = tortoise.getComponent(AnimationRenderComponent.class);
    // If your AnimationRenderComponent names this differently, delete or adapt this assertion.
    assertEquals("idle", animator.getCurrentAnimation());
  }

  @Test
  void createTortoiseReturnsDistinctEntitiesPerCall() {
    Entity first = TortoiseFactory.createTortoise("level1_a");
    Entity second = TortoiseFactory.createTortoise("level1_b");

    assertNotSame(first, second);
  }

  @Test
  void isFoundDelegatesToRegistry() {
    try (MockedStatic<TortoiseRegistry> registry = Mockito.mockStatic(TortoiseRegistry.class)) {
      registry.when(() -> TortoiseRegistry.isFound("found_one")).thenReturn(true);
      registry.when(() -> TortoiseRegistry.isFound("missing_one")).thenReturn(false);

      assertTrue(TortoiseFactory.isFound("found_one"));
      assertFalse(TortoiseFactory.isFound("missing_one"));
    }
  }

  @Test
  void resetAllDelegatesToRegistry() {
    try (MockedStatic<TortoiseRegistry> registry = Mockito.mockStatic(TortoiseRegistry.class)) {
      TortoiseFactory.resetAll();

      registry.verify(TortoiseRegistry::resetAll);
    }
  }

  @Test
  void constructorIsPrivateAndThrows() throws NoSuchMethodException {
    Constructor<TortoiseFactory> constructor = TortoiseFactory.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    InvocationTargetException thrown =
        assertThrows(InvocationTargetException.class, constructor::newInstance);

    assertInstanceOf(IllegalStateException.class, thrown.getCause());
  }
}
