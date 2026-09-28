package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.csse3200.game.extensions.GameExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopGamblingUITest {

  private Label.LabelStyle labelStyle;
  private GamblingCatalogs catalogs;

  @BeforeEach
  void setUp() {
    labelStyle = new Label.LabelStyle(new BitmapFont(), Color.WHITE);

    ShopComponent shop = new ShopComponent().seedDefaultCatalog();
    catalogs = shop.getGamblingCatalogs();
  }

  /** Advances Scene2D actions so the blind-box animation can complete. */
  private static void advanceTime(Actor actor, float totalSeconds) {
    float step = 0.1f;
    float elapsed = 0f;

    while (elapsed < totalSeconds) {
      actor.act(step);
      elapsed += step;
    }
  }

  private GamblingWheel newWheel() {
    GamblingWheel gamblingWheel = new GamblingWheel(labelStyle);
    gamblingWheel.setCatalog(GamblingCatalogs.CatalogId.STANDARD, catalogs.getStandard());
    return gamblingWheel;
  }

  @Test
  void shouldCreateGamblingWheel() {
    GamblingWheel gamblingWheel = new GamblingWheel(labelStyle);

    assertTrue(
        gamblingWheel.getChildren().size > 0, "GamblingWheel should contain its blind box UI");
  }

  @Test
  void shouldDisplayCatalogWhenSet() {
    GamblingWheel gamblingWheel = newWheel();

    assertNotNull(gamblingWheel.getCatalog(), "GamblingWheel should store the selected catalog");
    assertEquals(
        GamblingCatalogs.CatalogId.STANDARD,
        gamblingWheel.getCatalogId(),
        "GamblingWheel should store the selected catalog ID");
  }

  @Test
  void shouldStartBlindBoxAnimation() {
    GamblingWheel gamblingWheel = newWheel();

    gamblingWheel.spinToSlot(1, null);

    assertTrue(
        gamblingWheel.isSpinning(),
        "GamblingWheel should be spinning immediately after spin starts");
    assertFalse(gamblingWheel.isAwaitingClick(), "Result should not be waiting yet");
  }

  @Test
  void shouldHoldResultUntilPlayerClicks() {
    GamblingWheel gamblingWheel = newWheel();

    AtomicBoolean completed = new AtomicBoolean(false);
    gamblingWheel.spinToSlot(1, () -> completed.set(true));

    // Animation and lock delay are long finished, but nobody has clicked yet.
    advanceTime(gamblingWheel, 10.0f);

    assertTrue(gamblingWheel.isAwaitingClick(), "Result should wait for the player to click");
    assertTrue(gamblingWheel.isSpinning(), "Display stays busy while the result is held");
    assertFalse(completed.get(), "Callback must not run before the player clicks");

    // Time passing must never dismiss the result on its own.
    advanceTime(gamblingWheel, 30.0f);
    assertTrue(gamblingWheel.isAwaitingClick());
    assertFalse(completed.get());
  }

  @Test
  void shouldFinishAfterPlayerClicks() {
    GamblingWheel gamblingWheel = newWheel();

    AtomicBoolean completed = new AtomicBoolean(false);
    gamblingWheel.spinToSlot(1, () -> completed.set(true));

    advanceTime(gamblingWheel, 10.0f);
    gamblingWheel.confirmResult();

    assertTrue(completed.get(), "Completion callback should execute after the player clicks");
    assertFalse(gamblingWheel.isSpinning(), "GamblingWheel should return to idle after the click");
    assertFalse(gamblingWheel.isAwaitingClick());
  }

  @Test
  void shouldIgnoreClickBeforeResultIsRevealed() {
    GamblingWheel gamblingWheel = newWheel();

    AtomicInteger calls = new AtomicInteger();
    gamblingWheel.spinToSlot(1, calls::incrementAndGet);

    // Click while the bag is still shaking.
    advanceTime(gamblingWheel, 0.5f);
    gamblingWheel.confirmResult();

    assertTrue(gamblingWheel.isSpinning(), "Early click must not cancel the animation");
    assertEquals(0, calls.get());

    advanceTime(gamblingWheel, 10.0f);
    gamblingWheel.confirmResult();
    gamblingWheel.confirmResult(); // second click must not run the callback again

    assertEquals(1, calls.get(), "Callback should run exactly once");
  }

  @Test
  void shouldIgnoreSubsequentSpinsWhileAnimating() {
    GamblingWheel gamblingWheel = newWheel();

    AtomicBoolean firstFinished = new AtomicBoolean(false);
    AtomicBoolean secondFinished = new AtomicBoolean(false);

    gamblingWheel.spinToSlot(1, () -> firstFinished.set(true));

    assertTrue(gamblingWheel.isSpinning());

    gamblingWheel.spinToSlot(4, () -> secondFinished.set(true));

    advanceTime(gamblingWheel, 10.0f);

    // Still held on the first result, so a new spin must still be ignored.
    gamblingWheel.spinToSlot(4, () -> secondFinished.set(true));

    gamblingWheel.confirmResult();
    advanceTime(gamblingWheel, 10.0f);

    assertTrue(firstFinished.get(), "First spin should complete after the click");
    assertFalse(secondFinished.get(), "Second spin should be ignored while first spin is active");
  }

  @Test
  void shouldRejectInvalidSlots() {
    GamblingWheel gamblingWheel = newWheel();

    gamblingWheel.spinToSlot(0, null);

    assertFalse(gamblingWheel.isSpinning(), "Slot 0 should be rejected");

    gamblingWheel.spinToSlot(GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT + 1, null);

    assertFalse(gamblingWheel.isSpinning(), "Slot after the final prize slot should be rejected");
  }

  @Test
  void shouldResetBlindBoxState() {
    GamblingWheel gamblingWheel = newWheel();

    gamblingWheel.spinToSlot(1, null);

    assertTrue(gamblingWheel.isSpinning());

    gamblingWheel.reset();

    assertFalse(gamblingWheel.isSpinning(), "Reset should stop the animation");
    assertFalse(gamblingWheel.isAwaitingClick());
  }

  @Test
  void shouldResetWhileWaitingForClick() {
    GamblingWheel gamblingWheel = newWheel();

    AtomicBoolean completed = new AtomicBoolean(false);
    gamblingWheel.spinToSlot(1, () -> completed.set(true));
    advanceTime(gamblingWheel, 10.0f);

    gamblingWheel.reset();

    assertFalse(gamblingWheel.isAwaitingClick(), "Reset should drop the held result");
    assertFalse(gamblingWheel.isSpinning());

    gamblingWheel.confirmResult();
    assertFalse(completed.get(), "Callback should not run after the result was reset");
  }
}
