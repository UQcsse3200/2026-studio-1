package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class TutorialTest {
  private Stage stage;
  private Entity player;
  private Tutorial tutorial;
  private Actor guide;

  @BeforeEach
  void setUp() {
    Viewport viewport = mock(Viewport.class);
    when(viewport.getWorldWidth()).thenReturn(1280f);
    when(viewport.getWorldHeight()).thenReturn(720f);
    stage = mock(Stage.class);
    when(stage.getViewport()).thenReturn(viewport);

    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));

    tutorial = new Tutorial();
    player = new Entity().addComponent(tutorial);
    player.create();

    ArgumentCaptor<Actor> added = ArgumentCaptor.forClass(Actor.class);
    verify(stage, times(1)).addActor(added.capture());
    guide = added.getValue();
  }

  @Test
  void shouldAddVisibleGuideToStageOnCreate() {
    assertTrue(guide.isVisible());
    assertEquals(1f, guide.getColor().a, 0.001f);
  }

  @Test
  void shouldHideGuideOnToggle() {
    toggle();
    finishAnimation();

    assertFalse(guide.isVisible());
  }

  @Test
  void shouldShowGuideAgainOnSecondToggle() {
    toggle();
    finishAnimation();

    toggle();
    assertTrue(guide.isVisible());
    assertEquals(0f, guide.getColor().a, 0.001f, "the guide should fade in from transparent");

    finishAnimation();
    assertEquals(1f, guide.getColor().a, 0.001f);
  }

  @Test
  void shouldCancelHidingWhenToggledMidFade() {
    toggle();
    guide.act(0.05f);

    toggle();
    finishAnimation();

    assertTrue(guide.isVisible(), "the earlier fade-out should not hide the guide");
    assertEquals(1f, guide.getColor().a, 0.001f);
  }

  @Test
  void shouldRemoveGuideOnDispose() {
    Group parent = new Group();
    parent.addActor(guide);

    tutorial.dispose();

    assertFalse(guide.hasParent());
  }

  /** Simulates the player pressing the tutorial key. */
  private void toggle() {
    player.getEvents().trigger("toggleTutorial");
  }

  /** Plays a few frames so the guide's fade animation runs to the end. */
  private void finishAnimation() {
    for (int frame = 0; frame < 3; frame++) {
      guide.act(1f);
    }
  }
}
