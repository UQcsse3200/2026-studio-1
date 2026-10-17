package com.csse3200.game.pausemenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.Quests.Quest;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.loot.ConsumableGenerator;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.player.BallisticShieldComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ShieldComponent;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.components.player.StaminaComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.entities.spawn.NpcQuestRegistry;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.files.LoadService;
import com.csse3200.game.files.SavedNpc;
import com.csse3200.game.files.SavedPerk;
import com.csse3200.game.perks.Perk;
import com.csse3200.game.perks.PerkService;
import com.csse3200.game.perks.tortoises.TortoisePlan;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.win.TortoiseLedger;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that {@link PauseMenuActions#createSaveData} writes the newer save fields: shields, shop
 * stock, perks, NPC quests, quest counters and the tortoise plan. The older fields are covered by
 * {@code PauseMenuActionsTest}.
 */
@ExtendWith(GameExtension.class)
class PauseMenuActionsSaveFieldsTest {
  private static final String LEVEL = "maps/level1-greek.json";
  private static final String NPC_ID = "npc@Level 1 - Out of the Underworld:8,3";

  private GameTime time;
  private Entity player;
  private PauseMenuActions actions;

  private Map<String, Perk> registeredPerks;
  private Map<String, Perk> originalPerks;
  private Perk testPerk;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(time);

    // PerkService is static, so swap in one test perk and put the real ones back afterwards.
    Field field = PerkService.class.getDeclaredField("perksById");
    field.setAccessible(true);
    registeredPerks = (Map<String, Perk>) field.get(null);
    originalPerks = new LinkedHashMap<>(registeredPerks);
    registeredPerks.clear();
    testPerk = new Perk("test_save_perk", "Test", "t", "testEvent", 3);
    PerkService.register(testPerk);
    PerkService.resetAll();

    resetGlobalState();

    player = createPlayer();
    actions = new PauseMenuActions(() -> player, HashMap::new, () -> LEVEL, ArrayList::new);
  }

  @AfterEach
  void tearDown() {
    resetGlobalState();
    PerkService.resetAll();
    registeredPerks.clear();
    registeredPerks.putAll(originalPerks);
  }

  private static void resetGlobalState() {
    NpcQuestRegistry.reset();
    TortoiseLedger.reset();
    TortoisePlan.reset();
  }

  private static Entity createPlayer() {
    Entity entity =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new StaminaComponent())
            .addComponent(new InventoryComponent(0))
            .addComponent(new ShieldComponent(30000L))
            .addComponent(new BallisticShieldComponent(30000L))
            .addComponent(
                PlayerFactory.installDefaultCatalog(
                    new ShopComponent(), new WeaponGenerator(), new ConsumableGenerator()));
    entity.create();
    return entity;
  }

  // Shields

  @Test
  void savesAHeldShieldThatWasNotUsed() {
    player.getComponent(ShieldComponent.class).grantShield();
    player.getComponent(BallisticShieldComponent.class).grantShield();

    GameSaveData data = actions.createSaveData(player);

    assertTrue(data.shieldHeld);
    assertTrue(data.ballisticShieldHeld);
    assertEquals(0L, data.shieldRemainingMillis);
    assertEquals(0L, data.ballisticShieldRemainingMillis);
  }

  @Test
  void savesTheTimeLeftOnActiveShields() {
    ShieldComponent shield = player.getComponent(ShieldComponent.class);
    BallisticShieldComponent ballistic = player.getComponent(BallisticShieldComponent.class);
    shield.grantShield();
    shield.activateShield();
    ballistic.grantShield();
    ballistic.activateBallisticShield();
    when(time.getTime()).thenReturn(10000L);

    GameSaveData data = actions.createSaveData(player);

    assertFalse(data.shieldHeld);
    assertEquals(20000L, data.shieldRemainingMillis);
    assertEquals(20000L, data.ballisticShieldRemainingMillis);
  }

  @Test
  void playerWithoutShieldsSavesNoShields() {
    Entity plain =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new StaminaComponent())
            .addComponent(new InventoryComponent(0));
    plain.create();

    GameSaveData data = actions.createSaveData(plain);

    assertFalse(data.shieldHeld);
    assertFalse(data.ballisticShieldHeld);
    assertNull(
        data.shopItemSlots, "no shop means nothing to save, so older load behaviour is kept");
  }

  // Shop

  @Test
  void savesWhichShopItemsAreStillInStock() {
    player.getComponent(ShopComponent.class).setItemListing(2, null);

    GameSaveData data = actions.createSaveData(player);

    assertEquals(List.of(1), data.shopItemSlots);
  }

  // Perks

  @Test
  void savesEveryPerksState() {
    PerkService.restorePerk(testPerk.getId(), 3, true, true);

    GameSaveData data = actions.createSaveData(player);

    assertNotNull(data.perks);
    assertEquals(1, data.perks.size());
    SavedPerk saved = data.perks.get(0);
    assertEquals("test_save_perk", saved.id);
    assertEquals(3, saved.progress);
    assertTrue(saved.unlocked);
    assertTrue(saved.active);
  }

  // Quests and NPCs

  @Test
  void savesNpcQuestStateAndKilledNpcs() {
    SavedNpc npc = new SavedNpc();
    npc.id = NPC_ID;
    npc.dialogueState = "CHECKING";
    npc.amountXToDo = 5;
    npc.questType = Quest.JUMP_QUEST;
    npc.amountToDo = 5;
    npc.snapshot = 2f;
    NpcQuestRegistry.loadFrom(List.of(npc), List.of("dead@Level 1 - Out of the Underworld:2,2"));

    GameSaveData data = actions.createSaveData(player);

    assertEquals(1, data.npcs.size());
    SavedNpc saved = data.npcs.get(0);
    assertEquals(NPC_ID, saved.id);
    assertEquals("CHECKING", saved.dialogueState);
    assertEquals(Quest.JUMP_QUEST, saved.questType);
    assertEquals(5, saved.amountToDo);
    assertEquals(List.of("dead@Level 1 - Out of the Underworld:2,2"), data.killedNpcIds);
  }

  @Test
  void savesTheQuestCounters() {
    Quest.restoreCounters(4, 2, 30, 1);

    GameSaveData data = actions.createSaveData(player);

    assertEquals(4, data.questJumps);
    assertEquals(2, data.questEnemiesKilled);
    assertEquals(30, data.questGoldSpent);
    assertEquals(1, data.questShieldsCollected);
  }

  // Tortoises

  @Test
  void savesTheTortoiseTotalAndPlan() {
    TortoiseLedger.setTotal(6);
    Map<String, Integer> plan = new HashMap<>();
    plan.put("Level 1 - Out of the Underworld", 2);
    TortoisePlan.set(plan);

    GameSaveData data = actions.createSaveData(player);

    assertEquals(6, data.tortoiseTotal);
    assertEquals(Integer.valueOf(2), data.tortoisePlan.get("Level 1 - Out of the Underworld"));
  }

  // Save then load

  @Test
  void savedShieldsAndShopComeBackOnANewPlayer() {
    ShieldComponent shield = player.getComponent(ShieldComponent.class);
    shield.grantShield();
    shield.activateShield();
    player.getComponent(BallisticShieldComponent.class).grantShield();
    player.getComponent(ShopComponent.class).setItemListing(1, null);
    when(time.getTime()).thenReturn(10000L);

    GameSaveData data = actions.createSaveData(player);

    // A new screen starts its game clock from zero again.
    when(time.getTime()).thenReturn(0L);
    Entity loaded = createPlayer();
    LoadService.apply(loaded, data, 50f, 30f, new ArrayList<>());

    assertTrue(loaded.getComponent(ShieldComponent.class).isActive());
    assertEquals(20000L, loaded.getComponent(ShieldComponent.class).getRemainingMillis());
    assertTrue(loaded.getComponent(BallisticShieldComponent.class).hasShield());
    assertEquals(
        List.of(2),
        new ArrayList<>(loaded.getComponent(ShopComponent.class).getItemCatalog().keySet()));
  }
}
