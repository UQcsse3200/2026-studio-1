package com.csse3200.game.components.lighting;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.areas.terrain.map.MapLayerData;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class EmitterScanner {
  private EmitterScanner() {}

  /** A light position in tile units (tile x covers x..x+1) with its spec. */
  public record LightPlacement(float tileX, float tileY, LightSpec spec) {}

  record TileRect(int x, int y, int w, int h) {}

  /**
   * Converts the json/mapdata to a light spec record of the key info
   *
   * @param def takes the tile definition from the json legend
   * @return the lightspec (with the light colour (default white), light radius and flicker)
   */
  static LightSpec specFor(TileDefinition def) {
    if (def == null || !def.has("light")) return null;
    return LightSpec.of(
        LightColour.parse(def.get("lightColour"), Color.WHITE),
        def.getFloat("lightRadius", 2f),
        def.getFloat("flicker", 0f));
  }

  /**
   * Scans the map layers for tiles which are light sources
   *
   * @param layers the map layers (searches all layers)
   * @param tileSize the size of tiles in the map
   * @return list of the light locations
   */
  public static List<LightPlacement> scan(List<MapLayerData> layers, float tileSize) {
    List<LightPlacement> out = new ArrayList<>(); // output list

    // for each layer
    for (MapLayerData layer : layers) {
      Map<TileDefinition, boolean[][]> masks = new IdentityHashMap<>();
      Map<TileDefinition, LightSpec> specs = new IdentityHashMap<>();

      // for each tile in each layer
      for (int x = 0; x < layer.getWidth(); x++) {
        for (int y = 0; y < layer.getHeight(); y++) {
          // get the tile definition
          TileDefinition def = layer.get(x, y);
          if (def == null) continue;

          // add the spec for the tile
          if (!specs.containsKey(def)) specs.put(def, specFor(def));
          if (specs.get(def) == null) continue;
          // puts true if the tile is a light source
          masks.computeIfAbsent(def, d -> new boolean[layer.getWidth()][layer.getHeight()])[x][y] =
              true;
        }
      }

      // for each light source
      for (Map.Entry<TileDefinition, boolean[][]> e : masks.entrySet()) {
        LightSpec spec = specs.get(e.getKey());
        // place light according to spec
        for (TileRect r : findRects(e.getValue())) place(r, spec, tileSize, out);
      }
    }
    return out;
  }

  /**
   * Greedily covers the lights with rectangle masks
   *
   * @param mask the tiles of light sources
   * @return the list of tile rectangles
   */
  static List<TileRect> findRects(boolean[][] mask) {
    // ret list
    List<TileRect> rects = new ArrayList<>();
    int w = mask.length;
    int h = mask[0].length;

    // for each tile in the mask
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        if (!mask[x][y]) continue;

        // if its a light source
        // extend width if adjacent tiles are light sources
        int rw = 1;
        while (x + rw < w && mask[x + rw][y]) rw++;
        // extend height the same way
        int rh = 1;
        outer:
        while (y + rh < h) {
          for (int i = 0; i < rw; i++) {
            if (!mask[x + i][y + rh]) break outer;
          }
          rh++;
        }
        // make sure no additional single lights around
        for (int i = 0; i < rw; i++) {
          for (int j = 0; j < rh; j++) mask[x + i][y + j] = false;
        }
        rects.add(new TileRect(x, y, rw, rh));
      }
    }
    return rects;
  }

  /**
   * Places lights in a rectangle roughly spaced around, saves placements to output list
   *
   * @param r the rectangle
   * @param spec the type of light
   * @param tileSize the tileSize of the map
   * @param out the output list of light placements
   */
  private static void place(TileRect r, LightSpec spec, float tileSize, List<LightPlacement> out) {
    // if the rectangle is wider than the height
    boolean horizontal = r.w() >= r.h();
    // get longest side
    float lengthWorld = (horizontal ? r.w() : r.h()) * tileSize;
    // place lights 0.75 around the rectangle
    int n = Math.max(1, (int) Math.ceil(lengthWorld / (0.75f * spec.radius())));
    for (int i = 0; i < n; i++) {
      float t = (i + 0.5f) / n;
      float tx = horizontal ? r.x() + r.w() * t : r.x() + r.w() / 2f;
      float ty = horizontal ? r.y() + r.h() / 2f : r.y() + r.h() * t;
      out.add(new LightPlacement(tx, ty, spec));
    }
  }
}
