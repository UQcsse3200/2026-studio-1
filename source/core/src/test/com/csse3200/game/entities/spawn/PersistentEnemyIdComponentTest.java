package com.csse3200.game.entities.spawn;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.Entity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PersistentEnemyIdComponentTest {
  private static final String ENEMY_ID = "level-1:3,4";

  @BeforeEach
  void clearRegistry() {
    EnemyRegistry.loadFrom(List.of());
  }

  @Test
  void recordsEnemyAfterDeathEvent() {
    Entity enemy = new Entity().addComponent(new PersistentEnemyIdComponent(ENEMY_ID));
    enemy.create();

    enemy.getEvents().trigger("death");

    assertTrue(EnemyRegistry.isKilled(ENEMY_ID));
  }

  @Test
  void doesNotRecordEnemyWhenRoomUnloads() {
    PersistentEnemyIdComponent component = new PersistentEnemyIdComponent(ENEMY_ID);
    Entity enemy = new Entity().addComponent(component);
    enemy.create();

    component.dispose();

    assertFalse(EnemyRegistry.isKilled(ENEMY_ID));
  }
}
