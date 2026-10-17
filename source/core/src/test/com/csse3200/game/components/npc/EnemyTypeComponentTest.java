package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.components.EnemyType;
import com.csse3200.game.components.enemies.EnemyTypeComponent;
import org.junit.jupiter.api.Test;

/** Tests {@link EnemyTypeComponent}, which turns an {@link EnemyType} into a readable label. */
class EnemyTypeComponentTest {

  @Test
  void shouldLabelASingleWordTypeInTitleCase() {
    assertEquals("Skeleton", new EnemyTypeComponent(EnemyType.SKELETON).getEnemyLabel());
  }

  @Test
  void shouldLabelAMultiWordTypeWithSpacesAndTitleCase() {
    assertEquals(
        "Ranged Skeleton", new EnemyTypeComponent(EnemyType.RANGED_SKELETON).getEnemyLabel());
    assertEquals("Ranged Harpy", new EnemyTypeComponent(EnemyType.RANGED_HARPY).getEnemyLabel());
  }

  @Test
  void shouldGiveEveryEnemyTypeAReadableLabel() {
    for (EnemyType type : EnemyType.values()) {
      String label = new EnemyTypeComponent(type).getEnemyLabel();

      assertFalse(label.isBlank(), type + " has no label");
      assertFalse(label.contains("_"), type + " label still has an underscore: " + label);
      assertEquals(type.name().replace('_', ' ').toLowerCase(), label.toLowerCase());
    }
  }

  @Test
  void shouldRejectANullEnemyType() {
    assertThrows(IllegalArgumentException.class, () -> new EnemyTypeComponent(null));
  }
}
