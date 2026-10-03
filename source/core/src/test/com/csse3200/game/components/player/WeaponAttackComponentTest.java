package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

class WeaponAttackComponentTest {
  @Test
  void shouldKeepSingleProjectileAlignedWithAim() {
    Vector2 direction = WeaponAttackComponent.getBowProjectileDirection(new Vector2(2f, 0f), 0, 1);

    assertEquals(1f, direction.x, 0.001f);
    assertEquals(0f, direction.y, 0.001f);
  }

  @Test
  void shouldSpreadMultipleProjectilesSymmetricallyAroundAim() {
    Vector2 aim = new Vector2(1f, 0f);
    Vector2 first = WeaponAttackComponent.getBowProjectileDirection(aim, 0, 5);
    Vector2 center = WeaponAttackComponent.getBowProjectileDirection(aim, 2, 5);
    Vector2 last = WeaponAttackComponent.getBowProjectileDirection(aim, 4, 5);

    assertEquals(352.5f, first.angleDeg(), 0.001f);
    assertEquals(0f, center.angleDeg(), 0.001f);
    assertEquals(7.5f, last.angleDeg(), 0.001f);
  }

  @Test
  void shouldRejectInvalidProjectileIndex() {
    assertThrows(
        IllegalArgumentException.class,
        () -> WeaponAttackComponent.getBowProjectileDirection(new Vector2(1f, 0f), 1, 1));
  }
}
