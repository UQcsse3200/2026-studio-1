package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Draws a brief gold flash over the player whenever an upgrade activates (purchase or tier-up), so
 * the moment it happened is visible - a short fade, not a continuous glow.
 *
 * <p>Reuses the shield bubble texture ({@code "images/Shield.png"}) purely as a plain shape to tint
 * - not as a shield indicator - since it's already loaded by LevelGameArea and confirmed working
 * via {@link ShieldRenderComponent}. The player's own sprite is an animated atlas rendered by
 * {@link com.csse3200.game.rendering.PlayerRenderComponent}, which exposes no public way to read
 * its current frame, so this deliberately does not try to overlay it.
 *
 * <p>Tinted gold rather than white: {@code setColor(1,1,1,alpha)} alpha-blends a near-identical
 * copy of an opaque sprite over itself, which is nearly invisible - white only shifts opacity, not
 * colour. Drawn at {@link #SCALE_PADDING}, copying {@link ShieldRenderComponent}'s exact
 * width/height/position formula, so the flash reads as a coloured outline around the character
 * regardless of draw order against the player's own renderer (neither overrides
 * getZIndex()/getLayer(), so which draws on top is undefined).
 *
 * <p>Listens for {@code "upgradeActivated"}, fired by {@code UpgradesDisplay}'s apply*Effect()
 * methods. Retriggering while already fading restarts the flash from full brightness rather than
 * stacking, since flashTimeRemaining is simply reset, not added to.
 */
public class UpgradeActivationFlashComponent extends RenderComponent {
  private static final String TEXTURE_PATH = "images/Shield.png";

  /** How long the flash takes to fade out completely. */
  private static final float FLASH_DURATION = 0.4f;

  /** Peak alpha at the moment of activation, so the flash is visible but not blinding. */
  private static final float PEAK_ALPHA = 0.6f;

  /**
   * How much larger than the player's own scale the flash is drawn - same value and reasoning as
   * {@link ShieldRenderComponent}'s own SCALE_PADDING.
   */
  private static final float SCALE_PADDING = 1.4f;

  private Texture texture;
  private float flashTimeRemaining = 0f;

  @Override
  public void create() {
    super.create();
    texture = ServiceLocator.getResourceService().getAsset(TEXTURE_PATH, Texture.class);
    entity.getEvents().addListener("upgradeActivated", this::onUpgradeActivated);
  }

  private void onUpgradeActivated() {
    flashTimeRemaining = FLASH_DURATION;
  }

  @Override
  public void update() {
    flashTimeRemaining =
        Math.max(0f, flashTimeRemaining - ServiceLocator.getTimeSource().getDeltaTime());
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (flashTimeRemaining <= 0) {
      return;
    }

    float alpha = (flashTimeRemaining / FLASH_DURATION) * PEAK_ALPHA;

    float width = entity.getScale().x * SCALE_PADDING;
    float height = entity.getScale().y * SCALE_PADDING;
    float x = entity.getPosition().x - (width - entity.getScale().x) / 2f;
    float y = entity.getPosition().y - (height - entity.getScale().y) / 2f;

    batch.setColor(1f, 0.85f, 0.3f, alpha);
    batch.draw(texture, x, y, width, height);
    // Immediately reset, exactly as PlayerStatsDisplay.HealthHeartImage.draw() does - so this
    // tint can never leak into anything drawn after it on the shared SpriteBatch.
    batch.setColor(Color.WHITE);
  }
}
