package com.csse3200.game.components.difficulty;

import com.csse3200.game.GdxGame;
import com.csse3200.game.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DifficultySelectActions extends Component {
    private static final Logger logger = LoggerFactory.getLogger(DifficultySelectActions.class);
    private final GdxGame game;

    public DifficultySelectActions(GdxGame game) {
        this.game = game;
    }

    @Override
    public void create() {
        entity.getEvents().addListener("easy", () -> startGame("EASY"));
        entity.getEvents().addListener("normal", () -> startGame("NORMAL"));
        entity.getEvents().addListener("hard", () -> startGame("HARD"));
        entity.getEvents().addListener("back", this::onBack);
    }

    /**
     * Starts a new run on the chosen difficulty.
     *
     * @param difficulty the chosen mode
     */
    private void startGame(String difficulty) {
        logger.info("Difficulty selected: {}", difficulty);
        // DifficultyService.setDifficulty(Difficulty.valueOf(difficulty));
        game.setScreenDeferred(GdxGame.ScreenType.MAIN_GAME);
    }


    private void onBack() {
        logger.info("Back to main menu");
        game.setScreenDeferred(GdxGame.ScreenType.MAIN_MENU);
    }
}