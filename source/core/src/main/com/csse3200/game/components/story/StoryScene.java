package com.csse3200.game.components.story;

/**
 * Represents one scene in a story cutscene.
 *
 * <p>A scene contains the text shown to the player and an optional image path.
 */
public class StoryScene {
  private final String title;
  private final String text;
  private final String imagePath;

  /**
   * Creates a story scene.
   *
   * @param title short title displayed for the scene
   * @param text story text displayed to the player
   * @param imagePath path to the scene image, or null when no image is available
   */
  public StoryScene(String title, String text, String imagePath) {
    this.title = title;
    this.text = text;
    this.imagePath = imagePath;
  }

  public String getTitle() {
    return title;
  }

  public String getText() {
    return text;
  }

  public String getImagePath() {
    return imagePath;
  }
}
