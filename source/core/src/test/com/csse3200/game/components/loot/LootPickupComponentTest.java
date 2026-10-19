package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.BallisticShieldComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ShieldComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayDeque;
import java.util.Queue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(GameExtension.class)
class LootPickupComponentTest {
  private GameTime timeSource;
  private Application previousApp;
  private PhysicsEngine physics;
  private EntityService entities;
  private final Queue<Runnable> scheduled = new ArrayDeque<>();

  @BeforeEach
  void setUp() {
    previousApp = Gdx.app;
    Gdx.app = mock(Application.class);
    // Keep disposal on the test thread, after collision dispatch and outside any Box2D step.
    doAnswer(
            invocation -> {
              scheduled.add(invocation.getArgument(0));
              return null;
            })
        .when(Gdx.app)
        .postRunnable(any(Runnable.class));
    timeSource = mock(GameTime.class);
    when(timeSource.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(timeSource);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    physics = ServiceLocator.getPhysicsService().getPhysics();
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
  }

  @AfterEach
  void tearDown() {
    try {
      scheduled.clear();
      entities.dispose();
      physics.dispose();
    } finally {
      scheduled.clear();
      Gdx.app = previousApp;
    }
  }

  @Test
  void shouldTemporarilyBlockPickupByPlayerWhoDroppedItem() {
    when(timeSource.getTime()).thenReturn(1000L);
    Entity player = createPlayer(new InventoryComponent(0, 2));
    Item item = new Item("Potion", ItemType.CONSUMABLE, 1, 9);
    Entity loot = createLoot(new LootPickupComponent(item, player, 750L));

    triggerPlayerCollision(loot, player);
    assertEquals(0, player.getComponent(InventoryComponent.class).getOccupiedSlots());
    assertNotCollected(loot);

    when(timeSource.getTime()).thenReturn(1749L);
    triggerPlayerCollision(loot, player);
    assertEquals(0, player.getComponent(InventoryComponent.class).getOccupiedSlots());
    assertNotCollected(loot);

    when(timeSource.getTime()).thenReturn(1750L);
    triggerPlayerCollision(loot, player);
    assertEquals(1, player.getComponent(InventoryComponent.class).getTotalQuantity(item));
    assertTrue(loot.getComponent(LootPickupComponent.class).isCollected());
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
    assertNotCollected(loot);

    // Re-entering a full stack must not award the part already picked up.
    triggerPlayerCollision(loot, player);
    assertEquals(10, inventory.getItem(1).getQuantity());
    assertEquals(3, worldItem.getQuantity());
    assertNotCollected(loot);

    inventory.removeItem(1, 2);
    triggerPlayerCollision(loot, player);
    assertEquals(10, inventory.getItem(1).getQuantity());
    assertEquals(1, worldItem.getQuantity());
    assertNotCollected(loot);

    inventory.removeItem(1, 2);
    triggerPlayerCollision(loot, player);
    assertEquals(9, inventory.getItem(1).getQuantity());
    assertTrue(loot.getComponent(LootPickupComponent.class).isCollected());
    assertEquals(1, scheduled.size());
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

    assertNotNull(loot.getComponent(HitboxComponent.class), "loot entity should be untouched");
    assertEquals(
        0,
        player.getComponent(InventoryComponent.class).getOccupiedSlots(),
        "inventory should remain unaffected when the player cannot hold a shield");
    assertNotCollected(loot);
  }

  @Test
  void shouldCollectOnceAndDeferEntityDisposal() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Entity player = createPlayer(inventory);
    Item item = new Item("Potion", ItemType.CONSUMABLE, 2, 9);
    LootPickupComponent pickup = new LootPickupComponent(item);
    Entity loot = createLoot(pickup);
    assertSame(item, pickup.getItem());
    assertFalse(pickup.isCollected());

    triggerPlayerCollision(loot, player);
    triggerPlayerCollision(loot, player);

    assertTrue(pickup.isCollected());
    assertEquals(2, inventory.getTotalQuantity(item));
    assertEquals(1, scheduled.size());
    assertFalse(loot.isDisposed(), "Entity disposal must wait until after collision dispatch");
    assertFalse(physics.getWorld().isLocked());
    scheduled.remove().run();
    assertTrue(loot.isDisposed());
    assertTrue(entities.getEntitiesWithComponent(LootPickupComponent.class).isEmpty());
    assertEquals(1, physics.getWorld().getBodyCount(), "Only the player body should remain");
  }

  @Test
  void shouldAllowAnotherPlayerToPickUpDuringTheDropperDelay() {
    Entity dropper = createPlayer(new InventoryComponent(0, 2));
    Entity otherPlayer = createPlayer(new InventoryComponent(0, 2));
    Item item = new Item("Potion", ItemType.CONSUMABLE, 1, 9);
    Entity loot = createLoot(new LootPickupComponent(item, dropper, 750L));

    triggerPlayerCollision(loot, otherPlayer);

    assertEquals(0, dropper.getComponent(InventoryComponent.class).getOccupiedSlots());
    assertEquals(1, otherPlayer.getComponent(InventoryComponent.class).getTotalQuantity(item));
    assertTrue(loot.getComponent(LootPickupComponent.class).isCollected());
    assertEquals(1, scheduled.size());
  }

  @ParameterizedTest
  @ValueSource(longs = {0L, -1L, -750L})
  void shouldAllowImmediatePickupWithANonPositiveDelay(long delay) {
    when(timeSource.getTime()).thenReturn(1000L);
    Entity player = createPlayer(new InventoryComponent(0, 2));
    Item item = new Item("Potion", ItemType.CONSUMABLE, 1, 9);
    Entity loot = createLoot(new LootPickupComponent(player, item, delay));

    triggerPlayerCollision(loot, player);

    assertEquals(1, player.getComponent(InventoryComponent.class).getTotalQuantity(item));
    assertTrue(loot.getComponent(LootPickupComponent.class).isCollected());
  }

  @Test
  void shouldAllowPickupWhenNoClockIsRegistered() {
    Entity player = createPlayer(new InventoryComponent(0, 2));
    ServiceLocator.registerTimeSource(null);
    Item item = new Item("Potion", ItemType.CONSUMABLE, 1, 9);
    Entity loot = createLoot(new LootPickupComponent(item, player, 750L));

    triggerPlayerCollision(loot, player);

    assertEquals(1, player.getComponent(InventoryComponent.class).getTotalQuantity(item));
    assertTrue(loot.getComponent(LootPickupComponent.class).isCollected());
  }

  @Test
  void shouldIgnoreCollisionFromADifferentLootFixture() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Entity player = createPlayer(inventory);
    Entity loot =
        createLoot(new LootPickupComponent(new Item("Potion", ItemType.CONSUMABLE, 1, 9)));
    Fixture otherLootFixture = mock(Fixture.class);

    loot.getEvents()
        .trigger(
            "collisionStart",
            otherLootFixture,
            player.getComponent(HitboxComponent.class).getFixture());

    assertEquals(0, inventory.getOccupiedSlots());
    assertNotCollected(loot);
  }

  @Test
  void shouldIgnoreCollisionWhenLootHasNoHitbox() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Entity player = createPlayer(inventory);
    Entity loot =
        new Entity()
            .addComponent(new LootPickupComponent(new Item("Potion", ItemType.CONSUMABLE, 1, 9)));
    entities.register(loot);

    loot.getEvents()
        .trigger(
            "collisionStart",
            mock(Fixture.class),
            player.getComponent(HitboxComponent.class).getFixture());

    assertEquals(0, inventory.getOccupiedSlots());
    assertNotCollected(loot);
  }

  @Test
  void shouldIgnoreAnEntityOutsideThePlayerLayer() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Entity npc =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(inventory);
    entities.register(npc);
    Entity loot =
        createLoot(new LootPickupComponent(new Item("Potion", ItemType.CONSUMABLE, 1, 9)));

    triggerPlayerCollision(loot, npc);

    assertEquals(0, inventory.getOccupiedSlots());
    assertNotCollected(loot);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldIgnorePlayerFixturesWithoutAnEntity(boolean missingUserData) {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Entity player = createPlayer(inventory);
    player
        .getComponent(PhysicsComponent.class)
        .getBody()
        .setUserData(missingUserData ? null : new BodyUserData());
    Entity loot =
        createLoot(new LootPickupComponent(new Item("Potion", ItemType.CONSUMABLE, 1, 9)));

    triggerPlayerCollision(loot, player);

    assertEquals(0, inventory.getOccupiedSlots());
    assertNotCollected(loot);
  }

  @Test
  void shouldIgnorePlayerWithoutInventory() {
    Entity player = createPlayer(null);
    Item item = new Item("Potion", ItemType.CONSUMABLE, 2, 9);
    Entity loot = createLoot(new LootPickupComponent(item));

    triggerPlayerCollision(loot, player);

    assertEquals(2, item.getQuantity());
    assertNotCollected(loot);
  }

  @Test
  void shouldIgnoreLootWithoutAnItem() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Entity player = createPlayer(inventory);
    Entity loot = createLoot(new LootPickupComponent(null));

    triggerPlayerCollision(loot, player);

    assertEquals(0, inventory.getOccupiedSlots());
    assertNotCollected(loot);
  }

  @Test
  void shouldAddCurrencyOnceEvenWhenInventoryIsFull() {
    InventoryComponent inventory = new InventoryComponent(10, 1);
    Item potion = new Item("Potion", ItemType.CONSUMABLE, 9, 9);
    inventory.addItem(potion);
    Entity player = createPlayer(inventory);
    Entity loot = createLoot(new LootPickupComponent(new Item("Gold", ItemType.CURRENCY, 7, 99)));

    triggerPlayerCollision(loot, player);
    triggerPlayerCollision(loot, player);

    assertEquals(17, inventory.getGold());
    assertSame(potion, inventory.getItem(1));
    assertEquals(9, potion.getQuantity());
    assertTrue(loot.getComponent(LootPickupComponent.class).isCollected());
    assertEquals(1, scheduled.size());
  }

  @Test
  void shouldIgnoreAnEmptyCurrencyPickup() {
    InventoryComponent inventory = new InventoryComponent(10, 1);
    Entity player = createPlayer(inventory);
    Entity loot = createLoot(new LootPickupComponent(new Item("Gold", ItemType.CURRENCY, 0, 99)));

    triggerPlayerCollision(loot, player);

    assertEquals(10, inventory.getGold());
    assertNotCollected(loot);
  }

  @Test
  void shouldGrantBallisticShieldEvenWhenInventoryIsFull() {
    InventoryComponent inventory = new InventoryComponent(0, 1);
    Item potion = new Item("Potion", ItemType.CONSUMABLE, 9, 9);
    inventory.addItem(potion);
    BallisticShieldComponent shield = new BallisticShieldComponent();
    Entity player = createPlayer(inventory, shield);
    Entity loot =
        createLoot(
            new LootPickupComponent(new Item("Ballistic Shield", ItemType.BALLISTIC_SHIELD, 1, 1)));

    triggerPlayerCollision(loot, player);

    assertTrue(shield.hasShield());
    assertFalse(shield.isActive());
    assertSame(potion, inventory.getItem(1));
    assertEquals(1, inventory.getOccupiedSlots());
    assertTrue(loot.getComponent(LootPickupComponent.class).isCollected());
    assertEquals(1, scheduled.size());
  }

  @Test
  void shouldNotCollectBallisticShieldWithoutItsComponent() {
    InventoryComponent inventory = new InventoryComponent(0, 1);
    Entity player = createPlayer(inventory);
    Entity loot =
        createLoot(
            new LootPickupComponent(new Item("Ballistic Shield", ItemType.BALLISTIC_SHIELD, 1, 1)));

    triggerPlayerCollision(loot, player);

    assertEquals(0, inventory.getOccupiedSlots());
    assertNotCollected(loot);
  }

  @Test
  void shouldKeepLootWhenInventoryIsFullAndAllowRetryAfterSpaceIsFreed() {
    InventoryComponent inventory = new InventoryComponent(0, 1);
    Item bread = new Item("Bread", ItemType.CONSUMABLE, 2, 9);
    inventory.addItem(bread);
    Entity player = createPlayer(inventory);
    Item potion = new Item("Potion", ItemType.CONSUMABLE, 2, 9);
    Entity loot = createLoot(new LootPickupComponent(potion));

    triggerPlayerCollision(loot, player);

    assertSame(bread, inventory.getItem(1));
    assertEquals(2, potion.getQuantity());
    assertNotCollected(loot);

    inventory.removeItem(1);
    triggerPlayerCollision(loot, player);

    assertSame(potion, inventory.getItem(1));
    assertEquals(2, inventory.getTotalQuantity(potion));
    assertTrue(loot.getComponent(LootPickupComponent.class).isCollected());
    assertEquals(1, scheduled.size());
  }

  private void assertNotCollected(Entity loot) {
    assertFalse(loot.getComponent(LootPickupComponent.class).isCollected());
    assertFalse(loot.isDisposed());
    assertTrue(scheduled.isEmpty());
  }

  private Entity createPlayer(InventoryComponent inventory, Component... extraComponents) {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    if (inventory != null) {
      player.addComponent(inventory);
    }
    for (Component component : extraComponents) {
      player.addComponent(component);
    }
    entities.register(player);
    return player;
  }

  private Entity createPlayerWithShield(InventoryComponent inventory) {
    return createPlayer(inventory, new ShieldComponent());
  }

  private Entity createLoot(LootPickupComponent pickupComponent) {
    Entity loot =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(pickupComponent);
    entities.register(loot);
    return loot;
  }

  private void triggerPlayerCollision(Entity loot, Entity player) {
    Fixture lootFixture = loot.getComponent(HitboxComponent.class).getFixture();
    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    loot.getEvents().trigger("collisionStart", lootFixture, playerFixture);
  }
}
