package com.csse3200.game.entities.spawn;

import com.csse3200.game.Quests.Quest;
import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.components.npc.DialogueComponent;
import com.csse3200.game.files.SavedNpc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps each friendly NPC's quest state by its stable id, so an NPC that is created again after a
 * revive or a load carries on where it left off. Also records which NPCs were killed.
 */
public final class NpcQuestRegistry {
  /** Quest steps that are only valid while the NPC has a quest in progress. */
  private static final Set<String> NEEDS_ACTIVE_QUEST = Set.of("CHECKING", "CLEARING", "ERROR");

  private static final Map<String, NpcState> states = new HashMap<>();
  private static final Set<String> killed = new HashSet<>();

  private NpcQuestRegistry() {
    throw new IllegalStateException("Utility class");
  }

  /** Clears all quest and NPC state. Called when a new game starts. */
  public static void reset() {
    Quest.resetAll();
    states.clear();
    killed.clear();
  }

  /** Replaces all quest and NPC state with what a save file holds. Called before a level loads. */
  public static void loadFrom(List<SavedNpc> savedNpcs, List<String> killedIds) {
    reset();
    if (killedIds != null) {
      for (String id : killedIds) {
        markKilled(id);
      }
    }
    if (savedNpcs == null) {
      return;
    }
    for (SavedNpc saved : savedNpcs) {
      if (saved == null || saved.id == null) {
        continue;
      }
      NpcState state = new NpcState();
      state.questId = Quest.giveOutUniqueNPCID();
      state.dialogueState = saved.dialogueState;
      state.amountXToDo = saved.amountXToDo;
      if (saved.questType != null) {
        Quest.restoreQuest(state.questId, saved.questType, saved.amountToDo, saved.snapshot);
      }
      states.put(saved.id, state);
    }
  }

  public static void markKilled(String id) {
    if (id != null) {
      killed.add(id);
    }
  }

  public static boolean isKilled(String id) {
    return id != null && killed.contains(id);
  }

  public static List<String> exportKilled() {
    return new ArrayList<>(killed);
  }

  /** Builds the save file entries for every NPC met so far, including ones in earlier levels. */
  public static List<SavedNpc> exportAll() {
    List<SavedNpc> result = new ArrayList<>();
    for (Map.Entry<String, NpcState> entry : states.entrySet()) {
      NpcState state = entry.getValue();
      if (state.owner != null) {
        capture(state, state.owner);
      }
      SavedNpc saved = new SavedNpc();
      saved.id = entry.getKey();
      saved.dialogueState = state.dialogueState;
      saved.amountXToDo = state.amountXToDo;
      Quest.ActiveQuest quest = Quest.exportQuest(state.questId);
      if (quest != null) {
        saved.questType = quest.type();
        saved.amountToDo = quest.amountToDo();
        saved.snapshot = quest.snapshot();
      }
      result.add(saved);
    }
    return result;
  }

  /** Called when an NPC is created. Gives it back its earlier quest state if there is one. */
  static void attach(String id, PersistentNpcComponent component) {
    QuestGiverComponent giver = component.getQuestGiver();
    DialogueComponent dialogue = component.getDialogue();
    NpcState state = states.get(id);

    if (state == null) {
      state = new NpcState();
      state.questId = giver == null ? -1 : giver.uniqueNPCID;
      state.owner = component;
      states.put(id, state);
      return;
    }

    // On a revive the new NPC is created before the old one is disposed, so read the old one first.
    if (state.owner != null && state.owner != component) {
      capture(state, state.owner);
    }
    state.owner = component;

    if (giver == null || dialogue == null) {
      return;
    }

    Quest.ActiveQuest quest = Quest.exportQuest(state.questId);
    boolean sameType = quest == null || quest.type().equalsIgnoreCase(dialogue.getQuestType());
    if (!Quest.isValidNPCID(state.questId) || !sameType) {
      // Cannot safely reuse the old quest, so the NPC starts fresh with its own id.
      state.questId = giver.uniqueNPCID;
      state.dialogueState = null;
      return;
    }

    giver.uniqueNPCID = state.questId;
    giver.restoreAmountXToDo(state.amountXToDo);
    boolean stepNeedsQuest =
        state.dialogueState != null && NEEDS_ACTIVE_QUEST.contains(state.dialogueState);
    if (quest != null || !stepNeedsQuest) {
      dialogue.restoreQuestState(state.dialogueState);
    }
  }

  /** Called when an NPC is disposed. Keeps its latest quest state. */
  static void detach(String id, PersistentNpcComponent component) {
    NpcState state = states.get(id);
    if (state != null && state.owner == component) {
      capture(state, component);
      state.owner = null;
    }
  }

  private static void capture(NpcState state, PersistentNpcComponent owner) {
    DialogueComponent dialogue = owner.getDialogue();
    QuestGiverComponent giver = owner.getQuestGiver();
    if (dialogue != null) {
      state.dialogueState = dialogue.getQuestState();
    }
    if (giver != null) {
      state.amountXToDo = giver.getAmountXToDo();
    }
  }

  private static final class NpcState {
    int questId = -1;
    String dialogueState;
    int amountXToDo;
    PersistentNpcComponent owner;
  }
}
