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
import com.csse3200.game.win.TortoiseLedger;
import com.csse3200.game.win.WinEvaluator;
import com.csse3200.game.win.WinResult;
import com.csse3200.game.win.WinTier;
import java.util.List;

public class WinScreenDisplay extends UIComponent {
  /** The most guardians named on the screen; any others are summed up as "and N more". */
  private static final int MAX_MISSING_SHOWN = 4;

  private final GdxGame game;

  private Table rootTable;
  private TextButton[] buttons;
  private int selectedIndex = 0;

  // Package access, so a test can read what the screen says.
  Label titleLabel;
  Label subtitleLabel;
  Label rankLabel;
  Label guardiansLabel;
  Label questsLabel;
  Label tortoisesLabel;
  Label missingLabel;

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
    popup.setBackground(skin.getDrawable("window"));
    popup.pad(30f);

    titleLabel = new Label("YOU WIN!", skin, "title");
    subtitleLabel = new Label("", skin);
    rankLabel = new Label("", skin);
    guardiansLabel = new Label("", skin);
    questsLabel = new Label("", skin);
    tortoisesLabel = new Label("", skin);
    missingLabel = new Label("", skin);
    TextButton playAgainButton = new TextButton("Play Again", skin);
    TextButton menuButton = new TextButton("Main Menu", skin);

    buttons = new TextButton[] {playAgainButton, menuButton};

    popup.add(titleLabel).padBottom(10f);

    popup.row();
    popup.add(subtitleLabel).padBottom(5f);

    popup.row();
    popup.add(rankLabel).padBottom(15f);

    popup.row();
    popup.add(guardiansLabel);

    popup.row();
    popup.add(questsLabel);

    popup.row();
    popup.add(tortoisesLabel).padBottom(10f);

    popup.row();
    popup.add(missingLabel).padBottom(20f);

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

  /** Shows the win screen for the game as it stands right now, with the final boss defeated. */
  public void showWinScreen() {
    showWinScreen(WinEvaluator.evaluateNow(true));
  }

  /**
   * Shows the win screen for one outcome. It fills in the tier's title, and how many guardians,
   * quests and tortoises were done, then makes the popup visible.
   *
   * @param result the outcome to show; not null
   */
  public void showWinScreen(WinResult result) {
    WinTier tier = result.getTier();
    titleLabel.setText(tier.getTitle().toUpperCase());
    subtitleLabel.setText(tier.getSubtitle());
    rankLabel.setText(rankText(tier));
    guardiansLabel.setText(guardiansText(result));
    questsLabel.setText(questsText(result));
    tortoisesLabel.setText(tortoisesText(result));
    missingLabel.setText(missingText(result));
    rootTable.setVisible(true);
  }

  /**
   * Builds the line that shows the tier as a rank out of three, for example "Rank 2 of 3".
   *
   * @param tier the tier reached
   * @return the rank line
   */
  static String rankText(WinTier tier) {
    return "Rank " + tier.getLevel() + " of " + WinTier.LEGEND.getLevel();
  }

  /**
   * Builds the line that counts the side-room guardians, for example "Guardians defeated: 6 of 8".
   *
   * @param result the outcome being shown
   * @return the guardians line
   */
  static String guardiansText(WinResult result) {
    return "Guardians defeated: "
        + result.getMiniBossesDefeated()
        + " of "
        + result.getMiniBossesRequired();
  }

  /**
   * Builds the line that counts the quests, for example "Quests completed: 2 of 3".
   *
   * @param result the outcome being shown
   * @return the quests line
   */
  static String questsText(WinResult result) {
    return "Quests completed: " + result.getQuestsCompleted() + " of " + result.getQuestsRequired();
  }

  /**
   * Builds the line that counts the hidden tortoises, and names the Tortoise Champion when every
   * one of them was found.
   *
   * @param result the outcome being shown
   * @return the tortoises line
   */
  static String tortoisesText(WinResult result) {
    if (result.getTortoisesTotal() == 0) {
      return "Tortoises found: none are hidden yet";
    }
    String found =
        "Tortoises found: " + result.getTortoisesFound() + " of " + result.getTortoisesTotal();
    return result.isTortoiseChampion() ? found + " - " + TortoiseLedger.CHAMPION_TITLE : found;
  }

  /**
   * Builds the text that names what still stands between the player and the top tier. It is empty
   * at Legend.
   *
   * @param result the outcome being shown
   * @return the missing guardians, then the quests still needed
   */
  static String missingText(WinResult result) {
    if (result.isFullyComplete()) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    List<String> missing = result.getMissingMiniBosses();
    if (!missing.isEmpty()) {
      text.append("Still standing:");
      for (int i = 0; i < Math.min(missing.size(), MAX_MISSING_SHOWN); i++) {
        text.append("\n").append(missing.get(i));
      }
      if (missing.size() > MAX_MISSING_SHOWN) {
        text.append("\nand ").append(missing.size() - MAX_MISSING_SHOWN).append(" more");
      }
    }
    int questsShort = result.getQuestsRequired() - result.getQuestsCompleted();
    if (questsShort > 0) {
      if (text.length() > 0) {
        text.append("\n");
      }
      text.append(questsShort).append(questsShort == 1 ? " more quest" : " more quests");
    }
    return text.toString();
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
