package com.csse3200.game.pausemenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.StaminaComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.win.QuestLedger;
import com.csse3200.game.win.TortoiseLedger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that a save carries the win system's progress: the quests completed and the tortoises found
 * are written into the {@link GameSaveData} that {@link PauseMenuActions} builds, so a loaded game
 * can still reach Legend or Tortoise Champion.
 *
 * <p>{@code createSaveData} only builds the data; nothing is written to disk here. The player is a
 * bare entity with the two components that method reads, health and an inventory.
 */
@ExtendWith(GameExtension.class)
class PauseMenuActionsWinSaveTest {
  private PauseMenuActions actions;
  private Entity player;

  @BeforeEach
  void beforeEach() {
    QuestLedger.reset();
    TortoiseLedger.reset();
    Map<String, Long> seeds = new HashMap<>();
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(50, 5))
            .addComponent(new InventoryComponent(7))
            .addComponent(new StaminaComponent());
    actions = new PauseMenuActions(() -> player, () -> seeds, () -> "maps/level3.json");
  }

  @AfterEach
  void afterEach() {
    QuestLedger.reset();
    TortoiseLedger.reset();
  }

  @Test
  void shouldSaveNoQuestsOrTortoisesForAFreshGame() {
    GameSaveData data = actions.createSaveData(player);

    assertTrue(data.completedQuestsByKind.isEmpty());
    assertTrue(data.foundTortoiseIds.isEmpty());
  }

  @Test
  void shouldSaveTheQuestsCompletedByKind() {
    QuestLedger.recordCompleted(QuestLedger.JUMP);
    QuestLedger.recordCompleted(QuestLedger.JUMP);
    QuestLedger.recordCompleted(QuestLedger.SHIELDS_COLLECTED);

    GameSaveData data = actions.createSaveData(player);

    assertEquals(2, data.completedQuestsByKind.get(QuestLedger.JUMP));
    assertEquals(1, data.completedQuestsByKind.get(QuestLedger.SHIELDS_COLLECTED));
    assertEquals(2, data.completedQuestsByKind.size());
  }

  @Test
  void shouldSaveTheTortoisesFound() {
    TortoiseLedger.recordFound("Hound's Den:3,2");
    TortoiseLedger.recordFound("Cyclops Forge:9,8");

    GameSaveData data = actions.createSaveData(player);

    assertEquals(
        Set.of("Hound's Den:3,2", "Cyclops Forge:9,8"), new HashSet<>(data.foundTortoiseIds));
  }

  @Test
  void shouldRestoreBothLedgersFromWhatWasSaved() {
    QuestLedger.recordCompleted(QuestLedger.GOLD_SPENT);
    TortoiseLedger.recordFound("a tortoise");
    GameSaveData data = actions.createSaveData(player);
    QuestLedger.reset();
    TortoiseLedger.reset();

    QuestLedger.loadFrom(data.completedQuestsByKind);
    TortoiseLedger.loadFrom(data.foundTortoiseIds);

    assertEquals(1, QuestLedger.getCompleted(QuestLedger.GOLD_SPENT));
    assertEquals(1, TortoiseLedger.getFoundCount());
  }

  @Test
  void shouldNotBeChangedByProgressMadeAfterTheSaveWasBuilt() {
    QuestLedger.recordCompleted(QuestLedger.JUMP);
    GameSaveData data = actions.createSaveData(player);

    QuestLedger.recordCompleted(QuestLedger.JUMP);
    TortoiseLedger.recordFound("found later");

    assertEquals(1, data.completedQuestsByKind.get(QuestLedger.JUMP), "a save is a snapshot");
    assertTrue(data.foundTortoiseIds.isEmpty());
  }

  @Test
  void shouldStillSaveWhatItSavedBefore() {
    GameSaveData data = actions.createSaveData(player);

    assertEquals(50, data.health);
    assertEquals(7, data.gold);
    assertEquals("maps/level3.json", data.level);
  }
}
