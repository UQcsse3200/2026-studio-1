package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.utils.math.Vector2Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class TouchPlayerInputComponentTest {
  private TouchPlayerInputComponent input;
  private final List<Vector2> walks = new ArrayList<>();
  private int stops;
  private int attacks;

  @BeforeEach
  void setUp() {
    walks.clear();
    stops = 0;
    attacks = 0;

    input = new TouchPlayerInputComponent();
    Entity player = new Entity().addComponent(input);
    player.getEvents().addListener("walk", (Vector2 direction) -> walks.add(direction.cpy()));
    player.getEvents().addListener("walkStop", () -> stops++);
    player.getEvents().addListener("attack", () -> attacks++);
  }

  /** Each arrow key with the direction it should walk in. */
  static Stream<Arguments> arrowKeys() {
    return Stream.of(
        Arguments.of(Keys.UP, Vector2Utils.UP),
        Arguments.of(Keys.DOWN, Vector2Utils.DOWN),
        Arguments.of(Keys.LEFT, Vector2Utils.LEFT),
        Arguments.of(Keys.RIGHT, Vector2Utils.RIGHT));
  }

  @ParameterizedTest
  @MethodSource("arrowKeys")
  void shouldWalkInArrowKeyDirection(int key, Vector2 direction) {
    assertTrue(input.keyDown(key));

    assertEquals(List.of(direction), walks);
    assertEquals(0, stops);
  }

  @ParameterizedTest
  @ValueSource(ints = {Keys.UP, Keys.DOWN, Keys.LEFT, Keys.RIGHT})
  void shouldStopWalkingWhenArrowKeyReleased(int key) {
    input.keyDown(key);

    assertTrue(input.keyUp(key));

    assertEquals(1, stops);
    assertEquals(1, walks.size(), "releasing the key should not walk again");
  }

  @Test
  void shouldCombineTwoArrowKeys() {
    input.keyDown(Keys.UP);
    input.keyDown(Keys.RIGHT);
    assertEquals(new Vector2(1f, 1f), lastWalk());

    input.keyUp(Keys.UP);
    assertEquals(Vector2Utils.RIGHT, lastWalk());
    assertEquals(0, stops);
  }

  @Test
  void shouldStopWhenOppositeKeysCancel() {
    input.keyDown(Keys.LEFT);
    input.keyDown(Keys.RIGHT);

    assertEquals(1, stops);
    assertEquals(1, walks.size());
  }

  @Test
  void shouldIgnoreOtherKeys() {
    assertFalse(input.keyDown(Keys.A));
    assertFalse(input.keyUp(Keys.A));

    assertTrue(walks.isEmpty());
    assertEquals(0, stops);
  }

  @Test
  void shouldAttackOnTouch() {
    assertTrue(input.touchDown(10, 20, 0, 0));

    assertEquals(1, attacks);
  }

  /** Returns the most recent walk direction. */
  private Vector2 lastWalk() {
    return walks.get(walks.size() - 1);
  }
}
