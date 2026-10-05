package com.csse3200.game.components.lighting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.LightService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

class LightComponentTest {
  private MockedConstruction<PointLight> pointLights;
  private final List<List<Object>> constructorArgs = new ArrayList<>();
  private RayHandler rayHandler;
  private Graphics previousGraphics;

  @BeforeEach
  void setUp() {
    rayHandler = mock(RayHandler.class);
    LightService lightService = mock(LightService.class);
    when(lightService.getRayHandler()).thenReturn(rayHandler);
    ServiceLocator.registerLightService(lightService);

    previousGraphics = Gdx.graphics;
    Gdx.graphics = mock(Graphics.class);
    when(Gdx.graphics.getDeltaTime()).thenReturn(0.016f);

    // Intercepts `new PointLight(...)` inside LightComponent, so no GL is needed.
    pointLights =
        mockConstruction(
            PointLight.class,
            (light, context) -> constructorArgs.add(new ArrayList<Object>(context.arguments())));
  }

  @AfterEach
  void tearDown() {
    pointLights.close();
    Gdx.graphics = previousGraphics;
    ServiceLocator.clear();
  }

  private PointLight light() {
    assertEquals(1, pointLights.constructed().size());
    return pointLights.constructed().get(0);
  }

  private static LightSpec spec(float radius, float flicker) {
    return LightSpec.of(Color.ORANGE, radius, flicker);
  }

  private static LightComponent fixed(LightSpec spec) {
    return new LightComponent(spec, new Vector2(4f, 5f));
  }

  private static Entity followedAt(Vector2 first, Vector2... rest) {
    Entity e = mock(Entity.class);
    when(e.getCenterPosition()).thenReturn(first, rest);
    return e;
  }

  // ---- create ----

  @Test
  void create_buildsPointLightFromSpecAtFixedPosition() {
    LightSpec spec = spec(3f, 0f);
    fixed(spec).create();

    List<Object> args = constructorArgs.get(0);
    assertSame(rayHandler, args.get(0));
    assertEquals(spec.rays(), (int) args.get(1));
    assertEquals(spec.color(), args.get(2));
    assertEquals(spec.radius(), (float) args.get(3), 1e-6f);
    assertEquals(4f, (float) args.get(4), 1e-6f);
    assertEquals(5f, (float) args.get(5), 1e-6f);
  }

  @Test
  void create_copiesTheFixedPosition() {
    Vector2 pos = new Vector2(1f, 2f);
    LightComponent component = new LightComponent(spec(2f, 0f), pos);
    pos.set(9f, 9f);
    component.create();

    assertEquals(1f, (float) constructorArgs.get(0).get(4), 1e-6f);
    assertEquals(2f, (float) constructorArgs.get(0).get(5), 1e-6f);
  }

  @Test
  void create_followLight_startsAtFollowedEntityCentre() {
    new LightComponent(spec(2f, 0f), followedAt(new Vector2(7f, 8f))).create();

    assertEquals(7f, (float) constructorArgs.get(0).get(4), 1e-6f);
    assertEquals(8f, (float) constructorArgs.get(0).get(5), 1e-6f);
  }

  @Test
  void create_makesLightXrayAndHardEdged() {
    fixed(spec(2f, 0f)).create();

    verify(light()).setXray(true);
    verify(light()).setSoft(false);
  }

  @Test
  void create_fixedSteadyLight_isStatic() {
    fixed(spec(2f, 0f)).create();
    verify(light()).setStaticLight(true);
  }

  @Test
  void create_fixedFlickeringLight_isNotStatic() {
    fixed(spec(2f, 0.3f)).create();
    verify(light()).setStaticLight(false);
  }

  @Test
  void create_followLight_isNotStatic() {
    new LightComponent(spec(2f, 0f), followedAt(new Vector2(0f, 0f))).create();
    verify(light()).setStaticLight(false);
  }

  // ---- update ----

  @Test
  void update_beforeCreate_doesNothing() {
    LightComponent component = fixed(spec(2f, 0.5f));
    assertDoesNotThrow(component::update);
  }

  @Test
  void update_fixedSteadyLight_neverMovesOrResizes() {
    LightComponent component = fixed(spec(2f, 0f));
    component.create();
    component.update();

    PointLight light = light();
    verify(light, never()).setPosition(any(Vector2.class));
    verify(light, never()).setPosition(anyFloat(), anyFloat());
    verify(light, never()).setDistance(anyFloat());
  }

  @Test
  void update_followLight_tracksTheEntity() {
    Entity target = followedAt(new Vector2(1f, 1f), new Vector2(3f, 4f));
    LightComponent component = new LightComponent(spec(2f, 0f), target);
    component.create();
    component.update();

    verify(light()).setPosition(new Vector2(3f, 4f));
  }

  @Test
  void update_flickerStaysWithinConfiguredBand() {
    LightComponent component = fixed(spec(4f, 0.5f));
    component.create();
    for (int i = 0; i < 300; i++) {
      component.update();
    }

    ArgumentCaptor<Float> distances = ArgumentCaptor.forClass(Float.class);
    verify(light(), times(300)).setDistance(distances.capture());

    float min = Float.MAX_VALUE;
    float max = -Float.MAX_VALUE;
    for (float d : distances.getAllValues()) {
      assertTrue(d >= 2f - 1e-3f && d <= 4f + 1e-3f, "distance out of band: " + d);
      min = Math.min(min, d);
      max = Math.max(max, d);
    }
    assertTrue(max - min > 0.01f, "flicker never varied");
  }

  // ---- dispose ----

  @Test
  void dispose_removesLightExactlyOnce() {
    LightComponent component = fixed(spec(2f, 0f));
    component.create();
    component.dispose();
    component.dispose();

    verify(light(), times(1)).remove();
  }

  @Test
  void dispose_beforeCreate_doesNotThrow() {
    LightComponent component = fixed(spec(2f, 0f));
    assertDoesNotThrow(component::dispose);
  }

  @Test
  void update_afterDispose_doesNotTouchTheLight() {
    LightComponent component = fixed(spec(2f, 0.5f));
    component.create();
    component.dispose();
    component.update();

    verify(light(), never()).setDistance(anyFloat());
  }
}
