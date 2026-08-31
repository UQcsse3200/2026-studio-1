package com.csse3200.game.pausemenu;

import com.badlogic.gdx.audio.Music;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PauseMenuComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(PauseMenuComponent.class);
  static final String BACKGROUND_MUSIC =
      "sounds/BGM_03_mp3.mp3"; // change the background music file name here if you want to change
  // the music
  private boolean isPaused = false;

  public boolean isPaused() {
    return isPaused;
  }

  public void toggleIsPaused() {
    isPaused = !isPaused;
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
  }
}
