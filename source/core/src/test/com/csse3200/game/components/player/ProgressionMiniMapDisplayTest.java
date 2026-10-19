package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.areas.LevelGameArea;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

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
  void hidesUntilThereIsAPlayerThenShowsAndDrawsProgress() {
    Stage stage = registerStage();
    LevelGameArea area = mock(LevelGameArea.class);
    AtomicReference<LevelGameArea> current = new AtomicReference<>();
    ProgressionMiniMapDisplay display = new ProgressionMiniMapDisplay(current::get);
    new Entity().addComponent(display);

    display.create();
    Actor actor = capturedActor(stage);
    assertFalse(actor.isVisible(), "no area yet");

    current.set(area);
    display.update();
    assertFalse(actor.isVisible(), "area without a player");

    Entity player = new Entity();
    player.setPosition(10f, 20f);
    when(area.getPlayer()).thenReturn(player);
    when(area.getMapPath()).thenReturn("maps/level2.json");
    when(area.getMapWorldWidth()).thenReturn(50f);
    when(area.getMapWorldHeight()).thenReturn(40f);
    display.update();
    assertTrue(actor.isVisible());

    Batch batch = mock(Batch.class);
    when(batch.getColor()).thenReturn(new Color(0.5f, 0.5f, 0.5f, 1f));
    actor.draw(batch, 1f);
    verify(batch).draw(any(TextureRegion.class), anyFloat(), anyFloat(), eq(48f), eq(53f));
    verify(batch).setColor(new Color(0.5f, 0.5f, 0.5f, 1f));

    assertDoesNotThrow(() -> display.draw(mock(SpriteBatch.class)));
    assertDoesNotThrow(display::dispose);
  }

  @Test
  void updateAndDisposeBeforeCreateAreSafe() {
    registerStage();
    ProgressionMiniMapDisplay display = new ProgressionMiniMapDisplay(() -> null);
    new Entity().addComponent(display);

    assertDoesNotThrow(display::update);
    assertDoesNotThrow(display::dispose);
  }

  @Test
  void unknownOrEmptyMapsCountAsLevelOne() {
    assertEquals(0, ProgressionMiniMapDisplay.levelIndex(null));
    assertEquals(
        0f,
        ProgressionMiniMapDisplay.overallProgress("maps/test.json", new Vector2(5f, 5f), 0f, 0f),
        EPSILON);
  }

  private static Stage registerStage() {
    Texture playerIcon = mock(Texture.class);
    when(playerIcon.getWidth()).thenReturn(49);
    when(playerIcon.getHeight()).thenReturn(56);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset("images/knight_default.png", Texture.class)).thenReturn(playerIcon);
    ServiceLocator.registerResourceService(resources);

    Stage stage = mock(Stage.class);
    when(stage.getHeight()).thenReturn(720f);
    RenderService renderer = mock(RenderService.class);
    when(renderer.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderer);
    return stage;
  }

  private static Actor capturedActor(Stage stage) {
    ArgumentCaptor<Actor> actor = ArgumentCaptor.forClass(Actor.class);
    verify(stage).addActor(actor.capture());
    return actor.getValue();
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
