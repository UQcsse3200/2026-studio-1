package com.csse3200.game.pausemenu;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.ConsumableItem;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.LootRegistry;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.files.SaveService;
import com.csse3200.game.files.SavedItem;
import java.util.Map;
import java.util.function.Supplier;

public class PauseMenuActions extends Component {

  private PauseMenuComponent pauseMenu;
  private final Supplier<Entity> playerSupplier;
  private final Supplier<Map<String, Long>> lootSeedsSupplier;
  private final Supplier<String> levelSupplier;

  public PauseMenuActions(
      Supplier<Entity> playerSupplier,
      Supplier<Map<String, Long>> lootSeedsSupplier,
      Supplier<String> levelSupplier) {
    this.playerSupplier = playerSupplier;
    this.lootSeedsSupplier = lootSeedsSupplier;
    this.levelSupplier = levelSupplier;
  }

  @Override
  public void create() {
    super.create();
    pauseMenu = entity.getComponent(PauseMenuComponent.class);
    registerEventListeners();
  }

  private void registerEventListeners() {
    entity.getEvents().addListener("resumeClicked", this::resume);
    entity.getEvents().addListener("restartClicked", this::restart);
    entity.getEvents().addListener("mainMenuClicked", this::goToMainMenu);
    entity.getEvents().addListener("saveClicked", this::save);
  }

  private void resume() {
    pauseMenu.toggleIsPaused();
  }

  private void restart() {
    pauseMenu.toggleIsPaused();
    entity.getEvents().trigger("restartGame");
  }

  private void goToMainMenu() {
    saveCheckpoint();
    entity.getEvents().trigger("exit");
  }

  public void saveCheckpoint() {
    Entity player = playerSupplier.get();
    if (player == null) {
      return;
    }

    GameSaveData data = createSaveData(player);
    SaveService.save(data);
  }

  private void save() {
    Entity player = playerSupplier.get();
    if (player == null) {
      return;
    }

    GameSaveData data = createSaveData(player);
    SaveService.save(data);
    entity.getEvents().trigger("mainMenuClicked");
  }

  public GameSaveData createSaveData(Entity player) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);

    GameSaveData data = new GameSaveData();
    data.health = stats.getHealth();
    data.gold = inventory.getGold();
    data.posX = player.getPosition().x;
    data.posY = player.getPosition().y;
    data.lootSeedsByRoom = lootSeedsSupplier.get();
    data.collectedLootIds = LootRegistry.exportAll();
    data.level = levelSupplier.get();

    for (Map.Entry<Integer, Item> entry : inventory.getInventorySlots().entrySet()) {
      Item item = entry.getValue();

      SavedItem saved = new SavedItem();
      saved.slot = entry.getKey();
      saved.name = item.getName();
      saved.itemType = item.getItemType().name();
      saved.quantity = item.getQuantity();
      saved.maxQuantity = item.getMaxQuantity();

      if (item instanceof WeaponItem weapon) {
        saved.weaponType = weapon.getWeaponType().name();
        saved.damage = weapon.getDamage();
      } else if (item instanceof ConsumableItem consumable) {
        saved.consumableType = consumable.getConsumableType().name();
      }

      data.items.add(saved);
    }

    return data;
  }
}
