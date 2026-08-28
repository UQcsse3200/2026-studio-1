package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.LootPickupComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Factory for creating loot entities that can be picked up by the player. */
public class LootFactory {

  /**
   * Creates a loot entity containing the given item.
   *
   * @param item item represented by this loot entity
   * @return loot entity
   */
  public static Entity createLoot(Item item) {
    Entity loot = new Entity();

    if (item instanceof WeaponItem weaponItem) {
      String texturePath;

      if (weaponItem.getWeaponType() == WeaponType.BOW) {
        texturePath = "images/bow.png";
      } else {
        texturePath = "images/sword.png";
      }

      loot.addComponent(new TextureRenderComponent(texturePath));
    } else {
      AnimationRenderComponent animator =
              new AnimationRenderComponent(
                      ServiceLocator.getResourceService()
                              .getAsset("images/gold_coin/gold_coin.atlas", TextureAtlas.class));

      animator.addAnimation("gold_coin", 0.15f, Animation.PlayMode.LOOP);
      animator.startAnimation("gold_coin");

      loot.addComponent(animator);
    }

    loot.addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new LootPickupComponent(item));

    return loot;
  }

  private LootFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}