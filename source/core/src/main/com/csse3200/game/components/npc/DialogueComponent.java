package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;

public class DialogueComponent extends Component {
  private final String[] dialogue;

  private int currentLine = 0;

  private boolean talking = false;

  public DialogueComponent(String[] dialogue) {
    this.dialogue = dialogue;
  }

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener("nextDialogue", this::nextDialogue);
    entity.getEvents().addListener("endDialogue", this::endDialogue);
  }

  private void nextDialogue() {
    if (dialogue.length == 0) {
      return;
    }
    if (talking == false) {
      talking = true;
      currentLine = 0;
    }
    if (currentLine < dialogue.length) {
      String text = dialogue[currentLine];
      entity.getEvents().trigger("showDialogue", text);
      currentLine++;
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
}
