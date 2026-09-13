package com.csse3200.game.rendering;

/** Renders a composed map image behind terrain, entities, and collision debug geometry. */
public class MapBackgroundRenderComponent extends TextureRenderComponent {
  public MapBackgroundRenderComponent(String texturePath) {
    super(texturePath);
  }

  @Override
  public int getLayer() {
    return -1;
  }

  @Override
  public float getZIndex() {
    return 0f;
  }
}
