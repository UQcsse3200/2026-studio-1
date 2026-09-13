package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.Quests.JumpQuest;
import com.csse3200.game.Quests.Quest;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;

/**
 * UI component for displaying the player's current quests.
 */
public class QuestDisplay extends UIComponent {

  private Table rootTable;
  private Table questTable;
  private ArrayList<JumpQuest> jumpQuestsToDisplay;
  //Only proof of concept for now delete later
  int NPCID = 1;
  //Only proof of concept for now delete later

  @Override
  public void create() {
    super.create();
    //Only proof of concept for now delete later
    NPCID= Quest.giveOutUniqueNPCID();
    Quest.logJumpQuest(NPCID,5);
    jumpQuestsToDisplay = Quest.getJumpQuests();
    //Only proof of concept for now delete later
    addActors();
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
    questTable.add(title).left().padBottom(8f).row();


    rootTable.add(questTable).width(400f);

    stage.addActor(rootTable);
  }
  @Override
  public void update(){
    jumpQuestsToDisplay = Quest.getJumpQuests();
    questTable.clear();
    if(jumpQuestsToDisplay!= null) {
      for (int i = 0; i < jumpQuestsToDisplay.size(); i++) {
        if(jumpQuestsToDisplay.get(i)!=null) {
          jumpQuestsToDisplay.get(i).checkGlobalJumps();
          Label questToDisplay = new Label("Jump Quest: " + jumpQuestsToDisplay.get(i).checkQuestProgress()+"%", skin);
          questTable.add(questToDisplay).left().padTop(i * 5).row();
        }
      }
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