package com.csse3200.game.components.loot;

import com.csse3200.game.entities.Entity;

/**
 * Represents a consumable item that can be stored in the player's inventory and used for an
 * immediate effect.
 *
 * <p>A consumable has the common properties of an {@link Item}, plus a {@link ConsumableType} and
 * the {@link ConsumableEffect} it applies when used. Following {@code WeaponItem}, this stays a
 * plain data class: the use handler lives in {@code ConsumableUseComponent} on the player entity.
 */
public class ConsumableItem extends Item {
  private final ConsumableType consumableType;
  private final ConsumableEffect effect;

  /**
   * Creates a consumable item.
   *
   * @param name display name
   * @param consumableType which consumable this is
   * @param effect effect applied on use
   * @param quantity starting stack size
   * @param maxQuantity maximum stack size; must be {@code > 0}
   * @throws IllegalArgumentException if {@code consumableType} or {@code effect} is null
   */
  public ConsumableItem(
      String name,
      ConsumableType consumableType,
      ConsumableEffect effect,
      int quantity,
      int maxQuantity) {
    super(name, ItemType.CONSUMABLE, quantity, maxQuantity);

    if (consumableType == null) {
      throw new IllegalArgumentException("ConsumableType must not be null.");
    }

    if (effect == null) {
      throw new IllegalArgumentException("ConsumableEffect must not be null.");
    }

    this.consumableType = consumableType;
    this.effect = effect;
  }

  /**
   * Returns which consumable this is.
   *
   * @return consumable type
   */
  public ConsumableType getConsumableType() {
    return consumableType;
  }

  /**
   * Returns the effect applied when this item is used.
   *
   * @return the consumable's effect
   */
  public ConsumableEffect getEffect() {
    return effect;
  }

  /**
   * Returns the sprite representing this consumable.
   *
   * @return internal asset path of the texture
   */
  public String getTexturePath() {
    return consumableType.getTexturePath();
  }

  /**
   * Applies this consumable's effect to the given entity.
   *
   * <p>Returning {@code false} means the item had no effect and must not be consumed, for example a
   * health potion used at full health.
   *
   * @param entity entity using the item
   * @return {@code true} if the effect was applied
   */
  public boolean use(Entity entity) {
    return effect.apply(entity);
  }
}
