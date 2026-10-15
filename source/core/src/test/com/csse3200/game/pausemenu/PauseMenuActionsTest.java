package com.csse3200.game.pausemenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.LootPickupComponent;
import com.csse3200.game.components.loot.LootRegistry;
import com.csse3200.game.components.loot.PersistentLootIdComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.player.BuffStat;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.PlayerBuffComponent;
import com.csse3200.game.components.player.PlayerRegenComponent;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.components.player.StaminaComponent;
import com.csse3200.game.difficulty.Difficulty;
import com.csse3200.game.difficulty.DifficultyService;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.spawn.EnemyRegistry;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.files.LoadService;
import com.csse3200.game.files.SavedBuff;
import com.csse3200.game.files.SavedItem;
import com.csse3200.game.files.SavedLoot;
import com.csse3200.game.files.SavedUpgrade;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.upgrades.UpgradeNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PauseMenuActionsTest {
  private static final String LEVEL = "maps/level2.json";

  private GameTime time;
  private Entity player;
  private InventoryComponent inventory;
  private CombatStatsComponent stats;
  private Map<String, Long> lootSeeds;
  private List<UpgradeNode> upgrades;
  private PauseMenuActions actions;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(time);

    inventory = new InventoryComponent(60);
    player = createPlayer(inventory);
    player.setPosition(7f, 9f);
    stats = player.getComponent(CombatStatsComponent.class);

    lootSeeds = new HashMap<>();
    lootSeeds.put(LEVEL, 1234L);
    upgrades = new ArrayList<>();

    actions = new PauseMenuActions(() -> player, () -> lootSeeds, () -> LEVEL, () -> upgrades);

    resetGlobalState();
  }

  @AfterEach
  void tearDown() {
    // These are static, so reset them to avoid leaking state into other tests.
    resetGlobalState();
  }

  private static void resetGlobalState() {
    LootRegistry.loadFrom(new ArrayList<>());
    EnemyRegistry.loadFrom(new ArrayList<>());
    DifficultyService.setCurrent(Difficulty.NORMAL);
  }

  private static Entity createPlayer(InventoryComponent inventory) {
    Entity entity =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new StaminaComponent())
            .addComponent(inventory)
            .addComponent(new PlayerBuffComponent())
            .addComponent(new PlayerRegenComponent());
    entity.create();
    return entity;
  }

  private static UpgradeNode timeUpgrade(String id) {
    return UpgradeNode.timeBased(
        id, "Test", "For testing.", new int[] {10, 10, 10}, new float[] {10f, 10f, 10f});
  }

  private static SavedItem findBySlot(List<SavedItem> items, int slot) {
    return items.stream().filter(item -> item.slot == slot).findFirst().orElseThrow();
  }

  // ---------------------------------------------------------------- player and world state

  @Test
  void shouldSavePlayerStats() {
    stats.setHealth(45);
    player.getComponent(StaminaComponent.class).setStamina(30f);

    GameSaveData data = actions.createSaveData(player);

    assertEquals(45, data.health);
    assertEquals(30f, data.stamina);
    assertEquals(60, data.gold);
    assertEquals(7f, data.posX);
    assertEquals(9f, data.posY);
  }

  @Test
  void shouldSaveLevelAndLootSeeds() {
    GameSaveData data = actions.createSaveData(player);

    assertEquals(LEVEL, data.level);
    assertEquals(1234L, data.lootSeedsByRoom.get(LEVEL));
  }

  @Test
  void shouldSaveCollectedLootAndKilledEnemies() {
    LootRegistry.markCollected("loot-1");
    EnemyRegistry.markKilled("enemy-1");

    GameSaveData data = actions.createSaveData(player);

    assertTrue(data.collectedLootIds.contains("loot-1"));
    assertTrue(data.killedEnemyIds.contains("enemy-1"));
  }

  @Test
  void shouldSaveSelectedDifficulty() {
    DifficultyService.setCurrent(Difficulty.HARD);

    GameSaveData data = actions.createSaveData(player);

    assertEquals("HARD", data.difficulty);
  }

  @Test
  void shouldLeavePetFieldsEmptyWhenPlayerHasNoPetComponents() {
    GameSaveData data = actions.createSaveData(player);

    assertTrue(data.ownedPetNames.isEmpty());
    assertNull(data.activePetName);
  }

  @Test
  void shouldSavePetsFromInventorySlotsInOrder() {
    inventory.addPet(new ShopComponent.Pet("Dog"));
    inventory.addPet(new ShopComponent.Pet("Cat"));

    GameSaveData data = actions.createSaveData(player);

    assertEquals(List.of("Dog", "Cat"), data.ownedPetNames);
  }

  // ---------------------------------------------------------------- inventory

  @Test
  void shouldSaveActiveSlot() {
    inventory.setActiveSlot(3);

    GameSaveData data = actions.createSaveData(player);

    assertEquals(3, data.activeSlot);
  }

  @Test
  void shouldSaveItemsWithTheirSlots() {
    inventory.setItem(1, new Item("Gem", ItemType.CURRENCY, 3, 10));
    inventory.setItem(3, new Item("Coin", ItemType.CURRENCY, 5, 10));

    GameSaveData data = actions.createSaveData(player);

    assertEquals(2, data.items.size());
    SavedItem coin = findBySlot(data.items, 3);
    assertEquals("Coin", coin.name);
    assertEquals("CURRENCY", coin.itemType);
    assertEquals(5, coin.quantity);
    assertEquals(10, coin.maxQuantity);
  }

  @Test
  void shouldSaveWeaponDetailsAndSellPrice() {
    WeaponItem weapon = new WeaponItem("Sword", WeaponType.SWORD, 12, 1, 1);
    weapon.setSellPrice(20);
    inventory.setItem(1, weapon);

    GameSaveData data = actions.createSaveData(player);

    SavedItem sword = findBySlot(data.items, 1);
    assertEquals("SWORD", sword.weaponType);
    assertEquals(12, sword.damage);
    assertEquals(1, sword.weaponTier);
    assertEquals(20, sword.sellPrice);
    assertNull(sword.consumableType);
  }

  // ---------------------------------------------------------------- upgrades

  @Test
  void shouldSaveOnlyActiveUpgrades() {
    UpgradeNode speed = timeUpgrade("player_speed");
    speed.purchaseNextTier();
    upgrades.add(speed);
    upgrades.add(timeUpgrade("endurance"));

    GameSaveData data = actions.createSaveData(player);

    assertEquals(1, data.upgrades.size());
    SavedUpgrade saved = data.upgrades.get(0);
    assertEquals("player_speed", saved.id);
    assertEquals(1, saved.tier);
    assertEquals(10f, saved.remainingSeconds);
  }

  @Test
  void shouldSaveRemainingShieldHits() {
    stats.setShieldHits(3);

    GameSaveData data = actions.createSaveData(player);

    assertEquals(3, data.shieldHits);
  }

  @Test
  void shouldSaveNoUpgradesWhenBuiltWithoutAnUpgradeSupplier() {
    PauseMenuActions basic = new PauseMenuActions(() -> player, () -> lootSeeds, () -> LEVEL);

    GameSaveData data = basic.createSaveData(player);

    assertTrue(data.upgrades.isEmpty());
  }

  // ---------------------------------------------------------------- potion buffs and regen

  @Test
  void shouldSaveActiveBuffWithItsRemainingTime() {
    player.getComponent(PlayerBuffComponent.class).applyBuff(BuffStat.DAMAGE, 2f, 30f);
    when(time.getTime()).thenReturn(12000L);

    GameSaveData data = actions.createSaveData(player);

    assertEquals(1, data.buffs.size());
    SavedBuff saved = data.buffs.get(0);
    assertEquals("DAMAGE", saved.stat);
    assertEquals(2f, saved.magnitude);
    assertEquals(18f, saved.remainingSeconds);
  }

  @Test
  void shouldSaveNoBuffsWhenNoneAreActive() {
    GameSaveData data = actions.createSaveData(player);

    assertTrue(data.buffs.isEmpty());
  }

  @Test
  void shouldSaveRegenerationInProgress() {
    player.getComponent(PlayerRegenComponent.class).startRegen(5, 10f);
    when(time.getTime()).thenReturn(4000L);

    GameSaveData data = actions.createSaveData(player);

    assertEquals(5, data.regenHealPerTick);
    assertEquals(6f, data.regenRemainingSeconds);
  }

  // ---------------------------------------------------------------- dropped loot

  private static Entity lootEntity(Item item, float x, float y) {
    Entity loot = new Entity().addComponent(new LootPickupComponent(item));
    loot.setPosition(x, y);
    return loot;
  }

  @Test
  void shouldSaveDroppedLootWithItsPosition() {
    EntityService entityService = new EntityService();
    ServiceLocator.registerEntityService(entityService);
    entityService.register(lootEntity(new Item("Gem", ItemType.CURRENCY, 3, 10), 4f, 2f));

    GameSaveData data = actions.createSaveData(player);

    assertEquals(1, data.droppedLoot.size());
    SavedLoot saved = data.droppedLoot.get(0);
    assertEquals("Gem", saved.item.name);
    assertEquals(3, saved.item.quantity);
    assertEquals(4f, saved.x);
    assertEquals(2f, saved.y);
  }

  @Test
  void shouldNotSaveLevelPlacedLootAsDroppedLoot() {
    EntityService entityService = new EntityService();
    ServiceLocator.registerEntityService(entityService);

    Entity levelLoot = lootEntity(new Item("Gem", ItemType.CURRENCY, 3, 10), 4f, 2f);
    levelLoot.addComponent(new PersistentLootIdComponent("Level 1:4,2"));
    entityService.register(levelLoot);

    GameSaveData data = actions.createSaveData(player);

    assertTrue(data.droppedLoot.isEmpty());
  }

  @Test
  void shouldSaveNoDroppedLootWhenNoEntityServiceIsRunning() {
    GameSaveData data = actions.createSaveData(player);

    assertTrue(data.droppedLoot.isEmpty());
  }

  // ---------------------------------------------------------------- save then load

  @Test
  void savedDataShouldRestoreOntoANewPlayer() {
    stats.setHealth(45);
    inventory.setItem(2, new Item("Gem", ItemType.CURRENCY, 3, 10));
    inventory.setActiveSlot(2);
    player.getComponent(PlayerBuffComponent.class).applyBuff(BuffStat.SPEED, 1.5f, 20f);

    UpgradeNode speed = timeUpgrade("player_speed");
    speed.purchaseNextTier();
    upgrades.add(speed);

    GameSaveData data = actions.createSaveData(player);

    InventoryComponent newInventory = new InventoryComponent(0);
    Entity newPlayer = createPlayer(newInventory);
    UpgradeNode newSpeed = timeUpgrade("player_speed");
    LoadService.apply(newPlayer, data, 50f, 30f, List.of(newSpeed));

    assertEquals(45, newPlayer.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(60, newInventory.getGold());
    assertEquals("Gem", newInventory.getItem(2).getName());
    assertEquals(2, newInventory.getActiveSlot());
    assertEquals(7f, newPlayer.getPosition().x);
    assertEquals(9f, newPlayer.getPosition().y);
    assertTrue(newPlayer.getComponent(PlayerBuffComponent.class).hasBuff(BuffStat.SPEED));
    assertEquals(1, newSpeed.getCurrentTier());
    assertEquals(10f, newSpeed.getRemainingSeconds());
  }
}
