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
import java.util.concurrent.atomic.AtomicInteger;
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

  @Test
  void shouldWaitTwoSecondsAfterSpinBeforeFlying() {
    Entity pet = createPet(1f, 0.5f);
    Vector2 restingPosition = pet.getPosition();
    advance(pet, 3f);
    advance(pet, 1.2f);

    advance(pet, 1.9f);
    assertPositionEquals(restingPosition, pet.getPosition());
    advance(pet, 0.2f);

    assertTrue(pet.getPosition().x > restingPosition.x);
    assertTrue(pet.getPosition().y > restingPosition.y);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldFlyToOtherSideAndBackFromEitherFacing(boolean initiallyFacingRight) {
    Entity pet = createPet(1f, 0.5f);
    if (!initiallyFacingRight) {
      owner.setPosition(9f, 5f);
      advance(pet, 0.1f);
    }
    finishSpinAndWaitForFlight(pet);
    Vector2 restingPosition = pet.getPosition();
    float ownerCenterX = owner.getCenterPosition().x;

    advance(pet, 0.5f);
    assertEquals(ownerCenterX, pet.getCenterPosition().x, 0.001f);
    assertTrue(pet.getPosition().y > restingPosition.y);
    assertEquals(initiallyFacingRight ? "bird_right" : "bird_left", currentAnimation);

    advance(pet, 0.5f);
    if (initiallyFacingRight) {
      assertTrue(pet.getPosition().x > owner.getPosition().x + owner.getScale().x);
    } else {
      assertTrue(pet.getPosition().x + pet.getScale().x < owner.getPosition().x);
    }
    assertEquals(initiallyFacingRight ? "bird_left" : "bird_right", currentAnimation);

    advance(pet, 0.5f);
    assertEquals(ownerCenterX, pet.getCenterPosition().x, 0.001f);
    assertTrue(pet.getPosition().y > restingPosition.y);
    advance(pet, 0.5f);
    assertPositionEquals(restingPosition, pet.getPosition());
    assertEquals(initiallyFacingRight ? "bird_right" : "bird_left", currentAnimation);
  }

  @Test
  void shouldStartTripsEveryFourSecondsWithARestBetweenThem() {
    Entity pet = createPet(1f, 0.5f);
    Vector2 restingPosition = pet.getPosition();
    finishSpinAndWaitForFlight(pet);
    advance(pet, 0.5f);
    Vector2 firstTripPosition = pet.getPosition();
    advance(pet, 1.5f);
    assertPositionEquals(restingPosition, pet.getPosition());

    advance(pet, 1.9f);
    assertPositionEquals(restingPosition, pet.getPosition());
    advance(pet, 0.6f);
    assertPositionEquals(firstTripPosition, pet.getPosition());

    // Only the initial turn uses the front/back poses; repeated trips do not replay it.
    assertEquals(1, playedAnimations.stream().filter("bird_back"::equals).count());
    assertEquals(1, playedAnimations.stream().filter("bird_front"::equals).count());
  }

  @ParameterizedTest
  @ValueSource(floats = {-1f, 1f})
  void shouldSmoothlyResumeFollowingWhenOwnerMovesDuringFlight(float movement) {
    Entity pet = createPet(1f, 0.5f);
    finishSpinAndWaitForFlight(pet);
    advance(pet, 0.5f);
    Vector2 flightPosition = pet.getPosition();

    owner.setPosition(10f + movement, 5f);
    Vector2 followPosition = new Vector2(movement > 0 ? 9.75f : 11.25f, 8.25f);
    advance(pet, 0.1f);

    assertTrue(pet.getPosition().dst2(followPosition) < flightPosition.dst2(followPosition));
    assertTrue(pet.getPosition().dst2(followPosition) > 0.001f);
    assertEquals(movement > 0 ? "bird_right" : "bird_left", currentAnimation);
    advance(pet, 2.9f);
    assertPositionEquals(followPosition, pet.getPosition());
    advance(pet, 0.1f);
    assertEquals(movement > 0 ? "bird_back" : "bird_front", currentAnimation);
  }

  @Test
  void shouldCancelFlightWhenOwnerMovesVertically() {
    Entity pet = createPet(1f, 0.5f);
    finishSpinAndWaitForFlight(pet);
    advance(pet, 0.5f);
    owner.setPosition(10f, 6f);
    advance(pet, 0.1f);
    advance(pet, 2.9f);

    assertPositionEquals(new Vector2(8.75f, 9.25f), pet.getPosition());
    assertEquals("bird_right", currentAnimation);
  }

  @ParameterizedTest
  @ValueSource(floats = {0f, -1f})
  void shouldFreezeFlightAndResumeFromTheSamePosition(float pausedDelta) {
    Entity pet = createPet(1f, 0.5f);
    finishSpinAndWaitForFlight(pet);
    advance(pet, 0.5f);
    Vector2 pausedPosition = pet.getPosition();
    int animationsBeforePause = playedAnimations.size();

    for (int frame = 0; frame < 30; frame++) {
      advance(pet, pausedDelta);
    }
    assertPositionEquals(pausedPosition, pet.getPosition());
    assertEquals(animationsBeforePause, playedAnimations.size());

    advance(pet, 0.5f);
    assertTrue(pet.getPosition().x > owner.getPosition().x + owner.getScale().x);
    assertEquals("bird_left", currentAnimation);
  }

  @Test
  void shouldCancelFlightAndCatchUpAfterOwnerTeleports() {
    Entity pet = createPet(1f, 0.5f);
    finishSpinAndWaitForFlight(pet);
    advance(pet, 0.5f);

    owner.setPosition(30f, 20f);
    advance(pet, 0.1f);

    assertPositionEquals(new Vector2(28.75f, 23.25f), pet.getPosition());
    assertEquals("bird_right", currentAnimation);
  }

  @Test
  void shouldCancelFlightWhileOwnerIsDead() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    owner.addComponent(stats);
    Entity pet = createPet(1f, 0.5f);
    Vector2 restingPosition = pet.getPosition();
    finishSpinAndWaitForFlight(pet);
    advance(pet, 0.5f);

    stats.setHealth(0);
    advance(pet, 2f);
    advance(pet, 10f);

    assertPositionEquals(restingPosition, pet.getPosition());
    assertEquals("bird_right", currentAnimation);
  }

  @Test
  void shouldReachTheSameFlightPositionWithDifferentFrameTimes() {
    Entity pet = createPet(1f, 0.5f);
    Entity coarsePet =
        new Entity()
            .addComponent(new PetComponent(owner))
            .addComponent(new PetMovementComponent(timeSource, "bird"));
    coarsePet.setScale(1f, 0.5f);
    coarsePet.create();
    float[] frameTimes = {0.016f, 0.033f, 0.05f, 0.011f};
    double elapsed = 0;
    int frame = 0;
    while (elapsed < 6.9) {
      float delta = (float) Math.min(frameTimes[frame++ % frameTimes.length], 6.9 - elapsed);
      advance(pet, delta);
      elapsed += delta;
    }
    advance(coarsePet, 6.9f);

    assertPositionEquals(pet.getPosition(), coarsePet.getPosition());
  }

  @Test
  void shouldStayBesideOwnerWhileRepeatedlyAttacking() {
    Entity pet = createCombatPet();
    PetCombatComponent combat = pet.getComponent(PetCombatComponent.class);
    Vector2 restingPosition = pet.getPosition();
    AtomicInteger attacks = new AtomicInteger();
    pet.getEvents().addListener("petAttack", (Entity target) -> attacks.incrementAndGet());
    combat.requestAttack(new Entity().addComponent(new CombatStatsComponent(100, 0)));

    for (int frame = 0; frame < 80; frame++) {
      advance(pet, 0.1f);
      assertPositionEquals(restingPosition, pet.getPosition());
      assertEquals("bird_right", currentAnimation);
    }

    assertTrue(attacks.get() > 1);
    assertTrue(playedAnimations.isEmpty(), "Combat must suppress both idle turns and flights.");
  }

  @Test
  void shouldInterruptSpinBeforeQueuedAttackIsProcessed() {
    Entity pet = createCombatPet();
    advance(pet, 3f);
    assertEquals("bird_back", currentAnimation);
    pet.getComponent(PetCombatComponent.class)
        .requestAttack(new Entity().addComponent(new CombatStatsComponent(100, 0)));

    // Exercise movement first: a queued hit must suppress idle regardless of update order.
    when(timeSource.getDeltaTime()).thenReturn(0.1f);
    pet.getComponent(PetMovementComponent.class).update();

    assertEquals("bird_right", currentAnimation);
    advance(pet, 3f);
    assertEquals("bird_right", currentAnimation);
  }

  @Test
  void shouldReturnSmoothlyFromIdleFlightWhenCombatStarts() {
    Entity pet = createCombatPet();
    Vector2 restingPosition = pet.getPosition();
    finishSpinAndWaitForFlight(pet);
    advance(pet, 0.5f);
    Vector2 flightPosition = pet.getPosition();
    pet.getComponent(PetCombatComponent.class)
        .requestAttack(new Entity().addComponent(new CombatStatsComponent(100, 0)));

    advance(pet, 0.1f);

    assertTrue(pet.getPosition().dst2(restingPosition) < flightPosition.dst2(restingPosition));
    assertTrue(pet.getPosition().dst2(restingPosition) > 0.001f);
    assertEquals("bird_right", currentAnimation);
    advance(pet, 2f);
    assertPositionEquals(restingPosition, pet.getPosition());
  }

  @Test
  void shouldRestartIdleCountdownAfterTargetDies() {
    Entity pet = createCombatPet();
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 0));
    pet.getComponent(PetCombatComponent.class).requestAttack(target);
    advance(pet, 8f);
    assertTrue(playedAnimations.isEmpty());

    target.getComponent(CombatStatsComponent.class).setHealth(0);
    advance(pet, 2.9f);
    assertTrue(playedAnimations.isEmpty());
    advance(pet, 0.1f);
    assertEquals("bird_back", currentAnimation);
  }

  @Test
  void shouldKeepFollowingOwnerDuringCombat() {
    Entity pet = createCombatPet();
    Vector2 restingPosition = pet.getPosition();
    pet.getComponent(PetCombatComponent.class)
        .requestAttack(new Entity().addComponent(new CombatStatsComponent(100, 0)));
    owner.setPosition(9f, 5f);

    advance(pet, 0.1f);

    assertTrue(pet.getPosition().x > restingPosition.x);
    assertEquals("bird_left", currentAnimation);
    advance(pet, 8f);
    assertPositionEquals(new Vector2(11.25f, 8.25f), pet.getPosition());
    assertEquals("bird_left", currentAnimation);
  }

  private Entity createCombatPet() {
    owner.addComponent(new CombatStatsComponent(100, 10));
    Entity pet =
        new Entity()
            .addComponent(new PetComponent(owner))
            .addComponent(animator)
            .addComponent(new PetMovementComponent(timeSource, "bird"))
            .addComponent(new PetCombatComponent(timeSource));
    pet.setScale(1f, 0.5f);
    pet.create();
    return pet;
  }

  private void finishSpinAndWaitForFlight(Entity pet) {
    advance(pet, 3f);
    for (int step = 0; step < 4; step++) {
      advance(pet, 0.3f);
    }
    advance(pet, 2f);
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
