package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.Quests.*;
import com.csse3200.game.Quests.JumpQuest;
import com.csse3200.game.Quests.Quest;
import com.csse3200.game.perks.PerkService;
import com.csse3200.game.ui.UIComponent;
import com.csse3200.game.win.QuestLedger;
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
    restoreCompletedFromLedger();

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

    Label title = new Label("QUEST", skin);
    title.setAlignment(Align.center);
    questTable.add(title).expandX().fillX().padBottom(10f).row();

    rootTable.add(questTable).width(300f);

    stage.addActor(rootTable);
  }

  /** Updates the quest list. */
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

    Label title = new Label("QUEST", skin, "subtitle");
    title.setAlignment(Align.center);
    questTable.add(title).expandX().fillX().padBottom(10f).row();

    // Completed quests section
    Label completedTitle = new Label("COMPLETED", skin);
    questTable.add(completedTitle).expandX().fillX().left().padBottom(5f).row();

    if (PerkService.getPerk("timeLord").isUnlocked()) {
      Label questToDisplay = new Label("• Find the tortoise!", skin);

      questTable.add(questToDisplay).expandX().fillX().left().row();
    }

    if (!completedJumpQuests.isEmpty()) {
      for (JumpQuest quest : completedJumpQuests) {
        Label completedQuest = new Label("✓ Jump Quest", skin);
        questTable.add(completedQuest).expandX().fillX().left().row();
      }
    }
    if (!completedEnemiesKilledQuest.isEmpty()) {
      for (EnemiesKilledQuest quest : completedEnemiesKilledQuest) {
        Label completedQuest = new Label("✓ Enemies Killed Quest", skin);
        questTable.add(completedQuest).left().row();
      }
    }
    if (!completedGoldSpentQuest.isEmpty()) {
      for (GoldSpentQuest quest : completedGoldSpentQuest) {
        Label completedQuest = new Label("✓ Gold Spent Quest", skin);
        questTable.add(completedQuest).left().row();
      }
    }
    if (!completedShieldsCollectedQuest.isEmpty()) {
      for (ShieldsCollectedQuest quest : completedShieldsCollectedQuest) {
        Label completedQuest = new Label("✓ Shields Collected Quest", skin);
        questTable.add(completedQuest).left().row();
      }
    }
    if (!PerkService.getPerk("timeLord").isUnlocked()
        && completedEnemiesKilledQuest.isEmpty()
        && completedJumpQuests.isEmpty()
        && completedGoldSpentQuest.isEmpty()
        && completedShieldsCollectedQuest.isEmpty()) {
      Label noCompleted = new Label("No completed quests", skin);
      questTable.add(noCompleted).expandX().fillX().left().row();
    }

    // Remaining quests section
    Label remainingTitle = new Label("REMAINING", skin);
    questTable.add(remainingTitle).expandX().fillX().left().padTop(10f).padBottom(5f).row();

    boolean hasRemainingQuests = false;

    if (!PerkService.getPerk("timeLord").isUnlocked()) {
      Label questToDisplay = new Label("• Find the tortoise!", skin);

      questTable.add(questToDisplay).expandX().fillX().left().row();
    }

    if (jumpQuestsToDisplay != null) {
      for (JumpQuest quest : jumpQuestsToDisplay) {
        if (quest != null) {

          int progress = quest.checkQuestProgress();

          if (progress >= 100) {
            completedJumpQuests.add(quest);
          } else {
            hasRemainingQuests = true;

            Label questToDisplay = new Label("• Jump Quest - " + progress + "%", skin);

            questTable.add(questToDisplay).expandX().fillX().left().row();
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

    if (!hasRemainingQuests && PerkService.getPerk("timeLord").isUnlocked()) {
      Label noRemaining = new Label("No remaining quests", skin);
      questTable.add(noRemaining).expandX().fillX().left().row();
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

  /**
   * Save/load: lists the quests already turned in, so the Completed section is not empty after a
   * load or a revive. The entries only feed the "✓" labels and are never checked for progress.
   */
  private void restoreCompletedFromLedger() {
    for (int i = 0; i < QuestLedger.getCompleted(QuestLedger.JUMP); i++) {
      completedJumpQuests.add(new JumpQuest(1));
    }
    for (int i = 0; i < QuestLedger.getCompleted(QuestLedger.ENEMIES_KILLED); i++) {
      completedEnemiesKilledQuest.add(new EnemiesKilledQuest(1));
    }
    for (int i = 0; i < QuestLedger.getCompleted(QuestLedger.GOLD_SPENT); i++) {
      completedGoldSpentQuest.add(new GoldSpentQuest(1));
    }
    for (int i = 0; i < QuestLedger.getCompleted(QuestLedger.SHIELDS_COLLECTED); i++) {
      completedShieldsCollectedQuest.add(new ShieldsCollectedQuest(1));
    }
  }
}
