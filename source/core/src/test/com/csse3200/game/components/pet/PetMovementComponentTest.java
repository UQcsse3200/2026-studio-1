package com.csse3200.game.components.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.GameTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PetMovementComponentTest {
  private GameTime timeSource;
  private Entity owner;
  private AnimationRenderComponent animator;
  private String currentAnimation;
  private final List<String> playedAnimations = new ArrayList<>();

  @BeforeEach
  void setUp() {
    timeSource = mock(GameTime.class);
    when(timeSource.getDeltaTime()).thenReturn(0.1f);

    owner = new Entity();
    owner.setPosition(10f, 5f);
    owner.setScale(2f, 3f);

    animator = mock(AnimationRenderComponent.class);
    currentAnimation = "bird_right";
    when(animator.getCurrentAnimation()).thenAnswer(invocation -> currentAnimation);
    doAnswer(
            invocation -> {
              currentAnimation = invocation.getArgument(0);
              playedAnimations.add(currentAnimation);
              return null;
            })
        .when(animator)
        .startAnimation(anyString());
  }

  @Test
  void shouldStartAboveAndBehindOwner() {
    Entity pet = createPet(1f, 0.5f);

    assertPositionEquals(new Vector2(8.75f, 8.25f), pet.getPosition());
  }

  @Test
  void shouldSmoothlyFollowOwner() {
    Entity pet = createPet(1f, 0.5f);
    Vector2 startPosition = pet.getPosition();

    owner.setPosition(11f, 6f);
    pet.update();

    Vector2 updatedPosition = pet.getPosition();
    Vector2 targetPosition = new Vector2(9.75f, 9.25f);
    assertTrue(updatedPosition.x > startPosition.x && updatedPosition.x < targetPosition.x);
    assertTrue(updatedPosition.y > startPosition.y && updatedPosition.y < targetPosition.y);
  }

  @Test
  void shouldMoveBehindOwnerWhenOwnerTurnsLeft() {
    Entity pet = createPet(1f, 0.5f);
    float startingX = pet.getPosition().x;

    owner.setPosition(9f, 5f);
    pet.update();

    assertTrue(pet.getPosition().x > startingX);
  }

  @Test
  void shouldCatchUpImmediatelyAfterOwnerTeleports() {
    Entity pet = createPet(1f, 0.5f);

    owner.setPosition(30f, 20f);
    pet.update();

    assertPositionEquals(new Vector2(28.75f, 23.25f), pet.getPosition());
  }

  @Test
  void shouldWaitThreeSecondsBeforeStartingIdleSpin() {
    Entity pet = createPet(1f, 0.5f);

    advance(pet, 2.9f);
    assertTrue(playedAnimations.isEmpty());
    advance(pet, 0.1f);

    assertEquals("bird_back", currentAnimation);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldTurnInPlaceAndReturnToOriginalFacing(boolean initiallyFacingRight) {
    Entity pet = createPet(1f, 0.5f);
    if (!initiallyFacingRight) {
      owner.setPosition(9f, 5f);
      advance(pet, 0.1f);
      playedAnimations.clear();
    }

    advance(pet, 3f);
    Vector2 spinPosition = pet.getPosition();
    for (int step = 0; step < 4; step++) {
      advance(pet, 0.3f);
      assertPositionEquals(spinPosition, pet.getPosition());
    }

    List<String> expected =
        initiallyFacingRight
            ? List.of("bird_back", "bird_left", "bird_front", "bird_right")
            : List.of("bird_front", "bird_right", "bird_back", "bird_left");
    assertEquals(expected, playedAnimations);
    assertEquals(initiallyFacingRight ? "bird_right" : "bird_left", currentAnimation);
  }

  @Test
  void shouldSpinOnlyOnceUntilOwnerMovesAgain() {
    Entity pet = createPet(1f, 0.5f);
    advance(pet, 3f);
    for (int step = 0; step < 4; step++) {
      advance(pet, 0.3f);
    }
    advance(pet, 30f);
    assertEquals(4, playedAnimations.size());
    assertEquals("bird_right", currentAnimation);

    owner.setPosition(11f, 5f);
    advance(pet, 0.1f);
    advance(pet, 2.9f);
    assertEquals("bird_right", currentAnimation);
    advance(pet, 0.1f);
    assertEquals("bird_back", currentAnimation);
  }

  @ParameterizedTest
  @ValueSource(floats = {-1f, 1f})
  void shouldInterruptSpinWhenOwnerMovesHorizontally(float movement) {
    Entity pet = createPet(1f, 0.5f);
    advance(pet, 3f);
    assertEquals("bird_back", currentAnimation);

    owner.setPosition(10f + movement, 5f);
    advance(pet, 0.1f);
    String expectedFacing = movement > 0 ? "bird_right" : "bird_left";
    assertEquals(expectedFacing, currentAnimation);
    advance(pet, 2.9f);
    assertEquals(expectedFacing, currentAnimation);
  }

  @Test
  void shouldInterruptSpinWhenOwnerMovesVertically() {
    Entity pet = createPet(1f, 0.5f);
    advance(pet, 3f);

    owner.setPosition(10f, 6f);
    advance(pet, 0.1f);

    assertEquals("bird_right", currentAnimation);
    advance(pet, 2.9f);
    assertEquals("bird_right", currentAnimation);
  }

  @Test
  void shouldResetIdleCountdownWhenOwnerMoves() {
    Entity pet = createPet(1f, 0.5f);
    advance(pet, 2.5f);

    owner.setPosition(10f, 6f);
    advance(pet, 0.1f);
    advance(pet, 2.9f);
    assertTrue(playedAnimations.isEmpty());
    advance(pet, 0.1f);
    assertEquals("bird_back", currentAnimation);
  }

  @ParameterizedTest
  @ValueSource(floats = {0f, -1f})
  void shouldFreezeIdleCountdownAndSpinWhenGameTimeDoesNotAdvance(float delta) {
    Entity pet = createPet(1f, 0.5f);
    advance(pet, 2.5f);
    for (int frame = 0; frame < 20; frame++) {
      advance(pet, delta);
    }
    assertTrue(playedAnimations.isEmpty());

    advance(pet, 0.5f);
    assertEquals("bird_back", currentAnimation);
    for (int frame = 0; frame < 20; frame++) {
      advance(pet, delta);
    }
    assertEquals(List.of("bird_back"), playedAnimations);
    advance(pet, 0.3f);
    assertEquals("bird_left", currentAnimation);
  }

  @Test
  void shouldNotRestartTheSameAnimationOnEveryFrame() {
    Entity pet = createPet(1f, 0.5f);
    advance(pet, 3f);
    for (int frame = 0; frame < 4; frame++) {
      advance(pet, 0.05f);
    }

    assertEquals(List.of("bird_back"), playedAnimations);
  }

  @Test
  void shouldAllowIdleSpinDespiteTinyPositionJitter() {
    Entity pet = createPet(1f, 0.5f);
    for (int frame = 0; frame < 30; frame++) {
      owner.setPosition(10f + (frame % 2 == 0 ? 0.0001f : -0.0001f), 5f);
      advance(pet, 0.1f);
    }

    assertEquals("bird_back", currentAnimation);
  }

  @Test
  void shouldInterruptSpinAndCatchUpAfterOwnerTeleports() {
    Entity pet = createPet(1f, 0.5f);
    advance(pet, 3f);

    owner.setPosition(30f, 20f);
    advance(pet, 0.1f);

    assertPositionEquals(new Vector2(28.75f, 23.25f), pet.getPosition());
    assertEquals("bird_right", currentAnimation);
  }

  @Test
  void shouldStopIdleSpinWhileOwnerIsDead() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    owner.addComponent(stats);
    Entity pet = createPet(1f, 0.5f);
    advance(pet, 3f);

    stats.setHealth(0);
    advance(pet, 0.1f);
    advance(pet, 10f);

    assertEquals("bird_right", currentAnimation);
    assertEquals(List.of("bird_back", "bird_right"), playedAnimations);
  }

  @Test
  void shouldStillFollowWithoutAnimationRenderer() {
    Entity pet =
        new Entity()
            .addComponent(new PetComponent(owner))
            .addComponent(new PetMovementComponent(timeSource, "bird"));
    pet.create();
    owner.setPosition(11f, 6f);
    advance(pet, 0.1f);
    advance(pet, 3f);

    assertPositionEquals(new Vector2(9.75f, 9.25f), pet.getPosition());
  }

  private void advance(Entity pet, float delta) {
    when(timeSource.getDeltaTime()).thenReturn(delta);
    pet.update();
  }

  private Entity createPet(float width, float height) {
    Entity pet =
        new Entity()
            .addComponent(new PetComponent(owner))
            .addComponent(animator)
            .addComponent(new PetMovementComponent(timeSource, "bird"));
    pet.setScale(width, height);
    pet.create();
    return pet;
  }

  private static void assertPositionEquals(Vector2 expected, Vector2 actual) {
    assertEquals(expected.x, actual.x, 0.001f);
    assertEquals(expected.y, actual.y, 0.001f);
  }
}
