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
import com.csse3200.game.win.WinResult;
import com.csse3200.game.win.WinTier;

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

  // TODO (win system): add seven labels as package-access fields so a test can read them:
  //   titleLabel, subtitleLabel, rankLabel, guardiansLabel, questsLabel, tortoisesLabel,
  //   missingLabel. In create(), give the popup the skin's "window" background, and add the labels
  //   as rows above the two buttons. Make showWinScreen() show the result of
  //   WinEvaluator.evaluateNow(true).

  /**
   * Shows the win screen for one outcome. It fills in the tier's title, and how many guardians,
   * quests and tortoises were done, then makes the popup visible.
   *
   * @param result the outcome to show; not null
   */
  public void showWinScreen(WinResult result) {
    // BEGIN showWinScreen(result)
    //   tier <- the result's tier
    //   titleLabel     <- the tier's title in UPPER CASE
    //   subtitleLabel  <- the tier's subtitle
    //   rankLabel      <- rankText(tier)
    //   guardiansLabel <- guardiansText(result)
    //   questsLabel    <- questsText(result)
    //   tortoisesLabel <- tortoisesText(result)
    //   missingLabel   <- missingText(result)
    //   make the root table visible
    // END showWinScreen(result)
  }

  /**
   * Builds the line that shows the tier as a rank out of three, for example "Rank 2 of 3".
   *
   * @param tier the tier reached
   * @return the rank line
   */
  static String rankText(WinTier tier) {
    // BEGIN rankText
    //   give back "Rank <the tier's level> of <Legend's level>"
    // END rankText
    return "";
  }

  /**
   * Builds the line that counts the side-room guardians, for example "Guardians defeated: 6 of 8".
   *
   * @param result the outcome being shown
   * @return the guardians line
   */
  static String guardiansText(WinResult result) {
    // BEGIN guardiansText
    //   give back "Guardians defeated: <defeated> of <required>"
    // END guardiansText
    return "";
  }

  /**
   * Builds the line that counts the quests, for example "Quests completed: 2 of 3".
   *
   * @param result the outcome being shown
   * @return the quests line
   */
  static String questsText(WinResult result) {
    // BEGIN questsText
    //   give back "Quests completed: <completed> of <required>"
    // END questsText
    return "";
  }

  /**
   * Builds the line that counts the hidden tortoises, and names the Tortoise Champion when every
   * one of them was found.
   *
   * @param result the outcome being shown
   * @return the tortoises line
   */
  static String tortoisesText(WinResult result) {
    // BEGIN tortoisesText
    //   IF the tortoise total is zero THEN give back "Tortoises found: none are hidden yet"
    //   found <- "Tortoises found: <found> of <total>"
    //   IF the result is a tortoise champion THEN
    //     give back found + " - " + TortoiseLedger.CHAMPION_TITLE
    //   ELSE give back found
    // END tortoisesText
    return "";
  }

  /**
   * Builds the text that names what still stands between the player and the top tier. It is empty
   * at Legend.
   *
   * @param result the outcome being shown
   * @return the missing guardians, then the quests still needed
   */
  static String missingText(WinResult result) {
    // BEGIN missingText
    //   IF the result is fully complete THEN give back "" (empty)
    //   text <- empty
    //   missing <- the result's missing mini boss labels
    //   IF missing is not empty THEN
    //     add "Still standing:"
    //     FOR the first four labels (or all of them if there are fewer)
    //       add a new line, then the label
    //     END FOR
    //     IF there are more than four THEN add a new line, then "and <the rest> more"
    //   END IF
    //   short <- quests required minus quests completed
    //   IF short is above zero THEN
    //     IF text is not empty THEN add a new line
    //     add "<short> more quest" when short is 1, otherwise "<short> more quests"
    //   END IF
    //   give back the text
    // END missingText
    return "";
  }

  /**
   * @return whether the win screen popup is currently visible - used by WinScreenInputComponent to
   *     gate keyboard input the same way PauseMenuInputComponent gates on
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
