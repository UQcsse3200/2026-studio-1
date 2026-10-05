package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.ConsumableItem;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.perks.Perk;
import com.csse3200.game.perks.PerkService;
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

  private int maxHealth;
  private static final String PERK_ID = "thickSkin";
  private static final int HEALTH_PERK = 50;

  /** Slot of the weapon the player last selected, for the Upgrade Stone. 0 means none yet. */
  private int heldWeaponSlot = 0;

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

  /** Registers the {@code "useItem"} listener and starts tracking the held weapon. */
  @Override
  public void create() {
    entity.getEvents().addListener("useItem", this::useItem);
    entity.getEvents().addListener("activeSlotChanged", this::rememberHeldWeapon);

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    if (inventory != null) {
      rememberHeldWeapon(inventory.getActiveSlot());
    }

    Perk thickSkinPerk = PerkService.getPerk(PERK_ID);
    if (thickSkinPerk != null) {
      if (thickSkinPerk.isActive()) {
        increaseMaxHealth(HEALTH_PERK);
      }
      thickSkinPerk.setOnActivated(() -> increaseMaxHealth(HEALTH_PERK));
      thickSkinPerk.setOnDeactivated(() -> decreaseMaxHealth(HEALTH_PERK));
    }
  }

  /**
   * Remembers the slot whenever the player selects a weapon.
   *
   * <p>Pressing an item's slot key both selects that slot and uses the item, so by the time an
   * Upgrade Stone is used, the stone's own slot is selected. Remembering the last weapon the player
   * selected lets the stone upgrade the weapon they were holding just before.
   *
   * @param slot slot that was just selected
   */
  private void rememberHeldWeapon(int slot) {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    if (inventory != null && inventory.getItem(slot) instanceof WeaponItem) {
      heldWeaponSlot = slot;
    }
  }

  /**
   * Returns the slot of the weapon the player last selected.
   *
   * <p>The slot may no longer hold a weapon (it could have been dropped or thrown), so callers must
   * check what is in it.
   *
   * @return the slot number, or 0 if the player has not held a weapon yet
   */
  public int getHeldWeaponSlot() {
    return heldWeaponSlot;
  }

  public void increaseMaxHealth(int amount) {
    if (amount <= 0) {
      return;
    }
    maxHealth += amount;
    logger.info("Max health increased by {}, now {}", amount, maxHealth);

    CombatStatsComponent stats =
        entity == null ? null : entity.getComponent(CombatStatsComponent.class);
    if (stats != null) {
      stats.addHealth(amount);
    }
  }

  public void decreaseMaxHealth(int amount) {
    if (amount <= 0) {
      return;
    }
    maxHealth = Math.max(0, maxHealth - amount);
    logger.info("Max health decreased by {}, now {}", amount, maxHealth);

    CombatStatsComponent stats =
        entity == null ? null : entity.getComponent(CombatStatsComponent.class);
    if (stats != null && stats.getHealth() > maxHealth) {
      stats.setHealth(maxHealth);
    }
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
   * Returns the maximum health declared for an entity.
   *
   * <p>{@code CombatStatsComponent} is shared with other teams and has no maximum health field, so
   * every healing effect resolves the cap through this component instead. An entity without one is
   * left uncapped, matching current engine behaviour.
   *
   * @param entity entity whose cap is needed
   * @return the declared maximum health, or {@link Integer#MAX_VALUE} when there is none
   */
  public static int maxHealthOf(Entity entity) {
    if (entity == null) {
      return Integer.MAX_VALUE;
    }
    ConsumableUseComponent useComponent = entity.getComponent(ConsumableUseComponent.class);
    return useComponent == null ? Integer.MAX_VALUE : useComponent.getMaxHealth();
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
