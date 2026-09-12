package com.csse3200.game.pausemenu;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.files.SavedItem;
import com.csse3200.game.files.SaveService;
import java.util.Map;
import java.util.function.Supplier;

public class PauseMenuActions extends Component {

  private PauseMenuComponent pauseMenu;
  private final Supplier<Entity> playerSupplier;

  public PauseMenuActions(Supplier<Entity> playerSupplier) {
    this.playerSupplier = playerSupplier;
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
    entity.getEvents().trigger("exit");
  }

  private void save() {
    Entity player = playerSupplier.get();
    if (player == null) {
      return;
    }

    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);

    GameSaveData data = new GameSaveData();
    data.health = stats.getHealth();
    data.gold = inventory.getGold();

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
      }
      data.items.add(saved);
    }

    SaveService.save(data);
  }
}