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

  @Test
  void afterDeathCutsceneContainsFourScenes() {
    StoryCutscene cutscene = StoryCutscene.createAfterDeathCutscene();

    assertEquals(4, cutscene.getSceneCount());
  }

  @Test
  void afterDeathCutsceneStartsAtTheFall() {
    StoryCutscene cutscene = StoryCutscene.createAfterDeathCutscene();

    assertEquals("THE FALL", cutscene.getCurrentScene().getTitle());
  }

  @Test
  void afterDeathCutsceneContainsExpectedScenesInOrder() {
    StoryCutscene cutscene = StoryCutscene.createAfterDeathCutscene();

    assertEquals("THE FALL", cutscene.getScenes().get(0).getTitle());
    assertEquals("THE LEGEND", cutscene.getScenes().get(1).getTitle());
    assertEquals("THE NEXT HERO", cutscene.getScenes().get(2).getTitle());
    assertEquals("THE JOURNEY CONTINUES", cutscene.getScenes().get(3).getTitle());
  }

  @Test
  void afterDeathCutsceneAdvancesToFinalScene() {
    StoryCutscene cutscene = StoryCutscene.createAfterDeathCutscene();

    assertFalse(cutscene.advance());
    assertFalse(cutscene.advance());
    assertFalse(cutscene.advance());

    assertTrue(cutscene.isFinished());
    assertEquals("THE JOURNEY CONTINUES", cutscene.getCurrentScene().getTitle());
  }
}
