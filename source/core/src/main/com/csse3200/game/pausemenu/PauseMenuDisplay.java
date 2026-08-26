package com.csse3200.game.pausemenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.ui.UIComponent;

public class PauseMenuDisplay extends UIComponent {

  private Table table;
  private Table pauseOverlay;
  private Table pausePanel;
  private PauseMenuComponent pauseMenu;
  private TextButton[] buttons;
  private int selectedIndex = 0;
  private boolean wasPaused = false;


  private boolean usingKeyboardNav = true;

  @Override
  public void create() {
    super.create();
    pauseMenu = entity.getComponent(PauseMenuComponent.class);
    addActors();
    registerEventListeners();
  }

  private void addActors() {
    table = new Table();
    table.setFillParent(true);
    pauseOverlay = new Table();
    pauseOverlay.setFillParent(true);
    pauseOverlay.setBackground(
            skin.newDrawable("white", new Color(0, 0, 0, 0.5f)));
    pauseOverlay.setVisible(false);

    stage.addActor(pauseOverlay);

    pausePanel = new Table();
    pausePanel.setBackground(
            skin.newDrawable("white", new Color(0.05f, 0.08f, 0.05f, 0.85f)));
    pausePanel.pad(35f);

    Label title = new Label("PAUSED", skin);
    title.getStyle().fontColor = Color.WHITE;

    TextButton resumeBtn = new TextButton("Resume", skin);
    TextButton restartBtn = new TextButton("Restart", skin);
    TextButton mainMenuBtn = new TextButton("Main Menu", skin);

    buttons = new TextButton[] {resumeBtn, restartBtn, mainMenuBtn};


    resumeBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            entity.getEvents().trigger("resumeClicked");
          }
        });
    restartBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            entity.getEvents().trigger("restartClicked");
          }
        });
    mainMenuBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            entity.getEvents().trigger("mainMenuClicked");
          }
        });

    pausePanel.add(title);
    pausePanel.row();

    pausePanel.add(resumeBtn).padTop(20f);
    pausePanel.row();

    pausePanel.add(restartBtn).padTop(15f);
    pausePanel.row();

    pausePanel.add(mainMenuBtn).padTop(15f);

    table.add(pausePanel).width(350f).height(350f);

    table.addListener(
        new InputListener() {
          @Override
          public boolean mouseMoved(InputEvent event, float x, float y) {
            usingKeyboardNav = false;
            for (TextButton button : buttons) {
              button.setColor(Color.WHITE);
            }
            return false;
          }
        });


    for (TextButton button : buttons) {
      button.addListener(
          new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
              if (!usingKeyboardNav) {
                button.setColor(Color.YELLOW);
              }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
              button.setColor(Color.WHITE);
            }
          });
    }


    table.setVisible(false);
    stage.addActor(table);
  }


  private void registerEventListeners() {
    entity.getEvents().addListener("navigateUp", this::navigateUp);
    entity.getEvents().addListener("navigateDown", this::navigateDown);
    entity.getEvents().addListener("confirmSelection", this::confirmSelection);
  }

  private void navigateUp() {
    usingKeyboardNav = true;
    selectedIndex = (selectedIndex - 1 + buttons.length) % buttons.length;
    updateHighlight();
  }

  private void navigateDown() {
    usingKeyboardNav = true;
    selectedIndex = (selectedIndex + 1) % buttons.length;
    updateHighlight();
  }


  private void updateHighlight() {
    if (!usingKeyboardNav) {
      return;
    }
    for (int i = 0; i < buttons.length; i++) {
      buttons[i].setColor(i == selectedIndex ? Color.YELLOW : Color.WHITE);
    }
  }


  private void confirmSelection() {
    usingKeyboardNav = true;
    updateHighlight();
    switch (selectedIndex) {
      case 0 -> entity.getEvents().trigger("resumeClicked");
      case 1 -> entity.getEvents().trigger("restartClicked");
      case 2 -> entity.getEvents().trigger("mainMenuClicked");
      default -> {}
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    boolean isPaused = pauseMenu.isPaused();
    pauseOverlay.setVisible(isPaused);
    table.setVisible(isPaused);


    if (isPaused && !wasPaused) {
      selectedIndex = 0;
      usingKeyboardNav = true;
      updateHighlight();
    }
    wasPaused = isPaused;
  }

  @Override
  public void dispose() {
    table.clear();
    super.dispose();
  }
}
