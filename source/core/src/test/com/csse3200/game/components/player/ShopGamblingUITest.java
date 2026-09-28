package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.csse3200.game.extensions.GameExtension;
import java.util.concurrent.atomic.AtomicBoolean;
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

  /** Advances Scene2D actions so the lottery animation can complete. */
  private static void advanceTime(Actor actor, float totalSeconds) {
    float step = 0.1f;
    float elapsed = 0f;

    while (elapsed < totalSeconds) {
      actor.act(step);
      elapsed += step;
    }
  }

  @Test
  void shouldCreateGamblingWheel() {
    GamblingWheel gamblingWheel = new GamblingWheel(labelStyle);

    assertTrue(
        gamblingWheel.getChildren().size > 0, "GamblingWheel should contain its lottery result UI");
  }

  @Test
  void shouldDisplayCatalogWhenSet() {
    GamblingWheel gamblingWheel = new GamblingWheel(labelStyle);

    gamblingWheel.setCatalog(GamblingCatalogs.CatalogId.STANDARD, catalogs.getStandard());

    assertTrue(
        gamblingWheel.getCatalog() != null, "GamblingWheel should store the selected catalog");

    assertTrue(
        gamblingWheel.getCatalogId() == GamblingCatalogs.CatalogId.STANDARD,
        "GamblingWheel should store the selected catalog ID");
  }

  @Test
  void shouldStartLotteryAnimation() {
    GamblingWheel gamblingWheel = new GamblingWheel(labelStyle);

    gamblingWheel.setCatalog(GamblingCatalogs.CatalogId.STANDARD, catalogs.getStandard());

    gamblingWheel.spinToSlot(1, null);

    assertTrue(
        gamblingWheel.isSpinning(),
        "GamblingWheel should be spinning immediately after spin starts");
  }

  @Test
  void shouldFinishLotteryAnimation() {
    GamblingWheel gamblingWheel = new GamblingWheel(labelStyle);

    gamblingWheel.setCatalog(GamblingCatalogs.CatalogId.STANDARD, catalogs.getStandard());

    AtomicBoolean completed = new AtomicBoolean(false);

    gamblingWheel.spinToSlot(1, () -> completed.set(true));

    advanceTime(gamblingWheel, 10.0f);

    assertTrue(
        completed.get(), "Completion callback should execute when lottery animation finishes");

    assertFalse(
        gamblingWheel.isSpinning(), "GamblingWheel should return to idle state after animation");
  }

  @Test
  void shouldIgnoreSubsequentSpinsWhileAnimating() {
    GamblingWheel gamblingWheel = new GamblingWheel(labelStyle);

    gamblingWheel.setCatalog(GamblingCatalogs.CatalogId.STANDARD, catalogs.getStandard());

    AtomicBoolean firstFinished = new AtomicBoolean(false);
    AtomicBoolean secondFinished = new AtomicBoolean(false);

    gamblingWheel.spinToSlot(1, () -> firstFinished.set(true));

    assertTrue(gamblingWheel.isSpinning());

    gamblingWheel.spinToSlot(4, () -> secondFinished.set(true));

    advanceTime(gamblingWheel, 10.0f);

    assertTrue(firstFinished.get(), "First lottery spin should complete");

    assertFalse(secondFinished.get(), "Second spin should be ignored while first spin is active");
  }

  @Test
  void shouldRejectInvalidSlots() {
    GamblingWheel gamblingWheel = new GamblingWheel(labelStyle);

    gamblingWheel.setCatalog(GamblingCatalogs.CatalogId.STANDARD, catalogs.getStandard());

    gamblingWheel.spinToSlot(0, null);

    assertFalse(gamblingWheel.isSpinning(), "Slot 0 should be rejected");

    gamblingWheel.spinToSlot(GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT + 1, null);

    assertFalse(gamblingWheel.isSpinning(), "Slot after the final prize slot should be rejected");
  }

  @Test
  void shouldResetLotteryState() {
    GamblingWheel gamblingWheel = new GamblingWheel(labelStyle);

    gamblingWheel.setCatalog(GamblingCatalogs.CatalogId.STANDARD, catalogs.getStandard());

    gamblingWheel.spinToSlot(1, null);

    assertTrue(gamblingWheel.isSpinning());

    gamblingWheel.reset();

    assertFalse(gamblingWheel.isSpinning(), "Reset should stop the lottery animation");
  }
}
