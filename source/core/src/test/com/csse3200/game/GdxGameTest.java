package com.csse3200.game;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class GdxGameTest {
  @Test
  void shouldQueueOnlyOneDeferredScreenChange() {
    Application originalApplication = Gdx.app;
    Application application = mock(Application.class);
    Gdx.app = application;
    try {
      GdxGame game = new GdxGame();

      game.setScreenDeferred(ScreenType.MAIN_GAME);
      game.setScreenDeferred(ScreenType.MAIN_MENU);

      verify(application, times(1)).postRunnable(any(Runnable.class));
    } finally {
      Gdx.app = originalApplication;
    }
  }
}
