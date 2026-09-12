package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders animated weapon overlays on enemy entities (such as skeletons). Renders on top of the
 * entity's main body animation.
 */
public class EnemyWeaponAnimationComponent extends RenderComponent {
  private static final Logger logger = LoggerFactory.getLogger(EnemyWeaponAnimationComponent.class);
  private final GameTime timeSource;
  private final TextureAtlas atlas;
  private final Map<String, Animation<TextureRegion>> animations;
  private Animation<TextureRegion> currentAnimation;
  private String currentAnimationName;
  private float animationPlayTime;
  private float scaleMultiplier = 1.5f;
  private float offsetX = 0f;
  private float offsetY = -0.15f;

  /**
   * Create the component for a given texture atlas.
   *
   * @param atlas libGDX-supported texture atlas containing weapon animations
   */
  public EnemyWeaponAnimationComponent(TextureAtlas atlas) {
    this.atlas = atlas;
    this.animations = new HashMap<>(4);
    this.timeSource = ServiceLocator.getTimeSource();
  }

  /**
   * Register an animation from the texture atlas. Will play once when called with startAnimation()
   *
   * @param name Name of the animation. Must match the name of this animation inside the texture
   *     atlas.
   * @param frameDuration How long, in seconds, to show each frame of the animation for when playing
   * @return true if added successfully, false otherwise
   */
  public boolean addAnimation(String name, float frameDuration) {
    return addAnimation(name, frameDuration, PlayMode.NORMAL);
  }

  /**
   * Register an animation from the texture atlas.
   *
   * @param name Name of the animation. Must match the name of this animation inside the texture
   *     atlas.
   * @param frameDuration How long, in seconds, to show each frame of the animation for when playing
   * @param playMode How the animation should be played (e.g. looping, normal)
   * @return true if added successfully, false otherwise
   */
  public boolean addAnimation(String name, float frameDuration, PlayMode playMode) {
    Array<AtlasRegion> regions = atlas.findRegions(name);
    if (regions == null || regions.size == 0) {
      AtlasRegion singleRegion = atlas.findRegion(name);
      if (singleRegion != null) {
        regions = new Array<>(1);
        regions.add(singleRegion);
      } else {
        logger.warn("Animation {} not found in texture atlas", name);
        return false;
      }
    } else if (animations.containsKey(name)) {
      logger.warn(
          "Animation {} already added in texture atlas. Animations should only be added once.",
          name);
      return false;
    }

    Animation<TextureRegion> animation = new Animation<>(frameDuration, regions, playMode);
    animations.put(name, animation);
    logger.debug("Adding weapon animation {}", name);
    return true;
  }

  /**
   * Start playback of an animation. The animation must have been added using addAnimation().
   *
   * @param name Name of the animation to play.
   */
  public void startAnimation(String name) {
    Animation<TextureRegion> animation = animations.getOrDefault(name, null);
    if (animation == null) {
      logger.error(
          "Attempted to play unknown animation {}. Ensure animation is added before playback.",
          name);
      return;
    }

    currentAnimation = animation;
    currentAnimationName = name;
    animationPlayTime = 0f;
    logger.debug("Starting weapon animation {}", name);
  }

  /**
   * Stop the currently running animation. Does nothing if no animation is playing.
   *
   * @return true if animation was stopped, false if no animation is playing.
   */
  public boolean stopAnimation() {
    if (currentAnimation == null) {
      return false;
    }

    logger.debug("Stopping weapon animation {}", currentAnimationName);
    currentAnimation = null;
    currentAnimationName = null;
    animationPlayTime = 0f;
    return true;
  }

  /**
   * Get the name of the animation currently being played.
   *
   * @return current animation name, or null if not playing.
   */
  public String getCurrentAnimation() {
    return currentAnimationName;
  }

  /**
   * Has the playing animation finished? This will always be false for looping animations.
   *
   * @return true if animation was playing and has now finished, false otherwise.
   */
  public boolean isFinished() {
    return currentAnimation != null && currentAnimation.isAnimationFinished(animationPlayTime);
  }

  /**
   * Whether the animator has added the given animation.
   *
   * @param name Name of the added animation.
   * @return true if added, false otherwise.
   */
  public boolean hasAnimation(String name) {
    return animations.containsKey(name);
  }

  /**
   * Remove an animation from this animator.
   *
   * @param name Name of the previously added animation.
   * @return true if removed, false if animation was not found.
   */
  public boolean removeAnimation(String name) {
    logger.debug("Removing weapon animation {}", name);
    return animations.remove(name) != null;
  }

  /**
   * Set the scale multiplier relative to the entity scale.
   *
   * @param scaleMultiplier multiplier (e.g. 1.25f for 40px sprite on 32px entity)
   */
  public void setScaleMultiplier(float scaleMultiplier) {
    this.scaleMultiplier = scaleMultiplier;
  }

  public float getScaleMultiplier() {
    return scaleMultiplier;
  }

  public void setOffsetX(float offsetX) {
    this.offsetX = offsetX;
  }

  public float getOffsetX() {
    return offsetX;
  }

  public void setOffsetY(float offsetY) {
    this.offsetY = offsetY;
  }

  public float getOffsetY() {
    return offsetY;
  }

  public void setOffset(float offsetX, float offsetY) {
    this.offsetX = offsetX;
    this.offsetY = offsetY;
  }

  @Override
  public float getZIndex() {
    // Drawn slightly in front of the base entity's body sprite
    return -entity.getPosition().y + 0.01f;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (currentAnimation == null) {
      return;
    }

    TextureRegion region = currentAnimation.getKeyFrame(animationPlayTime);
    Vector2 pos = entity.getPosition();
    Vector2 scale = entity.getScale();

    float width = scale.x * scaleMultiplier;
    float height = scale.y * scaleMultiplier;
    float offsetRatio = (scaleMultiplier - 1f) / 2f;
    float x = pos.x - (scale.x * offsetRatio) + offsetX;
    float y = pos.y - (scale.y * offsetRatio) + offsetY;

    batch.draw(region, x, y, width, height);
    animationPlayTime += timeSource.getDeltaTime();
  }

  @Override
  public void dispose() {
    atlas.dispose();
    super.dispose();
  }
}
