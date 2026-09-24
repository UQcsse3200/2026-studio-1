package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.Quests.JumpQuest;
import com.csse3200.game.Quests.Quest;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** UI component for displaying the player's current quests. */
public class QuestDisplay extends UIComponent {

  private Table rootTable;
  private Table questTable;
  private ArrayList<JumpQuest> jumpQuestsToDisplay;

  private final Set<JumpQuest> completedQuests = new HashSet<>();

  // Only proof of concept for now delete later
  int NPCID = 1;

  @Override
  public void create() {
    super.create();

    // Only proof of concept for now delete later
    NPCID = Quest.giveOutUniqueNPCID();
    Quest.logJumpQuest(NPCID, 5);
    jumpQuestsToDisplay = Quest.getJumpQuests();

    addActors();

    entity
        .getEvents()
        .addListener(
            "toggleQuestMenu",
            () -> {
              rootTable.setVisible(!rootTable.isVisible());
            });
  }

  /** Creates and positions the quest box. */
  private void addActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.top().right();
    rootTable.padTop(75f).padRight(20f);

    questTable = new Table();
    questTable.setBackground(skin.getDrawable("window"));
    questTable.pad(10f);

    Label title = new Label("QUEST", skin, "subtitle");
    questTable.add(title).left().padBottom(6f).row();

    rootTable.add(questTable).width(300f);

    stage.addActor(rootTable);
  }

  /** Updates the quest list and handles the quest menu toggle. */
  @Override
  public void update() {

    jumpQuestsToDisplay = Quest.getJumpQuests();

    refreshQuestTable();
  }

  /** Refreshes the completed and remaining quest sections. */
  private void refreshQuestTable() {
    questTable.clear();

    Label title = new Label("QUEST", skin, "subtitle");
    questTable.add(title).left().padBottom(6f).row();

    // Completed quests section
    Label completedTitle = new Label("COMPLETED", skin);
    questTable.add(completedTitle).left().padBottom(5f).row();

    if (!completedQuests.isEmpty()) {
      for (JumpQuest quest : completedQuests) {
        Label completedQuest = new Label("✓ Jump Quest", skin);
        questTable.add(completedQuest).left().row();
      }
    } else {
      Label noCompleted = new Label("No completed quests", skin);
      questTable.add(noCompleted).left().row();
    }

    // Remaining quests section
    Label remainingTitle = new Label("REMAINING", skin);
    questTable.add(remainingTitle).left().padTop(8f).padBottom(4f).row();

    boolean hasRemainingQuests = false;

    if (jumpQuestsToDisplay != null) {
      for (JumpQuest quest : jumpQuestsToDisplay) {
        if (quest != null) {
          quest.checkGlobalJumps();

          int progress = quest.checkQuestProgress();

          if (progress >= 100) {
            completedQuests.add(quest);
          } else {
            hasRemainingQuests = true;

            Label questToDisplay = new Label("• Jump Quest - " + progress + "%", skin);

            questTable.add(questToDisplay).left().row();
          }
        }
      }
    }

    if (!hasRemainingQuests) {
      Label noRemaining = new Label("No remaining quests", skin);
      questTable.add(noRemaining).left().row();
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawing is handled by the stage.
  }

  @Override
  public void dispose() {
    super.dispose();

    if (rootTable != null) {
      rootTable.remove();
    }
  }
}
