package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.services.ServiceLocator;

/**
 * Draws a soft pool of light over the terrain, for a tile that should look lit such as a lamp or a
 * fire. The light is added to whatever is beneath it and flickers gently. It is purely visual.
 *
 * <p>The entity's position is the centre of the light.
 */
public class GlowRenderComponent extends RenderComponent {
  private static final float BASE_ALPHA = 0.45f;
  private static final float FLICKER_ALPHA = 0.12f;
  private static final float FLICKER_SPEED = 5f;

  private final String texturePath;
  private final float size;
  private final float phase;
  private Texture texture;
  private float elapsed;

  /**
   * @param texturePath a light sprite that is bright at its centre and transparent at its edge
   * @param size the width and height of the light in world units
   * @param phase where in its flicker this light starts, so neighbours do not pulse in step
   */
  public GlowRenderComponent(String texturePath, float size, float phase) {
    this.texturePath = texturePath;
    this.size = size;
    this.phase = phase;
  }

  @Override
  public void create() {
    super.create();
    texture = ServiceLocator.getResourceService().getAsset(texturePath, Texture.class);
  }

  @Override
  public void update() {
    elapsed += ServiceLocator.getTimeSource().getDeltaTime();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    Vector2 centre = entity.getPosition();
    int sourceBlend = batch.getBlendSrcFunc();
    int destinationBlend = batch.getBlendDstFunc();

    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    batch.setColor(1f, 1f, 1f, alphaAt(elapsed, phase));
    batch.draw(texture, centre.x - size / 2f, centre.y - size / 2f, size, size);
    batch.setColor(1f, 1f, 1f, 1f);
    batch.setBlendFunction(sourceBlend, destinationBlend);
  }

  /**
   * @param elapsed seconds the light has been burning
   * @param phase the light's flicker offset
   * @return the light's opacity, which wavers around a steady level and never goes out
   */
  static float alphaAt(float elapsed, float phase) {
    return BASE_ALPHA + FLICKER_ALPHA * MathUtils.sin(elapsed * FLICKER_SPEED + phase);
  }

  /** Lights sit on the terrain's layer, drawn after it and before the characters. */
  @Override
  public int getLayer() {
    return 0;
  }

  @Override
  public float getZIndex() {
    return 1f;
  }
}
