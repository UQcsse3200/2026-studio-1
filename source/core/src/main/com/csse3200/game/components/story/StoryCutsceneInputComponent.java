package com.csse3200.game.components.story;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;

/** Handles keyboard input while a story cutscene is active. */
public class StoryCutsceneInputComponent extends InputComponent {

  private boolean active = true;

  public StoryCutsceneInputComponent() {
    super(20);
  }

  @Override
  public void create() {
    super.create();

    entity.getEvents().addListener("storyFinished", this::disable);
  }

  @Override
  public boolean keyDown(int keycode) {
    if (!active) {
      return false;
    }

    if (keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER) {
      entity.getEvents().trigger("advanceStory");
      return true;
    }

    return true;
  }

  /** Disables story input once the cutscene has finished. */
  private void disable() {
    active = false;
  }
}
