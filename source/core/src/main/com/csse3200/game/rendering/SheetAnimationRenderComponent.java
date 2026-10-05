package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.services.ServiceLocator;

/**
 * Loops a sprite sheet strip over the entity's position and scale, for a tile that moves, such as a
 * brazier's flame. The strip is one row of equally sized frames. It is purely visual.
 */
public class SheetAnimationRenderComponent extends RenderComponent {
  private final String texturePath;
  private final int frames;
  private final float fps;
  private Texture texture;
  private float elapsed;

  /**
   * @param texturePath a strip of frames laid out left to right
   * @param frames the number of frames in the strip
   * @param fps frames shown per second
   * @param offset seconds into the loop this strip starts, so neighbours do not move in step
   */
  public SheetAnimationRenderComponent(String texturePath, int frames, float fps, float offset) {
    this.texturePath = texturePath;
    this.frames = Math.max(1, frames);
    this.fps = fps;
    this.elapsed = offset;
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
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    float frameWidth = 1f / frames;
    int frame = frameAt(elapsed, frames, fps);
    batch.draw(
        texture,
        position.x,
        position.y,
        scale.x,
        scale.y,
        frame * frameWidth,
        1f,
        (frame + 1) * frameWidth,
        0f);
  }

  /**
   * @param elapsed seconds the strip has been playing
   * @param frames the number of frames in the strip
   * @param fps frames shown per second
   * @return the frame of a looping strip to show
   */
  public static int frameAt(float elapsed, int frames, float fps) {
    return Math.floorMod((int) Math.floor(elapsed * fps), frames);
  }

  /** Animated tiles sit on the terrain's layer, over their still tile and under its glow. */
  @Override
  public int getLayer() {
    return 0;
  }

  @Override
  public float getZIndex() {
    return 0.5f;
  }
}
