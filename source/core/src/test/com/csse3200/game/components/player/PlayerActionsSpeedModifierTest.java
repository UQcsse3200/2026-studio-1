package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class PlayerActionsSpeedModifierTest {

  @Test
  void shouldDefaultToFullSpeedWithNoModifiers() {
    PlayerActions actions = new PlayerActions();
    assertEquals(1f, actions.getEffectiveSpeedMultiplier());
  }

  @Test
  void shouldApplySingleModifier() {
    PlayerActions actions = new PlayerActions();
    Object key = new Object();

    actions.addSpeedModifier(key, 0.5f);

    assertEquals(0.5f, actions.getEffectiveSpeedMultiplier());
  }

  @Test
  void shouldMultiplyStackedModifiers() {
    PlayerActions actions = new PlayerActions();
    Object slowKey = new Object();
    Object speedKey = new Object();

    actions.addSpeedModifier(slowKey, 0.5f);
    actions.addSpeedModifier(speedKey, 2f);

    // 0.5 * 2 = 1.0, cancel out
    assertEquals(1f, actions.getEffectiveSpeedMultiplier());
  }

  @Test
  void pauseShouldDominateOtherModifiers() {
    PlayerActions actions = new PlayerActions();
    Object pauseKey = new Object();
    Object speedKey = new Object();

    actions.addSpeedModifier(pauseKey, 0f);
    actions.addSpeedModifier(speedKey, 2f);

    // anything * 0 = 0, still paused
    assertEquals(0f, actions.getEffectiveSpeedMultiplier());
  }

  @Test
  void shouldRevertToDefaultAfterRemovingOnlyModifier() {
    PlayerActions actions = new PlayerActions();
    Object key = new Object();

    actions.addSpeedModifier(key, 0.5f);
    actions.removeSpeedModifier(key);

    assertEquals(1f, actions.getEffectiveSpeedMultiplier());
  }

  @Test
  void shouldOnlyRemoveTheGivenKey() {
    PlayerActions actions = new PlayerActions();
    Object slowKey = new Object();
    Object pauseKey = new Object();

    actions.addSpeedModifier(slowKey, 0.5f);
    actions.addSpeedModifier(pauseKey, 0f);
    actions.removeSpeedModifier(pauseKey);

    assertEquals(0.5f, actions.getEffectiveSpeedMultiplier());
  }

  @Test
  void addingSameKeyTwiceShouldOverwriteNotStack() {
    PlayerActions actions = new PlayerActions();
    Object key = new Object();

    actions.addSpeedModifier(key, 0.5f);
    actions.addSpeedModifier(key, 0.25f);

    assertEquals(0.25f, actions.getEffectiveSpeedMultiplier());
  }
}
