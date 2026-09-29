package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Draws the shield bubble texture over the owning entity while its {@link ShieldComponent} is
 * active.
 *
 * <p>Must be added to the same entity as a {@link ShieldComponent}; it reads that component's
 * {@link ShieldComponent#isActive()} state each frame and only draws when the shield is currently
 * blocking damage. The bubble automatically stops being drawn the moment the shield deactivates
 * (including its automatic 30-second expiry), since no extra timer is needed here.
 */
public class ShieldRenderComponent extends RenderComponent {
  private static final String TEXTURE_PATH = "images/Shield.png";

  /**
   * How much larger than the player's own scale the bubble is drawn, so it visibly surrounds them.
   */
  private static final float SCALE_PADDING = 1.4f;

  private Texture texture;
  private ShieldComponent shieldComponent;

  @Override
  public void create() {
    super.create();
    texture = ServiceLocator.getResourceService().getAsset(TEXTURE_PATH, Texture.class);
    shieldComponent = entity.getComponent(ShieldComponent.class);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (shieldComponent == null || !shieldComponent.isActive()) {
      return;
    }

    float width = entity.getScale().x * SCALE_PADDING;
    float height = entity.getScale().y * SCALE_PADDING;
    float x = entity.getPosition().x - (width - entity.getScale().x) / 2f;
    float y = entity.getPosition().y - (height - entity.getScale().y) / 2f;

    batch.draw(texture, x, y, width, height);
  }
}
