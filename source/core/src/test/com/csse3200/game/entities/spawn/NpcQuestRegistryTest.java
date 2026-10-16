package com.csse3200.game.entities.spawn;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.Quests.Quest;
import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.components.npc.DialogueComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.SavedNpc;
import com.csse3200.game.services.ServiceLocator;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class NpcQuestRegistryTest {
  private static final String NPC_ID = "npc:wizard@level1:12,5";
  private static final String[] LINES = {
    "Hello, here's a quest!",
    "Here's the progress of your quest: ",
    "I've cleared your quest!",
    "Thanks for the help",
    "Hmm, something went wrong with completing your quest..."
  };

  @BeforeEach
  void setUp() {
    ServiceLocator.registerEntityService(new EntityService());
    NpcQuestRegistry.reset();
  }

  @AfterEach
  void tearDown() {
    NpcQuestRegistry.reset();
  }

  private Entity createNpc(String npcId) {
    Entity npc =
        new Entity()
            .addComponent(new QuestGiverComponent(new Entity()))
            .addComponent(new DialogueComponent(LINES, Quest.JUMP_QUEST, 5))
            .addComponent(new PersistentNpcComponent(npcId));
    npc.create();
    return npc;
  }

  private QuestGiverComponent giver(Entity npc) {
    return npc.getComponent(QuestGiverComponent.class);
  }

  private DialogueComponent dialogue(Entity npc) {
    return npc.getComponent(DialogueComponent.class);
  }

  @Test
  void firstSpawnLeavesNpcUnchanged() {
    Entity npc = createNpc(NPC_ID);
    int ownId = giver(npc).uniqueNPCID;

    assertEquals("LOGGING", dialogue(npc).getQuestState());
    assertEquals(ownId, giver(npc).uniqueNPCID);
  }

  @Test
  void reviveKeepsQuestInProgress() {
    Entity first = createNpc(NPC_ID);
    first.getEvents().trigger("nextDialogue");
    assertEquals("CHECKING", dialogue(first).getQuestState());

    // On a revive the new NPC is created before the old one is disposed.
    Entity revived = createNpc(NPC_ID);
    first.dispose();

    assertEquals(giver(first).uniqueNPCID, giver(revived).uniqueNPCID);
    assertEquals("CHECKING", dialogue(revived).getQuestState());
    assertEquals("CHECKING", NpcQuestRegistry.exportAll().get(0).dialogueState);
  }

  @Test
  void finishedQuestCannotBeRepeatedAfterRevive() {
    Entity first = createNpc(NPC_ID);
    dialogue(first).restoreQuestState("DONE");

    Entity revived = createNpc(NPC_ID);

    assertEquals("DONE", dialogue(revived).getQuestState());
    assertNull(Quest.exportQuest(giver(revived).uniqueNPCID));
  }

  @Test
  void saveAndLoadKeepsQuestProgress() {
    Entity npc = createNpc(NPC_ID);
    npc.getEvents().trigger("nextDialogue");
    Quest.incrementGlobalJumps();
    Quest.incrementGlobalJumps();

    List<SavedNpc> saved = NpcQuestRegistry.exportAll();
    List<String> killed = NpcQuestRegistry.exportKilled();
    int[] counters = Quest.exportCounters();
    npc.dispose();

    NpcQuestRegistry.loadFrom(saved, killed);
    Quest.restoreCounters(counters[0], counters[1], counters[2], counters[3]);
    Entity loaded = createNpc(NPC_ID);

    assertEquals("CHECKING", dialogue(loaded).getQuestState());
    assertEquals(40, giver(loaded).checkJumpQuestComplete());
    assertEquals(5, giver(loaded).getAmountXToDo());
  }

  @Test
  void savedStepWithoutQuestStartsFreshAndDoesNotCrash() {
    SavedNpc saved = new SavedNpc();
    saved.id = NPC_ID;
    saved.dialogueState = "CHECKING";
    NpcQuestRegistry.loadFrom(List.of(saved), List.of());

    Entity npc = createNpc(NPC_ID);

    assertEquals("LOGGING", dialogue(npc).getQuestState());
    assertDoesNotThrow(() -> npc.getEvents().trigger("nextDialogue"));
    assertDoesNotThrow(() -> npc.getEvents().trigger("nextDialogue"));
  }

  @Test
  void savedQuestOfDifferentTypeIsNotGivenToNpc() {
    SavedNpc saved = new SavedNpc();
    saved.id = NPC_ID;
    saved.dialogueState = "CHECKING";
    saved.questType = Quest.GOLD_SPENT_QUEST;
    saved.amountToDo = 50;
    NpcQuestRegistry.loadFrom(List.of(saved), List.of());

    Entity npc = createNpc(NPC_ID);

    assertEquals("LOGGING", dialogue(npc).getQuestState());
    assertNull(Quest.exportQuest(giver(npc).uniqueNPCID));
    assertDoesNotThrow(() -> npc.getEvents().trigger("nextDialogue"));
    assertDoesNotThrow(() -> npc.getEvents().trigger("nextDialogue"));
  }

  @Test
  void unknownSavedStepIsIgnored() {
    SavedNpc saved = new SavedNpc();
    saved.id = NPC_ID;
    saved.dialogueState = "NOT_A_STATE";
    NpcQuestRegistry.loadFrom(List.of(saved), List.of());

    Entity npc = createNpc(NPC_ID);

    assertEquals("LOGGING", dialogue(npc).getQuestState());
  }

  @Test
  void deathIsRecordedAndSurvivesLoad() {
    Entity npc = createNpc(NPC_ID);
    npc.getEvents().trigger("death");

    assertTrue(NpcQuestRegistry.isKilled(NPC_ID));
    List<String> killed = NpcQuestRegistry.exportKilled();
    assertTrue(killed.contains(NPC_ID));

    NpcQuestRegistry.loadFrom(List.of(), killed);
    assertTrue(NpcQuestRegistry.isKilled(NPC_ID));
  }

  @Test
  void resetClearsKilledNpcsAndQuests() {
    Entity npc = createNpc(NPC_ID);
    npc.getEvents().trigger("nextDialogue");
    npc.getEvents().trigger("death");

    NpcQuestRegistry.reset();

    assertFalse(NpcQuestRegistry.isKilled(NPC_ID));
    assertTrue(NpcQuestRegistry.exportAll().isEmpty());
    assertTrue(Quest.getJumpQuests().isEmpty());
  }

  @Test
  void loadFromNullOrBadEntriesIsSafe() {
    SavedNpc noId = new SavedNpc();
    assertDoesNotThrow(() -> NpcQuestRegistry.loadFrom(null, null));
    assertDoesNotThrow(
        () -> NpcQuestRegistry.loadFrom(Arrays.asList(null, noId), Arrays.asList((String) null)));
    assertTrue(NpcQuestRegistry.exportAll().isEmpty());
    assertFalse(NpcQuestRegistry.isKilled(null));
  }

  @Test
  void npcWithoutQuestComponentsDoesNotCrash() {
    Entity bare = new Entity().addComponent(new PersistentNpcComponent(NPC_ID));
    assertDoesNotThrow(bare::create);

    Entity again = new Entity().addComponent(new PersistentNpcComponent(NPC_ID));
    assertDoesNotThrow(again::create);
    assertDoesNotThrow(NpcQuestRegistry::exportAll);
    assertNull(NpcQuestRegistry.exportAll().get(0).questType);
  }

  @Test
  void differentNpcsKeepSeparateState() {
    Entity wizard = createNpc(NPC_ID);
    Entity satyr = createNpc("npc:satyr@level1:20,5");
    wizard.getEvents().trigger("nextDialogue");

    assertNotEquals(giver(wizard).uniqueNPCID, giver(satyr).uniqueNPCID);
    assertEquals("CHECKING", dialogue(wizard).getQuestState());
    assertEquals("LOGGING", dialogue(satyr).getQuestState());
  }
}
