package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Draws the Ballistic Shield energy bubble around the player while it is active.
 *
 * <p>The Ballistic Shield is represented as a larger, stronger version of the existing shield
 * bubble. The player remains visible inside the bubble so that it is obvious when the shield is
 * equipped and active.
 */
public class BallisticShieldRenderComponent extends RenderComponent {

  /*
   * Use the existing Shield.png because it is already a proper transparent
   * bubble asset in the game. This avoids rendering the checkerboard contained
   * in the downloaded BallisticShield.png.
   */
  private static final String TEXTURE_PATH = "images/BallisticShield.png";

  /** Ballistic Shield is larger than the normal Shield to make the upgrade obvious. */
  private static final float SCALE_PADDING = 1.25f;

  /** Slight transparency keeps the player clearly visible through the bubble. */
  private static final float ALPHA = 0.72f;

  private Texture texture;
  private BallisticShieldComponent ballisticShieldComponent;

  @Override
  public void create() {
    super.create();

    texture = ServiceLocator.getResourceService().getAsset(TEXTURE_PATH, Texture.class);
    ballisticShieldComponent = entity.getComponent(BallisticShieldComponent.class);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (ballisticShieldComponent == null || !ballisticShieldComponent.isActive()) {
      return;
    }

    float width = entity.getScale().x * SCALE_PADDING;
    float height = entity.getScale().y * SCALE_PADDING;

    float x = entity.getPosition().x - (width - entity.getScale().x) / 2f;
    float y = entity.getPosition().y - (height - entity.getScale().y) / 2f;

    /*
     * Tint the existing shield bubble slightly blue and make it translucent.
     * This makes the Ballistic Shield visually stronger than the normal Shield
     * while keeping the player visible.
     */
    Color previousColor = batch.getColor().cpy();

    batch.setColor(0.35f, 0.75f, 1.0f, ALPHA);
    batch.draw(texture, x, y, width, height);

    /*
     * Always restore the SpriteBatch colour so that the shield does not
     * accidentally tint the player or other entities rendered afterwards.
     */
    batch.setColor(previousColor);
  }
}
