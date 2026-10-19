package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.Quests.Quest;
import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DialogueComponentTest {
  private static final String[] LINES = {
    "Hello, here's a quest!",
    "Here's the progress of your quest: ",
    "I've cleared your quest!",
    "Thanks for the help",
    "Hmm, something went wrong with completing your quest..."
  };
  private static final int JUMPS_TO_DO = 2;

  private Entity npc;
  private DialogueComponent dialogue;
  private final List<String> shown = new ArrayList<>();
  private int hides;

  @BeforeEach
  void setUp() {
    Quest.resetAll();
    shown.clear();
    hides = 0;
  }

  @AfterEach
  void tearDown() {
    Quest.resetAll();
  }

  @Test
  void shouldShowGreetingOnFirstPress() {
    createNpc(playerWithInventory());

    press();

    assertEquals(List.of(LINES[0]), shown);
    assertEquals("LOGGING", dialogue.getQuestState());
    assertTrue(dialogue.isTalking());
  }

  @Test
  void shouldGiveQuestAndShowProgressOnSecondPress() {
    createNpc(playerWithInventory());

    press();
    press();

    assertEquals(LINES[1] + "0%", lastShown());
    assertEquals("CHECKING", dialogue.getQuestState());
  }

  @Test
  void shouldCloseOnThirdPress() {
    createNpc(playerWithInventory());

    press();
    press();
    press();

    assertEquals(1, hides);
    assertFalse(dialogue.isTalking());
  }

  @Test
  void shouldShowProgressThenCloseWhileQuestUnfinished() {
    createNpc(playerWithInventory());
    acceptQuest();
    Quest.incrementGlobalJumps();

    press();
    assertEquals(LINES[1] + "50%", lastShown());
    assertEquals("CHECKING", dialogue.getQuestState());

    press();
    assertFalse(dialogue.isTalking(), "the second press should close the conversation");
  }

  @Test
  void shouldClearQuestOnThePressThatShowsCleared() {
    createNpc(playerWithInventory());
    acceptQuest();
    finishQuest();

    press();

    assertEquals(LINES[2], lastShown());
    assertEquals("DONE", dialogue.getQuestState(), "the quest should clear on this same press");
    assertNull(Quest.exportQuest(questId()));
  }

  @Test
  void shouldShowThanksAndCloseOnceDone() {
    createNpc(playerWithInventory());
    acceptQuest();
    finishQuest();
    press();
    press();

    press();
    assertEquals(LINES[3], lastShown());

    press();
    assertFalse(dialogue.isTalking(), "the thanks line should not repeat forever");
  }

  @Test
  void shouldShowErrorLineWhenClearingFails() {
    createNpc(new Entity());
    acceptQuest();
    finishQuest();

    press();

    assertEquals(LINES[4], lastShown());
    assertEquals("ERROR", dialogue.getQuestState());
  }

  @Test
  void shouldClearRestoredClearingStateOnOpening() {
    createNpc(playerWithInventory());
    acceptQuest();
    finishQuest();
    dialogue.restoreQuestState("CLEARING");

    press();

    assertEquals(LINES[2], lastShown());
    assertEquals("DONE", dialogue.getQuestState());
  }

  @Test
  void shouldGreetAgainAfterWalkingAwayBeforeAccepting() {
    createNpc(playerWithInventory());
    press();

    npc.getEvents().trigger("endDialogue");
    press();

    assertEquals(1, hides);
    assertEquals(List.of(LINES[0], LINES[0]), shown);
    assertEquals("LOGGING", dialogue.getQuestState());
  }

  /** Builds an NPC with a jump quest and records what its dialogue box shows. */
  private void createNpc(Entity player) {
    npc =
        new Entity()
            .addComponent(new QuestGiverComponent(player))
            .addComponent(new DialogueComponent(LINES, Quest.JUMP_QUEST, JUMPS_TO_DO));
    npc.create();
    dialogue = npc.getComponent(DialogueComponent.class);
    npc.getEvents().addListener("showDialogue", (EventListener1<String>) shown::add);
    npc.getEvents().addListener("hideDialogue", () -> hides++);
  }

  /** Returns a player that can receive quest rewards. */
  private Entity playerWithInventory() {
    return new Entity().addComponent(new InventoryComponent(0));
  }

  /** Simulates the player pressing the dialogue key. */
  private void press() {
    npc.getEvents().trigger("nextDialogue");
  }

  /** Greets, accepts the quest and closes the conversation. */
  private void acceptQuest() {
    press();
    press();
    press();
  }

  /** Does enough jumps to finish the quest. */
  private void finishQuest() {
    for (int i = 0; i < JUMPS_TO_DO; i++) {
      Quest.incrementGlobalJumps();
    }
  }

  /** Returns the quest id this NPC's quest is tracked under. */
  private int questId() {
    return npc.getComponent(QuestGiverComponent.class).uniqueNPCID;
  }

  /** Returns the most recent line shown in the dialogue box. */
  private String lastShown() {
    return shown.get(shown.size() - 1);
  }
}
