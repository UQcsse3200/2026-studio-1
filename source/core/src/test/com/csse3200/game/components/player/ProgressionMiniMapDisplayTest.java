package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ProgressionMiniMapDisplayTest {
  private static final float EPSILON = 0.0001f;

  @Test
  void createsWithProjectSkinAndPlayerIcon() {
    Texture playerIcon = mock(Texture.class);
    when(playerIcon.getWidth()).thenReturn(49);
    when(playerIcon.getHeight()).thenReturn(56);

    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset("images/knight_default.png", Texture.class)).thenReturn(playerIcon);
    ServiceLocator.registerResourceService(resources);

    Stage stage = mock(Stage.class);
    RenderService renderer = mock(RenderService.class);
    when(renderer.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderer);

    ProgressionMiniMapDisplay display = new ProgressionMiniMapDisplay(() -> null);
    assertDoesNotThrow(display::create);
  }

  @Test
  void identifiesLevelFromMapPath() {
    assertEquals(0, ProgressionMiniMapDisplay.levelIndex("maps/level1-greek.json"));
    assertEquals(1, ProgressionMiniMapDisplay.levelIndex("maps/level2.json"));
    assertEquals(2, ProgressionMiniMapDisplay.levelIndex("maps/level3.json"));
  }

  @Test
  void levelOneOccupiesBottomThird() {
    float progress =
        ProgressionMiniMapDisplay.overallProgress(
            "maps/level1-greek.json", new Vector2(10f, 25f), 50f, 50f);

    assertEquals(1f / 6f, progress, EPSILON);
  }

  @Test
  void levelTwoOccupiesMiddleThird() {
    float progress =
        ProgressionMiniMapDisplay.overallProgress(
            "maps/level2.json", new Vector2(10f, 50f), 50f, 100f);

    assertEquals(0.5f, progress, EPSILON);
  }

  @Test
  void levelThreeUsesHorizontalPalaceProgress() {
    float progress =
        ProgressionMiniMapDisplay.overallProgress(
            "maps/level3.json", new Vector2(25f, 2f), 100f, 10f);

    assertEquals(0.75f, progress, EPSILON);
  }

  @Test
  void progressIsClampedToMapBounds() {
    assertEquals(
        0f,
        ProgressionMiniMapDisplay.overallProgress(
            "maps/level1-greek.json", new Vector2(0f, -10f), 50f, 50f),
        EPSILON);
    assertEquals(
        1f,
        ProgressionMiniMapDisplay.overallProgress(
            "maps/level3.json", new Vector2(150f, 0f), 100f, 10f),
        EPSILON);
  }
}
