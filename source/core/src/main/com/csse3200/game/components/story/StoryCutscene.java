package com.csse3200.game.components.story;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores and controls the sequence of scenes that make up the story cutscene.
 *
 * <p>The cutscene contains five scenes that introduce the hero, show the journey into the dungeon,
 * the hero's defeat, and the beginning of a new hero's journey.
 */
public class StoryCutscene {

  private final List<StoryScene> scenes;
  private int currentSceneIndex;

  /**
   * Creates the default story cutscene.
   *
   * <p>Each scene contains its own title, story text and background image.
   */
  public StoryCutscene() {
    List<StoryScene> storyScenes = new ArrayList<>();

    storyScenes.add(
        new StoryScene(
            "THE LEGEND",
            "A legend speaks of a dangerous dungeon and a treasure hidden deep within.",
            "images/cutscenes/scene1.png"));

    storyScenes.add(
        new StoryScene(
            "THE JOURNEY",
            "Driven by the promise of treasure, the hero enters the forgotten ruins.",
            "images/cutscenes/scene2.png"));

    storyScenes.add(
        new StoryScene(
            "THE DUNGEON",
            "The deeper the hero travels, the more dangerous the dungeon becomes.",
            "images/cutscenes/scene3.png"));

    storyScenes.add(
        new StoryScene(
            "THE FALL",
            "But the dungeon demands a price. The hero falls before reaching the treasure.",
            "images/cutscenes/scene4.png"));

    storyScenes.add(
        new StoryScene(
            "THE LEGEND LIVES ON",
            "The hero's story becomes a legend, inspiring another to finish what was started.",
            "images/cutscenes/scene5.png"));

    this.scenes = Collections.unmodifiableList(storyScenes);
    this.currentSceneIndex = 0;
  }

  /**
   * Creates a cutscene from a supplied list of scenes.
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
