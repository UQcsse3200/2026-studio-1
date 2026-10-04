package com.csse3200.game.components.npc;

import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;

/**
 * Makes an entity fly: zeroes its physics body's gravity scale once created, so the world's gravity
 * never pulls it down between steering updates, and optionally applies linear damping so knockback
 * impulses fade out instead of carrying the entity away indefinitely (ground NPCs get the same
 * effect for free from floor friction, which a flying entity has none of).
 *
 * <p>Pairs with a {@link com.csse3200.game.physics.components.PhysicsMovementComponent} left in its
 * default (non-grounded) mode, which already steers toward the full 2D direction to a target -
 * including the vertical component that a grounded entity deliberately ignores (see {@link
 * com.csse3200.game.physics.components.PhysicsMovementComponent#setGroundedMovement}). With gravity
 * zeroed, that steering becomes the only vertical force acting on the entity, which is what keeps a
 * flying enemy (Harpy, Ranged Harpy) airborne while it wanders or chases instead of sinking like a
 * grounded one.
 *
 * <p>Must be added to an entity that also has a {@link PhysicsComponent}, and relies on {@link
 * PhysicsComponent#create()} having already run - true for every entity built the normal way, where
 * component creation order follows {@code addComponent} order and {@code PhysicsComponent} is
 * always added first (see {@code NPCFactory}'s {@code createBaseFlyingNPC}).
 */
public class FlightComponent extends Component {
  private final float linearDamping;

  /** Creates a flight component that only zeroes gravity, leaving linear damping untouched. */
  public FlightComponent() {
    this(0f);
  }

  /**
   * Creates a flight component that zeroes gravity and applies the given linear damping, so a
   * knockback impulse on this entity fades out over time instead of persisting forever (there is no
   * ground friction to do this for a flying entity otherwise).
   *
   * @param linearDamping linear damping to apply to the physics body; not negative
   * @throws IllegalArgumentException if {@code linearDamping} is negative
   */
  public FlightComponent(float linearDamping) {
    if (linearDamping < 0) {
      throw new IllegalArgumentException("linearDamping must not be negative");
    }
    this.linearDamping = linearDamping;
  }

  @Override
  public void create() {
    PhysicsComponent physicsComponent = entity.getComponent(PhysicsComponent.class);
    if (physicsComponent != null) {
      Body body = physicsComponent.getBody();
      if (body != null) {
        body.setGravityScale(0f);
        body.setLinearDamping(this.linearDamping);
      }
    }
  }
}
