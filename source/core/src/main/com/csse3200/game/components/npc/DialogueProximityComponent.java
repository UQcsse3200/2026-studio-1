package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;

public class DialogueProximityComponent extends Component {

  private final Entity player;

  private final float dialogueDistance;

  private boolean playerInRange = false;

  public DialogueProximityComponent(Entity player, float dialogueDistance) {
    this.player = player;
    this.dialogueDistance = dialogueDistance;
  }

  @Override
  public void update() {
    super.update();

    if (player == null) {
      return;
    }

    Vector2 npcPosition = entity.getCenterPosition();

    Vector2 playerPosition = player.getCenterPosition();

    float distance = npcPosition.dst(playerPosition);

    if (distance <= dialogueDistance) {
      if (!playerInRange) {
        playerInRange = true;
        startDialogue();
      }
      if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
        nextDialogue();
      }
    } else {
      if (playerInRange) {
        playerInRange = false;
        endDialogue();
      }
    }
  }

  private void startDialogue() {
    DialogueComponent dialogue = entity.getComponent(DialogueComponent.class);
    if (dialogue == null) {
      return;
    }
    entity.getEvents().trigger("nextDialogue");
  }

  private void nextDialogue() {
    DialogueComponent dialogue = entity.getComponent(DialogueComponent.class);
    if (dialogue == null) {
      return;
    }
    entity.getEvents().trigger("nextDialogue");
  }

  private void endDialogue() {
    DialogueComponent dialogue = entity.getComponent(DialogueComponent.class);
    if (dialogue == null) {
      return;
    }
    entity.getEvents().trigger("endDialogue");
  }

  public boolean isPlayerInRange() {
    return playerInRange;
  }
}
