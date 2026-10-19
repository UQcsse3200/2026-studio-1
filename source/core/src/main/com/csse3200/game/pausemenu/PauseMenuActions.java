package com.csse3200.game.pausemenu;

import com.csse3200.game.Quests.Quest;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.ConsumableItem;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.LootPickupComponent;
import com.csse3200.game.components.loot.LootRegistry;
import com.csse3200.game.components.loot.PersistentLootIdComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.pet.PetManagerComponent;
import com.csse3200.game.components.player.ActiveBuff;
import com.csse3200.game.components.player.BallisticShieldComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.PlayerBuffComponent;
import com.csse3200.game.components.player.PlayerRegenComponent;
import com.csse3200.game.components.player.ShieldComponent;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.components.player.StaminaComponent;
import com.csse3200.game.difficulty.DifficultyService;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.spawn.NpcQuestRegistry;
import com.csse3200.game.entities.spawn.enemies.EnemyRegistry;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.files.SaveService;
import com.csse3200.game.files.SavedBuff;
import com.csse3200.game.files.SavedItem;
import com.csse3200.game.files.SavedLoot;
import com.csse3200.game.files.SavedPerk;
import com.csse3200.game.files.SavedUpgrade;
import com.csse3200.game.perks.Perk;
import com.csse3200.game.perks.PerkService;
import com.csse3200.game.perks.tortoises.TortoisePlan;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.upgrades.UpgradeNode;
import com.csse3200.game.win.QuestLedger;
import com.csse3200.game.win.TortoiseLedger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Handles the pause menu's buttons (resume, restart, main menu, save) and builds the save data.
 *
 * <p>{@link #createSaveData(Entity)} reads the current game state into a {@link GameSaveData}. Each
 * part of the game is saved by its own small helper, in the same way {@code LoadService} restores
 * each part with its own {@code loadXxx} method.
 */
public class PauseMenuActions extends Component {

  private PauseMenuComponent pauseMenu;
  private final Supplier<Entity> playerSupplier;
  private final Supplier<Map<String, Long>> lootSeedsSupplier;
  private final Supplier<String> levelSupplier;
  private final Supplier<List<UpgradeNode>> upgradesSupplier;

  public PauseMenuActions(
      Supplier<Entity> playerSupplier,
      Supplier<Map<String, Long>> lootSeedsSupplier,
      Supplier<String> levelSupplier) {
    this(playerSupplier, lootSeedsSupplier, levelSupplier, List::of);
  }

  public PauseMenuActions(
      Supplier<Entity> playerSupplier,
      Supplier<Map<String, Long>> lootSeedsSupplier,
      Supplier<String> levelSupplier,
      Supplier<List<UpgradeNode>> upgradesSupplier) {
    this.playerSupplier = playerSupplier;
    this.lootSeedsSupplier = lootSeedsSupplier;
    this.levelSupplier = levelSupplier;
    this.upgradesSupplier = upgradesSupplier;
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
    if (pauseMenu.isPaused()) {
      pauseMenu.toggleIsPaused();
    }
    entity.getEvents().trigger("exit");
  }

  /** Saves without leaving the game. Called each time the player enters a new level. */
  public void saveCheckpoint() {
    writeSave();
  }

  /** Saves from the pause menu, then returns to the Main Menu. */
  private void save() {
    if (writeSave()) {
      entity.getEvents().trigger("mainMenuClicked");
    }
  }

  /**
   * Builds the save data for the current player and writes it to disk.
   *
   * @return true if there was a player to save
   */
  private boolean writeSave() {
    Entity player = playerSupplier.get();
    if (player == null) {
      return false;
    }
    SaveService.save(createSaveData(player));
    return true;
  }

  /**
   * Reads the current game state into a new {@link GameSaveData}.
   *
   * @param player the player entity to save
   * @return the data to write to the save file
   */
  public GameSaveData createSaveData(Entity player) {
    GameSaveData data = new GameSaveData();
    savePlayer(data, player);
    saveWorld(data);
    saveQuests(data);
    savePerksAndTortoises(data);
    savePets(data, player);
    saveShields(data, player);
    saveShopStock(data, player);
    saveBuffs(data, player);
    saveUpgrades(data);
    saveInventory(data, player);
    saveDroppedLoot(data);
    return data;
  }

  /** Health, stamina, gold, hotbar slot, position, shield hits and the current level. */
  private void savePlayer(GameSaveData data, Entity player) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    StaminaComponent stamina = player.getComponent(StaminaComponent.class);

    data.health = stats.getHealth();
    data.shieldHits = stats.getShieldHits();
    data.stamina = stamina.getStamina();
    data.gold = inventory.getGold();
    data.activeSlot = inventory.getActiveSlot();
    data.posX = player.getPosition().x;
    data.posY = player.getPosition().y;
    data.level = levelSupplier.get();
  }

  /** Loot seeds, collected loot, killed enemies and the difficulty. */
  private void saveWorld(GameSaveData data) {
    data.lootSeedsByRoom = lootSeedsSupplier.get();
    data.collectedLootIds = LootRegistry.exportAll();
    data.killedEnemyIds = EnemyRegistry.exportAll();
    data.difficulty = DifficultyService.getCurrent().name();
  }

  /** Completed quests, NPC quest state, killed NPCs and the quest counters. */
  private static void saveQuests(GameSaveData data) {
    data.completedQuestsByKind = QuestLedger.exportAll();
    data.npcs = NpcQuestRegistry.exportAll();
    data.killedNpcIds = NpcQuestRegistry.exportKilled();

    int[] questCounters = Quest.exportCounters();
    data.questJumps = questCounters[0];
    data.questEnemiesKilled = questCounters[1];
    data.questGoldSpent = questCounters[2];
    data.questShieldsCollected = questCounters[3];
  }

  /** Every perk's state, and the tortoises found, total and plan. */
  private static void savePerksAndTortoises(GameSaveData data) {
    data.perks = new ArrayList<>();
    for (Perk perk : PerkService.getAllPerks()) {
      SavedPerk savedPerk = new SavedPerk();
      savedPerk.id = perk.getId();
      savedPerk.progress = perk.getProgress();
      savedPerk.unlocked = perk.isUnlocked();
      savedPerk.active = perk.isActive();
      data.perks.add(savedPerk);
    }

    data.foundTortoiseIds = TortoiseLedger.exportAll();
    data.tortoiseTotal = TortoiseLedger.getTotal();
    data.tortoisePlan = TortoisePlan.exportAll();
  }

  /** Owned pets in slot order, and the active pet. */
  private static void savePets(GameSaveData data, Entity player) {
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    data.ownedPetNames =
        inventory.getPetSlots().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getValue().getName())
            // An ArrayList, so the JSON writer stores it as a plain list.
            .collect(Collectors.toCollection(ArrayList::new));

    PetManagerComponent petManager = player.getComponent(PetManagerComponent.class);
    if (petManager != null) {
      data.activePetName = petManager.getActivePetName();
    }
  }

  /** Held and active shields of both types, with the time left on an active one. */
  private static void saveShields(GameSaveData data, Entity player) {
    ShieldComponent shield = player.getComponent(ShieldComponent.class);
    if (shield != null) {
      data.shieldHeld = shield.hasShield();
      data.shieldRemainingMillis = shield.getRemainingMillis();
    }

    BallisticShieldComponent ballisticShield = player.getComponent(BallisticShieldComponent.class);
    if (ballisticShield != null) {
      data.ballisticShieldHeld = ballisticShield.hasShield();
      data.ballisticShieldRemainingMillis = ballisticShield.getRemainingMillis();
    }
  }

  /** Which shop item listings are still in stock. Left null when the player has no shop. */
  private static void saveShopStock(GameSaveData data, Entity player) {
    ShopComponent shop = player.getComponent(ShopComponent.class);
    if (shop != null) {
      data.shopItemSlots = new ArrayList<>(shop.getItemCatalog().keySet());
    }
  }

  /** Active potion buffs with the time they have left, and regeneration in progress. */
  private static void saveBuffs(GameSaveData data, Entity player) {
    GameTime timeSource = ServiceLocator.getTimeSource();
    PlayerBuffComponent buffs = player.getComponent(PlayerBuffComponent.class);
    if (buffs != null && timeSource != null) {
      long now = timeSource.getTime();
      for (ActiveBuff buff : buffs.getActiveBuffs()) {
        float remaining = buff.getRemainingSeconds(now);
        if (remaining > 0f) {
          SavedBuff saved = new SavedBuff();
          saved.stat = buff.getStat().name();
          saved.magnitude = buff.getMagnitude();
          saved.remainingSeconds = remaining;
          data.buffs.add(saved);
        }
      }
    }

    PlayerRegenComponent regen = player.getComponent(PlayerRegenComponent.class);
    if (regen != null) {
      data.regenHealPerTick = regen.getHealPerTick();
      data.regenRemainingSeconds = regen.getRemainingSeconds();
    }
  }

  /** Every active upgrade with its tier and the time or kills it has left. */
  private void saveUpgrades(GameSaveData data) {
    for (UpgradeNode node : upgradesSupplier.get()) {
      if (node.isActive()) {
        SavedUpgrade saved = new SavedUpgrade();
        saved.id = node.getId();
        saved.tier = node.getCurrentTier();
        saved.remainingSeconds = node.getRemainingSeconds();
        saved.remainingKills = node.getRemainingKills();
        data.upgrades.add(saved);
      }
    }
  }

  /** Every inventory item with the slot it is in. */
  private static void saveInventory(GameSaveData data, Entity player) {
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    for (Map.Entry<Integer, Item> entry : inventory.getInventorySlots().entrySet()) {
      data.items.add(toSavedItem(entry.getValue(), entry.getKey()));
    }
  }

  /**
   * Items lying on the ground that were dropped during play. Loot placed by the level is already
   * tracked by LootRegistry, so it is skipped here.
   */
  private static void saveDroppedLoot(GameSaveData data) {
    EntityService entityService = ServiceLocator.getEntityService();
    if (entityService == null) {
      return;
    }

    for (Entity loot : entityService.getEntitiesWithComponent(LootPickupComponent.class)) {
      LootPickupComponent pickup = loot.getComponent(LootPickupComponent.class);
      if (isDroppedLoot(loot, pickup)) {
        SavedLoot saved = new SavedLoot();
        saved.item = toSavedItem(pickup.getItem(), 0);
        saved.x = loot.getPosition().x;
        saved.y = loot.getPosition().y;
        data.droppedLoot.add(saved);
      }
    }
  }

  private static boolean isDroppedLoot(Entity loot, LootPickupComponent pickup) {
    return loot.getComponent(PersistentLootIdComponent.class) == null
        && !pickup.isCollected()
        && pickup.getItem() != null;
  }

  private static SavedItem toSavedItem(Item item, int slot) {
    SavedItem saved = new SavedItem();
    saved.slot = slot;
    saved.name = item.getName();
    saved.itemType = item.getItemType().name();
    saved.quantity = item.getQuantity();
    saved.maxQuantity = item.getMaxQuantity();
    saved.sellPrice = item.getSellPrice();

    if (item instanceof WeaponItem weapon) {
      saved.weaponType = weapon.getWeaponType().name();
      saved.damage = weapon.getDamage();
      saved.weaponTier = weapon.getTier();
    } else if (item instanceof ConsumableItem consumable) {
      saved.consumableType = consumable.getConsumableType().name();
    }

    return saved;
  }
}
