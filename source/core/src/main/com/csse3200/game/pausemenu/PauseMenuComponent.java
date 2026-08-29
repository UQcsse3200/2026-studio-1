package com.csse3200.game.pausemenu;

import com.badlogic.gdx.audio.Music;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PauseMenuComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(PauseMenuComponent.class);
  static final String BACKGROUND_MUSIC = "sounds/background_music.mp3";
  private boolean isPaused = false;

  public boolean isPaused() {
    return isPaused;
  }

  public void toggleIsPaused() {
    isPaused = !isPaused;
    logger.info("Paused: {}", isPaused);
    Music music = ServiceLocator.getResourceService().getAsset(BACKGROUND_MUSIC, Music.class);
    if (isPaused) {
      music.pause();
    } else {
      music.play();
    }
  }
}
