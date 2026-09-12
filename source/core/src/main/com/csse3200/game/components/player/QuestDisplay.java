package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.ui.UIComponent;

/** UI component for displaying the player's current quests. */
public class QuestDisplay extends UIComponent {

  private Table rootTable;
  private Table questTable;

  @Override
  public void create() {
    super.create();
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

    Label questOne = new Label("Kill the enemies", skin);
    questTable.add(questOne).left().row();

    Label questTwo = new Label("Collect the potions", skin);
    questTable.add(questTwo).left().padTop(5f).row();

    rootTable.add(questTable).width(400f);

    stage.addActor(rootTable);
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
