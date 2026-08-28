package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Renders a static texture that bobs gently up and down, used to make dropped items read as
 * collectable.
 *
 * <p>The offset is applied when drawing rather than by moving the entity, so the item's real
 * position and its pickup area stay still while the sprite appears to float.
 *
 * <p>Timing comes from {@link GameTime}, the same service the rest of the engine uses, so no new
 * timer is introduced. Each entity is given a slightly different starting point in the wave, based
 * on its id, so several dropped items do not bob in lockstep.
 */
public class BobbingTextureRenderComponent extends RenderComponent {
  /** How far the sprite travels from its resting position, in world units. */
  private static final float DEFAULT_AMPLITUDE = 0.08f;

  /** How quickly the sprite bobs, in radians per second. */
  private static final float DEFAULT_SPEED = 3f;

  /** Spreads entities out across the wave so they do not all rise and fall together. */
  private static final float PHASE_PER_ID = 0.7f;

  private final Texture texture;
  private final float amplitude;
  private final float speed;
  private GameTime timeSource;

  /**
   * Creates a bobbing sprite from an asset path, using the default motion.
   *
   * @param texturePath internal path of the texture to render
   */
  public BobbingTextureRenderComponent(String texturePath) {
    this(ServiceLocator.getResourceService().getAsset(texturePath, Texture.class));
  }

  /**
   * Creates a bobbing sprite using the default motion.
   *
   * @param texture texture to render
   */
  public BobbingTextureRenderComponent(Texture texture) {
    this(texture, DEFAULT_AMPLITUDE, DEFAULT_SPEED);
  }

  /**
   * Creates a bobbing sprite with explicit motion values.
   *
   * @param texture texture to render
   * @param amplitude how far the sprite moves from its resting position, in world units
   * @param speed how quickly the sprite bobs, in radians per second
   */
  public BobbingTextureRenderComponent(Texture texture, float amplitude, float speed) {
    this.texture = texture;
    this.amplitude = amplitude;
    this.speed = speed;
  }

  /** Registers for rendering and stores the game clock. */
  @Override
  public void create() {
    super.create();
    timeSource = ServiceLocator.getTimeSource();
  }

  /** Scales the entity to a width of 1 and a height matching the texture's ratio. */
  public void scaleEntity() {
    entity.setScale(1f, (float) texture.getHeight() / texture.getWidth());
  }

  /**
   * Returns how far the sprite is currently drawn above or below its resting position.
   *
   * @return vertical offset in world units, between {@code -amplitude} and {@code +amplitude}
   */
  float getBobOffset() {
    float seconds = timeSource.getTime() / 1000f;
    float phase = entity.getId() * PHASE_PER_ID;
    return amplitude * (float) Math.sin(seconds * speed + phase);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    batch.draw(texture, position.x, position.y + getBobOffset(), scale.x, scale.y);
  }
}
