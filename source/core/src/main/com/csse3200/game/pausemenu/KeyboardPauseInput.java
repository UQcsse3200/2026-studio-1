package com.csse3200.game.pausemenu;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;

public class KeyboardPauseInput extends InputComponent {
    private static final int TOGGLE_PAUSE_KEY = Input.Keys.ESCAPE;
    private PauseMenuComponent pauseMenu;

    public KeyboardPauseInput(){
        super(10);
    }
    
    public KeyboardPauseInput(PauseMenuComponent pauseMenu){
        this();
        this.pauseMenu = pauseMenu;
    }

    @Override
    public void create(){
        super.create();
        pauseMenu = entity.getComponent(PauseMenuComponent.class);
    }

    @Override
    public boolean keyDown(int keycode){
        if (keycode == TOGGLE_PAUSE_KEY){
            pauseMenu.toggleIsPaused();
            return true;
        }
        return false;
    }
}