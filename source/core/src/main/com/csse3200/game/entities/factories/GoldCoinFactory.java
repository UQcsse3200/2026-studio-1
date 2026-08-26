package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Factory for creating gold coin entities. */
public class GoldCoinFactory {

  /**
   * Creates a gold coin entity with a looping spinning animation.
   *
   * @return gold coin entity
   */
  public static Entity createGoldCoin() {
    Entity coin = new Entity();

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/gold_coin/gold_coin.atlas", TextureAtlas.class));

    animator.addAnimation("gold_coin", 0.15f, Animation.PlayMode.LOOP);
    animator.startAnimation("gold_coin");

    coin.addComponent(animator);

    return coin;
  }

  private GoldCoinFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
