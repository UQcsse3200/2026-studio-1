package com.csse3200.game.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.LootTable;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.LootFactory;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Tests for {@link BonusLootDrop}. Requires Mockito's inline mock maker (default in Mockito 5+) for
 * {@code mockStatic}.
 *
 * <p>The "roll fails" branch is unreachable while BONUS_DROP_CHANCE is 1f, so it is not covered.
 */
class BonusLootDropTest {
  private MockedStatic<PerkService> perkService;
  private MockedStatic<LootFactory> lootFactory;
  private MockedStatic<ServiceLocator> serviceLocator;
  private MockedStatic<LootTable> lootTable;

  private EntityService entityService;
  private Entity deadEnemy;
  private Perk snatcher;

  @BeforeEach
  void setUp() {
    perkService = Mockito.mockStatic(PerkService.class);
    lootFactory = Mockito.mockStatic(LootFactory.class);
    serviceLocator = Mockito.mockStatic(ServiceLocator.class);
    lootTable = Mockito.mockStatic(LootTable.class);

    entityService = mock(EntityService.class);
    serviceLocator.when(ServiceLocator::getEntityService).thenReturn(entityService);

    snatcher = mock(Perk.class);
    when(snatcher.isActive()).thenReturn(true);
    perkService.when(() -> PerkService.getPerk("snatcher")).thenReturn(snatcher);

    LootTable table = mock(LootTable.class);
    when(table.rollItem()).thenReturn(new Item("Table Item", ItemType.CURRENCY, 1, 99));
    lootTable.when(() -> LootTable.createDefault(anyLong())).thenReturn(table);

    deadEnemy = mock(Entity.class);
    when(deadEnemy.getPosition()).thenReturn(new Vector2(3f, 4f));
    when(deadEnemy.getScale()).thenReturn(new Vector2(1.5f, 2f));
  }

  @AfterEach
  void tearDown() {
    lootTable.close();
    serviceLocator.close();
    lootFactory.close();
    perkService.close();
  }

  private Entity stubLoot() {
    Entity loot = mock(Entity.class);
    lootFactory
        .when(() -> LootFactory.createDroppedLoot(any(Item.class), eq(deadEnemy)))
        .thenReturn(loot);
    return loot;
  }

  @Test
  void doesNothingWhenPerkDoesNotExist() {
    perkService.when(() -> PerkService.getPerk("snatcher")).thenReturn(null);

    BonusLootDrop.tryBonusDrop(deadEnemy);

    lootFactory.verifyNoInteractions();
    verifyNoInteractions(entityService);
  }

  @Test
  void doesNothingWhenPerkInactive() {
    when(snatcher.isActive()).thenReturn(false);

    BonusLootDrop.tryBonusDrop(deadEnemy);

    lootFactory.verifyNoInteractions();
    verifyNoInteractions(entityService);
  }

  @Test
  void dropsAndRegistersOneLootEntityWhenActive() {
    Entity loot = stubLoot();

    BonusLootDrop.tryBonusDrop(deadEnemy);

    verify(entityService).register(loot);
  }

  @Test
  void positionsLootPastTheEnemy() {
    Entity loot = stubLoot();

    BonusLootDrop.tryBonusDrop(deadEnemy);

    // x = enemy.x + enemy.scale.x + 2 * DROP_GAP = 3 + 1.5 + 0.5 ; y = enemy.y
    verify(loot).setPosition(5f, 4f);
  }

  @Test
  void rolledItemsAreEitherBoundedGoldOrATableItem() {
    stubLoot();
    int runs = 200;

    for (int i = 0; i < runs; i++) {
      BonusLootDrop.tryBonusDrop(deadEnemy);
    }

    ArgumentCaptor<Item> items = ArgumentCaptor.forClass(Item.class);
    lootFactory.verify(
        () -> LootFactory.createDroppedLoot(items.capture(), eq(deadEnemy)), Mockito.times(runs));

    List<Item> dropped = new ArrayList<>(items.getAllValues());
    boolean sawGold = false;
    boolean sawTableItem = false;
    for (Item item : dropped) {
      if ("Gold".equals(item.getName())) {
        sawGold = true;
        assertTrue(
            item.getQuantity() >= 2 && item.getQuantity() <= 6,
            "gold quantity out of range: " + item.getQuantity());
      } else {
        sawTableItem = true;
        assertEquals("Table Item", item.getName());
      }
    }
    // With a 40% gold share over 200 rolls, missing either branch is astronomically unlikely.
    assertTrue(sawGold, "expected at least one gold drop");
    assertTrue(sawTableItem, "expected at least one table-item drop");
  }
}
