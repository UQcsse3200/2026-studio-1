package com.csse3200.game.components.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PetMovementComponentTest {
  private GameTime timeSource;
  private Entity owner;

  @BeforeEach
  void setUp() {
    timeSource = mock(GameTime.class);
    when(timeSource.getDeltaTime()).thenReturn(0.1f);

    owner = new Entity();
    owner.setPosition(10f, 5f);
    owner.setScale(2f, 3f);
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

  private Entity createPet(float width, float height) {
    Entity pet =
        new Entity()
            .addComponent(new PetComponent(owner))
            .addComponent(new PetMovementComponent(timeSource));
    pet.setScale(width, height);
    pet.create();
    return pet;
  }

  private static void assertPositionEquals(Vector2 expected, Vector2 actual) {
    assertEquals(expected.x, actual.x, 0.001f);
    assertEquals(expected.y, actual.y, 0.001f);
  }
}
