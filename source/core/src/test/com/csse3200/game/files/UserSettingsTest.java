package com.csse3200.game.files;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.UserSettings.DisplaySettings;
import com.csse3200.game.files.UserSettings.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class UserSettingsTest {
  @Test
  void shouldApplySettings() {
    Gdx.graphics = mock(Graphics.class);
    DisplayMode displayMode = mock(DisplayMode.class);
    when(Gdx.graphics.getDisplayMode()).thenReturn(displayMode);

    Settings settings = new Settings();
    settings.vsync = true;
    settings.displayMode = null;
    settings.fullscreen = true;
    settings.fps = 40;
    UserSettings.applySettings(settings);

    verify(Gdx.graphics).setForegroundFPS(settings.fps);
    verify(Gdx.graphics).setFullscreenMode(displayMode);
    verify(Gdx.graphics).setVSync(settings.vsync);
  }

  @Test
  void shouldUseCurrentDisplayModeEvenWhenResolutionSaved() {
    Gdx.graphics = mock(Graphics.class);
    DisplayMode currentMode = mock(DisplayMode.class);
    when(Gdx.graphics.getDisplayMode()).thenReturn(currentMode);

    Settings settings = new Settings();
    settings.displayMode = new DisplaySettings();
    settings.displayMode.width = 200;
    settings.displayMode.height = 100;
    settings.displayMode.refreshRate = 60;
    settings.fullscreen = true;
    UserSettings.applySettings(settings);

    verify(Gdx.graphics).setFullscreenMode(currentMode);
  }

  /** This exists to make the constructor public */
  static class CustomDisplayMode extends DisplayMode {
    public CustomDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
      super(width, height, refreshRate, bitsPerPixel);
    }
  }
}
