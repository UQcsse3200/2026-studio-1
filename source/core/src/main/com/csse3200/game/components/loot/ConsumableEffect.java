package com.csse3200.game.components.loot;

import com.csse3200.game.entities.Entity;

/**
 * The effect a consumable applies when it is used.
 *
 * <p>Implementations are strategies held by a {@link ConsumableItem}, which keeps the item itself a
 * plain data class in line with the rest of the loot package.
 */
@FunctionalInterface
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
