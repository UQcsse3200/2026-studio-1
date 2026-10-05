package com.csse3200.game.components.lighting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.areas.terrain.map.JsonMapLoader;
import com.csse3200.game.areas.terrain.map.MapLayerData;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import com.csse3200.game.components.lighting.EmitterScanner.LightPlacement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EmitterScannerTest {
  private static final float EPS = 1e-4f;

  private static final String LEGEND =
      """
            {
              "#": { "type": "WALL", "texture": "wall.png" },
              "~": { "type": "HAZARD", "texture": "lava.png", "light": "point",
                     "lightColour": "lava_glow", "lightRadius": "2", "flicker": "0.2" },
              "A": { "type": "DECORATIVE", "texture": "lamp.png", "light": "point",
                     "lightColour": "oil_lamp", "lightRadius": "2" }
            }
            """;

  /** Builds a mask from strings where '#' is set. The first string is y = 0. */
  private static boolean[][] mask(String... rows) {
    int width = rows[0].length();
    boolean[][] m = new boolean[width][rows.length];
    for (int y = 0; y < rows.length; y++) {
      for (int x = 0; x < width; x++) {
        m[x][y] = rows[y].charAt(x) == '#';
      }
    }
    return m;
  }

  private static TileDefinition tile(Map<String, String> props) {
    return new TileDefinition(TileType.DECORATIVE, null, props);
  }

  /** Parses real layers. Rows are listed top-to-bottom, as in a map file. */
  private static List<MapLayerData> layers(LinkedHashMap<String, String[]> rowsByLayer) {
    String layersJson =
        rowsByLayer.entrySet().stream()
            .map(
                e ->
                    "\""
                        + e.getKey()
                        + "\": ["
                        + Arrays.stream(e.getValue())
                            .map(r -> "\"" + r + "\"")
                            .collect(Collectors.joining(","))
                        + "]")
            .collect(Collectors.joining(","));
    String json =
        "{\"name\":\"test\",\"tileSize\":0.5,\"legend\":"
            + LEGEND
            + ",\"layers\":{"
            + layersJson
            + "}}";
    return new JsonMapLoader().parse(json).getLayers();
  }

  private static List<MapLayerData> layer(String... rows) {
    LinkedHashMap<String, String[]> m = new LinkedHashMap<>();
    m.put("terrain", rows);
    return layers(m);
  }

  @Nested
  class FindRects {
    @Test
    void emptyMask_givesNoRects() {
      assertTrue(EmitterScanner.findRects(mask("...", "...")).isEmpty());
    }

    @Test
    void singleCell_givesOneRect() {
      assertEquals(
          List.of(new EmitterScanner.TileRect(0, 0, 1, 1)), EmitterScanner.findRects(mask("#")));
    }

    @Test
    void horizontalStrip_givesOneWideRect() {
      assertEquals(
          List.of(new EmitterScanner.TileRect(0, 0, 5, 1)),
          EmitterScanner.findRects(mask("#####")));
    }

    @Test
    void verticalStrip_givesOneTallRect() {
      assertEquals(
          List.of(new EmitterScanner.TileRect(0, 0, 1, 3)),
          EmitterScanner.findRects(mask("#", "#", "#")));
    }

    @Test
    void twoByFourteenBlock_givesOneRect() {
      String[] rows = new String[14];
      Arrays.fill(rows, "##");
      assertEquals(
          List.of(new EmitterScanner.TileRect(0, 0, 2, 14)), EmitterScanner.findRects(mask(rows)));
    }

    @Test
    void lShape_givesTwoRects() {
      List<EmitterScanner.TileRect> rects = EmitterScanner.findRects(mask("###", "#..", "#.."));
      assertEquals(
          List.of(new EmitterScanner.TileRect(0, 0, 3, 1), new EmitterScanner.TileRect(0, 1, 1, 2)),
          rects);
    }

    @Test
    void disjointCells_giveSeparateRects() {
      assertEquals(
          List.of(new EmitterScanner.TileRect(0, 0, 1, 1), new EmitterScanner.TileRect(2, 0, 1, 1)),
          EmitterScanner.findRects(mask("#.#")));
    }

    @Test
    void consumesTheMask() {
      boolean[][] m = mask("##", "##");
      EmitterScanner.findRects(m);
      for (boolean[] column : m) {
        for (boolean cell : column) {
          assertTrue(!cell);
        }
      }
    }

    @Test
    void irregularShape_coversEveryCellExactlyOnce() {
      String[] shape = {"##.##", "#####", ".###.", "#...#"};
      boolean[][] original = mask(shape);
      List<EmitterScanner.TileRect> rects = EmitterScanner.findRects(mask(shape));

      int[][] covered = new int[original.length][original[0].length];
      for (EmitterScanner.TileRect r : rects) {
        for (int x = r.x(); x < r.x() + r.w(); x++) {
          for (int y = r.y(); y < r.y() + r.h(); y++) {
            covered[x][y]++;
          }
        }
      }
      for (int x = 0; x < original.length; x++) {
        for (int y = 0; y < original[0].length; y++) {
          assertEquals(original[x][y] ? 1 : 0, covered[x][y], "cell " + x + "," + y);
        }
      }
    }
  }

  @Nested
  class SpecFor {
    @Test
    void nullDefinition_givesNull() {
      assertNull(EmitterScanner.specFor(null));
    }

    @Test
    void tileWithoutLightKey_givesNull() {
      assertNull(EmitterScanner.specFor(tile(Map.of("lightRadius", "3"))));
    }

    @Test
    void lightKeyOnly_usesDefaults() {
      LightSpec spec = EmitterScanner.specFor(tile(Map.of("light", "point")));
      assertNotNull(spec);
      assertEquals(Color.WHITE, spec.color());
      assertEquals(2f, spec.radius(), 1e-6f);
      assertEquals(16, spec.rays());
      assertEquals(0f, spec.flicker(), 1e-6f);
    }

    @Test
    void allProperties_areRead() {
      LightSpec spec =
          EmitterScanner.specFor(
              tile(
                  Map.of(
                      "light", "point",
                      "lightColour", "oil_lamp",
                      "lightRadius", "3",
                      "flicker", "0.25")));
      assertEquals(LightColour.OIL_LAMP.getColour(), spec.color());
      assertEquals(3f, spec.radius(), 1e-6f);
      assertEquals(24, spec.rays());
      assertEquals(0.25f, spec.flicker(), 1e-6f);
    }

    @Test
    void tinyRadius_isRaisedToMinimum() {
      LightSpec spec = EmitterScanner.specFor(tile(Map.of("light", "point", "lightRadius", "0.1")));
      assertEquals(0.5f, spec.radius(), 1e-6f);
      assertEquals(8, spec.rays());
    }

    @Test
    void flicker_isClampedToUnitRange() {
      assertEquals(
          1f,
          EmitterScanner.specFor(tile(Map.of("light", "point", "flicker", "5"))).flicker(),
          1e-6f);
      assertEquals(
          0f,
          EmitterScanner.specFor(tile(Map.of("light", "point", "flicker", "-2"))).flicker(),
          1e-6f);
    }

    @Test
    void unknownColour_fallsBackToWhite() {
      LightSpec spec =
          EmitterScanner.specFor(tile(Map.of("light", "point", "lightColour", "not_a_colour")));
      assertEquals(Color.WHITE, spec.color());
    }

    @Test
    void nonNumericRadius_fallsBackToDefault() {
      LightSpec spec = EmitterScanner.specFor(tile(Map.of("light", "point", "lightRadius", "big")));
      assertEquals(2f, spec.radius(), 1e-6f);
    }
  }

  @Nested
  class Scan {
    @Test
    void mapWithoutEmitters_givesNoPlacements() {
      assertTrue(EmitterScanner.scan(layer("###", "###"), 0.5f).isEmpty());
    }

    @Test
    void singleEmitter_isCentredOnItsTile() {
      List<LightPlacement> out = EmitterScanner.scan(layer("   ", " ~ ", "   "), 0.5f);
      assertEquals(1, out.size());
      assertEquals(1.5f, out.get(0).tileX(), EPS);
      assertEquals(1.5f, out.get(0).tileY(), EPS);
    }

    @Test
    void rowsAreFlipped_soTopRowIsHighestY() {
      List<LightPlacement> out = EmitterScanner.scan(layer("~  ", "   ", "   "), 0.5f);
      assertEquals(0.5f, out.get(0).tileX(), EPS);
      assertEquals(2.5f, out.get(0).tileY(), EPS);
    }

    @Test
    void placementCarriesTheTilesSpec() {
      LightPlacement p = EmitterScanner.scan(layer("~"), 0.5f).get(0);
      assertEquals(LightColour.LAVA_GLOW.getColour(), p.spec().color());
      assertEquals(2f, p.spec().radius(), 1e-6f);
      assertEquals(0.2f, p.spec().flicker(), 1e-6f);
    }

    @Test
    void nonEmittingTiles_areIgnored() {
      List<LightPlacement> out = EmitterScanner.scan(layer("#~#"), 0.5f);
      assertEquals(1, out.size());
      assertEquals(1.5f, out.get(0).tileX(), EPS);
    }

    @Test
    void horizontalStrip_isSpacedAlongItsLength() {
      // 10 tiles * 0.5 = 5 world units; spacing 0.75 * radius 2 = 1.5; ceil(5 / 1.5) = 4
      List<LightPlacement> out = EmitterScanner.scan(layer("~~~~~~~~~~"), 0.5f);
      assertEquals(4, out.size());
      float[] expectedX = {1.25f, 3.75f, 6.25f, 8.75f};
      for (int i = 0; i < 4; i++) {
        assertEquals(expectedX[i], out.get(i).tileX(), EPS);
        assertEquals(0.5f, out.get(i).tileY(), EPS);
      }
    }

    @Test
    void verticalStrip_isSpacedAlongItsHeight() {
      // 8 tiles * 0.5 = 4 world units; ceil(4 / 1.5) = 3
      String[] rows = new String[8];
      Arrays.fill(rows, "~");
      List<LightPlacement> out = EmitterScanner.scan(layer(rows), 0.5f);
      assertEquals(3, out.size());
      float[] expectedY = {8f / 6f, 4f, 8f * 5f / 6f};
      for (int i = 0; i < 3; i++) {
        assertEquals(0.5f, out.get(i).tileX(), EPS);
        assertEquals(expectedY[i], out.get(i).tileY(), EPS);
      }
    }

    @Test
    void tileSize_changesLightCount() {
      // 10 tiles * 1.0 = 10 world units; ceil(10 / 1.5) = 7
      assertEquals(7, EmitterScanner.scan(layer("~~~~~~~~~~"), 1.0f).size());
    }

    @Test
    void differentSymbols_stayAsSeparateEmitters() {
      List<LightPlacement> out = EmitterScanner.scan(layer("~A"), 0.5f);
      assertEquals(2, out.size());
      assertTrue(
          out.stream().anyMatch(p -> p.spec().color().equals(LightColour.LAVA_GLOW.getColour())));
      assertTrue(
          out.stream().anyMatch(p -> p.spec().color().equals(LightColour.OIL_LAMP.getColour())));
    }

    @Test
    void sameSymbolOnTwoLayers_isScannedPerLayer() {
      LinkedHashMap<String, String[]> m = new LinkedHashMap<>();
      m.put("terrain", new String[] {"~"});
      m.put("foreground", new String[] {"~"});
      assertEquals(2, EmitterScanner.scan(layers(m), 0.5f).size());
    }
  }
}
