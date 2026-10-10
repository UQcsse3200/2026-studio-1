package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.loot.ConsumableItem;
import com.csse3200.game.components.loot.ConsumableType;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.LootPickupComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.player.BuffStat;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.PlayerBuffComponent;
import com.csse3200.game.components.player.PlayerRegenComponent;
import com.csse3200.game.components.player.StaminaComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.upgrades.UpgradeNode;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class LoadServiceTest {
  private static final float MAP_WIDTH = 50f;
  private static final float MAP_HEIGHT = 30f;
  private static final List<UpgradeNode> NO_UPGRADES = List.of();

  private Entity player;
  private InventoryComponent inventory;
  private CombatStatsComponent stats;
  private PlayerBuffComponent buffs;
  private PlayerRegenComponent regen;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(time);

    inventory = new InventoryComponent(0);
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new StaminaComponent())
            .addComponent(inventory)
            .addComponent(new PlayerBuffComponent())
            .addComponent(new PlayerRegenComponent());
    player.create();

    stats = player.getComponent(CombatStatsComponent.class);
    buffs = player.getComponent(PlayerBuffComponent.class);
    regen = player.getComponent(PlayerRegenComponent.class);
  }

  private void apply(GameSaveData data) {
    LoadService.apply(player, data, MAP_WIDTH, MAP_HEIGHT, NO_UPGRADES);
  }

  private static GameSaveData validSave() {
    GameSaveData data = new GameSaveData();
    data.health = 40;
    data.stamina = 25f;
    data.gold = 75;
    data.posX = 12f;
    data.posY = 8f;
    return data;
  }

  private static SavedItem savedItem(int slot, String name, int quantity) {
    SavedItem saved = new SavedItem();
    saved.slot = slot;
    saved.name = name;
    saved.itemType = ItemType.CURRENCY.name();
    saved.quantity = quantity;
    saved.maxQuantity = 10;
    return saved;
  }

  private static SavedItem savedSword(int slot, int tier) {
    SavedItem saved = savedItem(slot, "Sword", 1);
    saved.itemType = ItemType.WEAPON.name();
    saved.maxQuantity = 1;
    saved.weaponType = WeaponType.SWORD.name();
    saved.damage = 10;
    saved.weaponTier = tier;
    return saved;
  }

  private static UpgradeNode timeUpgrade(String id) {
    return UpgradeNode.timeBased(
        id, "Test", "For testing.", new int[] {10, 10, 10}, new float[] {10f, 10f, 10f});
  }

  // ---------------------------------------------------------------- player stats and position

  @Test
  void shouldRestoreHealthStaminaAndGold() {
    apply(validSave());

    assertEquals(40, stats.getHealth());
    assertEquals(25f, player.getComponent(StaminaComponent.class).getStamina());
    assertEquals(75, inventory.getGold());
  }

  @Test
  void shouldRestorePositionInsideMap() {
    apply(validSave());

    assertEquals(12f, player.getPosition().x);
    assertEquals(8f, player.getPosition().y);
  }

  @Test
  void shouldKeepSpawnPositionWhenSavedPositionIsOutsideMap() {
    player.setPosition(3f, 4f);
    GameSaveData data = validSave();
    data.posX = MAP_WIDTH + 100f;

    apply(data);

    assertEquals(3f, player.getPosition().x);
    assertEquals(4f, player.getPosition().y);
  }

  @Test
  void shouldDoNothingWhenPlayerOrDataIsNull() {
    LoadService.apply(null, validSave(), MAP_WIDTH, MAP_HEIGHT, NO_UPGRADES);
    LoadService.apply(player, null, MAP_WIDTH, MAP_HEIGHT, NO_UPGRADES);

    assertEquals(100, stats.getHealth());
  }

  // ---------------------------------------------------------------- inventory

  @Test
  void shouldReplaceStartingItemsWithSavedItems() {
    inventory.addItem(new Item("Starter", ItemType.CURRENCY, 1, 10));
    GameSaveData data = validSave();
    data.items.add(savedItem(1, "Gem", 3));

    apply(data);

    assertEquals(1, inventory.getOccupiedSlots());
    assertEquals("Gem", inventory.getItem(1).getName());
    assertEquals(3, inventory.getItem(1).getQuantity());
  }

  @Test
  void shouldRestoreItemsIntoTheirSavedSlots() {
    GameSaveData data = validSave();
    data.items.add(savedItem(1, "Gem", 3));
    data.items.add(savedItem(3, "Coin", 5));

    apply(data);

    assertEquals("Gem", inventory.getItem(1).getName());
    assertNull(inventory.getItem(2));
    assertEquals("Coin", inventory.getItem(3).getName());
  }

  @Test
  void shouldNotMergeSeparateStacksOfTheSameItem() {
    GameSaveData data = validSave();
    data.items.add(savedItem(1, "Gem", 3));
    data.items.add(savedItem(2, "Gem", 4));

    apply(data);

    assertEquals(3, inventory.getItem(1).getQuantity());
    assertEquals(4, inventory.getItem(2).getQuantity());
  }

  @Test
  void shouldRestoreActiveSlot() {
    GameSaveData data = validSave();
    data.activeSlot = 3;

    apply(data);

    assertEquals(3, inventory.getActiveSlot());
  }

  @Test
  void shouldIgnoreInvalidActiveSlot() {
    GameSaveData data = validSave();
    data.activeSlot = 99;

    apply(data);

    assertEquals(1, inventory.getActiveSlot());
  }

  @Test
  void shouldRestoreWeaponWithItsTier() {
    GameSaveData data = validSave();
    data.items.add(savedSword(2, 2));

    apply(data);

    WeaponItem weapon = assertInstanceOf(WeaponItem.class, inventory.getItem(2));
    assertEquals(WeaponType.SWORD, weapon.getWeaponType());
    assertEquals(2, weapon.getTier());
  }

  @Test
  void shouldFallBackToSavedDamageWhenTierIsUnknown() {
    SavedItem saved = savedSword(1, 999);
    saved.damage = 17;
    GameSaveData data = validSave();
    data.items.add(saved);

    apply(data);

    WeaponItem weapon = assertInstanceOf(WeaponItem.class, inventory.getItem(1));
    assertEquals(17, weapon.getDamage());
  }

  @Test
  void shouldRestoreSellPrice() {
    SavedItem saved = savedSword(1, 2);
    saved.sellPrice = 20;
    GameSaveData data = validSave();
    data.items.add(saved);

    apply(data);

    assertEquals(20, inventory.getItem(1).getSellPrice());
  }

  @Test
  void shouldIgnoreNegativeSellPrice() {
    SavedItem saved = savedItem(1, "Gem", 3);
    saved.sellPrice = -5;
    GameSaveData data = validSave();
    data.items.add(saved);

    apply(data);

    assertEquals(0, inventory.getItem(1).getSellPrice());
  }

  @Test
  void shouldRestoreAWholeStackOfConsumables() {
    SavedItem saved = savedItem(1, "Health Potion", 6);
    saved.itemType = ItemType.CONSUMABLE.name();
    saved.maxQuantity = 9;
    saved.consumableType = ConsumableType.HEALTH_POTION.name();
    GameSaveData data = validSave();
    data.items.add(saved);

    apply(data);

    ConsumableItem potion = assertInstanceOf(ConsumableItem.class, inventory.getItem(1));
    assertEquals(ConsumableType.HEALTH_POTION, potion.getConsumableType());
    assertEquals(6, potion.getQuantity());
  }

  @Test
  void shouldSkipItemsWithUnknownType() {
    SavedItem broken = savedItem(1, "Mystery", 1);
    broken.itemType = "NOT_A_REAL_TYPE";
    GameSaveData data = validSave();
    data.items.add(broken);
    data.items.add(savedItem(2, "Gem", 3));

    apply(data);

    assertFalse(inventory.containsItem(1));
    assertTrue(inventory.containsItem(2));
  }

  // ---------------------------------------------------------------- upgrades

  @Test
  void shouldRestoreUpgradeTierAndRemainingTime() {
    UpgradeNode speed = timeUpgrade("player_speed");
    AtomicInteger effectApplied = new AtomicInteger();
    speed.setOnTierChanged(effectApplied::incrementAndGet);

    SavedUpgrade saved = new SavedUpgrade();
    saved.id = "player_speed";
    saved.tier = 2;
    saved.remainingSeconds = 14f;
    GameSaveData data = validSave();
    data.upgrades.add(saved);

    LoadService.apply(player, data, MAP_WIDTH, MAP_HEIGHT, List.of(speed));

    assertEquals(2, speed.getCurrentTier());
    assertEquals(14f, speed.getRemainingSeconds());
    assertEquals(1, effectApplied.get(), "the upgrade's effect should be re-applied once");
  }

  @Test
  void shouldLeaveOtherUpgradesInactive() {
    UpgradeNode speed = timeUpgrade("player_speed");
    UpgradeNode endurance = timeUpgrade("endurance");

    SavedUpgrade saved = new SavedUpgrade();
    saved.id = "player_speed";
    saved.tier = 1;
    saved.remainingSeconds = 5f;
    GameSaveData data = validSave();
    data.upgrades.add(saved);

    LoadService.apply(player, data, MAP_WIDTH, MAP_HEIGHT, List.of(speed, endurance));

    assertTrue(speed.isActive());
    assertFalse(endurance.isActive());
  }

  @Test
  void shouldIgnoreSavedUpgradeThatNoLongerExists() {
    UpgradeNode speed = timeUpgrade("player_speed");

    SavedUpgrade saved = new SavedUpgrade();
    saved.id = "removed_upgrade";
    saved.tier = 1;
    saved.remainingSeconds = 5f;
    GameSaveData data = validSave();
    data.upgrades.add(saved);

    LoadService.apply(player, data, MAP_WIDTH, MAP_HEIGHT, List.of(speed));

    assertFalse(speed.isActive());
  }

  @Test
  void shouldRestoreRemainingShieldHits() {
    GameSaveData data = validSave();
    data.shieldHits = 2;

    apply(data);

    assertEquals(2, stats.getShieldHits());
  }

  @Test
  void shouldLeaveShieldHitsAloneForOlderSaves() {
    stats.setShieldHits(4);
    GameSaveData data = validSave();
    data.shieldHits = null;

    apply(data);

    assertEquals(4, stats.getShieldHits());
  }

  // ---------------------------------------------------------------- potion buffs and regen

  @Test
  void shouldRestoreBuffWithItsRemainingTime() {
    SavedBuff saved = new SavedBuff();
    saved.stat = BuffStat.DAMAGE.name();
    saved.magnitude = 2f;
    saved.remainingSeconds = 18f;
    GameSaveData data = validSave();
    data.buffs.add(saved);

    apply(data);

    assertTrue(buffs.hasBuff(BuffStat.DAMAGE));
    assertEquals(20, stats.getBaseAttack(), "a 2x damage buff should double base attack of 10");
    assertEquals(18f, buffs.getActiveBuffs().get(0).getRemainingSeconds(0L));
  }

  @Test
  void shouldSkipBuffWithUnknownStat() {
    SavedBuff saved = new SavedBuff();
    saved.stat = "NOT_A_REAL_STAT";
    saved.magnitude = 2f;
    saved.remainingSeconds = 18f;
    GameSaveData data = validSave();
    data.buffs.add(saved);

    apply(data);

    assertTrue(buffs.getActiveBuffs().isEmpty());
  }

  @Test
  void shouldRestoreRegeneration() {
    GameSaveData data = validSave();
    data.regenHealPerTick = 5;
    data.regenRemainingSeconds = 4f;

    apply(data);

    assertTrue(regen.isRegenerating());
    assertEquals(5, regen.getHealPerTick());
    assertEquals(4f, regen.getRemainingSeconds());
  }

  @Test
  void shouldNotStartRegenerationWhenNoneWasSaved() {
    apply(validSave());

    assertFalse(regen.isRegenerating());
    assertEquals(0f, regen.getRemainingSeconds());
  }

  // ---------------------------------------------------------------- dropped loot

  /** Registers the services LootFactory needs, with a mock entity service to capture spawns. */
  private static EntityService registerLootServices() {
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(16);
    when(texture.getHeight()).thenReturn(16);

    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);

    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());

    EntityService entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);
    return entityService;
  }

  private static SavedLoot savedLoot(float x, float y) {
    SavedLoot loot = new SavedLoot();
    loot.item = savedSword(0, 1);
    loot.x = x;
    loot.y = y;
    return loot;
  }

  @Test
  void shouldRespawnDroppedLootAtItsSavedPosition() {
    EntityService entityService = registerLootServices();
    GameSaveData data = validSave();
    data.droppedLoot.add(savedLoot(14f, 6f));

    apply(data);

    ArgumentCaptor<Entity> spawned = ArgumentCaptor.forClass(Entity.class);
    verify(entityService).register(spawned.capture());

    Entity loot = spawned.getValue();
    assertEquals(14f, loot.getPosition().x);
    assertEquals(6f, loot.getPosition().y);
    assertEquals("Sword", loot.getComponent(LootPickupComponent.class).getItem().getName());
  }

  @Test
  void shouldSkipDroppedLootOutsideTheMap() {
    EntityService entityService = registerLootServices();
    GameSaveData data = validSave();
    data.droppedLoot.add(savedLoot(MAP_WIDTH + 10f, 6f));
    data.droppedLoot.add(savedLoot(14f, -5f));

    apply(data);

    verify(entityService, never()).register(any());
  }

  @Test
  void shouldIgnoreDroppedLootWhenNoEntityServiceIsRunning() {
    GameSaveData data = validSave();
    data.droppedLoot.add(savedLoot(14f, 6f));

    apply(data);

    assertEquals(40, stats.getHealth(), "the rest of the save should still load");
  }

  @Test
  void shouldRefillPetSlotsInOrder() {
    GameSaveData data = validSave();
    data.ownedPetNames = List.of("Dog", "Cat");

    LoadService.apply(player, data, MAP_WIDTH, MAP_HEIGHT, NO_UPGRADES);

    assertEquals("Dog", inventory.getPet(1).getName());
    assertEquals("Cat", inventory.getPet(2).getName());
  }

  @Test
  void shouldSkipBlankDuplicateAndExtraPets() {
    GameSaveData data = validSave();
    data.ownedPetNames = Arrays.asList("Dog", null, "", "Dog", "Cat", "Owl");

    LoadService.apply(player, data, MAP_WIDTH, MAP_HEIGHT, NO_UPGRADES);

    assertEquals(2, inventory.getPetSlots().size());
    assertEquals("Dog", inventory.getPet(1).getName());
    assertEquals("Cat", inventory.getPet(2).getName());
  }
}
