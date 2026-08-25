package com.csse3200.game.areas.terrain;

import java.util.Locale;

/**
 * The category of a tile in a level map. Every tile in a {@link TerrainTile} carries a type so that
 * rendering, collision (#16), and entity spawning can all reason about the same tile definitions.
 *
 * <p>Two gameplay flags are exposed so downstream systems don't need to hard-code behaviour per
 * type:
 *
 * <ul>
 *   <li>{@link #isSolid()} - the tile blocks movement (used to generate the collision layer).
 *   <li>{@link #isHazardous()} - entering the tile triggers hazardous behaviour (e.g. spikes).
 * </ul>
 */
public enum TileType {
  /** Passable ground the player can walk across. */
  FLOOR(false, false),
  /** Solid tile that blocks movement. */
  WALL(true, false),
  /** Support tile the player can stand on (treated as solid for now; one-way support later). */
  PLATFORM(true, false),
  /** Passable tile that damages / triggers hazardous behaviour on entry. */
  HAZARD(false, true),
  /** Visual-only tile with no gameplay effect. */
  DECORATIVE(false, false),
  /** No tile present in this cell. */
  EMPTY(false, false);

  private final boolean solid;
  private final boolean hazardous;

  TileType(boolean solid, boolean hazardous) {
    this.solid = solid;
    this.hazardous = hazardous;
  }

  /**
   * @return true if the tile blocks movement and should produce a collider.
   */
  public boolean isSolid() {
    return solid;
  }

  /**
   * @return true if the tile triggers hazardous behaviour when entered.
   */
  public boolean isHazardous() {
    return hazardous;
  }

  /**
   * Parse a tile type from its name, case-insensitively.
   *
   * @param value the type name (e.g. "wall", "WALL"), may be null
   * @return the matching {@link TileType}, or {@link #EMPTY} if {@code value} is null
   * @throws IllegalArgumentException if {@code value} is non-null but not a known type
   */
  public static TileType fromString(String value) {
    if (value == null) {
      return EMPTY;
    }
    return TileType.valueOf(value.trim().toUpperCase(Locale.ROOT));
  }
}
