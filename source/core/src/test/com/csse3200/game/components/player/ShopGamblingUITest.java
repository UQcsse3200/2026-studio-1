package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
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

  /**
   * Helper method to simulate advancing time in discrete frame steps, ensuring all Scene2D actions,
   * rotations, and sequences complete properly.
   */
  private static void advanceTime(Actor actor, float totalSeconds) {
    float step = 0.1f;
    float elapsed = 0f;
    while (elapsed < totalSeconds) {
      actor.act(step);
      elapsed += step;
    }
  }

  @Test
  void shouldInitializeGamblingWheelWithCorrectChildActors() {
    GamblingWheel wheel = new GamblingWheel(labelStyle);

    // Stack should contain: 1 Group (wheelGroup) and 1 Table (pointerOverlay)
    assertEquals(
        2, wheel.getChildren().size, "GamblingWheel stack must hold wheelGroup and pointerOverlay");
    assertTrue(
        wheel.getChildren().get(0) instanceof Group, "First child must be the rotating wheelGroup");
    assertTrue(
        wheel.getChildren().get(1) instanceof Table, "Second child must be the pointer overlay");
  }

  @Test
  void shouldBuildFivePrizeLabelsOnCatalogSet() {
    GamblingWheel wheel = new GamblingWheel(labelStyle);
    wheel.setCatalog(GamblingCatalogs.CatalogId.STANDARD, catalogs.getStandard());

    Group wheelGroup = (Group) wheel.getChildren().get(0);
    Group labelsGroup = (Group) wheelGroup.getChildren().get(1);

    assertEquals(
        GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT,
        labelsGroup.getChildren().size,
        "Wheel must populate exactly 5 prize labels");

    Label firstLabel = (Label) labelsGroup.getChildren().get(0);
    assertEquals("Health Potion", firstLabel.getText().toString());
  }

  @Test
  void shouldCalculateTargetRotationAccuratelyForTargetSlot() {
    GamblingWheel wheel = new GamblingWheel(labelStyle);
    wheel.setCatalog(GamblingCatalogs.CatalogId.STANDARD, catalogs.getStandard());

    Group wheelGroup = (Group) wheel.getChildren().get(0);

    // Test spinning to slot 2 (Speed Potion)
    // Sector size = 72 deg. Target rotation = (2 - 0.5) * 72 = 108 deg.
    AtomicBoolean completed = new AtomicBoolean(false);
    wheel.spinToSlot(2, () -> completed.set(true));

    assertTrue(wheel.isSpinning(), "Wheel should mark state as spinning immediately");

    // Advance time through multiple steps so all actions and callbacks execute
    advanceTime(wheel, 3.5f);

    assertTrue(completed.get(), "Callback must execute upon spin finish");
    assertFalse(wheel.isSpinning(), "Wheel state should reset to idle when finished");

    // Final rotation modulo 360 should equal 108 degrees
    float normalizedRotation = (wheelGroup.getRotation() % 360f + 360f) % 360f;
    assertEquals(108f, normalizedRotation, 0.5f, "Target slot 2 must align at 108 degrees");
  }

  @Test
  void shouldOrientLabelUprightWhenSlotLandsOnTopPointer() {
    GamblingWheel wheel = new GamblingWheel(labelStyle);
    wheel.setCatalog(GamblingCatalogs.CatalogId.STANDARD, catalogs.getStandard());

    Group wheelGroup = (Group) wheel.getChildren().get(0);
    Group labelsGroup = (Group) wheelGroup.getChildren().get(1);

    // Slot 3 (15 Gold): Target rotation = (3 - 0.5) * 72 = 180 deg
    Label slot3Label = (Label) labelsGroup.getChildren().get(2);
    assertEquals(
        -180f, slot3Label.getRotation(), 0.1f, "Pre-rotation must be negative target rotation");

    wheel.spinToSlot(3, null);
    advanceTime(wheel, 3.5f);

    // Normalize group rotation to [0, 360)
    float groupRot = (wheelGroup.getRotation() % 360f + 360f) % 360f;
    float labelRot = (slot3Label.getRotation() % 360f + 360f) % 360f;

    // Combined net orientation when resting under top pointer
    float netRotation = (groupRot + labelRot) % 360f;

    // 0 deg and 360 deg are equivalent upright angles
    boolean isUpright = Math.abs(netRotation) < 0.5f || Math.abs(netRotation - 360f) < 0.5f;
    assertTrue(
        isUpright,
        "Label must be oriented upright (0 degrees) when under top pointer, got: " + netRotation);
  }

  @Test
  void shouldIgnoreSubsequentSpinsWhileWheelIsSpinning() {
    GamblingWheel wheel = new GamblingWheel(labelStyle);
    wheel.setCatalog(GamblingCatalogs.CatalogId.STANDARD, catalogs.getStandard());

    AtomicBoolean firstFinished = new AtomicBoolean(false);
    AtomicBoolean secondFinished = new AtomicBoolean(false);

    wheel.spinToSlot(1, () -> firstFinished.set(true));
    assertTrue(wheel.isSpinning());

    // Attempt second spin call during active animation
    wheel.spinToSlot(4, () -> secondFinished.set(true));

    advanceTime(wheel, 3.5f);

    assertTrue(firstFinished.get(), "First spin must complete");
    assertFalse(secondFinished.get(), "Second spin call during spin must be ignored");
  }

  @Test
  void shouldRejectSpinToInvalidSlots() {
    GamblingWheel wheel = new GamblingWheel(labelStyle);
    wheel.setCatalog(GamblingCatalogs.CatalogId.STANDARD, catalogs.getStandard());

    wheel.spinToSlot(0, null);
    assertFalse(wheel.isSpinning(), "Slot index 0 is invalid");

    wheel.spinToSlot(6, null);
    assertFalse(wheel.isSpinning(), "Slot index 6 is invalid");
  }
}
