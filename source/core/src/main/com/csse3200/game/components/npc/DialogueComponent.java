package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.QuestGiverComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DialogueComponent extends Component {
  private final String[] dialogue;

  private int currentLine = 0;

  enum state {
    LOGGING,
    CHECKING,
    CLEARING,
    DONE,
    ERROR
  }

  state currentState = state.LOGGING;

  private QuestGiverComponent questGiverComponent;

  private String questType;

  private int amountXToDo;

  private static Logger logger = LoggerFactory.getLogger(DialogueComponent.class);

  private boolean talking = false;

  public DialogueComponent(String[] dialogue, String questType, int amountXToDo) {
    this.dialogue = dialogue;
    this.questType = questType;
    this.amountXToDo = amountXToDo;
  }

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener("nextDialogue", this::nextDialogue);
    entity.getEvents().addListener("endDialogue", this::endDialogue);
    questGiverComponent = entity.getComponent(QuestGiverComponent.class);
  }

  private void nextDialogue() {
    if (dialogue.length == 0) {
      return;
    }
    if (!talking) {
      talking = true;
      showOpeningLine();
    } else if (currentState == state.LOGGING) {
      // The greeting was just shown, so this press hands out the quest.
      logCorrespondingQuest();
      currentState = state.CHECKING;
      showLine(state.CHECKING);
    } else {
      endDialogue();
    }
  }

  /** Shows the first line of a conversation, clearing a finished quest on this same press. */
  private void showOpeningLine() {
    switch (currentState) {
      case LOGGING:
        showLine(state.LOGGING);
        break;
      case CHECKING:
        if (checkCorrespondingQuest() >= 100) {
          clearAndShowResult();
        } else {
          showLine(state.CHECKING);
        }
        break;
      case CLEARING, ERROR:
        clearAndShowResult();
        break;
      case DONE:
        showLine(state.DONE);
        break;
    }
  }

  /** Clears the quest now and shows the cleared line, or the error line if clearing failed. */
  private void clearAndShowResult() {
    if (clearCorrospondingQuest()) {
      currentState = state.DONE;
      showLine(state.CLEARING);
    } else {
      currentState = state.ERROR;
      showLine(state.ERROR);
    }
  }

  /** Shows the line for a quest step, adding the quest's progress to the checking line. */
  private void showLine(state line) {
    currentLine = line.ordinal();
    if (currentLine >= dialogue.length) {
      endDialogue();
      return;
    }
    String text = dialogue[currentLine];
    if (line == state.CHECKING) {
      text = text + checkCorrespondingQuest() + "%";
    }
    entity.getEvents().trigger("showDialogue", text);
  }

  public void endDialogue() {
    talking = false;
    currentLine = 0;
    entity.getEvents().trigger("hideDialogue");
  }

  public boolean isTalking() {
    return talking;
  }

  public int getCurrentLine() {
    return currentLine;
  }

  public int getLineCount() {
    return dialogue.length;
  }

  private void logCorrespondingQuest() {
    switch (questType.toLowerCase()) {
      case "jumpquest":
        questGiverComponent.logJumpQuest(amountXToDo);
        break;
      case "enemiesquest":
        questGiverComponent.logEnemiesKilledQuest(amountXToDo);
        break;
      case "shieldscollectedquest":
        questGiverComponent.logShieldsCollectedQuest(amountXToDo);
        break;
      case "goldspentquest":
        questGiverComponent.logGoldSpentQuest(amountXToDo);
        break;
      default:
        logger.error(
            "In a NPC DialogueComponent, no quest type was specified for logging a corrosponding quest");
    }
  }

  private int checkCorrespondingQuest() {
    switch (questType.toLowerCase()) {
      case "jumpquest":
        return questGiverComponent.checkJumpQuestComplete();
      case "enemiesquest":
        return questGiverComponent.checkEnemiesKilledQuestComplete();
      case "shieldscollectedquest":
        return questGiverComponent.checkShieldsCollectedQuestComplete();
      case "goldspentquest":
        return questGiverComponent.checkGoldSpentQuestComplete();
      default:
        logger.error(
            "In a NPC DialogueComponent, no quest type was specified for checking a corrosponding quest");
        break;
    }
    return -1; // This should never be reached
  }

  private boolean clearCorrospondingQuest() {
    switch (questType.toLowerCase()) {
      case "jumpquest":
        return questGiverComponent.clearJumpQuest();
      case "enemiesquest":
        return questGiverComponent.clearEnemiesKilledQuest();
      case "shieldscollectedquest":
        return questGiverComponent.clearShieldsCollectedQuest();
      case "goldspentquest":
        return questGiverComponent.clearGoldSpentQuest();
      default:
        logger.error(
            "In a NPC DialogueComponent, no quest type was specified for clearing a corrosponding quest");
        break;
    }
    return false; // This should never be reached
  }

  // Do NOT call this function outside of NPCFactory
  public void changeQuestType(String questType) {
    this.questType = questType;
  }

  // Do NOT call this function outside of NPCFactory
  public void changeAmountXToDo(int amountXToDo) {
    if (amountXToDo <= 0) {
      return;
    }
    this.amountXToDo = amountXToDo;
  }

  /** Save/load: returns the quest step this NPC is on. */
  public String getQuestState() {
    return currentState.name();
  }

  /** Save/load: puts this NPC back on a saved quest step. Unknown names are ignored. */
  public void restoreQuestState(String stateName) {
    if (stateName == null) {
      return;
    }
    try {
      currentState = state.valueOf(stateName);
    } catch (IllegalArgumentException e) {
      logger.warn("Ignoring unknown saved quest state {}", stateName);
    }
  }

  /** Save/load: returns the type of quest this NPC gives out. */
  public String getQuestType() {
    return questType;
  }
}
