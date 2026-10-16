package com.csse3200.game.entities.spawn;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.components.npc.DialogueComponent;

/**
 * Gives a friendly NPC a stable id so its quest state and death survive saving, loading and
 * reviving. Added by LevelGameArea when the NPC is spawned.
 */
public class PersistentNpcComponent extends Component {
  private final String npcId;

  public PersistentNpcComponent(String npcId) {
    this.npcId = npcId;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("death", () -> NpcQuestRegistry.markKilled(npcId));
    NpcQuestRegistry.attach(npcId, this);
  }

  @Override
  public void dispose() {
    NpcQuestRegistry.detach(npcId, this);
  }

  public String getNpcId() {
    return npcId;
  }

  QuestGiverComponent getQuestGiver() {
    return entity == null ? null : entity.getComponent(QuestGiverComponent.class);
  }

  DialogueComponent getDialogue() {
    return entity == null ? null : entity.getComponent(DialogueComponent.class);
  }
}
