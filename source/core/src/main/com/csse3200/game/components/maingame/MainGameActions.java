package com.csse3200.game.components.maingame;

import com.csse3200.game.GdxGame;
import com.csse3200.game.components.Component;
import com.csse3200.game.screens.MainGameScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to events relevant to the Main Game Screen and does something when one of the
 * events is triggered.
 */
public class MainGameActions extends Component {
  private static final Logger logger = LoggerFactory.getLogger(MainGameActions.class);
  private final GdxGame game;
  private final MainGameScreen screen;

  public MainGameActions(GdxGame game, MainGameScreen screen) {
    this.game = game;
    this.screen = screen;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("exit", this::onExit);
    entity.getEvents().addListener("restartGame", this::onRestart);
  }

  private void onRestart() {
    logger.info("Restarting game");
    screen.resetPlayer();
  }

  /** Swaps to the Main Menu screen. */
  private void onExit() {
    logger.info("Exiting main game screen");
    game.setScreen(GdxGame.ScreenType.MAIN_MENU);
  }
}
