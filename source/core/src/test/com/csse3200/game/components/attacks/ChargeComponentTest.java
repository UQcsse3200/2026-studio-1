package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ChargeComponentTest {
  private GameTime gameTime;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(0.1f);
    ServiceLocator.registerTimeSource(gameTime);
  }

  @Test
  void shouldThrowOnInvalidConstructorArguments() {
    assertThrows(IllegalArgumentException.class, () -> new ChargeComponent(0f, 3f, 1.5f, 2.0f));
    assertThrows(IllegalArgumentException.class, () -> new ChargeComponent(-1f, 3f, 1.5f, 2.0f));
    assertThrows(IllegalArgumentException.class, () -> new ChargeComponent(2f, -1f, 1.5f, 2.0f));
    assertThrows(IllegalArgumentException.class, () -> new ChargeComponent(2f, 3f, 0.5f, 2.0f));
    assertThrows(IllegalArgumentException.class, () -> new ChargeComponent(2f, 3f, 1.5f, 1.0f));
    assertThrows(IllegalArgumentException.class, () -> new ChargeComponent(2f, 3f, 1.5f, 0.5f));
    assertThrows(
        IllegalArgumentException.class, () -> new ChargeComponent(2f, -0.1f, 3f, 1.5f, 2.0f));
    assertThrows(
        IllegalArgumentException.class, () -> new ChargeComponent(2f, 2.0f, 3f, 1.5f, 2.0f));
    assertThrows(
        IllegalArgumentException.class, () -> new ChargeComponent(2f, 2.5f, 3f, 1.5f, 2.0f));
  }

  @Test
  void shouldInitiallyBeReadyToChargeAndNotCharging() {
    ChargeComponent charge = new ChargeComponent(2.0f, 3.0f, 1.5f, 2.0f);
    assertFalse(charge.isCharging());
    assertTrue(charge.canCharge());
    assertEquals(1.0f, charge.getDamageMultiplier());
  }

  @Test
  void shouldStartChargeSuccessfullyAndEnableMovement() {
    Entity entity = new Entity();
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    ChargeComponent charge = new ChargeComponent(2.0f, 3.0f, 1.5f, 2.0f);
    entity.addComponent(new PhysicsComponent()).addComponent(movement).addComponent(charge);
    entity.create();

    EventListener0 chargeStartListener = mock(EventListener0.class);
    entity.getEvents().addListener("chargeStart", chargeStartListener);

    Vector2 targetPos = new Vector2(10f, 5f);
    charge.startCharge(targetPos);

    assertTrue(charge.isCharging());
    assertFalse(charge.canCharge());
    assertEquals(1.5f, charge.getDamageMultiplier());
    assertTrue(movement.getMoving());
    assertEquals(targetPos, movement.getTarget());
    assertEquals(2.0f, movement.getSpeedMultiplier());
    verify(chargeStartListener).handle();
  }

  @Test
  void shouldNotStartChargeWhenCanChargeIsFalse() {
    Entity entity = new Entity();
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    ChargeComponent charge = new ChargeComponent(2.0f, 3.0f, 1.5f, 2.0f);
    entity.addComponent(new PhysicsComponent()).addComponent(movement).addComponent(charge);
    entity.create();

    charge.startCharge(new Vector2(10f, 0f));
    assertTrue(charge.isCharging());

    // Second call while charging should be a no-op
    charge.startCharge(new Vector2(20f, 0f));
    assertEquals(new Vector2(10f, 0f), movement.getTarget());
  }

  @Test
  void shouldCompleteChargeAndEnterCooldownThenBeReadyAgain() {
    Entity entity = new Entity();
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    // 0.2s duration, 0.3s cooldown
    ChargeComponent charge = new ChargeComponent(0.2f, 0.3f, 2.0f, 2.5f);
    entity.addComponent(new PhysicsComponent()).addComponent(movement).addComponent(charge);
    entity.create();

    charge.startCharge(new Vector2(5f, 0f));
    assertTrue(charge.isCharging());
    assertEquals(2.5f, movement.getSpeedMultiplier());
    assertTrue(movement.getMoving());

    // Advance 0.1s (gameTime delta is 0.1f)
    charge.update();
    assertTrue(charge.isCharging());

    // Advance another 0.1s -> total 0.2s, charge ends!
    charge.update();
    assertFalse(charge.isCharging());
    assertEquals(1.0f, charge.getDamageMultiplier());
    assertEquals(1.0f, movement.getSpeedMultiplier());
    assertFalse(movement.getMoving());
    assertFalse(charge.canCharge(), "Should be on cooldown immediately after charge ends");

    // Cooldown is 0.3s. Update 2 times (0.2s elapsed in cooldown)
    charge.update();
    charge.update();
    assertFalse(charge.canCharge(), "Should still be on cooldown after 0.2s of 0.3s cooldown");

    // Update 1 more time (total 0.3s elapsed in cooldown)
    charge.update();
    assertTrue(charge.canCharge(), "Cooldown fully elapsed, should be able to charge again");

    // Charge again
    charge.startCharge(new Vector2(8f, 0f));
    assertTrue(charge.isCharging());
    assertEquals(2.5f, movement.getSpeedMultiplier());
    assertTrue(movement.getMoving());
  }

  @Test
  void shouldRemainStationaryDuringWindupAndMoveFastDuringRush() {
    Entity entity = new Entity();
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    // 0.4s total duration: 0.2s windup, 0.2s rush, 0.3s cooldown
    ChargeComponent charge = new ChargeComponent(0.4f, 0.2f, 0.3f, 1.5f, 2.0f);
    entity.addComponent(new PhysicsComponent()).addComponent(movement).addComponent(charge);
    entity.create();

    EventListener0 windupListener = mock(EventListener0.class);
    EventListener0 rushListener = mock(EventListener0.class);
    EventListener0 endListener = mock(EventListener0.class);
    entity.getEvents().addListener("chargeWindupStart", windupListener);
    entity.getEvents().addListener("chargeRushStart", rushListener);
    entity.getEvents().addListener("chargeEnd", endListener);

    Vector2 targetPos = new Vector2(10f, 0f);
    charge.startCharge(targetPos);

    // Initial windup phase:
    assertTrue(charge.isCharging());
    assertTrue(charge.isWindup());
    assertFalse(charge.isRushing());
    assertFalse(movement.getMoving(), "Must be stationary during windup");
    verify(windupListener).handle();
    verify(rushListener, never()).handle();

    // 0.1s in windup:
    charge.update();
    assertTrue(charge.isCharging());
    assertTrue(charge.isWindup());
    assertFalse(charge.isRushing());
    assertFalse(movement.getMoving(), "Must stay stationary during windup");

    // 0.2s: transition to rush phase!
    charge.update();
    assertTrue(charge.isCharging());
    assertFalse(charge.isWindup());
    assertTrue(charge.isRushing());
    assertTrue(movement.getMoving(), "Must start moving fast in rush phase");
    assertEquals(targetPos, movement.getTarget());
    assertEquals(2.0f, movement.getSpeedMultiplier());
    verify(rushListener).handle();

    // 0.3s (0.1s into rush):
    charge.update();
    assertTrue(charge.isCharging());
    assertTrue(charge.isRushing());
    assertTrue(movement.getMoving());

    // 0.4s (total charge duration elapsed):
    charge.update();
    assertFalse(charge.isCharging());
    assertFalse(charge.isWindup());
    assertFalse(charge.isRushing());
    assertFalse(movement.getMoving(), "Must stop moving once charge finishes");
    assertEquals(1.0f, movement.getSpeedMultiplier());
    verify(endListener).handle();
  }
}
