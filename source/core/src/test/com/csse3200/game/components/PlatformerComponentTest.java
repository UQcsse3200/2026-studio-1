package com.csse3200.game.components;

import org.junit.Test;

public class PlatformerComponentTest {
  @Test
  public void testMaxDoubleJump() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, true, 3, false, 1);
    assert (platformerComponent.getMaxDoubleJump() == 3);
  }

  @Test
  public void testBaseJumpScaler() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3);
    assert (platformerComponent.getBaseJumpScaler() == 3);
  }

  @Test
  public void testSuperJumpScaler() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, false, 3, true, 1);
    assert (platformerComponent.getSuperJumpScaler() == 1);
  }

  @Test
  public void testSuperJumpBool() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, true, 3, true, 1);
    assert (platformerComponent.getSuperJumpBool());
  }

  @Test
  public void testDoubleJumpBool() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, true, 3, true, 1);
    assert (platformerComponent.getDoubleJumpBool());
  }

  @Test
  public void testSetDoubleJump() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, false, 1, true, 1);
    platformerComponent.setDoubleJump(true, 3);
    assert (platformerComponent.getMaxDoubleJump() == 3 && platformerComponent.getDoubleJumpBool());
  }

  @Test
  public void testSetSuperJump() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, false, 3, false, 1);
    platformerComponent.setSuperJump(true, 3);
    assert (platformerComponent.getSuperJumpScaler() == 3
        && platformerComponent.getSuperJumpBool());
  }

  @Test
  public void testSetBaseJump() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3, false, 3, true, 1);
    platformerComponent.setBaseJumpScaler(4);
    assert (platformerComponent.getBaseJumpScaler() == 4);
  }

  @Test
  public void testDefaultPlatformerValues() {
    PlatformerComponent platformerComponent = new PlatformerComponent(3);
    assert (platformerComponent.getBaseJumpScaler() == 3);
    assert (!platformerComponent.getSuperJumpBool()) : "superJumpBool should start as false";
    assert (platformerComponent.getSuperJumpScaler() > 0)
        : "doubleJumpScaler should not" + "be zero or less";
    assert (!platformerComponent.getDoubleJumpBool()) : "doubleJumpBool should start as false";
    assert (platformerComponent.getMaxDoubleJump() >= 0)
        : "maxDoubleJump should" + "not be negative";
  }
}
