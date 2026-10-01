package com.csse3200.game.components.loot;

import com.csse3200.game.components.player.ConsumableUseComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Upgrade Stone's effect: upgrades the weapon the player is holding by one tier.
 *
 * <p>The upgrade itself (stats, projectile count, dagger turning into a sword) comes from {@link
 * WeaponUpgrader}. This class only connects it to the inventory: it takes the held weapon out of
 * its slot, puts the upgraded weapons back, and selects them so the player is holding them again.
 *
 * <p>One stone upgrades every weapon in the held stack, not just one: for example, 20 tier 1
 * daggers become 20 tier 2 swords, because a dagger turns into a sword on its first upgrade. {@link
 * WeaponUpgrader} upgrades one weapon at a time, so each weapon in the stack is upgraded and added
 * on its own.
 *
 * <p>Using the stone means pressing its slot key, which selects the stone's slot first. The weapon
 * to upgrade is therefore the one the player last selected, which {@link ConsumableUseComponent}
 * remembers.
 *
 * <p>Like a health potion at full health, the stone is kept (the effect returns {@code false}) when
 * there is no weapon to upgrade or the weapon is already at its highest tier.
 */
public class WeaponUpgradeEffect implements ConsumableEffect {
  private static final Logger logger = LoggerFactory.getLogger(WeaponUpgradeEffect.class);

  private final WeaponUpgrader upgrader = new WeaponUpgrader();

  /**
   * Upgrades the weapon the entity last held.
   *
   * @param entity entity using the stone
   * @return {@code true} if a weapon was upgraded and the stone should be used up
   */
  @Override
  public boolean apply(Entity entity) {
    if (entity == null) {
      return false;
    }
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    ConsumableUseComponent user = entity.getComponent(ConsumableUseComponent.class);
    if (inventory == null || user == null) {
      return false;
    }

    int weaponSlot = user.getHeldWeaponSlot();
    if (!(inventory.getItem(weaponSlot) instanceof WeaponItem weapon)) {
      logger.debug("No held weapon to upgrade");
      return false;
    }

    WeaponItem upgraded;
    try {
      upgraded = upgrader.upgrade(weapon);
    } catch (IllegalStateException alreadyAtMaxTier) {
      logger.debug("{} is already at its highest tier", weapon.getName());
      return false;
    }

    int count = weapon.getQuantity();
    if (!hasRoomFor(inventory, upgraded, count)) {
      logger.debug("Not enough inventory space to upgrade {} x{}", weapon.getName(), count);
      return false;
    }

    // Add the upgraded weapons one at a time. Each one stacks onto the last, and when a stack is
    // full the next weapon starts a new stack. Adding a large quantity in one call is avoided
    // because the inventory would build the extra stacks as plain items rather than weapons.
    inventory.removeItem(weaponSlot);
    inventory.addItem(upgraded);
    for (int i = 1; i < count; i++) {
      inventory.addItem(upgrader.upgrade(weapon));
    }
    int newSlot = selectWeapon(inventory, upgraded);

    logger.info(
        "Upgraded {} x{} (tier {}) to {} (tier {})",
        weapon.getName(),
        count,
        weapon.getTier(),
        upgraded.getName(),
        upgraded.getTier());
    entity.getEvents().trigger("weaponUpgraded", inventory.getItem(newSlot));
    return true;
  }

  /**
   * Checks there will be enough empty slots for the upgraded stack. The held weapon's own slot is
   * freed first, so it counts as empty.
   *
   * <p>A stack can need more than one slot after upgrading: 20 daggers fit in one slot, but 20
   * swords need two, because a sword stack holds fewer.
   *
   * @param inventory the player's inventory
   * @param upgraded one upgraded weapon, used for its stack size
   * @param count how many weapons are being upgraded
   * @return {@code true} if every upgraded weapon will fit
   */
  private boolean hasRoomFor(InventoryComponent inventory, WeaponItem upgraded, int count) {
    int stackSize = upgraded.getMaxQuantity();
    int slotsNeeded = (count + stackSize - 1) / stackSize; // count divided by stackSize, rounded up
    int emptySlots = inventory.getMaxSlots() - inventory.getOccupiedSlots() + 1;
    return slotsNeeded <= emptySlots;
  }

  /**
   * Selects the slot holding the upgraded weapon, so the player is holding it straight away.
   *
   * <p>The weapon is found by type and tier rather than by the exact object, because the upgraded
   * weapons can merge into a stack of the same weapon the player already had.
   *
   * @param inventory the player's inventory
   * @param upgraded the weapon that was just added
   * @return the selected slot, or the current active slot if the weapon was not found
   */
  private int selectWeapon(InventoryComponent inventory, WeaponItem upgraded) {
    for (int slot = 1; slot <= inventory.getMaxSlots(); slot++) {
      if (inventory.getItem(slot) instanceof WeaponItem weapon
          && weapon.getWeaponType() == upgraded.getWeaponType()
          && weapon.getTier() == upgraded.getTier()) {
        inventory.setActiveSlot(slot);
        return slot;
      }
    }
    return inventory.getActiveSlot();
  }
}
