package com.csse3200.game.components.difficulty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.csse3200.game.GdxGame;
import com.csse3200.game.difficulty.Difficulty;
import com.csse3200.game.difficulty.DifficultyService;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the difficulty-select menu doing nothing: startGame() used to have the
 * DifficultyService call commented out, so choosing a difficulty never actually applied it. Follows
 * MainGameActionsTest's pattern for driving a Component's events against a mocked GdxGame.
 */
class DifficultySelectActionsTest {

  @BeforeEach
  @AfterEach
  void resetToDefault() {
    DifficultyService.setCurrent(Difficulty.NORMAL);
  }

  @Test
  void triggeringEasySetsDifficultyToEasy() {
    GdxGame game = mock(GdxGame.class);
    Entity ui = new Entity().addComponent(new DifficultySelectActions(game));
    ui.create();

    ui.getEvents().trigger("easy");

    assertEquals(Difficulty.EASY, DifficultyService.getCurrent());
  }

  @Test
  void triggeringNormalSetsDifficultyToNormal() {
    GdxGame game = mock(GdxGame.class);
    Entity ui = new Entity().addComponent(new DifficultySelectActions(game));
    ui.create();

    DifficultyService.setCurrent(Difficulty.HARD); // starting from a non-default value
    ui.getEvents().trigger("normal");

    assertEquals(Difficulty.NORMAL, DifficultyService.getCurrent());
  }

  @Test
  void triggeringHardSetsDifficultyToHard() {
    GdxGame game = mock(GdxGame.class);
    Entity ui = new Entity().addComponent(new DifficultySelectActions(game));
    ui.create();

    ui.getEvents().trigger("hard");

    assertEquals(Difficulty.HARD, DifficultyService.getCurrent());
  }

  @Test
  void confirmingADifficultyOpensTheStoryCutsceneNotMainGameDirectly() {
    GdxGame game = mock(GdxGame.class);
    Entity ui = new Entity().addComponent(new DifficultySelectActions(game));
    ui.create();

    ui.getEvents().trigger("easy");

    verify(game).setScreenDeferred(GdxGame.ScreenType.STORY_CUTSCENE);
  }
}
