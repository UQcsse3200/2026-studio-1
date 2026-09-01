package com.csse3200.game.areas.terrain.map;

import com.csse3200.game.areas.terrain.TileType;

/**
 * An entry in a map's legend: the pairing of a tile {@link TileType} with the texture used to draw
 * it. A single symbol in a map file resolves to one {@code TileDefinition}.
 *
 * @param type the gameplay category of the tile
 * @param texture the asset path of the tile's texture (may be null for non-visual tiles)
 */
public record TileDefinition(TileType type, String texture) {}
