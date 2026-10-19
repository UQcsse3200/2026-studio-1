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
    for (MapLayerData layer : layers) {
      scanLayer(layer, tileSize, out);
    }
    return out;
  }

  /**
   * Finds the light source tiles in one layer and places lights over them
   *
   * @param layer the map layer to search
   * @param tileSize the size of tiles in the map
   * @param out the output list of light placements
   */
  private static void scanLayer(MapLayerData layer, float tileSize, List<LightPlacement> out) {
    Map<TileDefinition, LightSpec> specs = new IdentityHashMap<>();
    Map<TileDefinition, boolean[][]> masks = maskLightTiles(layer, specs);

    // for each light source
    for (Map.Entry<TileDefinition, boolean[][]> e : masks.entrySet()) {
      LightSpec spec = specs.get(e.getKey());
      // place light according to spec
      for (TileRect r : findRects(e.getValue())) place(r, spec, tileSize, out);
    }
  }

  /**
   * Marks which tiles in a layer are light sources, one mask per tile definition
   *
   * @param layer the map layer to search
   * @param specs filled with the light spec of each tile definition seen (null if not a light)
   * @return a mask of light source tiles for each light tile definition
   */
  private static Map<TileDefinition, boolean[][]> maskLightTiles(
      MapLayerData layer, Map<TileDefinition, LightSpec> specs) {
    Map<TileDefinition, boolean[][]> masks = new IdentityHashMap<>();

    // for each tile in the layer
    for (int x = 0; x < layer.getWidth(); x++) {
      for (int y = 0; y < layer.getHeight(); y++) {
        // get the tile definition
        TileDefinition def = layer.get(x, y);
        if (def == null) continue;

        // add the spec for the tile
        if (specs.computeIfAbsent(def, EmitterScanner::specFor) == null) continue;
        // puts true if the tile is a light source
        masks.computeIfAbsent(def, d -> new boolean[layer.getWidth()][layer.getHeight()])[x][y] =
            true;
      }
    }
    return masks;
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
        int rw = rectWidth(mask, x, y);
        // extend height the same way
        int rh = rectHeight(mask, x, y, rw);
        // make sure no additional single lights around
        clearRect(mask, x, y, rw, rh);
        rects.add(new TileRect(x, y, rw, rh));
      }
    }
    return rects;
  }

  /** How many light tiles run rightwards from (x, y), including it. */
  private static int rectWidth(boolean[][] mask, int x, int y) {
    int rw = 1;
    while (x + rw < mask.length && mask[x + rw][y]) rw++;
    return rw;
  }

  /** How many full rows of width rw run upwards from (x, y), including its own row. */
  private static int rectHeight(boolean[][] mask, int x, int y, int rw) {
    int rh = 1;
    while (y + rh < mask[0].length && isRowLit(mask, x, y + rh, rw)) rh++;
    return rh;
  }

  /** Whether every tile from x to x + rw - 1 in row y is a light source. */
  private static boolean isRowLit(boolean[][] mask, int x, int y, int rw) {
    for (int i = 0; i < rw; i++) {
      if (!mask[x + i][y]) return false;
    }
    return true;
  }

  /** Marks a rectangle of the mask as covered. */
  private static void clearRect(boolean[][] mask, int x, int y, int rw, int rh) {
    for (int i = 0; i < rw; i++) {
      for (int j = 0; j < rh; j++) mask[x + i][y + j] = false;
    }
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
