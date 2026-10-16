package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class PlayerRenderComponent extends AnimationRenderComponent {
  private final TextureAtlas playerAtlas;

  /**
   * Create the component for a given texture atlas.
   *
   * @param playerAtlas libGDX-supported texture atlas containing player animations
   */
  public PlayerRenderComponent(TextureAtlas playerAtlas) {
    super(playerAtlas);
    this.playerAtlas = playerAtlas;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    /*
     * Normal knight rendering.
     */
    if (currentAnimation == null) {
      return;
    }

    TextureRegion region = currentAnimation.getKeyFrame(animationPlayTime);
    Vector2 pos = entity.getPosition();
    Vector2 scale = entity.getScale();

    float pixel = 1f / 42f;
    float width = pixel * region.getRegionWidth();
    float height = pixel * region.getRegionHeight();
    float offset = (scale.x - width)/2f;

    batch.draw(region, pos.x + offset, pos.y, width, height);

    animationPlayTime += timeSource.getDeltaTime();
  }
}

