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
 * its slot, puts the upgraded weapon back, and selects it so the player is holding it again.
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

    // A stack of daggers keeps its size when it is upgraded. When the dagger turns into a sword,
    // the
    // stack is forged into one sword, so the quantity stays at 1.
    if (upgraded.getWeaponType() == weapon.getWeaponType()) {
      upgraded.setQuantity(weapon.getQuantity());
    }

    // Removing the old weapon frees a slot, so the upgraded weapon always has room to go back in.
    inventory.removeItem(weaponSlot);
    inventory.addItem(upgraded);
    selectWeapon(inventory, upgraded);

    logger.info(
        "Upgraded {} (tier {}) to {} (tier {})",
        weapon.getName(),
        weapon.getTier(),
        upgraded.getName(),
        upgraded.getTier());
    entity.getEvents().trigger("weaponUpgraded", upgraded);
    return true;
  }

  /**
   * Selects the slot holding the upgraded weapon, so the player is holding it straight away.
   *
   * <p>The weapon is found by type and tier rather than by the exact object, because a stack of
   * daggers can merge into an existing stack of the same tier when it is added back.
   *
   * @param inventory the player's inventory
   * @param upgraded the weapon that was just added
   */
  private void selectWeapon(InventoryComponent inventory, WeaponItem upgraded) {
    for (int slot = 1; slot <= inventory.getMaxSlots(); slot++) {
      if (inventory.getItem(slot) instanceof WeaponItem weapon
          && weapon.getWeaponType() == upgraded.getWeaponType()
          && weapon.getTier() == upgraded.getTier()) {
        inventory.setActiveSlot(slot);
        return;
      }
    }
  }
}
