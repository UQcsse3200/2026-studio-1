package com.csse3200.game.components.loot;

import com.csse3200.game.entities.Entity;

/**
 * What a consumable does when it is used.
 *
 * <p>Each consumable holds one of these instead of the item deciding its own behaviour with a
 * switch, so a new kind of consumable only needs a new class implementing this interface.
 */
public interface ConsumableEffect {
  /**
   * Applies this effect to the given entity.
   *
   * <p>Returning {@code false} means the effect could not do anything useful, for example healing
   * an entity that is already at full health. The caller must not consume the item in that case.
   *
   * @param entity entity using the consumable
   * @return {@code true} if the effect was applied and the item should be consumed
   */
  boolean apply(Entity entity);
}
