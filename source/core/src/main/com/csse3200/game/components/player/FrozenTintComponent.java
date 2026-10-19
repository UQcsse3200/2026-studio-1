package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.components.effects.FrozenCause;
import com.csse3200.game.components.effects.SpeedEffectComponent;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Draws a coloured overlay over the player while they are stopped: blue-grey when frozen, stone
 * grey when petrified. Drawn the same way as {@link UpgradeActivationFlashComponent}, because the
 * player's own sprite cannot be recoloured: it reuses the shield texture as a plain shape, at the
 * same padded size and position, and resets the batch colour afterwards.
 *
 * <p>Visible between {@link SpeedEffectComponent#FROZEN_START_EVENT} and {@link
 * SpeedEffectComponent#FROZEN_END_EVENT}. Cleared on death. There is no fade: the overlay is on for
 * exactly as long as the effect lasts.
 */
public class FrozenTintComponent extends RenderComponent {
  private static final String TEXTURE_PATH = "images/Shield.png";

  /** Blue-grey, for lightning. */
  private static final Color FROZEN_COLOUR = new Color(0.55f, 0.75f, 1f, 1f);

  /** Stone grey, for Medusa's gaze. */
  private static final Color PETRIFIED_COLOUR = new Color(0.62f, 0.62f, 0.62f, 1f);

  /** How see-through the overlay is, so the player can still be seen underneath. */
  private static final float TINT_ALPHA = 0.5f;

  /** Same padding as {@link ShieldRenderComponent}, so the overlay surrounds the player. */
  private static final float SCALE_PADDING = 1.4f;

  private Texture texture;
  private boolean active = false;
  private String cause;

  /**
   * Chooses the overlay colour for a cause.
   *
   * @param cause "Frozen", "Petrified", or anything else
   * @return the colour for that cause; the frozen colour for anything unknown or null
   */
  static Color colourFor(String cause) {
    return FrozenCause.PETRIFIED.equals(cause) ? PETRIFIED_COLOUR : FROZEN_COLOUR;
  }

  @Override
  public void create() {
    super.create();
    texture = ServiceLocator.getResourceService().getAsset(TEXTURE_PATH, Texture.class);
    entity.getEvents().addListener(FrozenCause.STATUS_CAUSE_EVENT, this::onCause);
    entity.getEvents().addListener(SpeedEffectComponent.FROZEN_START_EVENT, this::show);
    entity.getEvents().addListener(SpeedEffectComponent.FROZEN_END_EVENT, this::hide);
    entity.getEvents().addListener("death", this::hide);
  }

  /** Remembers why the player is being stopped; a null cause is ignored. */
  private void onCause(String newCause) {
    if (newCause != null) {
      cause = newCause;
    }
  }

  private void show() {
    active = true;
  }

  /** Turns the overlay off and forgets the cause. */
  private void hide() {
    active = false;
    cause = null;
  }

  /** Draws the overlay when active, then resets the batch colour to white. */
  @Override
  protected void draw(SpriteBatch batch) {
    if (!active) {
      return;
    }

    float width = entity.getScale().x * SCALE_PADDING;
    float height = entity.getScale().y * SCALE_PADDING;
    float x = entity.getPosition().x - (width - entity.getScale().x) / 2f;
    float y = entity.getPosition().y - (height - entity.getScale().y) / 2f;

    Color colour = colourFor(cause);
    batch.setColor(colour.r, colour.g, colour.b, TINT_ALPHA);
    batch.draw(texture, x, y, width, height);
    batch.setColor(Color.WHITE);
  }
}
