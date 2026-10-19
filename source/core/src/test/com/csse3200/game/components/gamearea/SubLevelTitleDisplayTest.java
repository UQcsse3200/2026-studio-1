package com.csse3200.game.components.gamearea;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class SubLevelTitleDisplayTest {
  private static final float STAGE_WIDTH = 1280f;
  private static final float TOLERANCE = 1e-4f;

  private Graphics realGraphics;
  private Graphics graphics;
  private Stage stage;
  private Entity player;
  private SubLevelTitleDisplay display;
  private Image blurredBackground;
  private Image veil;
  private Label title;

  @BeforeEach
  void setUp() {
    realGraphics = Gdx.graphics;
    graphics = mock(Graphics.class);
    when(graphics.getWidth()).thenReturn(100);
    when(graphics.getHeight()).thenReturn(50);
    Gdx.graphics = graphics;

    stage = mock(Stage.class);
    Viewport viewport = mock(Viewport.class);
    when(viewport.getWorldWidth()).thenReturn(STAGE_WIDTH);
    when(viewport.getWorldHeight()).thenReturn(720f);
    when(stage.getViewport()).thenReturn(viewport);
    RenderService renderService = mock(RenderService.class);
    when(renderService.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderService);

    player = new Entity();
    display = new SubLevelTitleDisplay(player);
    new Entity().addComponent(display);
    display.create();

    ArgumentCaptor<Actor> actors = ArgumentCaptor.forClass(Actor.class);
    verify(stage, times(3)).addActor(actors.capture());
    List<Actor> added = actors.getAllValues();
    blurredBackground = (Image) added.get(0);
    veil = (Image) added.get(1);
    title = (Label) added.get(2);
  }

  @AfterEach
  void tearDown() {
    Gdx.graphics = realGraphics;
  }

  @Test
  void theTitleFadesInHoldsAndFadesOut() {
    advance(0.1f);
    assertTrue(title.isVisible());
    assertEquals(0.5f, title.getColor().a, TOLERANCE);

    advance(0.5f);
    assertEquals(1f, title.getColor().a, TOLERANCE);
    assertEquals(0.68f, veil.getColor().a, TOLERANCE);
    assertFalse(blurredBackground.isVisible(), "nothing captured yet");

    advance(1.0f);
    assertTrue(title.getColor().a > 0f && title.getColor().a < 1f, "fading out");

    advance(5f);
    assertFalse(title.isVisible());
    assertFalse(veil.isVisible());
  }

  @Test
  void enteringASubLevelShowsItsNameAgain() {
    advance(5f);
    assertFalse(title.isVisible());

    player.getEvents().trigger(SubLevelEvents.SUB_LEVEL_ENTERED, "NETHER");
    advance(0.5f);

    assertTrue(title.isVisible());
    assertEquals("NETHER", title.getText().toString());
  }

  @Test
  void drawCapturesTheBackdropOnceAndLaysOutTheTitle() {
    SpriteBatch batch = mock(SpriteBatch.class);

    display.draw(batch);
    display.draw(batch);

    verify(batch, times(1)).flush();
    assertNotNull(blurredBackground.getDrawable());
    assertEquals(STAGE_WIDTH, blurredBackground.getWidth(), TOLERANCE);
    assertEquals(720f, veil.getHeight(), TOLERANCE);
    assertEquals(STAGE_WIDTH / 2f, title.getX() + title.getWidth() / 2f, TOLERANCE);

    advance(0.5f);
    assertTrue(blurredBackground.isVisible());

    // A new arrival recaptures, replacing the old texture.
    player.getEvents().trigger(SubLevelEvents.SUB_LEVEL_ENTERED, "DUNGEON");
    display.draw(batch);
    verify(batch, times(2)).flush();

    assertDoesNotThrow(display::dispose);
  }

  @Test
  void disposeWithoutACaptureIsSafe() {
    assertDoesNotThrow(display::dispose);
  }

  @Test
  void aShortTitleKeepsTheUsualSize() {
    assertEquals(3.2f, SubLevelTitleDisplay.titleScale(200f, STAGE_WIDTH), TOLERANCE);
  }

  @Test
  void aLongTitleShrinksToFillMostOfTheScreen() {
    // 512 wide unscaled fits 80% of a 1280 stage at exactly 2x.
    assertEquals(2f, SubLevelTitleDisplay.titleScale(512f, STAGE_WIDTH), TOLERANCE);
  }

  @Test
  void aTitleWiderThanTheScreenIsScaledBelowItsNaturalSize() {
    assertEquals(0.5f, SubLevelTitleDisplay.titleScale(2048f, STAGE_WIDTH), TOLERANCE);
  }

  @Test
  void aTitleWithNoWidthUsesTheUsualSize() {
    assertEquals(3.2f, SubLevelTitleDisplay.titleScale(0f, STAGE_WIDTH), TOLERANCE);
  }

  private void advance(float seconds) {
    when(graphics.getDeltaTime()).thenReturn(seconds);
    display.update();
  }
}
