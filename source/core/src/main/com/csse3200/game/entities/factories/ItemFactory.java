package com.csse3200.game.entities.factories;

import com.csse3200.game.components.loot.ConsumableItem;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.BobbingTextureRenderComponent;

/**
 * Factory to create world entities for items that have been dropped as loot.
 *
 * <p>An {@link com.csse3200.game.components.loot.Item} is plain data and cannot be drawn on its
 * own. This factory wraps one in an {@link Entity} carrying the components needed to render it, so
 * loot generation can place a dropped item into a game area with {@code spawnEntityAt}.
 *
 * <p>Entities created here are display only. Walking over one does not pick it up: collecting an
 * item needs a collider and a handler that moves it into the player's inventory, which belongs with
 * the loot generation and inventory connection work rather than here.
 */
public class ItemFactory {
  /** Height of a dropped item in world units, so items sit smaller than the player. */
  private static final float ITEM_HEIGHT = 0.6f;

  /**
   * Creates a world entity for a dropped consumable.
   *
   * <p>The sprite comes from the item itself, so a health potion and a speed potion produce
   * different visuals without the caller choosing a texture.
   *
   * @param item consumable to represent
   * @return an entity that renders the item, bobbing gently in place
   * @throws IllegalArgumentException if {@code item} is null
   */
  public static Entity createConsumable(ConsumableItem item) {
    if (item == null) {
      throw new IllegalArgumentException("ConsumableItem must not be null.");
    }

    Entity dropped =
        new Entity().addComponent(new BobbingTextureRenderComponent(item.getTexturePath()));

    dropped.getComponent(BobbingTextureRenderComponent.class).scaleEntity();
    dropped.scaleHeight(ITEM_HEIGHT);
    return dropped;
  }

  private ItemFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
