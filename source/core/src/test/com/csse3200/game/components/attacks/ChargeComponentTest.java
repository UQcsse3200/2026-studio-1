package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.enemies.attacks.ChargeComponent;
import com.csse3200.game.components.enemies.attacks.TouchAttackComponent;
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

  /** The rush starts exactly once, on the update where the windup runs out. */
  @Test
  void shouldStartRushExactlyOnceWhenWindupEnds() {
    Entity entity = new Entity();
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    // 0.4s charge with a 0.2s windup; each update advances 0.1s
    ChargeComponent charge = new ChargeComponent(0.4f, 0.2f, 0.3f, 1.5f, 2.0f);
    entity.addComponent(new PhysicsComponent()).addComponent(movement).addComponent(charge);
    entity.create();

    EventListener0 rushListener = mock(EventListener0.class);
    entity.getEvents().addListener("chargeRushStart", rushListener);

    Vector2 target = new Vector2(10f, 0f);
    charge.startCharge(target);

    charge.update(); // 0.1s, still winding up
    assertTrue(charge.isWindup());
    verify(rushListener, never()).handle();

    charge.update(); // 0.2s, windup ends
    assertTrue(charge.isRushing());
    assertTrue(movement.getMoving());
    assertEquals(target, movement.getTarget());
    assertEquals(2.0f, movement.getSpeedMultiplier());
    verify(rushListener, times(1)).handle();

    charge.update(); // 0.3s, the rush continues but does not start again
    verify(rushListener, times(1)).handle();
  }

  /** The charge ends when its timer runs out. */
  @Test
  void shouldEndTheChargeWhenTheTimerRunsOut() {
    Entity entity = new Entity();
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    // 0.2s charge, no windup; each update advances 0.1s
    ChargeComponent charge = new ChargeComponent(0.2f, 0.3f, 2.0f, 2.5f);
    entity.addComponent(new PhysicsComponent()).addComponent(movement).addComponent(charge);
    entity.create();

    EventListener0 endListener = mock(EventListener0.class);
    entity.getEvents().addListener("chargeEnd", endListener);

    charge.startCharge(new Vector2(5f, 0f));

    charge.update(); // 0.1s
    assertTrue(charge.isCharging());
    verify(endListener, never()).handle();

    charge.update(); // 0.2s, timer runs out
    assertFalse(charge.isCharging());
    assertFalse(movement.getMoving());
    assertEquals(1.0f, movement.getSpeedMultiplier());
    verify(endListener, times(1)).handle();
  }

  /** A touch hit during the rush ends the charge, once. */
  @Test
  void shouldEndTheChargeWhenTheTouchAttackLandsDuringTheRush() {
    Entity entity = new Entity();
    TouchAttackComponent touchAttack = mock(TouchAttackComponent.class);
    ChargeComponent charge = new ChargeComponent(2.0f, 3.0f, 1.5f, 2.0f); // no windup
    entity
      .addComponent(new PhysicsComponent())
      .addComponent(new PhysicsMovementComponent())
      .addComponent(touchAttack)
      .addComponent(charge);
    entity.create();

    EventListener0 endListener = mock(EventListener0.class);
    entity.getEvents().addListener("chargeEnd", endListener);

    charge.startCharge(new Vector2(10f, 0f));
    assertTrue(charge.isCharging());

    entity.getEvents().trigger("touchAttackHit", new Entity());

    assertFalse(charge.isCharging());
    verify(endListener, times(1)).handle();
    verify(touchAttack).deactivate();
  }

  /** A touch hit during the stationary windup does not end the charge. */
  @Test
  void shouldIgnoreATouchHitDuringTheWindup() {
    Entity entity = new Entity();
    TouchAttackComponent touchAttack = mock(TouchAttackComponent.class);
    ChargeComponent charge = new ChargeComponent(0.4f, 0.2f, 0.3f, 1.5f, 2.0f);
    entity
      .addComponent(new PhysicsComponent())
      .addComponent(new PhysicsMovementComponent())
      .addComponent(touchAttack)
      .addComponent(charge);
    entity.create();

    EventListener0 endListener = mock(EventListener0.class);
    entity.getEvents().addListener("chargeEnd", endListener);

    charge.startCharge(new Vector2(10f, 0f));
    assertTrue(charge.isWindup());

    entity.getEvents().trigger("touchAttackHit", new Entity());

    assertTrue(charge.isCharging());
    verify(endListener, never()).handle();
  }

  /** A hit on the first frame of the rush ends the charge once, not twice. */
  @Test
  void shouldEndOnlyOnceWhenAHitLandsOnTheFirstFrameOfTheRush() {
    Entity entity = new Entity();
    Entity victim = new Entity();
    TouchAttackComponent touchAttack = mock(TouchAttackComponent.class);
    ChargeComponent charge = new ChargeComponent(0.4f, 0.2f, 0.3f, 1.5f, 2.0f);
    entity
      .addComponent(new PhysicsComponent())
      .addComponent(new PhysicsMovementComponent())
      .addComponent(touchAttack)
      .addComponent(charge);
    entity.create();

    // switching the touch attack on lands a hit straight away
    doAnswer(
      invocation -> {
        entity.getEvents().trigger("touchAttackHit", victim);
        return null;
      })
      .when(touchAttack)
      .activate();

    EventListener0 endListener = mock(EventListener0.class);
    entity.getEvents().addListener("chargeEnd", endListener);

    charge.startCharge(new Vector2(10f, 0f));
    charge.update(); // 0.1s windup
    charge.update(); // 0.2s windup ends, the rush begins and the hit lands

    assertFalse(charge.isCharging());
    verify(endListener, times(1)).handle();
  }

  /** The windup can end on an entity with no movement component. */
  @Test
  void shouldCompleteTheWindupWithoutAMovementComponent() {
    Entity entity = new Entity();
    ChargeComponent charge = new ChargeComponent(0.4f, 0.2f, 0.3f, 1.5f, 2.0f);
    entity.addComponent(charge);
    entity.create();

    EventListener0 rushListener = mock(EventListener0.class);
    entity.getEvents().addListener("chargeRushStart", rushListener);

    charge.startCharge(new Vector2(10f, 0f));

    assertDoesNotThrow(
      () -> {
        charge.update();
        charge.update();
      });
    assertTrue(charge.isRushing());
    verify(rushListener, times(1)).handle();
  }
}