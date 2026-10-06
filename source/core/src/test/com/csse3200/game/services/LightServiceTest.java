package com.csse3200.game.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.physics.box2d.World;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class LightServiceTest {
  private static final float EPS = 1e-5f;

  private MockedConstruction<RayHandler> handlers;
  private final List<List<Object>> constructorArgs = new ArrayList<>();
  private Graphics previousGraphics;
  private Graphics graphics;
  private World world;
  private OrthographicCamera camera;
  private LightService service;
  private RayHandler handler;

  @BeforeEach
  void setUp() {
    world = mock(World.class);
    camera = mock(OrthographicCamera.class);

    previousGraphics = Gdx.graphics;
    graphics = mock(Graphics.class);
    Gdx.graphics = graphics;

    // Intercepts `new RayHandler(world)` inside LightService, so no GL is needed.
    handlers =
        mockConstruction(
            RayHandler.class,
            (h, context) -> constructorArgs.add(new ArrayList<Object>(context.arguments())));

    service = new LightService(world);
    handler = handlers.constructed().get(0);
  }

  @AfterEach
  void tearDown() {
    handlers.close();
    Gdx.graphics = previousGraphics;
  }

  // ---- helpers ----

  /** Every setAmbientLight(r, g, b, a) call made on the handler so far, in order. */
  private List<float[]> ambientCalls() {
    return mockingDetails(handler).getInvocations().stream()
        .filter(
            i -> i.getMethod().getName().equals("setAmbientLight") && i.getArguments().length == 4)
        .map(
            i ->
                new float[] {
                  (float) i.getArguments()[0],
                  (float) i.getArguments()[1],
                  (float) i.getArguments()[2],
                  (float) i.getArguments()[3]
                })
        .toList();
  }

  private float[] lastAmbient() {
    List<float[]> calls = ambientCalls();
    assertTrue(!calls.isEmpty(), "no ambient call was made");
    return calls.get(calls.size() - 1);
  }

  private static void assertAmbient(float r, float g, float b, float a, float[] actual) {
    assertEquals(r, actual[0], EPS, "red");
    assertEquals(g, actual[1], EPS, "green");
    assertEquals(b, actual[2], EPS, "blue");
    assertEquals(a, actual[3], EPS, "intensity");
  }

  private void renderWithDelta(float delta) {
    when(graphics.getDeltaTime()).thenReturn(delta);
    service.render(camera);
  }

  private List<String> methodNames() {
    return mockingDetails(handler).getInvocations().stream()
        .map(i -> i.getMethod().getName())
        .toList();
  }

  // ---- construction ----

  @Test
  void constructor_buildsRayHandlerFromTheWorld() {
    assertEquals(1, handlers.constructed().size());
    assertSame(world, constructorArgs.get(0).get(0));
  }

  @Test
  void constructor_startsFullBright() {
    assertAmbient(1f, 1f, 1f, 1f, lastAmbient());
    assertEquals(1, ambientCalls().size());
  }

  @Test
  void constructor_enablesDiffuseLight() {
    try (MockedStatic<RayHandler> rayHandlerStatics = mockStatic(RayHandler.class)) {
      new LightService(world);
      rayHandlerStatics.verify(() -> RayHandler.useDiffuseLight(true));
    }
  }

  @Test
  void getRayHandler_returnsTheConstructedHandler() {
    assertSame(handler, service.getRayHandler());
  }

  // ---- setAmbient ----

  @Test
  void setAmbient_passesRgbAndUsesIntensityAsAlpha() {
    service.setAmbient(new Color(0.1f, 0.2f, 0.3f, 1f), 0.25f);
    assertAmbient(0.1f, 0.2f, 0.3f, 0.25f, lastAmbient());
  }

  @Test
  void setAmbient_ignoresTheColoursOwnAlpha() {
    service.setAmbient(new Color(0.1f, 0.2f, 0.3f, 0.9f), 0.25f);
    assertEquals(0.25f, lastAmbient()[3], EPS);
  }

  @Test
  void setAmbient_cancelsAFadeInProgress() {
    service.fadeAmbientTo(new Color(0f, 0f, 0f, 1f), 0f, 1f);
    renderWithDelta(0.5f);

    service.setAmbient(new Color(0.9f, 0.8f, 0.7f, 1f), 0.3f);
    renderWithDelta(10f);

    assertAmbient(0.9f, 0.8f, 0.7f, 0.3f, lastAmbient());
  }

  // ---- fadeAmbientTo ----

  @Test
  void fade_doesNothingUntilRendered() {
    int before = ambientCalls().size();
    service.fadeAmbientTo(new Color(0f, 0f, 0f, 1f), 0f, 1f);
    assertEquals(before, ambientCalls().size());
  }

  @Test
  void fade_fromUntouchedService_startsFromFullBright() {
    service.fadeAmbientTo(new Color(0f, 0f, 0f, 1f), 0f, 1f);
    renderWithDelta(0.5f);
    assertAmbient(0.5f, 0.5f, 0.5f, 0.5f, lastAmbient());
  }

  @Test
  void fade_interpolatesColourAndIntensity() {
    service.setAmbient(new Color(0.2f, 0.4f, 0.6f, 1f), 0.2f);
    service.fadeAmbientTo(new Color(0.6f, 0.8f, 1f, 1f), 0.6f, 2f);

    renderWithDelta(1f); // halfway

    assertAmbient(0.4f, 0.6f, 0.8f, 0.4f, lastAmbient());
  }

  @Test
  void fade_progressesAcrossFrames_thenStops() {
    service.setAmbient(new Color(0f, 0f, 0f, 1f), 0f);
    service.fadeAmbientTo(new Color(1f, 1f, 1f, 1f), 1f, 1f);

    float[] expected = {0.25f, 0.5f, 0.75f, 1f};
    for (float e : expected) {
      renderWithDelta(0.25f);
      assertAmbient(e, e, e, e, lastAmbient());
    }

    int callsAtEnd = ambientCalls().size();
    renderWithDelta(0.25f);
    assertEquals(callsAtEnd, ambientCalls().size(), "ambient was reapplied after the fade ended");
  }

  @Test
  void fade_overshootingTheDuration_landsExactlyOnTarget() {
    service.fadeAmbientTo(new Color(0.3f, 0.2f, 0.1f, 1f), 0.4f, 0.5f);
    renderWithDelta(10f);
    assertAmbient(0.3f, 0.2f, 0.1f, 0.4f, lastAmbient());
  }

  @Test
  void fade_interrupted_restartsFromTheCurrentValue() {
    service.fadeAmbientTo(new Color(0f, 0f, 0f, 1f), 0f, 1f);
    renderWithDelta(0.5f); // now (0.5, 0.5, 0.5), intensity 0.5

    service.fadeAmbientTo(new Color(1f, 0f, 0f, 1f), 0.5f, 1f);
    renderWithDelta(0.5f); // halfway from the interrupted value

    assertAmbient(0.75f, 0.25f, 0.25f, 0.5f, lastAmbient());
  }

  @Test
  void fade_withZeroDuration_snapsImmediately() {
    service.fadeAmbientTo(new Color(0.3f, 0.2f, 0.1f, 1f), 0.7f, 0f);
    assertAmbient(0.3f, 0.2f, 0.1f, 0.7f, lastAmbient());

    int calls = ambientCalls().size();
    renderWithDelta(1f);
    assertEquals(calls, ambientCalls().size(), "a zero-length fade left a fade running");
  }

  @Test
  void fade_withNegativeDuration_snapsImmediately() {
    service.fadeAmbientTo(new Color(0.3f, 0.2f, 0.1f, 1f), 0.7f, -1f);
    assertAmbient(0.3f, 0.2f, 0.1f, 0.7f, lastAmbient());
  }

  // ---- render ----

  @Test
  void render_setsCombinedMatrixThenUpdatesAndRenders() {
    renderWithDelta(0.016f);

    List<String> names = methodNames();
    int matrix = names.lastIndexOf("setCombinedMatrix");
    int draw = names.lastIndexOf("updateAndRender");
    assertTrue(matrix >= 0 && draw > matrix, "expected setCombinedMatrix before updateAndRender");
    verify(handler).setCombinedMatrix(camera);
    verify(handler).updateAndRender();
  }

  @Test
  void render_withoutAFade_leavesAmbientUntouched() {
    int before = ambientCalls().size();
    renderWithDelta(0.016f);
    assertEquals(before, ambientCalls().size());
  }

  @Test
  void render_duringAFade_appliesAmbientBeforeDrawing() {
    service.fadeAmbientTo(new Color(0f, 0f, 0f, 1f), 0f, 1f);
    renderWithDelta(0.25f);

    List<String> names = methodNames();
    assertTrue(
        names.lastIndexOf("setAmbientLight") < names.lastIndexOf("updateAndRender"),
        "ambient must be set before the lights are drawn");
  }

  // ---- resize and dispose ----

  @Test
  void resize_usesQuarterResolutionFramebuffer() {
    service.resize(800, 600);
    verify(handler).resizeFBO(200, 150);
  }

  @Test
  void resize_roundsDownOddSizes() {
    service.resize(803, 601);
    verify(handler).resizeFBO(200, 150);
  }

  @Test
  void dispose_disposesTheRayHandler() {
    service.dispose();
    verify(handler).dispose();
  }
}
