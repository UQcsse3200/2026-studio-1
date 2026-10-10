package com.csse3200.game.Quests;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GoldSpentQuestTest {
  static Entity entity;
  static QuestGiverComponent questGiverComponent;
  static Entity player;
  static Item weapon;

  @BeforeEach
  public void createAQuestEntity() {
    entity = new Entity();
    player = new Entity();
    weapon = (new WeaponGenerator()).generateWeapon(WeaponType.SWORD,1);
    questGiverComponent = new QuestGiverComponent(player, 0);
    entity.addComponent(questGiverComponent);
  }

  @Test
  public void testIfCompletingAGoldSpentQuestWorks() {
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
      fail(
          "The test testIfCompletingAGoldSpentQuestWorks failed because Quest.getQuestActiveForNPCID()"
              + "returned true even though it should have returned false since no quest was created "
              + "for that entity");
    }
    // Creates the quest in the conditional
    if (Quest.getGoldSpentQuests().get(questGiverComponent.uniqueNPCID) != null) {
      fail(
          "The test testIfCompletingAGoldSpentQuestWorks did not return null"
              + "when it checked the goldSpentQuestTracker even though no "
              + "quest was created");
    }
    if (!questGiverComponent.logGoldSpentQuest(10)) {
      fail(
          "The test testIfCompletingAGoldSpentQuestWorks failed because "
              + "logGoldSpentQuest returned false when it shouldn't have");
    }
    if (!Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
      fail(
          "The test testIfCompletingAGoldSpentQuestWorks failed because Quest.getQuestActiveForNPCID() "
              + "returned false when it should have returned true");
    }
    questGiverComponent.clearGoldSpentQuest();
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
      fail(
          "The test testIfCompletingAGoldSpentQuestWorks failed because Quest.getQuestActiveForNPCID()"
              + "returned true even though it should have returned false since the quest was cleared "
              + "for that entity");
    }
  }

  @Test
  public void testMultipleSettingOfGoldSpentQuest() {
    Entity entity1 = new Entity();
    Entity entity2 = new Entity();
    Entity entity3 = new Entity();
    QuestGiverComponent questGiverComponent1 = new QuestGiverComponent(null, 0);
    QuestGiverComponent questGiverComponent2 = new QuestGiverComponent(null, 0);
    QuestGiverComponent questGiverComponent3 = new QuestGiverComponent(null, 0);
    entity1.addComponent(questGiverComponent1);
    entity2.addComponent(questGiverComponent2);
    entity3.addComponent(questGiverComponent3);
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent2.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent3.uniqueNPCID)) {
      fail(
          "The test testMultipleSettingOfGoldSpentQuest failed because "
              + "Quest.getQuestActiveForNPCID returned true even though no "
              + "quest was created for that NPC");
    }
    if (!questGiverComponent1.logGoldSpentQuest(5)
        || !questGiverComponent2.logGoldSpentQuest(6)
        || !questGiverComponent3.logGoldSpentQuest(7)) {
      fail(
          "The test testMultipleSettingOfGoldSpentQuest failed because "
              + "logGoldSpentQuest returned false when it should have returned "
              + "true");
    }
    if (questGiverComponent3.logGoldSpentQuest(6)) {
      fail(
          "The test testMultipleSettingOfGoldSpentQuest failed because "
              + "Quest.logGoldSpentQuest returned true when it shouldn't have "
              + "because a quest was already set");
    }
    assertEquals(
        0,
        questGiverComponent1.checkGoldSpentQuestComplete(),
        "checkGoldSpentQuestComplete() returned a value other than 0 even "
            + "though no progress was made");
    questGiverComponent1.clearGoldSpentQuest();
    questGiverComponent2.clearGoldSpentQuest();
    questGiverComponent3.clearGoldSpentQuest();
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent2.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent3.uniqueNPCID)) {
      fail(
          "Quest.getQuestActiveForNPCID() returned true when it should have returned "
              + "false since the quest was cleared");
    }
    // Logging the EnemiesKilledQuest
    if (!questGiverComponent1.logGoldSpentQuest(9)) {
      fail(
          "questGiverComponent1.logGoldSpentQuest returned false when it should "
              + "have returned true");
    }
    if (!Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)) {
      fail(
          "Quest.getQuestActiveForNPCID() returned false when it should have "
              + "returned true since a quest was logged");
    }
    if (Quest.getGoldSpentQuests().get(questGiverComponent1.uniqueNPCID) == null) {
      fail(
          "Quest.getGoldSpentQuests() returned null even though a quest was "
              + "logged so it should have returned the quest");
    }
    questGiverComponent1.clearGoldSpentQuest();
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)) {
      fail("Quest.getQuestActiveForNPCID() returned true even though the quest was cleared");
    }
    if (Quest.getGoldSpentQuests().get(questGiverComponent1.uniqueNPCID) != null) {
      fail("Quest.getGoldSpentQuests() returned a quest even though the quest was cleared");
    }
  }

  @Test
  public void testCheckGoldSpentCompleteWhenNoQuestWasMade() {
    if (questGiverComponent.clearGoldSpentQuest()) {
      fail(
          "The function clearGoldSpentQuest() should have returned false when there's no quest to clear but didn't");
    }
    assertThrows(
        NullPointerException.class,
        () -> questGiverComponent.checkGoldSpentQuestComplete(),
        "The questGiverComponent did not throw a NullPointerException when it "
            + "called checkGoldSpentQuestComplete when it did not create a"
            + "GoldSpentQuest");
  }

  @Test
  public void testIfGoldSpentChangesProgressOfQuest() {
    player = new Entity();
    questGiverComponent.logGoldSpentQuest(100);
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    shop.setItemListing(1, new ShopComponent.ShopListing<>(weapon, 10));
    player.addComponent(inventory);
    player.addComponent(shop);
    assertEquals(0, questGiverComponent.checkGoldSpentQuestComplete());
    shop.buyItem(1);
    assertEquals(10, questGiverComponent.checkGoldSpentQuestComplete());
  }

  @Test
  public void testIfGoldSpentGivesAProgressValueOver100() {
    player = new Entity();
    questGiverComponent.logGoldSpentQuest(5);
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    shop.setItemListing(1, new ShopComponent.ShopListing<>(weapon, 10));
    player.addComponent(inventory);
    player.addComponent(shop);
    shop.buyItem(1);
    assertEquals(100, questGiverComponent.checkGoldSpentQuestComplete());
  }

  @Test
  public void testIfGivingGoldToSpendAsZeroThrows() {
    int number = 0;
    assertThrows(
        IllegalArgumentException.class,
        () -> questGiverComponent.logGoldSpentQuest(number),
        "When the constructor for GoldSpentQuest was given "
            + number
            + " as the"
            + "parameter for goldToSpend, the constructor didn't throw an exception");
  }
  @Test
  public void testIfGoldRewardIsGivenIfGoldSpentQuestCompletedAndCleared(){
    int goldToSpend = 10;
    ShopComponent shopComponent = new ShopComponent();
    InventoryComponent inventoryComponent = new InventoryComponent(goldToSpend);
    player.addComponent(shopComponent);
    player.addComponent(inventoryComponent);
    questGiverComponent.setGoldToGive(goldToSpend);
    questGiverComponent.logGoldSpentQuest(goldToSpend);
    shopComponent.setItemListing(1, new ShopComponent.ShopListing<>(weapon, goldToSpend));
    shopComponent.buyItem(1);
    questGiverComponent.clearGoldSpentQuest();
    assertEquals(goldToSpend, inventoryComponent.getGold());
  }
  @Test
  public void testIfItemRewardIsGivenIfGoldSpentQuestCompletedAndCleared(){
    int goldToSpend = 10;
    ShopComponent shopComponent = new ShopComponent();
    InventoryComponent inventoryComponent = new InventoryComponent(goldToSpend);
    player.addComponent(shopComponent);
    player.addComponent(inventoryComponent);
    questGiverComponent.setItemToGive(weapon);
    questGiverComponent.logGoldSpentQuest(goldToSpend);
    Item listingWeapon = (new WeaponGenerator()).generateWeapon(WeaponType.BOW,2);
    shopComponent.setItemListing(1, new ShopComponent.ShopListing<>(listingWeapon, goldToSpend));
    shopComponent.buyItem(1);
    questGiverComponent.clearGoldSpentQuest();
    assertEquals(weapon,inventoryComponent.getItem(2));
  }
  @Test
  public void testIfRewardIsGivenIfGoldSpentQuestIsNotCompletedButCleared(){
    int goldToSpend = 0;
    ShopComponent shopComponent = new ShopComponent();
    InventoryComponent inventoryComponent = new InventoryComponent(goldToSpend);
    player.addComponent(shopComponent);
    player.addComponent(inventoryComponent);
    questGiverComponent.setItemToGive(weapon);
    questGiverComponent.logGoldSpentQuest(10);
    questGiverComponent.clearGoldSpentQuest();
    assertNotEquals(weapon,inventoryComponent.getItem(1));
    assertNotEquals(10, inventoryComponent.getGold());
  }
}
