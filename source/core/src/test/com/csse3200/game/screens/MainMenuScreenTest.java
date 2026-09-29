package com.csse3200.game.screens;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MainMenuScreenTest {
  @Test
  void restoresMenuBackgroundColourWhenShown() {
    MainMenuScreen screen = mock(MainMenuScreen.class, CALLS_REAL_METHODS);

    screen.show();

    verify(Gdx.gl).glClearColor(248f / 255f, 249f / 255f, 178f / 255f, 1f);
  }
}
