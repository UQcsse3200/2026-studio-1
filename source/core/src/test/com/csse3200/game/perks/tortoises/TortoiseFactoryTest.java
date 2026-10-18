package com.csse3200.game.perks.tortoises;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@link TortoiseFactory}: the tortoise entity it builds has the right parts, each tortoise
 * is a separate entity with its own id, and the found-state helpers read and clear the registry.
 *
 * <p>The atlas is the real {@code images/tortoise.atlas}; physics runs without a world step.
 */
@ExtendWith(GameExtension.class)
class TortoiseFactoryTest {
  @BeforeEach
  void setUp() {
    TortoiseTestSupport.assumePreferencesAvailable();
    TortoiseFactory.resetAll();
    TortoiseTestSupport.registerFactoryServices();
  }

  @AfterEach
  void tearDown() {
    TortoiseFactory.resetAll();
  }

  @Test
  void shouldBuildATortoiseWithEveryPart() {
    Entity tortoise = TortoiseFactory.createTortoise("level1_a");

    assertNotNull(tortoise.getComponent(AnimationRenderComponent.class));
    assertNotNull(tortoise.getComponent(PhysicsComponent.class));
    assertNotNull(tortoise.getComponent(ColliderComponent.class));
    assertNotNull(tortoise.getComponent(HitboxComponent.class));
    assertNotNull(tortoise.getComponent(TortoiseComponent.class));
  }

  @Test
  void shouldPlayTheIdleAnimation() {
    AnimationRenderComponent animator =
        TortoiseFactory.createTortoise("level1_a").getComponent(AnimationRenderComponent.class);

    assertTrue(animator.hasAnimation("idle"));
    assertEquals("idle", animator.getCurrentAnimation());
  }

  @Test
  void shouldPutTheTortoiseOnTheItemLayerAndOnlyCollideWithObstacles() {
    Entity tortoise = TortoiseFactory.createTortoise("level1_a");

    ColliderComponent collider = tortoise.getComponent(ColliderComponent.class);
    assertEquals(PhysicsLayer.ITEM, collider.getLayer());
    assertEquals(PhysicsLayer.OBSTACLE, collider.getMask());
    assertEquals(PhysicsLayer.ITEM, tortoise.getComponent(HitboxComponent.class).getLayer());
  }

  @Test
  void shouldBuildASeparateEntityEachTime() {
    Entity first = TortoiseFactory.createTortoise("level1_a");
    Entity second = TortoiseFactory.createTortoise("level1_a");

    assertNotSame(first, second);
    assertNotSame(
        first.getComponent(TortoiseComponent.class), second.getComponent(TortoiseComponent.class));
  }

  @Test
  void shouldGiveEachTortoiseItsOwnId() {
    Entity first = TortoiseFactory.createTortoise("level1_a");
    Entity second = TortoiseFactory.createTortoise("rnd_hound_s_den_1");

    assertEquals(
        "level1_a",
        TortoiseTestSupport.readField(first.getComponent(TortoiseComponent.class), "tortoiseId"));
    assertEquals(
        "rnd_hound_s_den_1",
        TortoiseTestSupport.readField(second.getComponent(TortoiseComponent.class), "tortoiseId"));
  }

  @Test
  void shouldNotCountATortoiseAsFoundUntilItIs() {
    TortoiseFactory.createTortoise("level1_a");

    assertFalse(TortoiseFactory.isFound("level1_a"));
  }

  @Test
  void shouldReportATortoiseAsFoundOnceTheRegistryHoldsIt() {
    TortoiseRegistry.markFound("level1_a");

    assertTrue(TortoiseFactory.isFound("level1_a"));
    assertFalse(TortoiseFactory.isFound("level1_b"));
  }

  @Test
  void shouldForgetEveryFoundTortoiseOnReset() {
    TortoiseRegistry.markFound("level1_a");
    TortoiseRegistry.markFound("rnd_hound_s_den_1");

    TortoiseFactory.resetAll();

    assertFalse(TortoiseFactory.isFound("level1_a"));
    assertFalse(TortoiseFactory.isFound("rnd_hound_s_den_1"));
  }

  @Test
  void shouldNotBeInstantiable() throws ReflectiveOperationException {
    Constructor<TortoiseFactory> constructor = TortoiseFactory.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    InvocationTargetException thrown =
        assertThrows(InvocationTargetException.class, constructor::newInstance);

    assertInstanceOf(IllegalStateException.class, thrown.getCause());
  }
}
