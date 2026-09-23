package com.csse3200.game.components.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class StoryCutsceneTest {

  @Test
  void startsAtFirstScene() {
    StoryScene first =
        new StoryScene("The Legend", "A legend speaks of a dangerous dungeon.", null);
    StoryScene second = new StoryScene("The Hero", "A hero decides to enter.", null);

    StoryCutscene cutscene = new StoryCutscene(List.of(first, second));

    assertEquals(first, cutscene.getCurrentScene());
    assertFalse(cutscene.isFinished());
  }

  @Test
  void advancesThroughScenesInOrder() {
    StoryScene first = new StoryScene("First", "First scene", null);
    StoryScene second = new StoryScene("Second", "Second scene", null);

    StoryCutscene cutscene = new StoryCutscene(List.of(first, second));

    assertFalse(cutscene.advance());
    assertEquals(second, cutscene.getCurrentScene());
    assertTrue(cutscene.isFinished());
  }

  @Test
  void finalSceneDoesNotAdvancePastEnd() {
    StoryScene scene = new StoryScene("Only", "Only scene", null);

    StoryCutscene cutscene = new StoryCutscene(List.of(scene));

    assertTrue(cutscene.advance());
    assertEquals(scene, cutscene.getCurrentScene());
  }

  @Test
  void resetReturnsToFirstScene() {
    StoryScene first = new StoryScene("First", "First scene", null);
    StoryScene second = new StoryScene("Second", "Second scene", null);

    StoryCutscene cutscene = new StoryCutscene(List.of(first, second));

    cutscene.advance();
    cutscene.reset();

    assertEquals(first, cutscene.getCurrentScene());
    assertFalse(cutscene.isFinished());
  }

  @Test
  void rejectsEmptyCutscene() {
    assertThrows(IllegalArgumentException.class, () -> new StoryCutscene(List.of()));
  }
}
