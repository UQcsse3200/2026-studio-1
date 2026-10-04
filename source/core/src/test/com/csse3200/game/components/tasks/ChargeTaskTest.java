package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.attacks.ChargeComponent;
import com.csse3200.game.entities.Entity;
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
class ChargeTaskTest {
  private GameTime gameTime;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(0.1f);
    ServiceLocator.registerTimeSource(gameTime);
  }

  @Test
  void shouldReturnInactivePriorityWhenTargetNullOrDead() {
    ChargeComponent charge = new ChargeComponent(2.0f, 3.0f, 1.5f, 2.0f);
    ChargeTask taskWithNullTarget = new ChargeTask(null, charge, 10f, 16, -1);
    assertEquals(-1, taskWithNullTarget.getPriority());

    Entity deadTarget = new Entity().addComponent(new CombatStatsComponent(0, 5));
    ChargeTask taskWithDeadTarget = new ChargeTask(deadTarget, charge, 10f, 16, -1);
    assertEquals(-1, taskWithDeadTarget.getPriority());
  }

  @Test
  void shouldReturnActivePriorityWhenTargetInRangeAndCanCharge() {
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 5));
    target.setPosition(5f, 0f);

    ChargeComponent charge = new ChargeComponent(2.0f, 3.0f, 1.5f, 2.0f);
    ChargeTask task = new ChargeTask(target, charge, 10f, 16, -1);

    Entity owner = new Entity().addComponent(charge);
    owner.setPosition(0f, 0f);
    AITaskComponent ai = new AITaskComponent().addTask(task);
    owner.addComponent(ai);

    assertEquals(16, task.getPriority());
  }

  @Test
  void shouldReturnInactivePriorityWhenTargetOutOfAggroRadius() {
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 5));
    target.setPosition(15f, 0f);

    ChargeComponent charge = new ChargeComponent(2.0f, 3.0f, 1.5f, 2.0f);
    ChargeTask task = new ChargeTask(target, charge, 10f, 16, -1);

    Entity owner = new Entity().addComponent(charge);
    owner.setPosition(0f, 0f);
    AITaskComponent ai = new AITaskComponent().addTask(task);
    owner.addComponent(ai);

    assertEquals(-1, task.getPriority());
  }

  @Test
  void shouldReturnInactivePriorityWhileOnCooldown() {
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 5));
    target.setPosition(5f, 0f);

    Entity owner = new Entity();
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    ChargeComponent charge = new ChargeComponent(0.1f, 3.0f, 1.5f, 2.0f);
    ChargeTask task = new ChargeTask(target, charge, 10f, 16, -1);

    owner
        .addComponent(new PhysicsComponent())
        .addComponent(movement)
        .addComponent(charge)
        .addComponent(new AITaskComponent().addTask(task));
    owner.create();
    owner.setPosition(0f, 0f);

    // Initial priority is active
    assertEquals(16, task.getPriority());

    // Start charge
    task.start();
    assertEquals(16, task.getPriority(), "Should maintain active priority while charging");

    // Advance time to complete charge (0.1s duration)
    charge.update();
    // Charge has completed and is now on cooldown
    assertEquals(-1, task.getPriority(), "Should return inactive priority while on cooldown");
  }

  @Test
  void shouldStartChargeOnStartAndResetMovementOnStop() {
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 5));
    target.setPosition(5f, 0f);

    Entity owner = new Entity();
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    ChargeComponent charge = new ChargeComponent(2.0f, 3.0f, 1.5f, 2.0f);
    ChargeTask task = new ChargeTask(target, charge, 10f, 16, -1);

    owner
        .addComponent(new PhysicsComponent())
        .addComponent(movement)
        .addComponent(charge)
        .addComponent(new AITaskComponent().addTask(task));
    owner.create();
    owner.setPosition(0f, 0f);

    task.start();
    assertEquals(new Vector2(5f, 0f), movement.getTarget());
    assertEquals(2.0f, movement.getSpeedMultiplier());
    assertTrue(movement.getMoving());

    task.stop();
    assertEquals(1.0f, movement.getSpeedMultiplier());
    assertFalse(movement.getMoving());
  }

  @Test
  void shouldRemainStationaryDuringWindupInChargeTask() {
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 5));
    target.setPosition(5f, 0f);

    Entity owner = new Entity();
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    // 0.4s total duration: 0.2s windup, 0.2s rush
    ChargeComponent charge = new ChargeComponent(0.4f, 0.2f, 3.0f, 1.5f, 2.0f);
    ChargeTask task = new ChargeTask(target, charge, 10f, 16, -1);

    owner
        .addComponent(new PhysicsComponent())
        .addComponent(movement)
        .addComponent(charge)
        .addComponent(new AITaskComponent().addTask(task));
    owner.create();
    owner.setPosition(0f, 0f);

    task.start();
    assertEquals(16, task.getPriority(), "Should have active priority during windup");
    assertFalse(movement.getMoving(), "Should remain stationary during windup");

    // Advance 0.2s into rush phase
    charge.update();
    charge.update();
    assertEquals(16, task.getPriority(), "Should have active priority during rush");
    assertTrue(movement.getMoving(), "Should move fast during rush");
    assertEquals(2.0f, movement.getSpeedMultiplier());
  }
}
