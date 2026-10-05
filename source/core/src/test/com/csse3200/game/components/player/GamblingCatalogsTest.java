package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.extensions.GameExtension;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(GameExtension.class)
class GamblingCatalogsTest {
  private static final Supplier<Item> POTION = () -> new Item("Potion", ItemType.CONSUMABLE, 1, 9);

  @Test
  void shouldCreateNewItemForEveryWin() {
    GamblingCatalogs.ItemPrize prize =
        new GamblingCatalogs.ItemPrize("Potion", ItemType.CONSUMABLE, POTION);

    assertNotSame(
        prize.create(), prize.create(), "each win must be a new item, never a shared instance");
  }

  @Test
  void shouldKeepTypedItemFromFactory() {
    WeaponGenerator generator = new WeaponGenerator();
    GamblingCatalogs.ItemPrize prize =
        new GamblingCatalogs.ItemPrize(
            "Basic Bow", ItemType.WEAPON, () -> generator.generateWeapon(WeaponType.BOW, 3));

    assertInstanceOf(
        WeaponItem.class, prize.create(), "a weapon prize must not be demoted to a plain Item");
  }

  @Test
  void shouldRejectNullDisplayName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GamblingCatalogs.ItemPrize(null, ItemType.CONSUMABLE, POTION));
  }

  @Test
  void shouldRejectBlankDisplayName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GamblingCatalogs.ItemPrize(" ", ItemType.CONSUMABLE, POTION));
  }

  @Test
  void shouldRejectNullItemType() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GamblingCatalogs.ItemPrize("Potion", null, POTION));
  }

  /** Gold is not an inventory item; it must be a {@link GamblingCatalogs.GoldPrize}. */
  @Test
  void shouldRejectCurrencyItemPrize() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GamblingCatalogs.ItemPrize("Gold", ItemType.CURRENCY, POTION));
  }

  @Test
  void shouldRejectNullFactory() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GamblingCatalogs.ItemPrize("Potion", ItemType.CONSUMABLE, null));
  }

  @Test
  void shouldFailCreateWhenFactoryReturnsNull() {
    GamblingCatalogs.ItemPrize prize =
        new GamblingCatalogs.ItemPrize("Potion", ItemType.CONSUMABLE, () -> null);

    assertThrows(IllegalStateException.class, prize::create);
  }

  @Test
  void shouldFailCreateWhenFactoryReturnsWrongType() {
    GamblingCatalogs.ItemPrize prize =
        new GamblingCatalogs.ItemPrize("Sword", ItemType.WEAPON, POTION);

    assertThrows(IllegalStateException.class, prize::create);
  }

  @Test
  void shouldStoreGoldAmount() {
    assertEquals(
        15, new GamblingCatalogs.GoldPrize(15).getAmount(), "gold prize should keep its amount");
  }

  @Test
  void shouldAcceptSmallestGoldAmount() {
    assertEquals(1, new GamblingCatalogs.GoldPrize(1).getAmount(), "1 gold is the lower bound");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -5})
  void shouldRejectNonPositiveGold(int amount) {
    assertThrows(IllegalArgumentException.class, () -> new GamblingCatalogs.GoldPrize(amount));
  }
}
