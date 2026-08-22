package com.csse3200.game.pausemenu;

import com.csse3200.game.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PauseMenuComponent extends Component {
    private static final Logger logger = LoggerFactory.getLogger(PauseMenuComponent.class);
    private boolean isPaused = false;

    public boolean isPaused(){
        return isPaused;
    }

    public void toggleIsPaused(){
        isPaused = !isPaused;
        logger.info("Paused: {}", isPaused);
    }
}
