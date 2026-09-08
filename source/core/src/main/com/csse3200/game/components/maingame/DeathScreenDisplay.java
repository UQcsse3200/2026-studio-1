package com.csse3200.game.components.maingame;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.GdxGame;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.ui.UIComponent;

public class DeathScreenDisplay extends UIComponent {
  private final GdxGame game;
  private Table rootTable;

  public DeathScreenDisplay(GdxGame game) {
    super();
    this.game = game;
  }

  @Override
  public void create() {
    super.create();

    rootTable = new Table();
    rootTable.setFillParent(true);

    Table popup = new Table(skin);

    Label title = new Label("YOU DIED", skin, "title");
    TextButton retryButton = new TextButton("Try Again", skin);
    TextButton menuButton = new TextButton("Main Menu", skin);

    popup.add(title).padBottom(30f);
    popup.row();
    popup.add(retryButton).width(180f).padBottom(15f);
    popup.row();
    popup.add(menuButton).width(180f);

    rootTable.add(popup);

    stage.addActor(rootTable);

    rootTable.setVisible(false);

    retryButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            game.setScreen(ScreenType.MAIN_GAME);
          }
        });

    menuButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            game.setScreen(ScreenType.MAIN_MENU);
          }
        });
  }

  public void showDeathScreen() {
    rootTable.setVisible(true);
  }

  @Override
  protected void draw(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
    // Drawing is handled by the Stage.
  }

  @Override
  public void update() {
    stage.act(com.csse3200.game.services.ServiceLocator.getTimeSource().getDeltaTime());
  }

  @Override
  public void dispose() {
    rootTable.clear();
    super.dispose();
  }
}
