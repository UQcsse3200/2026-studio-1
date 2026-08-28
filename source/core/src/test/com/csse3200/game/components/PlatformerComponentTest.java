package com.csse3200.game.components;

import com.csse3200.game.extensions.GameExtension;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class PlatformerComponentTest {
  @Test
  public void testMaxDoubleJump() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, true, 3, false, 1);
    Assertions.assertEquals(
        3,
        platformerComponent.getMaxDoubleJump(),
        "Setting maxDoubleJump in the PlatformerComponent constructor "
            + "doesn't get set to the maxDoubleJump variable in the object");
  }

  @Test
  public void testBaseJumpScaler() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3);
    Assertions.assertEquals(
        3,
        platformerComponent.getBaseJumpScaler(),
        "Setting baseJumpScaler in the PlatformerComponent constructor"
            + "doesn't get set to the baseJumpScaler variable in the object");
  }

  @Test
  public void testSuperJumpScaler() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, false, 3, true, 1);
    Assertions.assertEquals(
        1,
        platformerComponent.getSuperJumpScaler(),
        "Setting superJumpScaler in the PlatformerComponent constructor"
            + "doesn't get set to the superJumpScaler variable in the object");
  }

  @Test
  public void testSuperJumpBool() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, true, 3, true, 1);
    Assertions.assertTrue(
        platformerComponent.getSuperJumpBool(),
        "Setting superJumpPowerup boolean in the PlatformerComponent constructor"
            + "doesn't get set to the superJumpScaler variable in the object");
  }

  @Test
  public void testDoubleJumpBool() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, true, 3, true, 1);
    Assertions.assertTrue(
        platformerComponent.getDoubleJumpBool(),
        "Setting the doubleJumpPowerup boolean in the PlatformerComponent constructor"
            + "doesn't get set to the doubleJumpPowerup variable in the object");
  }

  @Test
  public void testSetDoubleJump() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, false, 1, true, 1);
    platformerComponent.setDoubleJump(true, 3);
    Assertions.assertTrue(
        platformerComponent.getMaxDoubleJump() == 3 && platformerComponent.getDoubleJumpBool(),
        "The setDoubleJump setter function doesn't set the doubleJumpPowerup boolean"
            + "or the maxDoubleJump variable correctly in the object");
  }

  @Test
  public void testSetSuperJump() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, false, 3, false, 1);
    platformerComponent.setSuperJump(true, 3);
    Assertions.assertTrue(
        platformerComponent.getSuperJumpScaler() == 3 && platformerComponent.getSuperJumpBool(),
        "The setSuperJump setter function doesn't set the superJumpPowerup boolean"
            + "or the superJumpScaler variable correctly in the object");
  }

  @Test
  public void testSetBaseJump() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, false, 3, true, 1);
    platformerComponent.setBaseJumpScaler(4);
    Assertions.assertEquals(
        4,
        platformerComponent.getBaseJumpScaler(),
        "The setBaseJump setter function doesn't set the baseJumpScaler" + "correctly");
  }

  @Test
  public void testDefaultPlatformerValues() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3);
    Assertions.assertEquals(3, platformerComponent.getBaseJumpScaler());
    Assertions.assertFalse(
        platformerComponent.getSuperJumpBool(), "superJumpBool should start as false");
    Assertions.assertTrue(
        platformerComponent.getSuperJumpScaler() > 0,
        "doubleJumpScaler should not" + "be zero or less");
    Assertions.assertFalse(
        platformerComponent.getDoubleJumpBool(), "doubleJumpBool should start as false");
    Assertions.assertTrue(
        platformerComponent.getMaxDoubleJump() >= 0, "maxDoubleJump should" + "not be negative");
  }
}
