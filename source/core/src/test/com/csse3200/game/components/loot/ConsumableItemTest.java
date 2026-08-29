package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ConsumableItemTest {
  private static final ConsumableEffect NO_OP = entity -> true;

  @Test
  void shouldCreateConsumableCorrectly() {
    ConsumableItem potion =
        new ConsumableItem("Health Potion", ConsumableType.HEALTH_POTION, NO_OP, 3, 9);

    assertEquals("Health Potion", potion.getName());
    assertEquals(ItemType.CONSUMABLE, potion.getItemType());
    assertEquals(ConsumableType.HEALTH_POTION, potion.getConsumableType());
    assertEquals(3, potion.getQuantity());
    assertEquals(9, potion.getMaxQuantity());
    assertSame(NO_OP, potion.getEffect());
  }

  @Test
  void shouldExposeTexturePathForEachType() {
    assertEquals("images/Health.png", ConsumableType.HEALTH_POTION.getTexturePath());
    assertEquals("images/Poison.png", ConsumableType.DAMAGE_BUFF.getTexturePath());
    assertEquals("images/Strength.png", ConsumableType.SPEED_BUFF.getTexturePath());

    ConsumableItem potion =
        new ConsumableItem("Health Potion", ConsumableType.HEALTH_POTION, NO_OP, 1, 9);
    assertEquals("images/Health.png", potion.getTexturePath());
  }

  @Test
  void shouldDelegateUseToEffect() {
    ConsumableItem applied =
        new ConsumableItem("Applied", ConsumableType.HEALTH_POTION, entity -> true, 1, 9);
    ConsumableItem wasted =
        new ConsumableItem("Wasted", ConsumableType.HEALTH_POTION, entity -> false, 1, 9);

    assertTrue(applied.use(new Entity()));
    assertFalse(wasted.use(new Entity()));
  }

  @Test
  void shouldRejectNullTypeAndEffect() {
    assertThrows(
        IllegalArgumentException.class, () -> new ConsumableItem("Potion", null, NO_OP, 1, 9));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ConsumableItem("Potion", ConsumableType.HEALTH_POTION, null, 1, 9));
  }

  @Test
  void shouldStillValidateBaseItemRules() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ConsumableItem("", ConsumableType.HEALTH_POTION, NO_OP, 1, 9));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ConsumableItem("Potion", ConsumableType.HEALTH_POTION, NO_OP, 1, 0));
  }
}
