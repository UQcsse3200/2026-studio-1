package com.csse3200.game.pausemenu;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.csse3200.game.ui.UIComponent;

public class PauseMenuDisplay extends UIComponent {

  private Table table;

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    table = new Table();
    table.setFillParent(true);

    Label title = new Label("PAUSED", skin);

    TextButton resumeBtn = new TextButton("Resume", skin);
    TextButton restartBtn = new TextButton("Restart", skin);
    TextButton mainMenuBtn = new TextButton("Main Menu", skin);

    table.add(title);
    table.row();

    table.add(resumeBtn).padTop(20f);
    table.row();

    table.add(restartBtn).padTop(15f);
    table.row();

    table.add(mainMenuBtn).padTop(15f);

    stage.addActor(table);
  }

  @Override
  public void draw(SpriteBatch batch) {}

  @Override
  public void dispose() {
    table.clear();
    super.dispose();
  }
}
