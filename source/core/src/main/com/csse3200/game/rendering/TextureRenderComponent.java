package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.services.ServiceLocator;

/** Render a static texture. */
public class TextureRenderComponent extends RenderComponent {
  private Texture texture; // DELETE FINAL FOR CROUCH ABLILTY
  // True to mirror the texture horizontally when drawn (e.g. a projectile fired to the
  // left, when the source art only faces right). Purely visual - does not affect the
  // entity's scale, position, or collider.
  private boolean flipX = false;

  /**
   * @param texturePath Internal path of static texture to render. Will be scaled to the entity's
   *     scale.
   */
  public TextureRenderComponent(String texturePath) {
    this(ServiceLocator.getResourceService().getAsset(texturePath, Texture.class));
  }

  // ...
  /**
   * @param texture Static texture to render. Will be scaled to the entity's scale.
   */
  public TextureRenderComponent(Texture texture) {
    this.texture = texture;
  }

  /** Scale the entity to a width of 1 and a height matching the texture's ratio */
  public void scaleEntity() {
    entity.setScale(1f, (float) texture.getHeight() / texture.getWidth());
  }

  /**
   * Sets whether the texture should be mirrored horizontally when drawn.
   *
   * @param flipX true to draw the texture flipped horizontally
   */
  public void setFlipX(boolean flipX) {
    this.flipX = flipX;
  }

  /**
   * Returns whether the texture is currently drawn flipped horizontally.
   *
   * @return true if flipped
   */
  public boolean isFlipX() {
    return flipX;
  }

  public void setTexture(String texturePath) { // for crouch
    this.texture = ServiceLocator.getResourceService().getAsset(texturePath, Texture.class);
  }

  public void setTexture(Texture texture) { //
    this.texture = texture;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    if (flipX) {
      // Negative width mirrors the texture within the same bounding box (position.x to
      // position.x + scale.x) instead of shifting it - the box itself never moves.
      batch.draw(texture, position.x + scale.x, position.y, -scale.x, scale.y);
    } else {
      batch.draw(texture, position.x, position.y, scale.x, scale.y);
    }
  }
}
