package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class PlayerRenderComponent extends AnimationRenderComponent {
    private TextureAtlas leftAtlas;
    private TextureAtlas rightAtlas;

    /**
     * Create the component for a given texture atlas.
     *
     * @param leftAtlas libGDX-supported texture atlas containing desired animations
     */
    public PlayerRenderComponent(TextureAtlas rightAtlas, TextureAtlas leftAtlas) {
        super(rightAtlas);
        this.leftAtlas = leftAtlas;
        this.rightAtlas = rightAtlas;
    }

    /**
     * Register an animation from the texture atlas.
     *
     * @param name          Name of the animation. Must match the name of this animation inside the texture
     *                      atlas.
     * @param frameDuration How long, in seconds, to show each frame of the animation for when playing
     * @param playMode      How the animation should be played (e.g. looping, backwards)
     * @return true if added successfully, false otherwise
     */
    @Override
    public boolean addAnimation(String name, float frameDuration, Animation.PlayMode playMode) {
        Array<TextureAtlas.AtlasRegion> regions = rightAtlas.findRegions(name);
        Array<TextureAtlas.AtlasRegion> leftregions = this.leftAtlas.findRegions(name);
        if (!(regions == null || regions.size == 0)) {
            Animation<TextureRegion> animation = new Animation<>(frameDuration, regions, playMode);
            animations.put(name, animation);
            logger.debug("Adding animation {}", name);
            return true;
        } else if (!(leftregions == null || leftregions.size == 0)) {
            Animation<TextureRegion> animation = new Animation<>(frameDuration, leftregions, playMode);
            animations.put(name, animation);
            logger.debug("Adding animation {}", name);
            return true;
        } else if ((regions == null || regions.size == 0) && (leftregions == null || leftregions.size == 0)) {
            logger.warn("Animation {} not found in texture atlas", name);
            return false;
        } else if (animations.containsKey(name)) {
            logger.warn(
                    "Animation {} already added in texture atlas. Animations should only be added once.",
                    name);
            return false;
        }
        return false;
    }

    @Override
    protected void draw(SpriteBatch batch) {
        if (currentAnimation == null) {
            return;
        }
        TextureRegion region = currentAnimation.getKeyFrame(animationPlayTime);
        Vector2 pos = entity.getPosition();
        float pixel = 1f/42f;
        float width = pixel * region.getRegionWidth();
        float height = pixel * region.getRegionHeight();
        batch.draw(region, pos.x, pos.y, width, height);
        animationPlayTime += timeSource.getDeltaTime();
    }
}
