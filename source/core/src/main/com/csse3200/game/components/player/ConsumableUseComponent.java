package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.ConsumableItem;
import com.csse3200.game.components.loot.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers the use handler for consumable items on the owning entity.
 *
 * <p>Trigger {@code "useItem"} with an inventory slot index to consume the item in that slot. The
 * item is removed from the inventory only when its effect actually did something, so a health
 * potion used at full health is left untouched.
 *
 * <p>This component also declares the entity's maximum health. {@code CombatStatsComponent} is
 * shared with other teams and has no maximum health field, so the cap is stored here and read by
 * {@code HealEffect} instead of modifying that class.
 */
public class ConsumableUseComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(ConsumableUseComponent.class);

  private final int maxHealth;

  /**
   * Creates a use handler with an explicit health cap.
   *
   * @param maxHealth the entity's maximum health; must be {@code > 0}
   * @throws IllegalArgumentException if {@code maxHealth} is not positive
   */
  public ConsumableUseComponent(int maxHealth) {
    if (maxHealth <= 0) {
      throw new IllegalArgumentException("maxHealth must be greater than 0.");
    }
    this.maxHealth = maxHealth;
  }

  /** Registers the {@code "useItem"} listener. */
  @Override
  public void create() {
    entity.getEvents().addListener("useItem", this::useItem);
  }

  /**
   * Returns the entity's maximum health, used to cap healing.
   *
   * @return maximum health
   */
  public int getMaxHealth() {
    return maxHealth;
  }

  /**
   * Uses the consumable in the given inventory slot.
   *
   * <p>On success one unit is removed from the stack and {@code "itemConsumed"} is triggered with
   * the item, so UI can react.
   *
   * @param slot inventory slot index
   * @return {@code true} if a consumable was used and removed
   */
  public boolean useItem(int slot) {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    if (inventory == null) {
      logger.debug("Cannot use item, entity has no inventory");
      return false;
    }

    Item item = inventory.getItem(slot);
    if (!(item instanceof ConsumableItem)) {
      logger.debug("Slot {} does not hold a consumable", slot);
      return false;
    }

    ConsumableItem consumable = (ConsumableItem) item;
    if (!consumable.use(entity)) {
      logger.debug("{} had no effect, leaving it in the inventory", consumable.getName());
      return false;
    }

    inventory.removeItem(slot, 1);
    logger.debug("Consumed {} from slot {}", consumable.getName(), slot);
    entity.getEvents().trigger("itemConsumed", consumable);
    return true;
  }
}
