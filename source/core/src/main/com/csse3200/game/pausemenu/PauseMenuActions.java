package com.csse3200.game.pausemenu;

import com.csse3200.game.components.Component;

public class PauseMenuActions extends Component {

    private PauseMenuComponent pauseMenu;

    @Override
    public void create() {
        super.create();
        pauseMenu = entity.getComponent(PauseMenuComponent.class);
        registerEventListeners();
    }

    private void registerEventListeners() {
        entity.getEvents().addListener("resumeClicked", this::resume);
        entity.getEvents().addListener("restartClicked", this::restart);
        entity.getEvents().addListener("mainMenuClicked", this::goToMainMenu);
    }

    private void resume() {
        pauseMenu.toggleIsPaused();
    }

    private void restart() {
        pauseMenu.toggleIsPaused();
        entity.getEvents().trigger("restartGame");
    }

    private void goToMainMenu() {
        entity.getEvents().trigger("exit");
    }
}
