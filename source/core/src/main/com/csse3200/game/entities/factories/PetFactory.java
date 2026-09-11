package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.components.pet.PetComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Factory for creating companion pet entities.
 *
 * <p>The factory creates the base pet entity and its visual representation. Movement, targeting,
 * and combat behaviours are implemented separately.
 */
public class PetFactory {

  private static final String PET_ATLAS = "images/pet.atlas";

  /**
   * Creates a base companion pet owned by the given player.
   *
   * @param owner player entity that owns this pet
   * @return unregistered pet entity
   */
  public static Entity createPet(Entity owner) {
    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(PET_ATLAS, TextureAtlas.class));

    animator.addAnimation("idle", 0.15f, Animation.PlayMode.LOOP);

    Entity pet = new Entity().addComponent(new PetComponent(owner)).addComponent(animator);

    pet.getComponent(AnimationRenderComponent.class).scaleEntity();
    animator.startAnimation("idle");

    return pet;
  }

  private PetFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
