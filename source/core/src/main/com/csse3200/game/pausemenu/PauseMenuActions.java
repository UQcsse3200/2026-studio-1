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
import com.csse3200.game.entities.spawn.EnemyRegistry;
import com.csse3200.game.entities.spawn.NpcQuestRegistry;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.files.SaveService;
import com.csse3200.game.files.SavedBuff;
import com.csse3200.game.files.SavedItem;
import com.csse3200.game.files.SavedLoot;
import com.csse3200.game.files.SavedPerk;
import com.csse3200.game.files.SavedUpgrade;
import com.csse3200.game.perks.Perk;
import com.csse3200.game.perks.PerkService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.upgrades.UpgradeNode;
import com.csse3200.game.win.QuestLedger;
import com.csse3200.game.win.TortoiseLedger;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
    StaminaComponent stamina = player.getComponent(StaminaComponent.class);

    GameSaveData data = new GameSaveData();
    data.health = stats.getHealth();
    data.stamina = stamina.getStamina();
    data.gold = inventory.getGold();
    data.activeSlot = inventory.getActiveSlot();
    data.posX = player.getPosition().x;
    data.posY = player.getPosition().y;
    data.lootSeedsByRoom = lootSeedsSupplier.get();
    data.collectedLootIds = LootRegistry.exportAll();
    data.killedEnemyIds = EnemyRegistry.exportAll();
    data.completedQuestsByKind = QuestLedger.exportAll();
    data.foundTortoiseIds = TortoiseLedger.exportAll();
    data.perks = new java.util.ArrayList<>();
    for (Perk perk : PerkService.getAllPerks()) {
      SavedPerk savedPerk = new SavedPerk();
      savedPerk.id = perk.getId();
      savedPerk.progress = perk.getProgress();
      savedPerk.unlocked = perk.isUnlocked();
      savedPerk.active = perk.isActive();
      data.perks.add(savedPerk);
    }

    data.npcs = NpcQuestRegistry.exportAll();
    data.killedNpcIds = NpcQuestRegistry.exportKilled();
    int[] questCounters = Quest.exportCounters();
    data.questJumps = questCounters[0];
    data.questEnemiesKilled = questCounters[1];
    data.questGoldSpent = questCounters[2];
    data.questShieldsCollected = questCounters[3];
    data.level = levelSupplier.get();

    data.ownedPetNames =
        inventory.getPetSlots().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getValue().getName())
            .collect(java.util.stream.Collectors.toList());

    PetManagerComponent petManager = player.getComponent(PetManagerComponent.class);
    if (petManager != null) {
      data.activePetName = petManager.getActivePetName();
    }

    data.difficulty = DifficultyService.getCurrent().name();

    data.shieldHits = stats.getShieldHits();

    ShieldComponent shield = player.getComponent(ShieldComponent.class);
    if (shield != null) {
      data.shieldHeld = shield.hasShield();
      data.shieldRemainingMillis = shield.getRemainingMillis();
    }

    ShopComponent shopForStock = player.getComponent(ShopComponent.class);
    if (shopForStock != null) {
      data.shopItemSlots = new java.util.ArrayList<>(shopForStock.getItemCatalog().keySet());
    }

    BallisticShieldComponent ballisticShield = player.getComponent(BallisticShieldComponent.class);
    if (ballisticShield != null) {
      data.ballisticShieldHeld = ballisticShield.hasShield();
      data.ballisticShieldRemainingMillis = ballisticShield.getRemainingMillis();
    }

    GameTime timeSource = ServiceLocator.getTimeSource();
    PlayerBuffComponent buffs = player.getComponent(PlayerBuffComponent.class);
    if (buffs != null && timeSource != null) {
      long now = timeSource.getTime();
      for (ActiveBuff buff : buffs.getActiveBuffs()) {
        float remaining = buff.getRemainingSeconds(now);
        if (remaining <= 0f) {
          continue;
        }

        SavedBuff saved = new SavedBuff();
        saved.stat = buff.getStat().name();
        saved.magnitude = buff.getMagnitude();
        saved.remainingSeconds = remaining;
        data.buffs.add(saved);
      }
    }

    PlayerRegenComponent regen = player.getComponent(PlayerRegenComponent.class);
    if (regen != null) {
      data.regenHealPerTick = regen.getHealPerTick();
      data.regenRemainingSeconds = regen.getRemainingSeconds();
    }

    for (UpgradeNode node : upgradesSupplier.get()) {
      if (!node.isActive()) {
        continue;
      }

      SavedUpgrade saved = new SavedUpgrade();
      saved.id = node.getId();
      saved.tier = node.getCurrentTier();
      saved.remainingSeconds = node.getRemainingSeconds();
      saved.remainingKills = node.getRemainingKills();
      data.upgrades.add(saved);
    }

    for (Map.Entry<Integer, Item> entry : inventory.getInventorySlots().entrySet()) {
      data.items.add(toSavedItem(entry.getValue(), entry.getKey()));
    }

    EntityService entityService = ServiceLocator.getEntityService();
    if (entityService != null) {
      for (Entity loot : entityService.getEntitiesWithComponent(LootPickupComponent.class)) {
        LootPickupComponent pickup = loot.getComponent(LootPickupComponent.class);

        // Loot placed by the level is already tracked by LootRegistry. Only loot dropped during
        // play is saved here.
        if (loot.getComponent(PersistentLootIdComponent.class) != null
            || pickup.isCollected()
            || pickup.getItem() == null) {
          continue;
        }

        SavedLoot saved = new SavedLoot();
        saved.item = toSavedItem(pickup.getItem(), 0);
        saved.x = loot.getPosition().x;
        saved.y = loot.getPosition().y;
        data.droppedLoot.add(saved);
      }
    }

    return data;
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
