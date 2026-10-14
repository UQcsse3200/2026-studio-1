package com.csse3200.game.areas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.areas.terrain.map.JsonMapLoader;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.components.lighting.LightColour;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.services.LightService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class LevelGameAreaLightingTest {
  private static final float EPS = 1e-4f;
  private static final int BUDGET = readLightBudget();

  private static final String LEGEND =
      """
            {
              "#": { "type": "WALL", "texture": "wall.png" },
              "A": { "type": "DECORATIVE", "texture": "lamp.png", "light": "point",
                     "lightColour": "oil_lamp", "lightRadius": "2" }
            }
            """;

  private static final String SUBLEVELS =
      """
            [
              { "id": "dungeon", "title": "DUNGEON", "bounds": { "x": 0, "y": 0, "width": 8, "height": 4 } },
              { "id": "nether",  "title": "NETHER",  "bounds": { "x": 0, "y": 4, "width": 8, "height": 4 } }
            ]
            """;

  private static final String LIGHTING_WITH_SUBLEVELS =
      """
            {
              "ambient": { "color": "background_dark", "intensity": 0.5 },
              "player":  { "radius": 3.5, "color": "resinous_torch" },
              "subLevels": {
                "dungeon": { "ambient": { "color": "oil_lamp",  "intensity": 0.25 } },
                "nether":  { "ambient": { "color": "lava_glow", "intensity": 0.125 } }
              }
            }
            """;

  private static final String SIMPLE_LIGHTING =
      "{\"ambient\":{\"color\":\"background_dark\",\"intensity\":0.25},"
          + "\"player\":{\"radius\":3.5,\"color\":\"resinous_torch\"}}";

  private MockedConstruction<PointLight> pointLights;
  private final List<List<Object>> lights = new ArrayList<>();
  private LightService lightService;
  private TerrainComponent terrain;
  private LevelGameArea area;

  @BeforeEach
  void setUp() {
    lightService = mock(LightService.class);
    when(lightService.getRayHandler()).thenReturn(mock(RayHandler.class));
    ServiceLocator.registerLightService(lightService);
    ServiceLocator.registerEntityService(new EntityService());

    // Intercepts `new PointLight(...)` inside LightComponent, so no GL is needed.
    pointLights =
        mockConstruction(
            PointLight.class, (light, context) -> lights.add(new ArrayList<>(context.arguments())));

    terrain = mock(TerrainComponent.class);
    when(terrain.tileToWorldPosition(0, 0)).thenReturn(new Vector2(0f, 0f));

    area = new LevelGameArea(mock(TerrainFactory.class), "maps/test.json");
  }

  @AfterEach
  void tearDown() {
    pointLights.close();
    ServiceLocator.clear();
  }

  // ---- helpers ----

  private static int readLightBudget() {
    try {
      Field f = LevelGameArea.class.getDeclaredField("LIGHT_BUDGET");
      f.setAccessible(true);
      return f.getInt(null);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("LIGHT_BUDGET not found", e);
    }
  }

  private static LevelMapData levelMap(String lighting, String subLevels, String... rows) {
    String rowsJson =
        Arrays.stream(rows).map(r -> "\"" + r + "\"").collect(Collectors.joining(","));
    String json =
        "{\"name\":\"test\",\"tileSize\":0.5,\"legend\":"
            + LEGEND
            + (lighting == null ? "" : ",\"lighting\":" + lighting)
            + (subLevels == null ? "" : ",\"subLevels\":" + subLevels)
            + ",\"layers\":{\"terrain\":["
            + rowsJson
            + "]}}";
    return new JsonMapLoader().parse(json);
  }

  private static String[] blank(int rows) {
    String[] out = new String[rows];
    Arrays.fill(out, "        ");
    return out;
  }

  /** Lamps that never touch each other, so each one is its own emitter. */
  private static String[] isolatedLamps(int count) {
    int perRow = 20;
    int lampRows = (count + perRow - 1) / perRow;
    String[] rows = new String[lampRows * 2 - 1];
    int remaining = count;
    for (int r = 0; r < rows.length; r++) {
      if (r % 2 == 1) {
        rows[r] = " ".repeat(perRow * 2);
        continue;
      }
      int n = Math.min(perRow, remaining);
      remaining -= n;
      rows[r] = "A ".repeat(n);
    }
    return rows;
  }

  private static Entity playerAt(float x, float y) {
    Entity player = mock(Entity.class);
    when(player.getCenterPosition()).thenReturn(new Vector2(x, y));
    return player;
  }

  private void load(LevelMapData data, Entity player) throws ReflectiveOperationException {
    setField("mapData", data);
    setField("player", player);
    area.terrain = terrain;
  }

  private void setField(String name, Object value) throws ReflectiveOperationException {
    Field f = LevelGameArea.class.getDeclaredField(name);
    f.setAccessible(true);
    f.set(area, value);
  }

  private void spawnLighting() throws Exception {
    Method m = LevelGameArea.class.getDeclaredMethod("spawnLighting");
    m.setAccessible(true);
    try {
      m.invoke(area);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Error error) throw error;
      if (cause instanceof Exception exception) throw exception;
      throw e;
    }
  }

  // PointLight(rayHandler, rays, color, distance, x, y)
  private int raysOf(int light) {
    return (int) lights.get(light).get(1);
  }

  private Color colourOf(int light) {
    return (Color) lights.get(light).get(2);
  }

  private float radiusOf(int light) {
    return (float) lights.get(light).get(3);
  }

  private float xOf(int light) {
    return (float) lights.get(light).get(4);
  }

  private float yOf(int light) {
    return (float) lights.get(light).get(5);
  }

  /** Tile lights are spawned first and the player light last. */
  private int tileLightCount() {
    return lights.size() - 1;
  }

  private int playerLight() {
    return lights.size() - 1;
  }

  // ---- maps without a lighting block ----

  @Test
  void noLightingBlock_resetsAmbientToTheDefault() throws Exception {
    load(levelMap(null, null, blank(3)), null);

    spawnLighting();

    verify(lightService).setAmbient(LightColour.AMBIENT_LIGHT.getColour(), 1f);
  }

  @Test
  void noLightingBlock_spawnsNoLightsAndNeverFades() throws Exception {
    load(levelMap(null, null, "A   ", "    "), null);

    spawnLighting();

    assertTrue(lights.isEmpty());
    assertTrue(area.areaEntities.isEmpty());
    verify(lightService, never()).fadeAmbientTo(any(Color.class), anyFloat(), anyFloat());
  }

  // ---- ambient on load ----

  @Test
  void ambient_comesFromTheMapWideConfig() throws Exception {
    load(levelMap(SIMPLE_LIGHTING, null, blank(3)), playerAt(1f, 0.5f));

    spawnLighting();

    verify(lightService).setAmbient(LightColour.AMBIENT_DARK.getColour(), 0.25f);
    verify(lightService, never()).fadeAmbientTo(any(Color.class), anyFloat(), anyFloat());
  }

  @Test
  void startingInTheFirstSubLevel_usesItsAmbient() throws Exception {
    load(levelMap(LIGHTING_WITH_SUBLEVELS, SUBLEVELS, blank(8)), playerAt(1f, 0.75f)); // row 1

    spawnLighting();

    verify(lightService).setAmbient(LightColour.OIL_LAMP.getColour(), 0.25f);
  }

  @Test
  void startingInTheSecondSubLevel_usesItsAmbient() throws Exception {
    load(levelMap(LIGHTING_WITH_SUBLEVELS, SUBLEVELS, blank(8)), playerAt(1f, 2.75f)); // row 5

    spawnLighting();

    verify(lightService).setAmbient(LightColour.LAVA_GLOW.getColour(), 0.125f);
  }

  @Test
  void startingOutsideEverySubLevel_usesTheMapWideAmbient() throws Exception {
    load(levelMap(LIGHTING_WITH_SUBLEVELS, SUBLEVELS, blank(8)), playerAt(1f, 50f));

    spawnLighting();

    verify(lightService).setAmbient(LightColour.AMBIENT_DARK.getColour(), 0.5f);
  }

  @Test
  void mapWithoutSubLevels_usesTheMapWideAmbient() throws Exception {
    load(levelMap(LIGHTING_WITH_SUBLEVELS, null, blank(8)), playerAt(1f, 0.75f));

    spawnLighting();

    verify(lightService).setAmbient(LightColour.AMBIENT_DARK.getColour(), 0.5f);
  }

  // ---- tile lights ----

  @Test
  void emitterTile_becomesALightAtItsCentre_offsetByTheTerrainOrigin() throws Exception {
    when(terrain.tileToWorldPosition(0, 0)).thenReturn(new Vector2(10f, 20f));
    load(levelMap(SIMPLE_LIGHTING, null, "    ", "  A ", "    "), playerAt(1f, 1f));

    spawnLighting();

    assertEquals(2, lights.size()); // one tile light, one player light
    assertEquals(10f + 2.5f * 0.5f, xOf(0), EPS);
    assertEquals(20f + 1.5f * 0.5f, yOf(0), EPS);
    assertEquals(2f, radiusOf(0), EPS);
    assertEquals(16, raysOf(0));
    assertEquals(LightColour.OIL_LAMP.getColour(), colourOf(0));
  }

  @Test
  void missingTerrainOrigin_skipsTileLights_butStillSpawnsThePlayerLight() throws Exception {
    when(terrain.tileToWorldPosition(0, 0)).thenReturn(null);
    load(levelMap(SIMPLE_LIGHTING, null, "    ", "  A ", "    "), playerAt(4f, 5f));

    spawnLighting();

    assertEquals(1, lights.size());
    assertEquals(4f, xOf(0), EPS);
    assertEquals(5f, yOf(0), EPS);
  }

  @Test
  void everyLightIsRegisteredAndOwnedByTheArea() throws Exception {
    load(levelMap(SIMPLE_LIGHTING, null, "    ", "  A ", "    "), playerAt(1f, 1f));

    spawnLighting();

    assertEquals(lights.size(), area.areaEntities.size());
  }

  // ---- player light ----

  @Test
  void playerLight_usesTheConfiguredSpecAndStartsOnThePlayer() throws Exception {
    load(levelMap(SIMPLE_LIGHTING, null, blank(3)), playerAt(3f, 2f));

    spawnLighting();

    assertEquals(1, lights.size());
    assertEquals(3f, xOf(playerLight()), EPS);
    assertEquals(2f, yOf(playerLight()), EPS);
    assertEquals(3.5f, radiusOf(playerLight()), EPS);
    assertEquals(28, raysOf(playerLight()));
    assertEquals(LightColour.RESINOUS_TORCH.getColour(), colourOf(playerLight()));
  }

  @Test
  void playerLight_isSpawnedAfterTheTileLights() throws Exception {
    load(levelMap(SIMPLE_LIGHTING, null, isolatedLamps(3)), playerAt(7f, 8f));

    spawnLighting();

    assertEquals(4, lights.size());
    assertEquals(7f, xOf(playerLight()), EPS);
    assertEquals(8f, yOf(playerLight()), EPS);
  }

  // ---- light budget ----

  @Test
  void atTheBudget_everyEmitterKeepsItsLight() throws Exception {
    load(levelMap(SIMPLE_LIGHTING, null, isolatedLamps(BUDGET)), playerAt(1f, 1f));

    spawnLighting();

    assertEquals(BUDGET, tileLightCount());
  }

  @Test
  void justOverTheBudget_isThinnedToWithinIt() throws Exception {
    load(levelMap(SIMPLE_LIGHTING, null, isolatedLamps(BUDGET + 1)), playerAt(1f, 1f));

    spawnLighting();

    assertTrue(tileLightCount() <= BUDGET, "spawned " + tileLightCount());
    assertTrue(tileLightCount() >= (BUDGET + 1) / 2, "thinned too far: " + tileLightCount());
  }

  @Test
  void farOverTheBudget_stillStaysWithinIt() throws Exception {
    load(levelMap(SIMPLE_LIGHTING, null, isolatedLamps(BUDGET * 2 + 1)), playerAt(1f, 1f));

    spawnLighting();

    assertTrue(tileLightCount() <= BUDGET, "spawned " + tileLightCount());
    assertTrue(tileLightCount() > 0);
  }

  @Test
  void thinning_neverDropsThePlayerLight() throws Exception {
    load(levelMap(SIMPLE_LIGHTING, null, isolatedLamps(BUDGET * 2)), playerAt(6f, 9f));

    spawnLighting();

    assertEquals(6f, xOf(playerLight()), EPS);
    assertEquals(9f, yOf(playerLight()), EPS);
  }

  // ---- fadeAmbientForSection ----

  @Test
  void fade_onAMapWithoutLighting_doesNothing() throws Exception {
    load(levelMap(null, null, blank(3)), null);

    area.fadeAmbientForSection("dungeon", 0.5f);

    verify(lightService, never()).fadeAmbientTo(any(Color.class), anyFloat(), anyFloat());
  }

  @Test
  void fade_goesToTheSubLevelsAmbient() throws Exception {
    load(levelMap(LIGHTING_WITH_SUBLEVELS, SUBLEVELS, blank(8)), null);

    area.fadeAmbientForSection("nether", 0.5f);

    verify(lightService).fadeAmbientTo(LightColour.LAVA_GLOW.getColour(), 0.125f, 0.5f);
  }

  @Test
  void fade_passesTheRequestedDuration() throws Exception {
    load(levelMap(LIGHTING_WITH_SUBLEVELS, SUBLEVELS, blank(8)), null);

    area.fadeAmbientForSection("dungeon", 1.5f);

    verify(lightService).fadeAmbientTo(LightColour.OIL_LAMP.getColour(), 0.25f, 1.5f);
  }

  @Test
  void fade_toAnUnknownOrNullSection_usesTheMapWideAmbient() throws Exception {
    load(levelMap(LIGHTING_WITH_SUBLEVELS, SUBLEVELS, blank(8)), null);

    area.fadeAmbientForSection("missing", 0.5f);
    area.fadeAmbientForSection(null, 0.5f);

    verify(lightService, org.mockito.Mockito.times(2))
        .fadeAmbientTo(LightColour.AMBIENT_DARK.getColour(), 0.5f, 0.5f);
  }

  @Test
  void fade_neverSnapsTheAmbient() throws Exception {
    load(levelMap(LIGHTING_WITH_SUBLEVELS, SUBLEVELS, blank(8)), null);

    area.fadeAmbientForSection("nether", 0.5f);

    verify(lightService, never()).setAmbient(any(Color.class), anyFloat());
  }
}
