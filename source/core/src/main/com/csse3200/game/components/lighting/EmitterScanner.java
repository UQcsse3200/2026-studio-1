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

  /** Returns the light a legend entry asks for, or null if it has none. */
  static LightSpec specFor(TileDefinition def) {
    if (def == null || !def.has("light")) return null;
    return LightSpec.of(
        LightColour.parse(def.get("lightColour"), Color.WHITE),
        def.getFloat("lightRadius", 2f),
        def.getFloat("flicker", 0f));
  }

  public static List<LightPlacement> scan(List<MapLayerData> layers, float tileSize) {
    List<LightPlacement> out = new ArrayList<>();
    for (MapLayerData layer : layers) {
      Map<TileDefinition, boolean[][]> masks = new IdentityHashMap<>();
      Map<TileDefinition, LightSpec> specs = new IdentityHashMap<>();

      for (int x = 0; x < layer.getWidth(); x++) {
        for (int y = 0; y < layer.getHeight(); y++) {
          TileDefinition def = layer.get(x, y);
          if (def == null) continue;
          if (!specs.containsKey(def)) specs.put(def, specFor(def));
          if (specs.get(def) == null) continue;
          masks.computeIfAbsent(def, d -> new boolean[layer.getWidth()][layer.getHeight()])[x][y] =
              true;
        }
      }

      for (Map.Entry<TileDefinition, boolean[][]> e : masks.entrySet()) {
        LightSpec spec = specs.get(e.getKey());
        for (TileRect r : findRects(e.getValue())) place(r, spec, tileSize, out);
      }
    }
    return out;
  }

  /** Greedy cover of the marked cells with rectangles. Consumes the mask. */
  static List<TileRect> findRects(boolean[][] mask) {
    List<TileRect> rects = new ArrayList<>();
    int w = mask.length;
    int h = mask[0].length;
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        if (!mask[x][y]) continue;
        int rw = 1;
        while (x + rw < w && mask[x + rw][y]) rw++;
        int rh = 1;
        outer:
        while (y + rh < h) {
          for (int i = 0; i < rw; i++) {
            if (!mask[x + i][y + rh]) break outer;
          }
          rh++;
        }
        for (int i = 0; i < rw; i++) {
          for (int j = 0; j < rh; j++) mask[x + i][y + j] = false;
        }
        rects.add(new TileRect(x, y, rw, rh));
      }
    }
    return rects;
  }

  /** Spaces lights along the long axis of a rectangle, about 0.75 radii apart. */
  private static void place(TileRect r, LightSpec spec, float tileSize, List<LightPlacement> out) {
    boolean horizontal = r.w() >= r.h();
    float lengthWorld = (horizontal ? r.w() : r.h()) * tileSize;
    int n = Math.max(1, (int) Math.ceil(lengthWorld / (0.75f * spec.radius())));
    for (int i = 0; i < n; i++) {
      float t = (i + 0.5f) / n;
      float tx = horizontal ? r.x() + r.w() * t : r.x() + r.w() / 2f;
      float ty = horizontal ? r.y() + r.h() / 2f : r.y() + r.h() * t;
      out.add(new LightPlacement(tx, ty, spec));
    }
  }
}
