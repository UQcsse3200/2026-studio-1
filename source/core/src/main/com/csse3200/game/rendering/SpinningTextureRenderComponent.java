package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.services.ServiceLocator;

/** Render a static texture that spins around its centre. */
public class SpinningTextureRenderComponent extends RenderComponent {
  private final TextureRegion region;
  private final float degreesPerSecond;
  private float rotation = 0f;

  /**
   * @param texturePath Internal path of static texture to render. Will be scaled to the entity's
   *     scale.
   * @param degreesPerSecond How fast the texture spins.
   */
  public SpinningTextureRenderComponent(String texturePath, float degreesPerSecond) {
    this.region =
        new TextureRegion(ServiceLocator.getResourceService().getAsset(texturePath, Texture.class));
    this.degreesPerSecond = degreesPerSecond;
  }

  @Override
  public void update() {
    rotation = (rotation + degreesPerSecond * ServiceLocator.getTimeSource().getDeltaTime()) % 360f;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    batch.draw(
        region,
        position.x,
        position.y,
        scale.x / 2f,
        scale.y / 2f,
        scale.x,
        scale.y,
        1f,
        1f,
        rotation);
  }
}
