package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PersistentLootIdComponentTest {
  private static final String LOOT_ID = "Level 1:10,4";

  @BeforeEach
  void setUp() {
    ServiceLocator.registerEntityService(new EntityService());
    LootRegistry.loadFrom(new ArrayList<>());
  }

  @AfterEach
  void tearDown() {
    LootRegistry.loadFrom(new ArrayList<>());
  }

  private LootPickupComponent pickup;

  private Entity createLoot() {
    pickup = new LootPickupComponent(new Item("Health Potion", ItemType.CONSUMABLE, 1, 5));
    Entity loot =
        new Entity().addComponent(pickup).addComponent(new PersistentLootIdComponent(LOOT_ID));
    loot.create();
    return loot;
  }

  /** Marks the loot as picked up, the same flag LootPickupComponent sets on a real pickup. */
  private void markPickedUp() throws Exception {
    Field collected = LootPickupComponent.class.getDeclaredField("collected");
    collected.setAccessible(true);
    collected.set(pickup, true);
  }

  @Test
  void pickedUpLootIsRecordedWhenDisposed() throws Exception {
    Entity loot = createLoot();
    markPickedUp();

    loot.dispose();

    assertTrue(LootRegistry.isCollected(LOOT_ID));
  }

  @Test
  void lootLeftInAnUnloadedLevelIsNotRecorded() {
    Entity loot = createLoot();

    loot.dispose();

    assertFalse(LootRegistry.isCollected(LOOT_ID));
  }

  @Test
  void lootWithoutPickupComponentIsNotRecordedAndDoesNotCrash() {
    Entity loot = new Entity().addComponent(new PersistentLootIdComponent(LOOT_ID));
    loot.create();

    assertDoesNotThrow(loot::dispose);
    assertFalse(LootRegistry.isCollected(LOOT_ID));
  }
}
