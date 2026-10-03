package com.csse3200.game.areas.terrain.map;

/**
 * One image of a sub-level's parallax backdrop, drawn behind the tile layers and filling the view.
 *
 * <p>A backdrop is a stack of these, listed back to front. Each is pinned to the screen and slides
 * its artwork sideways as the camera pans, so distant layers appear to move less than near ones.
 *
 * @param texture the image, which must tile seamlessly in every direction it moves
 * @param scroll how far the artwork moves relative to the camera: 0 is fixed to the screen, 1 moves
 *     with the tiles
 * @param driftX constant sideways movement in world units per second, for fog and the like
 * @param driftY constant upward movement in world units per second, for rising embers and the like
 */
public record BackdropLayer(String texture, float scroll, float driftX, float driftY) {}
