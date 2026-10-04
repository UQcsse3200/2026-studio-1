package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.csse3200.game.components.attacks.MeleeAttackComponent;
import com.csse3200.game.components.attacks.RangedAttackComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.WeaponAttackComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that {@code AXE} is supported everywhere a weapon type is consumed: the generator, the
 * item, the upgrader, the melee and ranged attack components, the player's weapon attack and the
 * inventory. Generation basics (name, tier damage, windup, sell price, max stack) and the tier stat
 * table are already covered in WeaponGeneratorTest, WeaponItemTest and WeaponTierTest, so they are
 * not repeated here.
 */
@ExtendWith(GameExtension.class)
class WeaponAxeTest {
  private final WeaponGenerator generator = new WeaponGenerator();
  private final WeaponUpgrader upgrader = new WeaponUpgrader();

  // ---------- generator edges ----------

  @Test
  void shouldRejectAnAxeAboveTheTopTier() {
    assertThrows(IllegalArgumentException.class, () -> generator.generateWeapon(WeaponType.AXE, 4));
  }

  @Test
  void shouldRejectAnAxeAtTierZeroOrBelow() {
    assertThrows(IllegalArgumentException.class, () -> generator.generateWeapon(WeaponType.AXE, 0));
    assertThrows(
        IllegalArgumentException.class, () -> generator.generateWeapon(WeaponType.AXE, -1));
  }

  @Test
  void shouldGiveEachGeneratedAxeTheTierItWasAskedFor() {
    for (int tier = 1; tier <= 3; tier++) {
      assertEquals(tier, generator.generateWeapon(WeaponType.AXE, tier).getTier());
    }
  }

  // ---------- the axe's numbers through the item ----------

  @Test
  void shouldExposeTheTierOneAxeStatsThroughTheItem() {
    WeaponItem axe = generator.generateWeapon(WeaponType.AXE, 1);

    assertEquals(12, axe.getDamage());
    assertEquals(1.0f, axe.getAttackSpeed(), 1e-6f);
    assertEquals(1.3f, axe.getKnockback(), 1e-6f);
    assertEquals(2.3f, axe.getRange(), 1e-6f);
    assertEquals(1, axe.getProjectileCount());
  }

  @Test
  void shouldExposeTheTierThreeAxeStatsThroughTheItem() {
    WeaponItem axe = generator.generateWeapon(WeaponType.AXE, 3);

    assertEquals(34, axe.getDamage());
    assertEquals(1.5f, axe.getAttackSpeed(), 1e-6f);
    assertEquals(2.3f, axe.getKnockback(), 1e-6f);
    assertEquals(3.3f, axe.getRange(), 1e-6f);
    assertEquals(1, axe.getProjectileCount());
  }

  @Test
  void shouldHitHarderThanTheSwordAtEveryTier() {
    for (int tier = 1; tier <= 3; tier++) {
      int axe = generator.generateWeapon(WeaponType.AXE, tier).getDamage();
      int sword = generator.generateWeapon(WeaponType.SWORD, tier).getDamage();

      assertTrue(axe > sword, "tier " + tier + ": axe " + axe + " must beat sword " + sword);
    }
  }

  @Test
  void shouldKnockBackFartherAndReachFurtherThanTheSwordAtEveryTier() {
    for (int tier = 1; tier <= 3; tier++) {
      WeaponItem axe = generator.generateWeapon(WeaponType.AXE, tier);
      WeaponItem sword = generator.generateWeapon(WeaponType.SWORD, tier);

      assertTrue(axe.getKnockback() > sword.getKnockback(), "knockback, tier " + tier);
      assertTrue(axe.getRange() > sword.getRange(), "range, tier " + tier);
    }
  }

  // ---------- upgrader ----------

  @Test
  void shouldUpgradeATierOneAxeToATierTwoAxe() {
    WeaponItem upgraded = upgrader.upgrade(generator.generateWeapon(WeaponType.AXE, 1));

    assertEquals(WeaponType.AXE, upgraded.getWeaponType());
    assertEquals(2, upgraded.getTier());
    assertEquals("Basic Axe", upgraded.getName());
  }

  @Test
  void shouldUpgradeAnAxeThroughEveryTier() {
    WeaponItem two = upgrader.upgrade(generator.generateWeapon(WeaponType.AXE, 1));
    WeaponItem three = upgrader.upgrade(two);

    assertEquals(WeaponType.AXE, three.getWeaponType());
    assertEquals(3, three.getTier());
  }

  @Test
  void shouldRejectUpgradingAMaxTierAxeAndNameTheWeapon() {
    WeaponItem top = generator.generateWeapon(WeaponType.AXE, 3);

    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> upgrader.upgrade(top));

    assertTrue(error.getMessage().contains("AXE"), "message should name the weapon type");
  }

  @Test
  void shouldGiveTheUpgradedAxeMoreDamageAndAHigherSellPrice() {
    WeaponItem before = generator.generateWeapon(WeaponType.AXE, 1);
    WeaponItem after = upgrader.upgrade(before);

    assertTrue(after.getDamage() > before.getDamage());
    assertEquals(12, before.getSellPrice());
    assertEquals(24, after.getSellPrice());
  }

  @Test
  void shouldLeaveTheOriginalAxeUntouchedWhenUpgrading() {
    WeaponItem before = generator.generateWeapon(WeaponType.AXE, 1);

    WeaponItem after = upgrader.upgrade(before);

    assertNotSame(before, after);
    assertEquals(1, before.getTier());
    assertEquals(12, before.getDamage());
  }

  // ---------- melee and ranged attack components ----------

  @Test
  void shouldBeAcceptedByTheMeleeAttackComponentAndReportItsDamage() {
    WeaponItem axe = generator.generateWeapon(WeaponType.AXE, 1);

    MeleeAttackComponent melee = new MeleeAttackComponent(2f, 5f, 0f, axe);

    assertEquals(12, melee.getDamage());
  }

  @Test
  void shouldBeAcceptedByTheMeleeAttackComponentWithTheWindupSetToZeroLikeTheMinotaur() {
    WeaponItem axe = generator.generateWeapon(WeaponType.AXE, 2);
    axe.setWindupDuration(0f);

    MeleeAttackComponent melee = new MeleeAttackComponent(2.5f, 5f, 2f, axe);

    assertEquals(0f, melee.getWindupDuration(), 1e-6f);
    assertEquals(23, melee.getDamage());
  }

  @Test
  void shouldBeRejectedByTheMeleeAttackComponentWhenTheWindupIsNotBelowTheCooldown() {
    WeaponItem axe = generator.generateWeapon(WeaponType.AXE, 1); // windup 3

    assertThrows(IllegalArgumentException.class, () -> new MeleeAttackComponent(2f, 3f, 0f, axe));
    assertThrows(IllegalArgumentException.class, () -> new MeleeAttackComponent(2f, 2f, 0f, axe));
  }

  @Test
  void shouldBeRejectedByTheRangedAttackComponent() {
    WeaponItem axe = generator.generateWeapon(WeaponType.AXE, 1);

    assertThrows(IllegalArgumentException.class, () -> new RangedAttackComponent(5f, 5f, 0f, axe));
  }

  // ---------- the player's weapon attack ----------

  @Test
  void shouldAnnounceAnAxeSwingCarryingTheAxesDamageAndNotThrow() {
    WeaponItem axe = generator.generateWeapon(WeaponType.AXE, 1);
    Entity player = new Entity().addComponent(new WeaponAttackComponent(axe));
    player.create();
    List<Integer> swings = new ArrayList<>();
    player.getEvents().addListener("axeAttack", (Integer damage) -> swings.add(damage));

    assertDoesNotThrow(() -> player.getEvents().trigger("weaponAttack"));

    assertEquals(1, swings.size(), "one swing per attack");
    assertEquals(12, swings.get(0));
  }

  @Test
  void shouldCarryTheTierThreeAxeDamageInTheSwing() {
    WeaponItem axe = generator.generateWeapon(WeaponType.AXE, 3);
    Entity player = new Entity().addComponent(new WeaponAttackComponent(axe));
    player.create();
    List<Integer> swings = new ArrayList<>();
    player.getEvents().addListener("axeAttack", (Integer damage) -> swings.add(damage));

    player.getEvents().trigger("weaponAttack");

    assertEquals(34, swings.get(0));
  }

  @Test
  void shouldNotAnnounceASwordSwingForAnAxe() {
    WeaponItem axe = generator.generateWeapon(WeaponType.AXE, 1);
    Entity player = new Entity().addComponent(new WeaponAttackComponent(axe));
    player.create();
    List<Integer> swordSwings = new ArrayList<>();
    player.getEvents().addListener("swordAttack", (Integer damage) -> swordSwings.add(damage));

    player.getEvents().trigger("weaponAttack");

    assertTrue(swordSwings.isEmpty(), "the axe has its own swing event");
  }

  @Test
  void shouldNotSpawnAnArrowOrADaggerWhenSwingingAnAxe() {
    EntityService entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);
    WeaponItem axe = generator.generateWeapon(WeaponType.AXE, 1);
    Entity player = new Entity().addComponent(new WeaponAttackComponent(axe));
    player.create();

    player.getEvents().trigger("weaponAttack");

    verify(entityService, never()).register(any(Entity.class));
  }

  @Test
  void shouldSwingTheAxeInTheActiveInventorySlotInsteadOfTheConstructorWeapon() {
    WeaponItem sword = generator.generateWeapon(WeaponType.SWORD, 1);
    WeaponItem axe = generator.generateWeapon(WeaponType.AXE, 2);
    InventoryComponent inventory = new InventoryComponent(0);
    Entity player =
        new Entity().addComponent(new WeaponAttackComponent(sword)).addComponent(inventory);
    player.create();
    inventory.addItem(axe); // slot 1 is the active slot
    List<Integer> axeSwings = new ArrayList<>();
    List<Integer> swordSwings = new ArrayList<>();
    player.getEvents().addListener("axeAttack", (Integer damage) -> axeSwings.add(damage));
    player.getEvents().addListener("swordAttack", (Integer damage) -> swordSwings.add(damage));

    player.getEvents().trigger("weaponAttack");

    assertEquals(1, axeSwings.size());
    assertEquals(23, axeSwings.get(0));
    assertTrue(swordSwings.isEmpty());
  }

  // ---------- inventory ----------

  @Test
  void shouldStackTwoAxesOfTheSameTierAndStartASecondStackForTheThird() {
    InventoryComponent inventory = new InventoryComponent(0);

    inventory.addItem(generator.generateWeapon(WeaponType.AXE, 1));
    inventory.addItem(generator.generateWeapon(WeaponType.AXE, 1));
    inventory.addItem(generator.generateWeapon(WeaponType.AXE, 1));

    assertEquals(2, inventory.getItem(1).getQuantity(), "an axe stack holds at most 2");
    assertNotNull(inventory.getItem(2));
    assertEquals(1, inventory.getItem(2).getQuantity());
  }

  @Test
  void shouldNotStackAxesOfDifferentTiers() {
    InventoryComponent inventory = new InventoryComponent(0);

    inventory.addItem(generator.generateWeapon(WeaponType.AXE, 1));
    inventory.addItem(generator.generateWeapon(WeaponType.AXE, 2));

    assertEquals(1, inventory.getItem(1).getQuantity());
    assertNotNull(inventory.getItem(2), "a tier-2 axe needs its own slot");
  }
}
