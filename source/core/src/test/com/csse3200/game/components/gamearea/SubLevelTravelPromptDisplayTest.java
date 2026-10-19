package com.csse3200.game.components.gamearea;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.csse3200.game.components.player.LadderComponent;
import com.csse3200.game.components.player.SubLevelTravelComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class SubLevelTravelPromptDisplayTest {
  private Stage stage;
  private Camera camera;

  @BeforeEach
  void setUp() {
    stage = mock(Stage.class);
    Viewport viewport = mock(Viewport.class);
    when(viewport.unproject(any(Vector2.class))).thenAnswer(call -> call.getArgument(0));
    when(stage.getViewport()).thenReturn(viewport);
    RenderService renderService = mock(RenderService.class);
    when(renderService.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderService);
    camera = mock(Camera.class);
    when(camera.project(any(Vector3.class))).thenAnswer(call -> call.getArgument(0));
  }

  @Test
  void showsTheLiftPromptAtTheLiftDoor() {
    SubLevelTravelComponent travel = mock(SubLevelTravelComponent.class);
    when(travel.canTravelToNether()).thenReturn(true);
    SubLevelTravelPromptDisplay display = createDisplay(player(travel, null));
    Label prompt = capturedPrompt();

    display.update();

    assertTrue(prompt.isVisible());
    assertEquals(SubLevelTravelPromptDisplay.PROMPT_TEXT, prompt.getText().toString());
  }

  @Test
  void showsTheLadderPromptBesideAnAutoClimbableLadder() {
    LadderComponent ladder = mock(LadderComponent.class);
    when(ladder.canAutoClimb()).thenReturn(true);
    SubLevelTravelPromptDisplay display = createDisplay(player(null, ladder));
    Label prompt = capturedPrompt();

    display.update();

    assertTrue(prompt.isVisible());
    assertEquals(SubLevelTravelPromptDisplay.LADDER_PROMPT_TEXT, prompt.getText().toString());
  }

  @Test
  void liftPromptWinsOverTheLadderPrompt() {
    SubLevelTravelComponent travel = mock(SubLevelTravelComponent.class);
    when(travel.canTravelToNether()).thenReturn(true);
    LadderComponent ladder = mock(LadderComponent.class);
    SubLevelTravelPromptDisplay display = createDisplay(player(travel, ladder));
    Label prompt = capturedPrompt();

    display.update();

    assertEquals(SubLevelTravelPromptDisplay.PROMPT_TEXT, prompt.getText().toString());
    verify(ladder, never()).canAutoClimb();
  }

  @Test
  void hidesThePromptWithNothingToUse() {
    SubLevelTravelComponent travel = mock(SubLevelTravelComponent.class);
    LadderComponent ladder = mock(LadderComponent.class);
    SubLevelTravelPromptDisplay display = createDisplay(player(travel, ladder));
    Label prompt = capturedPrompt();

    display.update();

    assertFalse(prompt.isVisible());
  }

  @Test
  void hidesThePromptForAPlayerWithoutEitherComponent() {
    SubLevelTravelPromptDisplay display = createDisplay(player(null, null));
    Label prompt = capturedPrompt();

    display.update();

    assertFalse(prompt.isVisible());
  }

  @Test
  void drawCentresTheVisiblePromptAboveThePlayer() {
    SubLevelTravelComponent travel = mock(SubLevelTravelComponent.class);
    when(travel.canTravelToNether()).thenReturn(true);
    SubLevelTravelPromptDisplay display = createDisplay(player(travel, null));
    Label prompt = capturedPrompt();
    display.update();

    display.draw(mock(SpriteBatch.class));

    // The world-to-stage mapping is the identity apart from the flipped screen y.
    assertEquals(4.5f - prompt.getWidth() / 2f, prompt.getX(), 1e-4f);
    verify(camera).project(new Vector3(4.5f, 3.35f, 0f));
  }

  @Test
  void drawDoesNothingWhileHidden() {
    SubLevelTravelPromptDisplay display = createDisplay(player(null, null));
    capturedPrompt();
    display.update();

    display.draw(mock(SpriteBatch.class));

    verify(camera, never()).project(any(Vector3.class));
  }

  @Test
  void disposeRemovesThePrompt() {
    SubLevelTravelPromptDisplay display = createDisplay(player(null, null));

    assertDoesNotThrow(display::dispose);
  }

  /** A 1x1 player at (4, 2) with the given components, either of which may be null. */
  private static Entity player(SubLevelTravelComponent travel, LadderComponent ladder) {
    Entity player = mock(Entity.class);
    when(player.getComponent(SubLevelTravelComponent.class)).thenReturn(travel);
    when(player.getComponent(LadderComponent.class)).thenReturn(ladder);
    when(player.getPosition()).thenReturn(new Vector2(4f, 2f));
    when(player.getScale()).thenReturn(new Vector2(1f, 1f));
    when(player.getCenterPosition()).thenReturn(new Vector2(4.5f, 2.5f));
    return player;
  }

  private SubLevelTravelPromptDisplay createDisplay(Entity player) {
    SubLevelTravelPromptDisplay display = new SubLevelTravelPromptDisplay(player, camera);
    new Entity().addComponent(display);
    display.create();
    return display;
  }

  private Label capturedPrompt() {
    ArgumentCaptor<Actor> actor = ArgumentCaptor.forClass(Actor.class);
    verify(stage).addActor(actor.capture());
    return (Label) actor.getValue();
  }
}
