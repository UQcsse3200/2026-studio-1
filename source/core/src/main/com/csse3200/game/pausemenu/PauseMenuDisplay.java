package com.csse3200.game.pausemenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.ui.UIComponent;

public class PauseMenuDisplay extends UIComponent {

  private Table table;
  private PauseMenuComponent pauseMenu;
  private TextButton[] buttons;
  private int selectedIndex = 0;
  private boolean wasPaused = false;

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

    Label title = new Label("PAUSED", skin);

    TextButton resumeBtn = new TextButton("Resume", skin);
    TextButton restartBtn = new TextButton("Restart", skin);
    TextButton mainMenuBtn = new TextButton("Main Menu", skin);

    buttons = new TextButton[] {resumeBtn, restartBtn, mainMenuBtn};

    // Clicking a button does the same thing as navigating to it and pressing Enter.
    resumeBtn.addListener(
            new ChangeListener() {
              @Override
              public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                resume();
              }
            });
    restartBtn.addListener(
            new ChangeListener() {
              @Override
              public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                restart();
              }
            });
    mainMenuBtn.addListener(
            new ChangeListener() {
              @Override
              public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                goToMainMenu();
              }
            });

    table.add(title);
    table.row();

    table.add(resumeBtn).padTop(20f);
    table.row();

    table.add(restartBtn).padTop(15f);
    table.row();

    table.add(mainMenuBtn).padTop(15f);

    // Hidden until the game is actually paused.
    table.setVisible(false);
    stage.addActor(table);
  }

  /** Listens for the keyboard-navigation events fired by PauseMenuInputComponent. */
  private void registerEventListeners() {
    entity.getEvents().addListener("navigateUp", this::navigateUp);
    entity.getEvents().addListener("navigateDown", this::navigateDown);
    entity.getEvents().addListener("confirmSelection", this::confirmSelection);
  }

  private void navigateUp() {
    selectedIndex = (selectedIndex - 1 + buttons.length) % buttons.length;
    updateHighlight();
  }

  private void navigateDown() {
    selectedIndex = (selectedIndex + 1) % buttons.length;
    updateHighlight();
  }

  /** Highlights whichever button is currently selected via keyboard navigation. */
  private void updateHighlight() {
    for (int i = 0; i < buttons.length; i++) {
      buttons[i].setColor(i == selectedIndex ? Color.YELLOW : Color.WHITE);
    }
  }

  /** Enter/Space was pressed - trigger whatever the currently highlighted button does. */
  private void confirmSelection() {
    switch (selectedIndex) {
      case 0 -> resume();
      case 1 -> restart();
      case 2 -> goToMainMenu();
      default -> {}
    }
  }

  private void resume() {
    pauseMenu.toggleIsPaused(); // menu is open, so this closes it
  }

  private void restart() {
    // Not implemented yet - whichever component owns restart logic (e.g. MainGameActions)
    // should listen for this event.
    entity.getEvents().trigger("restartGame");
  }

  private void goToMainMenu() {
    // Not implemented yet - whichever component owns screen switching
    // should listen for this event.
    entity.getEvents().trigger("mainMenu");
  }

  @Override
  public void draw(SpriteBatch batch) {
    boolean isPaused = pauseMenu.isPaused();
    table.setVisible(isPaused);

    // Reset selection to the first button every time the menu is freshly opened.
    if (isPaused && !wasPaused) {
      selectedIndex = 0;
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
