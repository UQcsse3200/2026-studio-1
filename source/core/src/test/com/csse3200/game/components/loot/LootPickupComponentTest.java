package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ShieldComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class LootPickupComponentTest {
  private GameTime timeSource;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    timeSource = mock(GameTime.class);
    when(timeSource.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(timeSource);
  }

  @Test
  void shouldTemporarilyBlockPickupByPlayerWhoDroppedItem() {
    Entity player = createPlayer(new InventoryComponent(0, 2));
    Item item = new Item("Potion", ItemType.CONSUMABLE, 1, 9);
    Entity loot = createLoot(new LootPickupComponent(item, player, 750L));

    triggerPlayerCollision(loot, player);
    assertEquals(0, player.getComponent(InventoryComponent.class).getOccupiedSlots());

    when(timeSource.getTime()).thenReturn(750L);
    triggerPlayerCollision(loot, player);
    assertEquals(1, player.getComponent(InventoryComponent.class).getTotalQuantity(item));
  }

  @Test
  void shouldKeepOnlyUncollectedQuantityInWorld() {
    InventoryComponent inventory = new InventoryComponent(0, 1);
    inventory.addItem(new Item("Potion", ItemType.CONSUMABLE, 8, 10));
    Entity player = createPlayer(inventory);
    Item worldItem = new Item("Potion", ItemType.CONSUMABLE, 5, 10);
    Entity loot = createLoot(new LootPickupComponent(worldItem));

    triggerPlayerCollision(loot, player);

    assertEquals(10, inventory.getItem(1).getQuantity());
    assertEquals(3, worldItem.getQuantity());
  }

  /** Acceptance criterion: picking up a shield item grants the shield, bypassing inventory. */
  @Test
  void shouldGrantShieldOnPickupWithoutUsingAnInventorySlot() {
    Entity player = createPlayerWithShield(new InventoryComponent(0, 1));
    Item shieldItem = new Item("Shield", ItemType.SHIELD, 1, 1);
    Entity loot = createLoot(new LootPickupComponent(shieldItem));

    triggerPlayerCollision(loot, player);

    assertTrue(
        player.getComponent(ShieldComponent.class).hasShield(),
        "the player should hold a shield after collecting a shield item");
    assertEquals(
        0,
        player.getComponent(InventoryComponent.class).getOccupiedSlots(),
        "a shield pickup should not occupy an inventory slot");
  }

  /** Acceptance criterion: a shield item is ignored if the player has no ShieldComponent. */
  @Test
  void shouldNotCollectShieldItemWithoutAShieldComponent() {
    Entity player = createPlayer(new InventoryComponent(0, 1));
    Item shieldItem = new Item("Shield", ItemType.SHIELD, 1, 1);
    LootPickupComponent pickup = new LootPickupComponent(shieldItem);
    Entity loot = createLoot(pickup);

    triggerPlayerCollision(loot, player);

    assertFalse(
        loot.getComponent(HitboxComponent.class) == null, "loot entity should be untouched");
    assertEquals(
        0,
        player.getComponent(InventoryComponent.class).getOccupiedSlots(),
        "inventory should remain unaffected when the player cannot hold a shield");
  }

  private Entity createPlayer(InventoryComponent inventory) {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(inventory);
    player.create();
    return player;
  }

  private Entity createPlayerWithShield(InventoryComponent inventory) {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(inventory)
            .addComponent(new ShieldComponent());
    player.create();
    return player;
  }

  private Entity createLoot(LootPickupComponent pickupComponent) {
    Entity loot =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(pickupComponent);
    loot.create();
    return loot;
  }

  private void triggerPlayerCollision(Entity loot, Entity player) {
    Fixture lootFixture = loot.getComponent(HitboxComponent.class).getFixture();
    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    loot.getEvents().trigger("collisionStart", lootFixture, playerFixture);
  }
}
