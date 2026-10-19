package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.loot.ConsumableGenerator;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.upgrades.UpgradeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for restoring the shop's item stock on load, so items bought out before saving stay sold
 * out. The default catalog has items in slots 1 and 2.
 */
@ExtendWith(GameExtension.class)
class LoadServiceShopStockTest {
  private static final List<UpgradeNode> NO_UPGRADES = List.of();

  private Entity player;
  private ShopComponent shop;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(time);

    shop =
        PlayerFactory.installDefaultCatalog(
            new ShopComponent(), new WeaponGenerator(), new ConsumableGenerator());
    player = new Entity().addComponent(shop);
    player.create();
  }

  private void apply(GameSaveData data) {
    LoadService.apply(player, data, 50f, 30f, NO_UPGRADES);
  }

  private Set<Integer> itemSlots() {
    return shop.getItemCatalog().keySet();
  }

  @Test
  void olderSaveWithNoStockLeavesTheShopUnchanged() {
    GameSaveData data = new GameSaveData();

    apply(data);

    assertEquals(Set.of(1, 2), itemSlots());
  }

  @Test
  void itemsNotInTheSavedStockAreRemoved() {
    GameSaveData data = new GameSaveData();
    data.shopItemSlots = new ArrayList<>(List.of(1));

    apply(data);

    assertEquals(Set.of(1), itemSlots());
  }

  @Test
  void emptySavedStockMeansEverythingWasSoldOut() {
    GameSaveData data = new GameSaveData();
    data.shopItemSlots = new ArrayList<>();

    apply(data);

    assertTrue(itemSlots().isEmpty());
  }

  @Test
  void savedSlotsThatAreNotInTheShopAreIgnored() {
    GameSaveData data = new GameSaveData();
    data.shopItemSlots = new ArrayList<>(List.of(1, 2, 7));

    apply(data);

    assertEquals(Set.of(1, 2), itemSlots());
  }

  @Test
  @SuppressWarnings("unchecked")
  void slotNumbersReadBackAsOtherNumberTypesStillWork() {
    // The JSON reader can return slot numbers as Long or Float instead of Integer.
    List<Object> slots = new ArrayList<>();
    slots.add(2L);
    slots.add("not a number");
    GameSaveData data = new GameSaveData();
    data.shopItemSlots = (List<Integer>) (List<?>) slots;

    apply(data);

    assertEquals(Set.of(2), itemSlots());
  }

  @Test
  void stockSurvivesAJsonSaveAndLoad() {
    GameSaveData data = new GameSaveData();
    data.shopItemSlots = new ArrayList<>(List.of(2));

    String json = FileLoader.json.toJson(data);
    apply(FileLoader.json.fromJson(GameSaveData.class, json));

    assertEquals(Set.of(2), itemSlots());
  }

  @Test
  void playerWithoutAShopDoesNotCrash() {
    Entity noShop = new Entity();
    noShop.create();
    GameSaveData data = new GameSaveData();
    data.shopItemSlots = new ArrayList<>(List.of(1));

    assertDoesNotThrow(() -> LoadService.apply(noShop, data, 50f, 30f, NO_UPGRADES));
  }
}
