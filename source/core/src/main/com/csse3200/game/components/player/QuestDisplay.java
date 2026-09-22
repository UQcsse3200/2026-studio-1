package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.Quests.*;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** UI component for displaying the player's current quests. */
public class QuestDisplay extends UIComponent {

  private Table rootTable;
  private Table questTable;
  private ArrayList<JumpQuest> jumpQuestsToDisplay;
  private ArrayList<EnemiesKilledQuest> enemiesKilledQuestsToDisplay;
  private ArrayList<GoldSpentQuest> goldSpentQuestsToDisplay;
  private ArrayList<ShieldsCollectedQuest> shieldsCollectedQuestsToDisplay;

  private final Set<JumpQuest> completedJumpQuests = new HashSet<>();
  private final Set<EnemiesKilledQuest> completedEnemiesKilledQuest = new HashSet<>();
  private final Set<GoldSpentQuest> completedGoldSpentQuest = new HashSet<>();
  private final Set<ShieldsCollectedQuest> completedShieldsCollectedQuest = new HashSet<>();

  @Override
  public void create() {
    super.create();
    jumpQuestsToDisplay = Quest.getJumpQuests();
    enemiesKilledQuestsToDisplay = Quest.getEnemiesKilledQuests();
    goldSpentQuestsToDisplay = Quest.getGoldSpentQuests();
    shieldsCollectedQuestsToDisplay = Quest.getShieldsCollectedQuests();
    Quest.logShieldsCollectedQuest(Quest.giveOutUniqueNPCID(),2);

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
    questTable.setBackground(skin.getDrawable("window-w"));
    questTable.pad(14f);

    Label title = new Label("QUEST", skin, "large");
    questTable.add(title).left().padBottom(10f).row();

    rootTable.add(questTable).width(400f);

    stage.addActor(rootTable);
  }

  /** Updates the quest list and handles the quest menu toggle. */
  @Override
  public void update() {

    jumpQuestsToDisplay = Quest.getJumpQuests();
    enemiesKilledQuestsToDisplay = Quest.getEnemiesKilledQuests();
    goldSpentQuestsToDisplay = Quest.getGoldSpentQuests();
    shieldsCollectedQuestsToDisplay = Quest.getShieldsCollectedQuests();

    refreshQuestTable();
  }

  /** Refreshes the completed and remaining quest sections. */
  private void refreshQuestTable() {
    questTable.clear();

    Label title = new Label("QUEST", skin, "large");
    questTable.add(title).left().padBottom(10f).row();

    // Completed quests section
    Label completedTitle = new Label("COMPLETED", skin);
    questTable.add(completedTitle).left().padBottom(5f).row();

    if (!completedJumpQuests.isEmpty()) {
      for (JumpQuest quest : completedJumpQuests) {
        Label completedQuest = new Label("✓ Jump Quest", skin);
        questTable.add(completedQuest).left().row();
      }
    }
    if (!completedEnemiesKilledQuest.isEmpty()) {
      for (EnemiesKilledQuest quest : completedEnemiesKilledQuest) {
        Label completedQuest = new Label("✓ Enemies Killed Quest", skin);
        questTable.add(completedQuest).left().row();
      }
    }
    if(!completedGoldSpentQuest.isEmpty()){
      for (GoldSpentQuest quest : completedGoldSpentQuest) {
        Label completedQuest = new Label("✓ Gold Spent Quest", skin);
        questTable.add(completedQuest).left().row();
      }
    }
    if(!completedShieldsCollectedQuest.isEmpty()){
      for (ShieldsCollectedQuest quest : completedShieldsCollectedQuest) {
        Label completedQuest = new Label("✓ Shields Collected Quest", skin);
        questTable.add(completedQuest).left().row();
      }
    }
    if (completedEnemiesKilledQuest.isEmpty() && completedJumpQuests.isEmpty()&&completedGoldSpentQuest.isEmpty() &&
            completedShieldsCollectedQuest.isEmpty()) {
      Label noCompleted = new Label("No completed quests", skin);
      questTable.add(noCompleted).left().row();
    }

    // Remaining quests section
    Label remainingTitle = new Label("REMAINING", skin);
    questTable.add(remainingTitle).left().padTop(10f).padBottom(5f).row();

    boolean hasRemainingQuests = false;

    if (jumpQuestsToDisplay != null) {
      for (JumpQuest quest : jumpQuestsToDisplay) {
        if (quest != null) {

          int progress = quest.checkQuestProgress();

          if (progress >= 100) {
            completedJumpQuests.add(quest);
          } else {
            hasRemainingQuests = true;

            Label questToDisplay = new Label("• Jump Quest - " + progress + "%", skin);

            questTable.add(questToDisplay).left().row();
          }
        }
      }
    }
    if (enemiesKilledQuestsToDisplay != null) {
      for (EnemiesKilledQuest quest : enemiesKilledQuestsToDisplay) {
        if (quest != null) {

          int progress = quest.checkQuestProgress();

          if (progress >= 100) {
            completedEnemiesKilledQuest.add(quest);
          } else {
            hasRemainingQuests = true;
            Label questToDisplay = new Label("• Enemies Killed Quest - " + progress + "%", skin);

            questTable.add(questToDisplay).left().row();
          }
        }
      }
    }
    if (goldSpentQuestsToDisplay != null) {
      for (GoldSpentQuest quest : goldSpentQuestsToDisplay) {
        if (quest != null) {

          int progress = quest.checkQuestProgress();

          if (progress >= 100) {
            completedGoldSpentQuest.add(quest);
          } else {
            hasRemainingQuests = true;
            Label questToDisplay = new Label("• Gold Spent Quest - " + progress + "%", skin);

            questTable.add(questToDisplay).left().row();
          }
        }
      }
    }
    if (shieldsCollectedQuestsToDisplay != null) {
      for (ShieldsCollectedQuest quest : shieldsCollectedQuestsToDisplay) {
        if (quest != null) {

          int progress = quest.checkQuestProgress();

          if (progress >= 100) {
            completedShieldsCollectedQuest.add(quest);
          } else {
            hasRemainingQuests = true;
            Label questToDisplay = new Label("• Shields Collected Quest - " + progress + "%", skin);

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
