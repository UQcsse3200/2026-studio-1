package com.csse3200.game.components.story;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stores and controls the sequence of scenes that make up a story cutscene. */
public class StoryCutscene {
  private final List<StoryScene> scenes;
  private int currentSceneIndex;

  /**
   * Creates a cutscene from the supplied scenes.
   *
   * @param scenes scenes in the order they should be displayed
   */
  public StoryCutscene(List<StoryScene> scenes) {
    if (scenes == null || scenes.isEmpty()) {
      throw new IllegalArgumentException("A cutscene must contain at least one scene.");
    }

    this.scenes = Collections.unmodifiableList(new ArrayList<>(scenes));
    this.currentSceneIndex = 0;
  }

  /**
   * @return the scene currently being displayed
   */
  public StoryScene getCurrentScene() {
    return scenes.get(currentSceneIndex);
  }

  /**
   * Moves to the next scene.
   *
   * @return true if the cutscene has finished after advancing
   */
  public boolean advance() {
    if (currentSceneIndex < scenes.size() - 1) {
      currentSceneIndex++;
      return false;
    }

    return true;
  }

  /**
   * @return true when the current scene is the final scene
   */
  public boolean isFinished() {
    return currentSceneIndex == scenes.size() - 1;
  }

  /**
   * @return the number of scenes in this cutscene
   */
  public int getSceneCount() {
    return scenes.size();
  }

  /** Resets the cutscene back to its first scene. */
  public void reset() {
    currentSceneIndex = 0;
  }

  /**
   * @return an immutable view of all scenes
   */
  public List<StoryScene> getScenes() {
    return scenes;
  }
}
