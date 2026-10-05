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
    if (talking == false) {
      talking = true;
      currentLine = currentState.ordinal();
    }
    if (currentLine < dialogue.length) {
      switch (currentState) {
        case LOGGING:
          logCorrespondingQuest();
          currentState = state.CHECKING;
          currentLine = currentState.ordinal();
          break;
        case CHECKING: // CHECKING quest
          if (checkCorrespondingQuest() >= 100) {
            currentState = state.CLEARING;
            currentLine = currentState.ordinal();
          }
          break;
        case CLEARING: // CLEARING quest (if error, go to ERROR (4) state)
          if (clearCorrospondingQuest()) {
            currentState = state.DONE;
            currentLine = currentState.ordinal();
          } else {
            currentState = state.ERROR;
            currentLine = currentState.ordinal();
          }
          break;
        case DONE: // DONE
          break;
        case ERROR: // ERROR (trouble clearing quest)
          currentState = state.CLEARING;
          currentLine = currentState.ordinal();
          break;
      }
      String text;
      if (currentState != state.CHECKING) {
        text = dialogue[currentLine];
      } else {
        // Give out the progress of the quest
        text = dialogue[currentLine] + checkCorrespondingQuest() + "%";
      }
      entity.getEvents().trigger("showDialogue", text);
    } else {
      endDialogue();
    }
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
  //Do NOT call this function outside of NPCFactory
  public void changeQuestType (String questType){
    this.questType = questType;
  }
  //Do NOT call this function outside of NPCFactory
  public void changeAmountXToDo(int amountXToDo){
    if(amountXToDo<=0){
      return;
    }
    this.amountXToDo = amountXToDo;
  }
}
