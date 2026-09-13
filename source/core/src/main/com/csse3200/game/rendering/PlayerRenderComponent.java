package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.services.GameTime;

import java.util.Map;

public class PlayerRenderComponent extends AnimationRenderComponent{

    /**
     * Create the component for a given texture atlas.
     *
     * @param atlas libGDX-supported texture atlas containing desired animations
     */
    public PlayerRenderComponent(TextureAtlas atlas) {
        super(atlas);
    }

    @Override
    protected void draw(SpriteBatch batch) {
        if (currentAnimation == null) {
            return;
        }
        TextureRegion region = currentAnimation.getKeyFrame(animationPlayTime);
        Vector2 pos = entity.getPosition();
        float pixel = 1f/32f;
        float width = pixel * region.getRegionWidth();
        float height = pixel * region.getRegionHeight();
        batch.draw(region, pos.x, pos.y, width, height);
        animationPlayTime += timeSource.getDeltaTime();
    }
}
