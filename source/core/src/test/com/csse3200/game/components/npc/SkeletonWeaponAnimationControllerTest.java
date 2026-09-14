package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.EnemyWeaponAnimationComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
class SkeletonWeaponAnimationControllerTest {
  @Mock EnemyWeaponAnimationComponent weaponAnimator;

  private Entity entity;
  private SkeletonWeaponAnimationController controller;

  @BeforeEach
  void beforeEach() {
    entity = new Entity();
    controller = new SkeletonWeaponAnimationController();

    lenient().when(weaponAnimator.hasAnimation("default_r")).thenReturn(true);
    lenient().when(weaponAnimator.hasAnimation("default_l")).thenReturn(true);
    lenient().when(weaponAnimator.hasAnimation("sword_r")).thenReturn(true);
    lenient().when(weaponAnimator.hasAnimation("sword_l")).thenReturn(true);
    lenient().when(weaponAnimator.hasAnimation("bow_r")).thenReturn(true);
    lenient().when(weaponAnimator.hasAnimation("bow_l")).thenReturn(true);

    entity.addComponent(weaponAnimator).addComponent(controller);
  }

  @Test
  void shouldStartInRestingStanceFacingRight() {
    entity.create();
    verify(weaponAnimator).startAnimation("default_r");
    assertTrue(controller.isFacingRight());
    assertFalse(controller.isAttacking());
  }

  @Test
  void shouldFaceLeftOnLeftEvents() {
    entity.create();
    entity.getEvents().trigger("walkLeftStart");
    verify(weaponAnimator).startAnimation("default_l");
    assertFalse(controller.isFacingRight());

    entity.getEvents().trigger("idleLeftStart");
    verify(weaponAnimator, times(2)).startAnimation("default_l");
  }

  @Test
  void shouldFaceRightOnRightEvents() {
    entity.create();
    entity.getEvents().trigger("walkLeftStart");
    entity.getEvents().trigger("walkRightStart");

    verify(weaponAnimator, times(2)).startAnimation("default_r");
    assertTrue(controller.isFacingRight());
  }

  @Test
  void shouldPlaySwordAttackInCurrentDirection() {
    entity.create();

    // Facing right attack
    entity.getEvents().trigger("meleeAttack", new Entity());
    verify(weaponAnimator).startAnimation("sword_r");
    assertTrue(controller.isAttacking());

    // Switch to left and attack
    when(weaponAnimator.isFinished()).thenReturn(true);
    controller.update();
    entity.getEvents().trigger("walkLeftStart");
    entity.getEvents().trigger("meleeAttack", new Entity());
    verify(weaponAnimator).startAnimation("sword_l");
  }

  @Test
  void shouldPlayBowAttackInCurrentDirection() {
    entity.create();

    entity.getEvents().trigger("rangedAttackStart");
    verify(weaponAnimator).startAnimation("bow_r");
    assertTrue(controller.isAttacking());
  }

  @Test
  void shouldReturnToRestingStanceWhenAttackCompletes() {
    entity.create();
    entity.getEvents().trigger("meleeAttack", new Entity());
    assertTrue(controller.isAttacking());

    when(weaponAnimator.isFinished()).thenReturn(true);
    controller.update();

    assertFalse(controller.isAttacking());
    verify(weaponAnimator, times(2)).startAnimation("default_r");
  }
}
