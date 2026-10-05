package com.csse3200.game.pausemenu;

import com.badlogic.gdx.audio.Music;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PauseMenuComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(PauseMenuComponent.class);
  static final String BACKGROUND_MUSIC =
      "sounds/dungeon.mp3"; // change the background music file name here if you want to
  // change
  // the music
  private boolean isPaused = false;
  private boolean capturingKeybind = false;
  private static final AtomicBoolean gamePause = new AtomicBoolean(false);

  public boolean isPaused() {
    return isPaused;
  }

  public static AtomicBoolean isGamePaused() {
    return gamePause;
  }

  /** Whether the pause menu is currently waiting for the next key press to bind to an action. */
  public boolean isCapturingKeybind() {
    return capturingKeybind;
  }

  public void setCapturingKeybind(boolean capturingKeybind) {
    this.capturingKeybind = capturingKeybind;
  }

  public void toggleIsPaused() {
    isPaused = !isPaused;
    gamePause.set(isPaused);
    logger.info(
        "Paused: {}",
        isPaused); // resturns paused state in console, can be removed later if not needed
    Music music = ServiceLocator.getResourceService().getAsset(BACKGROUND_MUSIC, Music.class);
    if (music
        != null) { // pause/play music based on pause state. (also checks for music existance to
      // avoid crashing the game)
      if (isPaused) {
        music.pause();
      } else {
        music.play();
      }
    }
    ServiceLocator.getTimeSource().setTimeScale(isPaused ? 0f : 1f);
  }
}
