package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.GdxGame;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.ui.UIComponent;

public class WinScreenDisplay extends UIComponent {
  private final GdxGame game;

  private Table rootTable;
  private TextButton[] buttons;
  private int selectedIndex = 0;

  public WinScreenDisplay(GdxGame game) {
    super();
    this.game = game;
  }

  @Override
  public void create() {
    super.create();

    rootTable = new Table();
    rootTable.setFillParent(true);

    Table popup = new Table(skin);

    Label title = new Label("YOU WIN!", skin, "title");
    TextButton playAgainButton = new TextButton("Play Again", skin);
    TextButton menuButton = new TextButton("Main Menu", skin);

    buttons = new TextButton[] {playAgainButton, menuButton};

    popup.add(title).padBottom(30f);

    popup.row();
    popup.add(playAgainButton).width(180f).padBottom(15f);

    popup.row();
    popup.add(menuButton).width(180f);

    rootTable.add(popup);

    stage.addActor(rootTable);

    rootTable.setVisible(false);

    playAgainButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            onPlayAgain();
          }
        });

    menuButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            onMainMenu();
          }
        });

    registerEventListeners();
    updateHighlight();
  }

  /** Listens for the keyboard-navigation events fired by WinScreenInputComponent. */
  private void registerEventListeners() {
    entity.getEvents().addListener("winNavigateUp", this::navigateUp);
    entity.getEvents().addListener("winNavigateDown", this::navigateDown);
    entity.getEvents().addListener("winConfirmSelection", this::confirmSelection);
  }

  void navigateUp() {
    selectedIndex = (selectedIndex - 1 + buttons.length) % buttons.length;
    updateHighlight();
  }

  void navigateDown() {
    selectedIndex = (selectedIndex + 1) % buttons.length;
    updateHighlight();
  }

  /** Highlights whichever button is currently selected via keyboard navigation. */
  void updateHighlight() {
    for (int i = 0; i < buttons.length; i++) {
      buttons[i].setColor(i == selectedIndex ? Color.YELLOW : Color.WHITE);
    }
  }

  /** Enter/Space was pressed - trigger whatever the currently highlighted button does. */
  private void confirmSelection() {
    switch (selectedIndex) {
      case 0 -> onPlayAgain();
      case 1 -> onMainMenu();
      default -> {
        // No action needed for invalid selection index
      }
    }
  }

  private void onPlayAgain() {
    game.setScreen(ScreenType.MAIN_GAME);
  }

  private void onMainMenu() {
    game.setScreen(ScreenType.MAIN_MENU);
  }

  public void showWinScreen() {
    rootTable.setVisible(true);
  }

  /**
   * @return whether the win screen popup is currently visible - used by WinScreenInputComponent
   *     to gate keyboard input the same way PauseMenuInputComponent gates on
   *     PauseMenuComponent.isPaused().
   */
  public boolean isVisible() {
    return rootTable.isVisible();
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
