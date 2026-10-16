package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.ComponentPriority;
import com.csse3200.game.components.effects.FrozenCause;
import com.csse3200.game.components.effects.SpeedEffectComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * Tests {@link FrozenIndicatorDisplay}, the countdown label shown while the player is stopped. The
 * player is a bare entity with a mock {@code PlayerActions} behind a real {@code
 * SpeedEffectComponent}, so the whole chain (apply a stopping effect, see the label, count down,
 * see it vanish) runs without a game. The Stage is a mock; the table the display adds to it is
 * captured so the test can read its visibility and label text.
 */
@ExtendWith(GameExtension.class)
class FrozenIndicatorDisplayTest {
  private Stage stage;
  private Entity player;
  private SpeedEffectComponent speed;
  private FrozenIndicatorDisplay display;
  private Table table;
  private Label label;

  @BeforeEach
  void setUp() {
    stage = mock(Stage.class);
    RenderService renderService = mock(RenderService.class);
    when(renderService.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderService);

    PlayerActions playerActions = mock(PlayerActions.class);
    when(playerActions.getPrio()).thenReturn(ComponentPriority.LOW);
    speed = new SpeedEffectComponent();
    display = new FrozenIndicatorDisplay();
    player = new Entity().addComponent(playerActions).addComponent(speed).addComponent(display);
    player.create();

    ArgumentCaptor<Table> added = ArgumentCaptor.forClass(Table.class);
    verify(stage).addActor(added.capture());
    table = added.getValue();
    label = (Label) table.getCells().first().getActor();
  }

  /** Runs the speed effect's per-frame update the given number of times. */
  private void tick(int ticks) {
    for (int i = 0; i < ticks; i++) {
      speed.update();
    }
  }

  // ---------- formatLabel ----------

  @Test
  void shouldFormatTheLabelForEachCause() {
    assertEquals("Frozen 1.8s", FrozenIndicatorDisplay.formatLabel("Frozen", 108));
    assertEquals("Petrified 3.0s", FrozenIndicatorDisplay.formatLabel("Petrified", 180));
  }

  @Test
  void shouldShowFrozenForAnUnknownOrMissingCause() {
    assertEquals("Frozen 1.0s", FrozenIndicatorDisplay.formatLabel("Burning", 60));
    assertEquals("Frozen 1.0s", FrozenIndicatorDisplay.formatLabel(null, 60));
  }

  @Test
  void shouldNeverShowLessThanZeroSeconds() {
    assertEquals("Frozen 0.0s", FrozenIndicatorDisplay.formatLabel("Frozen", 0));
    assertEquals("Frozen 0.0s", FrozenIndicatorDisplay.formatLabel("Frozen", -5));
  }

  @Test
  void shouldRoundOneTickDownToOneDecimalPlace() {
    assertEquals("Frozen 0.0s", FrozenIndicatorDisplay.formatLabel("Frozen", 1));
  }

  @Test
  void shouldFormatTheLargestWholeNumberWithoutError() {
    String text =
        assertDoesNotThrow(() -> FrozenIndicatorDisplay.formatLabel("Frozen", Integer.MAX_VALUE));

    assertTrue(text.endsWith("s"));
  }

  // ---------- showing, counting down and hiding ----------

  @Test
  void shouldStartHiddenWithNoText() {
    assertFalse(table.isVisible());
    assertEquals("", label.getText().toString());
  }

  @Test
  void shouldFollowAFreezeThroughItsLifetime() {
    player.getEvents().trigger(FrozenCause.STATUS_CAUSE_EVENT, FrozenCause.FROZEN);
    player.getEvents().trigger("applySpeedEffect", 120, 0f);
    display.update();
    assertTrue(table.isVisible());
    assertEquals("Frozen 2.0s", label.getText().toString());

    tick(60);
    display.update();
    assertEquals("Frozen 1.0s", label.getText().toString());

    tick(60);
    assertFalse(table.isVisible());
  }

  @Test
  void shouldShowPetrifiedForMedusasGaze() {
    player.getEvents().trigger(FrozenCause.STATUS_CAUSE_EVENT, FrozenCause.PETRIFIED);
    player.getEvents().trigger("applySpeedEffect", 180, 0f);

    display.update();

    assertEquals("Petrified 3.0s", label.getText().toString());
  }

  @Test
  void shouldSwitchToTheCauseThatArrivesAfterTheStart() {
    player.getEvents().trigger(SpeedEffectComponent.FROZEN_START_EVENT);
    player.getEvents().trigger(FrozenCause.STATUS_CAUSE_EVENT, FrozenCause.PETRIFIED);

    display.update();

    assertTrue(label.getText().toString().startsWith("Petrified"));
  }

  @Test
  void shouldForgetTheCauseOnceTheFreezeEnds() {
    player.getEvents().trigger(FrozenCause.STATUS_CAUSE_EVENT, FrozenCause.PETRIFIED);
    player.getEvents().trigger("applySpeedEffect", 2, 0f);
    tick(2);

    player.getEvents().trigger("applySpeedEffect", 120, 0f); // a freeze with no cause named
    display.update();

    assertEquals("Frozen 2.0s", label.getText().toString());
  }

  @Test
  void shouldIgnoreANullCause() {
    player.getEvents().trigger(FrozenCause.STATUS_CAUSE_EVENT, (String) null);
    player.getEvents().trigger("applySpeedEffect", 60, 0f);

    display.update();

    assertEquals("Frozen 1.0s", label.getText().toString());
  }

  @Test
  void shouldHideOnDeath() {
    player.getEvents().trigger(FrozenCause.STATUS_CAUSE_EVENT, FrozenCause.PETRIFIED);
    player.getEvents().trigger("applySpeedEffect", 120, 0f);

    player.getEvents().trigger("death");

    assertFalse(table.isVisible());
  }

  @Test
  void shouldKeepASingleTableWhenStartedTwice() {
    player.getEvents().trigger(SpeedEffectComponent.FROZEN_START_EVENT);
    player.getEvents().trigger(SpeedEffectComponent.FROZEN_START_EVENT);

    assertTrue(table.isVisible());
    verify(stage).addActor(table); // added once, in create(), and never again
  }

  @Test
  void shouldStayHiddenForAnEffectThatOnlySlowsThePlayer() {
    player.getEvents().trigger("applySpeedEffect", 60, 0.5f);

    display.update();

    assertFalse(table.isVisible());
  }

  // ---------- lifecycle and errors ----------

  @Test
  void shouldRefuseToCreateWithoutASpeedEffect() {
    Entity bare = new Entity().addComponent(new FrozenIndicatorDisplay());

    IllegalStateException error = assertThrows(IllegalStateException.class, bare::create);

    assertTrue(error.getMessage().contains("SpeedEffectComponent"));
  }

  @Test
  void shouldNotFailWhenUpdatedBeforeItIsCreated() {
    FrozenIndicatorDisplay fresh = new FrozenIndicatorDisplay();

    assertDoesNotThrow(fresh::update);
  }

  @Test
  void shouldDisposeWhileShownAndAgainWithoutError() {
    player.getEvents().trigger(SpeedEffectComponent.FROZEN_START_EVENT);

    assertDoesNotThrow(display::dispose);
    assertDoesNotThrow(display::dispose);
  }
}
