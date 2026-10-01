package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.ConsumableUseComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Uses the Upgrade Stone the same way the keyboard does: select the stone's slot, then trigger
 * {@code "useItem"} for that slot.
 */
@ExtendWith(GameExtension.class)
class WeaponUpgradeEffectTest {
  private final WeaponGenerator weapons = new WeaponGenerator();
  private final ConsumableGenerator consumables = new ConsumableGenerator();
  private final WeaponUpgrader upgrader = new WeaponUpgrader();
  private Entity player;
  private InventoryComponent inventory;

  @BeforeEach
  void setUp() {
    player =
        new Entity()
            .addComponent(new InventoryComponent(0, 5))
            .addComponent(new ConsumableUseComponent(100));
    inventory = player.getComponent(InventoryComponent.class);
  }

  /** Adds items in order (slot 1, 2, ...) and then creates the player, like PlayerFactory does. */
  private void givePlayer(Item... items) {
    for (Item item : items) {
      inventory.addItem(item);
    }
    player.create();
  }

  private ConsumableItem stone() {
    return consumables.generateConsumable(ConsumableType.UPGRADE_STONE, 1);
  }

  /**
   * Returns a dagger whose next upgrade turns it into a sword. Under the team's rule that is a tier
   * 1 dagger. Searching for it keeps these tests about the stone, not about which tier
   * WeaponUpgrader uses for the change.
   */
  private WeaponItem daggerThatBecomesASword() {
    for (int tier = 1; tier <= WeaponTier.values().length; tier++) {
      WeaponItem dagger = weapons.generateWeapon(WeaponType.DAGGER, tier);
      if (upgrader.upgrade(dagger).getWeaponType() == WeaponType.SWORD) {
        return dagger;
      }
    }
    throw new AssertionError("WeaponUpgrader never turns a dagger into a sword");
  }

  /** Adds up how many weapons of a type and tier the inventory holds, across all slots. */
  private int countWeapons(WeaponType type, int tier) {
    int total = 0;
    for (int slot = 1; slot <= inventory.getMaxSlots(); slot++) {
      if (inventory.getItem(slot) instanceof WeaponItem weapon
          && weapon.getWeaponType() == type
          && weapon.getTier() == tier) {
        total += weapon.getQuantity();
      }
    }
    return total;
  }

  /** Presses the number key for a slot: selects it, then uses whatever is in it. */
  private void pressSlotKey(int slot) {
    inventory.setActiveSlot(slot);
    player.getEvents().trigger("useItem", slot);
  }

  @Test
  void shouldUpgradeTheHeldWeaponByOneTier() {
    givePlayer(weapons.generateWeapon(WeaponType.SWORD, 1), stone());

    pressSlotKey(2);

    WeaponItem sword = assertInstanceOf(WeaponItem.class, inventory.getItem(1));
    assertEquals(WeaponType.SWORD, sword.getWeaponType());
    assertEquals(2, sword.getTier());
    assertEquals(WeaponTier.TIER_2.getStats(WeaponType.SWORD).getDamage(), sword.getDamage());
  }

  @Test
  void shouldUseUpTheStoneAndHandTheWeaponBack() {
    givePlayer(weapons.generateWeapon(WeaponType.SWORD, 1), stone());

    pressSlotKey(2);

    assertNull(inventory.getItem(2), "the stone should be used up");
    assertEquals(1, inventory.getActiveSlot(), "the player should be holding the upgraded sword");
  }

  @Test
  void shouldGiveTheBowMoreArrowsWhenUpgraded() {
    givePlayer(weapons.generateWeapon(WeaponType.BOW, 1), stone());
    int arrowsBefore = ((WeaponItem) inventory.getItem(1)).getProjectileCount();

    pressSlotKey(2);

    WeaponItem bow = (WeaponItem) inventory.getItem(1);
    assertEquals(2, bow.getTier());
    assertTrue(bow.getProjectileCount() > arrowsBefore, "a tier 2 bow should fire more arrows");
  }

  @Test
  void shouldUpgradeTheWeaponSelectedBeforeTheStone() {
    givePlayer(
        weapons.generateWeapon(WeaponType.SWORD, 1),
        weapons.generateWeapon(WeaponType.BOW, 1),
        stone());

    inventory.setActiveSlot(2);
    pressSlotKey(3);

    assertEquals(1, ((WeaponItem) inventory.getItem(1)).getTier(), "the sword was not held");
    assertEquals(2, ((WeaponItem) inventory.getItem(2)).getTier(), "the bow was held");
  }

  @Test
  void shouldKeepTheStoneWhenTheWeaponIsAlreadyAtMaxTier() {
    WeaponItem maxSword = weapons.generateWeapon(WeaponType.SWORD, WeaponTier.values().length);
    givePlayer(maxSword, stone());

    pressSlotKey(2);

    assertSame(maxSword, inventory.getItem(1));
    assertEquals(1, inventory.getItem(2).getQuantity(), "the stone should not be used up");
  }

  @Test
  void shouldUpgradeEveryWeaponInTheStack() {
    WeaponItem daggers = weapons.generateWeapon(WeaponType.DAGGER, 1);
    daggers.setQuantity(15);
    WeaponItem expected = upgrader.upgrade(daggers);
    givePlayer(daggers, stone());

    pressSlotKey(2);

    assertEquals(
        15,
        countWeapons(expected.getWeaponType(), expected.getTier()),
        "all 15 daggers should be upgraded, not merged into one");
  }

  @Test
  void shouldSplitTheSwordsAcrossStacksWhenDaggersBecomeSwords() {
    WeaponItem daggers = daggerThatBecomesASword();
    daggers.setQuantity(12);
    givePlayer(daggers, stone());

    pressSlotKey(2);

    // A sword stack holds 10, so 12 swords fill one slot and start a second stack. Every slot must
    // still hold a real weapon, not a plain item.
    assertEquals(12, countWeapons(WeaponType.SWORD, upgrader.upgrade(daggers).getTier()));
    assertNull(inventory.getItem(2), "only the stone's slot should be empty");
  }

  @Test
  void shouldKeepTheStoneWhenThereIsNoRoomForTheUpgradedStack() {
    WeaponItem daggers = daggerThatBecomesASword();
    daggers.setQuantity(20);
    givePlayer(
        daggers,
        stone(),
        consumables.generateConsumable(ConsumableType.HEALTH_POTION, 1),
        consumables.generateConsumable(ConsumableType.SPEED_BUFF, 1),
        consumables.generateConsumable(ConsumableType.RESISTANCE, 1));

    // 20 swords need two slots, but only the daggers' own slot would be free.
    pressSlotKey(2);

    assertSame(daggers, inventory.getItem(1), "the daggers should be left as they were");
    assertEquals(20, daggers.getQuantity());
    assertEquals(1, inventory.getItem(2).getQuantity(), "the stone should not be used up");
  }

  @Test
  void shouldKeepTheStoneWhenNoWeaponHasBeenHeld() {
    givePlayer(stone());

    pressSlotKey(1);

    assertEquals(1, inventory.getItem(1).getQuantity());
  }

  @Test
  void shouldKeepTheStoneWhenTheHeldWeaponIsGone() {
    givePlayer(weapons.generateWeapon(WeaponType.SWORD, 1), stone());
    inventory.removeItem(1);
    inventory.addItem(consumables.generateConsumable(ConsumableType.HEALTH_POTION, 1));

    pressSlotKey(2);

    assertInstanceOf(ConsumableItem.class, inventory.getItem(1), "the potion is untouched");
    assertEquals(1, inventory.getItem(2).getQuantity(), "the stone should not be used up");
  }

  @Test
  void shouldAnnounceTheUpgradedWeapon() {
    List<WeaponItem> announced = new ArrayList<>();
    givePlayer(weapons.generateWeapon(WeaponType.SWORD, 1), stone());
    player.getEvents().addListener("weaponUpgraded", (WeaponItem weapon) -> announced.add(weapon));

    pressSlotKey(2);

    assertEquals(1, announced.size());
    assertSame(inventory.getItem(1), announced.getFirst());
  }

  @Test
  void shouldDoNothingForAnEntityWithoutAnInventory() {
    assertFalse(new WeaponUpgradeEffect().apply(new Entity()));
    assertFalse(new WeaponUpgradeEffect().apply(null));
  }

  @Test
  void shouldOnlyTreatTheStoneAsANonPotion() {
    for (ConsumableType type : ConsumableType.values()) {
      assertEquals(type != ConsumableType.UPGRADE_STONE, type.isPotion(), type.toString());
    }
  }
}
