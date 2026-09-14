package com.csse3200.game.components.maingame;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.Test;

class MainGameActionsTest {
  @Test
  void shouldDeferRestartScreenChange() {
    GdxGame game = mock(GdxGame.class);
    Entity ui = new Entity().addComponent(new MainGameActions(game));
    ui.create();

    ui.getEvents().trigger("restartGame");

    verify(game).setScreenDeferred(GdxGame.ScreenType.MAIN_GAME);
  }

  @Test
  void shouldDeferMainMenuScreenChange() {
    GdxGame game = mock(GdxGame.class);
    Entity ui = new Entity().addComponent(new MainGameActions(game));
    ui.create();

    ui.getEvents().trigger("exit");

    verify(game).setScreenDeferred(GdxGame.ScreenType.MAIN_MENU);
  }
}
